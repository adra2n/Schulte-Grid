package com.adrain.schultegrid

import android.content.Context
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
    val gameState: GameState = GameState.IDLE,
    val cells: List<CellState> = emptyList(),
    val nextExpected: Int = 1,
    val elapsedMs: Long = 0L,
    val bestMs: Long? = null,
    val lastResultMs: Long? = null,
    val wrongIndex: Int? = null
)

class SchulteViewModel(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var timerJob: Job? = null

    private val _uiState = MutableStateFlow(
        TrainingUiState(bestMs = bestFor(Difficulty.NORMAL5))
    )
    val uiState: StateFlow<TrainingUiState> = _uiState.asStateFlow()

    /** 切换难度（仅在非游戏中允许） */
    fun selectDifficulty(d: Difficulty) {
        if (_uiState.value.gameState == GameState.PLAYING) return
        _uiState.update {
            it.copy(difficulty = d, bestMs = bestFor(d), cells = emptyList(),
                nextExpected = 1, elapsedMs = 0L, lastResultMs = null, wrongIndex = null)
        }
    }

    /** 开始一局 */
    fun start() {
        val size = _uiState.value.difficulty.size
        val total = size * size
        val numbers = (1..total).shuffled()
        _uiState.update {
            it.copy(
                gameState = GameState.PLAYING,
                cells = numbers.map { n -> CellState(n) },
                nextExpected = 1,
                elapsedMs = 0L,
                lastResultMs = null,
                wrongIndex = null
            )
        }
        startTimer()
    }

    /** 重置回初始 */
    fun reset() {
        stopTimer()
        _uiState.update {
            it.copy(
                gameState = GameState.IDLE,
                cells = emptyList(),
                nextExpected = 1,
                elapsedMs = 0L,
                lastResultMs = null,
                wrongIndex = null
            )
        }
    }

    /**
     * 点击某个单元格，返回事件类型供 UI 触发震动。
     * 返回: ClickResult
     */
    fun clickCell(index: Int): ClickResult {
        val state = _uiState.value
        if (state.gameState != GameState.PLAYING) return ClickResult.NONE
        val cell = state.cells.getOrNull(index) ?: return ClickResult.NONE

        return if (cell.number == state.nextExpected) {
            val newCells = state.cells.toMutableList().also {
                it[index] = cell.copy(status = CellStatus.DONE)
            }
            val newNext = state.nextExpected + 1
            val total = state.difficulty.total
            if (newNext > total) {
                // 完成
                stopTimer()
                val result = state.elapsedMs
                val best = bestFor(state.difficulty)
                val isBest = best == null || result < best
                if (isBest) saveBest(state.difficulty, result)
                _uiState.update {
                    it.copy(
                        cells = newCells,
                        nextExpected = newNext,
                        gameState = GameState.FINISHED,
                        lastResultMs = result,
                        bestMs = if (isBest) result else best
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
            // 错误：标记该格为错误状态，短暂高亮
            val newCells = state.cells.toMutableList().also {
                it[index] = cell.copy(status = CellStatus.WRONG)
            }
            _uiState.update { it.copy(cells = newCells, wrongIndex = index) }
            // 350ms 后恢复该格默认状态
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

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = scope.launch {
            val startAt = System.currentTimeMillis()
            while (true) {
                delay(33)
                _uiState.update { it.copy(elapsedMs = System.currentTimeMillis() - startAt) }
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    private fun bestFor(d: Difficulty): Long? =
        prefs.getLong(key(d), -1L).takeIf { it >= 0 }

    private fun saveBest(d: Difficulty, ms: Long) {
        prefs.edit().putLong(key(d), ms).apply()
    }

    private fun key(d: Difficulty) = "best_${d.size}"

    companion object {
        private const val PREFS_NAME = "schulte_best"
    }
}
