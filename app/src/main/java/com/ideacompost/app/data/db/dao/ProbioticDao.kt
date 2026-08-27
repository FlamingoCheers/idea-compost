package com.ideacompost.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ideacompost.app.data.db.entity.ProbioticEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProbioticDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(probiotics: List<ProbioticEntity>)

    @Query("SELECT * FROM probiotics WHERE scope = 'global_builtin' AND hidden = 0 ORDER BY id")
    fun observeBuiltIn(): Flow<List<ProbioticEntity>>

    @Query("SELECT * FROM probiotics WHERE hidden = 0 ORDER BY created_at, id")
    fun observeAll(): Flow<List<ProbioticEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(probiotic: ProbioticEntity)

    @Query("DELETE FROM probiotics WHERE id = :id AND scope = 'user_defined'")
    suspend fun deleteUserDefined(id: String)

    @Query("UPDATE probiotics SET hidden = 1, updated_at = :now WHERE id = :id AND scope = 'global_builtin'")
    suspend fun hideBuiltin(id: String, now: Long)

    @Query("SELECT COUNT(*) FROM probiotics")
    suspend fun count(): Int

    @Query("SELECT * FROM probiotics WHERE id = :id")
    suspend fun byId(id: String): ProbioticEntity?
}
