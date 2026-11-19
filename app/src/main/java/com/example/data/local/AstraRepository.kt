package com.example.data.local

import com.example.data.seed.KtctModesSeed
import com.example.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AstraRepository(
    private val dao: AstraDao,
    private val settingsStore: SettingsDataStore
) {
    val settingsFlow: Flow<AiSettings> = settingsStore.settingsFlow

    val coursesFlow: Flow<List<Course>> = dao.observeCourses().map { entities ->
        entities.mapNotNull { runCatching { CourseJsonCodec.decodeCourse(it.payloadJson) }.getOrNull() }
    }

    val allRemediationsFlow: Flow<List<RemediationRecord>> = dao.observeAllRemediations().map { list ->
        list.map {
            RemediationRecord(
                id = it.id,
                courseId = it.courseId,
                nodeId = it.nodeId,
                conceptTag = it.conceptTag,
                originalQuestion = it.originalQuestion,
                mistakeSummary = it.mistakeSummary,
                wrongCount = it.wrongCount,
                resolved = it.resolved,
                updatedAtEpoch = it.updatedAtEpoch
            )
        }
    }

    val studyLogsFlow: Flow<List<StudyLogEntity>> = dao.observeRecentStudyLogs()

    val duelHistoryFlow: Flow<List<DuelHistoryRecord>> = dao.observeDuelHistory().map { list ->
        list.map {
            DuelHistoryRecord(
                id = it.id,
                code = it.code,
                courseTitle = it.courseTitle,
                myScore = it.myScore,
                opponentScore = it.opponentScore,
                myTimeSec = it.myTimeSec,
                opponentTimeSec = it.opponentTimeSec,
                opponentName = it.opponentName,
                playedAtEpoch = it.playedAtEpoch
            )
        }
    }

    fun observeTutorMessages(courseId: String): Flow<List<TutorChatMessage>> =
        dao.observeTutorMessages(courseId).map { list ->
            list.map {
                TutorChatMessage(
                    id = it.id,
                    courseId = it.courseId,
                    isUser = it.isUser,
                    mode = runCatching { TutorMode.valueOf(it.modeName) }.getOrDefault(TutorMode.SOCRATIC),
                    text = it.text,
                    attachedImageUri = it.attachedImageUri,
                    timestampEpoch = it.timestampEpoch
                )
            }
        }

    suspend fun ensureSeeded() {
        if (dao.countCourses() == 0) {
            val ktct = KtctModesSeed.buildFlagshipCourse()
            saveCourse(ktct)
            KtctModesSeed.initialRemediations().forEach { rec ->
                dao.upsertRemediation(
                    RemediationEntity(
                        courseId = rec.courseId,
                        nodeId = rec.nodeId,
                        conceptTag = rec.conceptTag,
                        originalQuestion = rec.originalQuestion,
                        mistakeSummary = rec.mistakeSummary,
                        wrongCount = rec.wrongCount
                    )
                )
            }
            // Pre-populate 3 recent study days so the calendar & streak look alive immediately
            val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val now = System.currentTimeMillis()
            val dayMs = 86_400_000L
            for (offset in 1..2) {
                val iso = fmt.format(Date(now - offset * dayMs))
                dao.upsertStudyLog(
                    StudyLogEntity(
                        dateIso = iso,
                        smartTextCompleted = 1,
                        flashcardsReviewed = 6,
                        quizzesCompleted = 1,
                        starsEarned = 85
                    )
                )
            }
        }
    }

    suspend fun saveCourse(course: Course) {
        dao.upsertCourse(
            CourseEntity(
                id = course.id,
                title = course.title,
                payloadJson = CourseJsonCodec.encodeCourse(course),
                updatedAtEpoch = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteCourse(courseId: String) = dao.deleteCourse(courseId)

    suspend fun logMistake(
        courseId: String,
        nodeId: String,
        conceptTag: String,
        originalQuestion: String,
        mistakeSummary: String
    ) {
        val existing = dao.findRemediation(courseId, conceptTag)
        if (existing != null) {
            dao.upsertRemediation(
                existing.copy(
                    wrongCount = existing.wrongCount + 1,
                    originalQuestion = originalQuestion,
                    mistakeSummary = mistakeSummary,
                    resolved = false,
                    updatedAtEpoch = System.currentTimeMillis()
                )
            )
        } else {
            dao.upsertRemediation(
                RemediationEntity(
                    courseId = courseId,
                    nodeId = nodeId,
                    conceptTag = conceptTag,
                    originalQuestion = originalQuestion,
                    mistakeSummary = mistakeSummary,
                    wrongCount = 1
                )
            )
        }
    }

    suspend fun resolveRemediation(id: Long) = dao.markRemediationResolved(id)

    suspend fun addTutorMessage(msg: TutorChatMessage) {
        dao.insertTutorMessage(
            TutorMessageEntity(
                courseId = msg.courseId,
                isUser = msg.isUser,
                modeName = msg.mode.name,
                text = msg.text,
                attachedImageUri = msg.attachedImageUri,
                timestampEpoch = msg.timestampEpoch
            )
        )
    }

    suspend fun clearTutorHistory(courseId: String) = dao.clearTutorMessages(courseId)

    suspend fun recordActivity(
        smartTextDelta: Int = 0,
        flashcardsDelta: Int = 0,
        quizDelta: Int = 0,
        starsDelta: Int = 0
    ) {
        val today = todayIso()
        val current = dao.getStudyLog(today) ?: StudyLogEntity(dateIso = today)
        dao.upsertStudyLog(
            current.copy(
                smartTextCompleted = current.smartTextCompleted + smartTextDelta,
                flashcardsReviewed = current.flashcardsReviewed + flashcardsDelta,
                quizzesCompleted = current.quizzesCompleted + quizDelta,
                starsEarned = current.starsEarned + starsDelta
            )
        )
        settingsStore.recordDailyStreak(today)
    }

    suspend fun saveDuelHistory(record: DuelHistoryRecord) {
        dao.insertDuelHistory(
            DuelHistoryEntity(
                code = record.code,
                courseTitle = record.courseTitle,
                myScore = record.myScore,
                opponentScore = record.opponentScore,
                myTimeSec = record.myTimeSec,
                opponentTimeSec = record.opponentTimeSec,
                opponentName = record.opponentName,
                playedAtEpoch = record.playedAtEpoch
            )
        )
    }

    suspend fun saveAiConfig(apiKey: String, fastModel: String, deepModel: String) =
        settingsStore.saveAiConfig(apiKey, fastModel, deepModel)

    suspend fun saveDiscoveredModels(models: List<String>) =
        settingsStore.saveDiscoveredModels(models)

    suspend fun setWhitePaperExam(enabled: Boolean) = settingsStore.setWhitePaperExam(enabled)

    suspend fun setTtsSpeed(speed: Float) = settingsStore.setTtsSpeed(speed)

    fun todayIso(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
}
