package com.ideacompost.app.ui.wait

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ideacompost.app.data.db.dao.CompostDao
import com.ideacompost.app.data.db.entity.CompostEntity
import com.ideacompost.app.domain.CompostEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WaitViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val compostDao: CompostDao,
    private val engine: CompostEngine
) : ViewModel() {

    val compostId: String = savedStateHandle.get<String>("id") ?: ""

    private var job: Job? = null
    private var runStarted = false

    /** 轮询驱动（Room Flow 在重动画负载下发射滞后；轮询是确定性的）。 */
    private val _compost = MutableStateFlow<CompostEntity?>(null)
    val compost: StateFlow<CompostEntity?> = _compost

    private val _started = MutableStateFlow(false)
    val started: StateFlow<Boolean> = _started

    init {
        // 守护循环：拉取最新行 + 自动点火 + 终态检测（IO 线程）。
        viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                val c = try {
                    compostDao.getById(compostId)
                } catch (t: Throwable) {
                    null
                }
                if (c != null && _compost.value != c) {
                    android.util.Log.d("WaitVM", "poll update: ${c.status}/${c.currentStage}")
                    _compost.value = c
                }
                if (c != null && !runStarted &&
                    (c.status == "pending" || c.status == "suspended")
                ) {
                    runStarted = true
                    _started.value = true
                    job = launch(Dispatchers.Default) { engine.run(compostId) }
                }
                if (c != null && (c.status == "awaiting_feedback" || c.status == "done")) break
                delay(if (runStarted) 800L else 400L)
            }
        }
    }

    /** 诚实挂起：切后台/离开即停（氧气语义）。 */
    fun pause() {
        job?.cancel()
        job = null
        runStarted = false
        _started.value = false
    }

    /** 失败重试：清阶段缓存 → 回到 pending → 守护循环自动点火。 */
    fun retry() {
        if (_restarting) return
        _restarting = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                compostDao.clearStages(compostId)
                compostDao.resetForRetry(compostId, System.currentTimeMillis())
            } finally {
                runStarted = false
                _restarting = false
            }
        }
    }

    private var _restarting = false

    override fun onCleared() {
        job?.cancel()
        super.onCleared()
    }
}
