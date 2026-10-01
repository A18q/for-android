package chat.stoat.composables.voice

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Handler
import android.os.Looper
import androidx.annotation.RawRes
import chat.stoat.R
import logcat.LogPriority
import logcat.logcat

internal enum class VoiceSound(@param:RawRes val resourceId: Int) {
    MUTE(R.raw.sfx_mute),
    UNMUTE(R.raw.sfx_unmute),
    DEAFEN(R.raw.sfx_deafen),
    UNDEAFEN(R.raw.sfx_undeafen),
    USER_JOIN(R.raw.sfx_user_join_voice),
    USER_LEAVE(R.raw.sfx_user_leave_voice),
    STREAM_START(R.raw.sfx_stream_start),
    STREAM_END(R.raw.sfx_stream_end),
    MESSAGE_PING(R.raw.sfx_message_ping),
    MENTION(R.raw.sfx_mention),
    CALL_RINGING(R.raw.sfx_call_ringing),
}

internal class VoiceSoundPlayer(context: Context) {
    private val soundPool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION_SIGNALLING)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val soundCount = VoiceSound.entries.size
    private val soundIds = IntArray(soundCount)
    private val stateLock = Any()
    private var loadedMask = 0
    private val pendingSounds = mutableListOf<VoiceSound>()

    init {
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status != 0) {
                logcat(LogPriority.ERROR) {
                    "Failed to load voice sound sample $sampleId (status $status)"
                }
                return@setOnLoadCompleteListener
            }

            var loadedSound: VoiceSound? = null
            var playCount = 0
            synchronized(stateLock) {
                for (sound in VoiceSound.entries) {
                    if (soundIds[sound.ordinal] == sampleId) {
                        loadedMask = loadedMask or (1 shl sound.ordinal)
                        loadedSound = sound
                        break
                    }
                }
                if (loadedSound != null) {
                    val it = pendingSounds.iterator()
                    while (it.hasNext()) {
                        if (it.next() == loadedSound) {
                            playCount++
                            it.remove()
                        }
                    }
                }
            }
            if (loadedSound != null) {
                for (i in 0 until playCount) {
                    playLoaded(loadedSound)
                }
            }
        }
        VoiceSound.entries.forEach { sound ->
            soundIds[sound.ordinal] =
                soundPool.load(context.applicationContext, sound.resourceId, 1)
        }
    }

    fun play(sound: VoiceSound) {
        val isLoaded = synchronized(stateLock) {
            val loaded = (loadedMask and (1 shl sound.ordinal)) != 0
            if (!loaded) {
                pendingSounds.add(sound)
            }
            loaded
        }
        if (isLoaded) {
            playLoaded(sound)
        }
    }

    fun release() {
        synchronized(stateLock) {
            pendingSounds.clear()
        }
        // let a leave sound finish
        mainHandler.postDelayed(soundPool::release, 1_000)
    }

    private fun playLoaded(sound: VoiceSound) {
        val streamId = soundPool.play(soundIds[sound.ordinal], 1f, 1f, 1, 0, 1f)
        logcat { "Playing voice sound $sound (stream $streamId)" }
        if (streamId == 0) {
            logcat(LogPriority.ERROR) { "SoundPool could not play $sound" }
        }
    }

    private companion object {
        val mainHandler = Handler(Looper.getMainLooper())
    }
}
