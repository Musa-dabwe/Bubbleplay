package com.musa.bubbleplayer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import androidx.core.app.NotificationCompat
import java.util.ArrayList
import java.util.Collections
import java.util.List

class BubbleService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var bubbleView: View
    private lateinit var playerView: View
    private lateinit var bubbleParams: WindowManager.LayoutParams
    private lateinit var playerParams: WindowManager.LayoutParams
    private var isExpanded = false
    private var isShuffleMode = false
    private val playlist: MutableList<Song> = ArrayList()
    private var currentSongIndex = 0

    // Notification channel ID
    private val CHANNEL_ID = "bubble_channel"

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(1, createNotification())
        initializePlaylist() // Load your songs here
        createBubble()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Music Player",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Music Player")
            .setContentText("Playing in background")
            .setSmallIcon(R.drawable.ic_music)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun createBubble() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        // Create bubble view
        bubbleView = LayoutInflater.from(this).inflate(R.layout.bubble_layout, null)
        val albumArt = bubbleView.findViewById<ImageView>(R.id.album_art)
        albumArt.setImageResource(R.drawable.default_album) // Set default album art

        bubbleParams = WindowManager.LayoutParams(
            120, // Width (adjust as needed)
            120, // Height
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 100
        }

        // Create player view
        playerView = LayoutInflater.from(this).inflate(R.layout.player_layout, null)
        setupPlayerControls()

        playerParams = WindowManager.LayoutParams(
            680, // Specified width
            120, // Same height as bubble
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 230 // Position next to bubble
            y = 100
        }


        // Bubble click listener
        bubbleView.setOnClickListener {
            if (!isExpanded) {
                expandPlayer()
            } else {
                collapsePlayer()
            }
        }

        // Handle drag for bubble
        bubbleView.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = bubbleParams.x
                        initialY = bubbleParams.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        bubbleParams.x = initialX + (event.rawX - initialTouchX).toInt()
                        bubbleParams.y = initialY + (event.rawY - initialTouchY).toInt()
                        playerParams.x = bubbleParams.x + 110
                        playerParams.y = bubbleParams.y
                        windowManager.updateViewLayout(bubbleView, bubbleParams)
                        if (isExpanded) {
                            windowManager.updateViewLayout(playerView, playerParams)
                        }
                        return true
                    }
                }
                return false
            }
        })

        windowManager.addView(bubbleView, bubbleParams)
    }

    private fun expandPlayer() {
        windowManager.addView(playerView, playerParams)
        isExpanded = true
    }

    private fun collapsePlayer() {
        windowManager.removeView(playerView)
        isExpanded = false
    }

    private fun setupPlayerControls() {
        // Set album art
        val albumArt = playerView.findViewById<ImageView>(R.id.album_art)
        albumArt.setImageResource(R.drawable.default_album)

        // Set song info
        updateSongInfo()

        // Shuffle button
        playerView.findViewById<View>(R.id.btn_shuffle).setOnClickListener {
            isShuffleMode = !isShuffleMode
            // Update button appearance (add visual feedback)
            if (isShuffleMode) {
                // Shuffle playlist
                Collections.shuffle(playlist)
            } else {
                // Sort alphabetically
                playlist.sortWith { s1, s2 -> s1.title.compareTo(s2.title) }
            }
            updateSongInfo()
        }

        // Previous button
        playerView.findViewById<View>(R.id.btn_prev).setOnClickListener {
            currentSongIndex = (currentSongIndex - 1 + playlist.size) % playlist.size
            updateSongInfo()
        }

        // Next button
        playerView.findViewById<View>(R.id.btn_next).setOnClickListener {
            currentSongIndex = (currentSongIndex + 1) % playlist.size
            updateSongInfo()
        }

        // Play/Pause button
        playerView.findViewById<View>(R.id.btn_play).setOnClickListener {
            // Toggle play/pause state
            // Add your playback logic here
        }

        // Menu button (playlist)
        playerView.findViewById<View>(R.id.btn_menu).setOnClickListener {
            // Show playlist in card panel
            showPlaylistPanel()
        }
    }

    private fun updateSongInfo() {
        if (playlist.isEmpty()) return

        val currentSong = playlist[currentSongIndex]
        // Update UI elements with song info
        // (You'll need to implement these in your layout)
    }

    private fun showPlaylistPanel() {
        // Create detached card panel with playlist
        // Implementation similar to bubble creation but with RecyclerView
    }

    private fun initializePlaylist() {
        // Load your songs here
        // Example:
        playlist.add(Song("Song 1", "Artist 1", R.drawable.album1))
        playlist.add(Song("Song 2", "Artist 2", R.drawable.album2))
        // Sort initially
        playlist.sortWith { s1, s2 -> s1.title.compareTo(s2.title) }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (this::bubbleView.isInitialized) {
            windowManager.removeView(bubbleView)
        }
        if (isExpanded && this::playerView.isInitialized) {
            windowManager.removeView(playerView)
        }
    }
}