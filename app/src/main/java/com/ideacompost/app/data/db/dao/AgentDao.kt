package com.ideacompost.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ideacompost.app.data.db.entity.AgentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AgentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(agents: List<AgentEntity>)

    @Query("SELECT COUNT(*) FROM agents")
    suspend fun count(): Int

    @Query("SELECT * FROM agents WHERE status IN ('active','embryo','compressed') ORDER BY vitality DESC")
    fun observeActive(): Flow<List<AgentEntity>>

    @Query("SELECT * FROM agents WHERE type = 'domain' ORDER BY id")
    fun observeDomains(): Flow<List<AgentEntity>>

    @Query("UPDATE agents SET status = :status, vitality = :vitality, updated_at = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, vitality: Double, updatedAt: Long)
}
