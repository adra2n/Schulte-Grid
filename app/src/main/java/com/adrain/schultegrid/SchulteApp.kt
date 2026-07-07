package com.adrain.schultegrid

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.EmojiEvents
import kotlin.math.max

/* ---------------- 配色 ---------------- */
private val BgDark = Color(0xFF0F172A)
private val CardBg = Color(0xFF1E293B)
private val Primary = Color(0xFF6366F1)
private val PrimaryLight = Color(0xFF818CF8)
private val Correct = Color(0xFF22C55E)
private val Wrong = Color(0xFFEF4444)
private val TextMain = Color(0xFFF8FAFC)
private val TextSub = Color(0xFF94A3B8)
private val CellBg = Color(0xFF334155)
private val CellDone = Color(0xFF166534)
private val CellDoneText = Color(0xFF86EFAC)
private val CellWrong = Color(0xFF7F1D1D)

private val SchulteScheme = darkColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    background = BgDark,
    onBackground = TextMain,
    surface = CardBg,
    onSurface = TextMain,
)

/* ---------------- 入口 ---------------- */
@Composable
fun SchulteApp(viewModel: SchulteViewModel) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val vibration = remember { VibrationHelper(context) }

    MaterialTheme(colorScheme = SchulteScheme) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = BgDark
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .padding(top = 12.dp, bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Header()

                Spacer(Modifier.height(12.dp))

                DifficultySelector(
                    current = state.difficulty,
                    enabled = state.gameState != GameState.PLAYING
                ) { viewModel.selectDifficulty(it) }

                Spacer(Modifier.height(12.dp))

                StatsRow(
                    elapsedMs = state.elapsedMs,
                    nextExpected = state.nextExpected,
                    total = state.difficulty.total,
                    bestMs = state.bestMs
                )

                Spacer(Modifier.height(12.dp))

                ActionButtons(
                    gameState = state.gameState,
                    onStart = { viewModel.start() },
                    onReset = { viewModel.reset() }
                )

                Spacer(Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    SchulteGrid(
                        state = state,
                        onTap = { index ->
                            when (viewModel.clickCell(index)) {
                                SchulteViewModel.ClickResult.CORRECT -> vibration.correct()
                                SchulteViewModel.ClickResult.WRONG -> vibration.wrong()
                                SchulteViewModel.ClickResult.FINISH -> vibration.finish()
                                SchulteViewModel.ClickResult.NONE -> Unit
                            }
                        }
                    )
                }

                TipsSection()
            }

            // 完成弹窗
            if (state.gameState == GameState.FINISHED && state.lastResultMs != null) {
                ResultDialog(
                    resultMs = state.lastResultMs!!,
                    bestMs = state.bestMs,
                    difficulty = state.difficulty,
                    onDismiss = { viewModel.reset() },
                    onAgain = { viewModel.start() }
                )
            }
        }
    }
}

/* ---------------- 组件 ---------------- */

@Composable
private fun Header() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "舒尔特方格",
            color = TextMain,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            "专注力训练 · Focus Training",
            color = TextSub,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun DifficultySelector(
    current: Difficulty,
    enabled: Boolean,
    onSelect: (Difficulty) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Difficulty.values().forEach { d ->
            val selected = d == current
            val bg = if (selected) Primary else CardBg
            val fg = if (selected) Color.White else TextSub
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(bg)
                    .clickable(enabled = enabled) { onSelect(d) }
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(d.label, color = fg, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(d.desc, color = fg.copy(alpha = 0.7f), fontSize = 9.sp)
            }
        }
    }
}

@Composable
private fun StatsRow(
    elapsedMs: Long,
    nextExpected: Int,
    total: Int,
    bestMs: Long?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Filled.Timer,
            label = "用时",
            value = formatTime(elapsedMs)
        )
        StatCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Filled.Psychology,
            label = "下一个",
            value = if (nextExpected > total) "完成" else nextExpected.toString(),
            highlight = true
        )
        StatCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Filled.EmojiEvents,
            label = "最佳",
            value = bestMs?.let { formatTime(it) } ?: "--"
        )
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    highlight: Boolean = false
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(CardBg)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (highlight) PrimaryLight else TextSub,
            modifier = Modifier.size(18.dp)
        )
        Column {
            Text(label, color = TextSub, fontSize = 10.sp)
            Text(
                value,
                color = if (highlight) PrimaryLight else TextMain,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ActionButtons(
    gameState: GameState,
    onStart: () -> Unit,
    onReset: () -> Unit
) {
    val playing = gameState == GameState.PLAYING
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Button(
            onClick = onStart,
            enabled = !playing,
            modifier = Modifier.weight(1f).height(46.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Primary,
                contentColor = Color.White,
                disabledContainerColor = CardBg,
                disabledContentColor = TextSub
            )
        ) {
            Text(if (gameState == GameState.FINISHED) "再来一局" else "开始训练", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
        OutlinedButton(
            onClick = onReset,
            modifier = Modifier.height(46.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("重置", fontSize = 15.sp)
        }
    }
}

@Composable
private fun SchulteGrid(
    state: TrainingUiState,
    onTap: (Int) -> Unit
) {
    val size = state.difficulty.size
    val cells = state.cells

    if (cells.isEmpty()) {
        // 空状态提示
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🎯", fontSize = 48.sp)
            Spacer(Modifier.height(8.dp))
            Text("选择难度后点击「开始训练」", color = TextSub, fontSize = 14.sp)
            Spacer(Modifier.height(4.dp))
            Text("按 1, 2, 3 … 顺序点击数字", color = TextSub.copy(alpha = 0.7f), fontSize = 12.sp)
        }
        return
    }

    val screenMin = LocalConfiguration.current.screenWidthDp.coerceAtMost(
        LocalConfiguration.current.screenHeightDp
    )
    val sidePadding = 32.dp
    val gap = 6.dp
    val totalGap = gap.value * (size - 1)
    val available = screenMin - sidePadding.value
    val cellSize = max(28f, (available - totalGap) / size)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        contentAlignment = Alignment.Center
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(gap)) {
            for (row in 0 until size) {
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    for (col in 0 until size) {
                        val index = row * size + col
                        val cell = cells[index]
                        Cell(
                            cell = cell,
                            sizeDp = cellSize.dp,
                            onClick = { onTap(index) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Cell(
    cell: CellState,
    sizeDp: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit
) {
    val bg = when (cell.status) {
        CellStatus.DEFAULT -> CellBg
        CellStatus.DONE -> CellDone
        CellStatus.WRONG -> CellWrong
    }
    val fg = when (cell.status) {
        CellStatus.DEFAULT -> TextMain
        CellStatus.DONE -> CellDoneText
        CellStatus.WRONG -> Color.White
    }
    Box(
        modifier = Modifier
            .size(sizeDp)
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = cell.number.toString(),
            color = fg,
            fontSize = (sizeDp.value * 0.42f).sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun TipsSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        TipItem("👆", "按顺序点击 1 到 N，越快越好")
        TipItem("📳", "每次点击都有震动反馈")
        TipItem("👁️", "视线注视中心，余光找数字")
    }
}

@Composable
private fun TipItem(icon: String, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(icon, fontSize = 12.sp)
        Text(text, color = TextSub.copy(alpha = 0.85f), fontSize = 11.sp)
    }
}

@Composable
private fun ResultDialog(
    resultMs: Long,
    bestMs: Long?,
    difficulty: Difficulty,
    onDismiss: () -> Unit,
    onAgain: () -> Unit
) {
    val isBest = bestMs != null && resultMs <= bestMs
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBg,
        titleContentColor = TextMain,
        title = {
            Text(if (isBest) "🎉 新纪录！" else "✅ 训练完成", fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                Text("${difficulty.label} · ${difficulty.desc}", color = TextSub, fontSize = 12.sp)
                Spacer(Modifier.height(6.dp))
                Text("用时 ${formatTime(resultMs)}", color = TextMain, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                if (bestMs != null) {
                    Text("最佳 ${formatTime(bestMs)}", color = PrimaryLight, fontSize = 13.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onAgain,
                colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = Color.White)
            ) { Text("再来一局") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("返回", color = TextSub) }
        }
    )
}

/* ---------------- 工具 ---------------- */
private fun formatTime(ms: Long): String {
    val seconds = ms / 1000.0
    return "%.2fs".format(seconds)
}
