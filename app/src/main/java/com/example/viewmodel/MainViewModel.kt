package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AstraDatabase
import com.example.data.local.AstraRepository
import com.example.data.local.CourseJsonCodec
import com.example.data.local.SettingsDataStore
import com.example.data.local.StudyLogEntity
import com.example.data.seed.KtctModesSeed
import com.example.model.*
import com.example.service.GeminiDomainUseCases
import com.example.service.LargePdfStrategy
import com.example.service.SelectedDocumentInfo
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

data class ActiveSessionState(
    val mode: LearningMode,
    val nodeId: String? = null
)

data class IngestionProgressState(
    val isRunning: Boolean = false,
    val stageIndex: Int = 0, // 1, 2, 3
    val message: String = "",
    val error: String? = null
)

data class ModelDiscoveryState(
    val isChecking: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null
)

data class StudyCalendarDay(
    val label: String,
    val dateIso: String,
    val completedCount: Int,
    val isToday: Boolean
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AstraDatabase.getInstance(application)
    private val settingsStore = SettingsDataStore(application)
    val repository = AstraRepository(db.astraDao(), settingsStore)
    val geminiUseCases = GeminiDomainUseCases()

    private val _selectedCourseId = MutableStateFlow(KtctModesSeed.KTCT_COURSE_ID)
    private val _activeTab = MutableStateFlow(MainTab.ROADMAP)
    val activeTab: StateFlow<MainTab> = _activeTab.asStateFlow()

    private val _activeSession = MutableStateFlow<ActiveSessionState?>(null)
    val activeSession: StateFlow<ActiveSessionState?> = _activeSession.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _ingestionState = MutableStateFlow(IngestionProgressState())
    val ingestionState: StateFlow<IngestionProgressState> = _ingestionState.asStateFlow()

    private val _modelDiscovery = MutableStateFlow(ModelDiscoveryState())
    val modelDiscovery: StateFlow<ModelDiscoveryState> = _modelDiscovery.asStateFlow()

    // Modals visibility
    private val _showAiSettings = MutableStateFlow(false)
    val showAiSettings: StateFlow<Boolean> = _showAiSettings.asStateFlow()

    private val _showIngestModal = MutableStateFlow(false)
    val showIngestModal: StateFlow<Boolean> = _showIngestModal.asStateFlow()

    private val _showShareModal = MutableStateFlow(false)
    val showShareModal: StateFlow<Boolean> = _showShareModal.asStateFlow()

    private val _showDuelModal = MutableStateFlow(false)
    val showDuelModal: StateFlow<Boolean> = _showDuelModal.asStateFlow()

    private val _showModeSheet = MutableStateFlow(false)
    val showModeSheet: StateFlow<Boolean> = _showModeSheet.asStateFlow()

    val settings: StateFlow<AiSettings> = repository.settingsFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), AiSettings()
    )

    val courses: StateFlow<List<Course>> = repository.coursesFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val currentCourse: StateFlow<Course> = combine(courses, _selectedCourseId) { list, selectedId ->
        list.find { it.id == selectedId } ?: list.firstOrNull() ?: KtctModesSeed.buildFlagshipCourse()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), KtctModesSeed.buildFlagshipCourse())

    val remediations: StateFlow<List<RemediationRecord>> = combine(
        repository.allRemediationsFlow,
        currentCourse
    ) { all, course ->
        all.filter { it.courseId == course.id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val studyLogs: StateFlow<List<StudyLogEntity>> = repository.studyLogsFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val dailyGoals: StateFlow<DailyGoalState> = studyLogs.map { logs ->
        val today = repository.todayIso()
        val log = logs.find { it.dateIso == today }
        DailyGoalState(
            smartTextCompletedToday = log?.smartTextCompleted ?: 0,
            smartTextTarget = 1,
            flashcardsReviewedToday = log?.flashcardsReviewed ?: 0,
            flashcardsTarget = 5,
            quizzesCompletedToday = log?.quizzesCompleted ?: 0,
            quizzesTarget = 1
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DailyGoalState())

    val studyCalendar: StateFlow<List<StudyCalendarDay>> = studyLogs.map { logs ->
        val map = logs.associateBy { it.dateIso }
        val isoFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val dayFmt = SimpleDateFormat("EEE", Locale("vi", "VN"))
        val now = System.currentTimeMillis()
        val dayMs = 86_400_000L
        (6 downTo 0).map { offset ->
            val d = Date(now - offset * dayMs)
            val iso = isoFmt.format(d)
            val log = map[iso]
            val count = (log?.smartTextCompleted ?: 0) + (log?.quizzesCompleted ?: 0) + if ((log?.flashcardsReviewed ?: 0) > 0) 1 else 0
            StudyCalendarDay(
                label = if (offset == 0) "Nay" else dayFmt.format(d).replace("Th ", "T"),
                dateIso = iso,
                completedCount = count,
                isToday = offset == 0
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val duelHistory: StateFlow<List<DuelHistoryRecord>> = repository.duelHistoryFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    init {
        viewModelScope.launch {
            repository.ensureSeeded()
        }
    }

    fun selectTab(tab: MainTab) {
        _activeTab.value = tab
    }

    fun selectCourse(courseId: String) {
        _selectedCourseId.value = courseId
    }

    fun openMode(mode: LearningMode, nodeId: String? = null) {
        _showModeSheet.value = false
        _activeSession.value = ActiveSessionState(mode = mode, nodeId = nodeId)
    }

    fun closeSession() {
        _activeSession.value = null
    }

    fun openAiSettings(show: Boolean) {
        _showAiSettings.value = show
        if (!show) _modelDiscovery.value = ModelDiscoveryState()
    }

    fun openIngestModal(show: Boolean) {
        _showIngestModal.value = show
        if (!show) _ingestionState.value = IngestionProgressState()
    }

    fun openShareModal(show: Boolean) {
        _showShareModal.value = show
    }

    fun openDuelModal(show: Boolean) {
        _showDuelModal.value = show
    }

    fun openModeSheet(show: Boolean) {
        _showModeSheet.value = show
    }

    fun showToast(message: String) {
        _toastMessage.value = message
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun toggleWhitePaperExam() {
        viewModelScope.launch {
            repository.setWhitePaperExam(!settings.value.isWhitePaperExam)
        }
    }

    fun setTtsSpeed(speed: Float) {
        viewModelScope.launch {
            repository.setTtsSpeed(speed)
        }
    }

    fun saveAiSettings(apiKey: String, fastModel: String, deepModel: String) {
        viewModelScope.launch {
            repository.saveAiConfig(apiKey, fastModel, deepModel)
            showToast("Đã lưu cấu hình Gemini AI thành công!")
        }
    }

    fun testConnectionAndDiscoverModels(apiKeyInput: String) {
        viewModelScope.launch {
            _modelDiscovery.value = ModelDiscoveryState(isChecking = true)
            val result = geminiUseCases.discoverModels(apiKeyInput)
            result.fold(
                onSuccess = { models ->
                    repository.saveDiscoveredModels(models)
                    _modelDiscovery.value = ModelDiscoveryState(
                        isChecking = false,
                        successMessage = "Kết nối thành công! Đã tìm thấy ${models.size} model Gemini khả dụng."
                    )
                },
                onFailure = { err ->
                    _modelDiscovery.value = ModelDiscoveryState(
                        isChecking = false,
                        errorMessage = err.message ?: "Không thể kết nối tới Gemini API."
                    )
                }
            )
        }
    }

    fun updateExamPlan(examDaysLeft: Int, targetScore: Double) {
        viewModelScope.launch {
            val updated = currentCourse.value.copy(
                examDaysLeft = examDaysLeft.coerceIn(1, 365),
                targetScore = targetScore.coerceIn(5.0, 10.0)
            )
            repository.saveCourse(updated)
            showToast("Đã cập nhật mục tiêu ${String.format("%.1f", updated.targetScore)}/10 · còn ${updated.examDaysLeft} ngày thi.")
        }
    }

    fun completeSmartNode(nodeId: String, accuracyPercent: Int, starsEarned: Int) {
        viewModelScope.launch {
            val course = currentCourse.value
            val updatedNodes = course.nodes.map { node ->
                if (node.id == nodeId) {
                    node.copy(
                        isCompleted = true,
                        masteryPercent = maxOf(node.masteryPercent, accuracyPercent.coerceIn(55, 100)),
                        lastStudiedEpoch = System.currentTimeMillis()
                    )
                } else node
            }
            val avgMastery = if (updatedNodes.isNotEmpty()) {
                updatedNodes.sumOf { it.masteryPercent } / updatedNodes.size
            } else course.readinessPercent
            val updatedCourse = course.copy(
                nodes = updatedNodes,
                readinessPercent = maxOf(course.readinessPercent, avgMastery).coerceIn(0, 100),
                starsEarned = course.starsEarned + starsEarned
            )
            repository.saveCourse(updatedCourse)
            repository.recordActivity(smartTextDelta = 1, starsDelta = starsEarned)
        }
    }

    fun recordQuizCompletion(scorePercent: Int, starsEarned: Int) {
        viewModelScope.launch {
            val course = currentCourse.value
            val boost = (scorePercent / 14).coerceIn(2, 10)
            repository.saveCourse(
                course.copy(
                    readinessPercent = (course.readinessPercent + boost).coerceAtMost(100),
                    starsEarned = course.starsEarned + starsEarned
                )
            )
            repository.recordActivity(quizDelta = 1, starsDelta = starsEarned)
        }
    }

    fun rateFlashcard(cardId: String, rating: SrsRating) {
        viewModelScope.launch {
            val course = currentCourse.value
            val updatedCards = course.flashcards.map { c ->
                if (c.id == cardId) {
                    c.copy(
                        masteryLevel = (c.masteryLevel + rating.deltaMastery).coerceIn(0, 100),
                        nextReviewEpoch = System.currentTimeMillis() + when (rating) {
                            SrsRating.FORGOT -> 60_000L * 10
                            SrsRating.HARD -> 3_600_000L * 12
                            SrsRating.GOOD -> 86_400_000L * 2
                            SrsRating.EASY -> 86_400_000L * 5
                        }
                    )
                } else c
            }
            repository.saveCourse(course.copy(flashcards = updatedCards, starsEarned = course.starsEarned + 5))
            repository.recordActivity(flashcardsDelta = 1, starsDelta = 5)
        }
    }

    fun addFlashcardToCurrentCourse(card: FlashcardItem) {
        viewModelScope.launch {
            val course = currentCourse.value
            val updated = course.copy(flashcards = listOf(card.copy(courseId = course.id)) + course.flashcards)
            repository.saveCourse(updated)
            showToast("🗂️ Đã lưu thẻ ghi nhớ mới vào kho '${course.title}'!")
        }
    }

    fun logMistake(nodeId: String, conceptTag: String, originalQuestion: String, mistakeSummary: String) {
        viewModelScope.launch {
            repository.logMistake(
                courseId = currentCourse.value.id,
                nodeId = nodeId,
                conceptTag = conceptTag,
                originalQuestion = originalQuestion,
                mistakeSummary = mistakeSummary
            )
        }
    }

    fun resolveGap(id: Long) {
        viewModelScope.launch {
            repository.resolveRemediation(id)
            showToast("✅ Đã đánh dấu vá xong lỗ hổng kiến thức!")
        }
    }

    fun ingestNewCourse(
        title: String,
        examDays: Int,
        targetScore: Double,
        docs: List<SelectedDocumentInfo>,
        largePdfStrategy: LargePdfStrategy,
        pageStart: Int,
        pageEnd: Int
    ) {
        viewModelScope.launch {
            val currentSettings = settings.value
            if (currentSettings.apiKey.isBlank()) {
                _ingestionState.value = IngestionProgressState(
                    isRunning = false,
                    error = "Bạn chưa nhập Gemini API Key. Hãy mở Cài đặt AI để dán khóa trước khi nạp tài liệu mới."
                )
                return@launch
            }
            if (docs.isEmpty()) {
                _ingestionState.value = IngestionProgressState(
                    isRunning = false,
                    error = "Hãy chọn ít nhất 1 tệp PDF, DOCX, TXT, Markdown hoặc ảnh trang sách."
                )
                return@launch
            }
            _ingestionState.value = IngestionProgressState(
                isRunning = true,
                stageIndex = 1,
                message = "Đang đọc cấu trúc tài liệu & trích xuất khái niệm cốt lõi..."
            )
            val result = geminiUseCases.ingestDocumentsToCourse(
                context = getApplication(),
                settings = currentSettings,
                courseTitle = title.ifBlank { docs.first().displayName.substringBeforeLast('.') },
                examDaysLeft = examDays,
                targetScore = targetScore,
                documents = docs,
                largePdfStrategy = largePdfStrategy,
                pageStart = pageStart,
                pageEnd = pageEnd,
                onProgress = { stage, msg ->
                    _ingestionState.value = IngestionProgressState(
                        isRunning = true,
                        stageIndex = stage,
                        message = msg
                    )
                }
            )
            result.fold(
                onSuccess = { newCourse ->
                    repository.saveCourse(newCourse)
                    _selectedCourseId.value = newCourse.id
                    _ingestionState.value = IngestionProgressState(isRunning = false)
                    _showIngestModal.value = false
                    _activeTab.value = MainTab.ROADMAP
                    showToast("🎉 Đã tạo xong khóa học '${newCourse.title}' với ${newCourse.nodes.size} trạm học!")
                },
                onFailure = { err ->
                    _ingestionState.value = IngestionProgressState(
                        isRunning = false,
                        error = err.message ?: "Có lỗi khi phân tích tài liệu."
                    )
                }
            )
        }
    }

    fun exportCurrentCourseShareString(): String = CourseJsonCodec.exportShareString(currentCourse.value)

    fun importCourseFromShareString(raw: String): Boolean {
        return runCatching {
            val imported = CourseJsonCodec.importShareString(raw)
            val unique = imported.copy(id = "imported_${System.currentTimeMillis()}")
            viewModelScope.launch {
                repository.saveCourse(unique)
                _selectedCourseId.value = unique.id
                showToast("📥 Đã nhập thành công khóa học '${unique.title}'!")
            }
            true
        }.getOrElse {
            showToast("⚠️ Chuỗi JSON khóa học không hợp lệ. Vui lòng kiểm tra lại.")
            false
        }
    }

    fun buildDuelQuestionsForCurrentCourse(): List<QuizQuestion> {
        val course = currentCourse.value
        val pool = (course.quizBank + course.nodes.flatMap { it.finalQuiz }).distinctBy { it.question }
        return if (pool.size >= 5) pool.shuffled().take(5) else pool
    }

    fun createEncodedDuelCode(creatorName: String, score: Int, timeSec: Int, questions: List<QuizQuestion>): String {
        val shortCode = "KTCT-${('A'..'Z').random()}${Random.nextInt(100, 999)}"
        val payload = DuelPayload(
            code = shortCode,
            courseTitle = currentCourse.value.title,
            creatorName = creatorName.ifBlank { "Bạn mình" },
            creatorScore = score,
            creatorTimeSec = timeSec,
            questions = questions
        )
        return CourseJsonCodec.encodeDuelCode(payload)
    }

    fun decodeDuelCode(raw: String): Result<DuelPayload> = runCatching {
        CourseJsonCodec.decodeDuelCode(raw)
    }

    fun saveDuelOutcome(
        code: String,
        courseTitle: String,
        myScore: Int,
        opponentScore: Int,
        myTimeSec: Int,
        opponentTimeSec: Int,
        opponentName: String
    ) {
        viewModelScope.launch {
            repository.saveDuelHistory(
                DuelHistoryRecord(
                    code = code,
                    courseTitle = courseTitle,
                    myScore = myScore,
                    opponentScore = opponentScore,
                    myTimeSec = myTimeSec,
                    opponentTimeSec = opponentTimeSec,
                    opponentName = opponentName
                )
            )
            repository.recordActivity(quizDelta = 1, starsDelta = 35)
        }
    }
}
