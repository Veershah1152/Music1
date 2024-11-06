package com.example.music

import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Binder
import android.os.IBinder
import android.widget.SeekBar

class MusicService : Service() {
    private var mediaPlayer: MediaPlayer? = null
    private var filePath: String? = null
    private var isPaused: Boolean = false
    private val binder = MusicBinder()
    private var appStarted = true
    private var isDragging = false // Make this mutable

    override fun onCreate() {
        super.onCreate()
        mediaPlayer = MediaPlayer()
        mediaPlayer?.setOnCompletionListener { stopSelf() }
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        filePath = intent?.getStringExtra("music_file_path")
        filePath?.let {
            mediaPlayer?.reset()
            mediaPlayer?.setDataSource(applicationContext, Uri.parse(filePath))
            mediaPlayer?.prepare()
            if (!appStarted) {
                mediaPlayer?.start()
            }
            appStarted = false
        }
        return START_STICKY
    }

    override fun onDestroy() {
        mediaPlayer?.release()
        super.onDestroy()
    }

    fun pause() {
        mediaPlayer?.pause()
        isPaused = true
    }

    fun play() {
        if (isPaused) {
            mediaPlayer?.start()
            isPaused = false
        } else {
            mediaPlayer?.reset()
            mediaPlayer?.setDataSource(applicationContext, Uri.parse(filePath))
            mediaPlayer?.prepare()
            mediaPlayer?.start()
        }
    }

    inner class MusicBinder : Binder() {
        fun getService(): MusicService {
            return this@MusicService
        }
    }

    fun initialiseSeekBar(seekBar: SeekBar) {
        mediaPlayer?.let {
            // Set the maximum value of the SeekBar to the duration of the media
            seekBar.max = it.duration

            // Start a background thread to update the SeekBar progress
            Thread {
                while (it.isPlaying) {
                    try {
                        val currentPosition = it.currentPosition
                        if (!isDragging) {
                            seekBar.post {
                                seekBar.progress = currentPosition
                            }
                        }
                        Thread.sleep(1000)
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }
                }
            }.start()

            // Set the OnSeekBarChangeListener to handle user input
            seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    // Optionally handle real-time updates if needed
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    // Set isDragging to true when the user starts dragging
                    isDragging = true
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    // Seek to the new position when the user stops dragging
                    seekBar?.let {
                        mediaPlayer?.seekTo(it.progress)
                    }
                    // Set isDragging to false when the user stops dragging
                    isDragging = false
                }
            })
        }
    }
}