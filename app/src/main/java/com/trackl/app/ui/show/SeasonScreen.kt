package com.trackl.app.ui.show

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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.RemoveDone
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.trackl.app.data.TracklRepository
import com.trackl.app.data.UiState
import com.trackl.app.data.loadUi
import com.trackl.app.data.remote.EpisodeDto
import com.trackl.app.data.remote.SeasonDetailsDto
import com.trackl.app.ui.components.BackButton
import com.trackl.app.ui.components.ErrorBox
import com.trackl.app.ui.components.LoadingBox
import com.trackl.app.ui.components.Poster
import com.trackl.app.ui.components.appViewModel
import com.trackl.app.ui.components.formatDate
import com.trackl.app.ui.components.formatRuntime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SeasonViewModel(
    private val showId: Int,
    private val season: Int,
    private val repo: TracklRepository,
) : ViewModel() {
    private val _details = MutableStateFlow<UiState<SeasonDetailsDto>>(UiState.Loading)
    val details = _details.asStateFlow()

    val watchedEpisodes = repo.observeWatched(showId)
        .map { list -> list.filter { it.season == season }.mapTo(HashSet()) { it.episode } as Set<Int> }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _details.value = UiState.Loading
            _details.value = loadUi { repo.seasonDetails(showId, season) }
        }
    }

    fun toggle(ep: EpisodeDto, watched: Boolean) = viewModelScope.launch {
        repo.setEpisodeWatched(showId, season, ep.episodeNumber, ep.runtime, watched)
    }

    fun setAll(episodes: List<EpisodeDto>, watched: Boolean) = viewModelScope.launch {
        repo.setSeasonWatched(showId, season, episodes, watched)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeasonScreen(showId: Int, season: Int, onBack: () -> Unit) {
    val vm = appViewModel { SeasonViewModel(showId, season, it.repository) }
    val details by vm.details.collectAsStateWithLifecycle()
    val watched by vm.watchedEpisodes.collectAsStateWithLifecycle()
    val data = (details as? UiState.Success)?.data
    val airedEpisodes = data?.episodes.orEmpty().filter { TracklRepository.isAired(it.airDate) }
    val allWatched = airedEpisodes.isNotEmpty() && airedEpisodes.all { it.episodeNumber in watched }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(data?.name ?: "Season $season", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { BackButton(onBack) },
                actions = {
                    if (data != null && airedEpisodes.isNotEmpty()) {
                        IconButton(onClick = { vm.setAll(data.episodes, !allWatched) }) {
                            Icon(
                                if (allWatched) Icons.Filled.RemoveDone else Icons.Filled.DoneAll,
                                contentDescription = if (allWatched) "Unmark season" else "Mark season watched",
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        when (val s = details) {
            UiState.Loading -> LoadingBox(Modifier.padding(padding))
            is UiState.Error -> ErrorBox(s.message, onRetry = vm::load, modifier = Modifier.padding(padding))
            is UiState.Success -> LazyColumn(
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = padding.calculateBottomPadding() + 16.dp,
                ),
            ) {
                s.data.overview?.takeIf { it.isNotBlank() }?.let { overview ->
                    item {
                        Text(
                            overview,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                }
                items(s.data.episodes, key = { it.id }) { ep ->
                    EpisodeCard(ep, ep.episodeNumber in watched) { vm.toggle(ep, it) }
                }
            }
        }
    }
}

@Composable
private fun EpisodeCard(ep: EpisodeDto, watched: Boolean, onToggle: (Boolean) -> Unit) {
    val aired = TracklRepository.isAired(ep.airDate)
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(Modifier.padding(12.dp).alpha(if (aired) 1f else 0.6f), verticalAlignment = Alignment.Top) {
            Poster(ep.stillPath, ep.name, Modifier.width(128.dp).aspectRatio(16f / 9f), size = "w300", corner = 6.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "E${ep.episodeNumber} · ${ep.name ?: "Episode ${ep.episodeNumber}"}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    listOfNotNull(formatDate(ep.airDate) ?: "TBA", formatRuntime(ep.runtime)).joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                ep.overview?.takeIf { it.isNotBlank() }?.let {
                    Spacer(Modifier.size(4.dp))
                    Text(it, style = MaterialTheme.typography.bodySmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
            }
            IconButton(onClick = { onToggle(!watched) }, enabled = aired || watched) {
                Icon(
                    if (watched) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle,
                    contentDescription = if (watched) "Mark unwatched" else "Mark watched",
                    tint = if (watched) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
