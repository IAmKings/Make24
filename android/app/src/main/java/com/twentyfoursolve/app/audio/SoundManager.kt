package com.twentyfoursolve.app.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.twentyfoursolve.app.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SoundManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var soundPool: SoundPool? = null
    private val soundMap = mutableMapOf<SoundType, Int>()
    var enabled: Boolean = true

    init {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(attrs)
            .build()

        loadSounds()
    }

    private fun loadSounds() {
        val sounds = mapOf(
            SoundType.CLICK to R.raw.click,
            SoundType.SELECT to R.raw.select,
            SoundType.OPERATOR to R.raw.operator,
            SoundType.SUCCESS to R.raw.success,
            SoundType.FAIL to R.raw.fail,
            SoundType.UNDO to R.raw.undo
        )

        sounds.forEach { (type, resId) ->
            try {
                val id = soundPool?.load(context, resId, 1)
                if (id != null && id != 0) {
                    soundMap[type] = id
                }
            } catch (_: Exception) {
                // Sound resource not found, skip
            }
        }
    }

    fun play(type: SoundType) {
        if (!enabled) return
        val id = soundMap[type] ?: return
        soundPool?.play(id, 1f, 1f, 1, 0, 1f)
    }

    fun release() {
        soundPool?.release()
        soundPool = null
        soundMap.clear()
    }
}

enum class SoundType {
    CLICK,
    SELECT,
    OPERATOR,
    SUCCESS,
    FAIL,
    UNDO
}
