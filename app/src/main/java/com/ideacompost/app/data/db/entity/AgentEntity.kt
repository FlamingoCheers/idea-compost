package com.ideacompost.app.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** 菌（Agent 是数据不是代码对象，INV-6）。DDL 对齐 specs/01 §4。 */
@Entity(
    tableName = "agents",
    indices = [Index(value = ["status", "vitality"], orders = [Index.Order.ASC, Index.Order.DESC])]
)
data class AgentEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "type") val type: String, // domain | method | creative
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "description") val description: String,
    @ColumnInfo(name = "capability_card") val capabilityCard: String,
    @ColumnInfo(name = "card_version", defaultValue = "1") val cardVersion: Int = 1,
    @ColumnInfo(name = "specialties", defaultValue = "[]") val specialties: String = "[]",
    @ColumnInfo(name = "status", defaultValue = "active") val status: String = "active", // embryo|active|compressed|dormant|fused
    @ColumnInfo(name = "vitality", defaultValue = "40") val vitality: Double = 40.0,
    @ColumnInfo(name = "nutrition_buffer", defaultValue = "0") val nutritionBuffer: Double = 0.0,
    @ColumnInfo(name = "parent_id") val parentId: String? = null,
    @ColumnInfo(name = "fusion_of") val fusionOf: String? = null,
    @ColumnInfo(name = "compressed_memory") val compressedMemory: String? = null,
    @ColumnInfo(name = "full_profile") val fullProfile: String,
    @ColumnInfo(name = "participation_count", defaultValue = "0") val participationCount: Int = 0,
    @ColumnInfo(name = "last_contribution_at") val lastContributionAt: Long? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long
)
