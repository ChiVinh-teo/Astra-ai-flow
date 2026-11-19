package com.example.ui.modes

import android.Manifest
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.*
import com.example.service.DocumentProcessor
import com.example.service.GeminiDomainUseCases
import com.example.service.HapticHelper
import com.example.service.VoiceEngine
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalAstraColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// =========================================================================
// MODE 10: ĐỀ THI THỬ VIẾT 80 PHÚT (Typing + Handwritten Photo OCR + Astra AI Grading Card)
// =========================================================================
@Composable
fun WrittenMockExamScreen(
    course: Course,
    settings: AiSettings,
    geminiUseCases: GeminiDomainUseCases,
    onToggleWhitePaper: () -> Unit,
    onOpenAiSettings: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val colors = LocalAstraColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val questions = course.writtenExamQuestions
    var secondsLeft by remember { mutableIntStateOf(80 * 60) }
    var activeQIdx by remember { mutableIntStateOf(0) }
    val typedAnswers = remember { mutableStateMapOf<String, String>() }
    val photoUris = remember { mutableStateMapOf<String, Uri>() }
    val gradingCards = remember { mutableStateMapOf<String, AiGradingCard>() }
    var gradingQId by remember { mutableStateOf<String?>(null) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    // Inline "💬 Hỏi gia sư" mini-chat per question
    var followUpInput by remember(activeQIdx) { mutableStateOf("") }
    var followUpLoading by remember(activeQIdx) { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (secondsLeft > 0) {
            delay(1000L)
            secondsLeft--
        }
    }

    val currentQ = questions.getOrNull(activeQIdx)
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null && currentQ != null) photoUris[currentQ.id] = uri
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok && pendingCameraUri != null && currentQ != null) photoUris[currentQ.id] = pendingCameraUri!!
    }

    val mm = secondsLeft / 60
    val ss = secondsLeft % 60
    val timerText = String.format("%02d:%02d", mm, ss)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        ModeTopHeader(
            title = "⏱️ Đề thi thử viết (80 phút)",
            subtitle = "Gõ trực tiếp hoặc chụp ảnh bài viết tay chấm AI",
            onBack = onBack,
            trailing = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(shape = RoundedCornerShape(10.dp), color = colors.violetTint) {
                        Text(
                            text = timerText,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = colors.violet,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }
                    IconButton(onClick = onToggleWhitePaper) {
                        Icon(
                            imageVector = if (settings.isWhitePaperExam) Icons.Rounded.DarkMode else Icons.Rounded.Description,
                            contentDescription = "Chế độ Giấy thi trắng chữ đen",
                            tint = colors.electricBlue
                        )
                    }
                }
            }
        )

        if (currentQ != null) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Question Selector Tabs
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    questions.forEachIndexed { idx, q ->
                        val graded = gradingCards.containsKey(q.id)
                        Surface(
                            modifier = Modifier.weight(1f).clickable { activeQIdx = idx },
                            shape = RoundedCornerShape(12.dp),
                            color = if (idx == activeQIdx) colors.violet else colors.card,
                            border = BorderStroke(1.dp, if (graded) colors.emerald else colors.border)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 8.dp)) {
                                Text(
                                    text = "Câu ${idx + 1}${if (graded) " ✓" else ""}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (idx == activeQIdx) Color.White else colors.textPrimary
                                )
                            }
                        }
                    }
                }

                // Exam Prompt Card
                Surface(shape = RoundedCornerShape(18.dp), color = colors.card, border = BorderStroke(1.dp, colors.border)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(currentQ.partLabel, style = MaterialTheme.typography.labelLarge, color = colors.violet)
                            Text("${currentQ.points} điểm", fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, color = colors.amber)
                        }
                        Text(currentQ.prompt, style = MaterialTheme.typography.bodyLarge, color = colors.textPrimary)
                        Text(currentQ.sourceCitation, style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                    }
                }

                // Typed Answer Box + Handwritten Photo Upload
                OutlinedTextField(
                    value = typedAnswers[currentQ.id].orEmpty(),
                    onValueChange = { typedAnswers[currentQ.id] = it },
                    label = { Text("Gõ bài làm tự luận / các bước tính của bạn...") },
                    minLines = 5,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().testTag("written_exam_input")
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            val uri = DocumentProcessor.createTempCameraUri(context)
                            pendingCameraUri = uri
                            cameraLauncher.launch(uri)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Chụp bài viết tay", fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = {
                            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Chọn ảnh giấy thi", fontSize = 12.sp)
                    }
                }

                photoUris[currentQ.id]?.let { uri ->
                    Surface(shape = RoundedCornerShape(14.dp), color = colors.cardElevated, border = BorderStroke(1.dp, colors.emerald)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            AsyncImage(
                                model = uri,
                                contentDescription = "Ảnh bài làm viết tay",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(10.dp))
                            )
                            Text("📷 Đã đính kèm ảnh bài viết tay để Gemini Vision OCR chấm", style = MaterialTheme.typography.bodySmall, color = colors.emerald, modifier = Modifier.weight(1f))
                            IconButton(onClick = { photoUris.remove(currentQ.id) }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Xóa ảnh")
                            }
                        }
                    }
                }

                // Grade Button
                Button(
                    onClick = {
                        val answer = typedAnswers[currentQ.id].orEmpty()
                        val photo = photoUris[currentQ.id]
                        if (answer.isBlank() && photo == null) return@Button
                        HapticHelper.tap(context)
                        gradingQId = currentQ.id
                        scope.launch {
                            val card = if (settings.apiKey.isNotBlank()) {
                                geminiUseCases.gradeWrittenAnswer(context, settings, currentQ, answer, photo)
                                    .getOrElse { buildOfflineGradingFallback(currentQ, answer) }
                            } else {
                                buildOfflineGradingFallback(currentQ, answer)
                            }
                            gradingCards[currentQ.id] = card
                            gradingQId = null
                        }
                    },
                    enabled = gradingQId == null && (typedAnswers[currentQ.id].orEmpty().isNotBlank() || photoUris[currentQ.id] != null),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("written_exam_grade_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.violet, contentColor = Color.White)
                ) {
                    if (gradingQId == currentQ.id) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text("AI đang chấm từng bước & dò lỗi...")
                    } else {
                        Text("⚡ Chấm bài theo chuẩn Astra AI", fontWeight = FontWeight.Bold)
                    }
                }

                // Signature Astra AI Step-by-Step Grading Card
                gradingCards[currentQ.id]?.let { card ->
                    AstraGradingResultCard(
                        card = card,
                        followUpInput = followUpInput,
                        followUpLoading = followUpLoading,
                        onFollowUpInputChange = { followUpInput = it },
                        onSendFollowUp = {
                            val qText = followUpInput.trim()
                            if (qText.isBlank()) return@AstraGradingResultCard
                            followUpInput = ""
                            followUpLoading = true
                            val withUser = card.copy(followUpChat = card.followUpChat + (true to qText))
                            gradingCards[currentQ.id] = withUser
                            scope.launch {
                                val reply = if (settings.apiKey.isNotBlank()) {
                                    geminiUseCases.askGradingFollowUp(settings, currentQ, card, qText)
                                        .getOrElse { "Hãy đối chiếu lại bước: ${card.whereYouWentWrong}" }
                                } else {
                                    "Mẹo nhanh: Đối chiếu kỹ lại bước sai sót trên và gắn Gemini API Key trong Cài đặt AI để trao đổi chi tiết hơn nhé!"
                                }
                                gradingCards[currentQ.id] = withUser.copy(followUpChat = withUser.followUpChat + (false to reply))
                                followUpLoading = false
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AstraGradingResultCard(
    card: AiGradingCard,
    followUpInput: String,
    followUpLoading: Boolean,
    onFollowUpInputChange: (String) -> Unit,
    onSendFollowUp: () -> Unit
) {
    val colors = LocalAstraColors.current
    val scoreColor = when {
        card.scorePercent >= 75 -> colors.emerald
        card.scorePercent >= 45 -> colors.amber
        else -> colors.crimson
    }
    var showTutorDrawer by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.card,
        border = BorderStroke(1.5.dp, scoreColor)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("ĐÁNH GIÁ AI | CÂU TRẢ LỜI CỦA BẠN", style = MaterialTheme.typography.labelLarge, color = colors.textPrimary)
                Surface(shape = RoundedCornerShape(50), color = scoreColor.copy(alpha = 0.18f), border = BorderStroke(1.dp, scoreColor)) {
                    Text(
                        text = "${card.scorePercent}%",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = scoreColor,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }

            // 1. Standard Solution
            Surface(shape = RoundedCornerShape(14.dp), color = colors.emeraldTint) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("✅ Lời giải chuẩn", style = MaterialTheme.typography.titleSmall, color = colors.emerald)
                    Text(card.standardSolution, style = MaterialTheme.typography.bodySmall, color = colors.textPrimary)
                    Text(card.sourceCitation, style = MaterialTheme.typography.labelSmall, color = colors.electricBlue)
                }
            }

            // 2. Where you went wrong
            Surface(shape = RoundedCornerShape(14.dp), color = colors.crimsonTint, border = BorderStroke(1.dp, colors.crimson.copy(alpha = 0.5f))) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("⚠️ Sai sót của bạn (Where you went wrong)", style = MaterialTheme.typography.titleSmall, color = colors.crimson)
                    Text(card.whereYouWentWrong, style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary)
                }
            }

            // 3. Focus Areas
            Surface(shape = RoundedCornerShape(14.dp), color = colors.amberTint) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("📚 Các phần cần tập trung", style = MaterialTheme.typography.titleSmall, color = colors.amber)
                    Text(card.focusSummary, style = MaterialTheme.typography.bodySmall, color = colors.textPrimary)
                    card.focusBullets.forEach { b ->
                        Text("• $b", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                    }
                }
            }

            // 4. Inline Follow-up Tutor Drawer
            OutlinedButton(
                onClick = { showTutorDrawer = !showTutorDrawer },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (showTutorDrawer) "Ẩn ngăn kéo 💬 Hỏi gia sư" else "💬 Hỏi gia sư về lỗi sai này")
            }

            if (showTutorDrawer) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    card.followUpChat.forEach { (isUser, msg) ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isUser) colors.blueTint else colors.cardElevated
                        ) {
                            Text(
                                text = (if (isUser) "Bạn: " else "Gia sư: ") + msg,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textPrimary,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = followUpInput,
                            onValueChange = onFollowUpInputChange,
                            placeholder = { Text("Hỏi thêm về bước sai...", fontSize = 12.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = onSendFollowUp, enabled = !followUpLoading) {
                            Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Gửi", tint = colors.electricBlue)
                        }
                    }
                }
            }
        }
    }
}

private fun buildOfflineGradingFallback(q: WrittenExamQuestion, answer: String): AiGradingCard {
    val hits = q.keyRubricPoints.count { pt ->
        val keywords = pt.split(" ").filter { it.length > 3 }.take(3)
        keywords.any { answer.contains(it, ignoreCase = true) }
    }
    val pct = ((hits * 100) / q.keyRubricPoints.size.coerceAtLeast(1)).coerceIn(35, 85)
    return AiGradingCard(
        questionId = q.id,
        scorePercent = pct,
        standardSolution = q.standardSolution,
        whereYouWentWrong = "Chấm đối chiếu ngoại tuyến: Bài làm của bạn chạm được $hits/${q.keyRubricPoints.size} ý chính trong thang điểm. Hãy dán Gemini API Key trong Cài đặt AI để chấm sâu từng câu chữ & đọc ảnh viết tay OCR!",
        focusSummary = "Cần trình bày đầy đủ lập luận nhân quả và công thức chuẩn theo giáo trình.",
        focusBullets = q.keyRubricPoints.take(3),
        sourceCitation = q.sourceCitation
    )
}

// =========================================================================
// MODE 11: THI THỬ VẤN ĐÁP (Oral Viva Examiner + Voice STT / TTS)
// =========================================================================
@Composable
fun OralVivaScreen(
    course: Course,
    settings: AiSettings,
    geminiUseCases: GeminiDomainUseCases,
    voiceEngine: VoiceEngine,
    onBack: () -> Unit
) {
    BackHandler {
        voiceEngine.stopSpeaking()
        voiceEngine.stopListening()
        onBack()
    }
    DisposableEffect(Unit) {
        onDispose {
            voiceEngine.stopSpeaking()
            voiceEngine.stopListening()
        }
    }

    val colors = LocalAstraColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val firstQuestion = remember(course.id) {
        if (course.isKtctFlagship) {
            "Chào em! Chúng ta bắt đầu buổi thi vấn đáp môn Kinh tế chính trị Mác - Lênin. Câu hỏi mở đầu: Em hãy phân biệt bản chất của Lao động cụ thể với Lao động trừu tượng, và cho biết vì sao tỷ suất lợi nhuận p' luôn nhỏ hơn tỷ suất giá trị thặng dư m'?"
        } else {
            "Chào em! Bắt đầu thi vấn đáp môn ${course.title}. Em hãy trình bày khái niệm cốt lõi nhất ở trạm 1 (${course.nodes.firstOrNull()?.title ?: course.title}) và nêu một ví dụ thực tiễn?"
        }
    }

    val transcript = remember(course.id) { mutableStateListOf(false to firstQuestion) }
    var input by remember { mutableStateOf("") }
    var turnCount by remember { mutableIntStateOf(0) }
    var isThinking by remember { mutableStateOf(false) }
    var isListening by remember { mutableStateOf(false) }

    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            isListening = true
            voiceEngine.startVietnameseListening(
                onResult = { spoken ->
                    isListening = false
                    input = if (input.isBlank()) spoken else "$input $spoken"
                },
                onError = { isListening = false }
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        ModeTopHeader(
            title = "🗣️ Thi thử vấn đáp (Oral Viva)",
            subtitle = "Giám khảo hỏi xoáy 2 lượt → Chấm điểm Rubric /10",
            onBack = {
                voiceEngine.stopSpeaking()
                onBack()
            },
            trailing = {
                IconButton(onClick = {
                    val lastExaminer = transcript.lastOrNull { !it.first }?.second.orEmpty()
                    if (lastExaminer.isNotBlank()) voiceEngine.speakSingle(lastExaminer, PodcastSpeaker.TS_LAN)
                }) {
                    Icon(Icons.Rounded.VolumeUp, contentDescription = "Đọc to câu hỏi", tint = colors.electricBlue)
                }
            }
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(transcript) { (isStudent, msg) ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = if (isStudent) Alignment.End else Alignment.Start
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isStudent) colors.electricBlue else colors.card,
                        border = if (isStudent) null else BorderStroke(1.dp, colors.violet.copy(alpha = 0.5f)),
                        modifier = Modifier.widthIn(max = 330.dp)
                    ) {
                        Column(modifier = Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = if (isStudent) "🎓 Bạn trả lời" else "👨‍🏫 Giám khảo vấn đáp",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isStudent) Color.White.copy(alpha = 0.85f) else colors.violet
                            )
                            Text(msg, style = MaterialTheme.typography.bodyMedium, color = if (isStudent) Color.White else colors.textPrimary)
                        }
                    }
                }
            }
            if (isThinking) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = colors.violet)
                        Text("Giám khảo đang đánh giá & chuẩn bị câu hỏi xoáy...", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                    }
                }
            }
        }

        Surface(color = colors.card, border = BorderStroke(1.dp, colors.border)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = {
                        if (isListening) {
                            voiceEngine.stopListening()
                            isListening = false
                        } else {
                            micPermission.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Rounded.StopCircle else Icons.Rounded.Mic,
                        contentDescription = "Trả lời bằng giọng nói",
                        tint = if (isListening) colors.crimson else colors.amber
                    )
                }

                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    placeholder = { Text(if (isListening) "🎙️ Đang nghe..." else "Nói hoặc gõ câu trả lời vấn đáp...", fontSize = 13.sp) },
                    modifier = Modifier.weight(1f),
                    maxLines = 3,
                    shape = RoundedCornerShape(14.dp)
                )

                IconButton(
                    onClick = {
                        val ans = input.trim()
                        if (ans.isBlank()) return@IconButton
                        HapticHelper.tap(context)
                        input = ""
                        transcript += (true to ans)
                        turnCount++
                        isThinking = true
                        scope.launch {
                            val reply = if (settings.apiKey.isNotBlank()) {
                                geminiUseCases.conductOralVivaTurn(
                                    settings = settings,
                                    courseTitle = course.title,
                                    topic = course.nodes.firstOrNull()?.title ?: course.title,
                                    transcript = transcript.toList(),
                                    turnNumber = turnCount
                                ).getOrElse { fallbackVivaReply(turnCount) }
                            } else {
                                fallbackVivaReply(turnCount)
                            }
                            transcript += (false to reply)
                            voiceEngine.speakSingle(reply, PodcastSpeaker.TS_LAN)
                            isThinking = false
                        }
                    },
                    enabled = !isThinking && input.isNotBlank(),
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(colors.violet)
                ) {
                    Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Gửi câu trả lời", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

private fun fallbackVivaReply(turn: Int): String = when (turn) {
    1 -> "Em nắm được ý nền tảng khá tốt! Tuy nhiên thầy hỏi xoáy thêm: Nếu cấu tạo hữu cơ c/v tăng mạnh do tự động hóa, trong khi tỷ suất giá trị thặng dư m' giữ nguyên, thì tỷ suất lợi nhuận p' sẽ biến động ra sao và tại sao?"
    2 -> "Lập luận nhân quả hợp lý! Một câu hỏi phụ nữa trước khi chốt điểm: Khi doanh nghiệp tăng cường độ lao động thay vì tăng năng suất lao động, lượng giá trị trong 1 đơn vị sản phẩm có thay đổi không?"
    else -> "Tổng kết vấn đáp (Rubric 8.5/10):\n• Kiến thức nền: 3.5/4\n• Tư duy nhân quả: 2.5/3\n• Phản xạ hỏi xoáy: 2.5/3\nNhận xét: Em phản xạ tốt! Gắn thêm Gemini API Key trong Cài đặt AI để giám khảo vấn đáp thích ứng sâu theo mọi câu trả lời của em."
}
