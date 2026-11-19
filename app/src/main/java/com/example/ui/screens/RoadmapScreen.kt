package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.service.HapticHelper
import com.example.ui.components.CircularReadinessRing
import com.example.ui.components.ReadinessTrajectoryChart
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalAstraColors
import com.example.viewmodel.StudyCalendarDay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoadmapScreen(
    courses: List<Course>,
    currentCourse: Course,
    settings: AiSettings,
    dailyGoals: DailyGoalState,
    calendarDays: List<StudyCalendarDay>,
    onSelectCourse: (String) -> Unit,
    onToggleTheme: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenIngest: () -> Unit,
    onOpenDuel: () -> Unit,
    onOpenShare: () -> Unit,
    onOpenModeSheet: () -> Unit,
    onUpdateExamPlan: (Int, Double) -> Unit,
    onOpenNode: (LearningNode) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAstraColors.current
    val context = LocalContext.current
    var showCourseMenu by remember { mutableStateOf(false) }
    var showExamPlanDialog by remember { mutableStateOf(false) }

    val nextNodeIndex = remember(currentCourse.nodes) {
        currentCourse.nodes.indexOfFirst { !it.isCompleted }.takeIf { it >= 0 } ?: 0
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Sticky Top Utility Bar
        item {
            Surface(
                color = colors.card.copy(alpha = 0.94f),
                border = BorderStroke(1.dp, colors.border.copy(alpha = 0.7f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = colors.cardElevated,
                            border = BorderStroke(1.dp, colors.border),
                            modifier = Modifier
                                .clickable { showCourseMenu = true }
                                .testTag("course_selector_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = colors.electricBlue, modifier = Modifier.size(16.dp))
                                Text(
                                    text = currentCourse.title,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = colors.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Đổi môn học", tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                            }
                        }
                        DropdownMenu(expanded = showCourseMenu, onDismissRequest = { showCourseMenu = false }) {
                            courses.forEach { course ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "${if (course.id == currentCourse.id) "✓ " else ""}${course.title}",
                                            fontWeight = if (course.id == currentCourse.id) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        showCourseMenu = false
                                        onSelectCourse(course.id)
                                    }
                                )
                            }
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("＋ Nạp tài liệu tạo khóa học mới", color = colors.electricBlue, fontWeight = FontWeight.SemiBold) },
                                onClick = {
                                    showCourseMenu = false
                                    onOpenIngest()
                                }
                            )
                        }
                    }

                    Spacer(Modifier.width(8.dp))

                    // Streak Badge
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = colors.amberTint,
                        border = BorderStroke(1.dp, colors.amber.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "🔥 ${settings.streakDays}",
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.amber,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                        )
                    }

                    Spacer(Modifier.width(6.dp))

                    // 1-tap Theme Toggle (Cosmic Dark <-> White-Paper Exam)
                    IconButton(
                        onClick = {
                            HapticHelper.tap(context)
                            onToggleTheme()
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(colors.cardElevated)
                            .testTag("theme_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (settings.isWhitePaperExam) Icons.Rounded.DarkMode else Icons.Rounded.Description,
                            contentDescription = "Chuyển giao diện Cosmic Dark / Giấy thi trắng",
                            tint = if (settings.isWhitePaperExam) colors.violet else colors.amber,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(Modifier.width(4.dp))

                    IconButton(
                        onClick = {
                            HapticHelper.tap(context)
                            onOpenSettings()
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(colors.cardElevated)
                            .testTag("ai_settings_top_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Tune,
                            contentDescription = "Cài đặt AI",
                            tint = if (settings.apiKey.isNotBlank()) colors.emerald else colors.electricBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // 2. Exam Prep Course Header Card
        item {
            Surface(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = colors.card,
                border = BorderStroke(1.dp, colors.border)
            ) {
                Column(
                    modifier = Modifier
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    colors.electricBlue.copy(alpha = if (colors.isDark) 0.16f else 0.08f),
                                    colors.violet.copy(alpha = if (colors.isDark) 0.10f else 0.04f),
                                    Color.Transparent
                                )
                            )
                        )
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text(
                                text = currentCourse.categoryTag,
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.electricBlue
                            )
                            Text(
                                text = currentCourse.title,
                                style = MaterialTheme.typography.headlineSmall,
                                color = colors.textPrimary
                            )
                            Text(
                                text = currentCourse.sourceBadge,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        CircularReadinessRing(percent = currentCourse.readinessPercent, size = 84.dp)
                    }

                    // Editable Exam Countdown + Target Score + Stars Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = colors.cardElevated,
                            border = BorderStroke(1.dp, colors.border),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { showExamPlanDialog = true }
                                .testTag("edit_exam_plan_chip")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Rounded.Event, contentDescription = null, tint = colors.amber, modifier = Modifier.size(16.dp))
                                Text(
                                    text = "Còn ${currentCourse.examDaysLeft} ngày",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = colors.textPrimary
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = colors.cardElevated,
                            border = BorderStroke(1.dp, colors.border),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { showExamPlanDialog = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Rounded.Flag, contentDescription = null, tint = colors.emerald, modifier = Modifier.size(16.dp))
                                Text(
                                    text = "Mục tiêu ${String.format("%.1f", currentCourse.targetScore)}/10",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = colors.textPrimary
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = colors.amberTint,
                            border = BorderStroke(1.dp, colors.amber.copy(alpha = 0.45f))
                        ) {
                            Text(
                                text = "⭐ ${currentCourse.starsEarned}",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = colors.amber,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                            )
                        }
                    }

                    // 4 Quick Action Buttons under Header Card
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuickActionPill("＋ Bài học", colors.electricBlue, Modifier.weight(1f).testTag("quick_add_course")) {
                            HapticHelper.tap(context)
                            onOpenIngest()
                        }
                        QuickActionPill("⚔️ Đấu đôi", colors.violet, Modifier.weight(1f).testTag("quick_duel")) {
                            HapticHelper.tap(context)
                            onOpenDuel()
                        }
                        QuickActionPill("↗ Chia sẻ", colors.emerald, Modifier.weight(1f).testTag("quick_share")) {
                            HapticHelper.tap(context)
                            onOpenShare()
                        }
                        QuickActionPill("🧩 11 Dạng", colors.amber, Modifier.weight(1f).testTag("quick_11_modes")) {
                            HapticHelper.tap(context)
                            onOpenModeSheet()
                        }
                    }
                }
            }
        }

        // 3. Readiness Trajectory Chart
        item {
            ReadinessTrajectoryChart(
                currentReadiness = currentCourse.readinessPercent,
                targetScore = currentCourse.targetScore,
                examDaysLeft = currentCourse.examDaysLeft,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // 4. Today's Goals + Study Calendar Strip
        item {
            TodayGoalsAndCalendarCard(
                goals = dailyGoals,
                calendarDays = calendarDays,
                streakDays = settings.streakDays,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // 5. Winding S-Curve Learning Path Header + Button to open 11-Mode Sheet
        item {
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Lộ trình uốn lượn (${currentCourse.nodes.size} trạm)",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "Chạm vào bất kỳ trạm nào để bắt đầu học ngay",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                }
                FilledTonalButton(
                    onClick = {
                        HapticHelper.tap(context)
                        onOpenModeSheet()
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("open_modes_sheet_button")
                ) {
                    Text("11 Dạng bài", style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        // 6. Signature Animated S-Curve Winding Path
        item {
            WindingSCurvePath(
                nodes = currentCourse.nodes,
                nextNodeIndex = nextNodeIndex,
                onNodeClick = { node ->
                    HapticHelper.tap(context)
                    onOpenNode(node)
                },
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
            )
        }
    }

    if (showExamPlanDialog) {
        EditExamPlanDialog(
            initialDays = currentCourse.examDaysLeft,
            initialTarget = currentCourse.targetScore,
            onDismiss = { showExamPlanDialog = false },
            onSave = { days, score ->
                showExamPlanDialog = false
                onUpdateExamPlan(days, score)
            }
        )
    }
}

@Composable
private fun QuickActionPill(
    label: String,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = LocalAstraColors.current
    Surface(
        modifier = modifier
            .heightIn(min = 42.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = accent.copy(alpha = if (colors.isDark) 0.16f else 0.10f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.4f))
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontSize = 11.5.sp,
                color = accent,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun TodayGoalsAndCalendarCard(
    goals: DailyGoalState,
    calendarDays: List<StudyCalendarDay>,
    streakDays: Int,
    modifier: Modifier = Modifier
) {
    val colors = LocalAstraColors.current
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = colors.card,
        border = BorderStroke(1.dp, colors.border)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🎯 Mục tiêu hôm nay & Lịch học", style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                Text("Chuỗi $streakDays ngày 🔥", style = MaterialTheme.typography.labelSmall, color = colors.amber)
            }

            GoalRow("Hoàn thành 1 bài Văn bản thông minh", goals.smartTextCompletedToday, goals.smartTextTarget, colors.electricBlue)
            GoalRow("Ôn 5 thẻ ghi nhớ 3D (Flashcards)", goals.flashcardsReviewedToday, goals.flashcardsTarget, colors.emerald)

            if (calendarDays.isNotEmpty()) {
                HorizontalDivider(color = colors.border.copy(alpha = 0.6f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    calendarDays.forEach { day ->
                        val active = day.completedCount > 0
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            active -> colors.emerald
                                            day.isToday -> colors.blueTint
                                            else -> colors.cardElevated
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (active) {
                                    Icon(Icons.Rounded.Check, contentDescription = "Đã học", tint = Color.White, modifier = Modifier.size(16.dp))
                                } else {
                                    Text(
                                        text = if (day.isToday) "•" else "",
                                        color = colors.electricBlue,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Text(
                                text = day.label,
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 10.sp,
                                color = if (day.isToday) colors.electricBlue else colors.textSecondary,
                                fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalRow(label: String, current: Int, target: Int, color: Color) {
    val colors = LocalAstraColors.current
    val done = current >= target
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = if (done) "✅ $label" else label,
                style = MaterialTheme.typography.bodySmall,
                color = if (done) colors.emerald else colors.textPrimary
            )
            Text(
                text = "${current.coerceAtMost(target)}/$target",
                style = MaterialTheme.typography.labelSmall,
                color = if (done) colors.emerald else color
            )
        }
        LinearProgressIndicator(
            progress = { (current.toFloat() / target.coerceAtLeast(1)).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(50)),
            color = if (done) colors.emerald else color,
            trackColor = colors.cardElevated
        )
    }
}

@Composable
private fun WindingSCurvePath(
    nodes: List<LearningNode>,
    nextNodeIndex: Int,
    onNodeClick: (LearningNode) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAstraColors.current
    val infiniteTransition = rememberInfiniteTransition(label = "halo_pulse")
    val haloScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.24f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "halo_scale"
    )
    val dashPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 36f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing)),
        label = "dash_phase"
    )

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(0.dp)) {
        nodes.forEachIndexed { index, node ->
            val alignRight = index % 2 == 1
            val isNext = index == nextNodeIndex
            val accent = when {
                node.isCompleted -> colors.emerald
                isNext -> colors.electricBlue
                node.primaryMode == LearningMode.WRITTEN_EXAM -> colors.violet
                else -> colors.border
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = if (alignRight) Arrangement.End else Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(0.86f)
                        .clickable { onNodeClick(node) }
                        .testTag("winding_node_${node.orderIndex}"),
                    shape = RoundedCornerShape(20.dp),
                    color = colors.card,
                    border = BorderStroke(
                        width = if (isNext) 2.dp else 1.dp,
                        color = if (isNext) colors.electricBlue else accent
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(54.dp)) {
                            if (isNext) {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .scale(haloScale)
                                        .clip(CircleShape)
                                        .background(colors.electricBlue.copy(alpha = 0.24f))
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            node.isCompleted -> colors.emerald
                                            isNext -> colors.electricBlue
                                            node.primaryMode == LearningMode.WRITTEN_EXAM -> colors.violet
                                            else -> colors.cardElevated
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (node.isCompleted) {
                                    Icon(Icons.Rounded.Check, contentDescription = "Hoàn thành", tint = Color.White)
                                } else {
                                    Text(
                                        text = "${node.orderIndex}",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = if (isNext || node.primaryMode == LearningMode.WRITTEN_EXAM) Color.White else colors.textPrimary
                                    )
                                }
                            }
                        }

                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "${node.chapterLabel} · ${node.primaryMode.title}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (node.primaryMode == LearningMode.WRITTEN_EXAM) colors.violet else colors.electricBlue
                                )
                                if (isNext) {
                                    Surface(shape = RoundedCornerShape(50), color = colors.electricBlue) {
                                        Text(
                                            text = "TIẾP THEO",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 9.sp,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = node.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = colors.textPrimary
                            )
                            Text(
                                text = node.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (node.masteryPercent > 0) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    LinearProgressIndicator(
                                        progress = { node.masteryPercent / 100f },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(5.dp)
                                            .clip(RoundedCornerShape(50)),
                                        color = colors.emerald,
                                        trackColor = colors.cardElevated
                                    )
                                    Text(
                                        text = "${node.masteryPercent}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.emerald
                                    )
                                }
                            }
                        }
                        Icon(
                            imageVector = Icons.Rounded.ChevronRight,
                            contentDescription = "Mở trạm",
                            tint = colors.textSecondary
                        )
                    }
                }
            }

            // Animated S-Curve dashed connector to next node
            if (index < nodes.lastIndex) {
                val segmentCompleted = node.isCompleted
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                ) {
                    val startX = if (alignRight) size.width * 0.72f else size.width * 0.28f
                    val endX = if (alignRight) size.width * 0.28f else size.width * 0.72f
                    val path = Path().apply {
                        moveTo(startX, 0f)
                        cubicTo(startX, size.height * 0.65f, endX, size.height * 0.35f, endX, size.height)
                    }
                    if (segmentCompleted) {
                        drawPath(
                            path = path,
                            color = colors.emerald.copy(alpha = 0.35f),
                            style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                    drawPath(
                        path = path,
                        color = if (segmentCompleted) colors.emerald else colors.electricBlue.copy(alpha = 0.65f),
                        style = Stroke(
                            width = 3.dp.toPx(),
                            cap = StrokeCap.Round,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), dashPhase)
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun EditExamPlanDialog(
    initialDays: Int,
    initialTarget: Double,
    onDismiss: () -> Unit,
    onSave: (Int, Double) -> Unit
) {
    var daysText by remember { mutableStateOf(initialDays.toString()) }
    var targetSlider by remember { mutableFloatStateOf(initialTarget.toFloat()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Lịch thi & Điểm mục tiêu") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = daysText,
                    onValueChange = { daysText = it.filter(Char::isDigit).take(3) },
                    label = { Text("Số ngày còn lại tới kỳ thi") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Điểm mục tiêu: ${String.format("%.1f", targetSlider)} / 10",
                    style = MaterialTheme.typography.titleSmall
                )
                Slider(
                    value = targetSlider,
                    onValueChange = { targetSlider = (it * 2).toInt() / 2f },
                    valueRange = 5.5f..10.0f,
                    steps = 8
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(daysText.toIntOrNull()?.coerceIn(1, 365) ?: 14, targetSlider.toDouble()) }) {
                Text("Lưu mục tiêu")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy") }
        }
    )
}
