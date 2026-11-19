package com.example.ui.modals

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.*
import com.example.service.DocumentProcessor
import com.example.service.HapticHelper
import com.example.service.LargePdfStrategy
import com.example.service.SelectedDocumentInfo
import com.example.ui.components.ConfettiBurstOverlay
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalAstraColors
import com.example.viewmodel.IngestionProgressState
import com.example.viewmodel.ModelDiscoveryState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// =========================================================================
// 1. CÀI ĐẶT AI (API KEY & DYNAMIC MODEL DISCOVERY MODAL)
// =========================================================================
@Composable
fun AiSettingsDialog(
    settings: AiSettings,
    discoveryState: ModelDiscoveryState,
    onTestAndDiscover: (String) -> Unit,
    onSave: (String, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalAstraColors.current
    var apiKey by remember(settings.apiKey) { mutableStateOf(settings.apiKey) }
    var fastModel by remember(settings.fastModelId) { mutableStateOf(settings.fastModelId) }
    var deepModel by remember(settings.deepModelId) { mutableStateOf(settings.deepModelId) }
    var showKey by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .widthIn(max = 520.dp),
            shape = RoundedCornerShape(24.dp),
            color = colors.card,
            border = BorderStroke(1.dp, colors.border)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("⚙️ Cài đặt AI (API & Models)", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
                    IconButton(onClick = onDismiss) { Icon(Icons.Rounded.Close, contentDescription = "Đóng") }
                }

                Text(
                    text = "Khóa Gemini API được lưu cục bộ trên máy của bạn và gửi qua header x-goog-api-key (an toàn để 2 bạn tự dùng khóa riêng).",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )

                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("Gemini API Key (AIza...)") },
                    visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showKey = !showKey }) {
                            Icon(if (showKey) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, contentDescription = "Hiện/Ẩn khóa")
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().testTag("settings_api_key_input")
                )

                // Mandatory "Kiểm tra kết nối & Tải Model khả dụng" button
                OutlinedButton(
                    onClick = { onTestAndDiscover(apiKey) },
                    enabled = !discoveryState.isChecking && apiKey.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("settings_discover_models_btn"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    if (discoveryState.isChecking) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Đang kiểm tra kết nối & tải danh sách model...")
                    } else {
                        Icon(Icons.Rounded.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Kiểm tra kết nối & Tải Model khả dụng")
                    }
                }

                discoveryState.successMessage?.let { msg ->
                    Surface(shape = RoundedCornerShape(12.dp), color = colors.emeraldTint) {
                        Text("✅ $msg", style = MaterialTheme.typography.bodySmall, color = colors.emerald, modifier = Modifier.padding(10.dp))
                    }
                }
                discoveryState.errorMessage?.let { err ->
                    Surface(shape = RoundedCornerShape(12.dp), color = colors.crimsonTint) {
                        Text("⚠️ $err", style = MaterialTheme.typography.bodySmall, color = colors.crimson, modifier = Modifier.padding(10.dp))
                    }
                }

                // Fast Tutor Model Slot + Chips
                OutlinedTextField(
                    value = fastModel,
                    onValueChange = { fastModel = it },
                    label = { Text("⚡ Model Gia sư nhanh (Chat, Quiz, Vấn đáp)") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    settings.discoveredModels.take(8).forEach { model ->
                        FilterChip(
                            selected = fastModel == model,
                            onClick = { fastModel = model },
                            label = { Text(model, fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp) }
                        )
                    }
                }

                // Deep Reasoning Model Slot + Chips
                OutlinedTextField(
                    value = deepModel,
                    onValueChange = { deepModel = it },
                    label = { Text("🧠 Model Suy luận & Chấm thi sâu (Nạp PDF, OCR)") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    settings.discoveredModels.take(8).forEach { model ->
                        FilterChip(
                            selected = deepModel == model,
                            onClick = { deepModel = model },
                            label = { Text(model, fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp) }
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) {
                        Text("Hủy")
                    }
                    Button(
                        onClick = {
                            onSave(apiKey, fastModel, deepModel)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1.4f).testTag("settings_save_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Lưu cấu hình AI", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// =========================================================================
// 2. NẠP TÀI LIỆU MỚI (+ KHÓA HỌC) STUDIO MODAL (OOM-Safe Large PDF)
// =========================================================================
@Composable
fun DocumentIngestionDialog(
    ingestionState: IngestionProgressState,
    onStartIngest: (String, Int, Double, List<SelectedDocumentInfo>, LargePdfStrategy, Int, Int) -> Unit,
    onOpenAiSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalAstraColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var courseTitle by remember { mutableStateOf("") }
    var examDays by remember { mutableStateOf("14") }
    var targetScore by remember { mutableFloatStateOf(8.5f) }
    val selectedDocs = remember { mutableStateListOf<SelectedDocumentInfo>() }
    var largePdfStrategy by remember { mutableStateOf(LargePdfStrategy.GEMINI_FILES_API) }
    var pageStart by remember { mutableStateOf("1") }
    var pageEnd by remember { mutableStateOf("15") }

    val hasLargePdf = selectedDocs.any { it.isLargePdf }
    val maxPdfPages = selectedDocs.firstOrNull { it.isLargePdf }?.pdfPageCount ?: 30

    val docPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        scope.launch {
            uris.forEach { uri ->
                val info = DocumentProcessor.inspectUri(context, uri)
                selectedDocs += info
                if (courseTitle.isBlank()) {
                    courseTitle = info.displayName.substringBeforeLast('.')
                }
            }
        }
    }

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = 6)
    ) { uris ->
        scope.launch {
            uris.forEach { uri ->
                selectedDocs += DocumentProcessor.inspectUri(context, uri)
            }
        }
    }

    Dialog(onDismissRequest = { if (!ingestionState.isRunning) onDismiss() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .widthIn(max = 540.dp),
            shape = RoundedCornerShape(24.dp),
            color = colors.card,
            border = BorderStroke(1.dp, colors.border)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("📄 Nạp tài liệu mới (+ Khóa học)", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
                    if (!ingestionState.isRunning) {
                        IconButton(onClick = onDismiss) { Icon(Icons.Rounded.Close, contentDescription = "Đóng") }
                    }
                }

                if (ingestionState.isRunning) {
                    // Animated 3-Step AI Ingestion Progress Screen
                    Surface(shape = RoundedCornerShape(18.dp), color = colors.blueTint, border = BorderStroke(1.dp, colors.electricBlue)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp, color = colors.electricBlue)
                                Text("AstraFlow đang thiết kế khóa học...", style = MaterialTheme.typography.titleSmall, color = colors.electricBlue)
                            }
                            listOf(
                                1 to "Đang đọc cấu trúc tài liệu & trích xuất khái niệm cốt lõi...",
                                2 to "Đang thiết kế lộ trình uốn lượn (Winding Path) 5–8 trạm...",
                                3 to "Đang tạo bài Văn bản thông minh & chốt chặn kiểm tra..."
                            ).forEach { (step, label) ->
                                val active = ingestionState.stageIndex == step
                                val done = ingestionState.stageIndex > step
                                Text(
                                    text = "${if (done) "✅" else if (active) "⚡" else "○"} $label",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (done) colors.emerald else if (active) colors.textPrimary else colors.textSecondary
                                )
                            }
                            Text(ingestionState.message, style = MaterialTheme.typography.labelSmall, color = colors.amber)
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = courseTitle,
                        onValueChange = { courseTitle = it },
                        label = { Text("Tên môn học (vd: Triết học Mác - Lênin, Kinh tế vi mô)") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = examDays,
                            onValueChange = { examDays = it.filter(Char::isDigit).take(3) },
                            label = { Text("Số ngày tới kỳ thi") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        )
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            color = colors.cardElevated,
                            border = BorderStroke(1.dp, colors.border)
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                Text("Điểm mục tiêu: ${String.format("%.1f", targetScore)}/10", style = MaterialTheme.typography.labelSmall, color = colors.emerald)
                                Slider(value = targetScore, onValueChange = { targetScore = (it * 2).toInt() / 2f }, valueRange = 6.0f..10.0f)
                            }
                        }
                    }

                    // Zero-Permission SAF + Photo Picker Buttons
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                docPicker.launch(
                                    arrayOf(
                                        "application/pdf",
                                        "text/plain",
                                        "text/markdown",
                                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                                    )
                                )
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Rounded.AttachFile, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("PDF / DOCX / TXT", fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = {
                                photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Ảnh sách / Slide", fontSize = 12.sp)
                        }
                    }

                    if (selectedDocs.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            selectedDocs.forEachIndexed { idx, doc ->
                                Surface(shape = RoundedCornerShape(12.dp), color = colors.cardElevated) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("📄 ${doc.displayName}", style = MaterialTheme.typography.bodySmall, color = colors.textPrimary)
                                            Text(
                                                text = "${doc.sizeMbFormatted}${if (doc.pdfPageCount > 0) " · ${doc.pdfPageCount} trang" else ""}${if (doc.isLargePdf) " · PDF Lớn ≥8MB" else ""}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (doc.isLargePdf) colors.amber else colors.textSecondary
                                            )
                                        }
                                        IconButton(onClick = { selectedDocs.removeAt(idx) }) {
                                            Icon(Icons.Rounded.DeleteOutline, contentDescription = "Bỏ tệp", modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // OOM-Safe Large PDF Selector (>= 8MB)
                    if (hasLargePdf) {
                        Surface(shape = RoundedCornerShape(14.dp), color = colors.amberTint, border = BorderStroke(1.dp, colors.amber)) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("🛡️ Phát hiện PDF lớn (≥ 8MB) — Chống tràn RAM", style = MaterialTheme.typography.titleSmall, color = colors.amber)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilterChip(
                                        selected = largePdfStrategy == LargePdfStrategy.GEMINI_FILES_API,
                                        onClick = { largePdfStrategy = LargePdfStrategy.GEMINI_FILES_API },
                                        label = { Text("Gemini Files API (Chờ ACTIVE)", fontSize = 11.sp) }
                                    )
                                    FilterChip(
                                        selected = largePdfStrategy == LargePdfStrategy.PDF_RENDERER_PAGE_RANGE,
                                        onClick = { largePdfStrategy = LargePdfStrategy.PDF_RENDERER_PAGE_RANGE },
                                        label = { Text("Chọn phạm vi trang", fontSize = 11.sp) }
                                    )
                                }
                                if (largePdfStrategy == LargePdfStrategy.PDF_RENDERER_PAGE_RANGE) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = pageStart,
                                            onValueChange = { pageStart = it.filter(Char::isDigit) },
                                            label = { Text("Từ trang") },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = pageEnd,
                                            onValueChange = { pageEnd = it.filter(Char::isDigit) },
                                            label = { Text("Đến trang (tối đa $maxPdfPages)") },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    ingestionState.error?.let { err ->
                        Surface(shape = RoundedCornerShape(12.dp), color = colors.crimsonTint) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(err, style = MaterialTheme.typography.bodySmall, color = colors.crimson, modifier = Modifier.weight(1f))
                                TextButton(onClick = onOpenAiSettings) { Text("Cài đặt AI") }
                            }
                        }
                    }

                    Button(
                        onClick = {
                            onStartIngest(
                                courseTitle,
                                examDays.toIntOrNull()?.coerceIn(1, 365) ?: 14,
                                targetScore.toDouble(),
                                selectedDocs.toList(),
                                largePdfStrategy,
                                pageStart.toIntOrNull() ?: 1,
                                pageEnd.toIntOrNull() ?: 15
                            )
                        },
                        enabled = selectedDocs.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("🚀 Phân tích & Tạo lộ trình học bằng AI", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// =========================================================================
// 3. CHIA SẺ KHÓA HỌC (JSON EXPORT / IMPORT 1 CHẠM)
// =========================================================================
@Composable
fun ShareCourseDialog(
    courseTitle: String,
    exportString: String,
    onImport: (String) -> Boolean,
    onShowToast: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalAstraColors.current
    val context = LocalContext.current
    var importText by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.94f).widthIn(max = 520.dp),
            shape = RoundedCornerShape(24.dp),
            color = colors.card,
            border = BorderStroke(1.dp, colors.border)
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()).padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("↗ Chia sẻ khóa học (JSON)", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
                    IconButton(onClick = onDismiss) { Icon(Icons.Rounded.Close, contentDescription = "Đóng") }
                }

                Text("Gửi mã khóa học '$courseTitle' qua Zalo/Messenger để bạn bè nhập trong 1 chạm mà không cần nạp lại tài liệu:", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            copyToClipboard(context, "AstraFlow Course", exportString)
                            onShowToast("📋 Đã sao chép mã khóa học! Hãy dán gửi qua Zalo/Messenger.")
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("📋 Sao chép mã") }
                    OutlinedButton(
                        onClick = {
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, exportString)
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Chia sẻ khóa học AstraFlow"))
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("↗ Gửi Zalo/App") }
                }

                HorizontalDivider(color = colors.border)

                Text("📥 Nhập khóa học bạn bè vừa gửi:", style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                OutlinedTextField(
                    value = importText,
                    onValueChange = { importText = it },
                    placeholder = { Text("Dán chuỗi ASTRA-COURSE-V1:... hoặc JSON vào đây") },
                    minLines = 3,
                    maxLines = 4,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        if (onImport(importText)) onDismiss()
                    },
                    enabled = importText.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.emerald, contentColor = Color.White)
                ) { Text("Nhập khóa học ngay", fontWeight = FontWeight.Bold) }
            }
        }
    }
}

// =========================================================================
// 4. ĐẤU TAY ĐÔI (FRIEND DUEL CHALLENGE CODE MODE)
// =========================================================================
@Composable
fun FriendDuelDialog(
    courseTitle: String,
    duelQuestions: List<QuizQuestion>,
    onEncodeDuel: (String, Int, Int, List<QuizQuestion>) -> String,
    onDecodeDuel: (String) -> Result<DuelPayload>,
    onSaveOutcome: (String, String, Int, Int, Int, Int, String) -> Unit,
    onShowToast: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalAstraColors.current
    val context = LocalContext.current

    // Stages: 0 = Menu, 1 = Playing 5-Q Timer, 2 = Creator Code Ready, 3 = Challenger Comparison Victory
    var stage by remember { mutableIntStateOf(0) }
    var creatorName by remember { mutableStateOf("Mình") }
    var incomingCode by remember { mutableStateOf("") }
    var incomingPayload by remember { mutableStateOf<DuelPayload?>(null) }
    var generatedCode by remember { mutableStateOf("") }
    var activeQuestions by remember { mutableStateOf(duelQuestions) }
    var qIdx by remember { mutableIntStateOf(0) }
    var myScore by remember { mutableIntStateOf(0) }
    var elapsedSec by remember { mutableIntStateOf(0) }

    LaunchedEffect(stage) {
        if (stage == 1) {
            elapsedSec = 0
            while (stage == 1) {
                delay(1000L)
                elapsedSec++
            }
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxWidth(0.95f).widthIn(max = 540.dp)) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = colors.card,
                border = BorderStroke(1.2.dp, colors.violet)
            ) {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()).padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("⚔️ Đấu tay đôi (Friend Duel)", style = MaterialTheme.typography.titleLarge, color = colors.violet)
                        IconButton(onClick = onDismiss) { Icon(Icons.Rounded.Close, contentDescription = "Đóng") }
                    }

                    when (stage) {
                        0 -> {
                            OutlinedTextField(
                                value = creatorName,
                                onValueChange = { creatorName = it },
                                label = { Text("Tên hiển thị của bạn") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Button(
                                onClick = {
                                    incomingPayload = null
                                    activeQuestions = duelQuestions.take(5)
                                    qIdx = 0; myScore = 0
                                    stage = 1
                                },
                                enabled = duelQuestions.isNotEmpty(),
                                modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = colors.violet, contentColor = Color.White)
                            ) {
                                Text("🔥 Chơi 5 câu & Tạo mã thách đấu", fontWeight = FontWeight.Bold)
                            }

                            HorizontalDivider(color = colors.border)

                            Text("Hoặc dán mã thách đấu bạn bè gửi:", style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                            OutlinedTextField(
                                value = incomingCode,
                                onValueChange = { incomingCode = it },
                                placeholder = { Text("Dán mã KTCT-XXXX::... vào đây") },
                                minLines = 2,
                                maxLines = 3,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedButton(
                                onClick = {
                                    onDecodeDuel(incomingCode).fold(
                                        onSuccess = { payload ->
                                            incomingPayload = payload
                                            activeQuestions = payload.questions
                                            qIdx = 0; myScore = 0
                                            stage = 1
                                        },
                                        onFailure = { onShowToast("⚠️ Mã thách đấu không hợp lệ.") }
                                    )
                                },
                                enabled = incomingCode.isNotBlank(),
                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) { Text("⚔️ Chấp nhận thách đấu ngay!") }
                        }

                        1 -> {
                            val q = activeQuestions.getOrNull(qIdx)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Câu ${qIdx + 1} / ${activeQuestions.size}", style = MaterialTheme.typography.labelLarge, color = colors.electricBlue)
                                Text("⏱️ ${elapsedSec}s · Điểm: $myScore", fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, color = colors.amber)
                            }
                            if (q != null) {
                                Text(q.question, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                                q.options.forEachIndexed { idx, opt ->
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                HapticHelper.tap(context)
                                                if (idx in q.correctIndices) myScore++
                                                if (qIdx < activeQuestions.lastIndex) {
                                                    qIdx++
                                                } else {
                                                    val opp = incomingPayload
                                                    if (opp == null) {
                                                        generatedCode = onEncodeDuel(creatorName, myScore, elapsedSec, activeQuestions)
                                                        stage = 2
                                                    } else {
                                                        HapticHelper.celebrate(context)
                                                        onSaveOutcome(opp.code, opp.courseTitle, myScore, opp.creatorScore, elapsedSec, opp.creatorTimeSec, opp.creatorName)
                                                        stage = 3
                                                    }
                                                }
                                            },
                                        shape = RoundedCornerShape(12.dp),
                                        color = colors.cardElevated,
                                        border = BorderStroke(1.dp, colors.border)
                                    ) {
                                        Text("${'A' + idx}. $opt", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(12.dp))
                                    }
                                }
                            }
                        }

                        2 -> {
                            val shortPrefix = generatedCode.substringBefore("::")
                            Text("🎉 Đã tạo mã thách đấu: $shortPrefix", style = MaterialTheme.typography.titleMedium, color = colors.emerald)
                            Text("Kỷ lục của bạn: $myScore/5 câu trong ${elapsedSec} giây ($courseTitle)", style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary)
                            Button(
                                onClick = {
                                    copyToClipboard(context, "AstraFlow Duel", generatedCode)
                                    onShowToast("📋 Đã sao chép mã thách đấu $shortPrefix! Gửi ngay cho bạn cùng học.")
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp)
                            ) { Text("📋 Sao chép mã thách đấu gửi Zalo") }
                        }

                        3 -> {
                            val opp = incomingPayload!!
                            val won = myScore > opp.creatorScore || (myScore == opp.creatorScore && elapsedSec <= opp.creatorTimeSec)
                            Text(
                                text = if (won) "🏆 CHIẾN THẮNG! Bạn đã hạ gục ${opp.creatorName}!" else "🔥 RẤT SÁT NÚT! ${opp.creatorName} tạm dẫn trước!",
                                style = MaterialTheme.typography.titleMedium,
                                color = if (won) colors.emerald else colors.amber
                            )
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Surface(modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp), color = colors.blueTint, border = BorderStroke(1.dp, colors.electricBlue)) {
                                    Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Bạn", style = MaterialTheme.typography.labelLarge, color = colors.electricBlue)
                                        Text("$myScore / 5", fontFamily = JetBrainsMonoFontFamily, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                                        Text("${elapsedSec}s", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                                Surface(modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp), color = colors.violetTint, border = BorderStroke(1.dp, colors.violet)) {
                                    Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(opp.creatorName, style = MaterialTheme.typography.labelLarge, color = colors.violet)
                                        Text("${opp.creatorScore} / 5", fontFamily = JetBrainsMonoFontFamily, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                                        Text("${opp.creatorTimeSec}s", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Đóng bảng đấu") }
                        }
                    }
                }
            }
            if (stage == 3) ConfettiBurstOverlay(trigger = true, modifier = Modifier.matchParentSize())
        }
    }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    cb.setPrimaryClip(ClipData.newPlainText(label, text))
}
