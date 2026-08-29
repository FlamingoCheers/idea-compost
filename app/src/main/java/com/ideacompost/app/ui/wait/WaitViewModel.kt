package com.ideacompost.app.ui.wait

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ideacompost.app.data.db.dao.CompostDao
import com.ideacompost.app.data.db.entity.CompostEntity
import com.ideacompost.app.domain.CompostProgressBus
import com.ideacompost.app.domain.CompostService
import com.ideacompost.app.domain.CompostServiceLock
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WaitViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val compostDao: CompostDao,
    private val progressBus: CompostProgressBus
) : ViewModel() {

    val compostId: String = savedStateHandle.get<String>("id") ?: ""

    /** 逐菌发酵进度（specs/41 P6），按本页堆肥过滤后交给 UI。 */
    val progress: StateFlow<CompostProgressBus.Round?> = progressBus.state

    private var igniteRequested = false

    /** 轮询驱动（Room Flow 在重动画负载下发射滞后；轮询是确定性的）。 */
    private val _compost = MutableStateFlow<CompostEntity?>(null)
    val compost: StateFlow<CompostEntity?> = _compost

    private val _started = MutableStateFlow(false)
    val started: StateFlow<Boolean> = _started

    init {
        // 守护循环：拉取最新行 + 自动点火（走前台服务） + 终态检测（IO 线程）。
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
                if (c != null && !igniteRequested) {
                    val deadRun = c.status == "running" && CompostServiceLock.active != compostId
                    if (c.status == "pending" || c.status == "suspended" || deadRun) {
                        igniteRequested = true
                        if (deadRun) {
                            // 进程曾中途死亡：状态归位为 suspended（阶段已持久化，服务重启后续跑）
                            compostDao.updateProgress(compostId, "suspended", c.currentStage, System.currentTimeMillis())
                        }
                        _started.value = true
                        CompostService.start(context, compostId)
                    }
                }
                if (c != null && (c.status == "awaiting_feedback" || c.status == "done")) break
                delay(if (igniteRequested) 800L else 400L)
            }
        }
    }

    /** 用户主动暂停：停前台服务 → 引擎任务取消 → 堆肥置为 suspended（断点保留，回来接着发酵）。 */
    fun pause() {
        CompostService.stop(context, compostId)
        _started.value = false
    }

    /** 失败重试：清阶段缓存（真正从头开始） → 回到 pending → 守护循环自动点火。 */
    fun retry() {
        if (_restarting) return
        _restarting = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                compostDao.clearStages(compostId)
                compostDao.resetForRetry(compostId, System.currentTimeMillis())
            } finally {
                igniteRequested = false
                _restarting = false
            }
        }
    }

    private var _restarting = false

    override fun onCleared() {
        // 不停服务：离开页面/销毁 VM 后发酵继续（V2 前台服务语义）。
        super.onCleared()
    }
}
