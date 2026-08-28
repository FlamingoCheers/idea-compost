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
    val providerReady: Boolean = false,
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
            _state.value = _state.value.copy(
                ideas = ideas,
                providerReady = providerStore.ready()
            )
        }
        // 益生菌实时观察：内置 + 用户自定义（增删改即时反映）
        viewModelScope.launch {
            probioticDao.observeAll().collect { list ->
                _state.value = _state.value.copy(
                    probiotics = list,
                    picked = _state.value.picked.filter { id -> list.any { it.id == id } }
                )
            }
        }
    }

    /** 新建/编辑自定义益生菌：prompt_logic 由用户描述直接构成思考方向。 */
    fun upsertProbiotic(id: String?, name: String, description: String) {
        val n = name.trim(); val d = description.trim()
        if (n.isEmpty() || d.isEmpty()) return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val existing = id?.let { probioticDao.byId(it) }
            val pb = ProbioticEntity(
                id = existing?.id ?: "user_${now}",
                name = if (n.endsWith("益生菌")) n else "${n}益生菌",
                icon = existing?.icon,
                description = d,
                promptLogic = "从以下用户指定的方向审视这批碎片：$d",
                scope = existing?.scope ?: "user_defined",
                usageCount = existing?.usageCount ?: 0,
                lastUsed = existing?.lastUsed,
                birthContext = existing?.birthContext ?: "用户在堆肥设置中亲手投放",
                createdAt = existing?.createdAt ?: now,
                updatedAt = now
            )
            probioticDao.upsert(pb)
        }
    }

    /** 内置→软删（hidden），自定义→硬删；同步取消已选。 */
    fun deleteProbiotic(id: String) {
        viewModelScope.launch {
            val pb = probioticDao.byId(id) ?: return@launch
            if (pb.scope == "user_defined") probioticDao.deleteUserDefined(id)
            else probioticDao.hideBuiltin(id, System.currentTimeMillis())
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
