package com.ideacompost.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.net.Uri
import com.ideacompost.app.data.ProfileStore
import com.ideacompost.app.data.ai.AiRouter
import com.ideacompost.app.data.ai.ProviderStore
import com.ideacompost.app.data.backup.BackupManager
import com.ideacompost.app.data.db.dao.AgentDao
import com.ideacompost.app.data.db.dao.LlmCallDao
import com.ideacompost.app.data.db.entity.AgentEntity
import com.ideacompost.app.data.db.entity.LlmCallEntity
import com.ideacompost.app.domain.EcoEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val store: ProviderStore,
    private val ai: AiRouter,
    private val profile: ProfileStore,
    private val eco: EcoEngine,
    private val backup: BackupManager,
    agentDao: AgentDao,
    llmCallDao: LlmCallDao,
) : ViewModel() {

    data class UiState(
        val nickname: String = "园丁",
        val avatarEmoji: String = "🌱",
        val agents: List<AgentEntity> = emptyList(),
        val ecoRunning: Boolean = false,
        val ecoProgress: String = "",
        val ecoReport: EcoEngine.EcoReport? = null,
        val ecoSuggestions: List<EcoEngine.EcoSuggestion> = emptyList(),
        val baseUrl: String = "",
        val apiKey: String = "",
        val model: String = "",
        val saved: Boolean = false,
        val testing: Boolean = false,
        val backupBusy: Boolean = false,
    )

    private val _state = MutableStateFlow(
        UiState(baseUrl = store.baseUrl, apiKey = store.apiKey, model = store.model)
    )
    val state: StateFlow<UiState> = _state

    init {
        viewModelScope.launch {
            _state.value = _state.value.copy(nickname = profile.nickname, avatarEmoji = profile.avatarEmoji)
        }
        viewModelScope.launch {
            agentDao.observeEvery().collect { list ->
                _state.value = _state.value.copy(agents = list)
            }
        }
    }

    /** 调用遥测（llm_calls）：最近 8 条。 */
    val recentCalls: StateFlow<List<LlmCallEntity>> =
        llmCallDao.observeRecent(8).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /* ------- 个人区块 ------- */

    fun cycleAvatar() {
        val i = (AVATARS.indexOf(_state.value.avatarEmoji) + 1) % AVATARS.size
        val e = AVATARS[i]
        profile.avatarEmoji = e
        _state.value = _state.value.copy(avatarEmoji = e)
    }

    fun updateNickname(n: String) {
        profile.nickname = n
        _state.value = _state.value.copy(nickname = n)
    }

    /* ------- 夜间生态任务 ------- */

    fun runNightlyTask() {
        if (_state.value.ecoRunning) return
        _state.value = _state.value.copy(ecoRunning = true, ecoProgress = "开始", ecoReport = null, ecoSuggestions = emptyList())
        viewModelScope.launch {
            try {
                val report = eco.runNightly { p ->
                    _state.value = _state.value.copy(ecoProgress = p)
                }
                _state.value = _state.value.copy(
                    ecoRunning = false, ecoProgress = "",
                    ecoReport = report, ecoSuggestions = report.suggestions,
                )
            } catch (t: Throwable) {
                android.util.Log.e("EcoEngine", "runNightly failed", t)
                _state.value = _state.value.copy(ecoRunning = false, ecoProgress = "")
            }
        }
    }

    fun applySuggestion(s: EcoEngine.EcoSuggestion) {
        viewModelScope.launch {
            eco.applySuggestion(s)
            _state.value = _state.value.copy(ecoSuggestions = _state.value.ecoSuggestions - s)
        }
    }

    fun dismissSuggestion(s: EcoEngine.EcoSuggestion) {
        viewModelScope.launch {
            eco.dismissSuggestion(s)
            _state.value = _state.value.copy(ecoSuggestions = _state.value.ecoSuggestions - s)
        }
    }

    /* ------- AI 服务商 ------- */

    fun updateProvider(baseUrl: String, apiKey: String, model: String) {
        _state.value = _state.value.copy(baseUrl = baseUrl, apiKey = apiKey, model = model, saved = false)
    }

    /**
     * 测试连接：用对话框里当前填写的值（而非已保存配置）先保存，
     * 再发一个最小请求，成功/失败都回调消息。
     */
    fun testConnection(baseUrl: String, apiKey: String, model: String, onResult: (String) -> Unit) {
        if (baseUrl.isBlank() || apiKey.isBlank() || model.isBlank()) {
            onResult("请先填写接口地址、Key 和模型名")
            return
        }
        store.baseUrl = baseUrl.trim()
        store.apiKey = apiKey.trim()
        store.model = model.trim()
        _state.value = _state.value.copy(baseUrl = baseUrl, apiKey = apiKey, model = model, testing = true)
        viewModelScope.launch {
            _state.value.let { cur ->
                try {
                    val reply = ai.testConnection()
                    val msg = "✅ 连接成功${if (reply.isBlank()) "" else "：模型回复「${reply.take(24)}」"}"
                    onResult(msg)
                    _state.value = cur.copy(testing = false, saved = true)
                } catch (t: Throwable) {
                    val reason = t.message?.take(160) ?: t.javaClass.simpleName
                    onResult("❌ 连接失败：$reason")
                    _state.value = cur.copy(testing = false)
                }
            }
        }
    }

    fun saveProvider(onToast: (String) -> Unit) {
        val s = _state.value
        if (s.baseUrl.isBlank() || s.apiKey.isBlank() || s.model.isBlank()) {
            onToast("还差必填项：接口地址、Key、模型名都要填好才能开炉")
            return
        }
        store.baseUrl = s.baseUrl.trim()
        store.apiKey = s.apiKey.trim()
        store.model = s.model.trim()
        _state.value = s.copy(saved = true)
        onToast("已保存 ✅ 堆肥将直连你的服务商")
    }

    /* ------- 导入/导出（specs/40）------- */

    /** 导出全量备份 zip（面包渣+堆肥+菌群+益生菌+生态事件+个人资料+AI 配置）。 */
    fun exportBackup(uri: Uri, onDone: (String) -> Unit) {
        if (_state.value.backupBusy) return
        _state.value = _state.value.copy(backupBusy = true)
        viewModelScope.launch {
            try {
                val s = backup.exportTo(uri)
                onDone("✅ 已导出：${s.ideas} 颗面包渣 · ${s.composts} 次堆肥 · ${s.agents} 位菌群 · ${s.probiotics} 枚益生菌")
            } catch (t: Throwable) {
                android.util.Log.e("Backup", "export failed", t)
                onDone("❌ 导出失败：${t.message ?: t.javaClass.simpleName}")
            } finally {
                _state.value = _state.value.copy(backupBusy = false)
            }
        }
    }

    /** 导入备份（清空当前数据并整库恢复，含 AI 配置）。 */
    fun importBackup(uri: Uri, onDone: (String) -> Unit) {
        if (_state.value.backupBusy) return
        _state.value = _state.value.copy(backupBusy = true)
        viewModelScope.launch {
            try {
                val s = backup.importFrom(uri)
                _state.value = _state.value.copy(
                    baseUrl = store.baseUrl, apiKey = store.apiKey, model = store.model,
                    nickname = profile.nickname, avatarEmoji = profile.avatarEmoji
                )
                onDone("✅ 已恢复：${s.ideas} 颗面包渣 · ${s.composts} 次堆肥 · ${s.agents} 位菌群 · ${s.probiotics} 枚益生菌")
            } catch (t: Throwable) {
                android.util.Log.e("Backup", "import failed", t)
                onDone("❌ 导入失败：${t.message ?: t.javaClass.simpleName}")
            } finally {
                _state.value = _state.value.copy(backupBusy = false)
            }
        }
    }

    companion object {
        val AVATARS = listOf("🌱", "🍄", "🌿", "🦠", "🍂", "🪴", "☕", "🐙")
    }
}
