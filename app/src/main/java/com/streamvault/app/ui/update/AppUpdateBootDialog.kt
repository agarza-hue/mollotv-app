package com.streamvault.app.ui.update

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.streamvault.app.update.AppUpdateDownloadStatus

@Composable
fun AppUpdateBootDialog(
    viewModel: AppUpdateBootViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.downloadStatus) {
        if (state.downloadStatus == AppUpdateDownloadStatus.Downloaded) {
            viewModel.autoInstallIfReady()
        }
    }

    if (!state.available) return

    val downloading = state.downloadStatus == AppUpdateDownloadStatus.Downloading
    val downloaded = state.downloadStatus == AppUpdateDownloadStatus.Downloaded

    AlertDialog(
        onDismissRequest = { viewModel.onLater() },
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(
                text = "Nueva versión disponible",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .widthIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                state.versionName?.let { v ->
                    Text(
                        text = "v$v",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (state.releaseNotes.isNotBlank()) {
                    Text(
                        text = state.releaseNotes,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                if (downloading) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Descargando...",
                        style = MaterialTheme.typography.labelLarge
                    )
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    )
                }
                state.errorMessage?.let { err ->
                    Text(
                        text = err,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { viewModel.onUpdateNow() },
                enabled = !downloading
            ) {
                Text(
                    text = when {
                        downloaded -> "Instalar"
                        downloading -> "Descargando..."
                        else -> "Actualizar ahora"
                    },
                    style = MaterialTheme.typography.labelLarge
                )
            }
        },
        dismissButton = {
            TextButton(onClick = { viewModel.onLater() }) {
                Text("Después")
            }
        },
        modifier = Modifier.padding(PaddingValues(8.dp))
    )
}
