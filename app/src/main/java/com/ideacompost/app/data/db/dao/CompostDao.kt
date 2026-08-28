package com.ideacompost.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.ideacompost.app.data.db.entity.CompostEntity
import com.ideacompost.app.data.db.entity.CompostStageEntity
import com.ideacompost.app.data.db.entity.FeedbackEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CompostDao {
    @Insert
    suspend fun insert(compost: CompostEntity)

    @Update
    suspend fun update(compost: CompostEntity)

    @Query("SELECT * FROM composts ORDER BY created_at DESC")
    fun observeAll(): Flow<List<CompostEntity>>

    @Query("SELECT * FROM composts WHERE id = :id")
    fun observeById(id: String): Flow<CompostEntity?>

    @Query("SELECT * FROM composts WHERE id = :id")
    suspend fun getById(id: String): CompostEntity?

    @Query("UPDATE composts SET status = :status, current_stage = :stage, updated_at = :now WHERE id = :id")
    suspend fun updateProgress(id: String, status: String, stage: String, now: Long)

    @Query("UPDATE composts SET title = :title, output_json = :output, status = :status, current_stage = :stage, updated_at = :now WHERE id = :id")
    suspend fun completeOutput(id: String, title: String, output: String, status: String, stage: String, now: Long)

    @Query("UPDATE composts SET nutrition_json = :nutrition, status = :status, current_stage = :stage, updated_at = :now WHERE id = :id")
    suspend fun setNutrition(id: String, nutrition: String, status: String, stage: String, now: Long)

    @Query("UPDATE composts SET status = :status, error = :error, updated_at = :now WHERE id = :id")
    suspend fun fail(id: String, status: String, error: String?, now: Long)

    @Insert
    suspend fun insertStage(stage: CompostStageEntity)

    @Query("SELECT * FROM compost_stages WHERE compost_id = :compostId ORDER BY id")
    suspend fun stages(compostId: String): List<CompostStageEntity>

    @Query("SELECT * FROM compost_stages WHERE compost_id = :compostId ORDER BY id")
    fun observeStages(compostId: String): Flow<List<CompostStageEntity>>

    @Insert
    suspend fun insertFeedback(event: FeedbackEventEntity)

    @Query("SELECT * FROM feedback_events WHERE compost_id = :compostId")
    suspend fun feedbacks(compostId: String): List<FeedbackEventEntity>

    @Query("SELECT COUNT(*) FROM feedback_events WHERE compost_id = :compostId")
    fun observeFeedbackCount(compostId: String): Flow<Int>

    /** 失败重试：回到 pending（保留已有 output 的场景不会走到这里）。 */
    @Query("UPDATE composts SET status = 'pending', current_stage = 'preflight', error = NULL, updated_at = :now WHERE id = :id AND status = 'failed'")
    suspend fun resetForRetry(id: String, now: Long)

    /** 重试时清空阶段缓存，全部重新发酵（宁多花调用，不留坏中间态）。 */
    @Query("DELETE FROM compost_stages WHERE compost_id = :compostId")
    suspend fun clearStages(compostId: String)

    /** 已完成的堆肥——夜间生态任务的共现统计源。 */
    @Query("SELECT * FROM composts WHERE status = 'done' ORDER BY created_at DESC LIMIT 200")
    suspend fun allDone(): List<CompostEntity>
}
