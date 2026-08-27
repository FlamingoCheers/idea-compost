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

    @Query("SELECT * FROM agents WHERE status IN ('active','embryo','compressed')")
    suspend fun convokable(): List<AgentEntity>

    @Query("SELECT * FROM agents WHERE id = :id")
    suspend fun byId(id: String): AgentEntity?

    @Query("SELECT * FROM agents WHERE name = :name LIMIT 1")
    suspend fun byName(name: String): AgentEntity?

    @Query("UPDATE agents SET nutrition_buffer = nutrition_buffer + :amount, updated_at = :now WHERE id = :id")
    suspend fun addNutrition(id: String, amount: Double, now: Long)

    @Query("UPDATE agents SET participation_count = participation_count + 1, last_contribution_at = :now, updated_at = :now WHERE id = :id")
    suspend fun recordParticipation(id: String, now: Long)

    @Query("SELECT * FROM agents WHERE type = 'domain' ORDER BY id")
    fun observeDomains(): Flow<List<AgentEntity>>

    @Query("UPDATE agents SET status = :status, vitality = :vitality, updated_at = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, vitality: Double, updatedAt: Long)
}
