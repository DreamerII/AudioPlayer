package com.dreamfire.bookdemo.player

import android.content.Context
import android.content.Intent
import android.os.Build
import com.dreamfire.bookdemo.service.AudioPlayerService
import com.dreamfire.bookdemo.utils.Constants

class AudioPlayerController(private val context: Context) {

    private fun sendCommand(action: String, extras: Intent.() -> Unit = {}) {
        val intent = Intent(context, AudioPlayerService::class.java).apply {
            this.action = action
            extras()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun play(url: String) = sendCommand(Constants.ACTION_PLAY) {
        putExtra(Constants.EXTRA_AUDIO_URL, url)
    }

    fun resume() = sendCommand(Constants.ACTION_PLAY)

    fun pause() = sendCommand(Constants.ACTION_PAUSE)

    fun seekTo(ms: Int) = sendCommand(Constants.ACTION_SEEK) {
        putExtra(Constants.EXTRA_SEEK_POSITION, ms)
    }

    fun changeSpeed(speed: Float) = sendCommand(Constants.ACTION_CHANGE_SPEED) {
        putExtra(Constants.EXTRA_SPEED, speed)
    }
}