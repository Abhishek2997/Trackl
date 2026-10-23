package com.trackl.app.ui.movies

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.trackl.app.data.TracklRepository
import com.trackl.app.data.local.MovieEntity
import com.trackl.app.ui.components.EmptyState
import com.trackl.app.ui.components.LoadingBox
import com.trackl.app.ui.components.Poster
import com.trackl.app.ui.components.appViewModel
import com.trackl.app.ui.components.formatEpochDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class MoviesViewModel(repo: TracklRepository) : ViewModel() {
    val watchlist = repo.observeMovieWatchlist()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val watched = repo.observeWatchedMovies()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoviesScreen(onOpenMovie: (Int) -> Unit, onDiscover: () -> Unit) {
    val vm = appViewModel { MoviesViewModel(it.repository) }
    val watchlist by vm.watchlist.collectAsStateWithLifecycle()
    val watched by vm.watched.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("My Movies") }) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Watchlist") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Watched") })
            }
            val list = if (tab == 0) watchlist else watched
            when {
                list == null -> LoadingBox()
                list.isEmpty() && tab == 0 -> EmptyState(
                    Icons.Outlined.BookmarkBorder,
                    "Your watchlist is empty",
                    "Save movies you want to watch from Discover.",
                    actionLabel = "Discover movies",
                    onAction = onDiscover,
                )
                list.isEmpty() -> EmptyState(Icons.Outlined.Movie, "No movies watched yet", "Mark a movie as watched to see it here.")
                else -> MovieGrid(list, showWatchedDate = tab == 1, onOpenMovie = onOpenMovie)
            }
        }
    }
}

@Composable
private fun MovieGrid(movies: List<MovieEntity>, showWatchedDate: Boolean, onOpenMovie: (Int) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(108.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(movies, key = { it.id }) { m ->
            Column(Modifier.clickable { onOpenMovie(m.id) }) {
                Poster(m.posterPath, m.title, Modifier.fillMaxWidth().aspectRatio(2f / 3f))
                Spacer(Modifier.height(6.dp))
                Text(m.title, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val sub = if (showWatchedDate) m.watchedAt?.let(::formatEpochDate) else m.releaseDate?.take(4)
                sub?.let {
                    Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
