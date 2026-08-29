package com.ideacompost.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ideacompost.app.data.db.entity.IdeaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IdeaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(idea: IdeaEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(ideas: List<IdeaEntity>)

    @Query("SELECT * FROM ideas ORDER BY created_at DESC")
    fun observeAll(): Flow<List<IdeaEntity>>

    @Query("SELECT * FROM ideas WHERE id = :id")
    suspend fun byId(id: String): IdeaEntity?

    @Query("SELECT * FROM ideas WHERE id IN (:ids)")
    suspend fun byIds(ids: List<String>): List<IdeaEntity>

    @Query("UPDATE ideas SET status = 'composted', updated_at = :now WHERE id IN (:ids)")
    suspend fun markComposted(ids: List<String>, now: Long)

    @Query("UPDATE ideas SET content = :content, updated_at = :updatedAt WHERE id = :id")
    suspend fun updateContent(id: String, content: String, updatedAt: Long)

    @Query("DELETE FROM ideas WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT COUNT(*) FROM ideas")
    suspend fun count(): Int

    /** 近期面包渣——菌床唤醒的关键词匹配源。 */
    @Query("SELECT * FROM ideas WHERE created_at >= :since ORDER BY created_at DESC LIMIT 60")
    suspend fun recentSince(since: Long): List<IdeaEntity>

    @Query("SELECT * FROM ideas ORDER BY created_at")
    suspend fun all(): List<IdeaEntity>

    @Query("DELETE FROM ideas")
    suspend fun deleteAll()
}
