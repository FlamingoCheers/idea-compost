package com.ideacompost.app.ui.settings

import androidx.lifecycle.ViewModel
import com.ideacompost.app.data.ai.ProviderStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val store: ProviderStore
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
