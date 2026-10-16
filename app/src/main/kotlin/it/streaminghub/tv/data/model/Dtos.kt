package it.streaminghub.tv.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StatusDto(
    val status: String = "offline",
    @SerialName("app_name") val appName: String = "Streaming Hub",
    val version: String = "",
    @SerialName("ha_host_ip") val haHostIp: String? = null,
    @SerialName("stream_port") val streamPort: Int = 8099,
    @SerialName("supervisor_connected") val supervisorConnected: Boolean = false,
    @SerialName("dns_mode") val dnsMode: String? = null,
    @SerialName("tmdb_configured") val tmdbConfigured: Boolean = false
)

@Serializable
data class ProfileDto(
    val id: String,
    val name: String,
    val avatar: String = "avatar_1",
    @SerialName("rating_filter") val ratingFilter: String = "ALL",
    @SerialName("has_pin") val hasPin: Boolean = false,
    @SerialName("tmdb_configured") val tmdbConfigured: Boolean = false,
    @SerialName("trakt_configured") val traktConfigured: Boolean = false,
    @SerialName("trakt_authenticated") val traktAuthenticated: Boolean = false
)

@Serializable
data class ProviderSourceDto(
    val id: String = "",
    @SerialName("media_id") val mediaId: String = "",
    @SerialName("provider_id") val providerId: String = "",
    @SerialName("provider_name") val providerName: String = "",
    @SerialName("page_url") val pageUrl: String = "",
    val language: String? = "ita",
    val quality: String? = "HD",
    val available: Boolean = true
)

fun formatImageUrl(rawUrl: String?, serverUrl: String?): String? {
    val url = rawUrl?.trim() ?: return null
    if (url.isEmpty()) return null

    val cleanServer = serverUrl?.trim()?.removeSuffix("/")

    if (url.startsWith("data:")) return url
    if (url.contains("image.tmdb.org") || url.contains("unsplash.com")) return url

    if (url.startsWith("/t/p/")) return "https://image.tmdb.org$url"
    if (url.startsWith("t/p/")) return "https://image.tmdb.org/$url"

    // Proxy external third-party images through Home Assistant addon to bypass CORS and Referer blocks
    if (!cleanServer.isNullOrEmpty() && (url.startsWith("http://") || url.startsWith("https://"))) {
        return try {
            val encoded = java.net.URLEncoder.encode(url, "UTF-8")
            "$cleanServer/api/proxy/image?url=$encoded"
        } catch (_: Exception) {
            url
        }
    }

    if (url.startsWith("/api/") || url.startsWith("/static/") || url.startsWith("/local/") || url.startsWith("/media/") || url.startsWith("/hls/")) {
        return if (!cleanServer.isNullOrEmpty()) "$cleanServer$url" else url
    }

    if (url.startsWith("/")) {
        val cleanPath = when {
            url.startsWith("/t/p/w500/") -> url.removePrefix("/t/p/w500")
            url.startsWith("/t/p/original/") -> url.removePrefix("/t/p/original")
            url.startsWith("/t/p/") -> "/" + url.substringAfter("/t/p/").substringAfter("/")
            else -> url
        }
        return "https://image.tmdb.org/t/p/w500$cleanPath"
    }

    return if (!cleanServer.isNullOrEmpty()) "$cleanServer/$url" else url
}

@Serializable
data class WatchProviderLogoDto(
    @SerialName("provider_id") val providerId: Int? = null,
    @SerialName("provider_name") val providerName: String = "",
    @SerialName("group_id") val groupId: String? = null,
    @SerialName("logo_path") val logoPath: String? = null,
    @SerialName("logo_url") val logoUrl: String? = null
)

@Serializable
data class WatchProviderGroupDto(
    @SerialName("group_id") val groupId: String = "",
    @SerialName("provider_name") val providerName: String = "",
    @SerialName("logo_path") val logoPath: String? = null,
    @SerialName("logo_url") val logoUrl: String? = null
)

@Serializable
data class StreamingAvailabilityDto(
    @SerialName("grouped_logos") val groupedLogos: List<WatchProviderLogoDto> = emptyList(),
    @SerialName("grouped_providers") val groupedProviders: List<WatchProviderGroupDto> = emptyList()
)

@Serializable
data class TvEpisodeDto(
    val id: String = "",
    @SerialName("media_id") val mediaId: String = "",
    @SerialName("season_number") val seasonNumber: Int = 1,
    @SerialName("episode_number") val episodeNumber: Int = 1,
    val title: String = "",
    val description: String? = null,
    @SerialName("poster_url") val posterUrl: String? = null,
    @SerialName("still_path") val stillPath: String? = null,
    @SerialName("still_url") val stillUrl: String? = null,
    @SerialName("thumbnail_url") val thumbnailUrl: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    val sources: List<ProviderSourceDto> = emptyList()
) {
    val effectivePosterUrl: String?
        get() = posterUrl?.ifBlank { null }
            ?: stillUrl?.ifBlank { null }
            ?: stillPath?.ifBlank { null }
            ?: thumbnailUrl?.ifBlank { null }
            ?: imageUrl?.ifBlank { null }

    fun getFormattedPosterUrl(serverUrl: String?): String? {
        return formatImageUrl(effectivePosterUrl, serverUrl)
    }
}

@Serializable
data class TvSeasonDto(
    val number: Int,
    val episodes: List<TvEpisodeDto> = emptyList()
)

@Serializable
data class MediaItemDto(
    val id: String,
    val type: String = "movie", // "movie" or "tv"
    val title: String,
    @SerialName("original_title") val originalTitle: String? = null,
    val year: Int? = null,
    @SerialName("poster_url") val posterUrl: String? = null,
    @SerialName("backdrop_url") val backdropUrl: String? = null,
    val description: String? = null,
    val genres: List<String> = emptyList(),
    val duration: Int? = null,
    val rating: Float? = null,
    val certification: String? = null,
    val cast: List<String> = emptyList(),
    val director: String? = null,
    val sources: List<ProviderSourceDto> = emptyList(),
    @SerialName("source_a_url") val sourceAUrl: String? = null,
    @SerialName("source_b_url") val sourceBUrl: String? = null,
    val seasons: List<TvSeasonDto> = emptyList(),
    @SerialName("is_favorite") val isFavorite: Boolean = false,
    @SerialName("streaming_availability") val streamingAvailability: StreamingAvailabilityDto? = null
) {
    val resolvedSources: List<ProviderSourceDto>
        get() {
            if (sources.isNotEmpty()) return sources
            val fallbackList = mutableListOf<ProviderSourceDto>()
            if (!sourceAUrl.isNullOrBlank()) {
                fallbackList.add(
                    ProviderSourceDto(
                        id = "$id-src-a",
                        mediaId = id,
                        providerId = "source_a",
                        providerName = "Streaming Community",
                        pageUrl = sourceAUrl,
                        available = true
                    )
                )
            }
            if (!sourceBUrl.isNullOrBlank()) {
                fallbackList.add(
                    ProviderSourceDto(
                        id = "$id-src-b",
                        mediaId = id,
                        providerId = "source_b",
                        providerName = "Sorgente B",
                        pageUrl = sourceBUrl,
                        available = true
                    )
                )
            }
            return fallbackList
        }

    fun getFormattedPosterUrl(serverUrl: String?): String? {
        return formatImageUrl(posterUrl, serverUrl)
    }

    fun getFormattedBackdropUrl(serverUrl: String?): String? {
        return formatImageUrl(backdropUrl ?: posterUrl, serverUrl)
    }
}

@Serializable
data class CatalogPageDto(
    val page: Int = 1,
    val source: String = "all",
    @SerialName("profile_id") val profileId: String = "default",
    val count: Int = 0,
    val results: List<MediaItemDto> = emptyList()
)

@Serializable
data class SearchResponseDto(
    val query: String,
    val count: Int = 0,
    val results: List<MediaItemDto> = emptyList()
)

@Serializable
data class ResolveRequestDto(
    @SerialName("page_url") val pageUrl: String,
    @SerialName("provider_id") val providerId: String? = null,
    @SerialName("media_id") val mediaId: String? = null,
    val quality: String? = null,
    @SerialName("prefer_fhd") val preferFhd: Boolean = true
)

@Serializable
data class ResolveResponseDto(
    val token: String,
    @SerialName("mime_type") val mimeType: String? = "application/vnd.apple.mpegurl",
    @SerialName("stream_format") val streamFormat: String? = "hls",
    @SerialName("local_stream_url") val localStreamUrl: String? = null,
    @SerialName("lan_stream_url") val lanStreamUrl: String,
    val headers: Map<String, String> = emptyMap()
)

@Serializable
data class ProgressRequestDto(
    @SerialName("media_id") val mediaId: String,
    val title: String,
    @SerialName("media_type") val mediaType: String = "movie",
    @SerialName("poster_url") val posterUrl: String? = null,
    @SerialName("season_number") val seasonNumber: Int? = null,
    @SerialName("episode_number") val episodeNumber: Int? = null,
    @SerialName("progress_seconds") val progressSeconds: Double = 0.0,
    @SerialName("duration_seconds") val durationSeconds: Double = 0.0,
    @SerialName("profile_id") val profileId: String = "default",
    val year: Int? = null,
    @SerialName("tmdb_id") val tmdbId: Int? = null,
    @SerialName("imdb_id") val imdbId: String? = null
)

@Serializable
data class WatchProgressItemDto(
    @SerialName("media_id") val mediaId: String,
    val title: String,
    @SerialName("media_type") val mediaType: String = "movie",
    @SerialName("poster_url") val posterUrl: String? = null,
    @SerialName("season_number") val seasonNumber: Int? = null,
    @SerialName("episode_number") val episodeNumber: Int? = null,
    @SerialName("progress_seconds") val progressSeconds: Double = 0.0,
    @SerialName("duration_seconds") val durationSeconds: Double = 0.0,
    @SerialName("profile_id") val profileId: String = "default"
) {
    val progressPercent: Float
        get() = if (durationSeconds > 0) ((progressSeconds / durationSeconds) * 100).toFloat() else 0f

    fun getFormattedPosterUrl(serverUrl: String?): String? {
        return formatImageUrl(posterUrl, serverUrl)
    }
}

@Serializable
data class FavoriteToggleRequestDto(
    @SerialName("title_id") val titleId: String,
    @SerialName("media_type") val mediaType: String = "movie",
    val title: String,
    @SerialName("poster_url") val posterUrl: String? = null,
    @SerialName("profile_id") val profileId: String = "default"
)

@Serializable
data class FavoriteToggleResponseDto(
    val status: String,
    val favorite: Boolean
)

@Serializable
data class HomeCarouselDto(
    val id: String = "",
    val title: String = "",
    val count: Int = 0,
    val items: List<MediaItemDto> = emptyList()
)

@Serializable
data class HomeCatalogDto(
    val mode: String = "grid", // "carousels" or "grid"
    val source: String = "all",
    @SerialName("profile_id") val profileId: String = "default",
    val hero: MediaItemDto? = null,
    val carousels: List<HomeCarouselDto> = emptyList()
)

@Serializable
data class SkipIntervalDto(
    val start: Double = 0.0,
    val end: Double = 0.0
)

@Serializable
data class SkipSegmentsResponseDto(
    @SerialName("has_segments") val hasSegments: Boolean = false,
    val intro: SkipIntervalDto? = null,
    val outro: SkipIntervalDto? = null,
    val source: String = "none"
)

@Serializable
data class NextEpisodeDetailDto(
    @SerialName("series_id") val seriesId: String = "",
    @SerialName("season_number") val seasonNumber: Int = 1,
    @SerialName("episode_number") val episodeNumber: Int = 1,
    val episode: TvEpisodeDto
)

@Serializable
data class NextEpisodeResponseDto(
    @SerialName("has_next") val hasNext: Boolean = false,
    val next: NextEpisodeDetailDto? = null
)

@Serializable
data class SubtitleTrackDto(
    val id: String = "",
    val language: String = "it",
    val label: String = "Italiano",
    val source: String = "opensubtitles",
    val url: String = ""
) {
    fun getFullUrl(serverUrl: String?): String {
        val cleanServer = serverUrl?.trim()?.removeSuffix("/") ?: ""
        return when {
            url.startsWith("http://") || url.startsWith("https://") -> url
            url.startsWith("/") -> "$cleanServer$url"
            else -> "$cleanServer/$url"
        }
    }
}
