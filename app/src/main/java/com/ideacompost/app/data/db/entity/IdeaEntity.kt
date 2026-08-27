package com.ideacompost.app.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** 面包渣。content 为 Markdown 原文，INV-1：永不压缩、永不改写。 */
@Entity(
    tableName = "ideas",
    indices = [Index("created_at"), Index("status")]
)
data class IdeaEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "content") val content: String,
    @ColumnInfo(name = "content_type", defaultValue = "text") val contentType: String = "text",
    @ColumnInfo(name = "title") val title: String? = null,
    @ColumnInfo(name = "source", defaultValue = "manual") val source: String = "manual",
    @ColumnInfo(name = "status", defaultValue = "raw") val status: String = "raw",
    @ColumnInfo(name = "metadata", defaultValue = "{}") val metadata: String = "{}",
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long
)
