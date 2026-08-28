package com.ideacompost.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** LLM 调用遥测（specs/02 §7）：每次调用一行，含连通性测试。 */
@Entity(tableName = "llm_calls")
data class LlmCallEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ts: Long,
    val stageKey: String,
    val provider: String,
    val status: String,
    val promptChars: Int,
    val responseChars: Int,
    val latencyMs: Long,
    val error: String? = null
)
