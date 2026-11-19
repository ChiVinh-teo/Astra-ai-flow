package com.example.ui.modes

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
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
import com.example.ui.components.*
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalAstraColors
import kotlinx.coroutines.launch

private data class MissedCheckpoint(
    val conceptTag: String,
    val originalQuestion: String,
    val explanation: String
)

@Composable
fun SmartTextSessionScreen(
    node: LearningNode,
    settings: AiSettings,
    geminiUseCases: GeminiDomainUseCases,
    onLogMistake: (nodeId: String, conceptTag: String, question: String, summary: String) -> Unit,
    onCompleteNode: (nodeId: String, accuracyPercent: Int, starsEarned: Int) -> Unit,
    onShowToast: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val colors = LocalAstraColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val steps = remember(node.id) {
        if (node.smartSteps.isNotEmpty()) node.smartSteps else listOf(
            SmartTextStep(
                stepNumber = 1,
                title = node.title,
                takeaway = node.subtitle,
                subtopics = listOf("Khái niệm cốt lõi", "Phân tích bản chất", "Kiểm tra tổng hợp"),
                sourceCitation = node.sourceCitation
            )
        )
    }

    // Stages: 0..steps.lastIndex = Micro-steps; steps.size = Step 7 Remediation (if missed); steps.size+1 = Step 8 Quiz; steps.size+2 = Celebration
    var stageIndex by remember(node.id) { mutableIntStateOf(0) }
    var mcqSelected by remember(stageIndex) { mutableStateOf<Int?>(null) }
    var tfSelected by remember(stageIndex) { mutableStateOf<Boolean?>(null) }
    var mascotMood by remember(stageIndex) { mutableStateOf(MascotMood.IDLE) }

    val missedList = remember(node.id) { mutableStateListOf<MissedCheckpoint>() }
    var totalCheckpoints by remember(node.id) { mutableIntStateOf(0) }
    var correctCheckpoints by remember(node.id) { mutableIntStateOf(0) }

    // Step 7 Remediation state
    var remediationMcq by remember { mutableStateOf<InlineMcq?>(null) }
    var remediationLoading by remember { mutableStateOf(false) }
    var remediationSelected by remember { mutableStateOf<Int?>(null) }

    // Step 8 Final Quiz state
    val finalQuiz = remember(node.id) {
        node.finalQuiz.ifEmpty {
            steps.mapNotNull { it.inlineMcq }.mapIndexed { i, m ->
                QuizQuestion("fq_$i", m.question, m.options, listOf(m.correctIndex), false, QuizDifficulty.MEDIUM, m.explanation, m.conceptTag, m.sourceCitation)
            }
        }
    }
    var quizCursor by remember { mutableIntStateOf(0) }
    var quizAnswerIdx by remember(quizCursor) { mutableStateOf<Int?>(null) }
    var quizCorrectCount by remember { mutableIntStateOf(0) }

    // Celebration state
    var difficultyCalib by remember { mutableFloatStateOf(0.5f) }

    val remediationStage = steps.size
    val quizStage = steps.size + 1
    val celebrationStage = steps.size + 2
    val totalVisualSegments = steps.size + 2

    LaunchedEffect(stageIndex) {
        if (stageIndex == remediationStage) {
            val firstMissed = missedList.firstOrNull()
            if (firstMissed == null) {
                stageIndex = if (finalQuiz.isNotEmpty()) quizStage else celebrationStage
            } else {
                remediationLoading = true
                remediationSelected = null
                if (settings.apiKey.isNotBlank()) {
                    val res = geminiUseCases.generateRephrasedRemediation(
                        settings = settings,
                        conceptTag = firstMissed.conceptTag,
                        originalQuestion = firstMissed.originalQuestion,
                        mistakeContext = firstMissed.explanation
                    )
                    remediationMcq = res.getOrElse { buildLocalRephrasedRemediation(firstMissed) }
                } else {
                    remediationMcq = buildLocalRephrasedRemediation(firstMissed)
                }
                remediationLoading = false
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar + Liquid-Filling Segmented Progress Bar
            Surface(color = colors.card, border = BorderStroke(1.dp, colors.border)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(onClick = onBack, modifier = Modifier.testTag("smart_text_back")) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Quay lại", tint = colors.textPrimary)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${node.chapterLabel} · Văn bản thông minh",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.electricBlue
                            )
                            Text(
                                text = node.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = colors.textPrimary,
                                maxLines = 1
                            )
                        }
                        AstraBuddyMascot(mood = mascotMood, size = 36.dp)
                    }

                    // Liquid-filling segmented top bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (seg in 0 until totalVisualSegments) {
                            val targetFill = when {
                                seg < stageIndex -> 1f
                                seg == stageIndex -> 0.55f
                                else -> 0f
                            }
                            val fill by animateFloatAsState(targetFill, tween(380), label = "seg_$seg")
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(colors.cardElevated)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(fill)
                                        .clip(RoundedCornerShape(50))
                                        .background(if (seg == remediationStage) colors.amber else colors.electricBlue)
                                )
                            }
                        }
                    }
                }
            }

            // Animated Body
            AnimatedContent(
                targetState = stageIndex,
                transitionSpec = {
                    (slideInHorizontally { it / 3 } + fadeIn(tween(240))) togetherWith
                        (slideOutHorizontally { -it / 3 } + fadeOut(tween(180)))
                },
                modifier = Modifier.weight(1f),
                label = "smart_step_transition"
            ) { currentStage ->
                when {
                    currentStage < steps.size -> {
                        val step = steps[currentStage]
                        val hasCheckpoint = step.inlineMcq != null || step.strikeThroughTf != null
                        val checkpointDone = !hasCheckpoint || mcqSelected != null || tfSelected != null

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            if (currentStage == 0) {
                                // Step 1: Advance Organizer
                                Surface(
                                    shape = RoundedCornerShape(22.dp),
                                    color = colors.card,
                                    border = BorderStroke(1.dp, colors.electricBlue.copy(alpha = 0.5f))
                                ) {
                                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Surface(shape = RoundedCornerShape(50), color = colors.blueTint) {
                                            Text(
                                                text = "BƯỚC 1 · ĐỊNH HƯỚNG TRƯỚC KHI HỌC",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = colors.electricBlue,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                        Text(step.title, style = MaterialTheme.typography.headlineSmall, color = colors.textPrimary)
                                        Text(step.takeaway, style = MaterialTheme.typography.bodyLarge, color = colors.emerald)
                                        Text(step.sourceCitation, style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                                        HorizontalDivider(color = colors.border)
                                        Text("Các mục trọng tâm sắp chinh phục:", style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                                        step.subtopics.forEachIndexed { i, topic ->
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                                                Text("${i + 1}.", fontFamily = JetBrainsMonoFontFamily, color = colors.electricBlue, fontWeight = FontWeight.Bold)
                                                Text(topic, style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary)
                                            }
                                        }
                                    }
                                }
                            } else {
                                // Steps 2-6: Micro-reading + Visual Blocks + Checkpoint
                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = colors.card,
                                    border = BorderStroke(1.dp, colors.border)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Text("BƯỚC ${step.stepNumber} · ${step.title}", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                                        Text(step.readingBody, style = MaterialTheme.typography.bodyLarge, color = colors.textPrimary)
                                        Text(step.sourceCitation, style = MaterialTheme.typography.labelSmall, color = colors.electricBlue)
                                    }
                                }

                                // 3-Card Swipeable Comparison Carousel
                                if (step.comparisonCards.isNotEmpty()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        step.comparisonCards.forEach { card ->
                                            val cardAccent = runCatching {
                                                Color(android.graphics.Color.parseColor(card.accentHex))
                                            }.getOrDefault(colors.electricBlue)
                                            Surface(
                                                modifier = Modifier.width(252.dp),
                                                shape = RoundedCornerShape(18.dp),
                                                color = colors.card,
                                                border = BorderStroke(1.2.dp, cardAccent.copy(alpha = 0.6f))
                                            ) {
                                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    Text(card.tag, style = MaterialTheme.typography.labelSmall, color = cardAccent)
                                                    Text(card.title, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                                                    card.points.forEach { pt ->
                                                        Text("• $pt", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // Red Trap Callout
                                if (!step.trapBody.isNullOrBlank()) {
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = colors.crimsonTint,
                                        border = BorderStroke(1.dp, colors.crimson.copy(alpha = 0.65f))
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                            Text(step.trapTitle ?: "❗ Bẫy tư duy thường gặp", style = MaterialTheme.typography.titleSmall, color = colors.crimson)
                                            Text(step.trapBody, style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary)
                                        }
                                    }
                                }

                                // Framed Mathematical Formula Box (Zero raw _ or ^)
                                step.formula?.let { MathFormulaBox(formula = it) }

                                // Amber Causality Box
                                step.causalityRule?.let { rule ->
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = colors.amberTint,
                                        border = BorderStroke(1.dp, colors.amber.copy(alpha = 0.6f))
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(rule.title, style = MaterialTheme.typography.titleSmall, color = colors.amber)
                                            rule.arrows.forEach { arr ->
                                                Text("▸ $arr", fontFamily = JetBrainsMonoFontFamily, fontSize = 12.5.sp, color = colors.textPrimary)
                                            }
                                            Text(rule.note, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                                        }
                                    }
                                }

                                // Inline Quick MCQ Checkpoint
                                step.inlineMcq?.let { mcq ->
                                    InlineMcqCard(
                                        mcq = mcq,
                                        selectedIdx = mcqSelected,
                                        onSelect = { idx ->
                                            mcqSelected = idx
                                            totalCheckpoints++
                                            if (idx == mcq.correctIndex) {
                                                correctCheckpoints++
                                                mascotMood = MascotMood.CELEBRATE
                                                HapticHelper.tap(context)
                                            } else {
                                                mascotMood = MascotMood.SHAKE
                                                HapticHelper.error(context)
                                                missedList += MissedCheckpoint(mcq.conceptTag, mcq.question, mcq.explanation)
                                                onLogMistake(node.id, mcq.conceptTag, mcq.question, mcq.explanation)
                                                onShowToast("📌 Mình sẽ quay lại phần này vào cuối bài học nhé.")
                                            }
                                        }
                                    )
                                }

                                // Interactive Strike-Through True/False Card
                                step.strikeThroughTf?.let { tf ->
                                    StrikeThroughTrueFalseCard(
                                        item = tf,
                                        answeredChoice = tfSelected,
                                        onAnswer = { chosen ->
                                            tfSelected = chosen
                                            totalCheckpoints++
                                            if (chosen == tf.isTrue) {
                                                correctCheckpoints++
                                                mascotMood = MascotMood.CELEBRATE
                                                HapticHelper.tap(context)
                                            } else {
                                                mascotMood = MascotMood.SHAKE
                                                HapticHelper.error(context)
                                                missedList += MissedCheckpoint(tf.conceptTag, "${tf.prefix}${tf.wrongClause}", tf.explanation)
                                                onLogMistake(node.id, tf.conceptTag, "${tf.prefix}${tf.wrongClause}", tf.explanation)
                                                onShowToast("📌 Mình sẽ quay lại phần này vào cuối bài học nhé.")
                                            }
                                        }
                                    )
                                }
                            }

                            Spacer(Modifier.height(6.dp))

                            Button(
                                onClick = {
                                    HapticHelper.tap(context)
                                    stageIndex++
                                },
                                enabled = checkpointDone,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .navigationBarsPadding()
                                    .heightIn(min = 52.dp)
                                    .testTag("smart_text_continue_button"),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text(
                                    text = if (currentStage == 0) "Bắt đầu học →" else if (checkpointDone) "Tiếp tục →" else "Hãy trả lời chốt chặn để mở khóa",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    currentStage == remediationStage -> {
                        // Step 7: Dynamic Rephrased Remediation
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = colors.amberTint,
                                border = BorderStroke(1.2.dp, colors.amber)
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("🔄 LÀM LẠI — Ôn nhanh trước khi kết thúc", style = MaterialTheme.typography.titleMedium, color = colors.amber)
                                    Text(
                                        text = "AstraFlow tạo câu hỏi diễn đạt hoàn toàn mới theo chiều đảo ngược cho khái niệm bạn vừa nhầm.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.textPrimary
                                    )
                                }
                            }

                            if (remediationLoading) {
                                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        CircularProgressIndicator(color = colors.amber)
                                        Text("Đang biến thể câu hỏi ôn lại...", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                                    }
                                }
                            } else {
                                remediationMcq?.let { mcq ->
                                    InlineMcqCard(
                                        mcq = mcq,
                                        selectedIdx = remediationSelected,
                                        onSelect = { idx ->
                                            remediationSelected = idx
                                            if (idx == mcq.correctIndex) {
                                                mascotMood = MascotMood.CELEBRATE
                                                HapticHelper.tap(context)
                                            } else {
                                                mascotMood = MascotMood.SHAKE
                                                HapticHelper.error(context)
                                            }
                                        }
                                    )
                                }
                                Button(
                                    onClick = {
                                        stageIndex = if (finalQuiz.isNotEmpty()) quizStage else celebrationStage
                                    },
                                    enabled = remediationSelected != null,
                                    modifier = Modifier.fillMaxWidth().navigationBarsPadding().heightIn(min = 52.dp),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Text("Vào bài kiểm tra tổng hợp (Bước 8) →", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    currentStage == quizStage -> {
                        // Step 8: 5-Question Final Synthesis Quiz
                        val q = finalQuiz.getOrNull(quizCursor)
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = "🏁 BƯỚC 8 · KIỂM TRA TỔNG HỢP (${quizCursor + 1}/${finalQuiz.size})",
                                style = MaterialTheme.typography.labelLarge,
                                color = colors.violet
                            )
                            if (q != null) {
                                InlineMcqCard(
                                    mcq = InlineMcq(q.question, q.options, q.correctIndices.firstOrNull() ?: 0, q.explanation, q.conceptTag, q.sourceCitation),
                                    selectedIdx = quizAnswerIdx,
                                    onSelect = { idx ->
                                        quizAnswerIdx = idx
                                        if (idx in q.correctIndices) {
                                            quizCorrectCount++
                                            mascotMood = MascotMood.CELEBRATE
                                            HapticHelper.tap(context)
                                        } else {
                                            mascotMood = MascotMood.SHAKE
                                            HapticHelper.error(context)
                                            onLogMistake(node.id, q.conceptTag, q.question, q.explanation)
                                        }
                                    }
                                )
                            }
                            Button(
                                onClick = {
                                    if (quizCursor < finalQuiz.lastIndex) {
                                        quizCursor++
                                    } else {
                                        val totalQ = (totalCheckpoints + finalQuiz.size).coerceAtLeast(1)
                                        val acc = ((correctCheckpoints + quizCorrectCount) * 100 / totalQ).coerceIn(50, 100)
                                        val stars = 60 + acc / 2
                                        HapticHelper.celebrate(context)
                                        onCompleteNode(node.id, acc, stars)
                                        stageIndex = celebrationStage
                                    }
                                },
                                enabled = quizAnswerIdx != null,
                                modifier = Modifier.fillMaxWidth().navigationBarsPadding().heightIn(min = 52.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text(
                                    text = if (quizCursor < finalQuiz.lastIndex) "Câu tiếp theo →" else "Hoàn thành bài học 🎉",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    else -> {
                        // Lesson Complete Celebration
                        val totalQ = (totalCheckpoints + finalQuiz.size).coerceAtLeast(1)
                        val accuracy = ((correctCheckpoints + quizCorrectCount) * 100 / totalQ).coerceIn(60, 100)
                        val stars = 60 + accuracy / 2

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Spacer(Modifier.height(10.dp))
                            AstraBuddyMascot(mood = MascotMood.CELEBRATE, size = 86.dp)
                            Text("🎉 Hoàn thành xuất sắc!", style = MaterialTheme.typography.headlineMedium, color = colors.emerald)
                            Text(node.title, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                StatBox("Chính xác", "$accuracy%", colors.electricBlue, Modifier.weight(1f))
                                StatBox("Thưởng sao", "+$stars ⭐", colors.amber, Modifier.weight(1f))
                                StatBox("Tiến bộ", "+12%", colors.emerald, Modifier.weight(1f))
                            }

                            // Difficulty Calibration Slider
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = colors.card,
                                border = BorderStroke(1.dp, colors.border),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("Hiệu chỉnh độ khó cho bài tiếp theo", style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                                    Slider(value = difficultyCalib, onValueChange = { difficultyCalib = it }, steps = 1)
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Quá dễ", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                                        Text("Vừa sức", style = MaterialTheme.typography.labelMedium, color = colors.electricBlue)
                                        Text("Quá khó", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                                    }
                                }
                            }

                            Button(
                                onClick = onBack,
                                modifier = Modifier.fillMaxWidth().navigationBarsPadding().heightIn(min = 52.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text("Quay về Lộ trình uốn lượn", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        if (stageIndex == celebrationStage) {
            ConfettiBurstOverlay(trigger = true, modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun StatBox(label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    val colors = LocalAstraColors.current
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = accent.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(value, fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = accent)
            Text(label, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
        }
    }
}

@Composable
fun InlineMcqCard(
    mcq: InlineMcq,
    selectedIdx: Int?,
    onSelect: (Int) -> Unit
) {
    val colors = LocalAstraColors.current
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = colors.card,
        border = BorderStroke(1.dp, colors.border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("🎯 CHỐT CHẶN KIỂM TRA NHANH", style = MaterialTheme.typography.labelSmall, color = colors.electricBlue)
            Text(mcq.question, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)

            mcq.options.forEachIndexed { idx, opt ->
                val isCorrect = idx == mcq.correctIndex
                val isChosen = selectedIdx == idx
                val bg = when {
                    selectedIdx == null -> colors.cardElevated
                    isCorrect -> colors.emeraldTint
                    isChosen -> colors.crimsonTint
                    else -> colors.cardElevated
                }
                val border = when {
                    selectedIdx == null -> colors.border
                    isCorrect -> colors.emerald
                    isChosen -> colors.crimson
                    else -> colors.border
                }
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clickable(enabled = selectedIdx == null) { onSelect(idx) },
                    shape = RoundedCornerShape(12.dp),
                    color = bg,
                    border = BorderStroke(1.2.dp, border)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "${'A' + idx}.",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedIdx != null && isCorrect) colors.emerald else colors.electricBlue
                        )
                        Text(opt, style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary, modifier = Modifier.weight(1f))
                    }
                }
            }

            if (selectedIdx != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (selectedIdx == mcq.correctIndex) colors.emeraldTint else colors.amberTint
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = if (selectedIdx == mcq.correctIndex) "✅ Tuyệt vời! Bạn đã nắm chắc cơ chế." else "💡 Giải thích chuẩn xác:",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selectedIdx == mcq.correctIndex) colors.emerald else colors.amber
                        )
                        Text(mcq.explanation, style = MaterialTheme.typography.bodySmall, color = colors.textPrimary)
                        Text(mcq.sourceCitation, style = MaterialTheme.typography.labelSmall, color = colors.electricBlue)
                    }
                }
            }
        }
    }
}

private fun buildLocalRephrasedRemediation(missed: MissedCheckpoint): InlineMcq = InlineMcq(
    question = "Tình huống đảo ngược về '${missed.conceptTag}': Nhận định nào sau đây phản ánh ĐÚNG bản chất cơ chế bạn vừa ôn lại?",
    options = listOf(
        missed.explanation.take(125),
        "Cơ chế này không chịu tác động của các quy luật kinh tế khách quan",
        "Hai phạm trù liên quan hoàn toàn đồng nhất với nhau về cả lượng và chất",
        "Chỉ áp dụng trong nền kinh tế tự cung tự cấp khép kín"
    ),
    correctIndex = 0,
    explanation = missed.explanation,
    conceptTag = missed.conceptTag,
    sourceCitation = "🔄 Câu hỏi ôn lại Bước 7 · Diễn đạt đảo chiều"
)
