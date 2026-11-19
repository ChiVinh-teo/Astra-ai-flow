package com.example.model

enum class MainTab(val title: String) {
    ROADMAP("Lộ trình"),
    TUTOR("Gia sư AI"),
    PRACTICE("Luyện tập"),
    LIBRARY("Kho tài liệu")
}

enum class LearningModeGroup(val label: String) {
    ACQUIRE("HỌC"),
    PRACTICE("LUYỆN TẬP"),
    ASSESS("THI")
}

enum class LearningMode(
    val title: String,
    val subtitle: String,
    val badge: String?,
    val group: LearningModeGroup
) {
    SMART_TEXT("Văn bản thông minh", "8 bước vi mô + chốt chặn tức thì", "ĐỀ XUẤT", LearningModeGroup.ACQUIRE),
    PODCAST("Podcast 2 Host", "Hội thoại Host Minh & TS. Lan + TTS", null, LearningModeGroup.ACQUIRE),
    VIDEO_SIMULATOR("Video Mô phỏng Động", "Mô phỏng cơ chế & đồ họa tương tác", null, LearningModeGroup.ACQUIRE),
    FLASHCARDS("Thẻ ghi nhớ", "Lật 3D chủ động + đánh giá trí nhớ", null, LearningModeGroup.ACQUIRE),
    KNOWLEDGE_GAPS("Khoảng trống kiến thức", "Bản đồ nhiệt lỗ hổng & bài vá nhanh", null, LearningModeGroup.ACQUIRE),

    EXERCISES("Bài tập tính toán", "Giải từng bước c, v, m, m', p' & tình huống", null, LearningModeGroup.PRACTICE),
    ADAPTIVE_QUIZ("Trắc nghiệm thích ứng", "3 mức độ · Chọn đơn & nhiều đáp án", null, LearningModeGroup.PRACTICE),
    TRUE_FALSE_BLITZ("Đúng / Sai phản xạ", "10 thẻ vuốt + gạch bỏ đỏ trực quan", null, LearningModeGroup.PRACTICE),
    SPACED_REPETITION("Ôn tập ngắt quãng", "Lặp lại các trạm sắp quên & lỗi cũ", null, LearningModeGroup.PRACTICE),

    WRITTEN_EXAM("Đề thi thử viết", "80 phút · Gõ chữ hoặc chụp bài viết tay", "80 PHÚT", LearningModeGroup.ASSESS),
    ORAL_VIVA("Thi thử vấn đáp", "Giám khảo hỏi xoáy + micro giọng nói", null, LearningModeGroup.ASSESS)
}

enum class QuizDifficulty(val label: String) {
    BASIC("Cơ bản"),
    MEDIUM("Trung bình"),
    ADVANCED("Nâng cao")
}

enum class TutorMode(val label: String, val subtitle: String) {
    SOCRATIC("Gợi ý từng bước (Socratic)", "Dẫn dắt câu hỏi gợi mở, không lộ đáp án ngay"),
    FAST("Giải thích nhanh", "Trả lời thẳng trọng tâm kèm công thức chuẩn")
}

enum class PodcastSpeaker(val displayName: String, val role: String) {
    HOST_MINH("Host Minh", "Sinh viên hay hỏi xoáy & dễ dính bẫy"),
    TS_LAN("TS. Lan", "Giảng viên phân tích bản chất cơ chế")
}

enum class SrsRating(val label: String, val deltaMastery: Int) {
    FORGOT("Quên", -10),
    HARD("Khó", 5),
    GOOD("Tốt", 15),
    EASY("Dễ", 25)
}

data class ComparisonItem(
    val title: String,
    val tag: String,
    val points: List<String>,
    val accentHex: String = "#3B82F6"
)

/**
 * Structured mathematical formula so zero raw LaTeX (_ or ^) ever appears on screen.
 */
data class FormulaTerm(
    val text: String = "",
    val numerator: String? = null,
    val denominator: String? = null,
    val superscript: String? = null,
    val subscript: String? = null
)

data class FormulaBlock(
    val title: String,
    val terms: List<FormulaTerm>,
    val legend: List<String>
)

data class CausalityRule(
    val title: String,
    val arrows: List<String>,
    val note: String
)

data class InlineMcq(
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String,
    val conceptTag: String,
    val sourceCitation: String
)

/**
 * 3-part Strike-Through True/False card:
 * When isTrue == false, a crimson Canvas strike-through draws across wrongClause and correctClause slides in.
 * When isTrue == true, an emerald confirmation highlight keeps the statement intact.
 */
data class StrikeThroughTf(
    val id: String,
    val prefix: String,
    val wrongClause: String,
    val correctClause: String,
    val suffix: String = "",
    val isTrue: Boolean,
    val explanation: String,
    val conceptTag: String,
    val sourceCitation: String
)

data class SmartTextStep(
    val stepNumber: Int,
    val title: String,
    val takeaway: String = "",
    val subtopics: List<String> = emptyList(),
    val readingBody: String = "",
    val sourceCitation: String = "",
    val comparisonCards: List<ComparisonItem> = emptyList(),
    val trapTitle: String? = null,
    val trapBody: String? = null,
    val formula: FormulaBlock? = null,
    val causalityRule: CausalityRule? = null,
    val inlineMcq: InlineMcq? = null,
    val strikeThroughTf: StrikeThroughTf? = null
)

data class QuizQuestion(
    val id: String,
    val question: String,
    val options: List<String>,
    val correctIndices: List<Int>,
    val isMultiSelect: Boolean,
    val difficulty: QuizDifficulty,
    val explanation: String,
    val conceptTag: String,
    val sourceCitation: String
)

data class FlashcardItem(
    val id: String,
    val courseId: String,
    val front: String,
    val back: String,
    val hint: String,
    val sourceCitation: String,
    val masteryLevel: Int = 0,
    val nextReviewEpoch: Long = 0L
)

data class ExerciseProblem(
    val id: String,
    val title: String,
    val prompt: String,
    val formulaHint: String,
    val expectedFinalAnswer: String,
    val solutionSteps: List<String>,
    val sourceCitation: String
)

data class PodcastLine(
    val speaker: PodcastSpeaker,
    val text: String
)

data class PodcastChapter(
    val chapterNumber: Int,
    val title: String,
    val lines: List<PodcastLine>,
    val checkpoint: InlineMcq
)

data class StoryboardFrame(
    val stepLabel: String,
    val title: String,
    val description: String,
    val highlightFormula: String,
    val keyTakeaway: String
)

data class WrittenExamQuestion(
    val id: String,
    val partLabel: String,
    val points: Double,
    val prompt: String,
    val standardSolution: String,
    val keyRubricPoints: List<String>,
    val sourceCitation: String
)

data class AiGradingCard(
    val questionId: String,
    val scorePercent: Int,
    val standardSolution: String,
    val whereYouWentWrong: String,
    val focusSummary: String,
    val focusBullets: List<String>,
    val sourceCitation: String,
    val followUpChat: List<Pair<Boolean, String>> = emptyList()
)

data class LearningNode(
    val id: String,
    val orderIndex: Int,
    val chapterLabel: String,
    val title: String,
    val subtitle: String,
    val primaryMode: LearningMode,
    val sourceCitation: String,
    val masteryPercent: Int = 0,
    val isCompleted: Boolean = false,
    val isUnlocked: Boolean = true,
    val lastStudiedEpoch: Long = 0L,
    val smartSteps: List<SmartTextStep> = emptyList(),
    val finalQuiz: List<QuizQuestion> = emptyList()
)

data class Course(
    val id: String,
    val title: String,
    val categoryTag: String,
    val sourceBadge: String,
    val examDaysLeft: Int = 14,
    val targetScore: Double = 8.5,
    val readinessPercent: Int = 18,
    val starsEarned: Int = 120,
    val nodes: List<LearningNode> = emptyList(),
    val flashcards: List<FlashcardItem> = emptyList(),
    val exercises: List<ExerciseProblem> = emptyList(),
    val quizBank: List<QuizQuestion> = emptyList(),
    val trueFalseDeck: List<StrikeThroughTf> = emptyList(),
    val podcastChapters: List<PodcastChapter> = emptyList(),
    val writtenExamQuestions: List<WrittenExamQuestion> = emptyList(),
    val storyboardFrames: List<StoryboardFrame> = emptyList(),
    val isKtctFlagship: Boolean = false
)

data class RemediationRecord(
    val id: Long = 0,
    val courseId: String,
    val nodeId: String,
    val conceptTag: String,
    val originalQuestion: String,
    val mistakeSummary: String,
    val wrongCount: Int = 1,
    val resolved: Boolean = false,
    val updatedAtEpoch: Long = System.currentTimeMillis()
)

data class TutorChatMessage(
    val id: Long = 0,
    val courseId: String,
    val isUser: Boolean,
    val mode: TutorMode,
    val text: String,
    val attachedImageUri: String? = null,
    val timestampEpoch: Long = System.currentTimeMillis()
)

data class DailyGoalState(
    val smartTextCompletedToday: Int = 0,
    val smartTextTarget: Int = 1,
    val flashcardsReviewedToday: Int = 0,
    val flashcardsTarget: Int = 5,
    val quizzesCompletedToday: Int = 0,
    val quizzesTarget: Int = 1
)

data class AiSettings(
    val apiKey: String = "",
    val fastModelId: String = "gemini-2.5-flash",
    val deepModelId: String = "gemini-2.5-pro",
    val isWhitePaperExam: Boolean = false,
    val ttsSpeed: Float = 1.0f,
    val streakDays: Int = 3,
    val discoveredModels: List<String> = listOf(
        "gemini-2.5-flash",
        "gemini-2.5-pro",
        "gemini-2.5-flash-lite"
    )
)

data class DuelPayload(
    val code: String,
    val courseTitle: String,
    val creatorName: String,
    val creatorScore: Int,
    val creatorTimeSec: Int,
    val questions: List<QuizQuestion>
)

data class DuelHistoryRecord(
    val id: Long = 0,
    val code: String,
    val courseTitle: String,
    val myScore: Int,
    val opponentScore: Int,
    val myTimeSec: Int,
    val opponentTimeSec: Int,
    val opponentName: String,
    val playedAtEpoch: Long = System.currentTimeMillis()
)
