package it.streaminghub.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import it.streaminghub.tv.ui.navigation.AppNavigation
import it.streaminghub.tv.ui.navigation.NavRoutes
import it.streaminghub.tv.ui.theme.BgPrimary
import it.streaminghub.tv.ui.theme.TvStreamingHubTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as StreamingHubApp

        setContent {
            TvStreamingHubTheme {
                val serverUrl by app.preferences.serverUrl.collectAsState(initial = "LOADING")

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(BgPrimary)
                ) {
                    if (serverUrl != "LOADING") {
                        val navController = rememberNavController()
                        val startRoute = if (serverUrl.isNullOrEmpty()) {
                            NavRoutes.Setup.route
                        } else {
                            NavRoutes.Profiles.route
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
}
