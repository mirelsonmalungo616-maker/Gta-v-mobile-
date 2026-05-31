package com.example.game

import android.media.AudioManager
import android.media.ToneGenerator

object GameSound {
    private var toneGenerator: ToneGenerator? = null
    var soundEnabled: Boolean = true

    init {
        try {
            // Allocate DTMF Tone generator mapped to master system alarm channel
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 85)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun playShoot() {
        if (!soundEnabled) return
        Thread {
            try {
                // Short dry high frequency tap
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 75)
            } catch (e: Exception) {
                // Safe guard
            }
        }.start()
    }

    fun playExplosion() {
        if (!soundEnabled) return
        Thread {
            try {
                // Solid buzz thud
                toneGenerator?.startTone(ToneGenerator.TONE_SUP_DIAL, 250)
            } catch (e: Exception) {
            }
        }.start()
    }

    fun playCash() {
        if (!soundEnabled) return
        Thread {
            try {
                // Classic cash register high pitch double ring
                toneGenerator?.startTone(ToneGenerator.TONE_DTMF_D, 120)
                Thread.sleep(140)
                toneGenerator?.startTone(ToneGenerator.TONE_DTMF_9, 150)
            } catch (e: Exception) {
            }
        }.start()
    }

    fun playCarHorn() {
        if (!soundEnabled) return
        Thread {
            try {
                toneGenerator?.startTone(ToneGenerator.TONE_SUP_RADIO_ACK, 250)
            } catch (e: Exception) {
            }
        }.start()
    }

    fun playMissionSuccess() {
        if (!soundEnabled) return
        Thread {
            try {
                // Uplifting chord sequence
                toneGenerator?.startTone(ToneGenerator.TONE_DTMF_1, 100)
                Thread.sleep(120)
                toneGenerator?.startTone(ToneGenerator.TONE_DTMF_5, 100)
                Thread.sleep(120)
                toneGenerator?.startTone(ToneGenerator.TONE_DTMF_9, 120)
                Thread.sleep(120)
                toneGenerator?.startTone(ToneGenerator.TONE_DTMF_A, 250)
            } catch (e: Exception) {
            }
        }.start()
    }

    fun playMissionFail() {
        if (!soundEnabled) return
        Thread {
            try {
                toneGenerator?.startTone(ToneGenerator.TONE_SUP_CONGESTION, 500)
            } catch (e: Exception) {
            }
        }.start()
    }
}
