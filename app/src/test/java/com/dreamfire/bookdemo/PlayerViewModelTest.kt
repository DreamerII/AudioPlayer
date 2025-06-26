package com.dreamfire.bookdemo

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.dreamfire.bookdemo.player.AudioPlayerController
import com.dreamfire.bookdemo.presentation.PlayerViewModel
import com.dreamfire.bookdemo.utils.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val mockUseCase: AudioPlayerController = mock()
    private val application: Application = mock()
    private lateinit var viewModel: PlayerViewModel

    @Before
    fun createViewModel() {
        viewModel = PlayerViewModel(application, mockUseCase)
    }

    @Test
    fun togglePlaybackToPlay() = runTest {
        viewModel.playerStateLiveData.value = false

        viewModel.togglePlayback()
        // run any pending coroutines (e.g. init observers)
        testDispatcher.scheduler.advanceUntilIdle()

        verify(mockUseCase).play(any()) // played with whichever URL
        Assert.assertTrue(viewModel.isLoading.value == true)
    }

    @Test
    fun togglePlaybackToPause() = runTest {
        viewModel.playerStateLiveData.value = true
        viewModel.togglePlayback()
        verify(mockUseCase).pause()
        Assert.assertFalse(viewModel.playerStateLiveData.value == true)
    }

    @Test
    fun togglePlaybackToResume() = runTest {
        viewModel.playerStateLiveData.value = false
        // Play
        viewModel.togglePlayback()
        viewModel.playerStateLiveData.value = true
        // Pause
        viewModel.togglePlayback()
        reset(mockUseCase)

        // Resume
        viewModel.togglePlayback()
        verify(mockUseCase).resume()
    }

    @Test
    fun nextChapter() = runTest {
        viewModel.currentIndexLiveDta.value = 0
        viewModel.nextChapter()
        testDispatcher.scheduler.advanceUntilIdle()

        Assert.assertEquals(1, viewModel.currentIndexLiveDta.value)
        verify(mockUseCase).play(Constants.BOOK_BASE_URL + Constants.BOOK_CHAPTER_2)
    }

    @Test
    fun previousChapter() = runTest {
        viewModel.currentIndexLiveDta.value = 1
        viewModel.previousChapter()
        testDispatcher.scheduler.advanceUntilIdle()

        Assert.assertEquals(0, viewModel.currentIndexLiveDta.value)
        verify(mockUseCase).play(Constants.BOOK_BASE_URL + Constants.BOOK_CHAPTER_1)
    }

    @Test
    fun increaseSpeed() = runTest {
        viewModel.increaseSpeed()
        verify(mockUseCase).changeSpeed(1.5f)
    }

    @Test
    fun decreaseSpeed() = runTest {
        viewModel.increaseSpeed()
        reset(mockUseCase)

        viewModel.decreaseSpeed()
        verify(mockUseCase).changeSpeed(1f)
    }

    @Test
    fun seek() {
        viewModel.seek(5000)
        verify(mockUseCase).seekTo(5000)
    }
}