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

/** LLM 调用接口（specs/02 §2）：system+user → 文本。stageKey 供审计与测试定位。 */
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

    override suspend fun complete(stageKey: String, system: String, user: String): String {
        val cfg = providerStore.config()
        val t0 = android.os.SystemClock.uptimeMillis()
        try {
            val r = withContext(Dispatchers.IO) {
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
                    provider = cfg.model,
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
                    provider = cfg.model,
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

    /** 连通性测试（设置页「测试连接」）：最小请求，成功返回模型回复原文，失败抛异常。 */
    suspend fun testConnection(): String = withContext(Dispatchers.IO) {
        val t0 = android.os.SystemClock.uptimeMillis()
        try {
            val reply = call(providerStore.config(), "你是一个连通性测试器。", "请只回复两个字：成功")
            telemetry.insert(
                com.ideacompost.app.data.db.entity.LlmCallEntity(
                    ts = System.currentTimeMillis(), stageKey = "test_connection",
                    provider = providerStore.config().model,
                    status = "ok", promptChars = 20, responseChars = reply.length,
                    latencyMs = android.os.SystemClock.uptimeMillis() - t0
                )
            )
            reply
        } catch (e: Exception) {
            telemetry.insert(
                com.ideacompost.app.data.db.entity.LlmCallEntity(
                    ts = System.currentTimeMillis(), stageKey = "test_connection",
                    provider = providerStore.config().model,
                    status = "error", promptChars = 20, responseChars = 0,
                    latencyMs = android.os.SystemClock.uptimeMillis() - t0,
                    error = e.message?.take(280)
                )
            )
            throw e
        }
    }
}
