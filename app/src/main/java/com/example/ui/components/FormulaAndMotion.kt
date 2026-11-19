package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.FormulaBlock
import com.example.model.StrikeThroughTf
import com.example.service.HapticHelper
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalAstraColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/**
 * Clean native mathematical formula box with stacked fractions, superscripts & subscripts.
 * Zero raw LaTeX underscores or carets ever rendered.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MathFormulaBox(
    formula: FormulaBlock,
    modifier: Modifier = Modifier
) {
    val colors = LocalAstraColors.current
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = colors.blueTint,
        border = BorderStroke(1.dp, colors.electricBlue.copy(alpha = 0.45f))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "📐 ${formula.title}",
                style = MaterialTheme.typography.labelLarge,
                color = colors.electricBlue
            )
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = colors.card,
                border = BorderStroke(1.dp, colors.border)
            ) {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    formula.terms.forEach { term ->
                        if (term.numerator != null && term.denominator != null) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            ) {
                                Text(
                                    text = term.numerator,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = colors.textPrimary
                                )
                                HorizontalDivider(
                                    modifier = Modifier
                                        .widthIn(min = 26.dp, max = 140.dp)
                                        .padding(vertical = 2.dp),
                                    thickness = 1.5.dp,
                                    color = colors.electricBlue
                                )
                                Text(
                                    text = term.denominator,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = colors.textPrimary
                                )
                            }
                        } else {
                            val styled = buildAnnotatedString {
                                append(term.text)
                                term.superscript?.let {
                                    withStyle(SpanStyle(fontSize = 11.sp, baselineShift = BaselineShift.Superscript)) {
                                        append(it)
                                    }
                                }
                                term.subscript?.let {
                                    withStyle(SpanStyle(fontSize = 11.sp, baselineShift = BaselineShift.Subscript)) {
                                        append(it)
                                    }
                                }
                            }
                            Text(
                                text = styled,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = colors.textPrimary,
                                modifier = Modifier.align(Alignment.CenterVertically)
                            )
                        }
                    }
                }
            }
            if (formula.legend.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    formula.legend.forEach { line ->
                        Text(
                            text = "• $line",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary
                        )
                    }
                }
            }
        }
    }
}

/**
 * Animated Circular Readiness Ring with smoothly counting percentage odometer.
 */
@Composable
fun CircularReadinessRing(
    percent: Int,
    size: Dp = 86.dp,
    modifier: Modifier = Modifier
) {
    val colors = LocalAstraColors.current
    val animatedProgress by animateFloatAsState(
        targetValue = (percent.coerceIn(0, 100)) / 100f,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "readiness_arc"
    )
    val animatedPercent = (animatedProgress * 100f).roundToInt()

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = 8.dp.toPx()
            val inset = strokePx / 2f
            val arcSize = Size(this.size.width - strokePx, this.size.height - strokePx)
            drawArc(
                color = colors.border.copy(alpha = 0.65f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
            drawArc(
                brush = Brush.sweepGradient(
                    listOf(colors.electricBlue, colors.violet, colors.emerald, colors.electricBlue)
                ),
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$animatedPercent%",
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = colors.textPrimary
            )
            Text(
                text = "Sẵn sàng",
                style = MaterialTheme.typography.bodySmall,
                fontSize = 10.sp,
                color = colors.textSecondary
            )
        }
    }
}

/**
 * Readiness Trajectory Canvas: projects readiness curve from Today to Exam Date vs Target Score line.
 */
@Composable
fun ReadinessTrajectoryChart(
    currentReadiness: Int,
    targetScore: Double,
    examDaysLeft: Int,
    modifier: Modifier = Modifier
) {
    val colors = LocalAstraColors.current
    val targetPercent = (targetScore * 10.0).toFloat().coerceIn(55f, 100f)
    val startVal = currentReadiness.toFloat().coerceIn(5f, 100f)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = colors.card,
        border = BorderStroke(1.dp, colors.border)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📈 Dự báo độ sẵn sàng tới ngày thi",
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.textPrimary
                )
                Surface(
                    shape = RoundedCornerShape(50),
                    color = colors.emeraldTint
                ) {
                    Text(
                        text = "Mục tiêu ${String.format("%.1f", targetScore)}/10",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.emerald,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(106.dp)
            ) {
                val w = size.width
                val h = size.height
                val padX = 12.dp.toPx()
                val padY = 12.dp.toPx()
                val plotW = w - padX * 2
                val plotH = h - padY * 2

                // Subtle grid lines
                listOf(0.25f, 0.5f, 0.75f).forEach { frac ->
                    val y = padY + plotH * frac
                    drawLine(
                        color = colors.border.copy(alpha = 0.4f),
                        start = Offset(padX, y),
                        end = Offset(w - padX, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // Target dashed line
                val targetY = padY + plotH * (1f - targetPercent / 100f)
                drawLine(
                    color = colors.emerald.copy(alpha = 0.85f),
                    start = Offset(padX, targetY),
                    end = Offset(w - padX, targetY),
                    strokeWidth = 1.8.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                )

                // Projected smooth trajectory curve
                val startY = padY + plotH * (1f - startVal / 100f)
                val endVal = maxOf(targetPercent, startVal + 20f).coerceAtMost(98f)
                val endY = padY + plotH * (1f - endVal / 100f)

                val curvePath = Path().apply {
                    moveTo(padX, startY)
                    cubicTo(
                        padX + plotW * 0.35f, startY - plotH * 0.12f,
                        padX + plotW * 0.68f, endY + plotH * 0.08f,
                        w - padX, endY
                    )
                }

                val fillPath = Path().apply {
                    addPath(curvePath)
                    lineTo(w - padX, h - padY)
                    lineTo(padX, h - padY)
                    close()
                }
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        listOf(colors.electricBlue.copy(alpha = 0.32f), Color.Transparent)
                    )
                )
                drawPath(
                    path = curvePath,
                    brush = Brush.horizontalGradient(listOf(colors.electricBlue, colors.violet, colors.emerald)),
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )

                // Today & Exam Day dots
                drawCircle(color = colors.electricBlue, radius = 5.dp.toPx(), center = Offset(padX, startY))
                drawCircle(color = Color.White, radius = 2.5.dp.toPx(), center = Offset(padX, startY))
                drawCircle(color = colors.emerald, radius = 5.dp.toPx(), center = Offset(w - padX, endY))
                drawCircle(color = Color.White, radius = 2.5.dp.toPx(), center = Offset(w - padX, endY))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Hôm nay ($currentReadiness%)", style = MaterialTheme.typography.labelSmall, color = colors.electricBlue)
                Text("Giữa chặng (+${examDaysLeft / 2}d)", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                Text("Ngày thi (-${examDaysLeft}d)", style = MaterialTheme.typography.labelSmall, color = colors.emerald)
            }
        }
    }
}

enum class MascotMood { IDLE, CELEBRATE, SHAKE }

/**
 * Expressive Astra Buddy Mascot with spring bounce + sparkle particles on correct,
 * horizontal spring shake on wrong!
 */
@Composable
fun AstraBuddyMascot(
    mood: MascotMood,
    size: Dp = 56.dp,
    modifier: Modifier = Modifier
) {
    val colors = LocalAstraColors.current
    val scale = remember { Animatable(1f) }
    val shakeX = remember { Animatable(0f) }
    val sparkleProgress = remember { Animatable(0f) }

    LaunchedEffect(mood) {
        when (mood) {
            MascotMood.CELEBRATE -> {
                sparkleProgress.snapTo(0f)
                scale.animateTo(1.22f, spring(dampingRatio = 0.35f, stiffness = 450f))
                scale.animateTo(1f, spring(dampingRatio = 0.5f))
                sparkleProgress.animateTo(1f, tween(650))
            }
            MascotMood.SHAKE -> {
                for (dx in listOf(-12f, 12f, -8f, 8f, -4f, 0f)) {
                    shakeX.animateTo(dx, tween(45))
                }
            }
            MascotMood.IDLE -> {
                scale.snapTo(1f)
                shakeX.snapTo(0f)
            }
        }
    }

    Box(modifier = modifier.size(size + 14.dp), contentAlignment = Alignment.Center) {
        if (mood == MascotMood.CELEBRATE && sparkleProgress.value in 0.01f..0.99f) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val p = sparkleProgress.value
                val radius = (this.size.minDimension / 2f) * (0.65f + p * 0.45f)
                val center = Offset(this.size.width / 2f, this.size.height / 2f)
                for (i in 0 until 8) {
                    val angle = i * (PI / 4.0)
                    val pt = Offset(
                        center.x + (cos(angle) * radius).toFloat(),
                        center.y + (sin(angle) * radius).toFloat()
                    )
                    drawCircle(
                        color = if (i % 2 == 0) colors.emerald else colors.amber,
                        radius = (4.dp.toPx() * (1f - p)).coerceAtLeast(1f),
                        center = pt
                    )
                }
            }
        }
        Image(
            painter = painterResource(id = R.drawable.img_astra_buddy_1795110821331),
            contentDescription = "Linh vật Astra Buddy",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(size)
                .offset { IntOffset(shakeX.value.roundToInt(), 0) }
                .scale(scale.value)
                .clip(CircleShape)
                .border(
                    width = 2.dp,
                    color = when (mood) {
                        MascotMood.CELEBRATE -> colors.emerald
                        MascotMood.SHAKE -> colors.crimson
                        MascotMood.IDLE -> colors.electricBlue.copy(alpha = 0.7f)
                    },
                    shape = CircleShape
                )
        )
    }
}

/**
 * Interactive Strike-Through True/False Card:
 * - Drag/swipe left = Sai, right = Đúng OR tap buttons.
 * - When isTrue == false: draws an animated crimson strike-through Canvas line left-to-right over wrongClause,
 *   then slides/fades emerald correctClause right beside it.
 * - When isTrue == true: displays emerald confirmation highlight keeping the statement intact!
 */
@Composable
fun StrikeThroughTrueFalseCard(
    item: StrikeThroughTf,
    answeredChoice: Boolean?,
    onAnswer: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAstraColors.current
    val context = LocalContext.current
    var dragOffsetX by remember(item.id) { mutableFloatStateOf(0f) }
    val strikeProgress = remember(item.id) { Animatable(0f) }

    LaunchedEffect(answeredChoice, item.id) {
        if (answeredChoice != null && !item.isTrue) {
            strikeProgress.snapTo(0f)
            strikeProgress.animateTo(1f, tween(durationMillis = 550, easing = FastOutSlowInEasing))
        } else {
            strikeProgress.snapTo(0f)
        }
    }

    val isAnswered = answeredChoice != null
    val isUserCorrect = isAnswered && (answeredChoice == item.isTrue)
    val swipeHintColor = when {
        dragOffsetX > 40f -> colors.emerald.copy(alpha = 0.25f)
        dragOffsetX < -40f -> colors.crimson.copy(alpha = 0.25f)
        else -> Color.Transparent
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                translationX = dragOffsetX * 0.45f
                rotationZ = (dragOffsetX / 35f).coerceIn(-6f, 6f)
            }
            .pointerInput(item.id, isAnswered) {
                if (!isAnswered) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            when {
                                dragOffsetX > 110f -> {
                                    HapticHelper.tap(context)
                                    onAnswer(true)
                                }
                                dragOffsetX < -110f -> {
                                    HapticHelper.tap(context)
                                    onAnswer(false)
                                }
                            }
                            dragOffsetX = 0f
                        },
                        onDragCancel = { dragOffsetX = 0f },
                        onHorizontalDrag = { _, dragAmount ->
                            dragOffsetX = (dragOffsetX + dragAmount).coerceIn(-220f, 220f)
                        }
                    )
                }
            },
        shape = RoundedCornerShape(20.dp),
        color = colors.card,
        border = BorderStroke(
            width = 1.5.dp,
            color = when {
                !isAnswered && dragOffsetX > 40f -> colors.emerald
                !isAnswered && dragOffsetX < -40f -> colors.crimson
                isAnswered && isUserCorrect -> colors.emerald
                isAnswered && !isUserCorrect -> colors.crimson
                else -> colors.border
            }
        )
    ) {
        Column(
            modifier = Modifier
                .background(swipeHintColor)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⚡ THẺ ĐÚNG / SAI GẠCH BỎ",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.violet
                )
                if (!isAnswered) {
                    Text(
                        text = "👈 Kéo trái: Sai · Kéo phải: Đúng 👉",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = colors.textSecondary
                    )
                } else {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (isUserCorrect) colors.emeraldTint else colors.crimsonTint
                    ) {
                        Text(
                            text = if (isUserCorrect) "✓ Chính xác" else "✗ Chưa đúng",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isUserCorrect) colors.emerald else colors.crimson,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            // Statement body with conditional Canvas crimson strike-through ONLY when isTrue == false
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = item.prefix,
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.textPrimary
                )

                if (isAnswered && !item.isTrue) {
                    // Animated red strike-through line over wrongClause + emerald correction sliding in
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = item.wrongClause,
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.crimson,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                        Canvas(modifier = Modifier.matchParentSize()) {
                            val progress = strikeProgress.value
                            if (progress > 0f) {
                                val yMid = size.height / 2f
                                drawLine(
                                    color = colors.crimson,
                                    start = Offset(0f, yMid),
                                    end = Offset(size.width * progress, yMid),
                                    strokeWidth = 3.dp.toPx(),
                                    cap = StrokeCap.Round
                                )
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = strikeProgress.value > 0.35f,
                        enter = fadeIn(tween(320)) + slideInHorizontally(initialOffsetX = { it / 3 })
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = colors.emeraldTint,
                            border = BorderStroke(1.dp, colors.emerald.copy(alpha = 0.6f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Rounded.CheckCircle, contentDescription = "Sửa đúng", tint = colors.emerald, modifier = Modifier.size(18.dp))
                                Text(
                                    text = "Cơ chế đúng: ${item.correctClause}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = colors.emerald
                                )
                            }
                        }
                    }
                } else if (isAnswered && item.isTrue) {
                    // True statement: emerald confirmation keeping clause completely intact (NO red strike-through!)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = colors.emeraldTint,
                        border = BorderStroke(1.dp, colors.emerald)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Rounded.Verified, contentDescription = "Mệnh đề chuẩn xác", tint = colors.emerald, modifier = Modifier.size(18.dp))
                            Text(
                                text = item.wrongClause,
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.emerald
                            )
                        }
                    }
                } else {
                    Text(
                        text = item.wrongClause + item.suffix,
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.electricBlue
                    )
                }
            }

            if (!isAnswered) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            HapticHelper.tap(context)
                            onAnswer(false)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("tf_false_button"),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.2.dp, colors.crimson.copy(alpha = 0.7f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.crimson)
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Sai", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = {
                            HapticHelper.tap(context)
                            onAnswer(true)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("tf_true_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.emerald, contentColor = Color.White)
                    ) {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Đúng", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Text(
                    text = item.explanation,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary
                )
                Text(
                    text = item.sourceCitation,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.electricBlue
                )
            }
        }
    }
}

/**
 * Celebration Confetti Burst rendered natively via Jetpack Compose Canvas.
 */
@Composable
fun ConfettiBurstOverlay(
    trigger: Boolean,
    modifier: Modifier = Modifier
) {
    if (!trigger) return
    val progress = remember { Animatable(0f) }
    val particles = remember {
        val palette = listOf(
            Color(0xFF3B82F6), Color(0xFF22C55E), Color(0xFFF59E0B),
            Color(0xFF8B5CF6), Color(0xFFEF4444), Color(0xFF38BDF8)
        )
        List(44) {
            Triple(
                Random.nextFloat(), // xFrac
                Random.nextFloat() * 0.7f + 0.45f, // speed
                palette[it % palette.size]
            )
        }
    }
    LaunchedEffect(trigger) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(2200, easing = LinearEasing))
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val p = progress.value
        if (p >= 1f) return@Canvas
        particles.forEachIndexed { idx, (xFrac, speed, color) ->
            val x = size.width * xFrac + sin(p * 10f + idx) * 24f
            val y = -20f + size.height * p * speed * 1.3f
            val alpha = (1f - p).coerceIn(0f, 1f)
            drawRect(
                color = color.copy(alpha = alpha),
                topLeft = Offset(x, y),
                size = Size(8.dp.toPx(), 12.dp.toPx())
            )
        }
    }
}
