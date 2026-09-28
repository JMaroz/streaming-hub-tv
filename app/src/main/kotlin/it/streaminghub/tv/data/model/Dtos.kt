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

    return when {
        url.startsWith("http://") || url.startsWith("https://") -> url
        url.startsWith("//") -> "https:$url"
        url.startsWith("/") -> {
            if (!cleanServer.isNullOrEmpty()) {
                "$cleanServer$url"
            } else if (url.startsWith("/t/p/")) {
                "https://image.tmdb.org$url"
            } else {
                url
            }
        }
        url.startsWith("t/p/") -> "https://image.tmdb.org/$url"
        !cleanServer.isNullOrEmpty() -> "$cleanServer/$url"
        else -> "https://image.tmdb.org/t/p/w500/$url"
    }
}

@Serializable
data class TvEpisodeDto(
    val id: String = "",
    @SerialName("media_id") val mediaId: String = "",
    @SerialName("season_number") val seasonNumber: Int = 1,
    @SerialName("episode_number") val episodeNumber: Int = 1,
    val title: String = "",
    val description: String? = null,
    @SerialName("poster_url") val posterUrl: String? = null,
    val sources: List<ProviderSourceDto> = emptyList()
) {
    fun getFormattedPosterUrl(serverUrl: String?): String? {
        return formatImageUrl(posterUrl, serverUrl)
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
    val seasons: List<TvSeasonDto> = emptyList(),
    @SerialName("is_favorite") val isFavorite: Boolean = false
) {
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
