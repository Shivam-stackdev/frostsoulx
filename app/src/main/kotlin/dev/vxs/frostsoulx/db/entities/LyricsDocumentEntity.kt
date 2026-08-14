/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package dev.vxs.frostsoulx.db.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "lyrics_document",
    indices = [Index(value = ["updatedAtMs"])],
)
data class LyricsDocumentEntity(
    @PrimaryKey val songId: String,
    val original: String,
    val translation: String?,
    val romanization: String?,
    val format: String,
    val source: String,
    val offsetMs: Long,
    val artworkKey: String?,
    val updatedAtMs: Long,
)
