package it.streaminghub.tv.ui.navigation

import android.net.Uri

sealed class NavRoutes(val route: String) {
    data object Setup : NavRoutes("setup")
    data object Profiles : NavRoutes("profiles")
    data object Home : NavRoutes("home")
    data object Search : NavRoutes("search")

    data object Details : NavRoutes("details/{mediaType}/{titleId}") {
        fun createRoute(mediaType: String, titleId: String): String {
            return "details/$mediaType/${Uri.encode(titleId)}"
        }
    }

    data object Player : NavRoutes("player?url={url}&title={title}&mediaId={mediaId}&mediaType={mediaType}&season={season}&episode={episode}&seek={seek}") {
        fun createRoute(
            url: String,
            title: String,
            mediaId: String,
            mediaType: String = "movie",
            season: Int? = null,
            episode: Int? = null,
            seekSeconds: Long = 0L
        ): String {
            val encodedUrl = Uri.encode(url)
            val encodedTitle = Uri.encode(title)
            val encodedId = Uri.encode(mediaId)
            val s = season ?: -1
            val e = episode ?: -1
            return "player?url=$encodedUrl&title=$encodedTitle&mediaId=$encodedId&mediaType=$mediaType&season=$s&episode=$e&seek=$seekSeconds"
        }
    }
}
