package it.streaminghub.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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

                // NavController vive qui: non viene ricreato ad ogni emissione DataStore.
                // La startDestination è calcolata una sola volta al primo serverUrl valido.
                val navController = rememberNavController()

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
}
