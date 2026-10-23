package com.trackl.app.ui.show

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.trackl.app.data.MediaItem
import com.trackl.app.data.MediaType
import com.trackl.app.data.ShowProgress
import com.trackl.app.data.TracklRepository
import com.trackl.app.data.UiState
import com.trackl.app.data.loadUi
import com.trackl.app.data.local.WatchedEpisodeEntity
import com.trackl.app.data.remote.SeasonSummaryDto
import com.trackl.app.data.remote.TvDetailsDto
import com.trackl.app.data.toMediaItem
import com.trackl.app.ui.components.BackButton
import com.trackl.app.ui.components.CastRow
import com.trackl.app.ui.components.DetailHeader
import com.trackl.app.ui.components.ErrorBox
import com.trackl.app.ui.components.LoadingBox
import com.trackl.app.ui.components.MediaRow
import com.trackl.app.ui.components.Poster
import com.trackl.app.ui.components.SectionTitle
import com.trackl.app.ui.components.appViewModel
import com.trackl.app.ui.components.relativeAirDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ShowDetailViewModel(private val showId: Int, private val repo: TracklRepository) : ViewModel() {
    private val _details = MutableStateFlow<UiState<TvDetailsDto>>(UiState.Loading)
    val details = _details.asStateFlow()

    val progress = repo.observeProgress(showId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val watched = repo.observeWatched(showId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _details.value = UiState.Loading
            _details.value = loadUi { repo.tvDetails(showId) }
        }
    }

    fun follow(d: TvDetailsDto) = viewModelScope.launch { repo.follow(d) }
    fun unfollow() = viewModelScope.launch { repo.unfollow(showId) }
    fun markNext(p: ShowProgress) = viewModelScope.launch { repo.markNext(p) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShowDetailScreen(
    showId: Int,
    onBack: () -> Unit,
    onOpen: (MediaItem) -> Unit,
    onOpenSeason: (Int) -> Unit,
) {
    val vm = appViewModel { ShowDetailViewModel(showId, it.repository) }
    val details by vm.details.collectAsStateWithLifecycle()
    val progress by vm.progress.collectAsStateWithLifecycle()
    val watched by vm.watched.collectAsStateWithLifecycle()
    var confirmUnfollow by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text((details as? UiState.Success)?.data?.name ?: "", maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                navigationIcon = { BackButton(onBack) },
            )
        },
    ) { padding ->
        when (val s = details) {
            UiState.Loading -> LoadingBox(Modifier.padding(padding))
            is UiState.Error -> ErrorBox(s.message, onRetry = vm::load, modifier = Modifier.padding(padding))
            is UiState.Success -> ShowContent(
                show = s.data,
                progress = progress,
                watched = watched,
                contentPadding = padding,
                onFollow = { vm.follow(s.data) },
                onUnfollow = { confirmUnfollow = true },
                onMarkNext = { progress?.let(vm::markNext) },
                onOpenSeason = onOpenSeason,
                onOpen = onOpen,
            )
        }
    }

    if (confirmUnfollow) {
        AlertDialog(
            onDismissRequest = { confirmUnfollow = false },
            title = { Text("Stop tracking?") },
            text = { Text("This removes the show and all of its watched episodes from your library.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmUnfollow = false
                    vm.unfollow()
                }) { Text("Remove") }
            },
            dismissButton = { TextButton(onClick = { confirmUnfollow = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun ShowContent(
    show: TvDetailsDto,
    progress: ShowProgress?,
    watched: List<WatchedEpisodeEntity>,
    contentPadding: PaddingValues,
    onFollow: () -> Unit,
    onUnfollow: () -> Unit,
    onMarkNext: () -> Unit,
    onOpenSeason: (Int) -> Unit,
    onOpen: (MediaItem) -> Unit,
) {
    val following = progress != null
    val watchedBySeason = watched.groupingBy { it.season }.eachCount()
    val seasons = show.seasons.filter { it.seasonNumber > 0 } + show.seasons.filter { it.seasonNumber == 0 }

    LazyColumn(contentPadding = contentPadding) {
        item {
            DetailHeader(
                backdropPath = show.backdropPath,
                posterPath = show.posterPath,
                title = show.name,
                subtitle = listOfNotNull(
                    show.firstAirDate?.take(4),
                    show.networks.firstOrNull()?.name,
                    show.status,
                ).joinToString(" · "),
                rating = show.voteAverage,
                genres = show.genres.map { it.name },
            )
        }
        item {
            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (following) {
                    OutlinedButton(onClick = onUnfollow, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.Check, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Tracking")
                    }
                } else {
                    Button(onClick = onFollow, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.Add, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Add to my shows")
                    }
                }
            }
        }
        if (progress != null) {
            item { ProgressCard(progress, onMarkNext) }
        }
        show.nextEpisodeToAir?.let { next ->
            item {
                Text(
                    "Next episode: S%02dE%02d · %s".format(
                        next.seasonNumber, next.episodeNumber, relativeAirDate(next.airDate) ?: "TBA",
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
        }
        item {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                show.tagline?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, fontStyle = FontStyle.Italic)
                    Spacer(Modifier.size(8.dp))
                }
                Text(show.overview?.takeIf { it.isNotBlank() } ?: "No overview available.", style = MaterialTheme.typography.bodyMedium)
            }
        }
        if (seasons.isNotEmpty()) {
            item { SectionTitle("Seasons") }
            items(seasons, key = { it.id }) { season ->
                SeasonRow(season, watchedBySeason[season.seasonNumber] ?: 0) { onOpenSeason(season.seasonNumber) }
            }
        }
        item { CastRow(show.credits?.cast.orEmpty()) }
        item {
            MediaRow(
                "More like this",
                show.recommendations?.results.orEmpty().mapNotNull { it.toMediaItem(MediaType.TV) },
                onOpen,
            )
        }
        item { Spacer(Modifier.size(24.dp)) }
    }
}

@Composable
private fun ProgressCard(progress: ShowProgress, onMarkNext: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    progress.next?.let { "Up next: ${it.code}" } ?: "You're all caught up!",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.size(8.dp))
                LinearProgressIndicator(progress = { progress.fraction }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.size(4.dp))
                Text(
                    "${progress.watchedCount} of ${progress.airedCount} episodes watched",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (progress.next != null) {
                Spacer(Modifier.width(12.dp))
                FilledIconButton(onClick = onMarkNext) { Icon(Icons.Filled.Check, "Mark ${progress.next.code} watched") }
            }
        }
    }
}

@Composable
private fun SeasonRow(season: SeasonSummaryDto, watchedCount: Int, onClick: () -> Unit) {
    val complete = season.episodeCount > 0 && watchedCount >= season.episodeCount
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = {
            Poster(season.posterPath, season.name, Modifier.width(48.dp).aspectRatio(2f / 3f), size = "w154", corner = 6.dp)
        },
        headlineContent = { Text(season.name ?: "Season ${season.seasonNumber}") },
        supportingContent = {
            Column {
                Text(
                    listOfNotNull(
                        "${season.episodeCount} episodes",
                        season.airDate?.take(4),
                    ).joinToString(" · "),
                )
                if (watchedCount > 0 && season.episodeCount > 0) {
                    Spacer(Modifier.size(4.dp))
                    LinearProgressIndicator(
                        progress = { (watchedCount.toFloat() / season.episodeCount).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        trailingContent = {
            if (complete) {
                Icon(Icons.Filled.CheckCircle, "Completed", tint = MaterialTheme.colorScheme.primary)
            } else {
                Text("$watchedCount/${season.episodeCount}", style = MaterialTheme.typography.labelMedium)
            }
        },
    )
}
