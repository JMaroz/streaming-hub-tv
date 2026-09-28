package it.streaminghub.tv.data.repository

import it.streaminghub.tv.data.api.ApiClient
import it.streaminghub.tv.data.api.StreamingHubApi
import it.streaminghub.tv.data.local.AppPreferences
import it.streaminghub.tv.data.model.ProfileDto
import kotlinx.coroutines.flow.firstOrNull

class ProfileRepository(
    private val preferences: AppPreferences
) {
    private suspend fun getApi(): StreamingHubApi {
        val url = preferences.serverUrl.firstOrNull()
            ?: throw IllegalStateException("Server URL non configurato.")
        return ApiClient.create(url)
    }

    suspend fun getProfiles(): Result<List<ProfileDto>> = runCatching {
        getApi().getProfiles()
    }

    suspend fun selectProfile(profile: ProfileDto) {
        preferences.setActiveProfile(
            id = profile.id,
            name = profile.name,
            avatar = profile.avatar
        )
    }
}
