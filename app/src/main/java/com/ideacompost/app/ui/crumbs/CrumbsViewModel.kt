package com.ideacompost.app.ui.crumbs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ideacompost.app.data.db.dao.BedEventDao
import com.ideacompost.app.data.db.dao.IdeaDao
import com.ideacompost.app.data.db.entity.BedEventEntity
import com.ideacompost.app.data.db.entity.IdeaEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class CrumbsUiState(
    val input: String = "",
    val justSaved: Boolean = false,
    val selecting: Boolean = false,
    val selected: Set<String> = emptySet()
)

@HiltViewModel
class CrumbsViewModel @Inject constructor(
    private val ideaDao: IdeaDao,
    private val bedEventDao: BedEventDao
) : ViewModel() {

    private val _state = MutableStateFlow(CrumbsUiState())
    val state: StateFlow<CrumbsUiState> = _state

    val crumbs: StateFlow<List<IdeaEntity>> = ideaDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onInputChange(value: String) {
        _state.value = _state.value.copy(input = value, justSaved = false)
    }

    /** 低阻力保存：无命名、无标签、无分类——丢进去就完成（P-01）。 */
    fun saveCrumb() {
        val content = _state.value.input.trim()
        if (content.isEmpty()) return
        val now = System.currentTimeMillis()
        val idea = IdeaEntity(
            id = UUID.randomUUID().toString(),
            content = content,
            createdAt = now,
            updatedAt = now
        )
        viewModelScope.launch {
            ideaDao.insert(idea)
            bedEventDao.insert(
                BedEventEntity(ts = now, eventType = "idea_created", payload = """{"idea_id":"${idea.id}"}""")
            )
            _state.value = CrumbsUiState(input = "", justSaved = true)
        }
    }

    fun updateCrumb(id: String, content: String) {
        viewModelScope.launch {
            ideaDao.updateContent(id, content.trim(), System.currentTimeMillis())
        }
    }

    fun toggleSelect(id: String) {
        val s = _state.value
        if (!s.selecting) {
            _state.value = s.copy(selecting = true, selected = setOf(id), justSaved = false)
        } else {
            val next = s.selected.toMutableSet().apply { if (!add(id)) remove(id) }
            _state.value = s.copy(selected = next, selecting = next.isNotEmpty())
        }
    }

    fun exitSelection() {
        _state.value = _state.value.copy(selecting = false, selected = emptySet())
    }
}
