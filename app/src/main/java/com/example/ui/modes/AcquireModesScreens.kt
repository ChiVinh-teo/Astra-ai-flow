package com.example.ui.modes

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.service.GeminiDomainUseCases
import com.example.service.HapticHelper
import com.example.service.VoiceEngine
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalAstraColors
import kotlinx.coroutines.launch
import kotlin.math.sin

// =========================================================================
// MODE 2: PODCAST 2 HOST (Host Minh & TS. Lan · Dual-Voice TTS + Waveform)
// =========================================================================
@Composable
fun PodcastModeScreen(
    course: Course,
    ttsSpeed: Float,
    voiceEngine: VoiceEngine,
    onSpeedChange: (Float) -> Unit,
    onBack: () -> Unit
) {
    BackHandler {
        voiceEngine.stopSpeaking()
        onBack()
    }
    DisposableEffect(Unit) {
        onDispose { voiceEngine.stopSpeaking() }
    }

    val colors = LocalAstraColors.current
    val context = LocalContext.current
    val chapters = course.podcastChapters
    var chapterIdx by remember { mutableIntStateOf(0) }
    var isPlaying by remember { mutableStateOf(false) }
    var activeLineIdx by remember(chapterIdx) { mutableIntStateOf(-1) }
    var checkpointSelected by remember(chapterIdx) { mutableStateOf<Int?>(null) }

    val listState = rememberLazyListState()
    val currentChapter = chapters.getOrNull(chapterIdx)

    LaunchedEffect(activeLineIdx) {
        if (activeLineIdx >= 0) {
            listState.animateScrollToItem(activeLineIdx)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        ModeTopHeader(
            title = "🎙️ Podcast 2 Host (TTS vi-VN)",
            subtitle = "Host Minh (trẻ trung) & TS. Lan (trầm ấm)",
            onBack = {
                voiceEngine.stopSpeaking()
                onBack()
            }
        )

        // Chapter selector pills
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            chapters.forEachIndexed { idx, ch ->
                val selected = idx == chapterIdx
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            voiceEngine.stopSpeaking()
                            isPlaying = false
                            chapterIdx = idx
                        },
                    shape = RoundedCornerShape(12.dp),
                    color = if (selected) colors.electricBlue else colors.card,
                    border = BorderStroke(1.dp, if (selected) colors.electricBlue else colors.border)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 8.dp)) {
                        Text(
                            text = "Chương ${ch.chapterNumber}",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selected) Color.White else colors.textSecondary
                        )
                    }
                }
            }
        }

        if (currentChapter != null) {
            // Audio Controller + Live Animated Waveform
            Surface(
                modifier = Modifier
                    .padding(horizontal = 14.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = colors.card,
                border = BorderStroke(1.dp, if (isPlaying) colors.electricBlue else colors.border)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(currentChapter.title, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                HapticHelper.tap(context)
                                if (isPlaying) {
                                    voiceEngine.stopSpeaking()
                                    isPlaying = false
                                } else {
                                    isPlaying = true
                                    voiceEngine.playPodcastLines(
                                        lines = currentChapter.lines,
                                        startIndex = if (activeLineIdx in currentChapter.lines.indices) activeLineIdx else 0,
                                        speed = ttsSpeed,
                                        onActiveLine = { idx -> activeLineIdx = idx },
                                        onComplete = { isPlaying = false }
                                    )
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.testTag("podcast_play_toggle")
                        ) {
                            Icon(if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text(if (isPlaying) "Tạm dừng" else "Phát hội thoại")
                        }

                        AudioWaveformBars(isPlaying = isPlaying, modifier = Modifier.weight(1f).height(34.dp))

                        // Speed toggle pill
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = colors.cardElevated,
                            modifier = Modifier.clickable {
                                val nextSpeed = when {
                                    ttsSpeed < 0.95f -> 1.0f
                                    ttsSpeed < 1.2f -> 1.25f
                                    else -> 0.85f
                                }
                                onSpeedChange(nextSpeed)
                                voiceEngine.setBaseSpeed(nextSpeed)
                            }
                        ) {
                            Text(
                                text = "${ttsSpeed}x",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = colors.amber,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(currentChapter.lines) { idx, line ->
                    val isActive = idx == activeLineIdx
                    val isMinh = line.speaker == PodcastSpeaker.HOST_MINH
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                activeLineIdx = idx
                                isPlaying = true
                                voiceEngine.playPodcastLines(
                                    lines = currentChapter.lines,
                                    startIndex = idx,
                                    speed = ttsSpeed,
                                    onActiveLine = { activeLineIdx = it },
                                    onComplete = { isPlaying = false }
                                )
                            },
                        shape = RoundedCornerShape(16.dp),
                        color = if (isActive) {
                            if (isMinh) colors.blueTint else colors.violetTint
                        } else colors.card,
                        border = BorderStroke(
                            width = if (isActive) 1.8.dp else 1.dp,
                            color = if (isActive) (if (isMinh) colors.electricBlue else colors.violet) else colors.border
                        )
                    ) {
                        Column(modifier = Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (isMinh) "🎧 Host Minh" else "🎓 TS. Lan",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (isMinh) colors.electricBlue else colors.violet
                                )
                                if (isActive && isPlaying) {
                                    Text("● Đang nói...", style = MaterialTheme.typography.labelSmall, color = colors.emerald)
                                }
                            }
                            Text(line.text, style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary)
                        }
                    }
                }

                // Chapter Checkpoint MCQ
                item {
                    InlineMcqCard(
                        mcq = currentChapter.checkpoint,
                        selectedIdx = checkpointSelected,
                        onSelect = { checkpointSelected = it }
                    )
                }
            }
        }
    }
}

@Composable
private fun AudioWaveformBars(isPlaying: Boolean, modifier: Modifier = Modifier) {
    val colors = LocalAstraColors.current
    val inf = rememberInfiniteTransition(label = "wave")
    val phase by inf.animateFloat(
        initialValue = 0f,
        targetValue = 6.283f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing)),
        label = "wave_phase"
    )
    Canvas(modifier = modifier) {
        val barCount = 18
        val barW = size.width / (barCount * 1.7f)
        val gap = (size.width - barW * barCount) / (barCount - 1).coerceAtLeast(1)
        for (i in 0 until barCount) {
            val amp = if (isPlaying) (0.28f + 0.68f * ((sin(phase + i * 0.65f) + 1f) / 2f)) else 0.18f
            val h = size.height * amp
            drawRoundRect(
                color = if (i % 2 == 0) colors.electricBlue else colors.violet,
                topLeft = Offset(i * (barW + gap), (size.height - h) / 2f),
                size = Size(barW, h),
                cornerRadius = CornerRadius(4f, 4f)
            )
        }
    }
}

// =========================================================================
// MODE 3: VIDEO MÔ PHỎNG CƠ CHẾ ĐỘNG (Surplus Value Simulator + Storyboard)
// =========================================================================
@Composable
fun VideoSimulatorScreen(
    course: Course,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val colors = LocalAstraColors.current
    val context = LocalContext.current

    // Interactive Surplus Value & Profit Simulator State (KTCT Flagship)
    var cVal by remember { mutableFloatStateOf(400f) } // Tư bản bất biến (tỷ)
    var vVal by remember { mutableFloatStateOf(100f) } // Tư bản khả biến (tỷ)
    var tVal by remember { mutableFloatStateOf(4f) }   // Thời gian LĐ tất yếu (giờ)
    var tPrimeVal by remember { mutableFloatStateOf(4f) } // Thời gian LĐ thặng dư (giờ)

    val mPrime = if (tVal > 0f) (tPrimeVal / tVal) * 100f else 0f
    val mVal = vVal * (mPrime / 100f)
    val pPrime = if (cVal + vVal > 0f) (mVal / (cVal + vVal)) * 100f else 0f
    val organicRatio = if (vVal > 0f) cVal / vVal else 0f
    val totalG = cVal + vVal + mVal

    var activeFrameIdx by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        ModeTopHeader(
            title = "▶️ Mô phỏng Cơ chế Động",
            subtitle = if (course.isKtctFlagship) "Phòng Lab Giá trị thặng dư & Tỷ suất lợi nhuận" else "Bảng phân cảnh tương tác · ${course.title}",
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (course.isKtctFlagship) {
                // 3 Preset Scenarios for 3 Methods of Surplus Value Production
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PresetChip("Tuyệt đối (t'↑)", colors.amber, Modifier.weight(1f)) {
                        HapticHelper.tap(context)
                        cVal = 400f; vVal = 100f; tVal = 4f; tPrimeVal = 6f
                    }
                    PresetChip("Tương đối (t↓)", colors.emerald, Modifier.weight(1f)) {
                        HapticHelper.tap(context)
                        cVal = 450f; vVal = 100f; tVal = 2.5f; tPrimeVal = 5.5f
                    }
                    PresetChip("c/v cao (p'↓)", colors.violet, Modifier.weight(1f)) {
                        HapticHelper.tap(context)
                        cVal = 800f; vVal = 100f; tVal = 4f; tPrimeVal = 4f
                    }
                }

                // Real-time Animated Bar & Value Flow Canvas
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = colors.card,
                    border = BorderStroke(1.dp, colors.electricBlue.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("📊 Cơ cấu Tổng giá trị G = c + v + m (${totalG.toInt()} tỷ)", style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)

                        val cFrac by animateFloatAsState(cVal / totalG.coerceAtLeast(1f), tween(300), label = "c")
                        val vFrac by animateFloatAsState(vVal / totalG.coerceAtLeast(1f), tween(300), label = "v")
                        val mFrac by animateFloatAsState(mVal / totalG.coerceAtLeast(1f), tween(300), label = "m")

                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                        ) {
                            val w = size.width
                            val h = size.height
                            drawRect(color = colors.amber, topLeft = Offset(0f, 0f), size = Size(w * cFrac, h))
                            drawRect(color = colors.electricBlue, topLeft = Offset(w * cFrac, 0f), size = Size(w * vFrac, h))
                            drawRect(color = colors.emerald, topLeft = Offset(w * (cFrac + vFrac), 0f), size = Size(w * mFrac, h))
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("■ c = ${cVal.toInt()} tỷ", style = MaterialTheme.typography.labelSmall, color = colors.amber)
                            Text("■ v = ${vVal.toInt()} tỷ", style = MaterialTheme.typography.labelSmall, color = colors.electricBlue)
                            Text("■ m = ${mVal.toInt()} tỷ", style = MaterialTheme.typography.labelSmall, color = colors.emerald)
                        }

                        HorizontalDivider(color = colors.border)

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SimMetric("m' = t'/t", "${mPrime.toInt()}%", colors.emerald, Modifier.weight(1f))
                            SimMetric("p' = m/(c+v)", "${String.format("%.1f", pPrime)}%", colors.violet, Modifier.weight(1f))
                            SimMetric("Cấu tạo c/v", "${String.format("%.1f", organicRatio)}/1", colors.amber, Modifier.weight(1f))
                        }

                        Text(
                            text = "💡 Nhận xét trực tiếp: Luôn có p' (${String.format("%.1f", pPrime)}%) < m' (${mPrime.toInt()}%). Khi tăng c (c/v tăng) mà m' không đổi, p' tự động giảm!",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary
                        )
                    }
                }

                // 4 Live Sliders
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = colors.card,
                    border = BorderStroke(1.dp, colors.border)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SimSlider("Tư bản bất biến c (Máy móc, NVL)", "${cVal.toInt()} tỷ", cVal, 100f..1000f, colors.amber) { cVal = it }
                        SimSlider("Tư bản khả biến v (Tiền lương)", "${vVal.toInt()} tỷ", vVal, 50f..400f, colors.electricBlue) { vVal = it }
                        SimSlider("Thời gian LĐ tất yếu t", "${String.format("%.1f", tVal)} giờ", tVal, 1.5f..8f, colors.violet) { tVal = it }
                        SimSlider("Thời gian LĐ thặng dư t'", "${String.format("%.1f", tPrimeVal)} giờ", tPrimeVal, 1f..10f, colors.emerald) { tPrimeVal = it }
                    }
                }
            }

            // Frame-by-frame Visual Storyboard Walkthrough (Works for BOTH KTCT & Uploaded Subjects!)
            if (course.storyboardFrames.isNotEmpty()) {
                val frame = course.storyboardFrames[activeFrameIdx.coerceIn(0, course.storyboardFrames.lastIndex)]
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = colors.card,
                    border = BorderStroke(1.dp, colors.violet.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("🎬 Bảng phân cảnh từng bước (${activeFrameIdx + 1}/${course.storyboardFrames.size})", style = MaterialTheme.typography.labelLarge, color = colors.violet)
                        Text("${frame.stepLabel}: ${frame.title}", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                        Text(frame.description, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                        if (frame.highlightFormula.isNotBlank()) {
                            Surface(shape = RoundedCornerShape(12.dp), color = colors.blueTint) {
                                Text(
                                    text = frame.highlightFormula,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.electricBlue,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                        Text("💡 ${frame.keyTakeaway}", style = MaterialTheme.typography.bodySmall, color = colors.emerald)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            OutlinedButton(
                                onClick = { if (activeFrameIdx > 0) activeFrameIdx-- },
                                enabled = activeFrameIdx > 0
                            ) { Text("← Khung trước") }
                            Button(
                                onClick = { if (activeFrameIdx < course.storyboardFrames.lastIndex) activeFrameIdx++ else activeFrameIdx = 0 }
                            ) { Text(if (activeFrameIdx < course.storyboardFrames.lastIndex) "Khung tiếp →" else "Xem lại từ đầu") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PresetChip(label: String, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = color, maxLines = 1)
        }
    }
}

@Composable
private fun SimMetric(label: String, value: String, color: Color, modifier: Modifier) {
    val colors = LocalAstraColors.current
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.14f)
    ) {
        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = color)
            Text(label, style = MaterialTheme.typography.bodySmall, fontSize = 10.sp, color = colors.textSecondary)
        }
    }
}

@Composable
private fun SimSlider(label: String, valueText: String, value: Float, range: ClosedFloatingPointRange<Float>, color: Color, onChange: (Float) -> Unit) {
    val colors = LocalAstraColors.current
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = colors.textPrimary)
            Text(valueText, fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = color)
        }
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            colors = SliderDefaults.colors(thumbColor = color, activeTrackColor = color)
        )
    }
}

// =========================================================================
// MODE 4: THẺ GHI NHỚ 3D FLIP FLASHCARDS (rotateY + SRS Rating)
// =========================================================================
@Composable
fun FlashcardsModeScreen(
    course: Course,
    onRateCard: (String, SrsRating) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val colors = LocalAstraColors.current
    val context = LocalContext.current
    val cards = course.flashcards
    var index by remember { mutableIntStateOf(0) }
    var flipped by remember(index) { mutableStateOf(false) }
    var showHint by remember(index) { mutableStateOf(false) }

    val rotation by animateFloatAsState(
        targetValue = if (flipped) 180f else 0f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 360f),
        label = "flashcard_flip"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        ModeTopHeader(
            title = "🗂️ Thẻ ghi nhớ 3D (Flashcards)",
            subtitle = if (cards.isNotEmpty()) "Thẻ ${index + 1} / ${cards.size} · Chạm để lật 3D" else "Chưa có thẻ",
            onBack = onBack
        )

        if (cards.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Hãy dùng nút [Tạo Flashcard] ở Gia sư AI để thêm thẻ!", color = colors.textSecondary)
            }
        } else {
            val card = cards[index % cards.size]
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(18.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                LinearProgressIndicator(
                    progress = { (index + 1).toFloat() / cards.size },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)),
                    color = colors.electricBlue,
                    trackColor = colors.cardElevated
                )

                // 3D Perspective Flip Card
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                        .graphicsLayer {
                            rotationY = rotation
                            cameraDistance = 14f * density
                        }
                        .clickable {
                            HapticHelper.tap(context)
                            flipped = !flipped
                        }
                        .testTag("flashcard_3d_surface"),
                    shape = RoundedCornerShape(24.dp),
                    color = if (rotation <= 90f) colors.card else colors.cardElevated,
                    border = BorderStroke(1.5.dp, if (rotation <= 90f) colors.electricBlue else colors.emerald)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(22.dp)
                            .graphicsLayer {
                                if (rotation > 90f) rotationY = 180f
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (rotation <= 90f) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Text("MẶT TRƯỚC · CÂU HỎI GỢI NHỚ", style = MaterialTheme.typography.labelSmall, color = colors.electricBlue)
                                Text(card.front, style = MaterialTheme.typography.headlineSmall, color = colors.textPrimary)
                                if (showHint) {
                                    Text("💡 Gợi ý: ${card.hint}", style = MaterialTheme.typography.bodySmall, color = colors.amber)
                                } else {
                                    TextButton(onClick = { showHint = true }) { Text("Xem gợi ý ngắn") }
                                }
                                Text("👆 Chạm vào thẻ để lật xem đáp án", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                            }
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text("MẶT SAU · CƠ CHẾ CHUẨN", style = MaterialTheme.typography.labelSmall, color = colors.emerald)
                                Text(card.back, style = MaterialTheme.typography.bodyLarge, color = colors.textPrimary)
                                Text(card.sourceCitation, style = MaterialTheme.typography.labelSmall, color = colors.electricBlue)
                            }
                        }
                    }
                }

                // 4 SRS Rating Buttons: Quên / Khó / Tốt / Dễ
                Column(
                    modifier = Modifier.navigationBarsPadding(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (flipped) "Đánh giá mức độ ghi nhớ của bạn:" else "Lật thẻ xong rồi chọn mức ghi nhớ bên dưới:",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            SrsRating.FORGOT to colors.crimson,
                            SrsRating.HARD to colors.amber,
                            SrsRating.GOOD to colors.electricBlue,
                            SrsRating.EASY to colors.emerald
                        ).forEach { (rating, accent) ->
                            Button(
                                onClick = {
                                    HapticHelper.tap(context)
                                    onRateCard(card.id, rating)
                                    index = (index + 1) % cards.size
                                },
                                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.White),
                                contentPadding = PaddingValues(horizontal = 4.dp)
                            ) {
                                Text(rating.label, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// MODE 5: KHOẢNG TRỐNG KIẾN THỨC (Knowledge Gaps Heatmap & Mini-Lesson)
// =========================================================================
@Composable
fun KnowledgeGapsScreen(
    course: Course,
    remediations: List<RemediationRecord>,
    settings: AiSettings,
    geminiUseCases: GeminiDomainUseCases,
    onResolveGap: (Long) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val colors = LocalAstraColors.current
    val scope = rememberCoroutineScope()
    var activeLessonText by remember { mutableStateOf<String?>(null) }
    var activeGapTitle by remember { mutableStateOf<String?>(null) }
    var loadingGapId by remember { mutableStateOf<Long?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        ModeTopHeader(
            title = "🫧 Khoảng trống kiến thức",
            subtitle = "Bản đồ nhiệt lỗ hổng từ nhật ký câu sai",
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (activeLessonText != null) {
                item {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = colors.blueTint,
                        border = BorderStroke(1.2.dp, colors.electricBlue)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("⚡ Bài vá lỗ hổng siêu tốc: ${activeGapTitle.orEmpty()}", style = MaterialTheme.typography.titleSmall, color = colors.electricBlue)
                            Text(activeLessonText.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary)
                        }
                    }
                }
            }

            if (remediations.isEmpty()) {
                item {
                    Surface(shape = RoundedCornerShape(18.dp), color = colors.card, border = BorderStroke(1.dp, colors.emerald)) {
                        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("✨ Chưa có lỗ hổng nào!", style = MaterialTheme.typography.titleMedium, color = colors.emerald)
                            Text("Mỗi câu trả lời sai trong Văn bản thông minh, Trắc nghiệm hoặc Đúng/Sai sẽ tự động hiện thành bản đồ nhiệt tại đây.", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                        }
                    }
                }
            } else {
                itemsIndexed(remediations) { _, gap ->
                    val heatColor = when {
                        gap.resolved -> colors.emerald
                        gap.wrongCount >= 3 -> colors.crimson
                        gap.wrongCount == 2 -> colors.amber
                        else -> colors.electricBlue
                    }
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = colors.card,
                        border = BorderStroke(1.2.dp, heatColor.copy(alpha = 0.6f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(gap.conceptTag, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary, modifier = Modifier.weight(1f))
                                Surface(shape = RoundedCornerShape(50), color = heatColor.copy(alpha = 0.16f)) {
                                    Text(
                                        text = if (gap.resolved) "✓ Đã vá" else "Sai ${gap.wrongCount} lần",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = heatColor,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                            Text(gap.mistakeSummary, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        loadingGapId = gap.id
                                        activeGapTitle = gap.conceptTag
                                        scope.launch {
                                            if (settings.apiKey.isNotBlank()) {
                                                val res = geminiUseCases.generateGapFixMiniLesson(settings, course.title, gap)
                                                activeLessonText = res.getOrElse {
                                                    "Bản chất cốt lõi: ${gap.mistakeSummary}\nMẹo phản xạ 3 giây: Đọc kỹ tiêu chí phân loại trong đề trước khi chọn đáp án!"
                                                }
                                            } else {
                                                activeLessonText = "1) Bản chất: ${gap.mistakeSummary}\n2) Lưu ý phòng thi: So sánh kỹ mẫu số/tiêu chí phân chia để tránh sập bẫy hoán đổi khái niệm.\n(Gắn Gemini API Key trong Cài đặt AI để sinh bài vá cá nhân hóa sâu hơn!)"
                                            }
                                            loadingGapId = null
                                        }
                                    },
                                    enabled = loadingGapId == null,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    if (loadingGapId == gap.id) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                                    } else {
                                        Text("Vá lỗ hổng bằng AI", fontSize = 12.sp)
                                    }
                                }
                                if (!gap.resolved) {
                                    OutlinedButton(
                                        onClick = { onResolveGap(gap.id) },
                                        shape = RoundedCornerShape(12.dp)
                                    ) { Text("Đã nắm vững") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ModeTopHeader(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    trailing: @Composable (() -> Unit)? = null
) {
    val colors = LocalAstraColors.current
    Surface(color = colors.card, border = BorderStroke(1.dp, colors.border), modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("mode_back_button")) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Quay lại", tint = colors.textPrimary)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
            }
            trailing?.invoke()
        }
    }
}
