package com.adrain.schultegrid

import android.media.AudioManager
import android.media.ToneGenerator

/**
 * 音效反馈：使用 ToneGenerator 合成提示音，无需音频资源文件。
 * - 点击正确：清脆短音
 * - 点击错误：低沉提示
 * - 训练完成：确认音
 *
 * 是否启用由调用方（UI）根据设置判断。
 */
class SoundHelper {

    private val tg: ToneGenerator? = runCatching {
        ToneGenerator(AudioManager.STREAM_MUSIC, 80)
    }.getOrNull()

    fun correct() {
        tg?.startTone(ToneGenerator.TONE_PROP_ACK, 90)
    }

    fun wrong() {
        tg?.startTone(ToneGenerator.TONE_PROP_NACK, 130)
    }

    fun finish() {
        tg?.startTone(ToneGenerator.TONE_CDMA_CONFIRM, 120)
    }
}
