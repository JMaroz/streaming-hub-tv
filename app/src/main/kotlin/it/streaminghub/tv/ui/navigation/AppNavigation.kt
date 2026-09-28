package it.streaminghub.tv.ui.navigation

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import it.streaminghub.tv.StreamingHubApp
import it.streaminghub.tv.ui.details.DetailsScreen
import it.streaminghub.tv.ui.details.DetailsViewModel
import it.streaminghub.tv.ui.home.HomeScreen
import it.streaminghub.tv.ui.home.HomeViewModel
import it.streaminghub.tv.ui.player.PlayerScreen
import it.streaminghub.tv.ui.player.PlayerViewModel
import it.streaminghub.tv.ui.profiles.ProfilePickerScreen
import it.streaminghub.tv.ui.profiles.ProfileViewModel
import it.streaminghub.tv.ui.search.SearchScreen
import it.streaminghub.tv.ui.search.SearchViewModel
import it.streaminghub.tv.ui.setup.SetupScreen
import it.streaminghub.tv.ui.setup.SetupViewModel

@Composable
fun AppNavigation(
    navController: NavHostController,
    startDestination: String
) {
    val context = LocalContext.current
    val app = context.applicationContext as StreamingHubApp

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // 1. Setup Screen
        composable(NavRoutes.Setup.route) {
            val setupViewModel = viewModel<SetupViewModel>(
                factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                        return SetupViewModel(app.preferences) as T
                    }
                }
            )
            SetupScreen(
                viewModel = setupViewModel,
                onConnected = {
                    navController.navigate(NavRoutes.Profiles.route) {
                        popUpTo(NavRoutes.Setup.route) { inclusive = true }
                    }
                }
            )
        }

        // 2. Profiles Screen
        composable(NavRoutes.Profiles.route) {
            val profileViewModel = viewModel<ProfileViewModel>(
                factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                        return ProfileViewModel(app.profileRepository) as T
                    }
                }
            )
            ProfilePickerScreen(
                viewModel = profileViewModel,
                onProfileSelected = {
                    navController.navigate(NavRoutes.Home.route) {
                        popUpTo(NavRoutes.Profiles.route) { inclusive = true }
                    }
                }
            )
        }

        // 3. Home Screen
        composable(NavRoutes.Home.route) {
            val homeViewModel = viewModel<HomeViewModel>(
                factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                        return HomeViewModel(
                            app.catalogRepository,
                            app.playbackRepository,
                            app.favoritesRepository,
                            app.preferences
                        ) as T
                    }
                }
            )
            HomeScreen(
                viewModel = homeViewModel,
                onNavigateToDetails = { mediaType, titleId ->
                    navController.navigate(NavRoutes.Details.createRoute(mediaType, titleId))
                },
                onNavigateToSearch = {
                    navController.navigate(NavRoutes.Search.route)
                },
                onNavigateToProfiles = {
                    navController.navigate(NavRoutes.Profiles.route)
                },
                onNavigateToSettings = {
                    navController.navigate(NavRoutes.Setup.route)
                },
                onQuickPlay = { item ->
                    navController.navigate(NavRoutes.Details.createRoute(item.type, item.id))
                }
            )
        }

        // 4. Details Screen
        composable(
            route = NavRoutes.Details.route,
            arguments = listOf(
                navArgument("mediaType") { type = NavType.StringType },
                navArgument("titleId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val mediaType = backStackEntry.arguments?.getString("mediaType") ?: "movie"
            val titleId = backStackEntry.arguments?.getString("titleId") ?: ""

            val detailsViewModel = viewModel<DetailsViewModel>(
                factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                        return DetailsViewModel(
                            mediaType = mediaType,
                            titleId = titleId,
                            catalogRepository = app.catalogRepository,
                            playbackRepository = app.playbackRepository,
                            favoritesRepository = app.favoritesRepository,
                            preferences = app.preferences
                        ) as T
                    }
                }
            )

            DetailsScreen(
                viewModel = detailsViewModel,
                onBackClick = { navController.popBackStack() },
                onPlayMovie = { sourceUrl, title, mediaId, seekSeconds ->
                    navController.navigate(
                        NavRoutes.Player.createRoute(
                            url = sourceUrl,
                            title = title,
                            mediaId = mediaId,
                            mediaType = "movie",
                            seekSeconds = seekSeconds
                        )
                    )
                },
                onPlayEpisode = { sourceUrl, seriesTitle, mediaId, season, episode, seekSeconds ->
                    navController.navigate(
                        NavRoutes.Player.createRoute(
                            url = sourceUrl,
                            title = seriesTitle,
                            mediaId = mediaId,
                            mediaType = "tv",
                            season = season,
                            episode = episode,
                            seekSeconds = seekSeconds
                        )
                    )
                }
            )
        }

        // 5. Search Screen
        composable(NavRoutes.Search.route) {
            val searchViewModel = viewModel<SearchViewModel>(
                factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                        return SearchViewModel(app.catalogRepository, app.preferences) as T
                    }
                }
            )
            SearchScreen(
                viewModel = searchViewModel,
                onBackClick = { navController.popBackStack() },
                onItemClick = { mediaType, titleId ->
                    navController.navigate(NavRoutes.Details.createRoute(mediaType, titleId))
                }
            )
        }

        // 6. Player Screen
        composable(
            route = NavRoutes.Player.route,
            arguments = listOf(
                navArgument("url") { type = NavType.StringType },
                navArgument("title") { type = NavType.StringType },
                navArgument("mediaId") { type = NavType.StringType },
                navArgument("mediaType") { type = NavType.StringType; defaultValue = "movie" },
                navArgument("season") { type = NavType.IntType; defaultValue = -1 },
                navArgument("episode") { type = NavType.IntType; defaultValue = -1 },
                navArgument("seek") { type = NavType.LongType; defaultValue = 0L }
            )
        ) { backStackEntry ->
            val url = backStackEntry.arguments?.getString("url") ?: ""
            val title = backStackEntry.arguments?.getString("title") ?: "Video"
            val mediaId = backStackEntry.arguments?.getString("mediaId") ?: ""
            val mediaType = backStackEntry.arguments?.getString("mediaType") ?: "movie"
            val season = backStackEntry.arguments?.getInt("season")
            val episode = backStackEntry.arguments?.getInt("episode")
            val seek = backStackEntry.arguments?.getLong("seek") ?: 0L

            val playerViewModel = viewModel<PlayerViewModel>(
                factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                        return PlayerViewModel(
                            application = context.applicationContext as Application,
                            pageUrl = url,
                            mediaTitle = title,
                            mediaId = mediaId,
                            mediaType = mediaType,
                            seasonNumber = if (season != null && season >= 0) season else null,
                            episodeNumber = if (episode != null && episode >= 0) episode else null,
                            initialSeekSeconds = seek,
                            playbackRepository = app.playbackRepository,
                            preferences = app.preferences
                        ) as T
                    }
                }
            )

            PlayerScreen(
                viewModel = playerViewModel,
                onExit = { navController.popBackStack() }
            )
        }
    }
}
