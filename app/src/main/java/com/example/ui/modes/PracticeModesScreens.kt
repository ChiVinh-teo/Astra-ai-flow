package com.example.ui.modes

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.service.GeminiDomainUseCases
import com.example.service.HapticHelper
import com.example.ui.components.ConfettiBurstOverlay
import com.example.ui.components.StrikeThroughTrueFalseCard
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalAstraColors
import kotlinx.coroutines.launch

// =========================================================================
// MODE 6: BÀI TẬP TÍNH TOÁN & TÌNH HUỐNG (Numerical + AI Step Verification)
// =========================================================================
@Composable
fun ExercisesModeScreen(
    course: Course,
    settings: AiSettings,
    geminiUseCases: GeminiDomainUseCases,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val colors = LocalAstraColors.current
    val scope = rememberCoroutineScope()
    val problems = course.exercises
    var idx by remember { mutableIntStateOf(0) }
    var userWork by remember(idx) { mutableStateOf("") }
    var showHint by remember(idx) { mutableStateOf(false) }
    var showSolution by remember(idx) { mutableStateOf(false) }
    var aiFeedback by remember(idx) { mutableStateOf<String?>(null) }
    var checking by remember(idx) { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        ModeTopHeader(
            title = "✏️ Bài tập tính toán & tình huống",
            subtitle = if (problems.isNotEmpty()) "Bài ${idx + 1} / ${problems.size} · Kiểm tra từng bước AI" else "Bài tập thực hành",
            onBack = onBack
        )

        if (problems.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text("Khóa học này chưa có bài tập tính toán số liệu — hãy dùng tab Gia sư AI để luyện tình huống!", color = colors.textSecondary)
            }
        } else {
            val prob = problems[idx]
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Problem Tabs
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    problems.forEachIndexed { i, _ ->
                        Surface(
                            modifier = Modifier.weight(1f).clickable { idx = i },
                            shape = RoundedCornerShape(10.dp),
                            color = if (i == idx) colors.electricBlue else colors.card,
                            border = BorderStroke(1.dp, if (i == idx) colors.electricBlue else colors.border)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 8.dp)) {
                                Text("Bài ${i + 1}", style = MaterialTheme.typography.labelMedium, color = if (i == idx) Color.White else colors.textSecondary)
                            }
                        }
                    }
                }

                Surface(shape = RoundedCornerShape(18.dp), color = colors.card, border = BorderStroke(1.dp, colors.border)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(prob.title, style = MaterialTheme.typography.titleMedium, color = colors.electricBlue)
                        Text(prob.prompt, style = MaterialTheme.typography.bodyLarge, color = colors.textPrimary)
                        Text(prob.sourceCitation, style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                    }
                }

                if (showHint) {
                    Surface(shape = RoundedCornerShape(14.dp), color = colors.amberTint, border = BorderStroke(1.dp, colors.amber.copy(alpha = 0.5f))) {
                        Text(
                            text = "💡 Gợi ý công thức: ${prob.formulaHint}",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 12.5.sp,
                            color = colors.amber,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = userWork,
                    onValueChange = { userWork = it },
                    label = { Text("Trình bày các bước tính của bạn (ví dụ: v = 200, c = 800, m = 300...)") },
                    minLines = 4,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { showHint = !showHint }, modifier = Modifier.weight(1f)) {
                        Text(if (showHint) "Ẩn gợi ý" else "💡 Gợi ý")
                    }
                    Button(
                        onClick = {
                            checking = true
                            scope.launch {
                                if (settings.apiKey.isNotBlank() && userWork.isNotBlank()) {
                                    val res = geminiUseCases.verifyExerciseStep(settings, prob, userWork)
                                    aiFeedback = res.getOrElse { "Đáp án chuẩn: ${prob.expectedFinalAnswer}" }
                                } else {
                                    aiFeedback = "Đáp án chuẩn cần đạt: ${prob.expectedFinalAnswer}\nHãy đối chiếu từng bước bên dưới (hoặc dán API Key trong Cài đặt AI để chấm tự động bài giải của bạn)."
                                }
                                showSolution = true
                                checking = false
                            }
                        },
                        enabled = !checking,
                        modifier = Modifier.weight(1.4f)
                    ) {
                        if (checking) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                        else Text("🤖 Kiểm tra từng bước")
                    }
                }

                if (aiFeedback != null) {
                    Surface(shape = RoundedCornerShape(16.dp), color = colors.blueTint, border = BorderStroke(1.dp, colors.electricBlue)) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("📋 Nhận xét bước giải", style = MaterialTheme.typography.titleSmall, color = colors.electricBlue)
                            Text(aiFeedback.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary)
                        }
                    }
                }

                if (showSolution) {
                    Surface(shape = RoundedCornerShape(16.dp), color = colors.emeraldTint, border = BorderStroke(1.dp, colors.emerald)) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("✅ Lời giải chuẩn từng bước", style = MaterialTheme.typography.titleSmall, color = colors.emerald)
                            prob.solutionSteps.forEach { step ->
                                Text(step, style = MaterialTheme.typography.bodySmall, color = colors.textPrimary)
                            }
                        }
                    }
                } else {
                    TextButton(onClick = { showSolution = true }, modifier = Modifier.align(Alignment.End)) {
                        Text("Xem lời giải chuẩn →")
                    }
                }
            }
        }
    }
}

// =========================================================================
// MODE 7: TRẮC NGHIỆM THÍCH ỨNG (3 Mức độ · Single & Multi-Select)
// =========================================================================
@Composable
fun AdaptiveQuizScreen(
    course: Course,
    onLogMistake: (String, String, String, String) -> Unit,
    onCompleteQuiz: (Int, Int) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val colors = LocalAstraColors.current
    val context = LocalContext.current

    var selectedDifficulty by remember { mutableStateOf<QuizDifficulty?>(null) }
    val questions = remember(course.id, selectedDifficulty) {
        val diff = selectedDifficulty ?: QuizDifficulty.MEDIUM
        val exact = course.quizBank.filter { it.difficulty == diff }
        val extra = course.quizBank.filter { it.difficulty != diff }
        (exact + extra).take(7)
    }

    var cursor by remember(selectedDifficulty) { mutableIntStateOf(0) }
    var chosenSet by remember(cursor, selectedDifficulty) { mutableStateOf(setOf<Int>()) }
    var submitted by remember(cursor, selectedDifficulty) { mutableStateOf(false) }
    var correctCount by remember(selectedDifficulty) { mutableIntStateOf(0) }
    var finished by remember(selectedDifficulty) { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(colors.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            ModeTopHeader(
                title = "🔠 Trắc nghiệm thích ứng",
                subtitle = selectedDifficulty?.let { "Mức độ: ${it.label} · 7 câu hỏi" } ?: "Chọn mức độ thử thách",
                onBack = onBack
            )

            if (selectedDifficulty == null) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Chọn mức độ đề trắc nghiệm:", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                    listOf(
                        Triple(QuizDifficulty.BASIC, "Khởi động chắc nền tảng — câu hỏi khái niệm & quy luật cốt lõi", colors.emerald),
                        Triple(QuizDifficulty.MEDIUM, "Chuẩn đề thi đại học — trộn chọn đơn & chọn nhiều bẫy c/v, cố định/lưu động", colors.electricBlue),
                        Triple(QuizDifficulty.ADVANCED, "Chinh phục điểm 9–10 — cơ chế nhân quả & câu đa lựa chọn chuyên sâu", colors.violet)
                    ).forEach { (diff, desc, accent) ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    HapticHelper.tap(context)
                                    selectedDifficulty = diff
                                }
                                .testTag("quiz_diff_${diff.name.lowercase()}"),
                            shape = RoundedCornerShape(18.dp),
                            color = colors.card,
                            border = BorderStroke(1.2.dp, accent.copy(alpha = 0.65f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(diff.label, style = MaterialTheme.typography.titleMedium, color = accent)
                                Text(desc, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                            }
                        }
                    }
                }
            } else if (finished) {
                val pct = if (questions.isNotEmpty()) correctCount * 100 / questions.size else 100
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("🏆 Hoàn thành phiên Trắc nghiệm!", style = MaterialTheme.typography.headlineSmall, color = colors.emerald)
                    Spacer(Modifier.height(10.dp))
                    Text("$correctCount / ${questions.size} câu đúng ($pct%)", fontFamily = JetBrainsMonoFontFamily, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = colors.electricBlue)
                    Spacer(Modifier.height(20.dp))
                    Button(onClick = onBack, modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp)) {
                        Text("Quay về")
                    }
                }
            } else {
                val q = questions[cursor]
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Câu ${cursor + 1} / ${questions.size}", style = MaterialTheme.typography.labelLarge, color = colors.electricBlue)
                        if (q.isMultiSelect) {
                            Surface(shape = RoundedCornerShape(50), color = colors.violetTint, border = BorderStroke(1.dp, colors.violet)) {
                                Text(
                                    text = "✓✓ CÓ THỂ CHỌN NHIỀU ĐÁP ÁN",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.violet,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Text(q.question, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)

                    q.options.forEachIndexed { idx, opt ->
                        val isPicked = idx in chosenSet
                        val isCorrectOpt = idx in q.correctIndices
                        val bg = when {
                            !submitted && isPicked -> colors.blueTint
                            !submitted -> colors.card
                            isCorrectOpt -> colors.emeraldTint
                            isPicked -> colors.crimsonTint
                            else -> colors.card
                        }
                        val border = when {
                            !submitted && isPicked -> colors.electricBlue
                            !submitted -> colors.border
                            isCorrectOpt -> colors.emerald
                            isPicked -> colors.crimson
                            else -> colors.border
                        }
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 50.dp)
                                .clickable(enabled = !submitted) {
                                    HapticHelper.tap(context)
                                    chosenSet = if (q.isMultiSelect) {
                                        if (idx in chosenSet) chosenSet - idx else chosenSet + idx
                                    } else {
                                        setOf(idx)
                                    }
                                },
                            shape = RoundedCornerShape(14.dp),
                            color = bg,
                            border = BorderStroke(1.2.dp, border)
                        ) {
                            Row(
                                modifier = Modifier.padding(13.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = when {
                                        q.isMultiSelect && isPicked -> Icons.Rounded.CheckBox
                                        q.isMultiSelect -> Icons.Rounded.CheckBoxOutlineBlank
                                        isPicked -> Icons.Rounded.RadioButtonChecked
                                        else -> Icons.Rounded.RadioButtonUnchecked
                                    },
                                    contentDescription = null,
                                    tint = if (isPicked) colors.electricBlue else colors.textSecondary
                                )
                                Text(opt, style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary, modifier = Modifier.weight(1f))
                            }
                        }
                    }

                    if (submitted) {
                        val allRight = chosenSet == q.correctIndices.toSet()
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (allRight) colors.emeraldTint else colors.amberTint
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(if (allRight) "✅ Chuẩn xác!" else "⚠️ Kiểm tra lại mệnh đề:", style = MaterialTheme.typography.titleSmall, color = if (allRight) colors.emerald else colors.amber)
                                Text(q.explanation, style = MaterialTheme.typography.bodySmall, color = colors.textPrimary)
                                Text(q.sourceCitation, style = MaterialTheme.typography.labelSmall, color = colors.electricBlue)
                            }
                        }
                    }

                    Spacer(Modifier.height(6.dp))
                    Button(
                        onClick = {
                            if (!submitted) {
                                submitted = true
                                if (chosenSet == q.correctIndices.toSet()) {
                                    correctCount++
                                    HapticHelper.tap(context)
                                } else {
                                    HapticHelper.error(context)
                                    onLogMistake("ktct_node_4", q.conceptTag, q.question, q.explanation)
                                }
                            } else if (cursor < questions.lastIndex) {
                                cursor++
                            } else {
                                val pct = correctCount * 100 / questions.size.coerceAtLeast(1)
                                HapticHelper.celebrate(context)
                                onCompleteQuiz(pct, 40 + pct / 2)
                                finished = true
                            }
                        },
                        enabled = chosenSet.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth().navigationBarsPadding().heightIn(min = 52.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = when {
                                !submitted -> "Kiểm tra đáp án"
                                cursor < questions.lastIndex -> "Câu tiếp theo →"
                                else -> "Xem kết quả 🎉"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        if (finished) ConfettiBurstOverlay(trigger = true)
    }
}

// =========================================================================
// MODE 8: ĐÚNG / SAI PHẢN XẠ NHANH (10-Card Swipeable Strike-Through Blitz)
// =========================================================================
@Composable
fun TrueFalseBlitzScreen(
    course: Course,
    onLogMistake: (String, String, String, String) -> Unit,
    onComplete: (Int, Int) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val colors = LocalAstraColors.current
    val context = LocalContext.current
    val deck = course.trueFalseDeck
    var cursor by remember { mutableIntStateOf(0) }
    var answeredChoice by remember(cursor) { mutableStateOf<Boolean?>(null) }
    var score by remember { mutableIntStateOf(0) }
    var combo by remember { mutableIntStateOf(0) }
    var done by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(colors.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            ModeTopHeader(
                title = "☑️ Đúng / Sai phản xạ nhanh",
                subtitle = if (deck.isNotEmpty()) "Thẻ ${cursor + 1}/${deck.size} · Chuỗi đúng: $combo 🔥" else "Bộ thẻ Đúng/Sai",
                onBack = onBack
            )

            if (done || deck.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("⚡ Hoàn thành 10 thẻ phản xạ!", style = MaterialTheme.typography.headlineSmall, color = colors.emerald)
                    Spacer(Modifier.height(8.dp))
                    Text("$score / ${deck.size} thẻ chính xác", fontFamily = JetBrainsMonoFontFamily, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = colors.electricBlue)
                    Spacer(Modifier.height(20.dp))
                    Button(onClick = onBack, modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp)) { Text("Quay về") }
                }
            } else {
                val item = deck[cursor]
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    LinearProgressIndicator(
                        progress = { (cursor + 1).toFloat() / deck.size },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)),
                        color = colors.violet,
                        trackColor = colors.cardElevated
                    )

                    StrikeThroughTrueFalseCard(
                        item = item,
                        answeredChoice = answeredChoice,
                        onAnswer = { chosen ->
                            answeredChoice = chosen
                            if (chosen == item.isTrue) {
                                score++
                                combo++
                                HapticHelper.tap(context)
                            } else {
                                combo = 0
                                HapticHelper.error(context)
                                onLogMistake("blitz", item.conceptTag, "${item.prefix}${item.wrongClause}", item.explanation)
                            }
                        }
                    )

                    if (answeredChoice != null) {
                        Button(
                            onClick = {
                                if (cursor < deck.lastIndex) {
                                    cursor++
                                } else {
                                    val pct = score * 100 / deck.size
                                    HapticHelper.celebrate(context)
                                    onComplete(pct, 35 + score * 4)
                                    done = true
                                }
                            },
                            modifier = Modifier.fillMaxWidth().navigationBarsPadding().heightIn(min = 52.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(if (cursor < deck.lastIndex) "Thẻ tiếp theo →" else "Tổng kết phiên phản xạ 🎉", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        if (done) ConfettiBurstOverlay(trigger = true)
    }
}

// =========================================================================
// MODE 9: ÔN TẬP NGẮT QUÃNG (Spaced Repetition Review)
// =========================================================================
@Composable
fun SpacedRepetitionScreen(
    course: Course,
    remediations: List<RemediationRecord>,
    onRateCard: (String, SrsRating) -> Unit,
    onResolveGap: (Long) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val colors = LocalAstraColors.current
    val weakNodes = course.nodes.sortedBy { it.masteryPercent }.take(3)
    val dueCards = course.flashcards.sortedBy { it.masteryLevel }.take(5)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        ModeTopHeader(
            title = "🔄 Ôn tập lặp lại ngắt quãng (SRS)",
            subtitle = "Ưu tiên trạm giảm trí nhớ & khái niệm từng sai",
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("📉 Trạm cần củng cố ưu tiên", style = MaterialTheme.typography.titleSmall, color = colors.amber)
            weakNodes.forEach { n ->
                Surface(shape = RoundedCornerShape(14.dp), color = colors.card, border = BorderStroke(1.dp, colors.border)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(13.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${n.chapterLabel}: ${n.title}", style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                            Text("Độ thành thạo hiện tại: ${n.masteryPercent}%", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                        }
                        Text("${n.masteryPercent}%", fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, color = colors.amber)
                    }
                }
            }

            if (remediations.any { !it.resolved }) {
                Text("📌 Lỗi cũ cần ôn lại ngay", style = MaterialTheme.typography.titleSmall, color = colors.crimson)
                remediations.filter { !it.resolved }.forEach { gap ->
                    Surface(shape = RoundedCornerShape(14.dp), color = colors.crimsonTint, border = BorderStroke(1.dp, colors.crimson.copy(alpha = 0.5f))) {
                        Column(modifier = Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(gap.conceptTag, style = MaterialTheme.typography.titleSmall, color = colors.crimson)
                            Text(gap.mistakeSummary, style = MaterialTheme.typography.bodySmall, color = colors.textPrimary)
                            OutlinedButton(onClick = { onResolveGap(gap.id) }, shape = RoundedCornerShape(10.dp)) {
                                Text("✓ Đã ghi nhớ vững")
                            }
                        }
                    }
                }
            }

            Text("🗂️ Thẻ đến hạn ôn tập (${dueCards.size} thẻ)", style = MaterialTheme.typography.titleSmall, color = colors.electricBlue)
            dueCards.forEach { card ->
                var revealed by remember(card.id) { mutableStateOf(false) }
                Surface(shape = RoundedCornerShape(14.dp), color = colors.card, border = BorderStroke(1.dp, colors.border)) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(card.front, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                        if (revealed) {
                            Text(card.back, style = MaterialTheme.typography.bodyMedium, color = colors.emerald)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { onRateCard(card.id, SrsRating.HARD); revealed = false }, modifier = Modifier.weight(1f)) { Text("Còn khó") }
                                Button(onClick = { onRateCard(card.id, SrsRating.EASY); revealed = false }, modifier = Modifier.weight(1f)) { Text("Nhớ chắc ✓") }
                            }
                        } else {
                            TextButton(onClick = { revealed = true }) { Text("Hiện đáp án kiểm tra trí nhớ →") }
                        }
                    }
                }
            }
        }
    }
}
