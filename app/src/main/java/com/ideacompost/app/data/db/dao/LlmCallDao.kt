package com.ideacompost.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.ideacompost.app.data.db.entity.LlmCallEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LlmCallDao {
    @Insert
    suspend fun insert(call: LlmCallEntity)

    @Insert
    suspend fun insertAll(calls: List<LlmCallEntity>)

    @Query("SELECT * FROM llm_calls ORDER BY id DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<LlmCallEntity>>

    @Query("SELECT COUNT(*) FROM llm_calls")
    fun observeCount(): Flow<Int>

    @Query("SELECT * FROM llm_calls ORDER BY id")
    suspend fun all(): List<LlmCallEntity>

    @Query("DELETE FROM llm_calls")
    suspend fun deleteAll()
}
