package com.example.service

import android.content.Context
import android.net.Uri
import com.example.model.*
import org.json.JSONArray
import org.json.JSONObject

class GeminiDomainUseCases(
    private val client: GeminiRestClient = GeminiRestClient()
) {
    suspend fun discoverModels(apiKey: String): Result<List<String>> = client.fetchAvailableModels(apiKey)

    /**
     * 1. Universal Document Ingestion (Deep Reasoning Model)
     * Generates a complete Course with 5-7 Winding Path nodes, Smart Text steps, Flashcards, Quiz & Citations.
     */
    suspend fun ingestDocumentsToCourse(
        context: Context,
        settings: AiSettings,
        courseTitle: String,
        examDaysLeft: Int,
        targetScore: Double,
        documents: List<SelectedDocumentInfo>,
        largePdfStrategy: LargePdfStrategy,
        pageStart: Int,
        pageEnd: Int,
        onProgress: (Int, String) -> Unit
    ): Result<Course> {
        return runCatching {
            onProgress(1, "Đang đọc cấu trúc tài liệu & trích xuất khái niệm cốt lõi...")
            val userParts = JSONArray()
            val fileNames = documents.map { it.displayName }

            for (doc in documents) {
                val prepared = DocumentProcessor.prepareDocumentParts(
                    context = context,
                    info = doc,
                    largePdfStrategy = largePdfStrategy,
                    pageStart = pageStart,
                    pageEnd = pageEnd
                )
                for (part in prepared) {
                    when (part) {
                        is PreparedDocumentPart.TextPart -> {
                            userParts.put(JSONObject().put("text", "=== TÀI LIỆU: ${part.fileName} ===\n${part.text}"))
                        }
                        is PreparedDocumentPart.InlineBlob -> {
                            userParts.put(JSONObject().put("inlineData", JSONObject().apply {
                                put("mimeType", part.mimeType)
                                put("data", part.base64Data)
                            }))
                        }
                        is PreparedDocumentPart.LargePdfForFilesApi -> {
                            val uploaded = client.uploadLargePdfAndWaitActive(
                                context = context,
                                apiKey = settings.apiKey,
                                uri = part.uri,
                                displayName = part.fileName,
                                sizeBytes = part.sizeBytes,
                                onStatus = { msg -> onProgress(1, msg) }
                            )
                            userParts.put(JSONObject().put("fileData", JSONObject().apply {
                                put("mimeType", uploaded.mimeType)
                                put("fileUri", uploaded.fileUri)
                            }))
                        }
                    }
                }
            }

            onProgress(2, "Đang thiết kế lộ trình uốn lượn (Winding Path) 5–6 trạm...")
            val prompt = """
                Bạn là chuyên gia thiết kế giáo trình của Astra AI. Hãy phân tích các tài liệu trên để tạo khóa học tiếng Việt 100% cho môn "$courseTitle".
                Yêu cầu nghiêm ngặt:
                - Tạo đúng 5 trạm học (nodes) theo thứ tự từ cơ bản đến nâng cao.
                - Trạm 1 và Trạm 2 có đầy đủ 4 bước Văn bản thông minh ngắn gọn (mỗi bước đọc <130 chữ) kèm câu hỏi kiểm tra chốt chặn.
                - Tạo 6 thẻ ghi nhớ (flashcards), 5 câu trắc nghiệm (có ít nhất 1 câu chọn nhiều đáp án), 5 thẻ Đúng/Sai Gạch bỏ (tách rõ prefix, wrongClause, correctClause, isTrue), 2 câu tự luận thi thử, và 3 khung Storyboard mô phỏng.
                - MỌI bước học, câu hỏi đều PHẢI có nguồn trích dẫn sourceCitation dạng "📄 Nguồn tài liệu: [${fileNames.firstOrNull() ?: courseTitle}, Chương/Mục ...]".
                - Trình bày công thức bằng ký hiệu Unicode sạch (×, ÷, →, ², ³), KHÔNG dùng ký tự LaTeX thô (${'$'} hay \frac).
            """.trimIndent()
            userParts.put(JSONObject().put("text", prompt))

            onProgress(3, "Đang tạo bài Văn bản thông minh & chốt chặn kiểm tra...")
            val contents = JSONArray().put(JSONObject().apply {
                put("role", "user")
                put("parts", userParts)
            })

            val rawJson = client.generateContent(
                apiKey = settings.apiKey,
                modelId = settings.deepModelId,
                systemInstruction = "Trả về JSON hợp lệ 100% tiếng Việt theo đúng responseSchema.",
                contents = contents,
                responseSchema = buildCourseSchema(),
                temperature = 0.35
            ).getOrThrow()

            parseGeneratedCourse(
                rawJson = rawJson,
                courseTitle = courseTitle,
                examDaysLeft = examDaysLeft,
                targetScore = targetScore,
                sourceFiles = fileNames
            )
        }
    }

    /**
     * 2. Step 7 Rephrased Remediation Question (Fast Model)
     */
    suspend fun generateRephrasedRemediation(
        settings: AiSettings,
        conceptTag: String,
        originalQuestion: String,
        mistakeContext: String
    ): Result<InlineMcq> {
        val prompt = """
            Sinh viên vừa trả lời sai khái niệm: "$conceptTag".
            Câu hỏi gốc đã sai: "$originalQuestion".
            Lỗi thường gặp: "$mistakeContext".
            Hãy tạo MỘT câu hỏi trắc nghiệm 4 đáp án HOÀN TOÀN MỚI (không lặp nguyên văn câu cũ), kiểm tra lại đúng khái niệm đó theo góc nhìn ĐẢO CHIỀU NHÂN QUẢ hoặc TÌNH HUỐNG THỰC TẾ.
            Dùng ký hiệu Unicode sạch cho công thức, không dùng LaTeX thô.
        """.trimIndent()
        val contents = singleUserTurn(prompt)
        return client.generateContent(
            apiKey = settings.apiKey,
            modelId = settings.fastModelId,
            systemInstruction = "Bạn là gia sư Astra AI tiếng Việt. Trả về JSON đúng schema.",
            contents = contents,
            responseSchema = buildInlineMcqSchema()
        ).map { raw ->
            val o = JSONObject(raw)
            val opts = o.optJSONArray("options")
            InlineMcq(
                question = sanitizeUnicodeMath(o.optString("question")),
                options = (0 until (opts?.length() ?: 0)).map { sanitizeUnicodeMath(opts!!.optString(it)) },
                correctIndex = o.optInt("correctIndex", 0).coerceIn(0, 3),
                explanation = sanitizeUnicodeMath(o.optString("explanation")),
                conceptTag = conceptTag,
                sourceCitation = "🔄 Câu hỏi ôn lại (Bước 7 · Diễn đạt mới)"
            )
        }
    }

    /**
     * 3. 24/7 Socratic / Fast AI Tutor with Snap-to-Solve image & Course Context
     */
    suspend fun askTutor(
        context: Context,
        settings: AiSettings,
        mode: TutorMode,
        courseContext: Course?,
        history: List<TutorChatMessage>,
        userQuestion: String,
        attachedImageUri: Uri?
    ): Result<String> {
        val systemPrompt = buildString {
            appendLine("Bạn là Gia sư AI 24/7 của nền tảng AstraFlow AI (phong cách Astra AI), giao tiếp 100% tiếng Việt thân thiện, sắc bén và chuẩn học thuật.")
            if (mode == TutorMode.SOCRATIC) {
                appendLine("CHẾ ĐỘ BẮT BUỘC: GỢI Ý TỪNG BƯỚC (SOCRATIC). TUYỆT ĐỐI KHÔNG đưa ngay đáp án cuối cùng. Hãy khen hoặc xác nhận bước đúng, chỉ ra 1 nút thắt tư duy, đưa ra 1 gợi ý ngắn rồi đặt 1 câu hỏi gợi mở để sinh viên tự tìm ra bước tiếp theo.")
            } else {
                appendLine("CHẾ ĐỘ: GIẢI THÍCH NHANH. Trả lời thẳng trọng tâm, rõ cơ chế, ngắn gọn dễ hiểu kèm ví dụ minh họa và công thức nếu có.")
            }
            appendLine("QUY TẮC ĐỊNH DẠNG CÔNG THỨC BẮT BUỘC: Trình bày mọi công thức kinh tế/toán học bằng ký hiệu Unicode rõ ràng như m' = (m / v) × 100%, p' = m / (c + v) × 100%, c/v, T - H - T', →, ², ³. TUYỆT ĐỐI KHÔNG dùng mã LaTeX thô như ${'$'}...${'$'}, \\frac, \\times, hay dấu gạch dưới _.")
            if (courseContext != null) {
                appendLine("NGỮ CẢNH KHÓA HỌC ĐANG GẮN: ${courseContext.title} (${courseContext.sourceBadge}). Các trạm chính: ${courseContext.nodes.joinToString("; ") { "${it.chapterLabel}: ${it.title}" }}.")
            }
        }

        val contents = JSONArray()
        history.takeLast(8).forEach { msg ->
            contents.put(JSONObject().apply {
                put("role", if (msg.isUser) "user" else "model")
                put("parts", JSONArray().put(JSONObject().put("text", msg.text)))
            })
        }

        val latestParts = JSONArray()
        if (attachedImageUri != null) {
            val b64 = DocumentProcessor.encodeImageUriToBase64Jpeg(context, attachedImageUri)
            if (b64 != null) {
                latestParts.put(JSONObject().put("inlineData", JSONObject().apply {
                    put("mimeType", "image/jpeg")
                    put("data", b64)
                }))
            }
        }
        latestParts.put(JSONObject().put("text", userQuestion.ifBlank { "Hãy đọc đề bài trong ảnh này và hướng dẫn mình từng bước." }))
        contents.put(JSONObject().apply {
            put("role", "user")
            put("parts", latestParts)
        })

        val modelToUse = if (attachedImageUri != null) settings.deepModelId else settings.fastModelId
        return client.generateContent(
            apiKey = settings.apiKey,
            modelId = modelToUse,
            systemInstruction = systemPrompt,
            contents = contents,
            temperature = if (mode == TutorMode.SOCRATIC) 0.55 else 0.35
        ).map(::sanitizeUnicodeMath)
    }

    /**
     * 4. Quick Chips under Tutor Reply: Generate 3-Question Mini-Quiz or 1 Flashcard
     */
    suspend fun generateQuickQuizFromReply(
        settings: AiSettings,
        replyText: String,
        courseTitle: String
    ): Result<List<QuizQuestion>> {
        val prompt = """
            Dựa trên nội dung giảng giải sau của môn "$courseTitle":
            "$replyText"
            Hãy tạo đúng 3 câu trắc nghiệm 4 đáp án ngắn gọn kiểm tra độ hiểu sâu (không dùng LaTeX thô, dùng Unicode sạch).
        """.trimIndent()
        val schema = JSONObject().apply {
            put("type", "OBJECT")
            put("properties", JSONObject().apply {
                put("questions", JSONObject().apply {
                    put("type", "ARRAY")
                    put("items", buildInlineMcqSchema())
                })
            })
            put("required", JSONArray().put("questions"))
        }
        return client.generateContent(
            apiKey = settings.apiKey,
            modelId = settings.fastModelId,
            systemInstruction = "Trả về JSON tiếng Việt đúng schema.",
            contents = singleUserTurn(prompt),
            responseSchema = schema
        ).map { raw ->
            val arr = JSONObject(raw).optJSONArray("questions") ?: JSONArray()
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                val opts = o.optJSONArray("options")
                QuizQuestion(
                    id = "tutor_q_${System.nanoTime()}_$i",
                    question = sanitizeUnicodeMath(o.optString("question")),
                    options = (0 until (opts?.length() ?: 0)).map { sanitizeUnicodeMath(opts!!.optString(it)) },
                    correctIndices = listOf(o.optInt("correctIndex", 0).coerceIn(0, 3)),
                    isMultiSelect = false,
                    difficulty = QuizDifficulty.MEDIUM,
                    explanation = sanitizeUnicodeMath(o.optString("explanation")),
                    conceptTag = courseTitle,
                    sourceCitation = "✨ Tạo nhanh từ Gia sư AI"
                )
            }
        }
    }

    suspend fun generateFlashcardFromReply(
        settings: AiSettings,
        replyText: String,
        courseId: String,
        courseTitle: String
    ): Result<FlashcardItem> {
        val prompt = """
            Từ đoạn giải thích sau của môn "$courseTitle":
            "$replyText"
            Hãy chưng cất thành 1 thẻ ghi nhớ (Flashcard) sắc bén: mặt trước là câu hỏi gợi nhớ ngắn, mặt sau là đáp án/cơ chế cốt lõi, kèm 1 mẹo nhớ ngắn. Dùng Unicode sạch, không LaTeX thô.
        """.trimIndent()
        val schema = JSONObject().apply {
            put("type", "OBJECT")
            put("properties", JSONObject().apply {
                put("front", JSONObject().put("type", "STRING"))
                put("back", JSONObject().put("type", "STRING"))
                put("hint", JSONObject().put("type", "STRING"))
            })
            put("required", JSONArray().put("front").put("back").put("hint"))
        }
        return client.generateContent(
            apiKey = settings.apiKey,
            modelId = settings.fastModelId,
            systemInstruction = "Trả về JSON tiếng Việt đúng schema.",
            contents = singleUserTurn(prompt),
            responseSchema = schema
        ).map { raw ->
            val o = JSONObject(raw)
            FlashcardItem(
                id = "fc_ai_${System.currentTimeMillis()}",
                courseId = courseId,
                front = sanitizeUnicodeMath(o.optString("front")),
                back = sanitizeUnicodeMath(o.optString("back")),
                hint = sanitizeUnicodeMath(o.optString("hint")),
                sourceCitation = "✨ Lưu từ Gia sư AI · $courseTitle"
            )
        }
    }

    /**
     * 5. Written Mock Exam Grading (Deep Reasoning + Handwritten Photo OCR)
     */
    suspend fun gradeWrittenAnswer(
        context: Context,
        settings: AiSettings,
        question: WrittenExamQuestion,
        typedAnswer: String,
        photoUri: Uri?
    ): Result<AiGradingCard> {
        val parts = JSONArray()
        if (photoUri != null) {
            val b64 = DocumentProcessor.encodeImageUriToBase64Jpeg(context, photoUri, maxDim = 1440)
            if (b64 != null) {
                parts.put(JSONObject().put("inlineData", JSONObject().apply {
                    put("mimeType", "image/jpeg")
                    put("data", b64)
                }))
            }
        }
        val prompt = """
            Hãy chấm bài thi tự luận đại học theo phong cách Astra AI Step-by-Step Grading:
            - Đề bài (${question.partLabel}, ${question.points} điểm): ${question.prompt}
            - Đáp án chuẩn giáo trình: ${question.standardSolution}
            - Bài làm gõ máy của sinh viên: ${typedAnswer.ifBlank { "(Sinh viên nộp ảnh bài viết tay đính kèm — hãy OCR đọc kỹ từng dòng trong ảnh)" }}
            Yêu cầu:
            - Chấm scorePercent (0–100).
            - whereYouWentWrong: Trích dẫn CHÍNH XÁC cụm từ, bước công thức hoặc mắt xích nhân quả sinh viên làm sai/thiếu, đồng thời ghi nhận những bước sau sinh viên làm đúng (nếu có).
            - focusSummary: 1 câu tổng hợp hướng khắc phục.
            - focusBullets: 2–3 gạch đầu dòng hành động cụ thể.
            - Dùng ký hiệu Unicode sạch cho công thức (m', p', ×, →), không dùng LaTeX thô.
        """.trimIndent()
        parts.put(JSONObject().put("text", prompt))

        val contents = JSONArray().put(JSONObject().apply {
            put("role", "user")
            put("parts", parts)
        })
        val schema = JSONObject().apply {
            put("type", "OBJECT")
            put("properties", JSONObject().apply {
                put("scorePercent", JSONObject().put("type", "INTEGER"))
                put("standardSolution", JSONObject().put("type", "STRING"))
                put("whereYouWentWrong", JSONObject().put("type", "STRING"))
                put("focusSummary", JSONObject().put("type", "STRING"))
                put("focusBullets", JSONObject().apply {
                    put("type", "ARRAY")
                    put("items", JSONObject().put("type", "STRING"))
                })
            })
            put("required", JSONArray().put("scorePercent").put("standardSolution").put("whereYouWentWrong").put("focusSummary").put("focusBullets"))
        }

        return client.generateContent(
            apiKey = settings.apiKey,
            modelId = settings.deepModelId,
            systemInstruction = "Giám khảo đại học chấm bài chuẩn xác, công tâm bằng tiếng Việt, trả về JSON đúng schema.",
            contents = contents,
            responseSchema = schema,
            temperature = 0.2
        ).map { raw ->
            val o = JSONObject(raw)
            val bulletsArr = o.optJSONArray("focusBullets")
            AiGradingCard(
                questionId = question.id,
                scorePercent = o.optInt("scorePercent", 60).coerceIn(0, 100),
                standardSolution = sanitizeUnicodeMath(o.optString("standardSolution", question.standardSolution)),
                whereYouWentWrong = sanitizeUnicodeMath(o.optString("whereYouWentWrong")),
                focusSummary = sanitizeUnicodeMath(o.optString("focusSummary")),
                focusBullets = (0 until (bulletsArr?.length() ?: 0)).map { sanitizeUnicodeMath(bulletsArr!!.optString(it)) },
                sourceCitation = question.sourceCitation
            )
        }
    }

    suspend fun askGradingFollowUp(
        settings: AiSettings,
        question: WrittenExamQuestion,
        grading: AiGradingCard,
        studentQuestion: String
    ): Result<String> {
        val prompt = """
            Đề thi: ${question.prompt}
            Nhận xét sai sót trước đó: ${grading.whereYouWentWrong}
            Lời giải chuẩn: ${grading.standardSolution}
            Sinh viên hỏi thêm: "$studentQuestion"
            Hãy giải đáp ngắn gọn, rõ ràng bằng tiếng Việt (dùng ký hiệu Unicode sạch, không LaTeX thô).
        """.trimIndent()
        return client.generateContent(
            apiKey = settings.apiKey,
            modelId = settings.fastModelId,
            systemInstruction = "Bạn là gia sư chấm thi AstraFlow AI.",
            contents = singleUserTurn(prompt)
        ).map(::sanitizeUnicodeMath)
    }

    /**
     * 6. Oral Viva Examiner (Fast Model)
     */
    suspend fun conductOralVivaTurn(
        settings: AiSettings,
        courseTitle: String,
        topic: String,
        transcript: List<Pair<Boolean, String>>,
        turnNumber: Int
    ): Result<String> {
        val system = """
            Bạn là Giảng viên hỏi thi vấn đáp đại học môn "$courseTitle", chủ đề "$topic".
            Luật hỏi thi:
            - Lượt 1–2: Đánh giá ngắn 1–2 câu câu trả lời vừa rồi của sinh viên, rồi đặt ngay 1 câu hỏi xoáy sâu kiểu "Tại sao...?" hoặc "Nếu biến số X thay đổi thì sao?".
            - Từ lượt 3 trở đi: Tổng kết nhận xét chi tiết và CHẤM ĐIỂM RUBRIC rõ ràng trên thang 10 (Kiến thức nền /4, Tư duy nhân quả /3, Phản xạ hỏi xoáy /3).
            - Dùng ký hiệu Unicode sạch cho công thức (m', p', c/v, →), không dùng LaTeX thô.
        """.trimIndent()
        val contents = JSONArray()
        transcript.forEach { (isStudent, text) ->
            contents.put(JSONObject().apply {
                put("role", if (isStudent) "user" else "model")
                put("parts", JSONArray().put(JSONObject().put("text", text)))
            })
        }
        return client.generateContent(
            apiKey = settings.apiKey,
            modelId = settings.fastModelId,
            systemInstruction = system,
            contents = contents,
            temperature = 0.5
        ).map(::sanitizeUnicodeMath)
    }

    /**
     * 7. Targeted Knowledge Gap Mini-Lesson
     */
    suspend fun generateGapFixMiniLesson(
        settings: AiSettings,
        courseTitle: String,
        gap: RemediationRecord
    ): Result<String> {
        val prompt = """
            Môn học: $courseTitle.
            Lỗ hổng kiến thức sinh viên hay sai (${gap.wrongCount} lần): ${gap.conceptTag}.
            Bối cảnh sai: ${gap.mistakeSummary}.
            Hãy viết một bài "Vá lỗ hổng siêu tốc" (tối đa 160 chữ) gồm:
            1) Bản chất 1 câu dễ nhớ
            2) So sánh cặp dễ nhầm / Công thức Unicode sạch
            3) 1 mẹo phản xạ 3 giây khi đi thi.
            Không dùng LaTeX thô.
        """.trimIndent()
        return client.generateContent(
            apiKey = settings.apiKey,
            modelId = settings.fastModelId,
            systemInstruction = "Gia sư AstraFlow AI tiếng Việt súc tích, dễ nhớ.",
            contents = singleUserTurn(prompt)
        ).map(::sanitizeUnicodeMath)
    }

    /**
     * 8. Step Verification for Numerical Exercises
     */
    suspend fun verifyExerciseStep(
        settings: AiSettings,
        problem: ExerciseProblem,
        userWork: String
    ): Result<String> {
        val prompt = """
            Bài tập: ${problem.prompt}
            Đáp án chuẩn: ${problem.expectedFinalAnswer}
            Các bước chuẩn: ${problem.solutionSteps.joinToString(" | ")}
            Lời giải của sinh viên: "$userWork"
            Hãy kiểm tra từng bước tính của sinh viên: bước nào đúng, bước nào lệch số hoặc nhầm công thức (dùng Unicode sạch như m' = (m / v) × 100%, không dùng LaTeX thô).
        """.trimIndent()
        return client.generateContent(
            apiKey = settings.apiKey,
            modelId = settings.deepModelId,
            systemInstruction = "Gia sư bài tập kinh tế chính trị chấm từng bước chính xác.",
            contents = singleUserTurn(prompt)
        ).map(::sanitizeUnicodeMath)
    }

    private fun singleUserTurn(text: String): JSONArray = JSONArray().put(
        JSONObject().apply {
            put("role", "user")
            put("parts", JSONArray().put(JSONObject().put("text", text)))
        }
    )

    private fun buildInlineMcqSchema(): JSONObject = JSONObject().apply {
        put("type", "OBJECT")
        put("properties", JSONObject().apply {
            put("question", JSONObject().put("type", "STRING"))
            put("options", JSONObject().apply {
                put("type", "ARRAY")
                put("items", JSONObject().put("type", "STRING"))
            })
            put("correctIndex", JSONObject().put("type", "INTEGER"))
            put("explanation", JSONObject().put("type", "STRING"))
        })
        put("required", JSONArray().put("question").put("options").put("correctIndex").put("explanation"))
    }

    private fun buildCourseSchema(): JSONObject = JSONObject().apply {
        put("type", "OBJECT")
        put("properties", JSONObject().apply {
            put("categoryTag", JSONObject().put("type", "STRING"))
            put("nodes", JSONObject().apply {
                put("type", "ARRAY")
                put("items", JSONObject().apply {
                    put("type", "OBJECT")
                    put("properties", JSONObject().apply {
                        put("chapterLabel", JSONObject().put("type", "STRING"))
                        put("title", JSONObject().put("type", "STRING"))
                        put("subtitle", JSONObject().put("type", "STRING"))
                        put("sourceCitation", JSONObject().put("type", "STRING"))
                        put("steps", JSONObject().apply {
                            put("type", "ARRAY")
                            put("items", JSONObject().apply {
                                put("type", "OBJECT")
                                put("properties", JSONObject().apply {
                                    put("title", JSONObject().put("type", "STRING"))
                                    put("readingBody", JSONObject().put("type", "STRING"))
                                    put("trapBody", JSONObject().put("type", "STRING"))
                                    put("sourceCitation", JSONObject().put("type", "STRING"))
                                    put("checkpoint", buildInlineMcqSchema())
                                })
                                put("required", JSONArray().put("title").put("readingBody").put("sourceCitation"))
                            })
                        })
                    })
                    put("required", JSONArray().put("chapterLabel").put("title").put("subtitle").put("sourceCitation"))
                })
            })
            put("flashcards", JSONObject().apply {
                put("type", "ARRAY")
                put("items", JSONObject().apply {
                    put("type", "OBJECT")
                    put("properties", JSONObject().apply {
                        put("front", JSONObject().put("type", "STRING"))
                        put("back", JSONObject().put("type", "STRING"))
                        put("hint", JSONObject().put("type", "STRING"))
                        put("sourceCitation", JSONObject().put("type", "STRING"))
                    })
                    put("required", JSONArray().put("front").put("back").put("sourceCitation"))
                })
            })
            put("trueFalse", JSONObject().apply {
                put("type", "ARRAY")
                put("items", JSONObject().apply {
                    put("type", "OBJECT")
                    put("properties", JSONObject().apply {
                        put("prefix", JSONObject().put("type", "STRING"))
                        put("wrongClause", JSONObject().put("type", "STRING"))
                        put("correctClause", JSONObject().put("type", "STRING"))
                        put("isTrue", JSONObject().put("type", "BOOLEAN"))
                        put("explanation", JSONObject().put("type", "STRING"))
                        put("sourceCitation", JSONObject().put("type", "STRING"))
                    })
                    put("required", JSONArray().put("prefix").put("wrongClause").put("correctClause").put("isTrue").put("explanation"))
                })
            })
            put("writtenQuestions", JSONObject().apply {
                put("type", "ARRAY")
                put("items", JSONObject().apply {
                    put("type", "OBJECT")
                    put("properties", JSONObject().apply {
                        put("partLabel", JSONObject().put("type", "STRING"))
                        put("prompt", JSONObject().put("type", "STRING"))
                        put("standardSolution", JSONObject().put("type", "STRING"))
                        put("sourceCitation", JSONObject().put("type", "STRING"))
                    })
                    put("required", JSONArray().put("partLabel").put("prompt").put("standardSolution"))
                })
            })
            put("storyboard", JSONObject().apply {
                put("type", "ARRAY")
                put("items", JSONObject().apply {
                    put("type", "OBJECT")
                    put("properties", JSONObject().apply {
                        put("title", JSONObject().put("type", "STRING"))
                        put("description", JSONObject().put("type", "STRING"))
                        put("highlightFormula", JSONObject().put("type", "STRING"))
                        put("keyTakeaway", JSONObject().put("type", "STRING"))
                    })
                    put("required", JSONArray().put("title").put("description").put("keyTakeaway"))
                })
            })
        })
        put("required", JSONArray().put("nodes").put("flashcards"))
    }

    private fun parseGeneratedCourse(
        rawJson: String,
        courseTitle: String,
        examDaysLeft: Int,
        targetScore: Double,
        sourceFiles: List<String>
    ): Course {
        val root = JSONObject(rawJson)
        val courseId = "course_${System.currentTimeMillis()}"
        val defaultSrc = "📄 Nguồn tài liệu: ${sourceFiles.joinToString(", ").ifBlank { courseTitle }}"

        val nodesArr = root.optJSONArray("nodes") ?: JSONArray()
        val nodes = (0 until nodesArr.length()).map { i ->
            val nObj = nodesArr.getJSONObject(i)
            val stepsArr = nObj.optJSONArray("steps") ?: JSONArray()
            val builtSteps = mutableListOf<SmartTextStep>()
            val subtopics = (0 until stepsArr.length()).map { stepsArr.getJSONObject(it).optString("title") }
            builtSteps += SmartTextStep(
                stepNumber = 1,
                title = nObj.optString("title"),
                takeaway = nObj.optString("subtitle"),
                subtopics = subtopics.ifEmpty { listOf("Khái niệm cốt lõi", "Cơ chế vận động", "Luyện chốt chặn") },
                sourceCitation = nObj.optString("sourceCitation", defaultSrc)
            )
            for (sIdx in 0 until stepsArr.length()) {
                val sObj = stepsArr.getJSONObject(sIdx)
                val cp = sObj.optJSONObject("checkpoint")?.let { m ->
                    val opts = m.optJSONArray("options")
                    InlineMcq(
                        question = sanitizeUnicodeMath(m.optString("question")),
                        options = (0 until (opts?.length() ?: 0)).map { sanitizeUnicodeMath(opts!!.optString(it)) }
                            .ifEmpty { listOf("Phương án A", "Phương án B", "Phương án C", "Phương án D") },
                        correctIndex = m.optInt("correctIndex", 0).coerceIn(0, 3),
                        explanation = sanitizeUnicodeMath(m.optString("explanation")),
                        conceptTag = nObj.optString("title"),
                        sourceCitation = sObj.optString("sourceCitation", defaultSrc)
                    )
                }
                builtSteps += SmartTextStep(
                    stepNumber = sIdx + 2,
                    title = sanitizeUnicodeMath(sObj.optString("title")),
                    readingBody = sanitizeUnicodeMath(sObj.optString("readingBody")),
                    sourceCitation = sObj.optString("sourceCitation", defaultSrc),
                    trapTitle = if (sObj.optString("trapBody").isNotBlank()) "❗ Bẫy tư duy thường gặp" else null,
                    trapBody = sObj.optString("trapBody").takeIf { it.isNotBlank() }?.let(::sanitizeUnicodeMath),
                    inlineMcq = cp
                )
            }
            LearningNode(
                id = "${courseId}_n_${i + 1}",
                orderIndex = i + 1,
                chapterLabel = nObj.optString("chapterLabel", "Trạm ${i + 1}"),
                title = sanitizeUnicodeMath(nObj.optString("title")),
                subtitle = sanitizeUnicodeMath(nObj.optString("subtitle")),
                primaryMode = if (i == nodesArr.length() - 1) LearningMode.WRITTEN_EXAM else LearningMode.SMART_TEXT,
                sourceCitation = nObj.optString("sourceCitation", defaultSrc),
                isUnlocked = true,
                smartSteps = builtSteps
            )
        }

        val fcArr = root.optJSONArray("flashcards") ?: JSONArray()
        val flashcards = (0 until fcArr.length()).map { i ->
            val o = fcArr.getJSONObject(i)
            FlashcardItem(
                id = "${courseId}_fc_$i",
                courseId = courseId,
                front = sanitizeUnicodeMath(o.optString("front")),
                back = sanitizeUnicodeMath(o.optString("back")),
                hint = sanitizeUnicodeMath(o.optString("hint", "Ghi nhớ từ khóa chính")),
                sourceCitation = o.optString("sourceCitation", defaultSrc)
            )
        }

        val tfArr = root.optJSONArray("trueFalse") ?: JSONArray()
        val tfDeck = (0 until tfArr.length()).map { i ->
            val o = tfArr.getJSONObject(i)
            StrikeThroughTf(
                id = "${courseId}_tf_$i",
                prefix = sanitizeUnicodeMath(o.optString("prefix")),
                wrongClause = sanitizeUnicodeMath(o.optString("wrongClause")),
                correctClause = sanitizeUnicodeMath(o.optString("correctClause")),
                isTrue = o.optBoolean("isTrue", false),
                explanation = sanitizeUnicodeMath(o.optString("explanation")),
                conceptTag = courseTitle,
                sourceCitation = o.optString("sourceCitation", defaultSrc)
            )
        }

        val wArr = root.optJSONArray("writtenQuestions") ?: JSONArray()
        val written = (0 until wArr.length()).map { i ->
            val o = wArr.getJSONObject(i)
            WrittenExamQuestion(
                id = "${courseId}_wq_$i",
                partLabel = o.optString("partLabel", "Câu ${i + 1}"),
                points = 5.0,
                prompt = sanitizeUnicodeMath(o.optString("prompt")),
                standardSolution = sanitizeUnicodeMath(o.optString("standardSolution")),
                keyRubricPoints = listOf("Trình bày đúng bản chất khái niệm", "Lập luận chặt chẽ theo giáo trình"),
                sourceCitation = o.optString("sourceCitation", defaultSrc)
            )
        }

        val sbArr = root.optJSONArray("storyboard") ?: JSONArray()
        val storyboard = (0 until sbArr.length()).map { i ->
            val o = sbArr.getJSONObject(i)
            StoryboardFrame(
                stepLabel = "Khung ${i + 1}",
                title = sanitizeUnicodeMath(o.optString("title")),
                description = sanitizeUnicodeMath(o.optString("description")),
                highlightFormula = sanitizeUnicodeMath(o.optString("highlightFormula")),
                keyTakeaway = sanitizeUnicodeMath(o.optString("keyTakeaway"))
            )
        }

        val quizBank = nodes.flatMap { n ->
            n.smartSteps.mapNotNull { it.inlineMcq }.mapIndexed { idx, m ->
                QuizQuestion(
                    id = "${n.id}_q_$idx",
                    question = m.question,
                    options = m.options,
                    correctIndices = listOf(m.correctIndex),
                    isMultiSelect = false,
                    difficulty = if (idx % 2 == 0) QuizDifficulty.BASIC else QuizDifficulty.MEDIUM,
                    explanation = m.explanation,
                    conceptTag = m.conceptTag,
                    sourceCitation = m.sourceCitation
                )
            }
        }

        val autoPodcast = listOf(
            PodcastChapter(
                chapterNumber = 1,
                title = "Tổng quan trọng tâm: $courseTitle",
                lines = listOf(
                    PodcastLine(PodcastSpeaker.HOST_MINH, "Chào cô Lan! Em vừa nạp tài liệu môn $courseTitle, phần nào quan trọng nhất dễ ra thi ạ?"),
                    PodcastLine(PodcastSpeaker.TS_LAN, "Chào Minh! Từ tài liệu em vừa nạp, trọng tâm lớn nhất nằm ở ${nodes.firstOrNull()?.title ?: courseTitle}. Mình cần nắm chắc bản chất thay vì học thuộc lòng máy móc!"),
                    PodcastLine(PodcastSpeaker.HOST_MINH, "Dạ em rõ rồi, để em vào làm ngay bài Văn bản thông minh và bộ thẻ ghi nhớ!")
                ),
                checkpoint = quizBank.firstOrNull()?.let {
                    InlineMcq(it.question, it.options, it.correctIndices.firstOrNull() ?: 0, it.explanation, it.conceptTag, it.sourceCitation)
                } ?: InlineMcq("Trọng tâm đầu tiên của khóa học là gì?", listOf(nodes.firstOrNull()?.title ?: courseTitle, "Không có trọng tâm", "Bỏ qua lý thuyết", "Chỉ học mẹo"), 0, "Cần nắm vững trạm 1 trước khi nâng cao.", courseTitle, defaultSrc)
            )
        )

        return Course(
            id = courseId,
            title = courseTitle,
            categoryTag = root.optString("categoryTag", "TÀI LIỆU TỰ NẠP · AI"),
            sourceBadge = defaultSrc,
            examDaysLeft = examDaysLeft,
            targetScore = targetScore,
            readinessPercent = 5,
            starsEarned = 0,
            nodes = nodes,
            flashcards = flashcards,
            exercises = emptyList(),
            quizBank = quizBank,
            trueFalseDeck = tfDeck,
            podcastChapters = autoPodcast,
            writtenExamQuestions = written,
            storyboardFrames = storyboard,
            isKtctFlagship = false
        )
    }

    companion object {
        /**
         * Ensures zero raw LaTeX ($...$, \frac, \times) slips into Tutor chat or cards.
         */
        fun sanitizeUnicodeMath(input: String): String {
            var s = input
            s = s.replace(Regex("\\\\frac\\{([^}]+)\\}\\{([^}]+)\\}"), "($1 / $2)")
            s = s.replace("\\times", "×")
            s = s.replace("\\cdot", "·")
            s = s.replace("\\rightarrow", "→")
            s = s.replace("\\Rightarrow", "⇒")
            s = s.replace("\\le", "≤")
            s = s.replace("\\ge", "≥")
            s = s.replace("\\neq", "≠")
            s = s.replace("$$", "")
            s = s.replace("$", "")
            return s.trim()
        }
    }
}
