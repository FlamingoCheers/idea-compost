package com.ideacompost.app.ui.setup

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ideacompost.app.data.ai.ProviderStore
import com.ideacompost.app.data.db.dao.CompostDao
import com.ideacompost.app.data.db.dao.IdeaDao
import com.ideacompost.app.data.db.dao.ProbioticDao
import com.ideacompost.app.data.db.entity.IdeaEntity
import com.ideacompost.app.data.db.entity.ProbioticEntity
import com.ideacompost.app.domain.CompostEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SetupUiState(
    val ideas: List<IdeaEntity> = emptyList(),
    val probiotics: List<ProbioticEntity> = emptyList(),
    val picked: List<String> = emptyList(),
    val depth: String = "standard",
    val mockMode: Boolean = true,
    val providerReady: Boolean = true,
    val firing: Boolean = false
)

@HiltViewModel
class SetupViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val ideaDao: IdeaDao,
    private val probioticDao: ProbioticDao,
    private val compostDao: CompostDao,
    private val providerStore: ProviderStore
) : ViewModel() {

    private val idsParam: String = savedStateHandle.get<String>("ids") ?: ""
    private val ideaIds = idsParam.split(",").filter { it.isNotBlank() }

    private val _state = MutableStateFlow(SetupUiState())
    val state: StateFlow<SetupUiState> = _state

    init {
        viewModelScope.launch {
            val ideas = ideaDao.byIds(ideaIds).sortedBy { ideaIds.indexOf(it.id) }
            val probiotics = probioticDao.observeBuiltIn().first()
            _state.value = _state.value.copy(
                ideas = ideas,
                probiotics = probiotics,
                mockMode = providerStore.mockMode,
                providerReady = providerStore.ready()
            )
        }
    }

    fun removeIdea(id: String) {
        _state.value = _state.value.copy(ideas = _state.value.ideas.filter { it.id != id })
    }

    fun toggleProbiotic(id: String) {
        val cur = _state.value.picked
        _state.value = _state.value.copy(
            picked = if (id in cur) cur - id else if (cur.size >= 2) cur else cur + id
        )
    }

    fun setDepth(d: String) {
        _state.value = _state.value.copy(depth = d)
    }

    /** 预检（03 §3）：面包渣非空、益生菌 ≤2；点火即建堆肥记录。 */
    fun fire(onCreated: (String) -> Unit) {
        if (_state.value.firing || _state.value.ideas.isEmpty()) return
        _state.value = _state.value.copy(firing = true)
        viewModelScope.launch {
            val compost = CompostEngine.newCompost(
                _state.value.ideas.map { it.id },
                _state.value.picked,
                _state.value.depth
            )
            compostDao.insert(compost)
            onCreated(compost.id)
        }
    }
}
