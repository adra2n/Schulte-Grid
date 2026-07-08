package com.adrain.schultegrid

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 训练界面的状态
 */
data class TrainingUiState(
    val difficulty: Difficulty = Difficulty.NORMAL5,
    val mode: Mode = Mode.ASC,
    val gameState: GameState = GameState.IDLE,
    val paused: Boolean = false,
    val cells: List<CellState> = emptyList(),
    val nextExpected: Int = 1,
    val elapsedMs: Long = 0L,
    val wrongCount: Int = 0,
    val bestMs: Long? = null,
    val avgMs: Long? = null,
    val history: List<Long> = emptyList(),
    val lastResultMs: Long? = null,
    val wrongIndex: Int? = null,
    val settings: Settings = Settings()
)

/** 当前是否已到达序列末端（用于 UI 显示"完成"） */
val TrainingUiState.isFinishedSequence: Boolean
    get() = when (mode) {
        Mode.ASC -> nextExpected > difficulty.total
        Mode.DESC -> nextExpected < 1
    }

class SchulteViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = app.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var timerJob: Job? = null
    private var runStartMs = 0L
    private var accumMs = 0L

    private val _uiState = MutableStateFlow(
        TrainingUiState(
            difficulty = Difficulty.NORMAL5,
            mode = Mode.ASC,
            settings = loadSettings()
        ).applyStats(Difficulty.NORMAL5, Mode.ASC)
    )
    val uiState: StateFlow<TrainingUiState> = _uiState.asStateFlow()

    private fun TrainingUiState.applyStats(d: Difficulty, m: Mode): TrainingUiState {
        val (best, avg, hist) = loadStats(d, m)
        return copy(bestMs = best, avgMs = avg, history = hist)
    }

    /* ---------------- 难度 / 模式 ---------------- */

    fun selectDifficulty(d: Difficulty) {
        if (_uiState.value.gameState == GameState.PLAYING) return
        _uiState.update {
            it.copy(difficulty = d, cells = emptyList(), nextExpected = 1,
                elapsedMs = 0L, lastResultMs = null, wrongCount = 0, wrongIndex = null,
                paused = false).applyStats(d, it.mode)
        }
    }

    fun selectMode(m: Mode) {
        if (_uiState.value.gameState == GameState.PLAYING) return
        _uiState.update {
            it.copy(mode = m, cells = emptyList(), nextExpected = 1,
                elapsedMs = 0L, lastResultMs = null, wrongCount = 0, wrongIndex = null,
                paused = false).applyStats(it.difficulty, m)
        }
    }

    /* ---------------- 开始 / 重置 / 暂停 ---------------- */

    fun start() {
        val size = _uiState.value.difficulty.size
        val total = size * size
        val numbers = (1..total).shuffled()
        val first = when (_uiState.value.mode) {
            Mode.ASC -> 1
            Mode.DESC -> total
        }
        accumMs = 0L
        _uiState.update {
            it.copy(
                gameState = GameState.PLAYING,
                cells = numbers.map { n -> CellState(n) },
                nextExpected = first,
                elapsedMs = 0L,
                wrongCount = 0,
                lastResultMs = null,
                wrongIndex = null,
                paused = false
            )
        }
        startTimer()
    }

    fun reset() {
        stopTimer()
        accumMs = 0L
        _uiState.update {
            it.copy(
                gameState = GameState.IDLE,
                cells = emptyList(),
                nextExpected = 1,
                elapsedMs = 0L,
                wrongCount = 0,
                lastResultMs = null,
                wrongIndex = null,
                paused = false
            )
        }
    }

    fun pause() {
        if (_uiState.value.gameState != GameState.PLAYING || _uiState.value.paused) return
        pauseTimer()
    }

    fun resume() {
        if (_uiState.value.gameState != GameState.PLAYING || !_uiState.value.paused) return
        resumeTimer()
    }

    /* ---------------- 点击 ---------------- */

    fun clickCell(index: Int): ClickResult {
        val state = _uiState.value
        if (state.gameState != GameState.PLAYING || state.paused) return ClickResult.NONE
        val cell = state.cells.getOrNull(index) ?: return ClickResult.NONE

        return if (cell.number == state.nextExpected) {
            val newCells = state.cells.toMutableList().also {
                it[index] = cell.copy(status = CellStatus.DONE)
            }
            val newNext = when (state.mode) {
                Mode.ASC -> state.nextExpected + 1
                Mode.DESC -> state.nextExpected - 1
            }
            val finished = when (state.mode) {
                Mode.ASC -> newNext > state.difficulty.total
                Mode.DESC -> newNext < 1
            }
            if (finished) {
                stopTimer()
                val result = accumMs
                val best = bestFor(state.difficulty, state.mode)
                val isBest = best == null || result < best
                if (isBest) saveBest(state.difficulty, state.mode, result)
                val (_, avg, hist) = pushHistory(state.difficulty, state.mode, result)
                _uiState.update {
                    it.copy(
                        cells = newCells,
                        nextExpected = newNext,
                        gameState = GameState.FINISHED,
                        lastResultMs = result,
                        bestMs = if (isBest) result else best,
                        avgMs = avg,
                        history = hist,
                        wrongIndex = null
                    )
                }
                ClickResult.FINISH
            } else {
                _uiState.update {
                    it.copy(cells = newCells, nextExpected = newNext, wrongIndex = null)
                }
                ClickResult.CORRECT
            }
        } else {
            _uiState.update { it.copy(wrongCount = it.wrongCount + 1) }
            val newCells = state.cells.toMutableList().also {
                it[index] = cell.copy(status = CellStatus.WRONG)
            }
            _uiState.update { it.copy(cells = newCells, wrongIndex = index) }
            scope.launch {
                delay(350)
                _uiState.update { s ->
                    if (s.wrongIndex == index) {
                        val restored = s.cells.toMutableList().also { list ->
                            if (list[index].status == CellStatus.WRONG) {
                                list[index] = list[index].copy(status = CellStatus.DEFAULT)
                            }
                        }
                        s.copy(cells = restored, wrongIndex = null)
                    } else s
                }
            }
            ClickResult.WRONG
        }
    }

    enum class ClickResult { NONE, CORRECT, WRONG, FINISH }

    /* ---------------- 计时（支持暂停） ---------------- */

    private fun startTimer() {
        timerJob?.cancel()
        runStartMs = System.currentTimeMillis()
        timerJob = scope.launch {
            while (true) {
                delay(33)
                val now = System.currentTimeMillis()
                _uiState.update { it.copy(elapsedMs = accumMs + (now - runStartMs)) }
            }
        }
    }

    private fun pauseTimer() {
        val now = System.currentTimeMillis()
        accumMs += now - runStartMs
        timerJob?.cancel()
        timerJob = null
        _uiState.update { it.copy(paused = true, elapsedMs = accumMs) }
    }

    private fun resumeTimer() {
        _uiState.update { it.copy(paused = false) }
        startTimer()
    }

    private fun stopTimer() {
        if (timerJob != null) {
            val now = System.currentTimeMillis()
            accumMs += now - runStartMs
        }
        timerJob?.cancel()
        timerJob = null
        _uiState.update { it.copy(elapsedMs = accumMs) }
    }

    /* ---------------- 设置 ---------------- */

    fun setVibrationEnabled(enabled: Boolean) {
        val s = _uiState.value.settings.copy(vibrationEnabled = enabled)
        saveSettings(s)
        _uiState.update { it.copy(settings = s) }
    }

    fun setSoundEnabled(enabled: Boolean) {
        val s = _uiState.value.settings.copy(soundEnabled = enabled)
        saveSettings(s)
        _uiState.update { it.copy(settings = s) }
    }

    fun setVibrationStrength(strength: VibrationStrength) {
        val s = _uiState.value.settings.copy(vibrationStrength = strength)
        saveSettings(s)
        _uiState.update { it.copy(settings = s) }
    }

    private fun loadSettings(): Settings = Settings(
        vibrationEnabled = prefs.getBoolean(KEY_SET_VIBRATION, true),
        soundEnabled = prefs.getBoolean(KEY_SET_SOUND, false),
        vibrationStrength = VibrationStrength.values()
            .getOrElse(prefs.getInt(KEY_SET_STRENGTH, 1)) { VibrationStrength.NORMAL }
    )

    private fun saveSettings(s: Settings) {
        prefs.edit()
            .putBoolean(KEY_SET_VIBRATION, s.vibrationEnabled)
            .putBoolean(KEY_SET_SOUND, s.soundEnabled)
            .putInt(KEY_SET_STRENGTH, s.vibrationStrength.ordinal)
            .apply()
    }

    /* ---------------- 最佳 / 历史 ---------------- */

    private fun bestFor(d: Difficulty, m: Mode): Long? =
        prefs.getLong(bestKey(d, m), -1L).takeIf { it >= 0 }

    private fun saveBest(d: Difficulty, m: Mode, ms: Long) {
        prefs.edit().putLong(bestKey(d, m), ms).apply()
    }

    /** 读取历史（最近 MAX_HISTORY 条），返回 (best, avg, history) */
    private fun loadStats(d: Difficulty, m: Mode): Triple<Long?, Long?, List<Long>> {
        val hist = readHistory(d, m)
        val best = hist.minOrNull()
        val avg = if (hist.isNotEmpty()) hist.average().toLong() else null
        return Triple(best, avg, hist)
    }

    /** 追加一条成绩并返回最新 (best, avg, history) */
    private fun pushHistory(d: Difficulty, m: Mode, ms: Long): Triple<Long?, Long?, List<Long>> {
        val hist = (readHistory(d, m) + ms).takeLast(MAX_HISTORY)
        prefs.edit().putString(histKey(d, m), hist.joinToString(",")).apply()
        val best = hist.minOrNull()
        val avg = hist.average().toLong()
        return Triple(best, avg, hist)
    }

    private fun readHistory(d: Difficulty, m: Mode): List<Long> {
        val raw = prefs.getString(histKey(d, m), "") ?: ""
        if (raw.isBlank()) return emptyList()
        return raw.split(",").mapNotNull { it.toLongOrNull() }
    }

    private fun bestKey(d: Difficulty, m: Mode) = "best_${d.size}_${m.name}"
    private fun histKey(d: Difficulty, m: Mode) = "history_${d.size}_${m.name}"

    companion object {
        private const val PREFS_NAME = "schulte_best"
        private const val MAX_HISTORY = 10
        private const val KEY_SET_VIBRATION = "set_vibration"
        private const val KEY_SET_SOUND = "set_sound"
        private const val KEY_SET_STRENGTH = "set_strength"
    }
}
