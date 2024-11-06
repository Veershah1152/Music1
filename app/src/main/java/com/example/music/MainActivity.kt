package com.example.music

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.res.AssetFileDescriptor
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.os.Bundle
import android.os.IBinder
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.music.Adopter.MusicAdopter
import com.example.music.Data.MusicModel
import com.example.music.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(), MusicAdopter.SongClick {
    private lateinit var binding: ActivityMainBinding
    private lateinit var musicAdopter: MusicAdopter
    private var musicService: MusicService? = null
    private var isMusicPlayer = false
    private var running = 0 // Initialize directly

    private val connection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val musicBinder = binder as MusicService.MusicBinder
            musicService = musicBinder.getService()
            // Ensure that initialiseSeekBar exists in MusicService
            musicService?.initialiseSeekBar(binding.seekBar2)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            musicService = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val list: List<MusicModel> = getSongList()
        binding.rv.layoutManager = LinearLayoutManager(this)
        musicAdopter = MusicAdopter(this, list, this)
        binding.rv.adapter = musicAdopter

        binding.play.setOnClickListener {
            if (isMusicPlayer) {
                pause()
            } else {
                play()
            }
        }

        binding.back.setOnClickListener {
            playBack()
        }

        binding.playNext.setOnClickListener {
            playNext()
        }
    }

    private fun playBack() {
        val list: List<MusicModel> = getSongList()
        running = if (running - 1 >= 0) {
            running - 1
        } else {
            list.size - 1
        }
        onSongClick(list[running], running)
    }

    private fun playNext() {
        val list: List<MusicModel> = getSongList()
        running = if (running + 1 < list.size) {
            running + 1
        } else {
            0
        }
        onSongClick(list[running], running)
    }

    private fun play() {
        if (!isMusicPlayer) {
            musicService?.play()
            isMusicPlayer = true
            binding.play.setImageResource(R.drawable.pause) // Should be play icon when paused
        }
    }

    private fun pause() {
        if (isMusicPlayer) {
            musicService?.pause()
            isMusicPlayer = false
            binding.play.setImageResource(R.drawable.pause) // Should be play icon when paused
        }
    }

    private fun getSongList(): List<MusicModel> {
        return listOf(
            MusicModel(R.raw.one, getSongTitle(R.raw.one), getMp3FileLength(R.raw.one)),
            MusicModel(R.raw.second, getSongTitle(R.raw.second), getMp3FileLength(R.raw.second))
        )
    }

    override fun onSongClick(music: MusicModel, position: Int) {
        val musicIntent = Intent(this, MusicService::class.java).apply {
            putExtra("music_file_path", "android.resource://${packageName}/${music.resourseId}")
        }
        startService(musicIntent)
        bindService(musicIntent, connection, Context.BIND_AUTO_CREATE)
    }

    private fun getSongTitle(mp3R: Int): String {
        val retriever = MediaMetadataRetriever()
        val fileDes: AssetFileDescriptor = resources.openRawResourceFd(mp3R)
        retriever.setDataSource(fileDes.fileDescriptor, fileDes.startOffset, fileDes.length)
        val title: String? = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
        retriever.release()
        return title ?: ""
    }

    private fun getMp3FileLength(mp3ResourceId: Int): String {
        val retriever = MediaMetadataRetriever()
        val fileDes: AssetFileDescriptor = resources.openRawResourceFd(mp3ResourceId)
        retriever.setDataSource(fileDes.fileDescriptor, fileDes.startOffset, fileDes.length)
        val duration: Long = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0
        retriever.release()
        val min: Long = duration / 1000 / 60
        val sec: Long = duration / 1000 % 60
        return "$min:${String.format("%02d", sec)}"
    }
}