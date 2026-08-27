package com.ideacompost.app.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** 一次堆肥。DDL 对齐 specs/01 §3（M2 字段子集）。 */
@Entity(tableName = "composts", indices = [Index(value = ["status"]), Index(value = ["created_at"])])
data class CompostEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "status") val status: String, // pending|running|awaiting_feedback|completed|failed|suspended
    @ColumnInfo(name = "depth") val depth: String, // shallow|standard|deep
    @ColumnInfo(name = "input_idea_ids") val inputIdeaIds: String, // json array
    @ColumnInfo(name = "probiotic_ids") val probioticIds: String, // json array
    @ColumnInfo(name = "current_stage") val currentStage: String, // identify|convoke|ferment_r1|r2|r3|integrate|assess|done
    @ColumnInfo(name = "roster_json") val rosterJson: String, // 召集菌群 [{name,code,weight,role,type}]
    @ColumnInfo(name = "title") val title: String?,
    @ColumnInfo(name = "output_json") val outputJson: String?,
    @ColumnInfo(name = "nutrition_json") val nutritionJson: String?,
    @ColumnInfo(name = "error") val error: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long
)

/** 阶段产物即 checkpoint（D17 / INV-8）。 */
@Entity(tableName = "compost_stages", indices = [Index(value = ["compost_id"])])
data class CompostStageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "compost_id") val compostId: String,
    @ColumnInfo(name = "stage_key") val stageKey: String,
    @ColumnInfo(name = "payload") val payload: String,
    @ColumnInfo(name = "created_at") val createdAt: Long
)

/** 低成本反馈四键（05 §4 / 02 §3.2）。 */
@Entity(tableName = "feedback_events", indices = [Index(value = ["compost_id"])])
data class FeedbackEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "compost_id") val compostId: String,
    @ColumnInfo(name = "kind") val kind: String, // heart|star|develop|disagree
    @ColumnInfo(name = "created_at") val createdAt: Long
)
