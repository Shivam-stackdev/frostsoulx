package dev.vxs.frostsoulx.ui.lyrics

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import dev.vxs.frostsoulx.R
import dev.vxs.frostsoulx.lyrics.LyricsEntry

/**
 * RecyclerView adapter for synced lyrics.
 */
class LyricsAdapter : RecyclerView.Adapter<LyricsAdapter.LyricViewHolder>() {

    private val lyrics = mutableListOf<LyricsEntry>()
    private var activePosition: Int = -1

    fun submitList(newLyrics: List<LyricsEntry>) {
        lyrics.clear()
        lyrics.addAll(newLyrics)
        notifyDataSetChanged()
    }

    /**
     * Updates which lyric line is currently active based on playback position.
     * Call this from your playback position update loop.
     */
    fun updateActivePosition(positionMs: Long) {
        var newActive = -1
        for (i in lyrics.indices) {
            if (lyrics[i].time <= positionMs) {
                newActive = i
            } else {
                break
            }
        }

        if (newActive != activePosition) {
            val old = activePosition
            activePosition = newActive
            if (old >= 0) notifyItemChanged(old)
            if (newActive >= 0) notifyItemChanged(newActive)
        }
    }

    fun getActivePosition(): Int = activePosition

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LyricViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_lyric_line, parent, false)
        return LyricViewHolder(view)
    }

    override fun onBindViewHolder(holder: LyricViewHolder, position: Int) {
        holder.bind(lyrics[position], position == activePosition)
    }

    override fun getItemCount(): Int = lyrics.size

    inner class LyricViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textView: TextView = itemView.findViewById(R.id.lyric_text)

        fun bind(entry: LyricsEntry, isActive: Boolean) {
            textView.text = entry.text
            val context = itemView.context
            when {
                isActive -> {
                    itemView.setBackgroundColor(0x00000000)
                    textView.setTextColor(ContextCompat.getColor(context, android.R.color.white))
                    textView.setTypeface(null, Typeface.BOLD)
                    textView.textSize = 18f
                }
                else -> {
                    // Inactive line
                    itemView.setBackgroundColor(0x00000000)
                    textView.setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray))
                    textView.setTypeface(null, Typeface.NORMAL)
                    textView.textSize = 16f
                }
            }
        }
    }
}
