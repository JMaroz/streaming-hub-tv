package it.streaminghub.tv.ui.setup

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SettingsEthernet
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import it.streaminghub.tv.BuildConfig
import it.streaminghub.tv.R
import it.streaminghub.tv.data.api.DiscoveryState
import it.streaminghub.tv.data.update.UpdateStatus
import it.streaminghub.tv.ui.components.TvButton
import it.streaminghub.tv.ui.components.tvFocusable
import it.streaminghub.tv.ui.theme.AccentGreen
import it.streaminghub.tv.ui.theme.AccentRed
import it.streaminghub.tv.ui.theme.BgCard
import it.streaminghub.tv.ui.theme.BgPrimary
import it.streaminghub.tv.ui.theme.BrandCyan
import it.streaminghub.tv.ui.theme.BrandGradient
import it.streaminghub.tv.ui.theme.PrimaryIndigo
import it.streaminghub.tv.ui.theme.TextDim
import it.streaminghub.tv.ui.theme.TextMain
import it.streaminghub.tv.ui.theme.TextMuted

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SetupScreen(
    viewModel: SetupViewModel,
    onConnected: () -> Unit
) {
    val discoveryState by viewModel.discoveryState.collectAsState()
    val manualUrl by viewModel.manualUrl.collectAsState()
    val isTesting by viewModel.isManualTesting.collectAsState()
    val manualError by viewModel.manualError.collectAsState()
    val updateStatus by (viewModel.updateStatus?.collectAsState() ?: remember { mutableStateOf(UpdateStatus.Idle) })

    var showManualInput by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPrimary)
            .padding(horizontal = 48.dp, vertical = 24.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .width(620.dp)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 16.dp)
        ) {
            // App Branding Icon & Title (aligned with streaming-hub-ha specifications)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_brand_logo),
                    contentDescription = "Streaming Hub",
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.5.dp, BrandCyan.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                )
                Spacer(modifier = Modifier.width(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Streaming",
                        color = TextMain,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Normal
                    )
                    Text(
                        text = "Hub",
                        style = TextStyle(
                            brush = BrandGradient,
                            fontSize = 40.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Per funzionare, l'app richiede l'installazione dell'addon 'streaming-hub-ha' sul tuo server Home Assistant.",
                color = TextMuted,
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            if (showManualInput) {
                // Section 2: Manual IP Configuration
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(BgCard)
                        .padding(24.dp)
                ) {
                    Text(
                        text = "Configurazione Manuale",
                        color = TextMain,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Input box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF0F1523))
                            .tvFocusable(shape = RoundedCornerShape(8.dp))
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        BasicTextField(
                            value = manualUrl,
                            onValueChange = { viewModel.setManualUrl(it) },
                            textStyle = TextStyle(
                                color = TextMain,
                                fontSize = 14.sp
                            ),
                            cursorBrush = SolidColor(PrimaryIndigo),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (manualError != null) {
                        Text(
                            text = manualError ?: "",
                            color = AccentRed,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    TvButton(
                        text = if (isTesting) "Verifica in corso…" else "Verifica e Connetti",
                        isPrimary = true,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            viewModel.connectToServer(manualUrl, onConnected)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                TvButton(
                    text = "Torna alla Ricerca Automatica",
                    isPrimary = false,
                    onClick = { showManualInput = false }
                )

            } else {
                // Section 1: Automatic Discovery Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(BgCard)
                        .padding(24.dp)
                ) {
                    when (val state = discoveryState) {
                        is DiscoveryState.Searching -> {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.SettingsEthernet,
                                        contentDescription = null,
                                        tint = PrimaryIndigo,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Rilevamento automatico in corso…",
                                        color = TextMain,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = state.currentTarget,
                                    color = TextDim,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        is DiscoveryState.Found -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = AccentGreen,
                                        modifier = Modifier.size(26.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "Server Rilevato: ${state.status.appName}",
                                            color = TextMain,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${state.url} • v${state.status.version}",
                                            color = AccentGreen,
                                            fontSize = 12.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(24.dp))

                                TvButton(
                                    text = "Continua",
                                    isPrimary = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = {
                                        viewModel.connectToServer(state.url, onConnected)
                                    }
                                )
                            }
                        }

                        is DiscoveryState.Failed -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "Nessun server rilevato automaticamente",
                                    color = AccentRed,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = state.message,
                                    color = TextMuted,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
                                    textAlign = TextAlign.Center
                                )
                                TvButton(
                                    text = "Riprova Scansione",
                                    icon = Icons.Default.Refresh,
                                    onClick = { viewModel.startDiscovery() }
                                )
                            }
                        }

                        DiscoveryState.Idle -> {
                            TvButton(
                                text = "Avvia Ricerca Automatica",
                                onClick = { viewModel.startDiscovery() }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                TvButton(
                    text = "Inserisci IP manualmente",
                    isPrimary = false,
                    onClick = { showManualInput = true }
                )
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Section 3: App Information & GitHub Updates
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(BgCard)
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Informazioni e Aggiornamenti",
                            color = TextMain,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "StreamingHub TV v${BuildConfig.VERSION_NAME}",
                            color = BrandCyan,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    TvButton(
                        text = if (updateStatus is UpdateStatus.Checking) "Verifica in corso..." else "Verifica Aggiornamenti",
                        icon = Icons.Default.SystemUpdate,
                        isPrimary = updateStatus is UpdateStatus.UpdateAvailable,
                        onClick = { viewModel.checkForUpdates() }
                    )
                }

                val statusMessage = when (updateStatus) {
                    is UpdateStatus.Checking -> "Controllo nuove release su GitHub in corso..."
                    is UpdateStatus.UpToDate -> "L'applicazione è aggiornata all'ultima versione disponibile."
                    is UpdateStatus.UpdateAvailable -> "Nuova versione v${(updateStatus as UpdateStatus.UpdateAvailable).version} disponibile! Clicca per visualizzare i dettagli."
                    is UpdateStatus.Downloading -> "Download in corso... ${((updateStatus as UpdateStatus.Downloading).progress * 100).toInt()}%"
                    is UpdateStatus.Installing, is UpdateStatus.ReadyToInstall -> "Preparazione installazione in corso..."
                    is UpdateStatus.Error -> "Errore verifica: ${(updateStatus as UpdateStatus.Error).message}"
                    UpdateStatus.Idle -> "Aggiornamenti automatici attivi tramite GitHub Releases."
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = statusMessage,
                    color = if (updateStatus is UpdateStatus.Error) AccentRed else TextMuted,
                    fontSize = 12.sp
                )
            }
        }
    }
}
