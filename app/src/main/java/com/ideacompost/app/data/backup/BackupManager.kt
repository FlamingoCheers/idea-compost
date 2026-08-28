package com.ideacompost.app.data.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.ideacompost.app.data.ProfileStore
import com.ideacompost.app.data.ai.ProviderStore
import com.ideacompost.app.data.db.IdeaCompostDatabase
import com.ideacompost.app.data.db.entity.AgentEntity
import com.ideacompost.app.data.db.entity.BedEventEntity
import com.ideacompost.app.data.db.entity.CompostEntity
import com.ideacompost.app.data.db.entity.CompostStageEntity
import com.ideacompost.app.data.db.entity.FeedbackEventEntity
import com.ideacompost.app.data.db.entity.IdeaEntity
import com.ideacompost.app.data.db.entity.LlmCallEntity
import com.ideacompost.app.data.db.entity.ProbioticEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 全量备份/恢复（specs/40）：一个 zip 带走整个认知生态。
 *
 * 包内结构：
 *   manifest.json   格式版本、App 标识、导出时间、DB 版本
 *   profile.json    昵称、头像
 *   provider.json   AI 服务商配置（含 Key——备份请妥善保管）
 *   ideas.json / composts.json / stages.json / feedbacks.json /
 *   agents.json / probiotics.json / bed_events.json / llm_calls.json
 *
 * 导入 = 单事务内清库重建（DB version 不变，见 BUG-007 约束）。
 */
@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: IdeaCompostDatabase,
    private val profile: ProfileStore,
    private val providerStore: ProviderStore,
) {

    companion object {
        const val FORMAT_VERSION = 1
        val TABLES = listOf(
            "ideas", "composts", "stages", "feedbacks",
            "agents", "probiotics", "bed_events", "llm_calls"
        )
    }

    data class BackupStats(
        val ideas: Int,
        val composts: Int,
        val agents: Int,
        val probiotics: Int,
        val bedEvents: Int
    )

    /* ---------------- 导出 ---------------- */

    suspend fun exportTo(uri: Uri): BackupStats = withContext(Dispatchers.IO) {
        val ideas = db.ideaDao().all()
        val composts = db.compostDao().all()
        val stages = db.compostDao().allStages()
        val feedbacks = db.compostDao().allFeedbacks()
        val agents = db.agentDao().all()
        val probiotics = db.probioticDao().allIncludingHidden()
        val bedEvents = db.bedEventDao().all()
        val calls = db.llmCallDao().all()
        val stats = BackupStats(ideas.size, composts.size, agents.size, probiotics.size, bedEvents.size)

        val manifest = JSONObject()
            .put("format_version", FORMAT_VERSION)
            .put("app", "ideacompost")
            .put("exported_at", System.currentTimeMillis())
            .put("db_version", db.openHelper.writableDatabase.version)

        val profileJson = JSONObject()
            .put("nickname", profile.nickname)
            .put("avatar_emoji", profile.avatarEmoji)

        val providerJson = JSONObject()
            .put("base_url", providerStore.baseUrl)
            .put("api_key", providerStore.apiKey)
            .put("model", providerStore.model)

        context.contentResolver.openOutputStream(uri)?.use { out ->
            fun arr(items: List<JSONObject>): JSONArray = JSONArray().apply { items.forEach { put(it) } }
            writeZip(out, mapOf(
                "manifest.json" to manifest,
                "profile.json" to profileJson,
                "provider.json" to providerJson,
                "ideas.json" to arr(ideas.map { ideaJson(it) }),
                "composts.json" to arr(composts.map { compostJson(it) }),
                "stages.json" to arr(stages.map { stageJson(it) }),
                "feedbacks.json" to arr(feedbacks.map { feedbackJson(it) }),
                "agents.json" to arr(agents.map { agentJson(it) }),
                "probiotics.json" to arr(probiotics.map { probioticJson(it) }),
                "bed_events.json" to arr(bedEvents.map { bedEventJson(it) }),
                "llm_calls.json" to arr(calls.map { callJson(it) }),
            ))
        } ?: throw IllegalStateException("无法写入导出文件")

        stats
    }

    private fun writeZip(out: OutputStream, files: Map<String, Any>) {
        ZipOutputStream(out.buffered()).use { zip ->
            files.forEach { (name, content) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(content.toString().toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
        }
    }

    /* ---------------- 导入 ---------------- */

    /** 返回恢复统计。任何校验失败抛 IllegalStateException（消息可直接展示）。 */
    suspend fun importFrom(uri: Uri): BackupStats = withContext(Dispatchers.IO) {
        val files = readZip(context.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("无法读取备份文件"))

        val manifest = files["manifest.json"]?.let { JSONObject(it) }
            ?: throw IllegalStateException("不是有效的思想堆肥备份（缺少 manifest.json）")
        if (manifest.optString("app") != "ideacompost")
            throw IllegalStateException("不是思想堆肥的备份文件")
        val fmt = manifest.optInt("format_version", 0)
        if (fmt > FORMAT_VERSION)
            throw IllegalStateException("备份来自更新版本（格式 v$fmt），请先升级 App")

        val ideas = parseArray(files["ideas.json"]) { ideaFrom(it) }
        val composts = parseArray(files["composts.json"]) { compostFrom(it) }
        val stages = parseArray(files["stages.json"]) { stageFrom(it) }
        val feedbacks = parseArray(files["feedbacks.json"]) { feedbackFrom(it) }
        val agents = parseArray(files["agents.json"]) { agentFrom(it) }
        val probiotics = parseArray(files["probiotics.json"]) { probioticFrom(it) }
        val bedEvents = parseArray(files["bed_events.json"]) { bedEventFrom(it) }
        val calls = parseArray(files["llm_calls.json"]) { callFrom(it) }

        JSONObject(files["profile.json"] ?: "{}").let { p ->
            if (p.has("nickname")) profile.nickname = p.optString("nickname")
            if (p.has("avatar_emoji")) profile.avatarEmoji = p.optString("avatar_emoji")
        }
        JSONObject(files["provider.json"] ?: "{}").let { p ->
            if (p.has("base_url")) providerStore.baseUrl = p.optString("base_url")
            if (p.has("api_key")) providerStore.apiKey = p.optString("api_key")
            if (p.has("model")) providerStore.model = p.optString("model")
        }

        db.withTransaction {
            db.ideaDao().deleteAll()
            db.compostDao().deleteAllStages()
            db.compostDao().deleteAllFeedbacks()
            db.compostDao().deleteAllComposts()
            db.agentDao().deleteAll()
            db.probioticDao().deleteAll()
            db.bedEventDao().deleteAll()
            db.llmCallDao().deleteAll()

            db.ideaDao().insertAll(ideas)
            db.compostDao().insertAllComposts(composts)
            db.compostDao().insertAllStages(stages)
            db.compostDao().insertAllFeedbacks(feedbacks)
            db.agentDao().insertAll(agents)
            db.probioticDao().insertAll(probiotics)
            db.bedEventDao().insertAll(bedEvents)
            db.llmCallDao().insertAll(calls)
        }
        BackupStats(ideas.size, composts.size, agents.size, probiotics.size, bedEvents.size)
    }

    private fun readZip(input: InputStream): Map<String, String> {
        val files = mutableMapOf<String, String>()
        ZipInputStream(input.buffered()).use { zip ->
            var entry: ZipEntry? = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    files[entry.name] = zip.readBytes().toString(Charsets.UTF_8)
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        return files
    }

    private inline fun <T> parseArray(name: String?, parse: (JSONObject) -> T): List<T> {
        if (name == null) return emptyList()
        val arr = JSONArray(name)
        return (0 until arr.length()).map { parse(arr.getJSONObject(it)) }
    }

    /* ---------------- JSON 映射 ---------------- */

    private fun ideaJson(e: IdeaEntity) = JSONObject()
        .put("id", e.id).put("content", e.content).put("content_type", e.contentType)
        .put("title", e.title ?: JSONObject.NULL).put("source", e.source)
        .put("status", e.status).put("metadata", e.metadata)
        .put("created_at", e.createdAt).put("updated_at", e.updatedAt)

    private fun ideaFrom(j: JSONObject) = IdeaEntity(
        id = j.getString("id"), content = j.getString("content"),
        contentType = j.optString("content_type", "text"),
        title = j.optStringOrNull("title"), source = j.optString("source", "manual"),
        status = j.optString("status", "raw"), metadata = j.optString("metadata", "{}"),
        createdAt = j.getLong("created_at"), updatedAt = j.getLong("updated_at")
    )

    private fun compostJson(e: CompostEntity) = JSONObject()
        .put("id", e.id).put("status", e.status).put("depth", e.depth)
        .put("input_idea_ids", e.inputIdeaIds).put("probiotic_ids", e.probioticIds)
        .put("current_stage", e.currentStage).put("roster_json", e.rosterJson)
        .put("title", e.title ?: JSONObject.NULL).put("output_json", e.outputJson ?: JSONObject.NULL)
        .put("nutrition_json", e.nutritionJson ?: JSONObject.NULL).put("error", e.error ?: JSONObject.NULL)
        .put("created_at", e.createdAt).put("updated_at", e.updatedAt)

    private fun compostFrom(j: JSONObject) = CompostEntity(
        id = j.getString("id"), status = j.getString("status"), depth = j.getString("depth"),
        inputIdeaIds = j.getString("input_idea_ids"), probioticIds = j.getString("probiotic_ids"),
        currentStage = j.getString("current_stage"), rosterJson = j.getString("roster_json"),
        title = j.optStringOrNull("title"), outputJson = j.optStringOrNull("output_json"),
        nutritionJson = j.optStringOrNull("nutrition_json"), error = j.optStringOrNull("error"),
        createdAt = j.getLong("created_at"), updatedAt = j.getLong("updated_at")
    )

    private fun stageJson(e: CompostStageEntity) = JSONObject()
        .put("compost_id", e.compostId).put("stage_key", e.stageKey)
        .put("payload", e.payload).put("created_at", e.createdAt)

    private fun stageFrom(j: JSONObject) = CompostStageEntity(
        compostId = j.getString("compost_id"), stageKey = j.getString("stage_key"),
        payload = j.getString("payload"), createdAt = j.getLong("created_at")
    )

    private fun feedbackJson(e: FeedbackEventEntity) = JSONObject()
        .put("compost_id", e.compostId).put("kind", e.kind).put("created_at", e.createdAt)

    private fun feedbackFrom(j: JSONObject) = FeedbackEventEntity(
        compostId = j.getString("compost_id"), kind = j.getString("kind"),
        createdAt = j.getLong("created_at")
    )

    private fun agentJson(e: AgentEntity) = JSONObject()
        .put("id", e.id).put("type", e.type).put("name", e.name)
        .put("description", e.description).put("capability_card", e.capabilityCard)
        .put("card_version", e.cardVersion).put("specialties", e.specialties)
        .put("status", e.status).put("vitality", e.vitality)
        .put("nutrition_buffer", e.nutritionBuffer)
        .put("parent_id", e.parentId ?: JSONObject.NULL)
        .put("fusion_of", e.fusionOf ?: JSONObject.NULL)
        .put("compressed_memory", e.compressedMemory ?: JSONObject.NULL)
        .put("full_profile", e.fullProfile)
        .put("participation_count", e.participationCount)
        .put("last_contribution_at", e.lastContributionAt ?: JSONObject.NULL)
        .put("created_at", e.createdAt).put("updated_at", e.updatedAt)

    private fun agentFrom(j: JSONObject) = AgentEntity(
        id = j.getString("id"), type = j.getString("type"), name = j.getString("name"),
        description = j.getString("description"), capabilityCard = j.getString("capability_card"),
        cardVersion = j.optInt("card_version", 1), specialties = j.optString("specialties", "[]"),
        status = j.optString("status", "active"), vitality = j.optDouble("vitality", 40.0),
        nutritionBuffer = j.optDouble("nutrition_buffer", 0.0),
        parentId = j.optStringOrNull("parent_id"), fusionOf = j.optStringOrNull("fusion_of"),
        compressedMemory = j.optStringOrNull("compressed_memory"),
        fullProfile = j.optString("full_profile", ""),
        participationCount = j.optInt("participation_count", 0),
        lastContributionAt = j.optLongOrNull("last_contribution_at"),
        createdAt = j.getLong("created_at"), updatedAt = j.getLong("updated_at")
    )

    private fun probioticJson(e: ProbioticEntity) = JSONObject()
        .put("id", e.id).put("name", e.name).put("icon", e.icon ?: JSONObject.NULL)
        .put("description", e.description).put("prompt_logic", e.promptLogic)
        .put("target_types", e.targetTypes).put("domain_boosts", e.domainBoosts)
        .put("stage_emphasis", e.stageEmphasis).put("diversity_shift", e.diversityShift ?: JSONObject.NULL)
        .put("scope", e.scope).put("hidden", e.hidden).put("usage_count", e.usageCount)
        .put("last_used", e.lastUsed ?: JSONObject.NULL)
        .put("birth_context", e.birthContext ?: JSONObject.NULL)
        .put("created_at", e.createdAt).put("updated_at", e.updatedAt)

    private fun probioticFrom(j: JSONObject) = ProbioticEntity(
        id = j.getString("id"), name = j.getString("name"),
        icon = j.optStringOrNull("icon"),
        description = j.getString("description"), promptLogic = j.getString("prompt_logic"),
        targetTypes = j.optString("target_types", "[]"),
        domainBoosts = j.optString("domain_boosts", "{}"),
        stageEmphasis = j.optString("stage_emphasis", "{}"),
        diversityShift = j.optStringOrNull("diversity_shift"),
        scope = j.optString("scope", "user_defined"),
        hidden = j.optBoolean("hidden", false),
        usageCount = j.optInt("usage_count", 0),
        lastUsed = j.optLongOrNull("last_used"),
        birthContext = j.optStringOrNull("birth_context"),
        createdAt = j.getLong("created_at"), updatedAt = j.getLong("updated_at")
    )

    private fun bedEventJson(e: BedEventEntity) = JSONObject()
        .put("ts", e.ts).put("event_type", e.eventType)
        .put("payload", e.payload).put("compost_id", e.compostId ?: JSONObject.NULL)

    private fun bedEventFrom(j: JSONObject) = BedEventEntity(
        ts = j.getLong("ts"), eventType = j.getString("event_type"),
        payload = j.optString("payload", "{}"), compostId = j.optStringOrNull("compost_id")
    )

    private fun callJson(e: LlmCallEntity) = JSONObject()
        .put("ts", e.ts).put("stage_key", e.stageKey).put("provider", e.provider)
        .put("status", e.status).put("prompt_chars", e.promptChars)
        .put("response_chars", e.responseChars).put("latency_ms", e.latencyMs)
        .put("error", e.error ?: JSONObject.NULL)

    private fun callFrom(j: JSONObject) = LlmCallEntity(
        ts = j.getLong("ts"), stageKey = j.getString("stage_key"),
        provider = j.optString("provider", ""), status = j.getString("status"),
        promptChars = j.optInt("prompt_chars", 0), responseChars = j.optInt("response_chars", 0),
        latencyMs = j.optLong("latency_ms", 0), error = j.optStringOrNull("error")
    )

    private fun JSONObject.optStringOrNull(key: String): String? =
        if (isNull(key) || !has(key)) null else optString(key)

    private fun JSONObject.optLongOrNull(key: String): Long? =
        if (isNull(key) || !has(key)) null else optLong(key)
}
