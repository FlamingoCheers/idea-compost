package com.ideacompost.app.di

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.room.Room
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.ideacompost.app.data.db.IdeaCompostDatabase
import com.ideacompost.app.data.db.dao.AgentDao
import com.ideacompost.app.data.db.dao.BedEventDao
import com.ideacompost.app.data.db.dao.CompostDao
import com.ideacompost.app.data.db.dao.IdeaDao
import com.ideacompost.app.data.db.dao.LlmCallDao
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
    @Provides fun provideLlmCallDao(db: IdeaCompostDatabase): LlmCallDao = db.llmCallDao()

    /**
     * BYO Key 存储（specs/02 §6）：EncryptedSharedPreferences（AES256-SIV 键 / AES256-GCM 值）。
     * 兼容路径：旧明文 prefs 自动迁移后清空；加密文件损坏时重置重建。
     */
    @Provides
    @Singleton
    fun providePrefs(@ApplicationContext context: Context): SharedPreferences {
        val legacy = context.getSharedPreferences("ideacompost", Context.MODE_PRIVATE)
        fun createEncrypted(): SharedPreferences {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            return EncryptedSharedPreferences.create(
                context,
                "ideacompost_secure",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }
        val encrypted = try {
            createEncrypted()
        } catch (t: Throwable) {
            Log.w("AppModule", "encrypted prefs broken, resetting", t)
            context.deleteSharedPreferences("ideacompost_secure")
            runCatching { createEncrypted() }.getOrElse {
                Log.e("AppModule", "encrypted prefs unavailable, falling back to plain", it)
                return context.getSharedPreferences("ideacompost_fallback", Context.MODE_PRIVATE)
            }
        }
        // 迁移旧明文 Key 并抹掉
        if (legacy.contains("api_key")) {
            val e = encrypted.edit()
            for (k in legacy.all.keys) {
                val v = legacy.all[k] ?: continue
                when (v) {
                    is String -> e.putString(k, v)
                    is Boolean -> e.putBoolean(k, v)
                    is Int -> e.putInt(k, v)
                    is Long -> e.putLong(k, v)
                    is Float -> e.putFloat(k, v)
                }
            }
            e.apply()
            legacy.edit().clear().commit()
            Log.i("AppModule", "migrated legacy plaintext prefs → encrypted, cleared legacy")
        }
        return encrypted
    }
}
