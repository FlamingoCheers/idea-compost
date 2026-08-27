package com.ideacompost.app.di

import android.content.Context
import android.content.SharedPreferences
import androidx.room.Room
import com.ideacompost.app.data.db.IdeaCompostDatabase
import com.ideacompost.app.data.db.dao.AgentDao
import com.ideacompost.app.data.db.dao.BedEventDao
import com.ideacompost.app.data.db.dao.CompostDao
import com.ideacompost.app.data.db.dao.IdeaDao
import com.ideacompost.app.data.db.dao.ProbioticDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): IdeaCompostDatabase =
        Room.databaseBuilder(context, IdeaCompostDatabase::class.java, "ideacompost.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideIdeaDao(db: IdeaCompostDatabase): IdeaDao = db.ideaDao()
    @Provides fun provideAgentDao(db: IdeaCompostDatabase): AgentDao = db.agentDao()
    @Provides fun provideProbioticDao(db: IdeaCompostDatabase): ProbioticDao = db.probioticDao()
    @Provides fun provideBedEventDao(db: IdeaCompostDatabase): BedEventDao = db.bedEventDao()
    @Provides fun provideCompostDao(db: IdeaCompostDatabase): CompostDao = db.compostDao()

    @Provides
    @Singleton
    fun providePrefs(@ApplicationContext context: Context): SharedPreferences =
        context.getSharedPreferences("ideacompost", Context.MODE_PRIVATE)
}
