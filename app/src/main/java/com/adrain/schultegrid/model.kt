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
 * 训练模式：正序 1→N，倒序 N→1
 */
enum class Mode(val label: String) {
    ASC("正序"),
    DESC("倒序")
}

/**
 * 震动强度
 */
enum class VibrationStrength(val label: String) {
    LIGHT("轻"),
    NORMAL("中"),
    STRONG("强")
}

/**
 * 用户设置（震动/音效）
 */
data class Settings(
    val vibrationEnabled: Boolean = true,
    val soundEnabled: Boolean = false,
    val vibrationStrength: VibrationStrength = VibrationStrength.NORMAL
)

/**
 * 单元格展示状态
 */
enum class CellStatus { DEFAULT, DONE, WRONG }

data class CellState(
    val number: Int,
    val status: CellStatus = CellStatus.DEFAULT
)
