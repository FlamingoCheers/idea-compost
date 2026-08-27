package com.ideacompost.app.data.ai

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** LLM 调用接口（specs/02 §2）：system+user → 文本。stageKey 供 Mock 与审计。 */
interface LLMClient {
    suspend fun complete(stageKey: String, system: String, user: String): String
}

@Singleton
class AiRouter @Inject constructor(
    private val providerStore: ProviderStore,
    private val telemetry: com.ideacompost.app.data.db.dao.LlmCallDao
) : LLMClient {

    private val http = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .build()

    private val mock = MockLLM()

    override suspend fun complete(stageKey: String, system: String, user: String): String {
        val cfg = providerStore.config()
        val t0 = android.os.SystemClock.uptimeMillis()
        try {
            val r = if (cfg.mockMode) mock.complete(stageKey, system, user) else withContext(Dispatchers.IO) {
                var last: IOException? = null
                repeat(3) { attempt ->
                    try {
                        return@withContext call(cfg, system, user)
                    } catch (e: IOException) {
                        last = e
                        // 网关限流/瞬断退避：2s / 6s（specs/02 §3）
                        if (attempt < 2) delay(if (attempt == 0) 2000L else 6000L)
                    }
                }
                throw last ?: IOException("llm call failed")
            }
            telemetry.insert(
                com.ideacompost.app.data.db.entity.LlmCallEntity(
                    ts = System.currentTimeMillis(), stageKey = stageKey,
                    provider = if (cfg.mockMode) "mock" else cfg.model,
                    status = "ok", promptChars = system.length + user.length,
                    responseChars = r.length, latencyMs = android.os.SystemClock.uptimeMillis() - t0
                )
            )
            return r
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            telemetry.insert(
                com.ideacompost.app.data.db.entity.LlmCallEntity(
                    ts = System.currentTimeMillis(), stageKey = stageKey,
                    provider = if (cfg.mockMode) "mock" else cfg.model,
                    status = "error", promptChars = system.length + user.length,
                    responseChars = 0, latencyMs = android.os.SystemClock.uptimeMillis() - t0,
                    error = e.message?.take(280)
                )
            )
            throw e
        }
    }

    private fun call(cfg: ProviderStore.Config, system: String, user: String): String {
        val messages = JSONArray().apply {
            put(JSONObject().put("role", "system").put("content", system))
            put(JSONObject().put("role", "user").put("content", user))
        }
        val body = JSONObject()
            .put("model", cfg.model)
            .put("messages", messages)
            .put("temperature", 0.8)
            .toString()
            .toRequestBody("application/json; charset=utf-8".toMediaType())
        val url = cfg.baseUrl.trimEnd('/') + "/chat/completions"
        val req = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer ${cfg.apiKey}")
            .post(body)
            .build()
        http.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("HTTP ${resp.code}: ${resp.body?.string()?.take(300)}")
            val txt = resp.body?.string() ?: throw IOException("empty body")
            return JSONObject(txt)
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
        }
    }
}

/** 演示模式：无 Key 也能走通五阶段全流程（管线/动画/落库全部真实运转，产物为本地生成）。 */
class MockLLM : LLMClient {

    private suspend fun think() {
        delay((900..2300L).random())
    }

    override suspend fun complete(stageKey: String, system: String, user: String): String {
        android.util.Log.d("MockLLM", "complete($stageKey) begin, len=${user.length}")
        val t0 = android.os.SystemClock.uptimeMillis()
        think()
        val gist = firstLine(user)
        val agent = agentName(user)
        val r = when (stageKey) {
            "identify" -> identify(gist)
            "r1" -> round1(agent, gist)
            "r2" -> round2(agent, gist)
            "r3" -> round3(agent, gist)
            "integrate" -> integrate(gist)
            "assess" -> assess()
            else -> "{}"
        }
        android.util.Log.d("MockLLM", "complete($stageKey) end in ${android.os.SystemClock.uptimeMillis() - t0}ms")
        return r
    }

    private fun firstLine(user: String): String {
        val crumbs = Regex("【面包渣】([\\s\\S]*?)【").find(user)?.groupValues?.get(1)
            ?: user.replace(Regex("\\s+"), " ").take(50)
        return crumbs.replace(Regex("\\s+"), " ").trim().take(60).ifEmpty { "这批碎片" }
    }

    private fun agentName(user: String): String =
        Regex("参与菌群之一，(.+?)。").find(user)?.groupValues?.get(1) ?: "概念辨析菌"

    private fun identify(gist: String) = """
        {"gist":"这批碎片共同指向：${gist.take(40)}背后的机制与前提",
         "potential_domains":["技术","心理学","社会学"],
         "potential_methods":["概念辨析","反驳","反事实"],
         "tensions":["碎片①与碎片②在『谁承担代价』上存在未言明的张力","『直觉有用』与『直觉可疑』同时成立"],
         "premises":["碎片①隐含假设了技术影响是单向的，未被检验","讨论默认『个人经验』可以代表普遍状况"],
         "bed_links":[],
         "suggest_depth":"standard"}
    """.trimIndent()

    private fun round1(agent: String, gist: String) = """
        {"agent":"$agent",
         "core_questions":["${gist.take(30)}的真正机制是什么？","谁在承担代价，谁在获得收益？"],
         "key_concepts":["机制","代价分配","隐含前提"],
         "implicit_premises":["默认了技术/制度影响是单向的","默认个人经验可以推广为普遍规律"],
         "claims":[
            {"id":"A1","text":"『${gist.take(20)}』至少能拆成机制与规范两个独立问题","confidence":"supported"},
            {"id":"A2","text":"碎片之间存在未被言明的共同前提","confidence":"emerging"}],
         "citations":["idea:0"]}
    """.trimIndent()

    private fun round2(agent: String, gist: String) = """
        {"agent":"$agent",
         "connection_candidates":[{"text":"『${gist.take(18)}』与『被安排感』共享『中心化操纵者』这个隐藏前提","source_claims":["A1"]}],
         "conflicts":[{"a":"应夺回选择权（行动派）","b":"概念本身懒惰，无需夺回（釜底抽薪派）","nature":"价值层面"}],
         "recombinations":["把『机制』与『感受』拆开后，两条线索可以分别检验"],
         "claims":[
            {"id":"B1","text":"反方最强论证：即使机制存在，它的实际影响也可能远小于叙事的影响","confidence":"supported"},
            {"id":"B2","text":"要守住原判断，需要补上『实际行为数据』而非『感觉证据』","confidence":"supported"}],
         "citations":["claim:A1"]}
    """.trimIndent()

    private fun round3(agent: String, gist: String) = """
        {"agent":"$agent",
         "new_hypotheses":[
            {"text":"H1：焦虑源于『开放循环』而非数量本身","falsifier":"若关闭线索后焦虑不随库存可见性变化，H1 死","confidence":"emerging"},
            {"text":"H2：『${gist.take(16)}』的影响主要通过叙事而非机制发生","falsifier":"若控制叙事暴露后行为差异仍显著，H2 死","confidence":"emerging"}],
         "new_concepts":[{"term":"备胎知识","definition":"以安抚而非使用为目的的储备","boundary_case":"低频但真用的急救手册"}],
         "research_questions":["机制组与叙事组的行为差异是否可测？"],
         "claims":[{"id":"D1","text":"新假说 H1/H2 均可检验，且互相独立","confidence":"emerging"}],
         "citations":["claim:B1"]}
    """.trimIndent()

    private fun integrate(gist: String) = """
        {"title":"（初步）${gist.take(24)}：机制还是叙事",
         "core_ideas":[
            {"text":"『${gist.take(20)}』可以拆成机制与叙事两个独立问题，分别检验","confidence":"emerging","source_claims":["A1"]},
            {"text":"反方的存在提示：叙事的影响可能大于机制本身","confidence":"emerging","source_claims":["B1"]}],
         "fragment_links":[
            {"from_idea":0,"to_idea":1,"relation":"碎片①的隐含前提正是碎片②在质疑的东西","source_claims":["A2"]}],
         "forming_judgments":[
            {"text":"与其争论概念对错，不如把两个机制拆开各自找证据","confidence":"emerging","source_claims":["B2"]}],
         "new_connections":[],
         "conflicts":[
            {"a":"应夺回选择权（行动派）","b":"概念本身懒惰，无需夺回（釜底抽薪派）","nature":"价值层面","resolution_hint":"可以并存：个人层面夺回，分析层面拆解"}],
         "strongest_objection":{
            "text":"你可能在用『机制』的复杂性回避更简单的『叙事』解释——简单的解释未必是错的。要推翻它，需要行为数据，不是更多论证。",
            "by":"反驳菌","source_claims":["B1","B2"]},
         "open_questions":["机制组与叙事组的行为差异是否可测？","个人经验能推广到什么范围？"],
         "future_directions":[
            {"text":"为 H1 设计一个最小检验","as_crumbs":"我要为『焦虑源于开放循环』设计一个最小实验"},
            {"text":"找两个机制的既有证据","as_crumbs":"帮我想想：叙事影响 vs 机制影响，有哪些现成研究"}],
         "agents_used":[
            {"agent":"技术菌","weight":0.82,"quota_role":"core","key_contribution":"把问题拆成机制/界面两层"},
            {"agent":"概念辨析菌","weight":0.95,"quota_role":"core","key_contribution":"拆出『机制 vs 规范』的分叉"},
            {"agent":"反驳菌","weight":1.0,"quota_role":"core","key_contribution":"构建叙事解释的反方"},
            {"agent":"历史菌","weight":0.31,"quota_role":"wildcard","key_contribution":"提供了『先例搬家』的历史视角"},
            {"agent":"假说菌","weight":0.7,"quota_role":"core","key_contribution":"产出两个可检验假说"}],
         "source_crumbs":[0,1]}
    """.trimIndent()

    private fun assess() = """
        {"nutrition_awarded":[
            {"agent":"概念辨析菌","score":5.0,"evidence":["core_ideas[0] ← 概念拆解"]},
            {"agent":"反驳菌","score":6.0,"evidence":["strongest_objection ← 反驳菌构建"]},
            {"agent":"历史菌","score":3.0,"evidence":["forming_judgments[0] ← 历史先例"]},
            {"agent":"技术菌","score":4.0,"evidence":["fragment_links[0] ← 技术视角"]},
            {"agent":"假说菌","score":3.5,"evidence":["future_directions ← 新假说"]}],
         "ecology_note":"意外菌（历史菌）贡献了关键的历史先例视角——异质性配额生效"}
    """.trimIndent()
}
