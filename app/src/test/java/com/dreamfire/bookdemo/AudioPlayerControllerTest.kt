package com.dreamfire.bookdemo

//import com.nhaarman.mockitokotlin2.argumentCaptor
//import com.nhaarman.mockitokotlin2.mock
//import com.nhaarman.mockitokotlin2.verify
import android.content.Context
import android.content.Intent
import com.dreamfire.bookdemo.player.AudioPlayerController
import com.dreamfire.bookdemo.utils.Constants
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.verify
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AudioPlayerControllerTest {

    private lateinit var context: Context
    private lateinit var controller: AudioPlayerController

    @Before
    fun setup() {
        context = mock()
        controller = AudioPlayerController(context)
    }

    @Test
    fun play() {
        val url = "https://example.com/audio.mp3"
        controller.play(url)

        // capture the Intent passed to startService
        val intentCaptor = argumentCaptor<Intent>()
        verify(context).startService(intentCaptor.capture())

        val sent = intentCaptor.firstValue
        assert(sent.action == Constants.ACTION_PLAY)
        assert(sent.getStringExtra(Constants.EXTRA_AUDIO_URL) == url)
    }

    @Test
    fun resume() {
        controller.resume()

        val intentCaptor = argumentCaptor<Intent>()
        verify(context).startService(intentCaptor.capture())

        val sent = intentCaptor.firstValue
        assert(sent.action == Constants.ACTION_PLAY)
        assert(sent.extras == null || sent.extras!!.isEmpty)
    }

    @Test
    fun pause() {
        controller.pause()

        val intentCaptor = argumentCaptor<Intent>()
        verify(context).startService(intentCaptor.capture())

        assert(intentCaptor.firstValue.action == Constants.ACTION_PAUSE)
    }

    @Test
    fun seekTo() {
        controller.seekTo(1234)

        val captor = argumentCaptor<Intent>()
        verify(context).startService(captor.capture())

        val sent = captor.firstValue
        assert(sent.action == Constants.ACTION_SEEK)
        assert(sent.getIntExtra(Constants.EXTRA_SEEK_POSITION, -1) == 1234)
    }

    @Test
    fun changeSpeed() {
        controller.changeSpeed(1.5f)

        val captor = argumentCaptor<Intent>()
        verify(context).startService(captor.capture())

        val sent = captor.firstValue
        assert(sent.action == Constants.ACTION_CHANGE_SPEED)
        assert(sent.getFloatExtra(Constants.EXTRA_SPEED, -1f) == 1.5f)
    }
}