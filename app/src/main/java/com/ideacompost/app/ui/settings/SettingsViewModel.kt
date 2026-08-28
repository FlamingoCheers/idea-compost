package com.ideacompost.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ideacompost.app.data.ProfileStore
import com.ideacompost.app.data.ai.ProviderStore
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
    private val profile: ProfileStore,
    private val eco: EcoEngine,
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
        val demoMode: Boolean = true,
        val saved: Boolean = false,
    )

    private val _state = MutableStateFlow(
        UiState(baseUrl = store.baseUrl, apiKey = store.apiKey, model = store.model, demoMode = store.mockMode)
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

    /** 调用遥测（llm_calls）：最近 8 条，Mock 也计入。 */
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

    fun updateProvider(baseUrl: String, apiKey: String, model: String, demoMode: Boolean) {
        _state.value = _state.value.copy(baseUrl = baseUrl, apiKey = apiKey, model = model, demoMode = demoMode, saved = false)
    }

    fun setDemoMode(on: Boolean) {
        _state.value = _state.value.copy(demoMode = on, saved = false)
    }

    fun saveProvider(onToast: (String) -> Unit) {
        val s = _state.value
        store.baseUrl = s.baseUrl.trim()
        store.apiKey = s.apiKey.trim()
        store.model = s.model.trim()
        store.mockMode = s.demoMode
        _state.value = s.copy(saved = true)
        onToast(if (s.demoMode) "已保存：继续演示模式 🎭" else "已保存 ✅ 下次堆肥将使用你的服务商")
    }

    companion object {
        val AVATARS = listOf("🌱", "🍄", "🌿", "🦠", "🍂", "🪴", "☕", "🐙")
    }
}
