package com.example.data.seed

import com.example.model.CausalityRule
import com.example.model.ComparisonItem
import com.example.model.FormulaBlock
import com.example.model.FormulaTerm
import com.example.model.InlineMcq
import com.example.model.LearningMode
import com.example.model.LearningNode
import com.example.model.QuizDifficulty
import com.example.model.QuizQuestion
import com.example.model.SmartTextStep
import com.example.model.StrikeThroughTf

object KtctNodesSeed {
    private const val SRC_CH12 = "📄 Giáo trình KTCT Mác - Lênin (Bộ GD&ĐT), Chương 1 & 2"
    private const val SRC_CH3 = "📄 Giáo trình KTCT Mác - Lênin (Bộ GD&ĐT), Chương 3"
    private const val SRC_CH4 = "📄 Giáo trình KTCT Mác - Lênin (Bộ GD&ĐT), Chương 4"
    private const val SRC_CH5 = "📄 Giáo trình KTCT Mác - Lênin (Bộ GD&ĐT), Chương 5"
    private const val SRC_CH6 = "📄 Giáo trình KTCT Mác - Lênin (Bộ GD&ĐT), Chương 6"

    fun buildNodes(): List<LearningNode> = listOf(
        buildNode1(),
        buildNode2(),
        buildNode3(),
        buildNode4(),
        buildNode5(),
        buildNode6(),
        buildNode7()
    )

    private fun buildNode1(): LearningNode = LearningNode(
        id = "ktct_node_1",
        orderIndex = 1,
        chapterLabel = "Chương 1 & 2",
        title = "Hàng hóa, Hai thuộc tính & Tính hai mặt của Lao động",
        subtitle = "Đối tượng nghiên cứu, điều kiện SXHH, GTSD/GT & quy tắc NSLĐ/CĐLĐ",
        primaryMode = LearningMode.SMART_TEXT,
        sourceCitation = SRC_CH12,
        masteryPercent = 25,
        isCompleted = false,
        isUnlocked = true,
        smartSteps = listOf(
            SmartTextStep(
                stepNumber = 1,
                title = "Bản chất Hàng hóa & Lao động sản xuất hàng hóa",
                takeaway = "Hàng hóa kết tinh hai thuộc tính đối lập nhưng thống nhất, bắt nguồn từ tính hai mặt của lao động sản xuất hàng hóa.",
                subtopics = listOf(
                    "Đối tượng nghiên cứu KTCT & 2 điều kiện ra đời sản xuất hàng hóa",
                    "So sánh 3 phạm trù: Giá trị sử dụng · Giá trị · Giá trị trao đổi",
                    "Tính hai mặt: Lao động cụ thể vs. Lao động trừu tượng (và bẫy thi kinh điển)",
                    "Công thức lượng giá trị & Quy tắc biến thiên NSLĐ vs. CĐLĐ"
                ),
                sourceCitation = SRC_CH12
            ),
            SmartTextStep(
                stepNumber = 2,
                title = "Đối tượng KTCT & Điều kiện ra đời sản xuất hàng hóa",
                readingBody = "Kinh tế chính trị Mác - Lênin nghiên cứu các quan hệ xã hội của sản xuất và trao đổi đặt trong mối liên hệ biện chứng với lực lượng sản xuất và kiến trúc thượng tầng. Sản xuất hàng hóa chỉ ra đời khi đồng thời hội đủ HAI điều kiện: (1) Phân công lao động xã hội (tạo nhu cầu trao đổi) và (2) Sự tách biệt tương đối về mặt kinh tế của các chủ thể sản xuất (tạo quyền độc lập trao đổi ngang giá).",
                sourceCitation = "$SRC_CH12, Mục 2.1",
                trapTitle = "❗ Bẫy tư duy thường gặp",
                trapBody = "Nhiều sinh viên nhầm rằng chỉ cần có 'Phân công lao động xã hội' là tự động có sản xuất hàng hóa. Sai! Trong công xã nguyên thủy đã có phân công lao động nhưng sở hữu chung nên không trao đổi mua bán.",
                inlineMcq = InlineMcq(
                    question = "Điều kiện nào làm cho những người sản xuất trở thành chủ thể độc lập, muốn tiêu dùng sản phẩm của nhau phải thông qua trao đổi mua bán?",
                    options = listOf(
                        "Phân công lao động xã hội",
                        "Sự tách biệt tương đối về mặt kinh tế giữa các chủ thể sản xuất",
                        "Sự xuất hiện của tiền giấy và ngân hàng thương mại",
                        "Trình độ cơ khí hóa đạt mức công nghiệp lớn"
                    ),
                    correctIndex = 1,
                    explanation = "Phân công lao động xã hội là điều kiện cần (tạo sự phụ thuộc), còn sự tách biệt tương đối về kinh tế (do các hình thức sở hữu khác nhau) là điều kiện đủ buộc các chủ thể phải trao đổi ngang giá.",
                    conceptTag = "Điều kiện ra đời sản xuất hàng hóa",
                    sourceCitation = "$SRC_CH12, Mục 2.1"
                )
            ),
            SmartTextStep(
                stepNumber = 3,
                title = "Ba phạm trù cốt lõi của Hàng hóa",
                readingBody = "Mọi hàng hóa (vật thể hoặc phi vật thể như dịch vụ) đều có hai thuộc tính là Giá trị sử dụng và Giá trị. Trong lưu thông, Giá trị không tự bộc lộ trực tiếp mà biểu hiện ra bên ngoài qua Giá trị trao đổi. Vuốt ngang 3 thẻ dưới đây để nắm chắc ranh giới phân biệt:",
                sourceCitation = "$SRC_CH12, Mục 2.1.2",
                comparisonCards = listOf(
                    ComparisonItem(
                        title = "Giá trị sử dụng (GTSD)",
                        tag = "PHẠM TRÙ VĨNH VIỄN",
                        points = listOf(
                            "Công dụng của vật phẩm thỏa mãn nhu cầu con người",
                            "Do thuộc tính tự nhiên của hàng hóa quyết định",
                            "GTSD chỉ thực hiện trong tiêu dùng (cho người mua)"
                        ),
                        accentHex = "#22C55E"
                    ),
                    ComparisonItem(
                        title = "Giá trị (GT)",
                        tag = "PHẠM TRÙ LỊCH SỬ",
                        points = listOf(
                            "Lao động xã hội của người sản xuất kết tinh trong hàng hóa",
                            "Phản ánh quan hệ kinh tế giữa những người sản xuất",
                            "Là nội dung, cơ sở bên trong của giá trị trao đổi"
                        ),
                        accentHex = "#3B82F6"
                    ),
                    ComparisonItem(
                        title = "Giá trị trao đổi",
                        tag = "HÌNH THỨC BIỂU HIỆN",
                        points = listOf(
                            "Tỷ lệ về lượng mà GTSD này trao đổi với GTSD khác",
                            "Ví dụ: 1m vải = 10kg thóc vì hao phí LĐXH bằng nhau",
                            "Tiền tệ là hình thái phát triển cao nhất của GTTĐ"
                        ),
                        accentHex = "#8B5CF6"
                    )
                ),
                strikeThroughTf = StrikeThroughTf(
                    id = "tf_n1_s3",
                    prefix = "Giá trị sử dụng của hàng hóa là một phạm trù ",
                    wrongClause = "lịch sử, chỉ tồn tại trong nền kinh tế thị trường",
                    correctClause = "vĩnh viễn, tồn tại trong mọi hình thái kinh tế - xã hội",
                    isTrue = false,
                    explanation = "Giá trị mới là phạm trù lịch sử (chỉ có khi có sản xuất hàng hóa). Giá trị sử dụng do thuộc tính tự nhiên quy định nên tồn tại vĩnh viễn cùng xã hội loài người.",
                    conceptTag = "Hai thuộc tính của hàng hóa",
                    sourceCitation = "$SRC_CH12, Mục 2.1.2"
                )
            ),
            SmartTextStep(
                stepNumber = 4,
                title = "Tính hai mặt của Lao động sản xuất hàng hóa",
                readingBody = "Phát hiện vĩ đại của C. Mác: Không phải có hai thứ lao động khác nhau, mà chính MỘT lao động của người sản xuất hàng hóa có tính hai mặt: Lao động cụ thể (hình thức nghề nghiệp cụ thể tạo ra Giá trị sử dụng) và Lao động trừu tượng (sự hao phí sức lực chung — thần kinh, cơ bắp — tạo ra Giá trị hàng hóa).",
                sourceCitation = "$SRC_CH12, Mục 2.1.3",
                trapTitle = "❗ Bẫy tư duy kinh điển trong đề thi",
                trapBody = "Đừng nhầm 'Lao động phức tạp / Lao động giản đơn' với 'Lao động cụ thể / Lao động trừu tượng'! Cụ thể–Trừu tượng giải thích TÍNH HAI MẶT tạo ra GTSD và GT; còn Phức tạp–Giản đơn là nhân tố ảnh hưởng đến LƯỢNG giá trị (1 giờ LĐ phức tạp = bội số LĐ giản đơn).",
                inlineMcq = InlineMcq(
                    question = "Mặt nào của lao động sản xuất hàng hóa trực tiếp tạo ra Giá trị hàng hóa và phản ánh tính chất xã hội của lao động?",
                    options = listOf(
                        "Lao động cụ thể",
                        "Lao động trừu tượng",
                        "Lao động quản lý kỹ thuật",
                        "Lao động cá biệt của doanh nghiệp"
                    ),
                    correctIndex = 1,
                    explanation = "Lao động trừu tượng gạt bỏ mọi hình thức nghề nghiệp cụ thể để quy về hao phí sức lao động nói chung — đây là nguồn gốc duy nhất tạo nên Giá trị hàng hóa.",
                    conceptTag = "Tính hai mặt của lao động",
                    sourceCitation = "$SRC_CH12, Mục 2.1.3"
                )
            ),
            SmartTextStep(
                stepNumber = 5,
                title = "Lượng giá trị & Quy tắc biến thiên NSLĐ vs. CĐLĐ",
                readingBody = "Lượng giá trị của một đơn vị hàng hóa được đo bằng Thời gian lao động xã hội cần thiết (TGLĐXHCT), bao gồm hao phí lao động quá khứ (c) và lao động sống mới kết tinh (v + m). Hai nhân tố sinh viên cực kỳ dễ nhầm là Năng suất lao động (NSLĐ) và Cường độ lao động (CĐLĐ).",
                sourceCitation = "$SRC_CH12, Mục 2.1.4",
                formula = FormulaBlock(
                    title = "Cấu thành lượng giá trị 1 đơn vị hàng hóa",
                    terms = listOf(
                        FormulaTerm(text = "G = c + v + m"),
                        FormulaTerm(text = "   |   "),
                        FormulaTerm(text = "g", subscript = "1sp"),
                        FormulaTerm(text = " = "),
                        FormulaTerm(numerator = "Tổng lượng giá trị", denominator = "Số lượng sản phẩm")
                    ),
                    legend = listOf(
                        "c: Giá trị tư liệu sản xuất chuyển vào (lao động quá khứ)",
                        "v + m: Giá trị mới do lao động sống tạo thêm"
                    )
                ),
                causalityRule = CausalityRule(
                    title = "💡 Quy tắc biến thiên nhanh (Bắt buộc thuộc)",
                    arrows = listOf(
                        "NSLĐ tăng → Số SP tăng → Giá trị 1 ĐV hàng hóa GIẢM → Tổng giá trị KHÔNG ĐỔI",
                        "CĐLĐ tăng → Hao phí sức lực tăng cùng tỷ lệ SP → Giá trị 1 ĐV KHÔNG ĐỔI → Tổng giá trị TĂNG"
                    ),
                    note = "Tăng CĐLĐ về bản chất giống như kéo dài ngày lao động trong cùng một khoảng thời gian."
                ),
                strikeThroughTf = StrikeThroughTf(
                    id = "tf_n1_s5",
                    prefix = "Khi tăng cường độ lao động lên 2 lần (các yếu tố khác không đổi), lượng giá trị trong một đơn vị sản phẩm sẽ ",
                    wrongClause = "giảm đi một nửa",
                    correctClause = "không đổi, còn tổng lượng giá trị tạo ra tăng gấp đôi",
                    isTrue = false,
                    explanation = "Tăng CĐLĐ làm tăng đồng thời cả hao phí lao động lẫn số lượng sản phẩm theo cùng tỷ lệ, nên lượng giá trị trên 1 sản phẩm giữ nguyên!",
                    conceptTag = "NSLĐ vs Cường độ lao động",
                    sourceCitation = "$SRC_CH12, Mục 2.1.4"
                )
            ),
            SmartTextStep(
                stepNumber = 6,
                title = "Tiền tệ & Quy luật Giá trị trong nền kinh tế thị trường",
                readingBody = "Tiền tệ ra đời là kết quả phát triển lâu dài của các hình thái giá trị, đóng vai trò vật ngang giá chung với 5 chức năng (Thước đo giá trị, Phương tiện lưu thông, Cất trữ, Thanh toán, Tiền tệ thế giới). Quy luật giá trị là quy luật kinh tế cơ bản nhất: sản xuất và trao đổi phải dựa trên cơ sở hao phí lao động xã hội cần thiết.",
                sourceCitation = "$SRC_CH12, Mục 2.2 & 2.3",
                causalityRule = CausalityRule(
                    title = "💡 3 Tác động của Quy luật giá trị",
                    arrows = listOf(
                        "Giá cả > Giá trị → Mở rộng sản xuất (điều tiết SX & lưu thông)",
                        "Hao phí cá biệt < Hao phí XH → Thu lợi nhuận cao (kích thích cải tiến kỹ thuật)",
                        "Cạnh tranh → Phân hóa giàu nghèo giữa những người sản xuất"
                    ),
                    note = "Trên thị trường, giá cả lên xuống xoay quanh trục giá trị dưới tác động của cung – cầu."
                ),
                strikeThroughTf = StrikeThroughTf(
                    id = "tf_n1_s6",
                    prefix = "Quy luật giá trị yêu cầu sản xuất và trao đổi hàng hóa phải căn cứ vào ",
                    wrongClause = "hao phí lao động xã hội cần thiết",
                    correctClause = "hao phí lao động xã hội cần thiết",
                    isTrue = true,
                    explanation = "Chính xác! Mọi chủ thể sản xuất phải cố gắng hạ hao phí lao động cá biệt thấp hơn hoặc bằng hao phí lao động xã hội cần thiết để có lãi.",
                    conceptTag = "Quy luật giá trị",
                    sourceCitation = "$SRC_CH12, Mục 2.3"
                )
            )
        ),
        finalQuiz = listOf(
            QuizQuestion(
                id = "n1_q1",
                question = "Hai điều kiện cần và đủ để sản xuất hàng hóa ra đời là gì?",
                options = listOf(
                    "Phân công lao động xã hội và sự tách biệt tương đối về kinh tế giữa các chủ thể",
                    "Thương mại quốc tế và hệ thống tiền tệ thống nhất",
                    "Chế độ tư hữu tuyệt đối và sản xuất đại công nghiệp cơ khí",
                    "Phân công lao động cá biệt và sở hữu toàn dân"
                ),
                correctIndices = listOf(0),
                isMultiSelect = false,
                difficulty = QuizDifficulty.BASIC,
                explanation = "Chỉ khi đồng thời có phân công lao động xã hội và sự tách biệt tương đối về kinh tế thì sản xuất hàng hóa mới hình thành.",
                conceptTag = "Điều kiện ra đời sản xuất hàng hóa",
                sourceCitation = "$SRC_CH12, Mục 2.1"
            ),
            QuizQuestion(
                id = "n1_q2",
                question = "Trong cùng một thời gian lao động, khi Năng suất lao động xã hội tăng lên 2 lần thì điều gì xảy ra?",
                options = listOf(
                    "Giá trị 1 đơn vị sản phẩm không đổi, tổng giá trị tăng 2 lần",
                    "Giá trị 1 đơn vị sản phẩm giảm 1/2, tổng giá trị không đổi",
                    "Cả giá trị 1 đơn vị và tổng giá trị đều giảm 1/2",
                    "Giá trị 1 đơn vị sản phẩm tăng gấp đôi"
                ),
                correctIndices = listOf(1),
                isMultiSelect = false,
                difficulty = QuizDifficulty.MEDIUM,
                explanation = "NSLĐ tỷ lệ nghịch với lượng giá trị trong 1 đơn vị hàng hóa và không làm thay đổi tổng lượng giá trị tạo ra trong cùng đơn vị thời gian.",
                conceptTag = "NSLĐ vs Cường độ lao động",
                sourceCitation = "$SRC_CH12, Mục 2.1.4"
            ),
            QuizQuestion(
                id = "n1_q3",
                question = "Lao động cụ thể tạo ra thuộc tính nào của hàng hóa?",
                options = listOf(
                    "Giá trị trao đổi",
                    "Giá trị hàng hóa",
                    "Giá trị sử dụng",
                    "Giá trị thặng dư"
                ),
                correctIndices = listOf(2),
                isMultiSelect = false,
                difficulty = QuizDifficulty.BASIC,
                explanation = "Lao động cụ thể là lao động có ích dưới hình thức nghề nghiệp chuyên môn nhất định, trực tiếp tạo ra Giá trị sử dụng.",
                conceptTag = "Tính hai mặt của lao động",
                sourceCitation = "$SRC_CH12, Mục 2.1.3"
            ),
            QuizQuestion(
                id = "n1_q4",
                question = "Thực hiện chức năng nào của tiền tệ KHÔNG nhất thiết phải có tiền mặt (vàng) đầy đủ giá trị tại chỗ?",
                options = listOf(
                    "Phương tiện cất trữ",
                    "Tiền tệ thế giới",
                    "Thước đo giá trị (chỉ cần tiền tưởng tượng)",
                    "Không chức năng nào"
                ),
                correctIndices = listOf(2),
                isMultiSelect = false,
                difficulty = QuizDifficulty.MEDIUM,
                explanation = "Khi làm thước đo giá trị (niêm yết giá cả), chỉ cần tiền tưởng tượng trong ý niệm, không cần cầm sẵn tiền mặt bên cạnh hàng hóa.",
                conceptTag = "Chức năng của tiền tệ",
                sourceCitation = "$SRC_CH12, Mục 2.2"
            ),
            QuizQuestion(
                id = "n1_q5",
                question = "Đâu là quy luật kinh tế cơ bản nhất của sản xuất và trao đổi hàng hóa?",
                options = listOf(
                    "Quy luật cạnh tranh",
                    "Quy luật cung - cầu",
                    "Quy luật giá trị",
                    "Quy luật lưu thông tiền tệ"
                ),
                correctIndices = listOf(2),
                isMultiSelect = false,
                difficulty = QuizDifficulty.BASIC,
                explanation = "Quy luật giá trị chi phối toàn bộ nền sản xuất hàng hóa ở bất cứ đâu có sản xuất và trao đổi hàng hóa.",
                conceptTag = "Quy luật giá trị",
                sourceCitation = "$SRC_CH12, Mục 2.3"
            )
        )
    )

    private fun buildNode2(): LearningNode = LearningNode(
        id = "ktct_node_2",
        orderIndex = 2,
        chapterLabel = "Chương 3 · Phần 1",
        title = "Hàng hóa Sức lao động & Sản xuất Giá trị thặng dư (m)",
        subtitle = "Công thức T–H–T', phân biệt c & v, tính m' và M",
        primaryMode = LearningMode.SMART_TEXT,
        sourceCitation = SRC_CH3,
        masteryPercent = 0,
        isCompleted = false,
        isUnlocked = true,
        smartSteps = listOf(
            SmartTextStep(
                stepNumber = 1,
                title = "Chìa khóa giải mã nguồn gốc Giá trị thặng dư",
                takeaway = "Giá trị thặng dư (m) không sinh ra từ lưu thông thuần túy mà từ việc tiêu dùng loại hàng hóa đặc biệt: Hàng hóa sức lao động.",
                subtopics = listOf(
                    "Công thức chung của tư bản (T – H – T') và mâu thuẫn của nó",
                    "2 điều kiện để sức lao động trở thành hàng hóa",
                    "Phân biệt Tư bản bất biến (c) và Tư bản khả biến (v)",
                    "Công thức tỷ suất m' và khối lượng giá trị thặng dư M"
                ),
                sourceCitation = "$SRC_CH3, Mục 3.1"
            ),
            SmartTextStep(
                stepNumber = 2,
                title = "Công thức chung của Tư bản (T – H – T')",
                readingBody = "Trong lưu thông hàng hóa giản đơn (H – T – H), mục đích là Giá trị sử dụng. Ngược lại, trong lưu thông tư bản (T – H – T' với T' = T + Δt), mục đích tối thượng là Giá trị lớn hơn — tức Giá trị thặng dư (m). Mâu thuẫn: Giá trị thặng dư không thể sinh ra ngoài lưu thông, nhưng cũng không thể sinh ra thuần túy trong lưu thông ngang giá!",
                sourceCitation = "$SRC_CH3, Mục 3.1.1",
                formula = FormulaBlock(
                    title = "Công thức chung của Tư bản",
                    terms = listOf(
                        FormulaTerm(text = "T → H → T'   với   T' = T + m  (m > 0)")
                    ),
                    legend = listOf(
                        "T: Tư bản ứng trước ban đầu",
                        "H: Hàng hóa gồm Tư liệu sản xuất (TLSX) và Sức lao động (SLĐ)",
                        "m (Δt): Giá trị thặng dư thu được sau chu kỳ sản xuất"
                    )
                ),
                inlineMcq = InlineMcq(
                    question = "Chìa khóa mà C. Mác tìm ra để giải quyết mâu thuẫn công thức chung của tư bản T – H – T' là gì?",
                    options = listOf(
                        "Nhà tư bản mua rẻ bán đắt trên thị trường quốc tế",
                        "Nhà tư bản mua được loại hàng hóa đặc biệt là Sức lao động",
                        "Nhà nước in thêm tiền giấy vào lưu thông",
                        "Máy móc tự động sinh ra giá trị mới vượt quá khấu hao"
                    ),
                    correctIndex = 1,
                    explanation = "Hàng hóa sức lao động có giá trị sử dụng đặc biệt: khi bị tiêu dùng trong quá trình lao động, nó tạo ra lượng giá trị mới (v + m) lớn hơn giá trị bản thân nó (v).",
                    conceptTag = "Công thức chung của tư bản",
                    sourceCitation = "$SRC_CH3, Mục 3.1.1"
                )
            ),
            SmartTextStep(
                stepNumber = 3,
                title = "Hai điều kiện để Sức lao động trở thành hàng hóa",
                readingBody = "Sức lao động là toàn bộ thể lực và trí lực tồn tại trong cơ thể con người đang sống. Để sức lao động biến thành hàng hóa, lịch sử cần đồng thời 2 điều kiện: (1) Người lao động được tự do về thân thể (có quyền bán sức lao động của mình) và (2) Người lao động không có đủ tư liệu sản xuất cần thiết để tự kết hợp với sức lao động tạo ra hàng hóa bán.",
                sourceCitation = "$SRC_CH3, Mục 3.1.1",
                trapTitle = "❗ Bẫy phân biệt: Sức lao động ≠ Lao động",
                trapBody = "Người công nhân bán SỨC LAO ĐỘNG (khả năng lao động), không phải bán LAO ĐỘNG! Lao động là quá trình tiêu dùng sức lao động diễn ra trong nhà máy sau khi đã ký hợp đồng.",
                strikeThroughTf = StrikeThroughTf(
                    id = "tf_n2_s3",
                    prefix = "Giá trị sử dụng của hàng hóa sức lao động có đặc điểm giống mọi hàng hóa thông thường là ",
                    wrongClause = "sau khi tiêu dùng thì mất dần cả giá trị lẫn giá trị sử dụng",
                    correctClause = "khi được tiêu dùng sẽ tạo ra một lượng giá trị mới lớn hơn giá trị bản thân nó",
                    isTrue = false,
                    explanation = "Đây chính là tính đặc biệt quyết định của hàng hóa sức lao động — nguồn gốc trực tiếp tạo ra giá trị thặng dư (m).",
                    conceptTag = "Hàng hóa sức lao động",
                    sourceCitation = "$SRC_CH3, Mục 3.1.1"
                )
            ),
            SmartTextStep(
                stepNumber = 4,
                title = "Phân biệt Tư bản bất biến (c) và Tư bản khả biến (v)",
                readingBody = "Căn cứ vào vai trò của từng bộ phận tư bản trong quá trình sản xuất ra giá trị thặng dư, C. Mác chia tư bản thành Tư bản bất biến (c) và Tư bản khả biến (v). Đây là nền tảng để tính đúng mọi bài tập chương 3!",
                sourceCitation = "$SRC_CH3, Mục 3.1.2",
                comparisonCards = listOf(
                    ComparisonItem(
                        title = "Tư bản bất biến (c)",
                        tag = "KHÔNG TĂNG LƯỢNG GIÁ TRỊ",
                        points = listOf(
                            "Tồn tại dưới hình thái Tư liệu sản xuất (máy móc, nhà xưởng, nguyên liệu)",
                            "Giá trị được bảo tồn và chuyển nguyên vẹn vào sản phẩm mới",
                            "Là ĐIỀU KIỆN cần thiết để sản xuất giá trị thặng dư"
                        ),
                        accentHex = "#F59E0B"
                    ),
                    ComparisonItem(
                        title = "Tư bản khả biến (v)",
                        tag = "NGUỒN GỐC TRỰC TIẾP CỦA m",
                        points = listOf(
                            "Bộ phận tư bản dùng để mua hàng hóa Sức lao động (tiền lương)",
                            "Trong sản xuất có sự biến đổi tăng lên về lượng: v → v + m",
                            "Là nguồn gốc DUY NHẤT sinh ra giá trị thặng dư"
                        ),
                        accentHex = "#22C55E"
                    ),
                    ComparisonItem(
                        title = "Giá trị hàng hóa (G)",
                        tag = "TỔNG HỢP c + v + m",
                        points = listOf(
                            "c: Giá trị cũ chuyển sang",
                            "v + m: Giá trị mới do lao động sống tạo ra",
                            "Chi phí sản xuất tư bản chủ nghĩa: k = c + v"
                        ),
                        accentHex = "#3B82F6"
                    )
                ),
                inlineMcq = InlineMcq(
                    question = "Căn cứ để C. Mác phân chia tư bản thành Tư bản bất biến (c) và Tư bản khả biến (v) là gì?",
                    options = listOf(
                        "Phương thức chu chuyển giá trị nhanh hay chậm vào sản phẩm",
                        "Vai trò khác nhau của các bộ phận tư bản trong quá trình sản xuất giá trị thặng dư",
                        "Hình thái tồn tại hữu hình hay vô hình của tài sản",
                        "Nguồn gốc sở hữu trong nước hay nước ngoài"
                    ),
                    correctIndex = 1,
                    explanation = "Cần nhớ rõ: Căn cứ chia c/v là vai trò tạo ra m; còn căn cứ chia Tư bản cố định/lưu động là phương thức chu chuyển giá trị!",
                    conceptTag = "Tư bản bất biến và khả biến",
                    sourceCitation = "$SRC_CH3, Mục 3.1.2"
                )
            ),
            SmartTextStep(
                stepNumber = 5,
                title = "Tỷ suất (m') và Khối lượng giá trị thặng dư (M)",
                readingBody = "Tỷ suất giá trị thặng dư (m') phản ánh TRÌNH ĐỘ bóc lột của nhà tư bản đối với lao động làm thuê. Khối lượng giá trị thặng dư (M) phản ánh QUY MÔ giá trị thặng dư mà nhà tư bản thu được trong một chu kỳ.",
                sourceCitation = "$SRC_CH3, Mục 3.1.3",
                formula = FormulaBlock(
                    title = "Bộ công thức vàng Chương 3",
                    terms = listOf(
                        FormulaTerm(text = "m' = "),
                        FormulaTerm(numerator = "m", denominator = "v"),
                        FormulaTerm(text = " × 100% = "),
                        FormulaTerm(numerator = "t'", denominator = "t"),
                        FormulaTerm(text = " × 100%   |   M = m' × V")
                    ),
                    legend = listOf(
                        "t: Thời gian lao động tất yếu (tạo ra v)",
                        "t': Thời gian lao động thặng dư (tạo ra m)",
                        "V: Tổng tư bản khả biến được sử dụng trong kỳ"
                    )
                ),
                causalityRule = CausalityRule(
                    title = "💡 Mẹo tính nhanh trên phòng thi",
                    arrows = listOf(
                        "Nếu ngày làm việc 8 giờ và m' = 100% → t = 4 giờ, t' = 4 giờ",
                        "Nếu m' = 150% và v = 200 triệu → m = 150% × 200 = 300 triệu",
                        "Tỷ suất lợi nhuận p' = m / (c + v) × 100% → Luôn nhỏ hơn m'!"
                    ),
                    note = "Tỷ suất giá trị thặng dư m' chỉ chia cho v, KHÔNG bao giờ chia cho (c + v)."
                ),
                strikeThroughTf = StrikeThroughTf(
                    id = "tf_n2_s5",
                    prefix = "Tỷ suất giá trị thặng dư (m') được tính bằng tỷ lệ phần trăm giữa giá trị thặng dư (m) và ",
                    wrongClause = "tổng tư bản ứng trước (c + v)",
                    correctClause = "tư bản khả biến (v)",
                    isTrue = false,
                    explanation = "m / (c + v) × 100% là công thức Tỷ suất lợi nhuận (p'). Còn Tỷ suất giá trị thặng dư m' = (m / v) × 100%.",
                    conceptTag = "Công thức m' và p'",
                    sourceCitation = "$SRC_CH3, Mục 3.1.3"
                )
            ),
            SmartTextStep(
                stepNumber = 6,
                title = "Bản chất Tiền công trong CNTB",
                readingBody = "Bề ngoài, tiền công có vẻ như là 'giá cả của lao động'. Nhưng thực chất, tiền công là GIÁ CẢ CỦA HÀNG HÓA SỨC LAO ĐỘNG. Do đó, người công nhân chỉ được trả công cho phần thời gian lao động tất yếu (t), còn phần giá trị tạo ra trong thời gian lao động thặng dư (t') thuộc về nhà tư bản.",
                sourceCitation = "$SRC_CH3, Mục 3.1.2",
                strikeThroughTf = StrikeThroughTf(
                    id = "tf_n2_s6",
                    prefix = "Về bản chất kinh tế, tiền công trong nền sản xuất tư bản chủ nghĩa là ",
                    wrongClause = "giá cả của hàng hóa sức lao động",
                    correctClause = "giá cả của hàng hóa sức lao động",
                    isTrue = true,
                    explanation = "Chính xác! Tiền công biểu hiện ra bề ngoài như giá cả của lao động, làm che giấu ranh giới giữa thời gian lao động tất yếu (t) và thặng dư (t').",
                    conceptTag = "Bản chất tiền công",
                    sourceCitation = "$SRC_CH3, Mục 3.1.2"
                )
            )
        ),
        finalQuiz = listOf(
            QuizQuestion(
                id = "n2_q1",
                question = "Bộ phận tư bản nào trực tiếp tạo ra giá trị thặng dư (m)?",
                options = listOf("Tư bản bất biến (c)", "Tư bản khả biến (v)", "Tư bản cố định", "Chi phí lưu thông"),
                correctIndices = listOf(1),
                isMultiSelect = false,
                difficulty = QuizDifficulty.BASIC,
                explanation = "Tư bản khả biến (v) dùng mua sức lao động, có khả năng tăng lượng giá trị trong sản xuất từ v thành v + m.",
                conceptTag = "Tư bản bất biến và khả biến",
                sourceCitation = "$SRC_CH3, Mục 3.1.2"
            ),
            QuizQuestion(
                id = "n2_q2",
                question = "Một nhà máy ứng ra v = 400 triệu đồng và thu được m = 600 triệu đồng. Tỷ suất giá trị thặng dư m' bằng bao nhiêu?",
                options = listOf("66,7%", "100%", "150%", "200%"),
                correctIndices = listOf(2),
                isMultiSelect = false,
                difficulty = QuizDifficulty.MEDIUM,
                explanation = "m' = (m / v) × 100% = (600 / 400) × 100% = 150%.",
                conceptTag = "Công thức m' và p'",
                sourceCitation = "$SRC_CH3, Mục 3.1.3"
            ),
            QuizQuestion(
                id = "n2_q3",
                question = "Ngày lao động dài 10 giờ, trong đó thời gian lao động tất yếu t = 4 giờ. Tỷ suất giá trị thặng dư m' là:",
                options = listOf("100%", "125%", "150%", "250%"),
                correctIndices = listOf(2),
                isMultiSelect = false,
                difficulty = QuizDifficulty.MEDIUM,
                explanation = "t' = 10 - 4 = 6 giờ → m' = (t' / t) × 100% = (6 / 4) × 100% = 150%.",
                conceptTag = "Công thức m' và p'",
                sourceCitation = "$SRC_CH3, Mục 3.1.3"
            ),
            QuizQuestion(
                id = "n2_q4",
                question = "Giá trị mới do lao động sống của công nhân tạo ra trong quá trình sản xuất là:",
                options = listOf("c + v", "v + m", "c + m", "c + v + m"),
                correctIndices = listOf(1),
                isMultiSelect = false,
                difficulty = QuizDifficulty.BASIC,
                explanation = "c là giá trị cũ từ TLSX chuyển sang; còn (v + m) là phần giá trị mới tạo thêm.",
                conceptTag = "Tư bản bất biến và khả biến",
                sourceCitation = "$SRC_CH3, Mục 3.1.2"
            ),
            QuizQuestion(
                id = "n2_q5",
                question = "Ý nghĩa của công thức M = m' × V là đo lường:",
                options = listOf(
                    "Trình độ bóc lột sức lao động",
                    "Quy mô (khối lượng) giá trị thặng dư thu được",
                    "Tốc độ chu chuyển tư bản cố định",
                    "Cấu tạo hữu cơ của tư bản"
                ),
                correctIndices = listOf(1),
                isMultiSelect = false,
                difficulty = QuizDifficulty.BASIC,
                explanation = "m' phản ánh trình độ (chất lượng), còn M = m' × V phản ánh quy mô (khối lượng) giá trị thặng dư.",
                conceptTag = "Công thức m' và p'",
                sourceCitation = "$SRC_CH3, Mục 3.1.3"
            )
        )
    )

    private fun buildNode3(): LearningNode = LearningNode(
        id = "ktct_node_3",
        orderIndex = 3,
        chapterLabel = "Chương 3 · Phần 2",
        title = "Lab Mô phỏng c, v, m, m', p' & 3 Phương pháp SX GTTD",
        subtitle = "Thanh trượt động Tuyệt đối · Tương đối · Siêu ngạch + 4 bài toán mẫu",
        primaryMode = LearningMode.VIDEO_SIMULATOR,
        sourceCitation = "$SRC_CH3, Mục 3.1.3 & 3.3",
        masteryPercent = 0,
        isCompleted = false,
        isUnlocked = true
    )

    private fun buildNode4(): LearningNode = LearningNode(
        id = "ktct_node_4",
        orderIndex = 4,
        chapterLabel = "Chương 3 & 4",
        title = "Tích lũy, Cấu tạo hữu cơ c/v, Chu chuyển & Độc quyền",
        subtitle = "Trắc nghiệm thích ứng 3 mức độ (Đơn & Đa lựa chọn bẫy thi)",
        primaryMode = LearningMode.ADAPTIVE_QUIZ,
        sourceCitation = "$SRC_CH4, Mục 3.2 & 4.1",
        masteryPercent = 0,
        isCompleted = false,
        isUnlocked = true
    )

    private fun buildNode5(): LearningNode = LearningNode(
        id = "ktct_node_5",
        orderIndex = 5,
        chapterLabel = "Chương 5",
        title = "Kinh tế thị trường định hướng XHCN & Lợi ích kinh tế ở VN",
        subtitle = "Tính tất yếu khách quan, 5 đặc trưng cốt lõi & hài hòa lợi ích",
        primaryMode = LearningMode.SMART_TEXT,
        sourceCitation = SRC_CH5,
        masteryPercent = 0,
        isCompleted = false,
        isUnlocked = true,
        smartSteps = listOf(
            SmartTextStep(
                stepNumber = 1,
                title = "KTTT định hướng XHCN ở Việt Nam",
                takeaway = "Vận hành đầy đủ theo các quy luật của kinh tế thị trường, đồng thời hướng tới mục tiêu 'dân giàu, nước mạnh, dân chủ, công bằng, văn minh'.",
                subtopics = listOf(
                    "3 lý do khách quan tất yếu phải phát triển KTTT định hướng XHCN",
                    "Đặc trưng về sở hữu, thành phần kinh tế và vai trò quản lý Nhà nước",
                    "Quan hệ phân phối & giải quyết hài hòa các quan hệ lợi ích kinh tế"
                ),
                sourceCitation = SRC_CH5
            ),
            SmartTextStep(
                stepNumber = 2,
                title = "Sở hữu & Các thành phần kinh tế ở Việt Nam",
                readingBody = "Nền KTTT định hướng XHCN ở Việt Nam có nhiều hình thức sở hữu và nhiều thành phần kinh tế. Trong đó, kinh tế nhà nước giữ vai trò CHỦ ĐẠO; kinh tế tập thể, hợp tác xã không ngừng được củng cố; kinh tế tư nhân là MỘT ĐỘNG LỰC QUAN TRỌNG của nền kinh tế.",
                sourceCitation = "$SRC_CH5, Mục 5.1.3",
                trapTitle = "❗ Bẫy thi cực hay gặp",
                trapBody = "Đề thi thường hoán đổi hai cụm từ: 'Kinh tế nhà nước giữ vai trò chủ đạo' và 'Kinh tế tư nhân là một động lực quan trọng'. Tuyệt đối không chọn ngược!",
                inlineMcq = InlineMcq(
                    question = "Trong nền kinh tế thị trường định hướng XHCN ở Việt Nam, thành phần kinh tế nào được xác định là 'một động lực quan trọng'?",
                    options = listOf(
                        "Kinh tế nhà nước",
                        "Kinh tế tư nhân",
                        "Kinh tế hộ tự cung tự cấp",
                        "Kinh tế bao cấp tập trung"
                    ),
                    correctIndex = 1,
                    explanation = "Kinh tế nhà nước giữ vai trò chủ đạo; kinh tế tư nhân là một động lực quan trọng; kinh tế có vốn đầu tư nước ngoài được khuyến khích phát triển.",
                    conceptTag = "Đặc trưng KTTT định hướng XHCN",
                    sourceCitation = "$SRC_CH5, Mục 5.1.3"
                )
            ),
            SmartTextStep(
                stepNumber = 3,
                title = "Phân phối & Quan hệ lợi ích kinh tế",
                readingBody = "Việt Nam thực hiện nhiều hình thức phân phối, trong đó phân phối theo KẾT QUẢ LAO ĐỘNG, hiệu quả kinh tế và mức đóng góp vốn cùng các nguồn lực khác, kết hợp phân phối thông qua hệ thống an sinh xã hội và phúc lợi xã hội. Lợi ích kinh tế là động lực trực tiếp của các chủ thể, vừa thống nhất vừa mâu thuẫn nên cần vai trò điều tiết của Nhà nước pháp quyền XHCN.",
                sourceCitation = "$SRC_CH5, Mục 5.3",
                strikeThroughTf = StrikeThroughTf(
                    id = "tf_n5_s3",
                    prefix = "Trong KTTT định hướng XHCN ở Việt Nam, thực hiện tiến bộ và công bằng xã hội chỉ được tiến hành ",
                    wrongClause = "sau khi nền kinh tế đã đạt trình độ phát triển cao",
                    correctClause = "ngay trong từng bước đi và từng chính sách phát triển",
                    isTrue = false,
                    explanation = "Đặc trưng nổi bật của Việt Nam là gắn tăng trưởng kinh tế với công bằng xã hội ngay trong từng bước và từng chính sách, không 'chờ giàu rồi mới chia'.",
                    conceptTag = "Công bằng xã hội trong KTTT ĐHXHCN",
                    sourceCitation = "$SRC_CH5, Mục 5.1.3"
                )
            )
        ),
        finalQuiz = listOf(
            QuizQuestion(
                id = "n5_q1",
                question = "Trong KTTT định hướng XHCN ở Việt Nam, thành phần kinh tế giữ vai trò chủ đạo là:",
                options = listOf("Kinh tế tư nhân", "Kinh tế nhà nước", "Kinh tế FDI", "Kinh tế cá thể"),
                correctIndices = listOf(1),
                isMultiSelect = false,
                difficulty = QuizDifficulty.BASIC,
                explanation = "Kinh tế nhà nước giữ vai trò chủ đạo, nắm giữ các lĩnh vực then chốt và là công cụ điều tiết vĩ mô.",
                conceptTag = "Đặc trưng KTTT định hướng XHCN",
                sourceCitation = "$SRC_CH5, Mục 5.1.3"
            )
        )
    )

    private fun buildNode6(): LearningNode = LearningNode(
        id = "ktct_node_6",
        orderIndex = 6,
        chapterLabel = "Chương 6",
        title = "Công nghiệp hóa, Hiện đại hóa & Hội nhập KTQT của VN",
        subtitle = "Cách mạng công nghiệp 4.0, kinh tế tri thức & hội nhập chủ động",
        primaryMode = LearningMode.SMART_TEXT,
        sourceCitation = SRC_CH6,
        masteryPercent = 0,
        isCompleted = false,
        isUnlocked = true,
        smartSteps = listOf(
            SmartTextStep(
                stepNumber = 1,
                title = "CNH, HĐH gắn với CMCN 4.0 & Hội nhập KTQT",
                takeaway = "CNH, HĐH là quy luật phổ biến giúp Việt Nam chuyển từ lao động thủ công sang công nghệ hiện đại, kết hợp chủ động hội nhập quốc tế sâu rộng.",
                subtopics = listOf(
                    "4 cuộc Cách mạng công nghiệp & đặc trưng nổi bật của CMCN 4.0",
                    "Mô hình CNH cổ điển, Liên Xô và Nhật Bản/NICs",
                    "Tác động hai mặt của hội nhập kinh tế quốc tế & xây dựng nền kinh tế độc lập tự chủ"
                ),
                sourceCitation = SRC_CH6
            ),
            SmartTextStep(
                stepNumber = 2,
                title = "Cách mạng công nghiệp 4.0 & Nội dung CNH, HĐH ở VN",
                readingBody = "CMCN 4.0 hình thành trên nền tảng liên kết thế giới thực và ảo (AI, IoT, Big Data, Cloud). Ở Việt Nam, CNH phải gắn liền với HĐH và phát triển kinh tế tri thức; chuyển dịch cơ cấu kinh tế theo hướng tăng tỷ trọng công nghiệp và dịch vụ, giảm dần tỷ trọng nông nghiệp theo hướng hiện đại.",
                sourceCitation = "$SRC_CH6, Mục 6.1",
                comparisonCards = listOf(
                    ComparisonItem(
                        title = "Mô hình Cổ điển (Anh)",
                        tag = "60 – 80 NĂM",
                        points = listOf("Bắt đầu từ công nghiệp nhẹ (dệt)", "Vốn từ khai thác thuộc địa & nông dân", "Thời gian kéo dài"),
                        accentHex = "#F59E0B"
                    ),
                    ComparisonItem(
                        title = "Mô hình Nhật Bản & NICs",
                        tag = "20 – 30 NĂM",
                        points = listOf("Đẩy mạnh xuất khẩu, thay thế nhập khẩu", "Tiếp thu công nghệ mới + đào tạo nhân lực", "Rút ngắn mạnh thời gian CNH"),
                        accentHex = "#22C55E"
                    ),
                    ComparisonItem(
                        title = "Định hướng Việt Nam",
                        tag = "CNH GẮN VỚI HĐH",
                        points = listOf("Gắn với kinh tế tri thức & chuyển đổi số", "Bảo vệ môi trường, phát triển bền vững", "Chủ động thích ứng CMCN 4.0"),
                        accentHex = "#3B82F6"
                    )
                ),
                inlineMcq = InlineMcq(
                    question = "Mô hình công nghiệp hóa của Nhật Bản và các nước công nghiệp mới (NICs) thường gọi là mô hình gì?",
                    options = listOf(
                        "Công nghiệp hóa cổ điển kéo dài 80 năm",
                        "Công nghiệp hóa rút ngắn, đẩy mạnh xuất khẩu và tiếp thu công nghệ mới",
                        "Công nghiệp hóa ưu tiên tuyệt đối công nghiệp nặng khép kín",
                        "Phi công nghiệp hóa nông nghiệp thuần túy"
                    ),
                    correctIndex = 1,
                    explanation = "Nhật Bản và các nước NICs (Hàn Quốc, Singapore...) thực hiện chiến lược CNH rút ngắn bằng xuất khẩu, chuyển giao công nghệ và đầu tư giáo dục.",
                    conceptTag = "Mô hình công nghiệp hóa",
                    sourceCitation = "$SRC_CH6, Mục 6.1"
                )
            ),
            SmartTextStep(
                stepNumber = 3,
                title = "Hội nhập kinh tế quốc tế & Kinh tế độc lập, tự chủ",
                readingBody = "Hội nhập kinh tế quốc tế là xu thế khách quan giúp Việt Nam mở rộng thị trường, tiếp thu vốn và công nghệ, nhưng cũng đặt ra nguy cơ cạnh tranh gay gắt, phụ thuộc thị trường và thách thức an ninh kinh tế. Vì vậy, hội nhập phải đi đôi với xây dựng nền kinh tế ĐỘC LẬP, TỰ CHỦ.",
                sourceCitation = "$SRC_CH6, Mục 6.2",
                strikeThroughTf = StrikeThroughTf(
                    id = "tf_n6_s3",
                    prefix = "Xây dựng nền kinh tế độc lập, tự chủ có nghĩa là Việt Nam phải ",
                    wrongClause = "đóng cửa thị trường, hạn chế tối đa tham gia các hiệp định FTA",
                    correctClause = "không bị lệ thuộc vào nước khác trong khi vẫn chủ động, tích cực hội nhập sâu rộng",
                    isTrue = false,
                    explanation = "Độc lập, tự chủ KHÔNG đồng nghĩa với biệt lập hay đóng cửa; đó là năng lực tự chủ về đường lối, tiềm lực và sức chống chịu trong hội nhập.",
                    conceptTag = "Hội nhập KTQT & Độc lập tự chủ",
                    sourceCitation = "$SRC_CH6, Mục 6.2"
                )
            )
        ),
        finalQuiz = listOf(
            QuizQuestion(
                id = "n6_q1",
                question = "Đặc trưng cốt lõi của cuộc Cách mạng công nghiệp lần thứ tư (4.0) là gì?",
                options = listOf(
                    "Sử dụng năng lượng hơi nước và cơ khí hóa",
                    "Điện khí hóa và dây chuyền sản xuất hàng loạt",
                    "Liên kết thế giới thực và ảo qua AI, IoT, Big Data",
                    "Phát minh động cơ đốt trong"
                ),
                correctIndices = listOf(2),
                isMultiSelect = false,
                difficulty = QuizDifficulty.BASIC,
                explanation = "CMCN 4.0 hội tụ công nghệ số, trí tuệ nhân tạo, vạn vật kết nối và dữ liệu lớn.",
                conceptTag = "Cách mạng công nghiệp 4.0",
                sourceCitation = "$SRC_CH6, Mục 6.1"
            )
        )
    )

    private fun buildNode7(): LearningNode = LearningNode(
        id = "ktct_node_7",
        orderIndex = 7,
        chapterLabel = "Tổng hợp 6 Chương",
        title = "Đề thi thử viết 80 phút (Tự luận + Bài toán GTTD)",
        subtitle = "3 câu chuẩn đại học · Gõ trực tiếp hoặc chụp ảnh bài viết tay chấm AI",
        primaryMode = LearningMode.WRITTEN_EXAM,
        sourceCitation = "📄 Bộ đề chuẩn Học phần KTCT Mác - Lênin (Bộ GD&ĐT)",
        masteryPercent = 0,
        isCompleted = false,
        isUnlocked = true
    )
}
