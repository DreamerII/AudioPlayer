package com.dreamfire.bookdemo.presentation

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.dreamfire.bookdemo.player.AudioPlayerController

class PlayerViewModelFactory(
    private val application: Application,
    private val audioPlayerController: AudioPlayerController
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PlayerViewModel::class.java)) {
            return PlayerViewModel(application, audioPlayerController) as T
        }
        throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
    }
}