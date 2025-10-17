package com.musa.poetmusic

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class SongAdapter(
    private val songs: List<Song>,
    private val onSongClick: (Song) -> Unit,
    private var nowPlayingId: Int? = null
) : RecyclerView.Adapter<SongAdapter.SongViewHolder>() {

    fun updateNowPlaying(songId: Int?) {
        this.nowPlayingId = songId
        notifyDataSetChanged() // or use DiffUtil for efficiency
    }

    inner class SongViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val albumArt: ImageView = itemView.findViewById(R.id.albumArtItem)
        val songTitle: TextView = itemView.findViewById(R.id.songTitle)
        val artistName: TextView = itemView.findViewById(R.id.artistName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_song, parent, false)
        return SongViewHolder(view)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        val song = songs[position]
        val isPlaying = (nowPlayingId == song.id)

        holder.songTitle.text = song.title
        holder.artistName.text = song.artist

        // Highlight logic
        if (isPlaying) {
            holder.songTitle.setTextColor(
                ContextCompat.getColor(holder.itemView.context, R.color.playing_text_color)
            )
            holder.artistName.setTextColor(
                ContextCompat.getColor(holder.itemView.context, R.color.playing_text_color)
            )
        } else {
            holder.songTitle.setTextColor(
                ContextCompat.getColor(holder.itemView.context, R.color.default_text_color)
            )
            holder.artistName.setTextColor(
                ContextCompat.getColor(holder.itemView.context, R.color.default_artist_color)
            )
        }

        Glide.with(holder.albumArt.context)
            .load(song.albumArtUrl)
            .placeholder(R.drawable.album_art_placeholder)
            .error(R.drawable.album_art_placeholder)
            .into(holder.albumArt)

        holder.itemView.setOnClickListener { onSongClick(song) }
    }

    override fun getItemCount() = songs.size
}