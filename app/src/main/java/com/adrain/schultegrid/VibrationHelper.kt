package com.adrain.schultegrid

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * 震动反馈统一封装。
 * - 点击正确：短促轻震
 * - 点击错误：双脉冲提示
 * - 训练完成：胜利节奏
 *
 * 支持强度（轻 / 中 / 强）缩放持续时间。是否启用由调用方（UI）根据设置判断。
 */
class VibrationHelper(context: Context) {

    private val vibrator: Vibrator? = run {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    private fun supported(): Boolean = vibrator?.hasVibrator() == true

    /** 强度系数：轻 0.5x，中 1x，强 1.6x */
    private fun factor(s: VibrationStrength): Double = when (s) {
        VibrationStrength.LIGHT -> 0.5
        VibrationStrength.NORMAL -> 1.0
        VibrationStrength.STRONG -> 1.6
    }

    private fun oneShot(ms: Long, strength: VibrationStrength) {
        val v = vibrator ?: return
        if (!supported()) return
        val dur = (ms * factor(strength)).toLong().coerceAtLeast(1L)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(dur, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(dur)
        }
    }

    private fun wave(pattern: LongArray, strength: VibrationStrength) {
        val v = vibrator ?: return
        if (!supported()) return
        val f = factor(strength)
        // 缩放每段时长（首段 0 为起始延迟，保持 0）
        val scaled = pattern.mapIndexed { i, p -> if (i == 0) 0L else (p * f).toLong() }.toLongArray()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createWaveform(scaled, -1))
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(scaled, -1)
        }
    }

    /** 点击正确：一次短震 */
    fun correct(strength: VibrationStrength) = oneShot(25, strength)

    /** 点击错误：双脉冲 */
    fun wrong(strength: VibrationStrength) = wave(longArrayOf(0, 40, 60, 40), strength)

    /** 训练完成：胜利节奏 */
    fun finish(strength: VibrationStrength) = wave(longArrayOf(0, 80, 70, 80, 70, 160), strength)
}
