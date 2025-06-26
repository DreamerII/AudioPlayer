package com.dreamfire.bookdemo.presentation

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.dreamfire.bookdemo.R
import com.dreamfire.bookdemo.databinding.FragmentPlayerBinding
import com.dreamfire.bookdemo.player.AudioPlayerController
import com.dreamfire.bookdemo.presentation.model.PlaybackState
import com.dreamfire.bookdemo.service.AudioPlayerService
import com.dreamfire.bookdemo.utils.Constants

class PlayerFragment : Fragment() {
    private var _binding: FragmentPlayerBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: PlayerViewModel
    private val handler = Handler(Looper.getMainLooper())

    private var audioService: AudioPlayerService? = null
    private var isServiceBound = false

    private val playbackStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val isPlaying = intent
                ?.getBooleanExtra(Constants.EXTRA_IS_PLAYING, false) ?: false
            viewModel.playerStateLiveData.postValue(isPlaying)
        }
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val localBinder = binder as AudioPlayerService.LocalBinder
            audioService = localBinder.getService()
            isServiceBound = true
            setState()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isServiceBound = false
            audioService = null
        }
    }

    override fun onStart() {
        super.onStart()
        Intent(requireContext(), AudioPlayerService::class.java).also { intent ->
            requireContext().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onStop() {
        super.onStop()
        if (isServiceBound) {
            requireContext().unbindService(serviceConnection)
            isServiceBound = false
        }
    }

    private val updateProgressTask = object : Runnable {
        override fun run() {
            setState()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlayerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onResume() {
        setState()
        super.onResume()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Create player controller
        val controller = AudioPlayerController(requireContext())

        // Create ViewModelFactory
        val factory = PlayerViewModelFactory(
            application = requireActivity().application,
            audioPlayerController = controller,
        )

        // Create ViewModel
        viewModel = ViewModelProvider(requireActivity(), factory)[PlayerViewModel::class.java]
        // Init ViewModel
        viewModel.init()

        handler.post(updateProgressTask)

        with(binding) {
            // Observe state
            viewModel.playerStateLiveData.observe(viewLifecycleOwner) { isPlaying ->
                progress.visibility = View.GONE
                playBtn.visibility = View.VISIBLE
                playBtn.setImageResource(
                    if (isPlaying) {
                        R.drawable.ic_pause
                    } else {
                        R.drawable.ic_play_arrow
                    }
                )
                if (isPlaying) {
                    handler.post(updateProgressTask)
                } else {
                    handler.removeCallbacks(updateProgressTask)
                }
            }

            viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
                progress.visibility = if (loading) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
                playBtn.visibility = if (loading) {
                    View.INVISIBLE
                } else {
                    View.VISIBLE
                }
            }

            viewModel.coverLiveData.observe(viewLifecycleOwner) { state ->
                coverProgressBar.visibility = if (state.isLoading) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
                coverIv.visibility = if (state.cover != null) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
                if (state.cover != null) {
                    coverIv.setImageBitmap(state.cover)
                } else {
                    coverIv.setImageResource(R.drawable.book_cover_placeholder)
                }
            }

            // Buttons
            playBtn.setOnClickListener { viewModel.togglePlayback() }
            btnNextChapter.setOnClickListener { viewModel.nextChapter() }
            btnPrevChapter.setOnClickListener { viewModel.previousChapter() }
            btnIncreaseSpeed.setOnClickListener { viewModel.increaseSpeed() }
            btnDecreaseSpeed.setOnClickListener { viewModel.decreaseSpeed() }

            // SeekBar
            binding.durationSeekBar.setOnSeekBarChangeListener(object :
                SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    if (fromUser) {
                        viewModel.seek(progress)
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }

        // Register receiver
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(
            playbackStateReceiver,
            IntentFilter(Constants.ACTION_PLAYBACK_STATE)
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        LocalBroadcastManager.getInstance(requireContext())
            .unregisterReceiver(playbackStateReceiver)
        handler.removeCallbacks(updateProgressTask)
        _binding = null
    }

    private fun setState() {
        with(binding) {
            val current = audioService?.getCurrentPosition() ?: 0
            val max = audioService?.getDuration() ?: 0
            durationSeekBar.max = max
            durationSeekBar.progress = current
            tvElapsed.text = formatTime(current)
            val delta = max - current
            val formatted = formatTime(delta)
            tvRemaining.text = getString(R.string.time_left_format, formatted)

            durationSeekBar.max = audioService?.getDuration() ?: 0
            durationSeekBar.progress = audioService?.getCurrentPosition() ?: 0
            viewModel.playbackState = audioService?.getPlayBackState() ?: PlaybackState.STOPPED
            viewModel.playerStateLiveData.postValue(viewModel.playbackState == PlaybackState.PLAYING)
        }
    }

    private fun formatTime(ms: Int): String {
        val sec = ms / 1000
        val minutes = sec / 60
        val seconds = sec % 60
        return "%02d:%02d".format(minutes, seconds)
    }
}