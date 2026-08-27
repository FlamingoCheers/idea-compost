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

    @Query("SELECT * FROM ideas ORDER BY created_at DESC")
    fun observeAll(): Flow<List<IdeaEntity>>

    @Query("SELECT * FROM ideas WHERE id = :id")
    suspend fun byId(id: String): IdeaEntity?

    @Query("UPDATE ideas SET content = :content, updated_at = :updatedAt WHERE id = :id")
    suspend fun updateContent(id: String, content: String, updatedAt: Long)

    @Query("SELECT COUNT(*) FROM ideas")
    suspend fun count(): Int
}
