package com.ideacompost.app.domain

import com.ideacompost.app.data.ai.AiRouter
import com.ideacompost.app.data.db.dao.AgentDao
import com.ideacompost.app.data.db.dao.BedEventDao
import com.ideacompost.app.data.db.dao.CompostDao
import com.ideacompost.app.data.db.dao.IdeaDao
import com.ideacompost.app.data.db.entity.AgentEntity
import com.ideacompost.app.data.db.entity.BedEventEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln

/**
 * 菌床生态引擎——夜间任务（个人页手动触发）。
 *
 * 设计依据：02-Agent生态协议 §2-§8 + 借鉴 Anthropic Dreaming（异步审阅历史、只建议、人决定）
 * 与记忆治理四原则（生命周期管理、非对称淘汰、自动在前人工在后、版本化一切）。
 *
 * 自动执行（低风险，直接落库 + bed_events 记录前后状态）：
 *   活性重算 → 时间衰减 → 压缩候选 → 休眠 → 关键词唤醒
 * 待用户确认（高风险，生成建议卡片）：
 *   增殖（分化子菌） / 融合（共生复合菌）
 */
@Singleton
class EcoEngine @Inject constructor(
    private val agentDao: AgentDao,
    private val compostDao: CompostDao,
    private val ideaDao: IdeaDao,
    private val bedEventDao: BedEventDao,
    private val llm: AiRouter
) {

    data class EcoAction(
        val type: String,      // vitality|compressed|dormant|awakened
        val agentName: String,
        val detail: String
    )

    data class EcoSuggestion(
        val type: String,      // proliferation | fusion
        val title: String,
        val detail: String,
        val payload: JSONObject
    )

    data class EcoReport(
        val ranAt: Long,
        val actions: List<EcoAction>,
        val suggestions: List<EcoSuggestion>
    )

    companion object {
        const val TAU_DAYS = 30.0            // recency 半衰常数（02 协议 τ=30d）
        const val COMPRESS_V = 15.0          // 压缩线
        const val COMPRESS_DAYS = 90L        // 90 天无贡献 → 压缩候选
        const val DORMANT_DAYS = 180L        // 再 180 天 → 休眠
        const val PROLIF_MIN_PC = 5          // 增殖门槛：参与次数
        const val PROLIF_MIN_V = 60.0        // 增殖门槛：活性
        const val FUSE_MIN_CO = 4            // 融合门槛：共现次数
        const val ADOPT_CAP = 30.0           // 营养封顶（30 分营养 = 满 adoption）
        const val ECO_SYSTEM = """你是思想堆肥 App 的菌床生态管理员。用户的思想菌群是数据而非代码：它们会增殖（分化子菌）、融合（共生复合菌）、休眠与唤醒。你只提出建议，最终决定权在用户。严格输出所要求的 JSON，不要输出其他内容。"""
    }

    // ── 活性重算（02 协议五因子的可计算子集：Recency/Adoption/Frequency；Citation/Affinity 待 insight 引用链）──

    fun recomputeVitality(a: AgentEntity, now: Long): Double {
        val last = a.lastContributionAt ?: a.createdAt
        val days = ((now - last).coerceAtLeast(0)) / 86_400_000.0
        val recency = exp(-days / TAU_DAYS)
        val adoption = (a.nutritionBuffer / ADOPT_CAP).coerceIn(0.0, 1.0)
        val freq = (ln(1.0 + a.participationCount) / ln(21.0)).coerceIn(0.0, 1.0)
        return (100.0 * (0.45 * recency + 0.30 * adoption + 0.25 * freq)).coerceIn(0.0, 100.0)
    }

    private fun daysSince(ts: Long?, now: Long): Long =
        if (ts == null) Long.MAX_VALUE else (now - ts) / 86_400_000L

    // ── 夜间任务主入口 ──

    suspend fun runNightly(onProgress: (String) -> Unit): EcoReport = withContext(Dispatchers.Default) {
        android.util.Log.d("EcoEngine", "runNightly START")
        val now = System.currentTimeMillis()
        val actions = mutableListOf<EcoAction>()
        val suggestions = mutableListOf<EcoSuggestion>()

        onProgress("正在读取菌床全貌……")
        val agents = agentDao.all()
        android.util.Log.d("EcoEngine", "step2 agents=${agents.size}")
        val recentCrumbs = ideaDao.recentSince(now - 14L * 86_400_000L)
        android.util.Log.d("EcoEngine", "step3 crumbs=${recentCrumbs.size}")
        val crumbTexts = recentCrumbs.map { it.content }
        val doneComposts = compostDao.allDone()
        android.util.Log.d("EcoEngine", "step4 composts=${doneComposts.size}")

        // 1) 活性重算 + 2) 状态机（自动执行）
        onProgress("正在结算 ${agents.size} 位菌的活性……")
        for (a in agents) {
            val v = recomputeVitality(a, now)
            if (abs(v - a.vitality) >= 1.0) {
                agentDao.updateVitality(a.id, v, now)
            }
            val oldStatus = a.status
            var newStatus = oldStatus
            val d = daysSince(a.lastContributionAt, now)
            when {
                oldStatus == "active" && v < COMPRESS_V && d > COMPRESS_DAYS -> newStatus = "compressed"
                oldStatus == "compressed" && d > DORMANT_DAYS -> newStatus = "dormant"
                (oldStatus == "dormant" || oldStatus == "compressed") && awakenByKeywords(a, crumbTexts) -> newStatus = "active"
            }
            if (newStatus != oldStatus) {
                agentDao.updateStatus(a.id, newStatus, v, now)
                val type = when (newStatus) {
                    "compressed" -> "compressed"
                    "dormant" -> "dormant"
                    else -> "awakened"
                }
                val verb = when (newStatus) {
                    "compressed" -> "压缩为索引卡（活性 ${"%.0f".format(v)}，${d} 天无贡献）"
                    "dormant" -> "进入休眠（压缩卡保留，可随时唤醒）"
                    else -> "被最近的面包渣唤醒，恢复活跃（活性 ${"%.0f".format(v)}）"
                }
                actions += EcoAction(type, a.name, verb)
                bedEventDao.insert(
                    BedEventEntity(
                        ts = now, eventType = "agent_status_$type",
                        payload = JSONObject()
                            .put("agent_id", a.id).put("agent_name", a.name)
                            .put("before", oldStatus).put("after", newStatus)
                            .put("vitality", v).put("trigger", "nightly_task")
                            .toString()
                    )
                )
            }
        }

        // 3) 增殖建议（LLM 提名，用户确认）
        val candidates = agents.filter {
            it.status == "active" && it.participationCount >= PROLIF_MIN_PC && it.vitality >= PROLIF_MIN_V
        }
        for (parent in candidates.take(2)) {
            android.util.Log.d("EcoEngine", "step6 prolif parent=${parent.name}")
            onProgress("正在观察 ${parent.name} 是否分化……")
            suggestProliferation(parent, recentCrumbs, now)?.let { suggestions += it }
        }

        // 4) 融合建议（共现矩阵 + LLM 提名）
        onProgress("正在统计菌群共生关系……")
        val coOccur = coOccurrence(doneComposts)
        val activeNames = agents.filter { it.status == "active" }.associateBy { it.name }
        val fusePair = coOccur.entries
            .filter { activeNames.containsKey(it.key.first) && activeNames.containsKey(it.key.second) }
            .filter { it.value >= FUSE_MIN_CO }
            .maxByOrNull { it.value }
        if (fusePair != null) {
            onProgress("正在评估 ${fusePair.key.first} × ${fusePair.key.second} 是否共生……")
            suggestFusion(activeNames[fusePair.key.first]!!, activeNames[fusePair.key.second]!!, fusePair.value, now)
                ?.let { suggestions += it }
        }

        android.util.Log.d("EcoEngine", "step7 final write, suggestions=${suggestions.size}")
        bedEventDao.insert(
            BedEventEntity(
                ts = now, eventType = "eco_task_run",
                payload = JSONObject()
                    .put("agents_scanned", agents.size)
                    .put("auto_actions", actions.size)
                    .put("suggestions", suggestions.size)
                    .toString()
            )
        )
        EcoReport(ranAt = now, actions = actions, suggestions = suggestions)
    }

    // ── 唤醒：specialties/描述关键词命中近期面包渣 ≥2 ──

    private fun awakenByKeywords(a: AgentEntity, crumbs: List<String>): Boolean {
        if (crumbs.isEmpty()) return false
        val keys = runCatching {
            JSONArray(a.specialties).let { arr -> (0 until arr.length()).map { arr.getString(it) } }
        }.getOrDefault(emptyList()) + listOf(a.name.removeSuffix("菌"))
        var hits = 0
        for (crumb in crumbs) {
            if (keys.any { it.length >= 2 && crumb.contains(it) }) hits++
        }
        return hits >= 2
    }

    // ── 共现矩阵（rosterJson 的 agents[].name）──

    private fun coOccurrence(composts: List<com.ideacompost.app.data.db.entity.CompostEntity>):
            Map<Pair<String, String>, Int> {
        val counter = mutableMapOf<Pair<String, String>, Int>()
        for (c in composts) {
            val names = runCatching {
                JSONArray(c.rosterJson).let { arr ->
                    (0 until arr.length()).mapNotNull { arr.optJSONObject(it)?.optString("name") }
                }
            }.getOrDefault(emptyList()).distinct()
            for (i in names.indices) for (j in i + 1 until names.size) {
                val key = if (names[i] < names[j]) names[i] to names[j] else names[j] to names[i]
                counter[key] = (counter[key] ?: 0) + 1
            }
        }
        return counter
    }

    // ── 增殖建议 ──

    private suspend fun suggestProliferation(parent: AgentEntity, crumbs: List<com.ideacompost.app.data.db.entity.IdeaEntity>, now: Long): EcoSuggestion? {
        val topics = crumbs.take(10).map { it.content.take(24) }
        val user = """
            【父菌档案】${parent.name}（${parent.type}）：${parent.description}
            参与堆肥 ${parent.participationCount} 次，当前活性 ${"%.0f".format(parent.vitality)}，累计营养 ${"%.1f".format(parent.nutritionBuffer)}
            【用户近期碎片话题】${topics.joinToString("；").ifEmpty { "（暂无）" }}
            请为这株高产菌提出一个分化子菌：更窄、更专精的方向。输出 JSON：{"child_name":"xx菌","child_description":"...","rationale":"..."}
        """.trimIndent()
        val json = runCatching {
            JSONObject(llm.complete("eco_proliferate", ECO_SYSTEM, user))
        }.getOrElse { return fallbackProliferation(parent) }
        val childName = json.optString("child_name").ifBlank { return fallbackProliferation(parent) }
        return EcoSuggestion(
            type = "proliferation",
            title = "${parent.name} 建议分化出「${childName}」",
            detail = json.optString("rationale").ifBlank { "父菌高产且方向集中，值得分化专精子菌。" },
            payload = JSONObject()
                .put("parent_id", parent.id).put("parent_name", parent.name)
                .put("child_name", childName)
                .put("child_description", json.optString("child_description"))
        )
    }

    private fun fallbackProliferation(parent: AgentEntity): EcoSuggestion {
        val stem = parent.name.removeSuffix("菌")
        return EcoSuggestion(
            type = "proliferation",
            title = "${parent.name} 建议分化出「${stem}·专精菌」",
            detail = "父菌参与 ${parent.participationCount} 次、活性 ${"%.0f".format(parent.vitality)}，方向持续集中，可分化专精子菌（LLM 提名失败，使用确定性模板）。",
            payload = JSONObject()
                .put("parent_id", parent.id).put("parent_name", parent.name)
                .put("child_name", "${stem}·专精菌")
                .put("child_description", "从 ${parent.name} 分化出的子菌，聚焦其近期最常被调用的专精方向。")
        )
    }

    // ── 融合建议 ──

    private suspend fun suggestFusion(a: AgentEntity, b: AgentEntity, coCount: Int, now: Long): EcoSuggestion? {
        val user = """
            【菌 A】${a.name}（${a.type}）：${a.description}
            【菌 B】${b.name}（${b.type}）：${b.description}
            【共生证据】近期共同参与堆肥 $coCount 次
            请提出一个融合复合菌：继承两菌互补视角。输出 JSON：{"fused_name":"xx菌","fused_description":"...","rationale":"..."}
        """.trimIndent()
        val json = runCatching {
            JSONObject(llm.complete("eco_fuse", ECO_SYSTEM, user))
        }.getOrElse { return fallbackFusion(a, b, coCount) }
        val fusedName = json.optString("fused_name").ifBlank { return fallbackFusion(a, b, coCount) }
        return EcoSuggestion(
            type = "fusion",
            title = "${a.name} × ${b.name} 建议共生为「${fusedName}」",
            detail = "${json.optString("rationale").ifBlank { "两菌长期共同参与，视角互补。" }}（共现 $coCount 次）",
            payload = JSONObject()
                .put("agent_a_id", a.id).put("agent_a_name", a.name)
                .put("agent_b_id", b.id).put("agent_b_name", b.name)
                .put("fused_name", fusedName)
                .put("fused_description", json.optString("fused_description"))
        )
    }

    private fun fallbackFusion(a: AgentEntity, b: AgentEntity, coCount: Int): EcoSuggestion {
        val stemA = a.name.removeSuffix("菌"); val stemB = b.name.removeSuffix("菌")
        return EcoSuggestion(
            type = "fusion",
            title = "${a.name} × ${b.name} 建议共生为「${stemA}${stemB}复合菌」",
            detail = "两菌共现 $coCount 次，视角互补（LLM 提名失败，使用确定性模板）。",
            payload = JSONObject()
                .put("agent_a_id", a.id).put("agent_a_name", a.name)
                .put("agent_b_id", b.id).put("agent_b_name", b.name)
                .put("fused_name", "${stemA}${stemB}复合菌")
                .put("fused_description", "由 ${a.name} 与 ${b.name} 共生而成的复合菌。")
        )
    }

    // ── 用户决定：采纳 / 忽略 ──

    suspend fun applySuggestion(s: EcoSuggestion) {
        val now = System.currentTimeMillis()
        when (s.type) {
            "proliferation" -> {
                val parentId = s.payload.optString("parent_id")
                val parent = agentDao.byId(parentId)
                val child = AgentEntity(
                    id = "eco_${UUID.randomUUID().toString().take(8)}",
                    type = parent?.type ?: "domain",
                    name = s.payload.optString("child_name"),
                    description = s.payload.optString("child_description"),
                    capabilityCard = "你是${s.payload.optString("child_name")}。${s.payload.optString("child_description")}\n保持父菌 ${parent?.name ?: ""} 的思考底色，但只在你的专精方向上发力。",
                    status = "embryo",
                    vitality = 45.0,
                    parentId = parentId,
                    specialties = parent?.specialties ?: "[]",
                    fullProfile = "分化自 ${parent?.name ?: parentId}。${s.payload.optString("child_description")}",
                    createdAt = now, updatedAt = now
                )
                agentDao.insertOne(child)
                bedEventDao.insert(
                    BedEventEntity(
                        ts = now, eventType = "agent_proliferated",
                        payload = s.payload.put("child_id", child.id).put("confirmed_by", "user").toString()
                    )
                )
            }
            "fusion" -> {
                val aId = s.payload.optString("agent_a_id")
                val bId = s.payload.optString("agent_b_id")
                val a = agentDao.byId(aId); val b = agentDao.byId(bId)
                val fused = AgentEntity(
                    id = "eco_${UUID.randomUUID().toString().take(8)}",
                    type = "creative",
                    name = s.payload.optString("fused_name"),
                    description = s.payload.optString("fused_description"),
                    capabilityCard = "你是${s.payload.optString("fused_name")}。${s.payload.optString("fused_description")}\n融合了 ${a?.name ?: ""} 与 ${b?.name ?: ""} 的互补视角。",
                    status = "embryo",
                    vitality = ((a?.vitality ?: 0.0) + (b?.vitality ?: 0.0)) / 2,
                    nutritionBuffer = (a?.nutritionBuffer ?: 0.0) + (b?.nutritionBuffer ?: 0.0),
                    participationCount = (a?.participationCount ?: 0) + (b?.participationCount ?: 0),
                    fusionOf = listOfNotNull(aId, bId).joinToString(","),
                    specialties = mergeSpecialties(a?.specialties, b?.specialties),
                    fullProfile = "共生自 ${a?.name ?: aId} × ${b?.name ?: bId}。${s.payload.optString("fused_description")}",
                    createdAt = now, updatedAt = now
                )
                agentDao.insertOne(fused)
                if (a != null) agentDao.updateStatus(a.id, "fused", a.vitality, now)
                if (b != null) agentDao.updateStatus(b.id, "fused", b.vitality, now)
                bedEventDao.insert(
                    BedEventEntity(
                        ts = now, eventType = "agent_fused",
                        payload = s.payload.put("fused_id", fused.id).put("confirmed_by", "user").toString()
                    )
                )
            }
        }
    }

    private fun mergeSpecialties(s1: String?, s2: String?): String {
        val arr = JSONArray()
        val seen = mutableSetOf<String>()
        for (src in listOf(s1, s2)) {
            runCatching {
                val ja = JSONArray(src ?: "[]")
                for (i in 0 until ja.length()) {
                    val t = ja.getString(i)
                    if (seen.add(t)) arr.put(t)
                }
            }
        }
        return arr.toString()
    }

    suspend fun dismissSuggestion(s: EcoSuggestion) {
        bedEventDao.insert(
            BedEventEntity(
                ts = System.currentTimeMillis(),
                eventType = "eco_suggestion_dismissed",
                payload = s.payload.put("type", s.type).toString()
            )
        )
    }

}
