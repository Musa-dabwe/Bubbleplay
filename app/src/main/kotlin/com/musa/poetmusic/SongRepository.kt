package com.musa.poetmusic

import android.content.Context
import android.provider.MediaStore
import android.util.Log

object SongRepository {
    fun getAllSongs(context: Context): List<Song> {
        val songs = mutableListOf<Song>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DATA // file path
        )

        val cursor = context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            "${MediaStore.Audio.Media.TITLE} ASC"
        )

        cursor?.use {
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumIdColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val dataColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

            while (it.moveToNext()) {
                val id = it.getLong(idColumn)
                val title = it.getString(titleColumn)
                val artist = it.getString(artistColumn)
                val albumId = it.getLong(albumIdColumn)
                val filePath = it.getString(dataColumn)

                val albumArtUri = "content://media/external/audio/albumart/$albumId"

                songs.add(
                    Song(
                        id = id.toInt(),
                        title = title,
                        artist = artist,
                        albumArtUrl = albumArtUri,
                        audioUrl = filePath
                    )
                )
            }
        }

        Log.d("SongRepo", "Loaded ${songs.size} songs")
        return songs
    }
}