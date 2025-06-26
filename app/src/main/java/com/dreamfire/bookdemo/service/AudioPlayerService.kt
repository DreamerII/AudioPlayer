package com.dreamfire.bookdemo.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.edit
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.media.app.NotificationCompat.MediaStyle
import com.dreamfire.bookdemo.R
import com.dreamfire.bookdemo.presentation.MainActivity
import com.dreamfire.bookdemo.presentation.model.PlaybackState
import com.dreamfire.bookdemo.utils.Constants
import com.dreamfire.bookdemo.utils.NetworkUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

class AudioPlayerService : Service() {
    private var mediaPlayer: MediaPlayer? = null
    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var notificationManager: NotificationManager
    private var currentSpeed = 1.0f
    private var cover: Bitmap? = null

    private val prefs by lazy {
        getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val binder = LocalBinder()

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()


        mediaSession = MediaSessionCompat(this, MEDIA_SESSION_TAG).apply {
            isActive = true
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            Constants.ACTION_PLAY -> {
                if (NetworkUtils().hasNetwork(this)) {
                    handlePlay(intent.getStringExtra(Constants.EXTRA_AUDIO_URL))
                } else {
                    handleStop()
                }
            }
            Constants.ACTION_PAUSE -> handlePause()
            Constants.ACTION_STOP -> handleStop()
            Constants.ACTION_SEEK -> {
                intent.getIntExtra(Constants.EXTRA_SEEK_POSITION, -1)
                    .takeIf { it >= 0 }?.let { handleSeek(it) }
            }

            Constants.ACTION_CHANGE_SPEED -> {
                handleChangeSpeed(intent.getFloatExtra(Constants.EXTRA_SPEED, 1f))
            }
        }
        return START_STICKY
    }

    private fun createMediaPlayer() {
        mediaPlayer = MediaPlayer().apply {
            setOnCompletionListener {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            setOnErrorListener { _, _, _ ->
                handleStop()
                true
            }
        }
    }

    private fun handlePlay(url: String?) {
        if (mediaPlayer == null) {
            createMediaPlayer()
        }
        if (!url.isNullOrEmpty()) {
            // Start playing
            mediaPlayer?.apply {
                reset()
                setDataSource(url)
                prepare()
                playbackParams = playbackParams.setSpeed(currentSpeed)
                start()
            }
        } else {
            // Resume playing
            val playbackParams = mediaPlayer?.playbackParams
            playbackParams?.let {
                mediaPlayer?.playbackParams = it.setSpeed(currentSpeed)
            }
            mediaPlayer?.start()
        }
        broadcastPlaybackState(true)
        updateNotificationAsync(true)
    }

    private fun handlePause() {
        mediaPlayer?.pause()
        broadcastPlaybackState(false)
        updateNotificationAsync(false)
    }

    private fun handleStop() {
        mediaPlayer?.run {
            stop()
            release()
        }
        mediaPlayer = null
        broadcastPlaybackState(false)
        serviceScope.launch {
            withContext(Dispatchers.Main) {
                stopForeground(true)
                stopSelf()
            }
        }
    }

    private fun handleSeek(position: Int) {
        mediaPlayer?.seekTo(position)
    }

    private fun handleChangeSpeed(speed: Float) {
        currentSpeed = speed
        prefs.edit { putFloat(Constants.EXTRA_SPEED, speed) }
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.let {
                    it.playbackParams = it.playbackParams.setSpeed(speed)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateNotificationAsync(isPlaying: Boolean) {
        serviceScope.launch {
            if (cover == null) {
                cover = loadCover()
            }
            val notification = buildNotification(cover, isPlaying)
            withContext(Dispatchers.Main) {
                startForeground(Constants.NOTIFICATION_ID, notification)
            }
        }
    }

    private fun loadCover(): Bitmap? = try {
        URL(Constants.BOOK_BASE_URL + Constants.BOOK_COVER_NAME).openConnection().run {
            connect(); BitmapFactory.decodeStream(inputStream)
        }
    } catch (_: Exception) {
        null
    }

    private fun buildNotification(cover: Bitmap?, isPlaying: Boolean): Notification {
        val playPauseAction = if (isPlaying) {
            getPauseAction()
        } else {
            getPlayAction()
        }

        val stopAction = NotificationCompat.Action(
            android.R.drawable.ic_menu_close_clear_cancel,
            getString(R.string.stop_label),
            PendingIntent.getService(
                this,
                3,
                Intent(this, AudioPlayerService::class.java).apply {
                    action = Constants.ACTION_STOP
                },
                PendingIntent.FLAG_IMMUTABLE
            )
        )

        val mediaStyle = MediaStyle()
            .setMediaSession(mediaSession.sessionToken)
            .setShowActionsInCompactView(0, 1)

        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(Constants.EXTRA_IS_PLAYING, isPlaying)
        }
        val contentPending = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.content_summary))
            .setContentText(getString(R.string.content_summary_description))
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setLargeIcon(cover)
            .setContentIntent(contentPending)
            .addAction(playPauseAction)
            .addAction(stopAction)
            .setStyle(mediaStyle)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(isPlaying)
            .build()
    }

    private fun getPlayAction(): NotificationCompat.Action {
        return NotificationCompat.Action(
            android.R.drawable.ic_media_play,
            getString(R.string.play_label),
            PendingIntent.getService(
                this, 2,
                Intent(this, AudioPlayerService::class.java).apply {
                    action = Constants.ACTION_PLAY
                },
                PendingIntent.FLAG_IMMUTABLE
            )
        )
    }

    private fun getPauseAction(): NotificationCompat.Action {
        return NotificationCompat.Action(
            android.R.drawable.ic_media_pause,
            getString(R.string.pause_label),
            PendingIntent.getService(
                this, 1,
                Intent(this, AudioPlayerService::class.java).apply {
                    action = Constants.ACTION_PAUSE
                },
                PendingIntent.FLAG_IMMUTABLE
            )
        )
    }

    private fun broadcastPlaybackState(isPlaying: Boolean) {
        val stateIntent = Intent(Constants.ACTION_PLAYBACK_STATE).apply {
            putExtra(Constants.EXTRA_IS_PLAYING, isPlaying)
        }
        LocalBroadcastManager.getInstance(this)
            .sendBroadcast(stateIntent)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel(
                Constants.NOTIFICATION_CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).also { notificationManager.createNotificationChannel(it) }
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        mediaPlayer?.release()
        super.onDestroy()
    }

    fun getCurrentPosition(): Int = mediaPlayer?.currentPosition ?: 0

    fun getDuration(): Int = mediaPlayer?.duration ?: 0

    fun getPlayBackState(): PlaybackState {
        return if (mediaPlayer?.isPlaying == true) {
            PlaybackState.PLAYING
        } else if (mediaPlayer != null) {
            PlaybackState.PAUSED
        } else {
            PlaybackState.STOPPED
        }
    }

    override fun onBind(intent: Intent?): IBinder = binder

    inner class LocalBinder : Binder() {
        fun getService(): AudioPlayerService = this@AudioPlayerService
    }

    companion object {
        const val MEDIA_SESSION_TAG = "BookAudioSession"
        const val CHANNEL_NAME = "Audio Playback"
    }
}