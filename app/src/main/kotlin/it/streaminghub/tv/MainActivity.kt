package it.streaminghub.tv

import android.content.Context
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
import it.streaminghub.tv.data.update.AppUpdateManager
import it.streaminghub.tv.data.update.UpdateStatus
import it.streaminghub.tv.ui.components.UpdateDialog
import it.streaminghub.tv.ui.navigation.AppNavigation
import it.streaminghub.tv.ui.navigation.NavRoutes
import it.streaminghub.tv.ui.theme.BgPrimary
import it.streaminghub.tv.ui.theme.TvStreamingHubTheme
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.firstOrNull

class MainActivity : ComponentActivity() {

    companion object {
        private const val PREFS_SESSION = "app_session"
        private const val KEY_LAST_BACKGROUND_TIME = "last_background_timestamp"
        private const val SESSION_TIMEOUT_MS = 5 * 60 * 1000L // 5 minuti
    }

    private val sessionPrefs by lazy {
        getSharedPreferences(PREFS_SESSION, Context.MODE_PRIVATE)
    }

    private val resetToInitialStateTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    override fun onCreate(savedInstanceState: Bundle?) {
        val lastBgPrefs = sessionPrefs.getLong(KEY_LAST_BACKGROUND_TIME, 0L)
        val lastBgBundle = savedInstanceState?.getLong(KEY_LAST_BACKGROUND_TIME, 0L) ?: 0L
        val lastBg = maxOf(lastBgPrefs, lastBgBundle)
        val isSessionExpired = lastBg > 0L && (System.currentTimeMillis() - lastBg >= SESSION_TIMEOUT_MS)

        // Se la sessione è scaduta (> 5 min) dopo che l'app è stata chiusa o uccisa dal sistema (Process Death),
        // passiamo null a super.onCreate per impedire a rememberNavController di ripristinare il vecchio backstack
        // (scheda dettaglio o player) e forzare una partenza pulita dalla Home/Profili.
        val stateToRestore = if (isSessionExpired) null else savedInstanceState
        super.onCreate(stateToRestore)

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

                    // Global in-app update prompt / progress overlay
                    val updateManager = remember { AppUpdateManager.getInstance(applicationContext) }
                    val updateStatus by updateManager.status.collectAsState()

                    when (val status = updateStatus) {
                        is UpdateStatus.UpdateAvailable,
                        is UpdateStatus.Downloading,
                        is UpdateStatus.ReadyToInstall,
                        is UpdateStatus.Installing,
                        is UpdateStatus.Error -> {
                            UpdateDialog(
                                status = status,
                                onDownloadAndInstall = { available ->
                                    updateManager.startDownloadAndInstall(available)
                                },
                                onDismiss = {
                                    updateManager.dismiss()
                                }
                            )
                        }
                        else -> {}
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

    override fun onPause() {
        super.onPause()
        // Registra il timestamp persistente appena l'app perde il primo piano (sopravvive a Process Death)
        sessionPrefs.edit().putLong(KEY_LAST_BACKGROUND_TIME, System.currentTimeMillis()).apply()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(KEY_LAST_BACKGROUND_TIME, System.currentTimeMillis())
    }

    override fun onResume() {
        super.onResume()
        val lastBg = sessionPrefs.getLong(KEY_LAST_BACKGROUND_TIME, 0L)
        if (lastBg > 0L) {
            val elapsed = System.currentTimeMillis() - lastBg
            if (elapsed >= SESSION_TIMEOUT_MS) {
                resetToInitialStateTrigger.tryEmit(Unit)
            }
            // Reset del timestamp per la sessione attiva
            sessionPrefs.edit().putLong(KEY_LAST_BACKGROUND_TIME, 0L).apply()
        }
    }
}
