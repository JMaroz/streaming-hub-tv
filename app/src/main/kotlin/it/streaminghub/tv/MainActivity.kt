package it.streaminghub.tv

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import it.streaminghub.tv.ui.navigation.AppNavigation
import it.streaminghub.tv.ui.navigation.NavRoutes
import it.streaminghub.tv.ui.theme.BgPrimary
import it.streaminghub.tv.ui.theme.TvStreamingHubTheme
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.firstOrNull

class MainActivity : ComponentActivity() {

    private var backgroundTimestamp = 0L
    private val resetToInitialStateTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as StreamingHubApp

        setContent {
            TvStreamingHubTheme {
                val serverUrl by app.preferences.serverUrl.collectAsState(initial = "LOADING")

                // NavController vive qui: non viene ricreato ad ogni emissione DataStore.
                // La startDestination è calcolata una sola volta al primo serverUrl valido.
                val navController = rememberNavController()

                LaunchedEffect(Unit) {
                    resetToInitialStateTrigger.collect {
                        val currentRoute = navController.currentBackStackEntry?.destination?.route
                        if (currentRoute != null &&
                            currentRoute != NavRoutes.Home.route &&
                            currentRoute != NavRoutes.Profiles.route &&
                            currentRoute != NavRoutes.Setup.route
                        ) {
                            val activeProfile = app.preferences.activeProfileId.firstOrNull()
                            val target = if (!activeProfile.isNullOrEmpty()) {
                                NavRoutes.Home.route
                            } else {
                                NavRoutes.Profiles.route
                            }
                            navController.navigate(target) {
                                popUpTo(0) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(BgPrimary)
                ) {
                    if (serverUrl != "LOADING") {
                        // remember { } garantisce che startRoute non cambi se serverUrl viene
                        // riemesso da DataStore (es. dopo setActiveProfile) — evita reset nav.
                        val startRoute = remember {
                            if (serverUrl.isNullOrEmpty()) NavRoutes.Setup.route
                            else NavRoutes.Profiles.route
                        }

                        AppNavigation(
                            navController = navController,
                            startDestination = startRoute
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val isLauncherIntent = intent.action == Intent.ACTION_MAIN && (
            intent.hasCategory(Intent.CATEGORY_LEANBACK_LAUNCHER) ||
            intent.hasCategory(Intent.CATEGORY_LAUNCHER)
        )
        if (isLauncherIntent) {
            resetToInitialStateTrigger.tryEmit(Unit)
        }
    }

    override fun onStop() {
        super.onStop()
        backgroundTimestamp = System.currentTimeMillis()
    }

    override fun onResume() {
        super.onResume()
        if (backgroundTimestamp > 0L) {
            val elapsed = System.currentTimeMillis() - backgroundTimestamp
            val fifteenMinutes = 15L * 60L * 1000L
            if (elapsed >= fifteenMinutes) {
                resetToInitialStateTrigger.tryEmit(Unit)
            }
            backgroundTimestamp = 0L
        }
    }
}
