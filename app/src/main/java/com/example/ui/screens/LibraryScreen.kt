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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.*
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalAstraColors

@Composable
fun LibraryScreen(
    courses: List<Course>,
    currentCourse: Course,
    remediations: List<RemediationRecord>,
    settings: AiSettings,
    onSelectCourse: (String) -> Unit,
    onOpenIngest: () -> Unit,
    onOpenShare: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenKnowledgeGaps: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAstraColors.current
    val unresolvedGaps = remediations.filter { !it.resolved }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Kho tài liệu & Thống kê", style = MaterialTheme.typography.headlineSmall, color = colors.textPrimary)
                    Text("Lưu trữ cục bộ trên máy · Xuất/Nhập JSON 1 chạm", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                }
                IconButton(onClick = onOpenSettings, modifier = Modifier.testTag("library_settings_button")) {
                    Icon(Icons.Rounded.Settings, contentDescription = "Cài đặt AI", tint = colors.electricBlue)
                }
            }
        }

        // Streak & AI Status Summary Card
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    color = colors.amberTint,
                    border = BorderStroke(1.dp, colors.amber.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("🔥 Chuỗi ngày học", style = MaterialTheme.typography.labelMedium, color = colors.amber)
                        Text("${settings.streakDays} ngày liên tiếp", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                        Text("Tổng sao: ${courses.sumOf { it.starsEarned }} ⭐", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                    }
                }
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onOpenSettings),
                    shape = RoundedCornerShape(16.dp),
                    color = if (settings.apiKey.isNotBlank()) colors.emeraldTint else colors.blueTint,
                    border = BorderStroke(1.dp, if (settings.apiKey.isNotBlank()) colors.emerald.copy(alpha = 0.5f) else colors.electricBlue.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("🤖 Gemini API", style = MaterialTheme.typography.labelMedium, color = if (settings.apiKey.isNotBlank()) colors.emerald else colors.electricBlue)
                        Text(
                            text = if (settings.apiKey.isNotBlank()) "Đã gắn API Key" else "Chưa gắn Key",
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textPrimary
                        )
                        Text(settings.fastModelId, fontFamily = JetBrainsMonoFontFamily, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                    }
                }
            }
        }

        // Primary Actions: Ingest New Document + JSON Share
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onOpenIngest,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("library_ingest_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Rounded.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Nạp tài liệu mới", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onOpenShare,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("library_share_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Chia sẻ JSON")
                }
            }
        }

        // Knowledge Gaps Heatmap Preview
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenKnowledgeGaps)
                    .testTag("library_gaps_card"),
                shape = RoundedCornerShape(18.dp),
                color = colors.card,
                border = BorderStroke(1.dp, colors.crimson.copy(alpha = 0.45f))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🫧 Bản đồ nhiệt Khoảng trống kiến thức", style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                        Text("${unresolvedGaps.size} lỗ hổng", style = MaterialTheme.typography.labelSmall, color = colors.crimson)
                    }
                    if (unresolvedGaps.isEmpty()) {
                        Text("Tuyệt vời! Chưa ghi nhận lỗ hổng kiến thức nào chưa được vá.", style = MaterialTheme.typography.bodySmall, color = colors.emerald)
                    } else {
                        unresolvedGaps.take(3).forEach { gap ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("• ${gap.conceptTag}", style = MaterialTheme.typography.bodySmall, color = colors.textPrimary, modifier = Modifier.weight(1f))
                                Surface(shape = RoundedCornerShape(50), color = colors.crimsonTint) {
                                    Text(
                                        text = "Sai ${gap.wrongCount} lần",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.crimson,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Text("Chạm để mở Bản đồ nhiệt & tạo bài vá lỗ hổng bằng AI →", style = MaterialTheme.typography.labelMedium, color = colors.electricBlue)
                    }
                }
            }
        }

        // Course List
        item {
            Text("📚 Khóa học trên thiết bị (${courses.size})", style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
        }

        items(courses, key = { it.id }) { course ->
            val selected = course.id == currentCourse.id
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectCourse(course.id) },
                shape = RoundedCornerShape(16.dp),
                color = colors.card,
                border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) colors.electricBlue else colors.border)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = if (course.isKtctFlagship) Icons.Rounded.School else Icons.Rounded.Description,
                        contentDescription = null,
                        tint = if (selected) colors.electricBlue else colors.textSecondary
                    )
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(course.title, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                        Text(course.sourceBadge, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                        Text(
                            text = "${course.nodes.size} trạm · Sẵn sàng ${course.readinessPercent}% · Mục tiêu ${String.format("%.1f", course.targetScore)}/10",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.emerald
                        )
                    }
                    if (selected) {
                        Surface(shape = RoundedCornerShape(50), color = colors.electricBlue) {
                            Text("Đang học", style = MaterialTheme.typography.labelSmall, color = androidx.compose.ui.graphics.Color.White, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                        }
                    }
                }
            }
        }
    }
}
