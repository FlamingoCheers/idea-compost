package com.ideacompost.app.domain

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** 发酵逐菌进度（specs/41 P6）：引擎同轮每完成/跳过一个菌上报一次，等待页实时显示。 */
@Singleton
class CompostProgressBus @Inject constructor() {
    data class Round(val compostId: String, val round: String, val done: Int, val total: Int)

    private val _state = MutableStateFlow<Round?>(null)
    val state: StateFlow<Round?> = _state.asStateFlow()

    fun update(compostId: String, round: String, done: Int, total: Int) {
        _state.value = Round(compostId, round, done, total)
    }

    fun step(compostId: String, round: String) {
        val cur = _state.value
        if (cur?.compostId == compostId && cur.round == round) {
            _state.value = cur.copy(done = (cur.done + 1).coerceAtMost(cur.total))
        }
    }
}
