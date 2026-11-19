package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "courses")
data class CourseEntity(
    @PrimaryKey val id: String,
    val title: String,
    val payloadJson: String,
    val updatedAtEpoch: Long = System.currentTimeMillis()
)

@Entity(tableName = "remediations")
data class RemediationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val courseId: String,
    val nodeId: String,
    val conceptTag: String,
    val originalQuestion: String,
    val mistakeSummary: String,
    val wrongCount: Int = 1,
    val resolved: Boolean = false,
    val updatedAtEpoch: Long = System.currentTimeMillis()
)

@Entity(tableName = "tutor_messages")
data class TutorMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val courseId: String,
    val isUser: Boolean,
    val modeName: String,
    val text: String,
    val attachedImageUri: String? = null,
    val timestampEpoch: Long = System.currentTimeMillis()
)

@Entity(tableName = "study_logs")
data class StudyLogEntity(
    @PrimaryKey val dateIso: String, // YYYY-MM-DD
    val smartTextCompleted: Int = 0,
    val flashcardsReviewed: Int = 0,
    val quizzesCompleted: Int = 0,
    val starsEarned: Int = 0
)

@Entity(tableName = "duel_history")
data class DuelHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val code: String,
    val courseTitle: String,
    val myScore: Int,
    val opponentScore: Int,
    val myTimeSec: Int,
    val opponentTimeSec: Int,
    val opponentName: String,
    val playedAtEpoch: Long = System.currentTimeMillis()
)

@Dao
interface AstraDao {
    @Query("SELECT * FROM courses ORDER BY updatedAtEpoch DESC")
    fun observeCourses(): Flow<List<CourseEntity>>

    @Query("SELECT COUNT(*) FROM courses")
    suspend fun countCourses(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCourse(entity: CourseEntity)

    @Query("DELETE FROM courses WHERE id = :courseId")
    suspend fun deleteCourse(courseId: String)

    @Query("SELECT * FROM remediations WHERE courseId = :courseId ORDER BY wrongCount DESC, updatedAtEpoch DESC")
    fun observeRemediations(courseId: String): Flow<List<RemediationEntity>>

    @Query("SELECT * FROM remediations ORDER BY wrongCount DESC, updatedAtEpoch DESC")
    fun observeAllRemediations(): Flow<List<RemediationEntity>>

    @Query("SELECT * FROM remediations WHERE courseId = :courseId AND conceptTag = :conceptTag LIMIT 1")
    suspend fun findRemediation(courseId: String, conceptTag: String): RemediationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRemediation(entity: RemediationEntity)

    @Query("UPDATE remediations SET resolved = 1 WHERE id = :id")
    suspend fun markRemediationResolved(id: Long)

    @Query("SELECT * FROM tutor_messages WHERE courseId = :courseId ORDER BY timestampEpoch ASC")
    fun observeTutorMessages(courseId: String): Flow<List<TutorMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTutorMessage(entity: TutorMessageEntity)

    @Query("DELETE FROM tutor_messages WHERE courseId = :courseId")
    suspend fun clearTutorMessages(courseId: String)

    @Query("SELECT * FROM study_logs ORDER BY dateIso DESC LIMIT 14")
    fun observeRecentStudyLogs(): Flow<List<StudyLogEntity>>

    @Query("SELECT * FROM study_logs WHERE dateIso = :dateIso LIMIT 1")
    suspend fun getStudyLog(dateIso: String): StudyLogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStudyLog(entity: StudyLogEntity)

    @Query("SELECT * FROM duel_history ORDER BY playedAtEpoch DESC LIMIT 20")
    fun observeDuelHistory(): Flow<List<DuelHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDuelHistory(entity: DuelHistoryEntity)
}

@Database(
    entities = [
        CourseEntity::class,
        RemediationEntity::class,
        TutorMessageEntity::class,
        StudyLogEntity::class,
        DuelHistoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AstraDatabase : RoomDatabase() {
    abstract fun astraDao(): AstraDao

    companion object {
        @Volatile
        private var INSTANCE: AstraDatabase? = null

        fun getInstance(context: Context): AstraDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AstraDatabase::class.java,
                    "astraflow_local.db"
                ).fallbackToDestructiveMigration(dropAllTables = true).build().also { INSTANCE = it }
            }
    }
}
