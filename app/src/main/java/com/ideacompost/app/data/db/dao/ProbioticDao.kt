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

    @Query("SELECT COUNT(*) FROM probiotics")
    suspend fun count(): Int
}
