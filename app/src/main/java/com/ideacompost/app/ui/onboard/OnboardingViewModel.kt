package com.ideacompost.app.ui.onboard

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ideacompost.app.data.seed.DataSeeder
import com.ideacompost.app.data.seed.ParsedAgentCard
import com.ideacompost.app.data.seed.PromptCardParser
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingUiState(
    val domains: List<ParsedAgentCard> = emptyList(),
    val picked: Set<String> = emptySet(),
    val seeding: Boolean = false
) {
    val canConfirm: Boolean get() = picked.isNotEmpty() && !seeding
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val seeder: DataSeeder,
    private val prefs: SharedPreferences
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingUiState())
    val state: StateFlow<OnboardingUiState> = _state

    init {
        val cards = context.assets.open("prompts/agents/domains.md")
            .bufferedReader().use { it.readText() }
            .let(PromptCardParser::parseAgents)
            .filter { it.seedStatus == "pickable" }
        // 默认预选三个（设计稿：哲学、技术、历史）
        _state.update {
            it.copy(
                domains = cards,
                picked = setOf(
                    "agent_domain_philosophy",
                    "agent_domain_technology",
                    "agent_domain_history"
                ).filter { id -> cards.any { c -> c.id == id } }.toSet()
            )
        }
    }

    fun toggle(id: String) {
        _state.update {
            it.copy(picked = if (id in it.picked) it.picked - id else it.picked + id)
        }
    }

    fun confirm(onDone: () -> Unit) {
        if (!_state.value.canConfirm) return
        _state.update { it.copy(seeding = true) }
        viewModelScope.launch {
            seeder.seed(_state.value.picked)
            prefs.edit().putBoolean("onboarded", true).apply()
            onDone()
        }
    }
}
