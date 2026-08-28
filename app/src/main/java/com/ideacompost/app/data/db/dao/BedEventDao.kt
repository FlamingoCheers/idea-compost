package com.ideacompost.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.ideacompost.app.data.db.entity.BedEventEntity

@Dao
interface BedEventDao {
    @Insert
    suspend fun insert(event: BedEventEntity)

    @Insert
    suspend fun insertAll(events: List<BedEventEntity>)

    @Query("SELECT COUNT(*) FROM bed_events")
    suspend fun count(): Int

    @Query("SELECT * FROM bed_events ORDER BY seq")
    suspend fun all(): List<BedEventEntity>

    @Query("DELETE FROM bed_events")
    suspend fun deleteAll()
}
