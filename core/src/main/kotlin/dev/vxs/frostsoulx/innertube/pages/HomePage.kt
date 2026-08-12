/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package dev.vxs.frostsoulx.innertube.pages

import dev.vxs.frostsoulx.innertube.models.Album
import dev.vxs.frostsoulx.innertube.models.AlbumItem
import dev.vxs.frostsoulx.innertube.models.Artist
import dev.vxs.frostsoulx.innertube.models.ArtistItem
import dev.vxs.frostsoulx.innertube.models.BrowseEndpoint
import dev.vxs.frostsoulx.innertube.models.MusicCarouselShelfRenderer
import dev.vxs.frostsoulx.innertube.models.MusicTwoRowItemRenderer
import dev.vxs.frostsoulx.innertube.models.PlaylistItem
import dev.vxs.frostsoulx.innertube.models.SectionListRenderer
import dev.vxs.frostsoulx.innertube.models.SongItem
import dev.vxs.frostsoulx.innertube.models.YTItem
import dev.vxs.frostsoulx.innertube.models.filterExplicit
import dev.vxs.frostsoulx.innertube.models.oddElements

data class HomePage(
    val chips: List<Chip>?,
    val sections: List<Section>,
    val continuation: String? = null,
) {
    data class Chip(
        val title: String,
        val endpoint: BrowseEndpoint?,
        val deselectEndPoint: BrowseEndpoint?,
    ) {
        companion object {
            fun fromChipCloudChipRenderer(renderer: SectionListRenderer.Header.ChipCloudRenderer.Chip): Chip? {
                return Chip(
                    title =
                        renderer.chipCloudChipRenderer.text
                            ?.runs
                            ?.firstOrNull()
                            ?.text ?: return null,
                    endpoint = renderer.chipCloudChipRenderer.navigationEndpoint.browseEndpoint,
                    deselectEndPoint = renderer.chipCloudChipRenderer.onDeselectedCommand?.browseEndpoint,
                )
            }
        }
    }

    data class Section(
        val title: String,
        val label: String?,
        val thumbnail: String?,
        val endpoint: BrowseEndpoint?,
        val items: List<YTItem>,
    ) {
        companion object {
            fun fromMusicCarouselShelfRenderer(renderer: MusicCarouselShelfRenderer): Section? {
                return Section(
                    title =
                        renderer.header
                            ?.musicCarouselShelfBasicHeaderRenderer
                            ?.title
                            ?.runs
                            ?.firstOrNull()
                            ?.text ?: return null,
                    label =
                        renderer.header.musicCarouselShelfBasicHeaderRenderer.strapline
                            ?.runs
                            ?.firstOrNull()
                            ?.text,
                    thumbnail =
                        renderer.header.musicCarouselShelfBasicHeaderRenderer.thumbnail
                            ?.musicThumbnailRenderer
                            ?.getThumbnailUrl(),
                    endpoint =
                        renderer.header.musicCarouselShelfBasicHeaderRenderer.moreContentButton
                            ?.buttonRenderer
                            ?.navigationEndpoint
                            ?.browseEndpoint,
                    items =
                        renderer.contents
                            .mapNotNull {
                                it.musicTwoRowItemRenderer
                            }.mapNotNull {
                                fromMusicTwoRowItemRenderer(it)
                            }.ifEmpty {
                                return null
                            },
                )
            }

            private fun fromMusicTwoRowItemRenderer(renderer: MusicTwoRowItemRenderer): YTItem? {
                return when {
                    renderer.isSong -> {
                        val subtitleRuns = renderer.subtitle?.runs ?: return null
                        val (artistRuns, albumRuns) =
                            subtitleRuns.partition { run ->
                                run.navigationEndpoint
                                    ?.browseEndpoint
                                    ?.browseId
                                    ?.startsWith("UC") == true
                            }
                        val artists =
                            artistRuns.map {
                                Artist(
                                    name = it.text,
                                    id = it.navigationEndpoint?.browseEndpoint?.browseId ?: return null,
                                )
                            }
                        val thumbnail = renderer.thumbnailRenderer.musicThumbnailRenderer?.getBestThumbnail() ?: return null
                        SongItem(
                            id = renderer.navigationEndpoint.watchEndpoint?.videoId ?: return null,
                            title =
                                renderer.title.runs
                                    ?.firstOrNull()
                                    ?.text ?: return null,
                            artists = artists,
                            album =
                                albumRuns
                                    .firstOrNull { run ->
                                        run.navigationEndpoint
                                            ?.browseEndpoint
                                            ?.browseId
                                            ?.startsWith("MPREb_") == true
                                    }?.let { run ->
                                        val endpoint = run.navigationEndpoint?.browseEndpoint ?: return null
                                        Album(
                                            name = run.text,
                                            id = endpoint.browseId,
                                        )
                                    },
                            duration = null,
                            thumbnail = thumbnail.normalizedUrl,
                            thumbnailWidth = thumbnail.width,
                            thumbnailHeight = thumbnail.height,
                            explicit =
                                renderer.subtitleBadges?.any {
                                    it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                                } == true,
                            endpoint = renderer.navigationEndpoint.anyWatchEndpoint,
                        )
                    }

                    renderer.isAlbum -> {
                        val thumbnail = renderer.thumbnailRenderer.musicThumbnailRenderer?.getBestThumbnail() ?: return null
                        AlbumItem(
                            browseId = renderer.navigationEndpoint.browseEndpoint?.browseId ?: return null,
                            playlistId =
                                renderer.thumbnailOverlay
                                    ?.musicItemThumbnailOverlayRenderer
                                    ?.content
                                    ?.musicPlayButtonRenderer
                                    ?.playNavigationEndpoint
                                    ?.watchPlaylistEndpoint
                                    ?.playlistId ?: return null,
                            title =
                                renderer.title.runs
                                    ?.firstOrNull()
                                    ?.text ?: return null,
                            artists =
                                renderer.subtitle?.runs?.oddElements()?.drop(1)?.map {
                                    Artist(
                                        name = it.text,
                                        id = it.navigationEndpoint?.browseEndpoint?.browseId,
                                    )
                                },
                            year = null,
                            thumbnail = thumbnail.normalizedUrl,
                            thumbnailWidth = thumbnail.width,
                            thumbnailHeight = thumbnail.height,
                            explicit =
                                renderer.subtitleBadges?.find {
                                    it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                                } != null,
                        )
                    }

                    renderer.isPlaylist -> {
                        val thumbnail = renderer.thumbnailRenderer.musicThumbnailRenderer?.getBestThumbnail() ?: return null
                        PlaylistItem(
                            id =
                                renderer.navigationEndpoint.browseEndpoint
                                    ?.browseId
                                    ?.removePrefix("VL") ?: return null,
                            title =
                                renderer.title.runs
                                    ?.firstOrNull()
                                    ?.text ?: return null,
                            author =
                                Artist(
                                    name =
                                        renderer.subtitle
                                            ?.runs
                                            ?.lastOrNull()
                                            ?.text ?: return null,
                                    id = null,
                                ),
                            songCountText = null,
                            thumbnail = thumbnail.normalizedUrl,
                            thumbnailWidth = thumbnail.width,
                            thumbnailHeight = thumbnail.height,
                            playEndpoint =
                                renderer.thumbnailOverlay
                                    ?.musicItemThumbnailOverlayRenderer
                                    ?.content
                                    ?.musicPlayButtonRenderer
                                    ?.playNavigationEndpoint
                                    ?.watchPlaylistEndpoint ?: return null,
                            shuffleEndpoint =
                                renderer.menu
                                    ?.menuRenderer
                                    ?.items
                                    ?.find {
                                        it.menuNavigationItemRenderer?.icon?.iconType == "MUSIC_SHUFFLE"
                                    }?.menuNavigationItemRenderer
                                    ?.navigationEndpoint
                                    ?.watchPlaylistEndpoint ?: return null,
                            radioEndpoint =
                                renderer.menu.menuRenderer.items
                                    .find {
                                        it.menuNavigationItemRenderer?.icon?.iconType == "MIX"
                                    }?.menuNavigationItemRenderer
                                    ?.navigationEndpoint
                                    ?.watchPlaylistEndpoint,
                        )
                    }

                    renderer.isArtist -> {
                        val thumbnail = renderer.thumbnailRenderer.musicThumbnailRenderer?.getBestThumbnail() ?: return null
                        ArtistItem(
                            id = renderer.navigationEndpoint.browseEndpoint?.browseId ?: return null,
                            title =
                                renderer.title.runs
                                    ?.lastOrNull()
                                    ?.text ?: return null,
                            thumbnail = thumbnail.normalizedUrl,
                            thumbnailWidth = thumbnail.width,
                            thumbnailHeight = thumbnail.height,
                            shuffleEndpoint =
                                renderer.menu
                                    ?.menuRenderer
                                    ?.items
                                    ?.find {
                                        it.menuNavigationItemRenderer?.icon?.iconType == "MUSIC_SHUFFLE"
                                    }?.menuNavigationItemRenderer
                                    ?.navigationEndpoint
                                    ?.watchPlaylistEndpoint ?: return null,
                            radioEndpoint =
                                renderer.menu.menuRenderer.items
                                    .find {
                                        it.menuNavigationItemRenderer?.icon?.iconType == "MIX"
                                    }?.menuNavigationItemRenderer
                                    ?.navigationEndpoint
                                    ?.watchPlaylistEndpoint ?: return null,
                        )
                    }

                    else -> {
                        null
                    }
                }
            }
        }
    }

    fun filterExplicit(enabled: Boolean = true) =
        if (enabled) {
            copy(
                sections =
                    sections.map {
                        it.copy(items = it.items.filterExplicit())
                    },
            )
        } else {
            this
        }
}
