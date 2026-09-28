package it.streaminghub.tv.data.api

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object ApiClient {

    val httpClient: HttpClient by lazy {
        HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    coerceInputValues = true
                    encodeDefaults = true
                })
            }

            install(HttpTimeout) {
                requestTimeoutMillis = 20_000
                connectTimeoutMillis = 12_000
                socketTimeoutMillis = 20_000
            }

            install(Logging) {
                level = LogLevel.INFO
            }
        }
    }

    fun create(baseUrl: String): StreamingHubApi {
        val sanitizedUrl = if (baseUrl.endsWith("/")) baseUrl.removeSuffix("/") else baseUrl
        return StreamingHubApi(httpClient, sanitizedUrl)
    }
}
