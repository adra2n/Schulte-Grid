package com.adrain.schultegrid

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.repeatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
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
    val sound = remember { SoundHelper() }
    var showSettings by remember { mutableStateOf(false) }

    // 倒计时滴答震动反馈
    LaunchedEffect(state.countdown) {
        if (state.countdown != null && state.settings.vibrationEnabled) {
            vibration.correct(VibrationStrength.LIGHT)
        }
    }

    MaterialTheme(colorScheme = SchulteScheme) {
        Surface(modifier = Modifier.fillMaxSize(), color = BgDark) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Primary.copy(alpha = 0.18f), BgDark, BgDark),
                            startY = 0f,
                            endY = 360f
                        )
                    )
                    .padding(horizontal = 16.dp)
                    .padding(top = 12.dp, bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Header(onSettings = { showSettings = true })

                Spacer(Modifier.height(12.dp))

                DifficultySelector(
                    current = state.difficulty,
                    enabled = state.gameState != GameState.PLAYING
                ) { viewModel.selectDifficulty(it) }

                Spacer(Modifier.height(8.dp))

                ModeSelector(
                    current = state.mode,
                    enabled = state.gameState != GameState.PLAYING
                ) { viewModel.selectMode(it) }

                Spacer(Modifier.height(12.dp))

                StatsRow(
                    elapsedMs = state.elapsedMs,
                    nextExpected = if (state.isFinishedSequence) "完成" else state.nextExpected.toString(),
                    bestMs = state.bestMs
                )

                // 平均 & 错误次数（游戏中/完成后显示）
                if (state.gameState != GameState.IDLE) {
                    Spacer(Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        Text(
                            "平均 ${state.avgMs?.let { formatTime(it) } ?: "--"}   ·   错误 ${state.wrongCount}",
                            color = TextSub, fontSize = 12.sp
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                ActionButtons(
                    gameState = state.gameState,
                    paused = state.paused,
                    countdown = state.countdown,
                    onMain = {
                        when {
                            state.gameState == GameState.PLAYING && !state.paused -> viewModel.pause()
                            state.gameState == GameState.PLAYING && state.paused -> viewModel.resume()
                            state.gameState == GameState.FINISHED -> viewModel.start()
                            else -> viewModel.start()
                        }
                    },
                    onReset = { viewModel.reset() }
                )

                Spacer(Modifier.height(10.dp))

                // 进度条
                val progress = when {
                    state.gameState != GameState.PLAYING -> 0f
                    state.mode == Mode.ASC -> (state.nextExpected - 1).toFloat() / state.difficulty.total
                    else -> (state.difficulty.total - state.nextExpected + 1).toFloat() / state.difficulty.total
                }
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = PrimaryLight,
                    trackColor = CardBg
                )

                Spacer(Modifier.height(10.dp))

                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    SchulteGrid(state = state) { index ->
                        when (viewModel.clickCell(index)) {
                            SchulteViewModel.ClickResult.CORRECT -> {
                                if (state.settings.vibrationEnabled) vibration.correct(state.settings.vibrationStrength)
                                if (state.settings.soundEnabled) sound.correct()
                            }
                            SchulteViewModel.ClickResult.WRONG -> {
                                if (state.settings.vibrationEnabled) vibration.wrong(state.settings.vibrationStrength)
                                if (state.settings.soundEnabled) sound.wrong()
                            }
                            SchulteViewModel.ClickResult.FINISH -> {
                                if (state.settings.vibrationEnabled) vibration.finish(state.settings.vibrationStrength)
                                if (state.settings.soundEnabled) sound.finish()
                            }
                            SchulteViewModel.ClickResult.NONE -> Unit
                        }
                    }

                    // 暂停遮罩
                    if (state.gameState == GameState.PLAYING && state.paused) {
                        Box(
                            modifier = Modifier.matchParentSize().background(BgDark.copy(alpha = 0.82f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Filled.Pause, null, tint = TextMain, modifier = Modifier.size(40.dp))
                                Spacer(Modifier.height(8.dp))
                                Text("已暂停", color = TextMain, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(4.dp))
                                Text("点击「继续」恢复训练", color = TextSub, fontSize = 12.sp)
                            }
                        }
                    }

                    // 倒计时遮罩
                    if (state.countdown != null) {
                        Box(
                            modifier = Modifier.matchParentSize().background(BgDark.copy(alpha = 0.88f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CountdownNumber(state.countdown!!)
                        }
                    }
                }

                TipsSection()
            }

            if (state.gameState == GameState.FINISHED && state.lastResultMs != null) {
                ResultDialog(
                    resultMs = state.lastResultMs!!,
                    bestMs = state.bestMs,
                    avgMs = state.avgMs,
                    wrongCount = state.wrongCount,
                    difficulty = state.difficulty,
                    mode = state.mode,
                    history = state.history,
                    onDismiss = { viewModel.reset() },
                    onAgain = { viewModel.start() }
                )
            }

            if (showSettings) {
                SettingsDialog(
                    settings = state.settings,
                    onVibration = { viewModel.setVibrationEnabled(it) },
                    onSound = { viewModel.setSoundEnabled(it) },
                    onStrength = { viewModel.setVibrationStrength(it) },
                    onTest = {
                        if (state.settings.vibrationEnabled) vibration.correct(state.settings.vibrationStrength)
                        if (state.settings.soundEnabled) sound.correct()
                    },
                    onDismiss = { showSettings = false }
                )
            }
        }
    }
}

/* ---------------- 组件 ---------------- */

@Composable
private fun Header(onSettings: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text("舒尔特方格", color = TextMain, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("专注力训练 · Focus Training", color = TextSub, fontSize = 11.sp)
        }
        IconButton(onClick = onSettings) {
            Icon(Icons.Filled.Settings, contentDescription = "设置", tint = TextSub, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
private fun DifficultySelector(current: Difficulty, enabled: Boolean, onSelect: (Difficulty) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Difficulty.values().forEach { d ->
            val selected = d == current
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (selected) Primary else CardBg)
                    .clickable(enabled = enabled, onClick = { onSelect(d) })
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(d.label, color = if (selected) Color.White else TextSub, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(d.desc, color = (if (selected) Color.White else TextSub).copy(alpha = 0.7f), fontSize = 9.sp)
            }
        }
    }
}

@Composable
private fun ModeSelector(current: Mode, enabled: Boolean, onSelect: (Mode) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Mode.values().forEach { m ->
            val selected = m == current
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (selected) PrimaryLight else CardBg)
                    .clickable(enabled = enabled, onClick = { onSelect(m) })
                    .padding(vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(m.label, color = if (selected) Color.White else TextSub, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun StatsRow(elapsedMs: Long, nextExpected: String, bestMs: Long?) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatCard(modifier = Modifier.weight(1f), icon = Icons.Filled.Timer, label = "用时", value = formatTime(elapsedMs))
        StatCard(modifier = Modifier.weight(1f), icon = Icons.Filled.Psychology, label = "下一个", value = nextExpected, highlight = true)
        StatCard(modifier = Modifier.weight(1f), icon = Icons.Filled.EmojiEvents, label = "最佳", value = bestMs?.let { formatTime(it) } ?: "--")
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
        Icon(imageVector = icon, contentDescription = label, tint = if (highlight) PrimaryLight else TextSub, modifier = Modifier.size(18.dp))
        Column {
            Text(label, color = TextSub, fontSize = 10.sp)
            Text(value, color = if (highlight) PrimaryLight else TextMain, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ActionButtons(
    gameState: GameState,
    paused: Boolean,
    countdown: Int?,
    onMain: () -> Unit,
    onReset: () -> Unit
) {
    val mainLabel = when {
        gameState == GameState.PLAYING && !paused -> "暂停"
        gameState == GameState.PLAYING && paused -> "继续"
        gameState == GameState.FINISHED -> "再来一局"
        else -> "开始训练"
    }
    val mainIcon = when {
        gameState == GameState.PLAYING && !paused -> Icons.Filled.Pause
        gameState == GameState.PLAYING && paused -> Icons.Filled.PlayArrow
        else -> null
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(
            onClick = onMain,
            enabled = countdown == null,
            modifier = Modifier.weight(1f).height(46.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Primary, contentColor = Color.White,
                disabledContainerColor = CardBg, disabledContentColor = TextSub
            )
        ) {
            if (mainIcon != null) {
                Icon(mainIcon, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
            }
            Text(mainLabel, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
        OutlinedButton(onClick = onReset, modifier = Modifier.height(46.dp), shape = RoundedCornerShape(14.dp)) {
            Text("重置", fontSize = 15.sp)
        }
    }
}

@Composable
private fun SchulteGrid(state: TrainingUiState, onTap: (Int) -> Unit) {
    val size = state.difficulty.size
    val cells = state.cells

    if (cells.isEmpty()) {
        EmptyState(mode = state.mode)
        return
    }

    val screenMin = LocalConfiguration.current.screenWidthDp.coerceAtMost(LocalConfiguration.current.screenHeightDp)
    val sidePadding = 32.dp
    val gap = 6.dp
    val totalGap = gap.value * (size - 1)
    val available = screenMin - sidePadding.value
    val cellSize = max(28f, (available - totalGap) / size)

    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth().wrapContentHeight(),
        contentAlignment = Alignment.Center
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(gap)) {
            for (row in 0 until size) {
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    for (col in 0 until size) {
                        val index = row * size + col
                        Cell(cell = cells[index], sizeDp = cellSize.dp, canTap = state.countdown == null, onClick = { onTap(index) })
                    }
                }
            }
        }
    }
}

@Composable
private fun Cell(cell: CellState, sizeDp: androidx.compose.ui.unit.Dp, canTap: Boolean, onClick: () -> Unit) {
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

    // 正确：弹跳缩放（先放大再回弹）
    val popScale = remember { Animatable(1f) }
    LaunchedEffect(cell.status) {
        if (cell.status == CellStatus.DONE) {
            popScale.snapTo(1.18f)
            popScale.animateTo(1f, spring(stiffness = 600f, dampingRatio = 0.5f))
        } else {
            popScale.snapTo(1f)
        }
    }

    // 错误：左右抖动
    val shake = remember { Animatable(0f) }
    LaunchedEffect(cell.status) {
        if (cell.status == CellStatus.WRONG) {
            repeat(3) {
                shake.animateTo(7f, spring(stiffness = 1200f, dampingRatio = 0.3f))
                shake.animateTo(-7f, spring(stiffness = 1200f, dampingRatio = 0.3f))
            }
            shake.animateTo(0f)
        } else {
            shake.snapTo(0f)
        }
    }

    val digits = cell.number.toString().length
    val fontSize = (sizeDp.value * (if (digits <= 1) 0.42f else 0.32f)).sp

    Box(
        modifier = Modifier
            .size(sizeDp)
            .graphicsLayer {
                scaleX = popScale.value
                scaleY = popScale.value
                translationX = shake.value
            }
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .clickable(
                enabled = canTap,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(text = cell.number.toString(), color = fg, fontSize = fontSize, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun EmptyState(mode: Mode) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        val transition = rememberInfiniteTransition(label = "pulse")
        val pulse by transition.animateFloat(1f, 1.12f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "pulse")
        Box(modifier = Modifier.size((56 * pulse).dp)) {
            Text("🎯", fontSize = 48.sp)
        }
        Spacer(Modifier.height(10.dp))
        Text("选择难度后点击「开始训练」", color = TextSub, fontSize = 14.sp)
        Spacer(Modifier.height(4.dp))
        Text(
            if (mode == Mode.ASC) "按 1, 2, 3 … 顺序点击数字" else "按 N, N-1 … 顺序点击数字",
            color = TextSub.copy(alpha = 0.7f), fontSize = 12.sp
        )
    }
}

@Composable
private fun CountdownNumber(value: Int) {
    val scale by animateFloatAsState(
        targetValue = 1.4f,
        animationSpec = keyframes {
            durationMillis = 700
            0.6f at 0
            1.4f at 350
            1.4f at 700
        },
        label = "countdownScale"
    )
    Text(
        text = value.toString(),
        color = PrimaryLight,
        fontSize = 96.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.scale(scale)
    )
}

@Composable
private fun TipsSection() {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        TipItem("👆", "按顺序点击数字，越快越好")
        TipItem("📳", "可在「设置」中开关震动与音效")
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

/* ---------------- 设置弹窗 ---------------- */

@Composable
private fun SettingsDialog(
    settings: Settings,
    onVibration: (Boolean) -> Unit,
    onSound: (Boolean) -> Unit,
    onStrength: (VibrationStrength) -> Unit,
    onTest: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBg,
        titleContentColor = TextMain,
        title = { Text("设置", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                SettingRow(label = "震动反馈") {
                    Switch(checked = settings.vibrationEnabled, onCheckedChange = onVibration,
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Primary))
                }
                Column {
                    Text("震动强度", color = TextSub, fontSize = 12.sp)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        VibrationStrength.values().forEach { s ->
                            val sel = settings.vibrationStrength == s
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (sel) Primary else CardBg)
                                    .clickable(enabled = settings.vibrationEnabled, onClick = { onStrength(s) })
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(s.label, color = if (sel) Color.White else TextSub, fontSize = 13.sp)
                            }
                        }
                    }
                }
                SettingRow(label = "音效") {
                    Switch(checked = settings.soundEnabled, onCheckedChange = onSound,
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Primary))
                }
                TextButton(onClick = onTest, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text("试一下", color = PrimaryLight)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("完成", color = PrimaryLight) } }
    )
}

@Composable
private fun SettingRow(label: String, control: @Composable () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = TextMain, fontSize = 14.sp)
        control()
    }
}

/* ---------------- 结果弹窗（含统计、评级与历史） ---------------- */

@Composable
private fun ResultDialog(
    resultMs: Long,
    bestMs: Long?,
    avgMs: Long?,
    wrongCount: Int,
    difficulty: Difficulty,
    mode: Mode,
    history: List<Long>,
    onDismiss: () -> Unit,
    onAgain: () -> Unit
) {
    val isBest = bestMs != null && resultMs <= bestMs
    val stars = rateResult(difficulty, resultMs, wrongCount)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBg,
        titleContentColor = TextMain,
        title = { Text(if (isBest) "🎉 新纪录！" else "✅ 训练完成", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                // 星级评级
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    repeat(3) { i ->
                        val filled = i < stars
                        Icon(
                            Icons.Filled.Star,
                            contentDescription = null,
                            tint = if (filled) Color(0xFFFFD54F) else TextSub.copy(alpha = 0.4f),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("${difficulty.label} · ${difficulty.desc} · ${mode.label}", color = TextSub, fontSize = 12.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                Spacer(Modifier.height(6.dp))
                Text("用时 ${formatTime(resultMs)}", color = TextMain, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                Spacer(Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Text("最佳 ${bestMs?.let { formatTime(it) } ?: "--"}", color = PrimaryLight, fontSize = 13.sp)
                    Spacer(Modifier.width(16.dp))
                    Text("平均 ${avgMs?.let { formatTime(it) } ?: "--"}", color = TextSub, fontSize = 13.sp)
                    Spacer(Modifier.width(16.dp))
                    Text("错误 $wrongCount", color = TextSub, fontSize = 13.sp)
                }
                if (history.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Filled.History, null, tint = TextSub, modifier = Modifier.size(16.dp))
                        Text("最近 ${history.size} 次", color = TextSub, fontSize = 12.sp)
                    }
                    Spacer(Modifier.height(6.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth().height(120.dp).verticalScroll(rememberScrollState())
                    ) {
                        history.reversed().forEachIndexed { i, t ->
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("#${i + 1}", color = TextSub, fontSize = 12.sp)
                                Text(formatTime(t), color = TextMain, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onAgain, colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = Color.White)) {
                Text("再来一局")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("返回", color = TextSub) } }
    )
}

/* ---------------- 工具 ---------------- */
private fun formatTime(ms: Long): String {
    val seconds = ms / 1000.0
    return "%.2fs".format(seconds)
}
