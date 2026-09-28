package it.streaminghub.tv.data.repository

import it.streaminghub.tv.data.api.ApiClient
import it.streaminghub.tv.data.api.StreamingHubApi
import it.streaminghub.tv.data.local.AppPreferences
import it.streaminghub.tv.data.model.CatalogPageDto
import it.streaminghub.tv.data.model.MediaItemDto
import it.streaminghub.tv.data.model.SearchResponseDto
import it.streaminghub.tv.data.model.TvSeasonDto
import kotlinx.coroutines.flow.firstOrNull

class CatalogRepository(
    private val preferences: AppPreferences
) {
    private suspend fun getApi(): StreamingHubApi {
        val url = preferences.serverUrl.firstOrNull()
            ?: throw IllegalStateException("Server URL non configurato.")
        return ApiClient.create(url)
    }

    suspend fun getLatest(
        type: String = "all",
        page: Int = 1,
        profileId: String = "default"
    ): Result<CatalogPageDto> = runCatching {
        getApi().getLatest(type = type, page = page, profileId = profileId)
    }

    suspend fun getGenres(): Result<List<String>> = runCatching {
        getApi().getGenres()
    }

    suspend fun getByGenre(
        genre: String,
        type: String = "movie",
        page: Int = 1,
        profileId: String = "default"
    ): Result<CatalogPageDto> = runCatching {
        getApi().getByGenre(genre = genre, type = type, page = page, profileId = profileId)
    }

    suspend fun search(
        query: String,
        type: String = "all",
        profileId: String = "default"
    ): Result<SearchResponseDto> = runCatching {
        getApi().search(query = query, type = type, profileId = profileId)
    }

    suspend fun getTitleDetails(
        mediaType: String,
        titleId: String,
        profileId: String = "default"
    ): Result<MediaItemDto> = runCatching {
        getApi().getTitleDetails(mediaType = mediaType, titleId = titleId, profileId = profileId)
    }

    suspend fun getSeasonEpisodes(
        seriesId: String,
        seasonNumber: Int,
        profileId: String = "default"
    ): Result<TvSeasonDto> = runCatching {
        getApi().getSeasonEpisodes(seriesId = seriesId, seasonNumber = seasonNumber, profileId = profileId)
    }
}
