package com.musa.poetmusic

data class Song(
    val id: Int,
    val title: String,
    val artist: String,
    val albumArtUrl: String, // or use R.drawable.xxx for local assets
    val audioUrl: String // Local file path or URL
)