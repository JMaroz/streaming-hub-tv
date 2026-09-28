package it.streaminghub.tv.data.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import it.streaminghub.tv.data.model.CatalogPageDto
import it.streaminghub.tv.data.model.FavoriteToggleRequestDto
import it.streaminghub.tv.data.model.FavoriteToggleResponseDto
import it.streaminghub.tv.data.model.MediaItemDto
import it.streaminghub.tv.data.model.ProfileDto
import it.streaminghub.tv.data.model.ProgressRequestDto
import it.streaminghub.tv.data.model.ResolveRequestDto
import it.streaminghub.tv.data.model.ResolveResponseDto
import it.streaminghub.tv.data.model.SearchResponseDto
import it.streaminghub.tv.data.model.StatusDto
import it.streaminghub.tv.data.model.TvSeasonDto
import it.streaminghub.tv.data.model.WatchProgressItemDto

class StreamingHubApi(
    private val client: HttpClient,
    private val baseUrl: String
) {
    suspend fun getStatus(): StatusDto {
        return client.get("$baseUrl/api/status").body()
    }

    suspend fun getProfiles(): List<ProfileDto> {
        return client.get("$baseUrl/api/profiles").body()
    }

    suspend fun getLatest(
        type: String = "all",
        source: String = "all",
        page: Int = 1,
        profileId: String = "default"
    ): CatalogPageDto {
        return client.get("$baseUrl/api/catalog/latest") {
            parameter("type", type)
            parameter("source", source)
            parameter("page", page)
            parameter("profile_id", profileId)
        }.body()
    }

    suspend fun search(
        query: String,
        type: String = "all",
        source: String = "all",
        profileId: String = "default"
    ): SearchResponseDto {
        return client.get("$baseUrl/api/catalog/search") {
            parameter("q", query)
            parameter("type", type)
            parameter("source", source)
            parameter("profile_id", profileId)
        }.body()
    }

    suspend fun getGenres(): List<String> {
        return client.get("$baseUrl/api/catalog/genres").body()
    }

    suspend fun getByGenre(
        genre: String,
        type: String = "movie",
        source: String = "all",
        page: Int = 1,
        profileId: String = "default"
    ): CatalogPageDto {
        return client.get("$baseUrl/api/catalog/genre/$genre") {
            parameter("type", type)
            parameter("source", source)
            parameter("page", page)
            parameter("profile_id", profileId)
        }.body()
    }

    suspend fun getTitleDetails(
        mediaType: String,
        titleId: String,
        profileId: String = "default"
    ): MediaItemDto {
        return client.get("$baseUrl/api/catalog/title/$mediaType/$titleId") {
            parameter("profile_id", profileId)
        }.body()
    }

    suspend fun getSeasonEpisodes(
        seriesId: String,
        seasonNumber: Int,
        profileId: String = "default"
    ): TvSeasonDto {
        return client.get("$baseUrl/api/catalog/seasons/$seriesId/$seasonNumber") {
            parameter("profile_id", profileId)
        }.body()
    }

    suspend fun resolveMediaSource(request: ResolveRequestDto): ResolveResponseDto {
        return client.post("$baseUrl/api/resolve") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun saveProgress(request: ProgressRequestDto): Map<String, String> {
        return client.post("$baseUrl/api/history") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun getContinueWatching(
        limit: Int = 20,
        profileId: String = "default"
    ): List<WatchProgressItemDto> {
        return client.get("$baseUrl/api/history/continue") {
            parameter("limit", limit)
            parameter("profile_id", profileId)
        }.body()
    }

    suspend fun getWatchedHistory(
        limit: Int = 30,
        profileId: String = "default"
    ): List<WatchProgressItemDto> {
        return client.get("$baseUrl/api/history/watched") {
            parameter("limit", limit)
            parameter("profile_id", profileId)
        }.body()
    }

    suspend fun getFavorites(profileId: String = "default"): List<MediaItemDto> {
        return client.get("$baseUrl/api/favorites") {
            parameter("profile_id", profileId)
        }.body()
    }

    suspend fun toggleFavorite(request: FavoriteToggleRequestDto): FavoriteToggleResponseDto {
        return client.post("$baseUrl/api/favorites/toggle") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }
}
