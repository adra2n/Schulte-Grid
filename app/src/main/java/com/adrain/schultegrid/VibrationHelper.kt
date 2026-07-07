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

    /** 点击正确：一次短震 */
    fun correct() {
        val v = vibrator ?: return
        if (!supported()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(25)
        }
    }

    /** 点击错误：双脉冲 */
    fun wrong() {
        val v = vibrator ?: return
        if (!supported()) return
        val pattern = longArrayOf(0, 40, 60, 40)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(pattern, -1)
        }
    }

    /** 训练完成：胜利节奏 */
    fun finish() {
        val v = vibrator ?: return
        if (!supported()) return
        val pattern = longArrayOf(0, 80, 70, 80, 70, 160)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(pattern, -1)
        }
    }
}
