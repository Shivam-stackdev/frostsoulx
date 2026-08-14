/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package dev.vxs.frostsoulx.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "recommendation_signal",
    foreignKeys = [
        ForeignKey(
            entity = SongEntity::class,
            parentColumns = ["id"],
            childColumns = ["songId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["songId", "occurredAtMs"]),
        Index(value = ["occurredAtMs"]),
        Index(value = ["type"]),
    ],
)
data class RecommendationSignalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val songId: String,
    val type: String,
    val occurredAtMs: Long,
    val positionMs: Long,
    val listenedMs: Long,
    val sessionId: Long,
    val contextFlags: Int,
)

@Entity(
    tableName = "recommendation_feature",
    foreignKeys = [
        ForeignKey(
            entity = SongEntity::class,
            parentColumns = ["id"],
            childColumns = ["songId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["updatedAtMs"])],
)
data class RecommendationFeatureEntity(
    @PrimaryKey val songId: String,
    val embeddingVersion: Int,
    val dimension: Int,
    val quantizedEmbedding: ByteArray,
    val scale: Float,
    val norm: Float,
    val replayScore: Float,
    val skipScore: Float,
    val updatedAtMs: Long,
)

@Entity(tableName = "recommendation_profile")
data class RecommendationProfileEntity(
    @PrimaryKey val profile: String,
    val embeddingVersion: Int,
    val dimension: Int,
    val quantizedEmbedding: ByteArray,
    val scale: Float,
    val norm: Float,
    val updatedAtMs: Long,
)
