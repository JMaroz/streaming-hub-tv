package it.streaminghub.tv.data.repository

import it.streaminghub.tv.data.api.ApiClient
import it.streaminghub.tv.data.api.StreamingHubApi
import it.streaminghub.tv.data.local.AppPreferences
import it.streaminghub.tv.data.model.ProgressRequestDto
import it.streaminghub.tv.data.model.ResolveRequestDto
import it.streaminghub.tv.data.model.ResolveResponseDto
import it.streaminghub.tv.data.model.WatchProgressItemDto
import kotlinx.coroutines.flow.firstOrNull

class PlaybackRepository(
    private val preferences: AppPreferences
) {
    private suspend fun getApi(): StreamingHubApi {
        val url = preferences.serverUrl.firstOrNull()
            ?: throw IllegalStateException("Server URL non configurato.")
        return ApiClient.create(url)
    }

    suspend fun resolveStream(
        pageUrl: String,
        providerId: String?,
        mediaId: String?,
        quality: String?,
        preferFhd: Boolean = true
    ): Result<ResolveResponseDto> = runCatching {
        getApi().resolveMediaSource(
            ResolveRequestDto(
                pageUrl = pageUrl,
                providerId = providerId,
                mediaId = mediaId,
                quality = quality,
                preferFhd = preferFhd
            )
        )
    }

    suspend fun reportProgress(request: ProgressRequestDto): Result<Unit> = runCatching {
        getApi().saveProgress(request)
        Unit
    }

    suspend fun getContinueWatching(
        profileId: String = "default"
    ): Result<List<WatchProgressItemDto>> = runCatching {
        getApi().getContinueWatching(profileId = profileId)
    }

    suspend fun getWatchedHistory(
        profileId: String = "default"
    ): Result<List<WatchProgressItemDto>> = runCatching {
        getApi().getWatchedHistory(profileId = profileId)
    }

    suspend fun getSkipSegments(
        seriesId: String,
        seasonNumber: Int,
        episodeNumber: Int,
        duration: Double? = null
    ): Result<it.streaminghub.tv.data.model.SkipSegmentsResponseDto> = runCatching {
        getApi().getSkipSegments(
            seriesId = seriesId,
            seasonNumber = seasonNumber,
            episodeNumber = episodeNumber,
            duration = duration
        )
    }

    suspend fun searchSubtitles(
        imdbId: String? = null,
        tmdbId: Int? = null,
        query: String? = null,
        season: Int? = null,
        episode: Int? = null
    ): Result<List<it.streaminghub.tv.data.model.SubtitleTrackDto>> = runCatching {
        getApi().searchSubtitles(
            imdbId = imdbId,
            tmdbId = tmdbId,
            query = query,
            season = season,
            episode = episode
        )
    }
}
