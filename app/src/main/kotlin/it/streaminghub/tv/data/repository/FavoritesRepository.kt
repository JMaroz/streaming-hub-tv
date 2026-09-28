package it.streaminghub.tv.data.repository

import it.streaminghub.tv.data.api.ApiClient
import it.streaminghub.tv.data.api.StreamingHubApi
import it.streaminghub.tv.data.local.AppPreferences
import it.streaminghub.tv.data.model.FavoriteToggleRequestDto
import it.streaminghub.tv.data.model.MediaItemDto
import kotlinx.coroutines.flow.firstOrNull

class FavoritesRepository(
    private val preferences: AppPreferences
) {
    private suspend fun getApi(): StreamingHubApi {
        val url = preferences.serverUrl.firstOrNull()
            ?: throw IllegalStateException("Server URL non configurato.")
        return ApiClient.create(url)
    }

    suspend fun getFavorites(profileId: String = "default"): Result<List<MediaItemDto>> = runCatching {
        getApi().getFavorites(profileId = profileId)
    }

    suspend fun toggleFavorite(
        titleId: String,
        mediaType: String,
        title: String,
        posterUrl: String?,
        profileId: String = "default"
    ): Result<Boolean> = runCatching {
        val response = getApi().toggleFavorite(
            FavoriteToggleRequestDto(
                titleId = titleId,
                mediaType = mediaType,
                title = title,
                posterUrl = posterUrl,
                profileId = profileId
            )
        )
        response.favorite
    }
}
