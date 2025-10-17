package com.musa.poetmusic

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class MainActivity : AppCompatActivity() {

    private lateinit var player: ExoPlayer
    private var currentSong: Song? = null
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
    private var shuffledSongs = mutableListOf<Song>()
    private lateinit var adapter: SongAdapter
    private lateinit var mediaSession: MediaSession

    private var isShuffleEnabled = false
    private var currentMediaId: String? = null
    private var repeatMode = ExoPlayer.REPEAT_MODE_OFF

    private val handler = Handler(Looper.getMainLooper())
    private val updateProgressRunnable = Runnable {
        updateProgress()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupUI()
        setupExoPlayer()
        setupRecyclerView()
        setupControls()
        requestPermission()
    }

    private fun setupUI() {
        searchPlaylist = findViewById(R.id.searchPlaylist)
        albumArt = findViewById(R.id.albumArt)
        artistName = findViewById(R.id.artistName)
        songTitle = findViewById(R.id.songTitle)
        btnPlayPause = findViewById(R.id.btnPlayPause)
        btnPrevious = findViewById(R.id.btnPrevious)
        btnNext = findViewById(R.id.btnNext)
        btnRepeat = findViewById(R.id.btnRepeat)
        btnShuffle = findViewById(R.id.btnShuffle)
        recyclerViewSongs = findViewById(R.id.recyclerViewSongs)
    }

    private fun setupExoPlayer() {
        player = ExoPlayer.Builder(this).build()
        mediaSession = MediaSession.Builder(this, player).build()
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) {
                    isPlaying = false
                    btnPlayPause.setImageResource(R.drawable.ic_play)
                }
            }
        })
    }

    private fun loadSongs() {
        songs.clear()
        songs.addAll(SongRepository.getAllSongs(this))
        adapter.notifyDataSetChanged()

        if (songs.isNotEmpty()) {
            currentSong = songs[0]
            updateSongInfo(currentSong!!)
        }
    }

    private fun setupRecyclerView() {
        recyclerViewSongs.layoutManager = LinearLayoutManager(this)
        adapter = SongAdapter(songs, { song ->
            playSong(song)
        }, null)
        recyclerViewSongs.adapter = adapter
    }

    private fun getCurrentPlaylist(): List<Song> = if (isShuffleEnabled) shuffledSongs else songs

    private fun setupControls() {
        btnPlayPause.setOnClickListener {
            currentSong?.let {
                if (isPlaying) {
                    pauseSong()
                } else {
                    playSong(it)
                }
            }
        }

        btnPrevious.setOnClickListener {
            val playlist = getCurrentPlaylist()
            val currentIndex = playlist.indexOf(currentSong)
            if (currentIndex > 0) {
                playSong(playlist[currentIndex - 1])
            }
        }

        btnNext.setOnClickListener {
            val playlist = getCurrentPlaylist()
            val currentIndex = playlist.indexOf(currentSong)
            if (currentIndex != -1 && currentIndex < playlist.size - 1) {
                playSong(playlist[currentIndex + 1])
            }
        }

        btnRepeat.setOnClickListener {
            repeatMode = when (repeatMode) {
                ExoPlayer.REPEAT_MODE_OFF -> ExoPlayer.REPEAT_MODE_ALL
                ExoPlayer.REPEAT_MODE_ALL -> ExoPlayer.REPEAT_MODE_ONE
                ExoPlayer.REPEAT_MODE_ONE -> ExoPlayer.REPEAT_MODE_OFF
                else -> ExoPlayer.REPEAT_MODE_OFF
            }
            player.repeatMode = repeatMode
            updateRepeatIcon()
        }

        btnShuffle.setOnClickListener {
            isShuffleEnabled = !isShuffleEnabled
            updateShuffleIcon()

            if (isShuffleEnabled) {
                shuffledSongs = songs.shuffled().toMutableList()
            } else {
                shuffledSongs = songs.toMutableList()
            }

            adapter = SongAdapter(getCurrentPlaylist(), { song -> playSong(song) }, currentSong?.id)
            recyclerViewSongs.adapter = adapter

            val newIndex = getCurrentPlaylist().indexOf(currentSong)
            if (newIndex != -1) {
                recyclerViewSongs.scrollToPosition(newIndex)
            }
        }
    }

    private fun playSong(song: Song) {
        if (currentMediaId != song.audioUrl) {
            currentMediaId = song.audioUrl
            currentSong = song
            updateSongInfo(song)

            player.setMediaItem(MediaItem.fromUri(Uri.parse("file://${song.audioUrl}")))
            player.prepare()
        }

        player.play()
        isPlaying = true
        btnPlayPause.setImageResource(R.drawable.ic_pause)
        startProgressUpdate()

        // Highlight currently playing song
        adapter.updateNowPlaying(song.id)
    }

    private fun pauseSong() {
        player.pause()
        isPlaying = false
        btnPlayPause.setImageResource(R.drawable.ic_play)
        stopProgressUpdate()
    }

    private fun updateSongInfo(song: Song) {
        artistName.text = song.artist
        songTitle.text = song.title
        Glide.with(this)
            .load(song.albumArtUrl)
            .placeholder(R.drawable.album_art_placeholder)
            .error(R.drawable.album_art_placeholder)
            .into(albumArt)
    }

    private fun updateProgress() {
        handler.postDelayed(updateProgressRunnable, 1000)
    }

    private fun startProgressUpdate() {
        handler.post(updateProgressRunnable)
    }

    private fun stopProgressUpdate() {
        handler.removeCallbacks(updateProgressRunnable)
    }

    private fun updateRepeatIcon() {
        val (icon, message) = when (repeatMode) {
            ExoPlayer.REPEAT_MODE_ALL -> {
                R.drawable.ic_repeat_all_on to "Repeat all"
            }
            ExoPlayer.REPEAT_MODE_ONE -> {
                R.drawable.ic_repeat_one_on to "Repeat one"
            }
            else -> {
                R.drawable.ic_repeat to "Repeat off"
            }
        }
        btnRepeat.setImageResource(icon)

        // Show toast at bottom center
        val toast = Toast.makeText(this, message, Toast.LENGTH_SHORT)
        toast.setGravity(android.view.Gravity.BOTTOM or android.view.Gravity.CENTER_HORIZONTAL, 0, 120) // 120px from bottom
        toast.show()
    }

    private fun updateShuffleIcon() {
        btnShuffle.setImageResource(
            if (isShuffleEnabled) R.drawable.ic_shuffle_on
            else R.drawable.ic_shuffle
        )
    }

    private fun requestPermission() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(permission), 100)
        } else {
            loadSongs()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            loadSongs()
        } else {
            Toast.makeText(this, "Permission required to access music", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        player.release()
        mediaSession.release()
        stopProgressUpdate()
    }
}