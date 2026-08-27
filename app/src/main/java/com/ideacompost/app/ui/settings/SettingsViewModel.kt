package com.ideacompost.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ideacompost.app.data.ai.ProviderStore
import com.ideacompost.app.data.db.dao.LlmCallDao
import com.ideacompost.app.data.db.entity.LlmCallEntity
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
    llmCallDao: LlmCallDao
) : ViewModel() {

    data class UiState(
        val baseUrl: String = "",
        val apiKey: String = "",
        val model: String = "",
        val mockMode: Boolean = true,
        val saved: Boolean = false
    )

    private val _state = MutableStateFlow(
        UiState(baseUrl = store.baseUrl, apiKey = store.apiKey, model = store.model, mockMode = store.mockMode)
    )
    val state: StateFlow<UiState> = _state

    /** 调用遥测（llm_calls）：最近 8 条，Mock 也计入。 */
    val recentCalls: StateFlow<List<LlmCallEntity>> =
        llmCallDao.observeRecent(8).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun update(baseUrl: String, apiKey: String, model: String, mockMode: Boolean) {
        _state.value = _state.value.copy(baseUrl = baseUrl, apiKey = apiKey, model = model, mockMode = mockMode, saved = false)
    }

    fun save(onToast: (String) -> Unit) {
        val s = _state.value
        store.baseUrl = s.baseUrl.trim()
        store.apiKey = s.apiKey.trim()
        store.model = s.model.trim()
        store.mockMode = s.mockMode
        _state.value = s.copy(saved = true)
        onToast(if (s.mockMode) "已保存：继续演示模式 🎭" else "已保存 ✅ 下次堆肥将使用你的服务商")
    }
}
