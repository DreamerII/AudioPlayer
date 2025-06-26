package com.dreamfire.bookdemo.presentation

import android.app.Application
import android.content.Context
import android.graphics.BitmapFactory
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.dreamfire.bookdemo.player.AudioPlayerController
import com.dreamfire.bookdemo.presentation.model.CoverStateModel
import com.dreamfire.bookdemo.presentation.model.PlaybackState
import com.dreamfire.bookdemo.utils.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

class PlayerViewModel(
    application: Application,
    private val playerController: AudioPlayerController,
) : AndroidViewModel(application) {

    // Chapters URLs
    private val chapters = listOf(
        Constants.BOOK_BASE_URL + Constants.BOOK_CHAPTER_1,
        Constants.BOOK_BASE_URL + Constants.BOOK_CHAPTER_2,
        Constants.BOOK_BASE_URL + Constants.BOOK_CHAPTER_3
    )
    // Current chapter index
    val currentIndexLiveDta = MutableLiveData(0)
    // Current chapter URL
    private val currentUrl: LiveData<String>
        get() = currentIndexLiveDta.map { idx ->
            chapters.getOrNull(idx) ?: chapters.first()
        }

    // Playback state
    val playerStateLiveData = MutableLiveData(false)
    val isLoading = MutableLiveData(false)

    private val prefs by lazy {
        application.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
    }

    // Speed control
    private val speeds = listOf(1f, 1.5f, 2f, 2.5f, 4f)
    private val _speedIndex = MutableLiveData(0)

    val coverLiveData = MutableLiveData<CoverStateModel>()
    var playbackState: PlaybackState = PlaybackState.STOPPED

    fun init() {
        playerStateLiveData.observeForever { playing ->
            if (playing) isLoading.postValue(false)
        }
        viewModelScope.launch {
            loadCover()
        }
        initPlayerSpeed()
    }

    fun togglePlayback() {
        if (playerStateLiveData.value == true) {
            pause()
        } else {
            if (playbackState == PlaybackState.PAUSED) {
                resume()
            } else {
                play()
            }
        }
    }

    fun nextChapter() {
        val next = (currentIndexLiveDta.value ?: 0) + 1
        if (next < chapters.size) {
            currentIndexLiveDta.value = next
            play()
        }
    }

    fun previousChapter() {
        val prev = (currentIndexLiveDta.value ?: 0) - 1
        if (prev >= 0) {
            currentIndexLiveDta.value = prev
            play()
        }
    }

    fun increaseSpeed() {
        val idx = _speedIndex.value ?: 0
        if (idx < speeds.lastIndex) {
            _speedIndex.value = idx + 1
            playerController.changeSpeed(speeds[idx + 1])
        }
    }

    fun decreaseSpeed() {
        val idx = _speedIndex.value ?: 0
        if (idx > 0) {
            _speedIndex.value = idx - 1
            playerController.changeSpeed(speeds[idx - 1])
        }
    }

    fun seek(positionMs: Int) {
        playerController.seekTo(positionMs)
    }

    private fun initPlayerSpeed() {
        val speed = prefs.getFloat(Constants.EXTRA_SPEED, 1f)
        val speedIndex = speeds.indexOfFirst { it == speed }.takeIf { it >= 0 } ?: 0
        _speedIndex.postValue(speedIndex)
    }

    private fun play() {
        isLoading.value = true
        playbackState = PlaybackState.PLAYING
        playerController.play(currentUrl.value ?: return)
    }

    private fun resume() {
        playbackState = PlaybackState.PLAYING
        playerController.resume()
    }

    private fun pause() {
        playbackState = PlaybackState.PAUSED
        playerController.pause()
        playerStateLiveData.postValue(false)
    }

    private suspend fun loadCover() {
        // Initial loading event for book cover
        coverLiveData.postValue(CoverStateModel(isLoading = true))
        try {
            withContext(Dispatchers.IO) {
                val url = URL(Constants.BOOK_BASE_URL + Constants.BOOK_COVER_NAME)
                val conn = url.openConnection().apply { connect() }
                val bmp = conn.getInputStream().use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
                // Post the loaded cover bitmap
                coverLiveData.postValue(CoverStateModel(cover = bmp, isLoading = false))
            }
        } catch (e: Exception) {
            // Hide loading in case of error and show cover placeholder
            coverLiveData.postValue(CoverStateModel(isLoading = false))
        }
    }
}