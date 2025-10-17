package com.musa.poetmusic

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.bumptech.glide.Glide

class MainActivity : AppCompatActivity() {

    private lateinit var player: ExoPlayer
    private lateinit var currentSong: Song
    private var isPlaying = false

    // UI Elements
    private lateinit var searchPlaylist: androidx.appcompat.widget.AppCompatEditText
    private lateinit var albumArt: ImageView
    private lateinit var artistName: TextView
    private lateinit var songTitle: TextView
    private lateinit var btnPlayPause: ImageButton
    private lateinit var btnPrevious: ImageButton
    private lateinit var btnNext: ImageButton
    private lateinit var btnRepeat: ImageButton
    private lateinit var btnShuffle: ImageButton
    private lateinit var recyclerViewSongs: RecyclerView

    private val songs = mutableListOf<Song>()
    private lateinit var adapter: SongAdapter

    private val handler = Handler(Looper.getMainLooper())
    private val updateProgressRunnable = Runnable {
        updateProgress()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(com.musa.poetmusic.R.layout.activity_main)

        setupUI()
        setupExoPlayer()
        setupSongs()
        setupRecyclerView()
        setupControls()
    }

    private fun setupUI() {
        searchPlaylist = findViewById(com.musa.poetmusic.R.id.searchPlaylist)
        albumArt = findViewById(com.musa.poetmusic.R.id.albumArt)
        artistName = findViewById(com.musa.poetmusic.R.id.artistName)
        songTitle = findViewById(com.musa.poetmusic.R.id.songTitle)
        btnPlayPause = findViewById(com.musa.poetmusic.R.id.btnPlayPause)
        btnPrevious = findViewById(com.musa.poetmusic.R.id.btnPrevious)
        btnNext = findViewById(com.musa.poetmusic.R.id.btnNext)
        btnRepeat = findViewById(com.musa.poetmusic.R.id.btnRepeat)
        btnShuffle = findViewById(com.musa.poetmusic.R.id.btnShuffle)
        recyclerViewSongs = findViewById(com.musa.poetmusic.R.id.recyclerViewSongs)
    }

    private fun setupExoPlayer() {
        player = ExoPlayer.Builder(this).build()
        // You can optionally add PlayerView to layout if needed for progress bar
    }

    private fun setupSongs() {
        // Mock data - replace with real data from database or API
        repeat(5) {
            songs.add(
                Song(
                    id = it + 1,
                    title = "I'm The Sinner",
                    artist = "Jared Benjamin",
                    albumArtUrl = "https://via.placeholder.com/300?text=Album+Art", // Replace with actual URL or drawable
                    audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3" // Replace with actual audio URL or file path
                )
            )
        }
        currentSong = songs[0]
    }

    private fun setupRecyclerView() {
        recyclerViewSongs.layoutManager = LinearLayoutManager(this)
        adapter = SongAdapter(songs) { song ->
            playSong(song)
        }
        recyclerViewSongs.adapter = adapter
    }

    private fun setupControls() {
        btnPlayPause.setOnClickListener {
            if (isPlaying) {
                pauseSong()
            } else {
                playSong(currentSong)
            }
        }

        btnPrevious.setOnClickListener {
            val currentIndex = songs.indexOf(currentSong)
            if (currentIndex > 0) {
                playSong(songs[currentIndex - 1])
            }
        }

        btnNext.setOnClickListener {
            val currentIndex = songs.indexOf(currentSong)
            if (currentIndex < songs.size - 1) {
                playSong(songs[currentIndex + 1])
            }
        }

        btnRepeat.setOnClickListener {
            // Toggle repeat mode
            player.repeatMode = if (player.repeatMode == ExoPlayer.REPEAT_MODE_ONE) {
                ExoPlayer.REPEAT_MODE_OFF
            } else {
                ExoPlayer.REPEAT_MODE_ONE
            }
            updateRepeatIcon()
        }

        btnShuffle.setOnClickListener {
            // Toggle shuffle
            player.shuffleModeEnabled = !player.shuffleModeEnabled
            updateShuffleIcon()
        }

        // Update UI with current song info
        updateSongInfo(currentSong)
    }

    private fun playSong(song: Song) {
        currentSong = song
        updateSongInfo(song)

        // Prepare player
        val mediaItem = MediaItem.fromUri(song.audioUrl)
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()

        isPlaying = true
        btnPlayPause.setImageResource(com.musa.poetmusic.R.drawable.ic_pause)
        startProgressUpdate()
    }

    private fun pauseSong() {
        player.pause()
        isPlaying = false
        btnPlayPause.setImageResource(com.musa.poetmusic.R.drawable.ic_play)
        stopProgressUpdate()
    }

    private fun updateSongInfo(song: Song) {
        artistName.text = song.artist
        songTitle.text = song.title
        Glide.with(this)
            .load(song.albumArtUrl)
            .placeholder(com.musa.poetmusic.R.drawable.album_art_placeholder)
            .into(albumArt)
    }

    private fun updateProgress() {
        // You can implement progress bar here if needed
        handler.postDelayed(updateProgressRunnable, 1000)
    }

    private fun startProgressUpdate() {
        handler.post(updateProgressRunnable)
    }

    private fun stopProgressUpdate() {
        handler.removeCallbacks(updateProgressRunnable)
    }

    private fun updateRepeatIcon() {
        btnRepeat.setImageResource(
            if (player.repeatMode == ExoPlayer.REPEAT_MODE_ONE)
                com.musa.poetmusic.R.drawable.ic_repeat_on
            else
                com.musa.poetmusic.R.drawable.ic_repeat
        )
    }

    private fun updateShuffleIcon() {
        btnShuffle.setImageResource(
            if (player.shuffleModeEnabled)
                com.musa.poetmusic.R.drawable.ic_shuffle_on
            else
                com.musa.poetmusic.R.drawable.ic_shuffle
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        player.release()
        stopProgressUpdate()
    }
}