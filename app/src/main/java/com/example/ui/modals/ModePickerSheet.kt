package com.example.ui.modals

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.model.LearningMode
import com.example.model.LearningModeGroup
import com.example.ui.screens.ModeCardRow
import com.example.ui.theme.LocalAstraColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ElevenModesBottomSheet(
    onSelectMode: (LearningMode) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalAstraColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("🧩 11 Dạng bài học chuẩn Astra AI", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
            Text("Chọn nhanh phương pháp phù hợp mục tiêu học hiện tại", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)

            LearningModeGroup.entries.forEach { group ->
                val accent = when (group) {
                    LearningModeGroup.ACQUIRE -> colors.electricBlue
                    LearningModeGroup.PRACTICE -> colors.emerald
                    LearningModeGroup.ASSESS -> colors.violet
                }
                Text(
                    text = when (group) {
                        LearningModeGroup.ACQUIRE -> "NHÓM 1 — HỌC (Acquire)"
                        LearningModeGroup.PRACTICE -> "NHÓM 2 — LUYỆN TẬP (Practice)"
                        LearningModeGroup.ASSESS -> "NHÓM 3 — THI (Assess)"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = accent,
                    modifier = Modifier.padding(top = 6.dp)
                )
                LearningMode.entries.filter { it.group == group }.forEach { mode ->
                    ModeCardRow(mode = mode) { onSelectMode(mode) }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
