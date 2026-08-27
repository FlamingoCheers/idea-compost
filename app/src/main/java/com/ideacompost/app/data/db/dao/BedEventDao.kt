package com.ideacompost.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.ideacompost.app.data.db.entity.BedEventEntity

@Dao
interface BedEventDao {
    @Insert
    suspend fun insert(event: BedEventEntity)

    @Query("SELECT COUNT(*) FROM bed_events")
    suspend fun count(): Int
}
