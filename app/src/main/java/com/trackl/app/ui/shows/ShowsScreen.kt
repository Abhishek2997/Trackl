package com.trackl.app.ui.shows

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Celebration
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.trackl.app.data.ShowProgress
import com.trackl.app.data.TracklRepository
import com.trackl.app.ui.components.EmptyState
import com.trackl.app.ui.components.LoadingBox
import com.trackl.app.ui.components.Poster
import com.trackl.app.ui.components.appViewModel
import com.trackl.app.ui.components.relativeAirDate
import java.time.LocalDate
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ShowsViewModel(private val repo: TracklRepository) : ViewModel() {
    val shows = repo.observeShowProgress()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _refreshing = MutableStateFlow(false)
    val refreshing = _refreshing.asStateFlow()

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    init {
        refresh(force = false)
    }

    fun refresh(force: Boolean = true) {
        if (_refreshing.value) return
        viewModelScope.launch {
            _refreshing.value = true
            val failures = repo.refreshShows(force)
            _refreshing.value = false
            if (failures > 0) _messages.send("Couldn't refresh $failures show(s). Check your connection.")
        }
    }

    fun markNext(p: ShowProgress) = viewModelScope.launch {
        runCatching { repo.markNext(p) }.onSuccess {
            _messages.send("${p.show.name} ${p.next?.code} marked as watched")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShowsScreen(onOpenShow: (Int) -> Unit, onDiscover: () -> Unit) {
    val vm = appViewModel { ShowsViewModel(it.repository) }
    val shows by vm.shows.collectAsStateWithLifecycle()
    val refreshing by vm.refreshing.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        vm.messages.collect { snackbar.showSnackbar(it) }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("My Shows") }) },
        snackbarHost = { SnackbarHost(snackbar) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            TabRow(selectedTabIndex = tab) {
                listOf("Watch next", "Upcoming", "All shows").forEachIndexed { i, label ->
                    Tab(selected = tab == i, onClick = { tab = i }, text = { Text(label) })
                }
            }
            val list = shows
            PullToRefreshBox(
                isRefreshing = refreshing,
                onRefresh = { vm.refresh(force = true) },
                modifier = Modifier.fillMaxSize(),
            ) {
                when {
                    list == null -> LoadingBox()
                    list.isEmpty() -> ScrollableEmpty {
                        EmptyState(
                            Icons.Outlined.Tv,
                            "No shows yet",
                            "Find shows in Discover and add them to start tracking episodes.",
                            actionLabel = "Discover shows",
                            onAction = onDiscover,
                        )
                    }
                    tab == 0 -> WatchNextList(list, onOpenShow, vm::markNext)
                    tab == 1 -> UpcomingList(list, onOpenShow)
                    else -> AllShowsGrid(list, onOpenShow)
                }
            }
        }
    }
}

/** Empty states need to be scrollable so pull-to-refresh still works. */
@Composable
private fun ScrollableEmpty(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) { content() }
}

@Composable
private fun WatchNextList(list: List<ShowProgress>, onOpenShow: (Int) -> Unit, onMarkNext: (ShowProgress) -> Unit) {
    val items = remember(list) {
        list.filter { it.next != null }
            .sortedWith(
                compareByDescending<ShowProgress> { it.lastWatchedAt ?: 0L }
                    .thenByDescending { it.show.addedAt }
            )
    }
    if (items.isEmpty()) {
        ScrollableEmpty {
            EmptyState(Icons.Outlined.Celebration, "All caught up", "No unwatched aired episodes. Check Upcoming for what's next.")
        }
        return
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 8.dp)) {
        items(items, key = { it.show.id }) { p ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clickable { onOpenShow(p.show.id) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Poster(p.show.posterPath, p.show.name, Modifier.width(60.dp).aspectRatio(2f / 3f), size = "w154", corner = 6.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(p.show.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(p.next!!.code, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(6.dp))
                        LinearProgressIndicator(progress = { p.fraction }, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "${p.watchedCount}/${p.airedCount} · ${p.airedCount - p.watchedCount.coerceAtMost(p.airedCount)} left",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    FilledIconButton(onClick = { onMarkNext(p) }) {
                        Icon(Icons.Filled.Check, contentDescription = "Mark ${p.next?.code} watched")
                    }
                }
            }
        }
    }
}

@Composable
private fun UpcomingList(list: List<ShowProgress>, onOpenShow: (Int) -> Unit) {
    val today = remember { LocalDate.now().toString() }
    val items = remember(list) {
        list.filter { it.show.nextAirDate != null && it.show.nextAirDate >= today }
            .sortedBy { it.show.nextAirDate }
    }
    if (items.isEmpty()) {
        ScrollableEmpty {
            EmptyState(Icons.Outlined.CalendarMonth, "Nothing scheduled", "Upcoming episodes of your shows will appear here.")
        }
        return
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 8.dp)) {
        items(items, key = { it.show.id }) { p ->
            val s = p.show
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { onOpenShow(s.id) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Poster(s.posterPath, s.name, Modifier.width(52.dp).aspectRatio(2f / 3f), size = "w154", corner = 6.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(s.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    val code = if (s.nextSeason != null && s.nextEpisode != null) "S%02dE%02d".format(s.nextSeason, s.nextEpisode) else null
                    Text(
                        listOfNotNull(code, s.nextEpisodeName).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    relativeAirDate(s.nextAirDate) ?: "",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun AllShowsGrid(list: List<ShowProgress>, onOpenShow: (Int) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(108.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(list, key = { it.show.id }) { p ->
            Column(Modifier.clickable { onOpenShow(p.show.id) }) {
                Poster(p.show.posterPath, p.show.name, Modifier.fillMaxWidth().aspectRatio(2f / 3f))
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(progress = { p.fraction }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(4.dp))
                Text(p.show.name, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    if (p.next == null && p.airedCount > 0) "Up to date" else "${p.watchedCount}/${p.airedCount}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
