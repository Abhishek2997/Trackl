package com.trackl.app.ui.profile

import android.content.ContentResolver
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.trackl.app.BuildConfig
import com.trackl.app.data.Stats
import com.trackl.app.data.TracklRepository
import com.trackl.app.ui.components.SectionTitle
import com.trackl.app.ui.components.appViewModel
import com.trackl.app.ui.components.formatWatchTime
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProfileViewModel(private val repo: TracklRepository) : ViewModel() {
    val stats = repo.observeStats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Stats())

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    fun export(resolver: ContentResolver, uri: Uri) = viewModelScope.launch {
        val result = runCatching {
            val json = repo.exportBackup()
            withContext(Dispatchers.IO) {
                resolver.openOutputStream(uri, "wt")!!.use { it.write(json.toByteArray()) }
            }
        }
        _messages.send(if (result.isSuccess) "Backup saved" else "Export failed: ${result.exceptionOrNull()?.message}")
    }

    fun import(resolver: ContentResolver, uri: Uri) = viewModelScope.launch {
        val result = runCatching {
            val text = withContext(Dispatchers.IO) {
                resolver.openInputStream(uri)!!.use { it.readBytes().decodeToString() }
            }
            repo.importBackup(text)
        }
        _messages.send(result.getOrElse { "Import failed: ${it.message}" })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen() {
    val vm = appViewModel { ProfileViewModel(it.repository) }
    val stats by vm.stats.collectAsStateWithLifecycle()
    val resolver = LocalContext.current.contentResolver
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { vm.messages.collect { snackbar.showSnackbar(it) } }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let { vm.export(resolver, it) }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { vm.import(resolver, it) }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Profile") }) },
        snackbarHost = { SnackbarHost(snackbar) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {
            SectionTitle("TV")
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Time watched", formatWatchTime(stats.episodeMinutes), Modifier.weight(1f))
                StatCard("Episodes", stats.episodesWatched.toString(), Modifier.weight(1f))
                StatCard("Shows", stats.showsFollowed.toString(), Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            SectionTitle("Movies")
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Time watched", formatWatchTime(stats.movieMinutes), Modifier.weight(1f))
                StatCard("Watched", stats.moviesWatched.toString(), Modifier.weight(1f))
                StatCard("Watchlist", stats.watchlistCount.toString(), Modifier.weight(1f))
            }

            Spacer(Modifier.height(8.dp))
            SectionTitle("Backup")
            Text(
                "Your library is stored on this device. Export a backup file regularly so you never lose your history.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(Modifier.height(8.dp))
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilledTonalButton(
                    onClick = { exportLauncher.launch("trackl-backup-${LocalDate.now()}.json") },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Outlined.Upload, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Export")
                }
                FilledTonalButton(
                    onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Outlined.Download, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Import")
                }
            }

            Spacer(Modifier.height(16.dp))
            SectionTitle("About")
            Text(
                "Trackl ${BuildConfig.VERSION_NAME}\n\nThis product uses the TMDB API but is not endorsed or certified by TMDB. " +
                    "Movie and TV metadata and images are provided by The Movie Database (themoviedb.org).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
