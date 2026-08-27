package com.ideacompost.app.ui.output

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ideacompost.app.data.db.dao.AgentDao
import com.ideacompost.app.data.db.dao.CompostDao
import com.ideacompost.app.data.db.dao.IdeaDao
import com.ideacompost.app.data.db.entity.BedEventEntity
import com.ideacompost.app.data.db.dao.BedEventDao
import com.ideacompost.app.data.db.entity.CompostEntity
import com.ideacompost.app.data.db.entity.IdeaEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject

data class OutputUiState(
    val compost: CompostEntity? = null,
    val crumbs: List<IdeaEntity> = emptyList(),
    val feedbackGiven: String? = null
)

@HiltViewModel
class OutputViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val compostDao: CompostDao,
    private val ideaDao: IdeaDao,
    private val agentDao: AgentDao,
    private val bedEventDao: BedEventDao
) : ViewModel() {

    val compostId: String = savedStateHandle.get<String>("id") ?: ""

    private val _state = MutableStateFlow(OutputUiState())
    val state: StateFlow<OutputUiState> = _state

    init {
        viewModelScope.launch {
            compostDao.observeById(compostId).collect { c ->
                if (c != null) {
                    val ids = org.json.JSONArray(c.inputIdeaIds).let { l ->
                        (0 until l.length()).map { l.getString(it) }
                    }
                    val crumbs = ideaDao.byIds(ids).sortedBy { ids.indexOf(it.id) }
                    _state.value = _state.value.copy(compost = c, crumbs = crumbs)
                }
            }
        }
        viewModelScope.launch {
            compostDao.observeFeedbackCount(compostId).collect { n ->
                val fb = if (n > 0) compostDao.feedbacks(compostId).lastOrNull()?.kind else null
                _state.value = _state.value.copy(feedbackGiven = fb)
            }
        }
    }

    /** 反馈四键（05 §4）：事件入库 + 营养分摊（02 §3.2 简化版）。 */
    fun feedback(kind: String, onToast: (String) -> Unit) {
        if (_state.value.feedbackGiven != null) return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            compostDao.insertFeedback(
                com.ideacompost.app.data.db.entity.FeedbackEventEntity(
                    compostId = compostId, kind = kind, createdAt = now
                )
            )
            val output = _state.value.compost?.outputJson?.let { runCatching { JSONObject(it) }.getOrNull() }
            val agents = output?.optJSONArray("agents_used")
            if (agents != null && kind != "disagree") {
                val per = when (kind) {
                    "heart" -> 1.5
                    "star" -> 3.0
                    else -> 3.5
                } / agents.length().coerceAtLeast(1)
                for (i in 0 until agents.length()) {
                    val name = agents.optJSONObject(i)?.optString("agent") ?: continue
                    val a = agentDao.byName(name) ?: continue
                    agentDao.addNutrition(a.id, per, now)
                }
            }
            bedEventDao.insert(
                BedEventEntity(
                    ts = now, eventType = "feedback_given",
                    payload = JSONObject().put("compost_id", compostId).put("kind", kind).toString()
                )
            )
            onToast(
                when (kind) {
                    "heart" -> "菌群收到了养分 ❤️"
                    "star" -> "已存入菌床，值得保留 ⭐"
                    "develop" -> "已生成后续堆肥候选 ↗"
                    else -> "已记录分歧——下次堆肥会引入对立菌"
                }
            )
        }
    }

    /** as_crumbs 一键转面包渣（闭环设计）。 */
    fun saveAsCrumb(text: String, onDone: () -> Unit) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val idea = IdeaEntity(id = java.util.UUID.randomUUID().toString(), content = text, createdAt = now, updatedAt = now)
            ideaDao.insert(idea)
            bedEventDao.insert(
                BedEventEntity(ts = now, eventType = "idea_created", payload = JSONObject().put("idea_id", idea.id).put("from", "future_direction").toString())
            )
            onDone()
        }
    }
}
