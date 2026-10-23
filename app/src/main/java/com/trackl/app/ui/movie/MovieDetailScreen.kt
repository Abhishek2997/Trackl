package com.trackl.app.ui.movie

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.trackl.app.data.MediaItem
import com.trackl.app.data.MediaType
import com.trackl.app.data.TracklRepository
import com.trackl.app.data.UiState
import com.trackl.app.data.loadUi
import com.trackl.app.data.local.MovieEntity
import com.trackl.app.data.remote.MovieDetailsDto
import com.trackl.app.data.toMediaItem
import com.trackl.app.ui.components.BackButton
import com.trackl.app.ui.components.CastRow
import com.trackl.app.ui.components.DetailHeader
import com.trackl.app.ui.components.ErrorBox
import com.trackl.app.ui.components.LoadingBox
import com.trackl.app.ui.components.MediaRow
import com.trackl.app.ui.components.appViewModel
import com.trackl.app.ui.components.formatDate
import com.trackl.app.ui.components.formatEpochDate
import com.trackl.app.ui.components.formatRuntime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MovieDetailViewModel(private val movieId: Int, private val repo: TracklRepository) : ViewModel() {
    private val _details = MutableStateFlow<UiState<MovieDetailsDto>>(UiState.Loading)
    val details = _details.asStateFlow()

    val tracked = repo.observeMovie(movieId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _details.value = UiState.Loading
            _details.value = loadUi { repo.movieDetails(movieId) }
        }
    }

    fun toggleWatchlist(d: MovieDetailsDto) = viewModelScope.launch { repo.toggleWatchlist(d) }
    fun toggleWatched(d: MovieDetailsDto) = viewModelScope.launch { repo.toggleMovieWatched(d) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieDetailScreen(movieId: Int, onBack: () -> Unit, onOpen: (MediaItem) -> Unit) {
    val vm = appViewModel { MovieDetailViewModel(movieId, it.repository) }
    val details by vm.details.collectAsStateWithLifecycle()
    val tracked by vm.tracked.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text((details as? UiState.Success)?.data?.title ?: "", maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                navigationIcon = { BackButton(onBack) },
            )
        },
    ) { padding ->
        when (val s = details) {
            UiState.Loading -> LoadingBox(Modifier.padding(padding))
            is UiState.Error -> ErrorBox(s.message, onRetry = vm::load, modifier = Modifier.padding(padding))
            is UiState.Success -> MovieContent(
                movie = s.data,
                tracked = tracked,
                contentPadding = padding,
                onToggleWatchlist = { vm.toggleWatchlist(s.data) },
                onToggleWatched = { vm.toggleWatched(s.data) },
                onOpen = onOpen,
            )
        }
    }
}

@Composable
private fun MovieContent(
    movie: MovieDetailsDto,
    tracked: MovieEntity?,
    contentPadding: PaddingValues,
    onToggleWatchlist: () -> Unit,
    onToggleWatched: () -> Unit,
    onOpen: (MediaItem) -> Unit,
) {
    val inWatchlist = tracked?.inWatchlist == true
    val watched = tracked?.watchedAt != null
    LazyColumn(contentPadding = contentPadding) {
        item {
            DetailHeader(
                backdropPath = movie.backdropPath,
                posterPath = movie.posterPath,
                title = movie.title,
                subtitle = listOfNotNull(movie.releaseDate?.take(4), formatRuntime(movie.runtime)).joinToString(" · "),
                rating = movie.voteAverage,
                genres = movie.genres.map { it.name },
            )
        }
        item {
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                FilledTonalButton(onClick = onToggleWatchlist, modifier = Modifier.weight(1f)) {
                    Icon(if (inWatchlist) Icons.Filled.BookmarkAdded else Icons.Outlined.BookmarkAdd, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (inWatchlist) "In watchlist" else "Watchlist")
                }
                if (watched) {
                    Button(onClick = onToggleWatched, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.CheckCircle, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Watched")
                    }
                } else {
                    OutlinedButton(onClick = onToggleWatched, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Outlined.CheckCircle, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Mark watched")
                    }
                }
            }
        }
        if (watched) {
            item {
                Text(
                    "Watched on ${formatEpochDate(tracked!!.watchedAt!!)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }
        item {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                movie.tagline?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, fontStyle = FontStyle.Italic)
                    Spacer(Modifier.size(8.dp))
                }
                Text(movie.overview?.takeIf { it.isNotBlank() } ?: "No overview available.", style = MaterialTheme.typography.bodyMedium)
                formatDate(movie.releaseDate)?.let {
                    Spacer(Modifier.size(8.dp))
                    Text(
                        "Release date: $it" + (movie.status?.let { s -> " · $s" } ?: ""),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        item { CastRow(movie.credits?.cast.orEmpty()) }
        item {
            MediaRow(
                "More like this",
                movie.recommendations?.results.orEmpty().mapNotNull { it.toMediaItem(MediaType.MOVIE) },
                onOpen,
            )
        }
        item { Spacer(Modifier.size(24.dp)) }
    }
}
