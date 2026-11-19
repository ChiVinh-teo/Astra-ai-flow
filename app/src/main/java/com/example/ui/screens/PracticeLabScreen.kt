package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.service.HapticHelper
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalAstraColors

@Composable
fun PracticeLabScreen(
    currentCourse: Course,
    duelHistory: List<DuelHistoryRecord>,
    onOpenMode: (LearningMode) -> Unit,
    onOpenDuelModal: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAstraColors.current
    val context = LocalContext.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("Luyện tập & Lab tương tác", style = MaterialTheme.typography.headlineSmall, color = colors.textPrimary)
                Text("11 chế độ chuẩn Astra AI · ${currentCourse.title}", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
            }
        }

        // Featured Interactive Lab + Friend Duel Banner Row
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            HapticHelper.tap(context)
                            onOpenMode(LearningMode.VIDEO_SIMULATOR)
                        }
                        .testTag("lab_surplus_shortcut"),
                    shape = RoundedCornerShape(18.dp),
                    color = colors.card,
                    border = BorderStroke(1.dp, colors.electricBlue.copy(alpha = 0.6f))
                ) {
                    Column(
                        modifier = Modifier
                            .background(Brush.linearGradient(listOf(colors.blueTint, Color.Transparent)))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Rounded.Tune, contentDescription = null, tint = colors.electricBlue)
                        Text("Lab Giá trị thặng dư", style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                        Text("Kéo thanh trượt c, v, t, t' → xem m', p', c/v trực tiếp", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                    }
                }

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            HapticHelper.tap(context)
                            onOpenDuelModal()
                        }
                        .testTag("lab_duel_shortcut"),
                    shape = RoundedCornerShape(18.dp),
                    color = colors.card,
                    border = BorderStroke(1.dp, colors.violet.copy(alpha = 0.6f))
                ) {
                    Column(
                        modifier = Modifier
                            .background(Brush.linearGradient(listOf(colors.violetTint, Color.Transparent)))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Rounded.SportsEsports, contentDescription = null, tint = colors.violet)
                        Text("⚔️ Đấu tay đôi", style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                        Text("Tạo mã thử thách 5 câu tốc độ & so điểm với bạn bè", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                    }
                }
            }
        }

        // All 11 Modes grouped into HỌC · LUYỆN TẬP · THI
        LearningModeGroup.entries.forEach { group ->
            val groupModes = LearningMode.entries.filter { it.group == group }
            item {
                Text(
                    text = when (group) {
                        LearningModeGroup.ACQUIRE -> "NHÓM 1 — HỌC (Acquire)"
                        LearningModeGroup.PRACTICE -> "NHÓM 2 — LUYỆN TẬP (Practice)"
                        LearningModeGroup.ASSESS -> "NHÓM 3 — THI THỬ (Assess)"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = when (group) {
                        LearningModeGroup.ACQUIRE -> colors.electricBlue
                        LearningModeGroup.PRACTICE -> colors.emerald
                        LearningModeGroup.ASSESS -> colors.violet
                    }
                )
            }
            items(groupModes) { mode ->
                ModeCardRow(mode = mode) {
                    HapticHelper.tap(context)
                    onOpenMode(mode)
                }
            }
        }

        if (duelHistory.isNotEmpty()) {
            item {
                Text("🏆 Lịch sử Đấu tay đôi gần đây", style = MaterialTheme.typography.titleSmall, color = colors.amber)
            }
            items(duelHistory.take(5)) { record ->
                val won = record.myScore > record.opponentScore || (record.myScore == record.opponentScore && record.myTimeSec <= record.opponentTimeSec)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = colors.card,
                    border = BorderStroke(1.dp, if (won) colors.emerald.copy(alpha = 0.5f) else colors.border)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("${record.code} · vs ${record.opponentName}", style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                            Text(record.courseTitle, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                        }
                        Text(
                            text = "${record.myScore}/5 (${record.myTimeSec}s) vs ${record.opponentScore}/5",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (won) colors.emerald else colors.amber
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ModeCardRow(
    mode: LearningMode,
    onClick: () -> Unit
) {
    val colors = LocalAstraColors.current
    val accent = when (mode.group) {
        LearningModeGroup.ACQUIRE -> colors.electricBlue
        LearningModeGroup.PRACTICE -> colors.emerald
        LearningModeGroup.ASSESS -> colors.violet
    }
    val icon = when (mode) {
        LearningMode.SMART_TEXT -> Icons.Rounded.AutoStories
        LearningMode.PODCAST -> Icons.Rounded.Podcasts
        LearningMode.VIDEO_SIMULATOR -> Icons.Rounded.PlayCircle
        LearningMode.FLASHCARDS -> Icons.Rounded.Style
        LearningMode.KNOWLEDGE_GAPS -> Icons.Rounded.BubbleChart
        LearningMode.EXERCISES -> Icons.Rounded.Calculate
        LearningMode.ADAPTIVE_QUIZ -> Icons.Rounded.FactCheck
        LearningMode.TRUE_FALSE_BLITZ -> Icons.Rounded.Swipe
        LearningMode.SPACED_REPETITION -> Icons.Rounded.Autorenew
        LearningMode.WRITTEN_EXAM -> Icons.Rounded.Timer
        LearningMode.ORAL_VIVA -> Icons.Rounded.RecordVoiceOver
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("mode_card_${mode.name.lowercase()}"),
        shape = RoundedCornerShape(16.dp),
        color = colors.card,
        border = BorderStroke(1.dp, colors.border)
    ) {
        Row(
            modifier = Modifier.padding(13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = accent.copy(alpha = 0.16f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = accent)
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(mode.title, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                    if (mode.badge != null) {
                        Surface(shape = RoundedCornerShape(50), color = accent) {
                            Text(
                                text = mode.badge,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(mode.subtitle, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
            }
            Icon(Icons.Rounded.ChevronRight, contentDescription = "Mở chế độ", tint = colors.textSecondary)
        }
    }
}
