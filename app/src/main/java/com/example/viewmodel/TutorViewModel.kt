package com.example.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AstraRepository
import com.example.model.*
import com.example.service.GeminiDomainUseCases
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class TutorViewModel(application: Application) : AndroidViewModel(application) {
    private val gemini = GeminiDomainUseCases()
    private var repository: AstraRepository? = null

    private val _boundCourse = MutableStateFlow<Course?>(null)
    val boundCourse: StateFlow<Course?> = _boundCourse.asStateFlow()

    private val _tutorMode = MutableStateFlow(TutorMode.SOCRATIC)
    val tutorMode: StateFlow<TutorMode> = _tutorMode.asStateFlow()

    private val _attachedImageUri = MutableStateFlow<Uri?>(null)
    val attachedImageUri: StateFlow<Uri?> = _attachedImageUri.asStateFlow()

    private val _isThinking = MutableStateFlow(false)
    val isThinking: StateFlow<Boolean> = _isThinking.asStateFlow()

    private val _errorBanner = MutableStateFlow<String?>(null)
    val errorBanner: StateFlow<String?> = _errorBanner.asStateFlow()

    private val _quickQuizModal = MutableStateFlow<List<QuizQuestion>?>(null)
    val quickQuizModal: StateFlow<List<QuizQuestion>?> = _quickQuizModal.asStateFlow()

    private val _messagesFlow = MutableStateFlow<List<TutorChatMessage>>(emptyList())
    val messages: StateFlow<List<TutorChatMessage>> = _messagesFlow.asStateFlow()

    private var observingCourseId: String? = null

    fun attachRepository(repo: AstraRepository, course: Course) {
        repository = repo
        if (_boundCourse.value?.id != course.id) {
            _boundCourse.value = course
        }
        if (observingCourseId != course.id) {
            observingCourseId = course.id
            viewModelScope.launch {
                repo.observeTutorMessages(course.id).collect { list ->
                    _messagesFlow.value = list
                }
            }
        }
    }

    fun selectContextCourse(course: Course?) {
        _boundCourse.value = course
        val repo = repository ?: return
        val cid = course?.id ?: "general"
        if (observingCourseId != cid) {
            observingCourseId = cid
            viewModelScope.launch {
                repo.observeTutorMessages(cid).collect { _messagesFlow.value = it }
            }
        }
    }

    fun setTutorMode(mode: TutorMode) {
        _tutorMode.value = mode
    }

    fun setAttachedImage(uri: Uri?) {
        _attachedImageUri.value = uri
    }

    fun dismissQuickQuiz() {
        _quickQuizModal.value = null
    }

    fun clearError() {
        _errorBanner.value = null
    }

    fun clearConversation() {
        val repo = repository ?: return
        val cid = _boundCourse.value?.id ?: "general"
        viewModelScope.launch { repo.clearTutorHistory(cid) }
    }

    fun sendMessage(
        userText: String,
        settings: AiSettings,
        overrideMode: TutorMode? = null
    ) {
        val repo = repository ?: return
        val mode = overrideMode ?: _tutorMode.value
        val imageUri = _attachedImageUri.value
        if (userText.isBlank() && imageUri == null) return

        val course = _boundCourse.value
        val cid = course?.id ?: "general"
        val cleanText = userText.trim().ifBlank { "📷 [Đã gửi ảnh đề bài — Snap-to-Solve]" }

        viewModelScope.launch {
            _errorBanner.value = null
            val userMsg = TutorChatMessage(
                courseId = cid,
                isUser = true,
                mode = mode,
                text = cleanText,
                attachedImageUri = imageUri?.toString()
            )
            repo.addTutorMessage(userMsg)
            _attachedImageUri.value = null
            _isThinking.value = true

            if (settings.apiKey.isBlank()) {
                val offlineReply = buildOfflineSocraticFallback(cleanText, mode, course)
                repo.addTutorMessage(
                    TutorChatMessage(
                        courseId = cid,
                        isUser = false,
                        mode = mode,
                        text = offlineReply
                    )
                )
                _isThinking.value = false
                return@launch
            }

            val result = gemini.askTutor(
                context = getApplication(),
                settings = settings,
                mode = mode,
                courseContext = course,
                history = _messagesFlow.value,
                userQuestion = cleanText,
                attachedImageUri = imageUri
            )
            result.fold(
                onSuccess = { reply ->
                    repo.addTutorMessage(
                        TutorChatMessage(
                            courseId = cid,
                            isUser = false,
                            mode = mode,
                            text = reply
                        )
                    )
                },
                onFailure = { err ->
                    _errorBanner.value = err.message ?: "Không thể kết nối Gia sư AI."
                }
            )
            _isThinking.value = false
        }
    }

    fun explainSimplerWay(originalReply: String, settings: AiSettings) {
        sendMessage(
            userText = "Hãy giải thích lại ý trên theo một cách khác dễ hiểu hơn, dùng ví dụ đời thường ngắn gọn: \"${originalReply.take(240)}\"",
            settings = settings,
            overrideMode = TutorMode.FAST
        )
    }

    fun generateQuickQuizFromMessage(replyText: String, settings: AiSettings) {
        viewModelScope.launch {
            _isThinking.value = true
            _errorBanner.value = null
            val courseTitle = _boundCourse.value?.title ?: "Kinh tế chính trị Mác - Lênin"
            if (settings.apiKey.isBlank()) {
                val fallback = _boundCourse.value?.quizBank?.take(3).orEmpty()
                if (fallback.isNotEmpty()) {
                    _quickQuizModal.value = fallback
                } else {
                    _errorBanner.value = "Hãy dán Gemini API Key trong Cài đặt AI để tạo Quiz trực tiếp từ đoạn chat này."
                }
                _isThinking.value = false
                return@launch
            }
            gemini.generateQuickQuizFromReply(settings, replyText, courseTitle).fold(
                onSuccess = { _quickQuizModal.value = it },
                onFailure = { _errorBanner.value = it.message ?: "Không tạo được Quiz nhanh." }
            )
            _isThinking.value = false
        }
    }

    fun generateFlashcardFromMessage(
        replyText: String,
        settings: AiSettings,
        onCardCreated: (FlashcardItem) -> Unit
    ) {
        viewModelScope.launch {
            _isThinking.value = true
            _errorBanner.value = null
            val course = _boundCourse.value
            val cid = course?.id ?: "course_ktct_flagship"
            val title = course?.title ?: "Kinh tế chính trị Mác - Lênin"
            if (settings.apiKey.isBlank()) {
                val localCard = FlashcardItem(
                    id = "fc_local_${System.currentTimeMillis()}",
                    courseId = cid,
                    front = "Tóm tắt cốt lõi từ Gia sư AI ($title)?",
                    back = replyText.take(220),
                    hint = "Ghi chú nhanh từ phòng chat",
                    sourceCitation = "✨ Lưu từ Gia sư AI"
                )
                onCardCreated(localCard)
                _isThinking.value = false
                return@launch
            }
            gemini.generateFlashcardFromReply(settings, replyText, cid, title).fold(
                onSuccess = { onCardCreated(it) },
                onFailure = { _errorBanner.value = it.message ?: "Không tạo được Flashcard." }
            )
            _isThinking.value = false
        }
    }

    private fun buildOfflineSocraticFallback(question: String, mode: TutorMode, course: Course?): String {
        val qLower = question.lowercase()
        val hintCore = when {
            qLower.contains("m'") || qLower.contains("p'") || qLower.contains("thặng dư") ->
                "Ta có m' = (m / v) × 100% và p' = m / (c + v) × 100%. Gợi ý: hãy để ý sự khác nhau ở mẫu số — một bên chỉ có v (tư bản khả biến), bên kia có tổng (c + v)."
            qLower.contains("năng suất") || qLower.contains("cường độ") ->
                "Quy tắc biến thiên: NSLĐ tăng → giá trị 1 sản phẩm giảm, tổng giá trị không đổi; còn CĐLĐ tăng → giá trị 1 sản phẩm không đổi, tổng giá trị tăng."
            qLower.contains("hàng hóa") || qLower.contains("lao động") ->
                "Lao động cụ thể tạo ra Giá trị sử dụng, còn Lao động trừu tượng tạo ra Giá trị hàng hóa."
            else ->
                "Mình đang gắn ngữ cảnh môn ${course?.title ?: "Kinh tế chính trị Mác - Lênin"}. Để mình phân tích sâu câu hỏi hoặc ảnh đề bài của bạn theo thời gian thực, hãy mở ⚙️ Cài đặt AI để dán Gemini API Key nhé!"
        }
        return if (mode == TutorMode.SOCRATIC) {
            "🧭 [Chế độ Socratic · Ngoại tuyến]\n$hintCore\n\nCâu hỏi gợi mở cho bạn: Theo bạn, trong tình huống trên yếu tố nào trực tiếp thay đổi mẫu số hay lượng giá trị mới (v + m)?"
        } else {
            "⚡ [Giải thích nhanh · Ngoại tuyến]\n$hintCore\n\n(Mẹo: Dán Gemini API Key trong ⚙️ Cài đặt AI để kích hoạt giải bài tập & đọc ảnh Snap-to-Solve không giới hạn!)"
        }
    }
}
