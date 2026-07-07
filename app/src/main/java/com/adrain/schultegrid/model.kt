package com.adrain.schultegrid

/**
 * 难度等级：n x n 的方格，需按顺序点击 1..n*n
 */
enum class Difficulty(val size: Int, val label: String, val desc: String) {
    EASY3(3, "3×3", "入门"),
    EASY4(4, "4×4", "简单"),
    NORMAL5(5, "5×5", "标准"),
    HARD6(6, "6×6", "困难"),
    MASTER7(7, "7×7", "大师");

    val total: Int get() = size * size
}

/**
 * 游戏状态
 */
enum class GameState { IDLE, PLAYING, FINISHED }

/**
 * 单元格展示状态
 */
enum class CellStatus { DEFAULT, DONE, WRONG }

data class CellState(
    val number: Int,
    val status: CellStatus = CellStatus.DEFAULT
)
