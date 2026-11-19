package com.example.data.seed

import com.example.model.Course
import com.example.model.ExerciseProblem
import com.example.model.FlashcardItem
import com.example.model.InlineMcq
import com.example.model.PodcastChapter
import com.example.model.PodcastLine
import com.example.model.PodcastSpeaker
import com.example.model.QuizDifficulty
import com.example.model.QuizQuestion
import com.example.model.RemediationRecord
import com.example.model.StoryboardFrame
import com.example.model.StrikeThroughTf
import com.example.model.WrittenExamQuestion

object KtctModesSeed {
    const val KTCT_COURSE_ID = "course_ktct_flagship"

    fun buildFlagshipCourse(): Course = Course(
        id = KTCT_COURSE_ID,
        title = "Kinh tế chính trị Mác - Lênin",
        categoryTag = "LÝ LUẬN CHÍNH TRỊ · ĐẠI HỌC",
        sourceBadge = "📄 Giáo trình Kinh tế chính trị Mác - Lênin (Bộ GD&ĐT)",
        examDaysLeft = 14,
        targetScore = 8.5,
        readinessPercent = 22,
        starsEarned = 140,
        nodes = KtctNodesSeed.buildNodes(),
        flashcards = buildFlashcards(),
        exercises = buildExercises(),
        quizBank = buildQuizBank(),
        trueFalseDeck = buildTrueFalseDeck(),
        podcastChapters = buildPodcastChapters(),
        writtenExamQuestions = buildWrittenExam(),
        storyboardFrames = buildStoryboardFrames(),
        isKtctFlagship = true
    )

    fun initialRemediations(): List<RemediationRecord> = listOf(
        RemediationRecord(
            courseId = KTCT_COURSE_ID,
            nodeId = "ktct_node_1",
            conceptTag = "NSLĐ vs Cường độ lao động",
            originalQuestion = "Khi tăng cường độ lao động 2 lần, giá trị 1 đơn vị hàng hóa thay đổi thế nào?",
            mistakeSummary = "Dễ nhầm lẫn tác động của tăng CĐLĐ (giá trị 1 SP không đổi) với tăng NSLĐ (giá trị 1 SP giảm).",
            wrongCount = 2
        ),
        RemediationRecord(
            courseId = KTCT_COURSE_ID,
            nodeId = "ktct_node_4",
            conceptTag = "Tư bản cố định/lưu động vs c/v",
            originalQuestion = "Nguyên nhiên vật liệu thuộc cặp phạm trù tư bản nào?",
            mistakeSummary = "Thường quên rằng nguyên vật liệu vừa thuộc Tư bản bất biến (c) vừa thuộc Tư bản lưu động.",
            wrongCount = 1
        )
    )

    private fun buildFlashcards(): List<FlashcardItem> = listOf(
        FlashcardItem("fc_1", KTCT_COURSE_ID, "Hai điều kiện ra đời sản xuất hàng hóa?", "1) Phân công lao động xã hội\n2) Sự tách biệt tương đối về mặt kinh tế giữa các chủ thể sản xuất.", "Nhớ: Cần (PCLĐ) + Đủ (Tách biệt kinh tế)", "📄 Chương 2, Mục 2.1"),
        FlashcardItem("fc_2", KTCT_COURSE_ID, "Lao động cụ thể vs. Lao động trừu tượng tạo ra gì?", "• Lao động cụ thể → Giá trị sử dụng (phạm trù vĩnh viễn)\n• Lao động trừu tượng → Giá trị hàng hóa (phạm trù lịch sử)", "Cụ thể = Công dụng; Trừu tượng = Hao phí sức lực", "📄 Chương 2, Mục 2.1.3"),
        FlashcardItem("fc_3", KTCT_COURSE_ID, "Tăng NSLĐ ảnh hưởng thế nào đến giá trị 1 SP & tổng giá trị?", "Giá trị 1 đơn vị sản phẩm GIẢM (tỷ lệ nghịch), còn tổng giá trị tạo ra trong cùng thời gian KHÔNG ĐỔI.", "NSLĐ tăng → mỗi cái rẻ hơn", "📄 Chương 2, Mục 2.1.4"),
        FlashcardItem("fc_4", KTCT_COURSE_ID, "Tăng Cường độ lao động (CĐLĐ) ảnh hưởng thế nào?", "Giá trị 1 đơn vị sản phẩm KHÔNG ĐỔI, còn tổng lượng giá trị tạo ra TĂNG tương ứng với mức tăng sản phẩm.", "CĐLĐ tăng = làm gấp gáp hơn", "📄 Chương 2, Mục 2.1.4"),
        FlashcardItem("fc_5", KTCT_COURSE_ID, "2 điều kiện để Sức lao động trở thành hàng hóa?", "1) Người lao động tự do về thân thể\n2) Người lao động không có đủ tư liệu sản xuất để tự tạo ra sản phẩm bán.", "Tự do thân thể + Thiếu TLSX", "📄 Chương 3, Mục 3.1.1"),
        FlashcardItem("fc_6", KTCT_COURSE_ID, "Công thức Tỷ suất (m') & Khối lượng (M) giá trị thặng dư?", "m' = (m / v) × 100% = (t' / t) × 100%\nM = m' × V", "m' đo trình độ; M đo quy mô", "📄 Chương 3, Mục 3.1.3"),
        FlashcardItem("fc_7", KTCT_COURSE_ID, "3 phương pháp sản xuất giá trị thặng dư?", "1) Tuyệt đối: kéo dài ngày LĐ / tăng CĐLĐ\n2) Tương đối: tăng NSLĐ XH để rút ngắn t\n3) Siêu ngạch: tăng NSLĐ cá biệt (hình thức biến tướng của GTTD tương đối).", "Tuyệt đối kéo dài; Tương đối rút t", "📄 Chương 3, Mục 3.1.3"),
        FlashcardItem("fc_8", KTCT_COURSE_ID, "Phân biệt Tư bản cố định (c₁) và Tư bản lưu động (c₂ + v)?", "• Cố định: máy móc, nhà xưởng (chuyển giá trị dần qua khấu hao)\n• Lưu động: nguyên vật liệu + tiền lương (chuyển 1 lần sau chu kỳ).", "Căn cứ: phương thức chu chuyển giá trị", "📄 Chương 3, Mục 3.1.4"),
        FlashcardItem("fc_9", KTCT_COURSE_ID, "So sánh Tỷ suất lợi nhuận (p') và Tỷ suất GTTD (m')?", "p' = m / (c + v) × 100%\nVề lượng: p' luôn nhỏ hơn m' (do chia thêm cho c).\nVề chất: p' chỉ nói lên mức sinh lời, che giấu bản chất bóc lột.", "Mẫu số của p' là (c + v)", "📄 Chương 3, Mục 3.3"),
        FlashcardItem("fc_10", KTCT_COURSE_ID, "Cấu tạo hữu cơ của tư bản (c/v) tăng dẫn đến hệ quả gì?", "Tích tụ & tập trung tư bản tăng; tỷ trọng v tương đối giảm dẫn tới nạn thất nghiệp cơ cấu (đội quân dự bị công nghiệp).", "c/v phản ánh cấu tạo kỹ thuật quyết định cấu tạo giá trị", "📄 Chương 3, Mục 3.2"),
        FlashcardItem("fc_11", KTCT_COURSE_ID, "Vai trò các thành phần kinh tế trong KTTT ĐHXHCN Việt Nam?", "• Kinh tế nhà nước: giữ vai trò CHỦ ĐẠO\n• Kinh tế tư nhân: là MỘT ĐỘNG LỰC QUAN TRỌNG\n• Kinh tế tập thể, HTX: không ngừng củng cố.", "Nhà nước chủ đạo · Tư nhân động lực", "📄 Chương 5, Mục 5.1.3"),
        FlashcardItem("fc_12", KTCT_COURSE_ID, "Mối quan hệ giữa Hội nhập KTQT và Kinh tế độc lập, tự chủ?", "Biện chứng: Độc lập tự chủ là nền tảng để hội nhập hiệu quả không lệ thuộc; hội nhập giúp tăng cường nguồn lực củng cố độc lập tự chủ.", "Hội nhập nhưng không hòa tan/lệ thuộc", "📄 Chương 6, Mục 6.2")
    )

    private fun buildExercises(): List<ExerciseProblem> = listOf(
        ExerciseProblem(
            id = "ex_1",
            title = "Bài toán 1: Tính c, v, m, m' và p' cơ bản",
            prompt = "Một doanh nghiệp tư bản ứng ra tổng tư bản K = 1.000 tỷ đồng với cấu tạo hữu cơ c/v = 4/1. Biết tỷ suất giá trị thặng dư m' = 150%. Hãy tính c, v, m, giá trị tổng sản phẩm G và tỷ suất lợi nhuận p'.",
            formulaHint = "K = c + v; c/v = 4/1 ⇒ c = 4v; m = m' × v; p' = m / (c + v) × 100%",
            expectedFinalAnswer = "c = 800 tỷ; v = 200 tỷ; m = 300 tỷ; G = 1.300 tỷ; p' = 30%",
            solutionSteps = listOf(
                "Bước 1: Ta có c + v = 1.000 và c = 4v ⇒ 5v = 1.000 ⇒ v = 200 tỷ đồng, c = 800 tỷ đồng.",
                "Bước 2: Giá trị thặng dư m = m' × v = 150% × 200 = 300 tỷ đồng.",
                "Bước 3: Tổng giá trị hàng hóa G = c + v + m = 800 + 200 + 300 = 1.300 tỷ đồng.",
                "Bước 4: Tỷ suất lợi nhuận p' = (m / (c + v)) × 100% = (300 / 1.000) × 100% = 30%."
            ),
            sourceCitation = "📄 Chương 3, Mục 3.1.3 & 3.3"
        ),
        ExerciseProblem(
            id = "ex_2",
            title = "Bài toán 2: Thay đổi ngày lao động & Phương pháp SX GTTD",
            prompt = "Ngày lao động dài 8 giờ, tỷ suất giá trị thặng dư m' = 100%. Nếu nhà tư bản kéo dài ngày lao động lên 10 giờ (giá trị sức lao động không đổi), m' mới bằng bao nhiêu? Đây là phương pháp sản xuất GTTD nào?",
            formulaHint = "t + t' = 8h và t'/t = 1 ⇒ tìm t trước, sau đó tính t'_mới = 10 - t",
            expectedFinalAnswer = "t = 4 giờ, t' mới = 6 giờ ⇒ m' mới = 150% (Phương pháp sản xuất GTTD tuyệt đối)",
            solutionSteps = listOf(
                "Bước 1: Ban đầu m' = t'/t = 100% ⇒ t' = t. Vì t + t' = 8 giờ nên t = 4 giờ, t' = 4 giờ.",
                "Bước 2: Khi kéo dài ngày lao động lên 10 giờ mà t giữ nguyên 4 giờ ⇒ t' mới = 10 - 4 = 6 giờ.",
                "Bước 3: Tỷ suất GTTD mới m' = (6 / 4) × 100% = 150%.",
                "Bước 4: Kết luận — Đây là phương pháp sản xuất giá trị thặng dư TUYỆT ĐỐI."
            ),
            sourceCitation = "📄 Chương 3, Mục 3.1.3"
        ),
        ExerciseProblem(
            id = "ex_3",
            title = "Bài toán 3: Khối lượng GTTD & Tích lũy tư bản",
            prompt = "Một nhà máy thuê 200 công nhân, mỗi tháng trả lương 10 triệu/người. Tỷ suất GTTD m' = 120%. Nhà tư bản dùng 40% giá trị thặng dư để tiêu dùng cá nhân, phần còn lại để tích lũy mở rộng. Tính tổng khối lượng GTTD (M) và lượng tư bản tích lũy mỗi tháng.",
            formulaHint = "V = số công nhân × tiền lương; M = m' × V; Tích lũy = 60% × M",
            expectedFinalAnswer = "V = 2.000 triệu; M = 2.400 triệu (2,4 tỷ); Tư bản tích lũy = 1.440 triệu (1,44 tỷ)",
            solutionSteps = listOf(
                "Bước 1: Tổng tư bản khả biến V = 200 × 10 = 2.000 triệu đồng (2 tỷ).",
                "Bước 2: Khối lượng giá trị thặng dư M = m' × V = 120% × 2.000 = 2.400 triệu đồng.",
                "Bước 3: Tỷ lệ dành cho tích lũy = 100% - 40% = 60%.",
                "Bước 4: Lượng tư bản hóa giá trị thặng dư = 60% × 2.400 = 1.440 triệu đồng."
            ),
            sourceCitation = "📄 Chương 3, Mục 3.2"
        ),
        ExerciseProblem(
            id = "ex_4",
            title = "Bài toán 4: Tư bản cố định, Tư bản lưu động & Chi phí SX",
            prompt = "Tư bản ứng trước gồm: nhà xưởng và máy móc 600 tỷ (khấu hao trong 10 năm), nguyên nhiên vật liệu 300 tỷ/năm, tiền lương công nhân 100 tỷ/năm. Giả sử m' = 100%. Tính giá trị tư bản cố định, tư bản lưu động và tổng giá trị sản phẩm sản xuất trong 1 năm.",
            formulaHint = "c₁ (khấu hao năm) = 600 / 10; G_năm = c₁_năm + c₂ + v + m",
            expectedFinalAnswer = "TB cố định = 600 tỷ (khấu hao 60 tỷ/năm); TB lưu động = 400 tỷ; G_năm = 560 tỷ",
            solutionSteps = listOf(
                "Bước 1: Tư bản cố định = 600 tỷ đồng; phần khấu hao chuyển vào sản phẩm mỗi năm c₁ = 600 / 10 = 60 tỷ đồng.",
                "Bước 2: Tư bản lưu động = nguyên vật liệu (c₂ = 300 tỷ) + tiền lương (v = 100 tỷ) = 400 tỷ đồng.",
                "Bước 3: Giá trị thặng dư năm m = m' × v = 100% × 100 = 100 tỷ đồng.",
                "Bước 4: Tổng giá trị sản phẩm trong năm G = 60 + 300 + 100 + 100 = 560 tỷ đồng."
            ),
            sourceCitation = "📄 Chương 3, Mục 3.1.4"
        )
    )

    private fun buildQuizBank(): List<QuizQuestion> = listOf(
        // BASIC
        QuizQuestion(
            id = "qb_1",
            question = "Thuộc tính nào của hàng hóa phản ánh quan hệ xã hội giữa những người sản xuất hàng hóa?",
            options = listOf("Giá trị sử dụng", "Giá trị hàng hóa", "Trọng lượng hàng hóa", "Mẫu mã bao bì"),
            correctIndices = listOf(1),
            isMultiSelect = false,
            difficulty = QuizDifficulty.BASIC,
            explanation = "Giá trị là lao động xã hội kết tinh, phản ánh quan hệ kinh tế giữa những người sản xuất hàng hóa.",
            conceptTag = "Hai thuộc tính của hàng hóa",
            sourceCitation = "📄 Chương 2, Mục 2.1.2"
        ),
        QuizQuestion(
            id = "qb_2",
            question = "Nguồn gốc duy nhất của tích lũy tư bản là gì?",
            options = listOf("Tiền tiết kiệm từ tiêu dùng cá nhân", "Giá trị thặng dư (tư bản hóa m)", "Vốn vay ngân hàng trung ương", "Khấu hao máy móc"),
            correctIndices = listOf(1),
            isMultiSelect = false,
            difficulty = QuizDifficulty.BASIC,
            explanation = "Tích lũy tư bản là sự chuyển hóa một phần giá trị thặng dư trở lại thành tư bản phụ thêm.",
            conceptTag = "Tích lũy tư bản",
            sourceCitation = "📄 Chương 3, Mục 3.2"
        ),
        QuizQuestion(
            id = "qb_3",
            question = "Thời gian chu chuyển của tư bản bằng tổng của hai khoảng thời gian nào?",
            options = listOf(
                "Thời gian sản xuất + Thời gian lưu thông",
                "Thời gian làm việc + Thời gian nghỉ giải lao",
                "Thời gian mua máy + Thời gian khấu hao",
                "Thời gian lao động tất yếu + Thời gian dự trữ"
            ),
            correctIndices = listOf(0),
            isMultiSelect = false,
            difficulty = QuizDifficulty.BASIC,
            explanation = "Chu chuyển tư bản tính từ lúc ứng tiền mua yếu tố sản xuất cho đến khi bán xong hàng hóa thu tiền về = T_sản xuất + T_lưu thông.",
            conceptTag = "Tuần hoàn và chu chuyển tư bản",
            sourceCitation = "📄 Chương 3, Mục 3.1.4"
        ),
        QuizQuestion(
            id = "qb_4",
            question = "Độc quyền hình thành trực tiếp từ nguyên nhân nào sau đây theo V.I. Lênin?",
            options = listOf(
                "Sự tích tụ và tập trung sản xuất đạt đến mức độ cao",
                "Do nhà nước cấm hoàn toàn doanh nghiệp nhỏ",
                "Do không còn lưu thông tiền tệ",
                "Do giảm năng suất lao động xã hội"
            ),
            correctIndices = listOf(0),
            isMultiSelect = false,
            difficulty = QuizDifficulty.BASIC,
            explanation = "Cạnh tranh tự do thúc đẩy tích tụ và tập trung sản xuất đến mức độ cao làm nảy sinh tổ chức độc quyền.",
            conceptTag = "Nguyên nhân hình thành độc quyền",
            sourceCitation = "📄 Chương 4, Mục 4.1"
        ),
        // MEDIUM (including Multi-Select!)
        QuizQuestion(
            id = "qb_5",
            question = "Chọn TẤT CẢ các mệnh đề ĐÚNG về Nguyên nhiên vật liệu trong quá trình sản xuất tư bản:",
            options = listOf(
                "Thuộc Tư bản bất biến (c)",
                "Thuộc Tư bản lưu động",
                "Thuộc Tư bản khả biến (v)",
                "Chuyển toàn bộ giá trị 1 lần vào sản phẩm sau chu kỳ sản xuất"
            ),
            correctIndices = listOf(0, 1, 3),
            isMultiSelect = true,
            difficulty = QuizDifficulty.MEDIUM,
            explanation = "Đây là bẫy thi kinh điển! Nguyên nhiên vật liệu vừa thuộc Tư bản bất biến (c — không tạo thêm m), vừa thuộc Tư bản lưu động (chuyển hết giá trị 1 lần vào sản phẩm).",
            conceptTag = "Tư bản cố định/lưu động vs c/v",
            sourceCitation = "📄 Chương 3, Mục 3.1.4"
        ),
        QuizQuestion(
            id = "qb_6",
            question = "Chọn TẤT CẢ các yếu tố thuộc Tư bản lưu động:",
            options = listOf(
                "Tiền lương trả cho công nhân (v)",
                "Nguyên liệu, nhiên liệu, vật liệu phụ (c₂)",
                "Nhà xưởng, dây chuyền máy móc (c₁)",
                "Bằng sáng chế khấu hao nhiều năm"
            ),
            correctIndices = listOf(0, 1),
            isMultiSelect = true,
            difficulty = QuizDifficulty.MEDIUM,
            explanation = "Tư bản lưu động = c₂ (nguyên vật liệu) + v (tiền lương), vì giá trị của chúng quay vòng toàn bộ sau một chu kỳ sản xuất.",
            conceptTag = "Tư bản cố định/lưu động vs c/v",
            sourceCitation = "📄 Chương 3, Mục 3.1.4"
        ),
        QuizQuestion(
            id = "qb_7",
            question = "Tốc độ chu chuyển tư bản (n) tính theo công thức n = CH / ch. Khi thời gian một vòng chu chuyển (ch) giảm từ 6 tháng xuống 3 tháng thì:",
            options = listOf(
                "n giảm từ 4 xuống 2 vòng/năm",
                "n tăng từ 2 lên 4 vòng/năm",
                "n giữ nguyên không đổi",
                "Lợi nhuận hằng năm giảm một nửa"
            ),
            correctIndices = listOf(1),
            isMultiSelect = false,
            difficulty = QuizDifficulty.MEDIUM,
            explanation = "CH = 12 tháng. Trước: n = 12/6 = 2 vòng/năm; Sau: n = 12/3 = 4 vòng/năm, giúp tăng khối lượng GTTD hằng năm.",
            conceptTag = "Tuần hoàn và chu chuyển tư bản",
            sourceCitation = "📄 Chương 3, Mục 3.1.4"
        ),
        QuizQuestion(
            id = "qb_8",
            question = "Điểm giống nhau giữa Tích tụ tư bản và Tập trung tư bản là gì?",
            options = listOf(
                "Đều làm tăng quy mô tư bản xã hội",
                "Đều làm tăng quy mô tư bản cá biệt",
                "Đều chỉ xuất phát từ nguồn giá trị thặng dư",
                "Đều diễn ra thông qua sáp nhập doanh nghiệp"
            ),
            correctIndices = listOf(1),
            isMultiSelect = false,
            difficulty = QuizDifficulty.MEDIUM,
            explanation = "Cả tích tụ và tập trung đều làm tăng quy mô tư bản cá biệt; khác nhau ở chỗ tích tụ làm tăng tư bản xã hội (từ m), còn tập trung chỉ hợp nhất các tư bản sẵn có.",
            conceptTag = "Tích tụ và tập trung tư bản",
            sourceCitation = "📄 Chương 3, Mục 3.2"
        ),
        // ADVANCED (Multi-select & Deep Mechanism)
        QuizQuestion(
            id = "qb_9",
            question = "Chọn TẤT CẢ các mệnh đề ĐÚNG về Giá trị thặng dư siêu ngạch:",
            options = listOf(
                "Thu được nhờ áp dụng công nghệ mới làm giá trị cá biệt thấp hơn giá trị xã hội",
                "Là hiện tượng tạm thời với từng doanh nghiệp nhưng tồn tại thường xuyên trong toàn xã hội",
                "Là hình thức biến tướng của giá trị thặng dư tương đối",
                "Sinh ra do kéo dài ngày lao động lên 14 giờ/ngày"
            ),
            correctIndices = listOf(0, 1, 2),
            isMultiSelect = true,
            difficulty = QuizDifficulty.ADVANCED,
            explanation = "GTTD siêu ngạch dựa trên tăng NSLĐ cá biệt (tạm thời với 1 hãng, thường xuyên toàn XH), là động lực mạnh nhất thúc đẩy cải tiến kỹ thuật.",
            conceptTag = "3 Phương pháp SX GTTD",
            sourceCitation = "📄 Chương 3, Mục 3.1.3"
        ),
        QuizQuestion(
            id = "qb_10",
            question = "Trong quan hệ giữa Độc quyền và Cạnh tranh ở Chương 4, chọn TẤT CẢ các nhận định chính xác:",
            options = listOf(
                "Độc quyền sinh ra từ cạnh tranh tự do",
                "Độc quyền không thủ tiêu cạnh tranh mà làm cạnh tranh đa dạng, gay gắt hơn",
                "Độc quyền xóa bỏ hoàn toàn mọi hình thức cạnh tranh trên thị trường",
                "Tồn tại cạnh tranh giữa tổ chức độc quyền với doanh nghiệp ngoài độc quyền và giữa các tổ chức độc quyền với nhau"
            ),
            correctIndices = listOf(0, 1, 3),
            isMultiSelect = true,
            difficulty = QuizDifficulty.ADVANCED,
            explanation = "V.I. Lênin khẳng định độc quyền không thủ tiêu cạnh tranh mà tồn tại song song và làm cạnh tranh phức tạp, khốc liệt hơn.",
            conceptTag = "Độc quyền và cạnh tranh",
            sourceCitation = "📄 Chương 4, Mục 4.1"
        ),
        QuizQuestion(
            id = "qb_11",
            question = "Nếu cấu tạo hữu cơ c/v tăng lên trong khi m' không đổi và tốc độ chu chuyển không đổi, Tỷ suất lợi nhuận (p') sẽ biến động thế nào?",
            options = listOf(
                "Có xu hướng giảm xuống",
                "Luôn tăng mạnh gấp đôi",
                "Lớn hơn tỷ suất giá trị thặng dư m'",
                "Bằng đúng 100%"
            ),
            correctIndices = listOf(0),
            isMultiSelect = false,
            difficulty = QuizDifficulty.ADVANCED,
            explanation = "p' = m' / (c/v + 1). Khi c/v tăng mà m' không đổi, mẫu số tăng làm p' có xu hướng giảm!",
            conceptTag = "Cấu tạo hữu cơ và p'",
            sourceCitation = "📄 Chương 3, Mục 3.3"
        ),
        QuizQuestion(
            id = "qb_12",
            question = "Chọn TẤT CẢ các đặc trưng kinh tế của Độc quyền trong CNTB theo V.I. Lênin:",
            options = listOf(
                "Tích tụ và tập trung sản xuất tạo ra các tổ chức độc quyền",
                "Tư bản tài chính và hệ thống tài phiệt chi phối",
                "Xuất khẩu tư bản trở nên phổ biến (thay vì chỉ xuất khẩu hàng hóa)",
                "Phân chia thế giới về kinh tế và lãnh thổ giữa các cường quốc tư bản"
            ),
            correctIndices = listOf(0, 1, 2, 3),
            isMultiSelect = true,
            difficulty = QuizDifficulty.ADVANCED,
            explanation = "Đó là trọn vẹn các đặc trưng kinh tế cơ bản của độc quyền trong CNTB do V.I. Lênin tổng kết.",
            conceptTag = "5 đặc trưng độc quyền CNTB",
            sourceCitation = "📄 Chương 4, Mục 4.1.2"
        )
    )

    private fun buildTrueFalseDeck(): List<StrikeThroughTf> = listOf(
        StrikeThroughTf(
            id = "blitz_1",
            prefix = "Tăng năng suất lao động xã hội làm cho lượng giá trị của một đơn vị hàng hóa ",
            wrongClause = "tăng lên tương ứng",
            correctClause = "giảm xuống tỷ lệ nghịch",
            isTrue = false,
            explanation = "NSLĐ tăng → thời gian lao động xã hội cần thiết để sản xuất 1 đơn vị sản phẩm giảm → lượng giá trị 1 đơn vị giảm.",
            conceptTag = "NSLĐ vs Cường độ lao động",
            sourceCitation = "📄 Chương 2, Mục 2.1.4"
        ),
        StrikeThroughTf(
            id = "blitz_2",
            prefix = "Lao động trừu tượng là nguồn gốc duy nhất tạo ra ",
            wrongClause = "giá trị của hàng hóa",
            correctClause = "giá trị của hàng hóa",
            isTrue = true,
            explanation = "Chính xác! Lao động trừu tượng là hao phí sức lao động nói chung, tạo nên thực thể giá trị hàng hóa.",
            conceptTag = "Tính hai mặt của lao động",
            sourceCitation = "📄 Chương 2, Mục 2.1.3"
        ),
        StrikeThroughTf(
            id = "blitz_3",
            prefix = "Trong nhà máy, máy móc tự động hiện đại có khả năng ",
            wrongClause = "tự mình sinh ra giá trị thặng dư mới vượt quá giá trị khấu hao",
            correctClause = "bảo tồn và chuyển dần giá trị cũ vào sản phẩm, không tự sinh ra m",
            isTrue = false,
            explanation = "Máy móc thuộc tư bản bất biến (c), là điều kiện cần thiết nhưng chỉ lao động sống của con người mới tạo ra giá trị mới (v + m).",
            conceptTag = "Tư bản bất biến và khả biến",
            sourceCitation = "📄 Chương 3, Mục 3.1.2"
        ),
        StrikeThroughTf(
            id = "blitz_4",
            prefix = "Tỷ suất lợi nhuận (p') về mặt lượng luôn ",
            wrongClause = "nhỏ hơn tỷ suất giá trị thặng dư (m')",
            correctClause = "nhỏ hơn tỷ suất giá trị thặng dư (m')",
            isTrue = true,
            explanation = "Đúng! Vì p' = m / (c + v) còn m' = m / v; do c > 0 nên mẫu số (c + v) > v ⇒ p' < m'.",
            conceptTag = "Công thức m' và p'",
            sourceCitation = "📄 Chương 3, Mục 3.3"
        ),
        StrikeThroughTf(
            id = "blitz_5",
            prefix = "Nguyên nhiên vật liệu thuộc Tư bản bất biến (c), nên nó cũng thuộc ",
            wrongClause = "Tư bản cố định",
            correctClause = "Tư bản lưu động (chuyển toàn bộ giá trị 1 lần sau chu kỳ)",
            isTrue = false,
            explanation = "Đừng bị lừa! Tiêu chí chia cố định/lưu động là phương thức chu chuyển giá trị, không trùng hoàn toàn với c/v.",
            conceptTag = "Tư bản cố định/lưu động vs c/v",
            sourceCitation = "📄 Chương 3, Mục 3.1.4"
        ),
        StrikeThroughTf(
            id = "blitz_6",
            prefix = "Phương pháp sản xuất giá trị thặng dư tương đối thực hiện bằng cách ",
            wrongClause = "kéo dài ngày lao động vượt quá giới hạn sinh lý",
            correctClause = "rút ngắn thời gian lao động tất yếu (t) nhờ tăng NSLĐ xã hội",
            isTrue = false,
            explanation = "Kéo dài ngày lao động là phương pháp sản xuất GTTD tuyệt đối; GTTD tương đối giữ nguyên độ dài ngày lao động nhưng giảm t.",
            conceptTag = "3 Phương pháp SX GTTD",
            sourceCitation = "📄 Chương 3, Mục 3.1.3"
        ),
        StrikeThroughTf(
            id = "blitz_7",
            prefix = "Tập trung tư bản làm tăng quy mô tư bản cá biệt nhưng ",
            wrongClause = "không trực tiếp làm tăng quy mô tư bản xã hội",
            correctClause = "không trực tiếp làm tăng quy mô tư bản xã hội",
            isTrue = true,
            explanation = "Chuẩn xác! Tập trung tư bản chỉ là hợp nhất nhiều tư bản cá biệt sẵn có trong xã hội thành một tư bản lớn hơn.",
            conceptTag = "Tích tụ và tập trung tư bản",
            sourceCitation = "📄 Chương 3, Mục 3.2"
        ),
        StrikeThroughTf(
            id = "blitz_8",
            prefix = "Khi độc quyền thống trị trong CNTB, quy luật cạnh tranh ",
            wrongClause = "bị thủ tiêu hoàn toàn trên thị trường",
            correctClause = "vẫn tồn tại và trở nên đa dạng, gay gắt hơn",
            isTrue = false,
            explanation = "Độc quyền sinh ra từ cạnh tranh nhưng không xóa bỏ cạnh tranh mà đẻ ra nhiều dạng cạnh tranh khốc liệt hơn.",
            conceptTag = "Độc quyền và cạnh tranh",
            sourceCitation = "📄 Chương 4, Mục 4.1"
        ),
        StrikeThroughTf(
            id = "blitz_9",
            prefix = "Trong nền KTTT định hướng XHCN ở Việt Nam, kinh tế tư nhân giữ vai trò là ",
            wrongClause = "một động lực quan trọng của nền kinh tế",
            correctClause = "một động lực quan trọng của nền kinh tế",
            isTrue = true,
            explanation = "Chính xác! Kinh tế nhà nước giữ vai trò chủ đạo, còn kinh tế tư nhân là một động lực quan trọng.",
            conceptTag = "Đặc trưng KTTT định hướng XHCN",
            sourceCitation = "📄 Chương 5, Mục 5.1.3"
        ),
        StrikeThroughTf(
            id = "blitz_10",
            prefix = "Hội nhập kinh tế quốc tế sâu rộng đồng nghĩa với việc Việt Nam phải ",
            wrongClause = "chấp nhận phụ thuộc hoàn toàn vào chuỗi cung ứng nước ngoài",
            correctClause = "song hành xây dựng nền kinh tế độc lập, tự chủ và nâng cao năng lực cạnh tranh",
            isTrue = false,
            explanation = "Xây dựng nền kinh tế độc lập, tự chủ và chủ động hội nhập kinh tế quốc tế là hai mặt gắn bó hữu cơ không thể tách rời.",
            conceptTag = "Hội nhập KTQT & Độc lập tự chủ",
            sourceCitation = "📄 Chương 6, Mục 6.2"
        )
    )

    private fun buildPodcastChapters(): List<PodcastChapter> = listOf(
        PodcastChapter(
            chapterNumber = 1,
            title = "Chương 1: Vì sao tăng NSLĐ lại giảm giá trị 1 sản phẩm?",
            lines = listOf(
                PodcastLine(PodcastSpeaker.HOST_MINH, "Chào cô Lan ạ! Em đang ôn chương 2 KTCT mà lú quá. Nếu em làm việc năng suất gấp đôi, ra gấp đôi số áo, sao giáo trình lại bảo tổng giá trị không đổi mà mỗi cái áo còn giảm giá trị?"),
                PodcastLine(PodcastSpeaker.TS_LAN, "Câu hỏi trúng tim đen đề thi đấy Minh! Em hãy nghĩ đơn giản: trong đúng 1 giờ, tổng hao phí sức lực của em vẫn chỉ là 1 giờ lao động xã hội."),
                PodcastLine(PodcastSpeaker.HOST_MINH, "À! Tức là 1 giờ đó trước đây chia cho 2 cái áo, mỗi cái gánh 30 phút. Giờ năng suất gấp đôi làm ra 4 cái áo, thì 1 giờ chia cho 4, mỗi cái chỉ còn 15 phút giá trị!"),
                PodcastLine(PodcastSpeaker.TS_LAN, "Chính xác tuyệt đối! Còn nếu em tăng cường độ lao động — chạy máy hộc tốc tốn gấp đôi sức lực trong 1 giờ — thì tổng giá trị mới tăng gấp đôi và giá trị 1 cái áo giữ nguyên.")
            ),
            checkpoint = InlineMcq(
                question = "Theo phân tích của TS. Lan, yếu tố nào làm TỔNG lượng giá trị tạo ra trong 1 giờ tăng lên?",
                options = listOf("Tăng năng suất lao động", "Tăng cường độ lao động", "Giảm giá nguyên liệu", "Giảm số công nhân"),
                correctIndex = 1,
                explanation = "Tăng cường độ lao động làm tăng tổng hao phí sức lực trong 1 giờ nên tổng giá trị tăng theo tỷ lệ sản phẩm.",
                conceptTag = "NSLĐ vs Cường độ lao động",
                sourceCitation = "📄 Podcast Chương 1 · Giáo trình Chương 2"
            )
        ),
        PodcastChapter(
            chapterNumber = 2,
            title = "Chương 2: Giải mã bẫy c, v, m và Tỷ suất lợi nhuận p'",
            lines = listOf(
                PodcastLine(PodcastSpeaker.HOST_MINH, "Cô ơi, sang chương 3 có câu trắc nghiệm hỏi: máy móc tự động robot có tạo ra giá trị thặng dư không? Em định khoanh Có vì không có robot sao sản xuất nhanh được!"),
                PodcastLine(PodcastSpeaker.TS_LAN, "Suýt nữa sập bẫy kinh điển nhé Minh! Robot thuộc tư bản bất biến c. Nó cực kỳ quan trọng như một điều kiện để tăng năng suất, nhưng bản thân máy móc chỉ chuyển dần giá trị cũ của chính nó qua khấu hao thôi."),
                PodcastLine(PodcastSpeaker.HOST_MINH, "Ồ, thế còn m' và p' thì sao ạ? Em hay bấm nhầm mẫu số khi đi thi lắm!"),
                PodcastLine(PodcastSpeaker.TS_LAN, "Mẹo nhớ 3 giây: m phẩy là chuyện giữa giá trị thặng dư và công nhân nên chỉ chia cho v. Còn p phẩy là lợi nhuận ông chủ tính trên toàn bộ vốn bỏ ra nên phải chia cho cả c cộng v!")
            ),
            checkpoint = InlineMcq(
                question = "Tại sao tỷ suất lợi nhuận p' luôn nhỏ hơn tỷ suất giá trị thặng dư m'?",
                options = listOf(
                    "Vì p' có tử số nhỏ hơn",
                    "Vì mẫu số của p' là (c + v) luôn lớn hơn mẫu số v của m'",
                    "Vì thuế thu nhập doanh nghiệp",
                    "Vì khấu hao bằng 0"
                ),
                correctIndex = 1,
                explanation = "p' = m/(c+v) × 100% < m/v × 100% = m' do tư bản bất biến c > 0.",
                conceptTag = "Công thức m' và p'",
                sourceCitation = "📄 Podcast Chương 2 · Giáo trình Chương 3"
            )
        ),
        PodcastChapter(
            chapterNumber = 3,
            title = "Chương 3: Kinh tế thị trường định hướng XHCN & CMCN 4.0",
            lines = listOf(
                PodcastLine(PodcastSpeaker.HOST_MINH, "Phần chương 5 và 6 hay ra câu tự luận liên hệ Việt Nam lắm cô ạ. Có phải kinh tế độc lập tự chủ nghĩa là mình hạn chế hội nhập không cô?"),
                PodcastLine(PodcastSpeaker.TS_LAN, "Hoàn toàn ngược lại Minh ạ! Độc lập tự chủ không bao giờ là đóng cửa. Mình chủ động hội nhập sâu qua các hiệp định FTA để hút công nghệ 4.0, đồng thời xây dựng nội lực vững để không bị lệ thuộc vào bất kỳ thị trường đơn lẻ nào."),
                PodcastLine(PodcastSpeaker.HOST_MINH, "Dạ em thông suốt cả 6 chương rồi! Vào phòng thi gặp câu phân tích KTTT định hướng XHCN em sẽ nhấn mạnh kinh tế nhà nước chủ đạo, kinh tế tư nhân là động lực quan trọng và gắn công bằng xã hội ngay trong từng chính sách.")
            ),
            checkpoint = InlineMcq(
                question = "Câu nào diễn đạt ĐÚNG mối quan hệ giữa độc lập tự chủ và hội nhập KTQT của Việt Nam?",
                options = listOf(
                    "Đóng cửa hạn chế thương mại để tự chủ",
                    "Vừa chủ động hội nhập sâu rộng vừa củng cố tiềm lực độc lập tự chủ",
                    "Chỉ tập trung nông nghiệp tự cung tự cấp",
                    "Phụ thuộc hoàn toàn vào vốn nước ngoài"
                ),
                correctIndex = 1,
                explanation = "Hai mặt gắn bó hữu cơ: hội nhập nâng cao nội lực, nội lực vững giúp hội nhập bình đẳng hiệu quả.",
                conceptTag = "Hội nhập KTQT & Độc lập tự chủ",
                sourceCitation = "📄 Podcast Chương 3 · Giáo trình Chương 6"
            )
        )
    )

    private fun buildWrittenExam(): List<WrittenExamQuestion> = listOf(
        WrittenExamQuestion(
            id = "exam_q1",
            partLabel = "Câu 1 · Lý thuyết phân tích",
            points = 3.5,
            prompt = "Phân tích tính hai mặt của lao động sản xuất hàng hóa (lao động cụ thể và lao động trừu tượng). Vì sao nói C. Mác phát hiện ra tính hai mặt này là 'điểm mấu chốt' để hiểu kinh tế chính trị?",
            standardSolution = "1. Lao động cụ thể: lao động có ích dưới hình thức nghề nghiệp chuyên môn riêng, tạo ra Giá trị sử dụng (phạm trù vĩnh viễn).\n2. Lao động trừu tượng: sự hao phí sức lực chung (thần kinh, cơ bắp) không kể hình thức cụ thể, tạo ra Giá trị hàng hóa (phạm trù lịch sử).\n3. Tính hai mặt giải thích mâu thuẫn cơ bản của sản xuất hàng hóa giữa lao động tư nhân và lao động xã hội, là cơ sở xây dựng học thuyết giá trị và giá trị thặng dư.",
            keyRubricPoints = listOf(
                "Nêu đúng khái niệm và kết quả của Lao động cụ thể (tạo GTSD)",
                "Nêu đúng khái niệm và kết quả của Lao động trừu tượng (tạo GT)",
                "Chỉ ra mối liên hệ với mâu thuẫn lao động tư nhân – lao động xã hội",
                "Không nhầm với lao động giản đơn / phức tạp"
            ),
            sourceCitation = "📄 Chương 2, Mục 2.1.3"
        ),
        WrittenExamQuestion(
            id = "exam_q2",
            partLabel = "Câu 2 · Bài toán Giá trị thặng dư & p'",
            points = 3.5,
            prompt = "Một doanh nghiệp ứng ra tổng tư bản 1.500 tỷ đồng với cấu tạo hữu cơ c/v = 4/1. Tỷ suất giá trị thặng dư m' = 120%. Hãy tính: (a) Tư bản bất biến c, tư bản khả biến v và giá trị thặng dư m; (b) Tổng giá trị hàng hóa G và tỷ suất lợi nhuận p'; (c) Nếu doanh nghiệp tăng c/v lên 5/1 với cùng tổng tư bản 1.500 tỷ và m' giữ nguyên 120%, p' thay đổi thế nào?",
            standardSolution = "(a) c + v = 1.500 và c = 4v ⇒ v = 300 tỷ, c = 1.200 tỷ; m = 120% × 300 = 360 tỷ.\n(b) G = 1.200 + 300 + 360 = 1.860 tỷ; p' = (360 / 1.500) × 100% = 24%.\n(c) Khi c/v = 5/1 ⇒ 6v = 1.500 ⇒ v mới = 250 tỷ, c mới = 1.250 tỷ; m mới = 120% × 250 = 300 tỷ ⇒ p' mới = (300 / 1.500) × 100% = 20% (giảm 4 điểm % do c/v tăng).",
            keyRubricPoints = listOf(
                "Tính đúng c = 1.200 tỷ, v = 300 tỷ, m = 360 tỷ",
                "Tính đúng G = 1.860 tỷ và p' = 24%",
                "Tính đúng trường hợp c/v = 5/1: v = 250 tỷ, m = 300 tỷ, p' = 20%",
                "Kết luận đúng quy luật c/v tăng làm p' giảm khi m' không đổi"
            ),
            sourceCitation = "📄 Chương 3, Mục 3.1.3 & 3.3"
        ),
        WrittenExamQuestion(
            id = "exam_q3",
            partLabel = "Câu 3 · Liên hệ thực tiễn Việt Nam",
            points = 3.0,
            prompt = "Phân tích các đặc trưng cơ bản của nền kinh tế thị trường định hướng XHCN ở Việt Nam và vai trò của sinh viên trong thích ứng cuộc Cách mạng công nghiệp 4.0 gắn với hội nhập kinh tế quốc tế.",
            standardSolution = "1. Đặc trưng KTTT ĐHXHCN Việt Nam: Mục tiêu dân giàu, nước mạnh, dân chủ, công bằng, văn minh; đa sở hữu – đa thành phần (KT nhà nước chủ đạo, KT tư nhân là động lực quan trọng); Nhà nước pháp quyền XHCN quản lý; phân phối chủ yếu theo kết quả lao động & hiệu quả kinh tế kết hợp an sinh xã hội; gắn tăng trưởng với công bằng xã hội ngay trong từng bước.\n2. Vai trò sinh viên trong CMCN 4.0 & hội nhập: trau dồi năng lực số, ngoại ngữ, tư duy đổi mới sáng tạo, đạo đức nghề nghiệp và ý thức xây dựng kinh tế độc lập tự chủ.",
            keyRubricPoints = listOf(
                "Trình bày đủ mục tiêu và cấu trúc thành phần kinh tế (Nhà nước chủ đạo, Tư nhân động lực)",
                "Nêu đúng cơ chế phân phối và gắn tăng trưởng với công bằng xã hội",
                "Liên hệ cụ thể trách nhiệm sinh viên trước CMCN 4.0 và hội nhập KTQT"
            ),
            sourceCitation = "📄 Chương 5 & Chương 6"
        )
    )

    private fun buildStoryboardFrames(): List<StoryboardFrame> = listOf(
        StoryboardFrame("Khung 1", "Chu trình Tư bản ứng trước", "Nhà tư bản ứng tiền (T) mua hai yếu tố: Tư liệu sản xuất (c) và Sức lao động (v).", "K = c + v", "Phân tách đúng c và v quyết định toàn bộ bài toán."),
        StoryboardFrame("Khung 2", "Quá trình tạo Giá trị mới", "Trong thời gian lao động tất yếu (t), công nhân tạo ra giá trị ngang bằng tiền lương (v). Trong thời gian thặng dư (t'), họ tạo ra m.", "m' = (t' / t) × 100%", "Sức lao động là hàng hóa duy nhất sinh thêm giá trị mới."),
        StoryboardFrame("Khung 3", "Biến hóa thành Lợi nhuận p'", "Khi bán hàng hóa trên thị trường, m biểu hiện dưới hình thái lợi nhuận p so sánh với toàn bộ chi phí (c + v).", "p' = m / (c + v) × 100%", "Vì có thêm c ở mẫu số, p' luôn nhỏ hơn m'.")
    )
}
