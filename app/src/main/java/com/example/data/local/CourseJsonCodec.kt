package com.example.data.local

import android.util.Base64
import com.example.model.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

object CourseJsonCodec {

    fun encodeCourse(course: Course): String = courseToJson(course).toString()

    fun decodeCourse(jsonString: String): Course = jsonToCourse(JSONObject(jsonString))

    fun exportShareString(course: Course): String {
        val raw = encodeCourse(course).toByteArray(Charsets.UTF_8)
        val bos = ByteArrayOutputStream()
        GZIPOutputStream(bos).use { it.write(raw) }
        val b64 = Base64.encodeToString(bos.toByteArray(), Base64.NO_WRAP)
        return "ASTRA-COURSE-V1:$b64"
    }

    fun importShareString(input: String): Course {
        val trimmed = input.trim()
        return if (trimmed.startsWith("ASTRA-COURSE-V1:")) {
            val payload = trimmed.removePrefix("ASTRA-COURSE-V1:")
            val bytes = Base64.decode(payload, Base64.DEFAULT)
            val json = GZIPInputStream(ByteArrayInputStream(bytes)).bufferedReader(Charsets.UTF_8).use { it.readText() }
            decodeCourse(json)
        } else {
            decodeCourse(trimmed)
        }
    }

    fun encodeDuelCode(payload: DuelPayload): String {
        val obj = JSONObject().apply {
            put("code", payload.code)
            put("courseTitle", payload.courseTitle)
            put("creatorName", payload.creatorName)
            put("creatorScore", payload.creatorScore)
            put("creatorTimeSec", payload.creatorTimeSec)
            put("questions", JSONArray().apply { payload.questions.forEach { put(quizToJson(it)) } })
        }
        val bos = ByteArrayOutputStream()
        GZIPOutputStream(bos).use { it.write(obj.toString().toByteArray(Charsets.UTF_8)) }
        val b64 = Base64.encodeToString(bos.toByteArray(), Base64.NO_WRAP)
        return "${payload.code}::$b64"
    }

    fun decodeDuelCode(rawInput: String): DuelPayload {
        val trimmed = rawInput.trim()
        val b64 = if (trimmed.contains("::")) trimmed.substringAfter("::") else trimmed
        val bytes = Base64.decode(b64, Base64.DEFAULT)
        val jsonStr = GZIPInputStream(ByteArrayInputStream(bytes)).bufferedReader(Charsets.UTF_8).use { it.readText() }
        val obj = JSONObject(jsonStr)
        val qArr = obj.optJSONArray("questions") ?: JSONArray()
        val questions = (0 until qArr.length()).map { jsonToQuiz(qArr.getJSONObject(it)) }
        return DuelPayload(
            code = obj.optString("code", "KTCT-DUEL"),
            courseTitle = obj.optString("courseTitle", "Kinh tế chính trị Mác - Lênin"),
            creatorName = obj.optString("creatorName", "Bạn cùng học"),
            creatorScore = obj.optInt("creatorScore", 0),
            creatorTimeSec = obj.optInt("creatorTimeSec", 60),
            questions = questions
        )
    }

    private fun courseToJson(c: Course): JSONObject = JSONObject().apply {
        put("id", c.id)
        put("title", c.title)
        put("categoryTag", c.categoryTag)
        put("sourceBadge", c.sourceBadge)
        put("examDaysLeft", c.examDaysLeft)
        put("targetScore", c.targetScore)
        put("readinessPercent", c.readinessPercent)
        put("starsEarned", c.starsEarned)
        put("isKtctFlagship", c.isKtctFlagship)
        put("nodes", JSONArray().apply { c.nodes.forEach { put(nodeToJson(it)) } })
        put("flashcards", JSONArray().apply { c.flashcards.forEach { put(flashcardToJson(it)) } })
        put("exercises", JSONArray().apply { c.exercises.forEach { put(exerciseToJson(it)) } })
        put("quizBank", JSONArray().apply { c.quizBank.forEach { put(quizToJson(it)) } })
        put("trueFalseDeck", JSONArray().apply { c.trueFalseDeck.forEach { put(tfToJson(it)) } })
        put("podcastChapters", JSONArray().apply { c.podcastChapters.forEach { put(podcastToJson(it)) } })
        put("writtenExamQuestions", JSONArray().apply { c.writtenExamQuestions.forEach { put(writtenToJson(it)) } })
        put("storyboardFrames", JSONArray().apply { c.storyboardFrames.forEach { put(storyboardToJson(it)) } })
    }

    private fun jsonToCourse(o: JSONObject): Course = Course(
        id = o.optString("id", "course_${System.currentTimeMillis()}"),
        title = o.optString("title", "Khóa học mới"),
        categoryTag = o.optString("categoryTag", "TÀI LIỆU TỰ NẠP · ĐẠI HỌC"),
        sourceBadge = o.optString("sourceBadge", "📄 Tài liệu người dùng tải lên"),
        examDaysLeft = o.optInt("examDaysLeft", 14),
        targetScore = o.optDouble("targetScore", 8.5),
        readinessPercent = o.optInt("readinessPercent", 0),
        starsEarned = o.optInt("starsEarned", 0),
        isKtctFlagship = o.optBoolean("isKtctFlagship", false),
        nodes = o.optJSONArray("nodes").toObjList(::jsonToNode),
        flashcards = o.optJSONArray("flashcards").toObjList(::jsonToFlashcard),
        exercises = o.optJSONArray("exercises").toObjList(::jsonToExercise),
        quizBank = o.optJSONArray("quizBank").toObjList(::jsonToQuiz),
        trueFalseDeck = o.optJSONArray("trueFalseDeck").toObjList(::jsonToTf),
        podcastChapters = o.optJSONArray("podcastChapters").toObjList(::jsonToPodcast),
        writtenExamQuestions = o.optJSONArray("writtenExamQuestions").toObjList(::jsonToWritten),
        storyboardFrames = o.optJSONArray("storyboardFrames").toObjList(::jsonToStoryboard)
    )

    private fun nodeToJson(n: LearningNode): JSONObject = JSONObject().apply {
        put("id", n.id)
        put("orderIndex", n.orderIndex)
        put("chapterLabel", n.chapterLabel)
        put("title", n.title)
        put("subtitle", n.subtitle)
        put("primaryMode", n.primaryMode.name)
        put("sourceCitation", n.sourceCitation)
        put("masteryPercent", n.masteryPercent)
        put("isCompleted", n.isCompleted)
        put("isUnlocked", n.isUnlocked)
        put("lastStudiedEpoch", n.lastStudiedEpoch)
        put("smartSteps", JSONArray().apply { n.smartSteps.forEach { put(stepToJson(it)) } })
        put("finalQuiz", JSONArray().apply { n.finalQuiz.forEach { put(quizToJson(it)) } })
    }

    private fun jsonToNode(o: JSONObject): LearningNode = LearningNode(
        id = o.optString("id"),
        orderIndex = o.optInt("orderIndex", 1),
        chapterLabel = o.optString("chapterLabel", "Chương 1"),
        title = o.optString("title"),
        subtitle = o.optString("subtitle"),
        primaryMode = runCatching { LearningMode.valueOf(o.optString("primaryMode")) }.getOrDefault(LearningMode.SMART_TEXT),
        sourceCitation = o.optString("sourceCitation"),
        masteryPercent = o.optInt("masteryPercent", 0),
        isCompleted = o.optBoolean("isCompleted", false),
        isUnlocked = o.optBoolean("isUnlocked", true),
        lastStudiedEpoch = o.optLong("lastStudiedEpoch", 0L),
        smartSteps = o.optJSONArray("smartSteps").toObjList(::jsonToStep),
        finalQuiz = o.optJSONArray("finalQuiz").toObjList(::jsonToQuiz)
    )

    private fun stepToJson(s: SmartTextStep): JSONObject = JSONObject().apply {
        put("stepNumber", s.stepNumber)
        put("title", s.title)
        put("takeaway", s.takeaway)
        put("subtopics", JSONArray(s.subtopics))
        put("readingBody", s.readingBody)
        put("sourceCitation", s.sourceCitation)
        put("comparisonCards", JSONArray().apply {
            s.comparisonCards.forEach { c ->
                put(JSONObject().apply {
                    put("title", c.title)
                    put("tag", c.tag)
                    put("points", JSONArray(c.points))
                    put("accentHex", c.accentHex)
                })
            }
        })
        s.trapTitle?.let { put("trapTitle", it) }
        s.trapBody?.let { put("trapBody", it) }
        s.formula?.let { f ->
            put("formula", JSONObject().apply {
                put("title", f.title)
                put("legend", JSONArray(f.legend))
                put("terms", JSONArray().apply {
                    f.terms.forEach { t ->
                        put(JSONObject().apply {
                            put("text", t.text)
                            t.numerator?.let { put("numerator", it) }
                            t.denominator?.let { put("denominator", it) }
                            t.superscript?.let { put("superscript", it) }
                            t.subscript?.let { put("subscript", it) }
                        })
                    }
                })
            })
        }
        s.causalityRule?.let { r ->
            put("causalityRule", JSONObject().apply {
                put("title", r.title)
                put("arrows", JSONArray(r.arrows))
                put("note", r.note)
            })
        }
        s.inlineMcq?.let { put("inlineMcq", inlineMcqToJson(it)) }
        s.strikeThroughTf?.let { put("strikeThroughTf", tfToJson(it)) }
    }

    private fun jsonToStep(o: JSONObject): SmartTextStep = SmartTextStep(
        stepNumber = o.optInt("stepNumber", 1),
        title = o.optString("title"),
        takeaway = o.optString("takeaway"),
        subtopics = o.optJSONArray("subtopics").toStringList(),
        readingBody = o.optString("readingBody"),
        sourceCitation = o.optString("sourceCitation"),
        comparisonCards = o.optJSONArray("comparisonCards").toObjList { c ->
            ComparisonItem(
                title = c.optString("title"),
                tag = c.optString("tag"),
                points = c.optJSONArray("points").toStringList(),
                accentHex = c.optString("accentHex", "#3B82F6")
            )
        },
        trapTitle = o.optString("trapTitle").takeIf { it.isNotBlank() },
        trapBody = o.optString("trapBody").takeIf { it.isNotBlank() },
        formula = o.optJSONObject("formula")?.let { f ->
            FormulaBlock(
                title = f.optString("title"),
                terms = f.optJSONArray("terms").toObjList { t ->
                    FormulaTerm(
                        text = t.optString("text"),
                        numerator = t.optString("numerator").takeIf { it.isNotBlank() },
                        denominator = t.optString("denominator").takeIf { it.isNotBlank() },
                        superscript = t.optString("superscript").takeIf { it.isNotBlank() },
                        subscript = t.optString("subscript").takeIf { it.isNotBlank() }
                    )
                },
                legend = f.optJSONArray("legend").toStringList()
            )
        },
        causalityRule = o.optJSONObject("causalityRule")?.let { r ->
            CausalityRule(
                title = r.optString("title"),
                arrows = r.optJSONArray("arrows").toStringList(),
                note = r.optString("note")
            )
        },
        inlineMcq = o.optJSONObject("inlineMcq")?.let(::jsonToInlineMcq),
        strikeThroughTf = o.optJSONObject("strikeThroughTf")?.let(::jsonToTf)
    )

    private fun inlineMcqToJson(m: InlineMcq) = JSONObject().apply {
        put("question", m.question)
        put("options", JSONArray(m.options))
        put("correctIndex", m.correctIndex)
        put("explanation", m.explanation)
        put("conceptTag", m.conceptTag)
        put("sourceCitation", m.sourceCitation)
    }

    private fun jsonToInlineMcq(o: JSONObject) = InlineMcq(
        question = o.optString("question"),
        options = o.optJSONArray("options").toStringList(),
        correctIndex = o.optInt("correctIndex", 0),
        explanation = o.optString("explanation"),
        conceptTag = o.optString("conceptTag"),
        sourceCitation = o.optString("sourceCitation")
    )

    private fun tfToJson(t: StrikeThroughTf) = JSONObject().apply {
        put("id", t.id)
        put("prefix", t.prefix)
        put("wrongClause", t.wrongClause)
        put("correctClause", t.correctClause)
        put("suffix", t.suffix)
        put("isTrue", t.isTrue)
        put("explanation", t.explanation)
        put("conceptTag", t.conceptTag)
        put("sourceCitation", t.sourceCitation)
    }

    private fun jsonToTf(o: JSONObject) = StrikeThroughTf(
        id = o.optString("id", "tf_${System.nanoTime()}"),
        prefix = o.optString("prefix"),
        wrongClause = o.optString("wrongClause"),
        correctClause = o.optString("correctClause"),
        suffix = o.optString("suffix"),
        isTrue = o.optBoolean("isTrue", false),
        explanation = o.optString("explanation"),
        conceptTag = o.optString("conceptTag"),
        sourceCitation = o.optString("sourceCitation")
    )

    private fun quizToJson(q: QuizQuestion) = JSONObject().apply {
        put("id", q.id)
        put("question", q.question)
        put("options", JSONArray(q.options))
        put("correctIndices", JSONArray(q.correctIndices))
        put("isMultiSelect", q.isMultiSelect)
        put("difficulty", q.difficulty.name)
        put("explanation", q.explanation)
        put("conceptTag", q.conceptTag)
        put("sourceCitation", q.sourceCitation)
    }

    private fun jsonToQuiz(o: JSONObject) = QuizQuestion(
        id = o.optString("id", "q_${System.nanoTime()}"),
        question = o.optString("question"),
        options = o.optJSONArray("options").toStringList(),
        correctIndices = o.optJSONArray("correctIndices").toIntList(),
        isMultiSelect = o.optBoolean("isMultiSelect", false),
        difficulty = runCatching { QuizDifficulty.valueOf(o.optString("difficulty")) }.getOrDefault(QuizDifficulty.MEDIUM),
        explanation = o.optString("explanation"),
        conceptTag = o.optString("conceptTag"),
        sourceCitation = o.optString("sourceCitation")
    )

    private fun flashcardToJson(f: FlashcardItem) = JSONObject().apply {
        put("id", f.id)
        put("courseId", f.courseId)
        put("front", f.front)
        put("back", f.back)
        put("hint", f.hint)
        put("sourceCitation", f.sourceCitation)
        put("masteryLevel", f.masteryLevel)
        put("nextReviewEpoch", f.nextReviewEpoch)
    }

    private fun jsonToFlashcard(o: JSONObject) = FlashcardItem(
        id = o.optString("id"),
        courseId = o.optString("courseId"),
        front = o.optString("front"),
        back = o.optString("back"),
        hint = o.optString("hint"),
        sourceCitation = o.optString("sourceCitation"),
        masteryLevel = o.optInt("masteryLevel", 0),
        nextReviewEpoch = o.optLong("nextReviewEpoch", 0L)
    )

    private fun exerciseToJson(e: ExerciseProblem) = JSONObject().apply {
        put("id", e.id)
        put("title", e.title)
        put("prompt", e.prompt)
        put("formulaHint", e.formulaHint)
        put("expectedFinalAnswer", e.expectedFinalAnswer)
        put("solutionSteps", JSONArray(e.solutionSteps))
        put("sourceCitation", e.sourceCitation)
    }

    private fun jsonToExercise(o: JSONObject) = ExerciseProblem(
        id = o.optString("id"),
        title = o.optString("title"),
        prompt = o.optString("prompt"),
        formulaHint = o.optString("formulaHint"),
        expectedFinalAnswer = o.optString("expectedFinalAnswer"),
        solutionSteps = o.optJSONArray("solutionSteps").toStringList(),
        sourceCitation = o.optString("sourceCitation")
    )

    private fun podcastToJson(p: PodcastChapter) = JSONObject().apply {
        put("chapterNumber", p.chapterNumber)
        put("title", p.title)
        put("lines", JSONArray().apply {
            p.lines.forEach { l ->
                put(JSONObject().apply {
                    put("speaker", l.speaker.name)
                    put("text", l.text)
                })
            }
        })
        put("checkpoint", inlineMcqToJson(p.checkpoint))
    }

    private fun jsonToPodcast(o: JSONObject) = PodcastChapter(
        chapterNumber = o.optInt("chapterNumber", 1),
        title = o.optString("title"),
        lines = o.optJSONArray("lines").toObjList { l ->
            PodcastLine(
                speaker = runCatching { PodcastSpeaker.valueOf(l.optString("speaker")) }.getOrDefault(PodcastSpeaker.HOST_MINH),
                text = l.optString("text")
            )
        },
        checkpoint = o.optJSONObject("checkpoint")?.let(::jsonToInlineMcq)
            ?: InlineMcq("Kiểm tra nhanh", listOf("Đúng", "Sai"), 0, "", "Podcast", "")
    )

    private fun writtenToJson(w: WrittenExamQuestion) = JSONObject().apply {
        put("id", w.id)
        put("partLabel", w.partLabel)
        put("points", w.points)
        put("prompt", w.prompt)
        put("standardSolution", w.standardSolution)
        put("keyRubricPoints", JSONArray(w.keyRubricPoints))
        put("sourceCitation", w.sourceCitation)
    }

    private fun jsonToWritten(o: JSONObject) = WrittenExamQuestion(
        id = o.optString("id"),
        partLabel = o.optString("partLabel"),
        points = o.optDouble("points", 3.0),
        prompt = o.optString("prompt"),
        standardSolution = o.optString("standardSolution"),
        keyRubricPoints = o.optJSONArray("keyRubricPoints").toStringList(),
        sourceCitation = o.optString("sourceCitation")
    )

    private fun storyboardToJson(s: StoryboardFrame) = JSONObject().apply {
        put("stepLabel", s.stepLabel)
        put("title", s.title)
        put("description", s.description)
        put("highlightFormula", s.highlightFormula)
        put("keyTakeaway", s.keyTakeaway)
    }

    private fun jsonToStoryboard(o: JSONObject) = StoryboardFrame(
        stepLabel = o.optString("stepLabel"),
        title = o.optString("title"),
        description = o.optString("description"),
        highlightFormula = o.optString("highlightFormula"),
        keyTakeaway = o.optString("keyTakeaway")
    )

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        return (0 until length()).map { optString(it) }
    }

    private fun JSONArray?.toIntList(): List<Int> {
        if (this == null) return emptyList()
        return (0 until length()).map { optInt(it) }
    }

    private fun <T> JSONArray?.toObjList(mapper: (JSONObject) -> T): List<T> {
        if (this == null) return emptyList()
        return (0 until length()).mapNotNull { optJSONObject(it)?.let(mapper) }
    }
}
