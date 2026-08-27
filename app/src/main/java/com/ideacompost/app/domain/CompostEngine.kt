package com.ideacompost.app.domain

import com.ideacompost.app.data.ai.AiRouter
import com.ideacompost.app.data.ai.GardenerPrompts
import com.ideacompost.app.data.ai.JsonExtractor
import com.ideacompost.app.data.db.dao.AgentDao
import com.ideacompost.app.data.db.dao.BedEventDao
import com.ideacompost.app.data.db.dao.CompostDao
import com.ideacompost.app.data.db.dao.IdeaDao
import com.ideacompost.app.data.db.dao.ProbioticDao
import com.ideacompost.app.data.db.entity.AgentEntity
import com.ideacompost.app.data.db.entity.BedEventEntity
import com.ideacompost.app.data.db.entity.CompostEntity
import com.ideacompost.app.data.db.entity.CompostStageEntity
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Deferred
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.min
import kotlin.random.Random

/** 堆肥编排器：预检→识别→召集→发酵→整合→评估反哺（03 协议 M2 实现）。 */
@Singleton
class CompostEngine @Inject constructor(
    private val compostDao: CompostDao,
    private val ideaDao: IdeaDao,
    private val agentDao: AgentDao,
    private val probioticDao: ProbioticDao,
    private val bedEventDao: BedEventDao,
    private val llm: AiRouter,
    private val prompts: GardenerPrompts
) {
    data class RosterEntry(
        val id: String, val name: String, val code: String,
        val weight: Double, val role: String, val type: String
    )

    private val builtinBoosts = mapOf(
        "probiotic_philosophy" to mapOf("哲学菌" to 2.5),
        "probiotic_research" to mapOf("研究问题菌" to 2.5, "假说菌" to 1.5),
        "probiotic_counterintuitive" to mapOf("反事实菌" to 2.0, "概念辨析菌" to 1.5),
        "probiotic_devils_advocate" to mapOf("反驳菌" to 3.0),
        "probiotic_interdisciplinary" to mapOf("类比菌" to 2.0),
        "probiotic_socratic" to mapOf("概念辨析菌" to 2.5, "哲学菌" to 1.5)
    )

    suspend fun run(compostId: String) {
        val compost = compostDao.getById(compostId) ?: return
        try {
            runStages(compost)
        } catch (e: CancellationException) {
            // 产物已生成则保持 awaiting_feedback（可回来看/反馈），否则诚实挂起
            val cur = compostDao.getById(compostId)
            if (cur?.outputJson == null) {
                compostDao.updateProgress(compostId, "suspended", cur?.currentStage ?: compost.currentStage, now())
            }
            throw e
        } catch (e: Exception) {
            compostDao.fail(compostId, "failed", e.message?.take(300), now())
        }
    }

    private suspend fun runStages(compost: CompostEntity) {
        val ideaIds = JSONArray(compost.inputIdeaIds).let { l -> (0 until l.length()).map { l.getString(it) } }
        val ideas = ideaDao.byIds(ideaIds).sortedBy { ideaIds.indexOf(it.id) }
        val probioticIds = JSONArray(compost.probioticIds).let { l -> (0 until l.length()).map { l.getString(it) } }
        val probiotics = probioticIds.mapNotNull { probioticDao.byId(it) }

        val ctx = buildContext(ideas, probiotics)

        // —— 断点续跑（INV-8）：已持久化的阶段直接复用，不重复调用 ——
        val done = compostDao.stages(compost.id).associateBy { it.stageKey }
        fun reuseObject(key: String): JSONObject? =
            done[key]?.let { runCatching { JSONObject(it.payload) }.getOrNull() }
        fun reuseArray(key: String): JSONArray? =
            done[key]?.let { runCatching { JSONArray(it.payload) }.getOrNull() }

        // —— S1 识别 ——
        android.util.Log.d("CompostEngine", "S1 identify begin")
        val identifyJson = reuseObject("identify") ?: run {
            compostDao.updateProgress(compost.id, "running", "identify", now())
            val raw = llm.complete("identify", prompts.system + "\n\n" + prompts.stage("s1"), ctx)
            JsonExtractor.extractObject(raw).also { persistStage(compost.id, "identify", it.toString()) }
        }
        android.util.Log.d("CompostEngine", "S1 done")

        // —— S2 召集（代码强制配额，园丁不参与 M2 简化） ——
        android.util.Log.d("CompostEngine", "S2 convoke begin")
        val roster: List<RosterEntry> = reuseObject("convoke")?.let { j ->
            val arr = j.optJSONArray("roster") ?: JSONArray(j.toString())
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                RosterEntry(
                    id = o.optString("id"), name = o.optString("name"), code = o.optString("code"),
                    weight = o.optDouble("weight", 1.0), role = o.optString("role"), type = o.optString("type")
                )
            }.takeIf { it.isNotEmpty() }
        } ?: run {
            compostDao.updateProgress(compost.id, "running", "convoke", now())
            val pool = agentDao.convokable()
            val r = convoke(pool, ideas, probioticIds, identifyJson)
            val rj = JSONArray(r.map { entryJson(it) })
            persistStage(compost.id, "convoke", JSONObject().put("roster", rj).toString())
            compostDao.update(compostDao.getById(compost.id)!!.copy(rosterJson = rj.toString(), updatedAt = now()))
            r
        }
        android.util.Log.d("CompostEngine", "S2 done: ${roster.size} agents")

        // —— S3 发酵 ——
        val rounds = when (compost.depth) {
            "shallow" -> listOf("r1", "r3")
            else -> listOf("r1", "r2", "r3")
        }
        val history = StringBuilder()
        for (r in rounds) {
            val arr: JSONArray = reuseArray("ferment_$r") ?: run {
                compostDao.updateProgress(compost.id, "running", "ferment_$r", now())
                android.util.Log.d("CompostEngine", "S3 $r begin")
                val participants = participantsFor(r, roster)
                val outputs: List<org.json.JSONObject> = coroutineScope {
                    val deferreds: List<Deferred<String>> = participants.map { agent ->
                        async {
                            val sys = agentCapability(agent) + "\n\n" +
                                    prompts.stage(r)
                                        .replace("{{agent_name}}", agent.name)
                                        .replace("{{CODE}}", codeOf(agent))
                            val user = ctx + history + "\n\n—— 你是 ${agent.name}（短码 ${codeOf(agent)}）——"
                            llm.complete(r, sys, user)
                        }
                    }
                    deferreds.map { d -> runCatching { JsonExtractor.extractObject(d.await()) } }
                        .mapNotNull { it.getOrNull() }
                }
                JSONArray().also { arr -> outputs.forEach { arr.put(it) } }
                    .also { persistStage(compost.id, "ferment_$r", it.toString()) }
            }
            history.append("\n\n—— 轮次 ${r.removePrefix("r")} 输出 ——\n").append(arr.toString())
        }

        // —— S4 整合 ——
        android.util.Log.d("CompostEngine", "S4 integrate begin")
        val output: JSONObject = when {
            compost.outputJson != null ->
                runCatching { JSONObject(compost.outputJson!!) }.getOrNull()
            done["integrate"] != null ->
                runCatching { JSONObject(done["integrate"]!!.payload) }.getOrNull()
            else -> null
        } ?: run {
            compostDao.updateProgress(compost.id, "running", "integrate", now())
            val raw = llm.complete(
                "integrate",
                prompts.system + "\n\n" + prompts.stage("s4"),
                ctx + history
            )
            val o = JsonExtractor.extractObject(raw)
            o.putSafe("agents_used", mergeRosterIntoOutput(o, roster))
            persistStage(compost.id, "integrate", o.toString())
            val title = o.optString("title", "（初步）一次堆肥")
            compostDao.completeOutput(compost.id, title, o.toString(), "awaiting_feedback", "integrate", now())
            ideaDao.markComposted(ideaIds, now())
            o
        }

        // —— S5 评估反哺（可独立重入） ——
        android.util.Log.d("CompostEngine", "S4 done")
        if (compost.nutritionJson != null) {
            compostDao.setNutrition(compost.id, compost.nutritionJson!!, "done", "done", now())
            return
        }
        compostDao.updateProgress(compost.id, "running", "assess", now())
        android.util.Log.d("CompostEngine", "S5 assess llm call begin")
        val assessRaw = llm.complete(
            "assess",
            prompts.system + "\n\n" + prompts.stage("s5"),
            ctx + history + "\n\n—— 最终产物 ——\n" + output.toString()
        )
        val assessJson = runCatching { JsonExtractor.extractObject(assessRaw) }.getOrNull() ?: JSONObject()
        android.util.Log.d("CompostEngine", "S5 assess done, applying nutrition")
        persistStage(compost.id, "assess", assessJson.toString())
        applyNutrition(assessJson)
        android.util.Log.d("CompostEngine", "S5 nutrition applied")
        compostDao.setNutrition(compost.id, assessJson.toString(), "done", "done", now())
        bedEventDao.insert(
            BedEventEntity(
                ts = now(), eventType = "compost_completed",
                payload = JSONObject().put("compost_id", compost.id).put("agents", JSONArray(roster.map { it.name })).toString()
            )
        )
    }

    private fun buildContext(
        ideas: List<com.ideacompost.app.data.db.entity.IdeaEntity>,
        probiotics: List<com.ideacompost.app.data.db.entity.ProbioticEntity>
    ): String {
        val sb = StringBuilder()
        sb.append("【面包渣】\n")
        val circ = listOf("①", "②", "③", "④", "⑤", "⑥", "⑦", "⑧")
        ideas.forEachIndexed { i, idea ->
            sb.append("idea:").append(i).append("（${circ.getOrElse(i) { "·" }}}） ").append(idea.content.trim()).append("\n")
        }
        if (probiotics.isNotEmpty()) {
            sb.append("\n【思想益生菌】（用户本次施加的认知方向）\n")
            probiotics.forEach { sb.append("·").append(it.name).append("：").append(it.promptLogic.replaceFirstChar { it.uppercase() }).append("\n") }
        } else {
            sb.append("\n【思想益生菌】无——本次由园丁自由调度。\n")
        }
        sb.append("\n【菌床】菌床尚年轻，暂无历史洞察。\n")
        return sb.toString()
    }

    /** 70/20/10 配额（02 §4.2）：核心相关菌 3 + 相邻菌 1 + 意外菌 1；反驳菌保证条款。 */
    private suspend fun convoke(
        pool: List<AgentEntity>,
        ideas: List<com.ideacompost.app.data.db.entity.IdeaEntity>,
        probioticIds: List<String>,
        identifyJson: JSONObject
    ): List<RosterEntry> {
        val crumbText = ideas.joinToString(" ") { it.content }
        val domains = identifyJson.optJSONArray("potential_domains")?.let { l -> (0 until l.length()).map { l.getString(it) } } ?: emptyList()
        val methods = identifyJson.optJSONArray("potential_methods")?.let { l -> (0 until l.length()).map { l.getString(it) } } ?: emptyList()
        val boosts = mutableMapOf<String, Double>()
        probioticIds.forEach { pid ->
            builtinBoosts[pid]?.forEach { (name, v) ->
                boosts[name] = maxOf(boosts[name] ?: 1.0, v)
            }
        }

        data class Scored(val a: AgentEntity, val w: Double)
        val scored = pool.map { a ->
            val rel = relevance(crumbText, domains, methods, a)
            val boost = boosts[a.name] ?: 1.0
            val vitalityFactor = 0.3 + 0.7 * a.vitality / 100.0
            Scored(a, rel * boost * vitalityFactor)
        }.sortedByDescending { it.w }

        val core = scored.take(3)
        val adjacent = scored.drop(3).take(2).firstOrNull()
        val rest = scored.drop(core.size + (if (adjacent != null) 1 else 0))
        val wildcard = rest.filter { it.a.status == "active" }.randomOrNull() ?: rest.firstOrNull()

        val picked = ArrayList<Scored>().apply {
            addAll(core)
            adjacent?.let { add(it) }
            wildcard?.let { add(it) }
        }
        // 反驳菌保证条款
        if (picked.none { it.a.type == "method" }) {
            val rebuttal = scored.firstOrNull { it.a.name == "反驳菌" } ?: scored.firstOrNull { it.a.type == "method" }
            if (rebuttal != null) {
                val idx = picked.indexOfFirst { it == wildcard }.takeIf { it >= 0 } ?: picked.lastIndex
                picked[idx] = rebuttal
            }
        }

        return picked.mapIndexed { i, s ->
            RosterEntry(
                id = s.a.id, name = s.a.name, code = codeOf(s.a),
                weight = (s.w * 100).toInt() / 100.0,
                role = when {
                    i < core.size && core.contains(s) -> "core"
                    s == adjacent -> "adjacent"
                    else -> "wildcard"
                },
                type = s.a.type
            )
        }
    }

    private fun relevance(crumbs: String, domains: List<String>, methods: List<String>, a: AgentEntity): Double {
        val text = (a.name + a.description)
        var r = 0.3
        domains.forEach { d -> if (text.contains(d.removeSuffix("菌")) || a.name.contains(d.removeSuffix("菌"))) r += 0.45 }
        methods.forEach { m -> if (text.contains(m.removeSuffix("菌")) || a.name.contains(m.removeSuffix("菌"))) r += 0.35 }
        // 简易 bigram 重叠：能力卡与面包渣的字符重合度
        val card = a.capabilityCard.take(400)
        r += min(0.3, bigramOverlap(crumbs, card))
        return min(r, 1.4)
    }

    private fun bigramOverlap(a: String, b: String): Double {
        if (a.length < 2 || b.length < 2) return 0.0
        val bs = b.windowed(2).toSet()
        val hits = a.windowed(2).count { it in bs }
        return hits / 60.0
    }

    private suspend fun participantsFor(round: String, roster: List<RosterEntry>): List<AgentEntity> {
        val agents = roster.mapNotNull { agentDaoSync(it.id) }
        return when (round) {
            "r3" -> {
                val creatives = agents.filter { it.type == "creative" }
                val topDomains = roster.filter { it.type == "domain" }.take(2).mapNotNull { agentDaoSync(it.id) }
                (creatives + topDomains).distinctBy { it.id }
            }
            else -> agents
        }
    }

    private var agentCache: MutableMap<String, AgentEntity> = mutableMapOf()
    private suspend fun agentDaoSync(id: String): AgentEntity? {
        agentCache[id]?.let { return it }
        val a = agentDao.byId(id)
        if (a != null) agentCache[id] = a
        return a
    }

    private fun codeOf(a: AgentEntity): String {
        val raw = a.id.substringAfterLast('_').uppercase().take(3)
        return raw.ifEmpty { a.name.take(1) }
    }

    private fun agentCapability(a: AgentEntity): String =
        a.capabilityCard.ifBlank { a.description }

    private suspend fun applyNutrition(assessJson: JSONObject) {
        val arr = assessJson.optJSONArray("nutrition_awarded") ?: return
        val now = now()
        for (i in 0 until arr.length()) {
            val item = arr.optJSONObject(i) ?: continue
            val name = item.optString("agent")
            val score = item.optDouble("score", 0.0)
            if (score <= 0) continue
            val agent = agentDao.byName(name) ?: continue
            agentDao.addNutrition(agent.id, score, now)
            agentDao.recordParticipation(agent.id, now)
        }
    }

    private fun mergeRosterIntoOutput(output: JSONObject, roster: List<RosterEntry>): JSONArray {
        val used = output.optJSONArray("agents_used") ?: JSONArray()
        val byName = roster.associateBy { it.name }
        for (i in 0 until used.length()) {
            val o = used.optJSONObject(i) ?: continue
            val entry = byName[o.optString("agent")] ?: continue
            o.put("weight", entry.weight)
            o.put("quota_role", entry.role)
        }
        return used
    }

    private fun entryJson(r: RosterEntry): JSONObject = JSONObject()
        .put("id", r.id).put("name", r.name).put("code", r.code)
        .put("weight", r.weight).put("role", r.role).put("type", r.type)

    private suspend fun persistStage(compostId: String, key: String, payload: String) {
        compostDao.insertStage(CompostStageEntity(compostId = compostId, stageKey = key, payload = payload, createdAt = now()))
    }

    private fun JSONObject.putSafe(key: String, value: Any): JSONObject = apply { put(key, value) }

    private fun now() = System.currentTimeMillis()

    companion object {
        fun newCompost(ideaIds: List<String>, probioticIds: List<String>, depth: String): CompostEntity {
            val now = System.currentTimeMillis()
            return CompostEntity(
                id = UUID.randomUUID().toString(),
                status = "pending",
                depth = depth,
                inputIdeaIds = JSONArray(ideaIds).toString(),
                probioticIds = JSONArray(probioticIds).toString(),
                currentStage = "preflight",
                rosterJson = "[]",
                title = null,
                outputJson = null,
                nutritionJson = null,
                error = null,
                createdAt = now,
                updatedAt = now
            )
        }
    }
}
