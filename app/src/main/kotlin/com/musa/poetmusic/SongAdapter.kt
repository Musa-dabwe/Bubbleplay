package com.musa.poetmusic

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class SongAdapter(private val songs: List<Song>, private val onSongClick: (Song) -> Unit) :
    RecyclerView.Adapter<SongAdapter.SongViewHolder>() {

    inner class SongViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val albumArt: ImageView = itemView.findViewById(com.musa.poetmusic.R.id.albumArtItem)
        val songTitle: TextView = itemView.findViewById(com.musa.poetmusic.R.id.songTitle)
        val artistName: TextView = itemView.findViewById(com.musa.poetmusic.R.id.artistName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_song, parent, false)
        return SongViewHolder(view)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        val song = songs[position]
        holder.songTitle.text = song.title
        holder.artistName.text = song.artist

        // Load album art with Glide (or use placeholder if no image)
        Glide.with(holder.albumArt.context)
            .load(song.albumArtUrl)
            .placeholder(com.musa.poetmusic.R.drawable.album_art_placeholder)
            .error(com.musa.poetmusic.R.drawable.album_art_placeholder)
            .into(holder.albumArt)

        holder.itemView.setOnClickListener {
            onSongClick(song)
        }
    }

    override fun getItemCount() = songs.size
}