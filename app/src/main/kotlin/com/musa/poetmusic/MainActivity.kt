package com.musa.poetmusic

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.textfield.TextInputEditText

class MainActivity : AppCompatActivity() {

    private lateinit var player: ExoPlayer
    private var currentSong: Song? = null
    private var isPlaying = false

    // UI Elements
    private lateinit var searchPlaylist: TextInputEditText
    private lateinit var albumArt: ImageView
    private lateinit var artistName: TextView
    private lateinit var songTitle: TextView
    private lateinit var btnPlayPause: ImageButton
    private lateinit var btnPrevious: ImageButton
    private lateinit var btnNext: ImageButton
    private lateinit var btnRepeat: ImageButton
    private lateinit var btnShuffle: ImageButton
    private lateinit var recyclerViewSongs: RecyclerView
    private lateinit var searchResultsOverlay: RecyclerView
    private lateinit var searchResultsAdapter: SongAdapter
    private lateinit var progressBar: ProgressBar

    private val allSongs = mutableListOf<Song>()
    private var shuffledSongs = mutableListOf<Song>()
    private lateinit var adapter: SongAdapter
    private lateinit var mediaSession: MediaSession

    private var isShuffleEnabled = false
    private var currentMediaId: String? = null
    private var repeatMode = ExoPlayer.REPEAT_MODE_OFF

    private val handler = Handler(Looper.getMainLooper())
    private val updateProgressRunnable = object : Runnable {
        override fun run() {
            updateProgress()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_main)

        setupUI()
        setupExoPlayer()
        setupRecyclerView()
        setupControls()
        setupProgressBarSeeking()
        requestPermission()

        val rootView = findViewById<View>(android.R.id.content)
        rootView.setupHideKeyboardOnTouch()
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
        searchResultsOverlay = findViewById(R.id.searchResultsOverlay)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupExoPlayer() {
        player = ExoPlayer.Builder(this).build()
        mediaSession = MediaSession.Builder(this, player).build()
        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                super.onMediaItemTransition(mediaItem, reason)
                if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED) {
                    val currentIndex = player.currentMediaItemIndex
                    val playlist = getCurrentPlaylist()
                    if (currentIndex in playlist.indices) {
                        val newSong = playlist[currentIndex]
                        currentSong = newSong
                        updateSongInfo(newSong)
                        adapter.updateNowPlaying(newSong.id)
                        // Progress bar will auto-update via existing listener
                    }
                }
            }

            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_ENDED -> {
                        // Only happens if REPEAT_MODE_OFF and last song ends
                        isPlaying = false
                        btnPlayPause.setImageResource(R.drawable.ic_play)
                        stopProgressUpdate()
                    }
                }
            }
        })
    }

    private fun loadSongs() {
        allSongs.clear()
        allSongs.addAll(SongRepository.getAllSongs(this))
        // Reset shuffled list
        shuffledSongs = allSongs.toMutableList()
        // Setup adapters
        adapter = SongAdapter(getCurrentPlaylist(), ::playSong, null)
        recyclerViewSongs.adapter = adapter
        // ⚠️ CRITICAL: Prepare full playlist in ExoPlayer
        preparePlaylist()
        // Play first song if available
        if (allSongs.isNotEmpty()) {
            playSong(allSongs[0])
        }
    }

    private fun setupRecyclerView() {
        recyclerViewSongs.layoutManager = LinearLayoutManager(this)
        adapter = SongAdapter(allSongs, { song ->
            playSong(song)
        }, null)
        recyclerViewSongs.adapter = adapter
    }

    private fun setupSearch() {
        searchResultsAdapter = SongAdapter(mutableListOf(), { song ->
            playSong(song)
            searchResultsOverlay.visibility = View.GONE
            searchPlaylist.hideKeyboard()
            searchPlaylist.setText("")
            searchPlaylist.clearFocus()
        }, null)
        searchResultsOverlay.adapter = searchResultsAdapter
        searchResultsOverlay.layoutManager = LinearLayoutManager(this)

        searchPlaylist.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString()?.trim() ?: ""
                if (query.isEmpty()) {
                    searchResultsOverlay.visibility = View.GONE
                } else {
                    val results = allSongs.filter { song ->
                        song.title.contains(query, ignoreCase = true) ||
                                song.artist.contains(query, ignoreCase = true)
                    }
                    searchResultsAdapter = SongAdapter(results, { song ->
                        playSong(song)
                        searchResultsOverlay.visibility = View.GONE
                        searchPlaylist.hideKeyboard()
                        searchPlaylist.setText("")
                        searchPlaylist.clearFocus()
                    }, null)
                    searchResultsOverlay.adapter = searchResultsAdapter
                    searchResultsOverlay.visibility = View.VISIBLE
                }
            }
        })
    }

    private fun getCurrentPlaylist(): List<Song> = if (isShuffleEnabled) shuffledSongs else allSongs

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

        btnNext.setOnClickListener { player.seekToNext() }
        btnPrevious.setOnClickListener { player.seekToPrevious() }

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
                shuffledSongs = allSongs.shuffled().toMutableList()
            } else {
                shuffledSongs = allSongs.toMutableList()
            }

            adapter = SongAdapter(getCurrentPlaylist(), { song -> playSong(song) }, currentSong?.id)
            recyclerViewSongs.adapter = adapter

            val newIndex = getCurrentPlaylist().indexOf(currentSong)
            if (newIndex != -1) {
                recyclerViewSongs.scrollToPosition(newIndex)
            }

            val message = if (isShuffleEnabled) "Shuffle on" else "Shuffle off"
            val toast = Toast.makeText(this, message, Toast.LENGTH_SHORT)
            toast.setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 0, 120)
            toast.show()
        }
    }

    private fun preparePlaylist() {
        val mediaItems = getCurrentPlaylist().map { song ->
            MediaItem.fromUri(song.audioUrl)
        }
        player.setMediaItems(mediaItems)
        player.prepare()
    }

    private fun playSong(song: Song) {
        val playlist = getCurrentPlaylist()
        val index = playlist.indexOf(song)
        if (index == -1) return

        // Update UI
        currentSong = song
        updateSongInfo(song)
        adapter.updateNowPlaying(song.id)

        // Tell ExoPlayer to play this item
        if (player.playbackState == Player.STATE_IDLE) {
            player.seekToDefaultPosition(index)
            player.play()
        } else {
            player.seekTo(index, 0) // restart from beginning
            if (!player.isPlaying) {
                player.play()
            }
        }
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
        if (player.isPlaying) {
            val duration = player.duration
            val current = player.currentPosition
            if (duration > 0) {
                val progress = (current * 100 / duration).toInt()
                progressBar.progress = progress
            }
        }
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

        val toast = Toast.makeText(this, message, Toast.LENGTH_SHORT)
        toast.setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 0, 120)
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

    override fun onBackPressed() {
        if (searchResultsOverlay.visibility == View.VISIBLE) {
            searchResultsOverlay.visibility = View.GONE
            searchPlaylist.setText("")
            searchPlaylist.clearFocus()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        player.release()
        mediaSession.release()
        stopProgressUpdate()
    }

    fun View.setupHideKeyboardOnTouch() {
        setOnTouchListener { _, _ ->
            hideKeyboard()
            clearFocus()
            false
        }
    }

    private fun View.hideKeyboard() {
        val inputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(windowToken, 0)
    }

    private fun setupProgressBarSeeking() {
        progressBar.setOnTouchListener { _, event ->
            if (player.duration <= 0) return@setOnTouchListener false

            val x = event.x
            val width = progressBar.width.toFloat()
            if (width <= 0) return@setOnTouchListener false

            val progress = (x / width).coerceIn(0f, 1f)
            val newPosition = (progress * player.duration).toLong()

            when (event.action) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                    // Optional: show preview (not implemented here)
                }
                MotionEvent.ACTION_UP -> {
                    player.seekTo(newPosition)
                    // Resume playback if it was playing
                    if (isPlaying) {
                        player.play()
                    }
                }
            }
            true // consume touch
        }
    }
}