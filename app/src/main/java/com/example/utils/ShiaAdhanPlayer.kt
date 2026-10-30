package com.example.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.util.Log

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object ShiaAdhanPlayer {
    private var mediaPlayer: MediaPlayer? = null
    private var audioTrack: AudioTrack? = null

    class SynthNote(val freq: Float, val start: Float, val dur: Float, val vol: Float = 1f)

    private fun playSpiritualChime(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                stop() // Ensure clean slate
                
                val sampleRate = 16000
                val duration = 12.0f // 12 seconds of custom prayer chime
                val totalSamples = (sampleRate * duration).toInt()
                val buffer = ShortArray(totalSamples)

                val ambientNotes = listOf(
                    SynthNote(440.00f, 0.0f, 3.0f),      // A4
                    SynthNote(523.25f, 1.5f, 3.0f),      // C5
                    SynthNote(659.25f, 3.0f, 3.0f),      // E5
                    SynthNote(880.00f, 4.5f, 4.0f),      // A5
                    SynthNote(783.99f, 6.0f, 4.0f),      // G5
                    SynthNote(659.25f, 7.5f, 4.0f),      // E5
                    SynthNote(523.25f, 9.0f, 3.0f),      // C5
                    SynthNote(440.00f, 10.0f, 2.0f)      // A4
                )

                for (sampleIdx in 0 until totalSamples) {
                    val t = sampleIdx.toFloat() / sampleRate
                    var mixedValue = 0.0
                    for (note in ambientNotes) {
                        if (t >= note.start && t < note.start + note.dur) {
                            val noteT = t - note.start
                            val angle = 2.0 * Math.PI * note.freq * noteT
                            val envelope = Math.exp(-2.5 * noteT)
                            val wave = Math.sin(angle) + 0.4 * Math.sin(2.0 * angle) + 0.2 * Math.sin(3.0 * angle)
                            mixedValue += wave * envelope * note.vol * 3500.0
                        }
                    }
                    if (mixedValue > 32767.0) mixedValue = 32767.0
                    if (mixedValue < -32768.0) mixedValue = -32768.0
                    buffer[sampleIdx] = mixedValue.toInt().toShort()
                }

                val track = AudioTrack(
                    android.media.AudioManager.STREAM_ALARM,
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    buffer.size * 2,
                    AudioTrack.MODE_STATIC
                )
                audioTrack = track
                track.write(buffer, 0, buffer.size)
                track.play()
                Log.d("ShiaAdhanPlayer", "Playing premium synthesized spiritual chime successfully!")
            } catch (t: Throwable) {
                Log.e("ShiaAdhanPlayer", "Spiritual chime synthesis failed: ${t.message}")
            }
        }
    }

    fun play(context: Context, soundKey: String? = null) {
        stop() // Stop and release previous session if any
        
        Log.d("ShiaAdhanPlayer", "Playing Mesmerizing Shia Adhan locally")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val mp = MediaPlayer.create(context, com.example.R.raw.mesmerizing_shia_azan)
                if (mp == null) {
                    Log.e("ShiaAdhanPlayer", "MediaPlayer.create returned null for R.raw.mesmerizing_shia_azan, playing chime fallback...")
                    playSpiritualChime(context)
                    return@launch
                }
                
                mp.apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .build()
                    )
                }
                mediaPlayer = mp
                mp.isLooping = false
                mp.start()
                Log.d("ShiaAdhanPlayer", "Playing local mesmerizing Shia Adhan resource successfully!")
            } catch (e: Exception) {
                Log.e("ShiaAdhanPlayer", "Error playing Adhan: ${e.message}. Trying chime fallback.")
                playSpiritualChime(context)
            }
        }
    }

    private fun playStreamingOrFallback(context: Context, soundKey: String) {
        play(context, "mesmerizing")
    }

    private fun playFallback(context: Context) {
        playSpiritualChime(context)
    }

    fun stop() {
        try {
            audioTrack?.let {
                try {
                    it.stop()
                } catch(e: Exception) {}
                it.release()
            }
        } catch(e: Exception) {}
        audioTrack = null

        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
        } catch (e: Exception) {
            Log.e("ShiaAdhanPlayer", "Error stopping: ${e.message}")
        } finally {
            mediaPlayer = null
        }
    }
}
