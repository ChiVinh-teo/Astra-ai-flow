package com.example.ui.screens

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.service.HapticHelper
import com.example.service.VoiceEngine
import com.example.ui.components.AstraBuddyMascot
import com.example.ui.components.MascotMood
import com.example.ui.theme.LocalAstraColors
import com.example.viewmodel.TutorViewModel

@Composable
fun TutorScreen(
    tutorViewModel: TutorViewModel,
    courses: List<Course>,
    settings: AiSettings,
    voiceEngine: VoiceEngine,
    onSaveFlashcard: (FlashcardItem) -> Unit,
    onOpenAiSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAstraColors.current
    val context = LocalContext.current
    val messages by tutorViewModel.messages.collectAsState()
    val boundCourse by tutorViewModel.boundCourse.collectAsState()
    val tutorMode by tutorViewModel.tutorMode.collectAsState()
    val attachedImage by tutorViewModel.attachedImageUri.collectAsState()
    val isThinking by tutorViewModel.isThinking.collectAsState()
    val errorBanner by tutorViewModel.errorBanner.collectAsState()
    val quickQuiz by tutorViewModel.quickQuizModal.collectAsState()

    var inputText by remember { mutableStateOf("") }
    var isListening by remember { mutableStateOf(false) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var showCourseDropdown by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    LaunchedEffect(messages.size, isThinking) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) tutorViewModel.setAttachedImage(uri)
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && pendingCameraUri != null) {
            tutorViewModel.setAttachedImage(pendingCameraUri)
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            isListening = true
            voiceEngine.startVietnameseListening(
                onResult = { spoken ->
                    isListening = false
                    inputText = if (inputText.isBlank()) spoken else "$inputText $spoken"
                },
                onError = { isListening = false }
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // Top Header + Mandatory Socratic / Fast Toggle + Course Context Chip
        Surface(
            color = colors.card,
            border = BorderStroke(1.dp, colors.border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        AstraBuddyMascot(mood = if (isThinking) MascotMood.CELEBRATE else MascotMood.IDLE, size = 36.dp)
                        Column {
                            Text("Gia sư AI 24/7", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                            Text("Hỏi chữ · Giọng nói · Chụp đề Snap-to-Solve", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(onClick = { tutorViewModel.clearConversation() }) {
                            Icon(Icons.Rounded.DeleteOutline, contentDescription = "Xóa hội thoại", tint = colors.textSecondary)
                        }
                        IconButton(onClick = onOpenAiSettings) {
                            Icon(Icons.Rounded.Tune, contentDescription = "Cài đặt AI", tint = colors.electricBlue)
                        }
                    }
                }

                // Mandatory 2-Mode Toggle: Socratic vs Fast
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.cardElevated)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TutorMode.entries.forEach { mode ->
                        val selected = tutorMode == mode
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    HapticHelper.tap(context)
                                    tutorViewModel.setTutorMode(mode)
                                }
                                .testTag("tutor_mode_${mode.name.lowercase()}"),
                            shape = RoundedCornerShape(10.dp),
                            color = if (selected) colors.electricBlue else Color.Transparent
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = if (mode == TutorMode.SOCRATIC) "🧭 Gợi ý từng bước" else "⚡ Giải thích nhanh",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (selected) Color.White else colors.textSecondary,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                // Course Context Attachment Selector
                Box {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = colors.violetTint,
                        border = BorderStroke(1.dp, colors.violet.copy(alpha = 0.45f)),
                        modifier = Modifier.clickable { showCourseDropdown = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Rounded.MenuBook, contentDescription = null, tint = colors.violet, modifier = Modifier.size(15.dp))
                            Text(
                                text = "Ngữ cảnh giáo trình: ${boundCourse?.title ?: "Tổng hợp tự do"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.violet
                            )
                            Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null, tint = colors.violet, modifier = Modifier.size(16.dp))
                        }
                    }
                    DropdownMenu(expanded = showCourseDropdown, onDismissRequest = { showCourseDropdown = false }) {
                        courses.forEach { c ->
                            DropdownMenuItem(
                                text = { Text("📚 ${c.title}") },
                                onClick = {
                                    showCourseDropdown = false
                                    tutorViewModel.selectContextCourse(c)
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("🌐 Hỏi tự do không gắn giáo trình") },
                            onClick = {
                                showCourseDropdown = false
                                tutorViewModel.selectContextCourse(null)
                            }
                        )
                    }
                }
            }
        }

        if (errorBanner != null) {
            Surface(
                color = colors.crimsonTint,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, colors.crimson.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(errorBanner.orEmpty(), style = MaterialTheme.typography.bodySmall, color = colors.crimson, modifier = Modifier.weight(1f))
                    TextButton(onClick = onOpenAiSettings) { Text("Cài đặt AI") }
                }
            }
        }

        // Chat Transcript or Welcome Prompt Suggestions
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = colors.card,
                        border = BorderStroke(1.dp, colors.border)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "👋 Chào bạn! Mình là Gia sư Socratic AstraFlow",
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.textPrimary
                            )
                            Text(
                                text = "Ở chế độ Gợi ý từng bước, mình sẽ không lộ đáp án ngay mà hỏi gợi mở để bạn tự thông suốt bản chất. Bạn có thể gõ câu hỏi, nói vào micro hoặc chụp ảnh bài tập!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.textSecondary
                            )
                            val starters = listOf(
                                "Tại sao tỷ suất lợi nhuận p' luôn nhỏ hơn tỷ suất giá trị thặng dư m'?",
                                "Phân biệt nhanh Tư bản bất biến (c) với Tư bản cố định giúp mình",
                                "Cho bài tập tính c, v, m, p' với K = 1.200 tỷ, c/v = 3/1, m' = 100%"
                            )
                            starters.forEach { s ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = colors.cardElevated,
                                    border = BorderStroke(1.dp, colors.border),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { tutorViewModel.sendMessage(s, settings) }
                                ) {
                                    Text(
                                        text = "💬 $s",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.electricBlue,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            items(messages, key = { it.id }) { msg ->
                TutorBubbleItem(
                    message = msg,
                    onCreateQuiz = { tutorViewModel.generateQuickQuizFromMessage(msg.text, settings) },
                    onCreateFlashcard = {
                        tutorViewModel.generateFlashcardFromMessage(msg.text, settings, onSaveFlashcard)
                    },
                    onExplainSimpler = { tutorViewModel.explainSimplerWay(msg.text, settings) }
                )
            }

            if (isThinking) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = colors.card,
                        border = BorderStroke(1.dp, colors.border)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = colors.electricBlue)
                            Text("Gia sư AstraFlow đang phân tích...", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                        }
                    }
                }
            }
        }

        // Attached Snap-to-Solve preview strip
        if (attachedImage != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.cardElevated)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AsyncImage(
                    model = attachedImage,
                    contentDescription = "Ảnh đề bài đính kèm",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
                Text("📷 Đã gắn ảnh Snap-to-Solve", style = MaterialTheme.typography.bodySmall, color = colors.emerald, modifier = Modifier.weight(1f))
                IconButton(onClick = { tutorViewModel.setAttachedImage(null) }) {
                    Icon(Icons.Rounded.Close, contentDescription = "Gỡ ảnh", tint = colors.textSecondary)
                }
            }
        }

        // Composer Bar
        Surface(
            color = colors.card,
            border = BorderStroke(1.dp, colors.border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Snap-to-Solve Camera
                IconButton(
                    onClick = {
                        val uri = DocumentProcessor.createTempCameraUri(context)
                        pendingCameraUri = uri
                        cameraLauncher.launch(uri)
                    },
                    modifier = Modifier.testTag("tutor_camera_snap")
                ) {
                    Icon(Icons.Rounded.PhotoCamera, contentDescription = "Chụp ảnh đề bài", tint = colors.electricBlue)
                }

                // Zero-permission Photo Picker
                IconButton(
                    onClick = {
                        photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    modifier = Modifier.testTag("tutor_gallery_snap")
                ) {
                    Icon(Icons.Rounded.Image, contentDescription = "Chọn ảnh đề bài", tint = colors.violet)
                }

                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = {
                        Text(
                            text = if (isListening) "🎙️ Đang nghe tiếng Việt..." else "Hỏi bài hoặc dán đề...",
                            fontSize = 13.sp
                        )
                    },
                    singleLine = false,
                    maxLines = 3,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("tutor_input_field")
                )

                IconButton(
                    onClick = {
                        if (isListening) {
                            voiceEngine.stopListening()
                            isListening = false
                        } else {
                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    modifier = Modifier.testTag("tutor_mic_button")
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Rounded.StopCircle else Icons.Rounded.Mic,
                        contentDescription = "Nói tiếng Việt",
                        tint = if (isListening) colors.crimson else colors.amber
                    )
                }

                IconButton(
                    onClick = {
                        HapticHelper.tap(context)
                        val textToSend = inputText
                        inputText = ""
                        tutorViewModel.sendMessage(textToSend, settings)
                    },
                    enabled = !isThinking && (inputText.isNotBlank() || attachedImage != null),
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(colors.electricBlue)
                        .testTag("tutor_send_button")
                ) {
                    Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Gửi câu hỏi", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }
    }

    // Inline Quick Quiz Modal generated from Tutor Reply
    if (quickQuiz != null) {
        TutorQuickQuizDialog(
            questions = quickQuiz.orEmpty(),
            onDismiss = { tutorViewModel.dismissQuickQuiz() }
        )
    }
}

@Composable
private fun TutorBubbleItem(
    message: TutorChatMessage,
    onCreateQuiz: () -> Unit,
    onCreateFlashcard: () -> Unit,
    onExplainSimpler: () -> Unit
) {
    val colors = LocalAstraColors.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (message.isUser) 18.dp else 4.dp,
                bottomEnd = if (message.isUser) 4.dp else 18.dp
            ),
            color = if (message.isUser) colors.electricBlue else colors.card,
            border = if (message.isUser) null else BorderStroke(1.dp, colors.border),
            modifier = Modifier.widthIn(max = 340.dp)
        ) {
            Column(modifier = Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!message.isUser) {
                    Text(
                        text = if (message.mode == TutorMode.SOCRATIC) "🧭 Gia sư Socratic" else "⚡ Giải thích nhanh",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.electricBlue
                    )
                }
                if (!message.attachedImageUri.isNullOrBlank()) {
                    AsyncImage(
                        model = Uri.parse(message.attachedImageUri),
                        contentDescription = "Ảnh đề bài",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                }
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (message.isUser) Color.White else colors.textPrimary
                )
            }
        }

        // 3 Mandatory Quick Action Chips below every AI Tutor response
        if (!message.isUser) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AssistChip(
                    onClick = onCreateQuiz,
                    label = { Text("Tạo Quiz từ ý này", fontSize = 11.5.sp) },
                    leadingIcon = { Icon(Icons.Rounded.Quiz, contentDescription = null, modifier = Modifier.size(15.dp)) }
                )
                AssistChip(
                    onClick = onCreateFlashcard,
                    label = { Text("Tạo Flashcard", fontSize = 11.5.sp) },
                    leadingIcon = { Icon(Icons.Rounded.Style, contentDescription = null, modifier = Modifier.size(15.dp)) }
                )
                AssistChip(
                    onClick = onExplainSimpler,
                    label = { Text("Giải thích cách khác dễ hiểu hơn", fontSize = 11.5.sp) },
                    leadingIcon = { Icon(Icons.Rounded.Lightbulb, contentDescription = null, modifier = Modifier.size(15.dp)) }
                )
            }
        }
    }
}

@Composable
private fun TutorQuickQuizDialog(
    questions: List<QuizQuestion>,
    onDismiss: () -> Unit
) {
    val colors = LocalAstraColors.current
    var idx by remember { mutableIntStateOf(0) }
    var selected by remember(idx) { mutableStateOf<Int?>(null) }
    val q = questions.getOrNull(idx)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("⚡ Mini-Quiz từ Gia sư (${idx + 1}/${questions.size})") },
        text = {
            if (q != null) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(q.question, style = MaterialTheme.typography.titleSmall)
                    q.options.forEachIndexed { optIdx, opt ->
                        val isCorrect = optIdx in q.correctIndices
                        val bg = when {
                            selected == null -> colors.cardElevated
                            isCorrect -> colors.emeraldTint
                            selected == optIdx -> colors.crimsonTint
                            else -> colors.cardElevated
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = bg,
                            border = BorderStroke(1.dp, if (selected == optIdx) colors.electricBlue else colors.border),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = selected == null) { selected = optIdx }
                        ) {
                            Text(opt, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(10.dp))
                        }
                    }
                    if (selected != null) {
                        Text(q.explanation, style = MaterialTheme.typography.bodySmall, color = colors.emerald)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (idx < questions.lastIndex) idx++ else onDismiss()
            }) {
                Text(if (idx < questions.lastIndex) "Câu tiếp →" else "Hoàn tất")
            }
        }
    )
}
