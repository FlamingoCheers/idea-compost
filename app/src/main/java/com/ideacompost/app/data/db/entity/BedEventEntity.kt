package com.ideacompost.app.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** 菌床事件流：append-only（INV-10）。App 层禁止 UPDATE/DELETE。 */
@Entity(
    tableName = "bed_events",
    indices = [Index("event_type", "ts"), Index("compost_id")]
)
data class BedEventEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "seq") val seq: Long = 0,
    @ColumnInfo(name = "ts") val ts: Long,
    @ColumnInfo(name = "event_type") val eventType: String,
    @ColumnInfo(name = "payload", defaultValue = "{}") val payload: String = "{}",
    @ColumnInfo(name = "compost_id") val compostId: String? = null
)
