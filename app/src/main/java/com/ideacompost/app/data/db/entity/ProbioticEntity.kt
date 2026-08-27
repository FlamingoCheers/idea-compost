package com.ideacompost.app.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** 益生菌。DDL 对齐 specs/01 §8。 */
@Entity(tableName = "probiotics")
data class ProbioticEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "icon") val icon: String? = null,
    @ColumnInfo(name = "description") val description: String,
    @ColumnInfo(name = "prompt_logic") val promptLogic: String,
    @ColumnInfo(name = "target_types", defaultValue = "[]") val targetTypes: String = "[]",
    @ColumnInfo(name = "domain_boosts", defaultValue = "{}") val domainBoosts: String = "{}",
    @ColumnInfo(name = "stage_emphasis", defaultValue = "{}") val stageEmphasis: String = "{}",
    @ColumnInfo(name = "diversity_shift") val diversityShift: String? = null,
    @ColumnInfo(name = "scope", defaultValue = "user_defined") val scope: String = "global_builtin",
    @ColumnInfo(name = "hidden", defaultValue = "0") val hidden: Boolean = false,
    @ColumnInfo(name = "usage_count", defaultValue = "0") val usageCount: Int = 0,
    @ColumnInfo(name = "last_used") val lastUsed: Long? = null,
    @ColumnInfo(name = "birth_context") val birthContext: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long
)
