package com.ideacompost.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ideacompost.app.data.db.dao.AgentDao
import com.ideacompost.app.data.db.dao.BedEventDao
import com.ideacompost.app.data.db.dao.IdeaDao
import com.ideacompost.app.data.db.dao.ProbioticDao
import com.ideacompost.app.data.db.entity.AgentEntity
import com.ideacompost.app.data.db.entity.BedEventEntity
import com.ideacompost.app.data.db.entity.IdeaEntity
import com.ideacompost.app.data.db.entity.ProbioticEntity

@Database(
    entities = [
        IdeaEntity::class,
        AgentEntity::class,
        ProbioticEntity::class,
        BedEventEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class IdeaCompostDatabase : RoomDatabase() {
    abstract fun ideaDao(): IdeaDao
    abstract fun agentDao(): AgentDao
    abstract fun probioticDao(): ProbioticDao
    abstract fun bedEventDao(): BedEventDao
}
