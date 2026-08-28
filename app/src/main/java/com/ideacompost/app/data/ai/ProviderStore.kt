package com.ideacompost.app.data.ai

import android.content.SharedPreferences
import javax.inject.Inject
import javax.inject.Singleton

/** BYO Key（08 §5）：仅存本地，仅用于直连你的服务商。 */
@Singleton
class ProviderStore @Inject constructor(private val prefs: SharedPreferences) {

    data class Config(
        val baseUrl: String,
        val apiKey: String,
        val model: String
    )

    var baseUrl: String
        get() = prefs.getString("provider_base_url", "") ?: ""
        set(v) = prefs.edit().putString("provider_base_url", v).apply()

    var apiKey: String
        get() = prefs.getString("provider_api_key", "") ?: ""
        set(v) = prefs.edit().putString("provider_api_key", v).apply()

    var model: String
        get() = prefs.getString("provider_model", "") ?: ""
        set(v) = prefs.edit().putString("provider_model", v).apply()

    fun config(): Config = Config(baseUrl, apiKey, model)

    /** 堆肥必须配置真实 AI 服务商（v0.2 发布版：演示模式已移除）。 */
    fun ready(): Boolean = baseUrl.isNotBlank() && apiKey.isNotBlank() && model.isNotBlank()
}
