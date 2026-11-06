package it.streaminghub.tv.ui.components

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import it.streaminghub.tv.data.update.UpdateStatus
import it.streaminghub.tv.ui.theme.AccentRed
import it.streaminghub.tv.ui.theme.BgCard
import it.streaminghub.tv.ui.theme.BrandCyan
import it.streaminghub.tv.ui.theme.BrandGradient
import it.streaminghub.tv.ui.theme.TextDim
import it.streaminghub.tv.ui.theme.TextMain
import it.streaminghub.tv.ui.theme.TextMuted

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun UpdateDialog(
    status: UpdateStatus,
    onDownloadAndInstall: (UpdateStatus.UpdateAvailable) -> Unit,
    onDismiss: () -> Unit
) {
    val initialFocusRequester = remember { FocusRequester() }

    LaunchedEffect(status) {
        if (status is UpdateStatus.UpdateAvailable || status is UpdateStatus.Error) {
            try {
                initialFocusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    // Modal background barrier
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC000000)),
        contentAlignment = Alignment.Center
    ) {
        val shape = RoundedCornerShape(16.dp)

        Box(
            modifier = Modifier
                .widthIn(max = 620.dp)
                .fillMaxWidth(0.65f)
                .clip(shape)
                .background(BgCard)
                .border(1.5.dp, BrandCyan.copy(alpha = 0.4f), shape)
                .padding(32.dp)
        ) {
            when (status) {
                is UpdateStatus.UpdateAvailable -> {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.SystemUpdate,
                                contentDescription = null,
                                tint = BrandCyan,
                                modifier = Modifier.padding(end = 12.dp)
                            )
                            Text(
                                text = "Nuovo Aggiornamento Disponibile",
                                color = TextMain,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Versione: v${status.version}",
                            color = BrandCyan,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0x22FFFFFF))
                                .padding(12.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = status.releaseNotes.ifBlank { "Miglioramenti di stabilità e prestazioni generali." },
                                color = TextDim,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TvButton(
                                text = "Più tardi",
                                icon = Icons.Default.Close,
                                isPrimary = false,
                                onClick = onDismiss
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            TvButton(
                                text = "Aggiorna Ora",
                                icon = Icons.Default.Download,
                                isPrimary = true,
                                modifier = Modifier.focusRequester(initialFocusRequester),
                                onClick = { onDownloadAndInstall(status) }
                            )
                        }
                    }
                }

                is UpdateStatus.Downloading -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Download dell'aggiornamento...",
                            color = TextMain,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        val percentage = (status.progress * 100).toInt()

                        // Progress Bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(Color(0x33FFFFFF))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction = status.progress)
                                    .height(10.dp)
                                    .background(BrandGradient)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "$percentage%",
                            color = BrandCyan,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                is UpdateStatus.ReadyToInstall, UpdateStatus.Installing -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        TvLoadingSpinner()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Preparazione all'installazione...",
                            color = TextMain,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "A breve apparirà la conferma di sistema di Android TV.",
                            color = TextMuted,
                            fontSize = 13.sp
                        )
                    }
                }

                is UpdateStatus.Error -> {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Errore durante l'aggiornamento",
                            color = AccentRed,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = status.message,
                            color = TextDim,
                            fontSize = 14.sp
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TvButton(
                                text = "Chiudi",
                                icon = Icons.Default.Close,
                                isPrimary = true,
                                modifier = Modifier.focusRequester(initialFocusRequester),
                                onClick = onDismiss
                            )
                        }
                    }
                }

                else -> {}
            }
        }
    }
}
