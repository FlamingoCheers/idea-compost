package com.ideacompost.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** LLM 调用遥测（specs/02 §7）：每次调用一行，Mock 也记录。 */
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
