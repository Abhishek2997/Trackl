package com.trackl.app.ui.discover

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.trackl.app.data.DiscoverSection
import com.trackl.app.data.MediaItem
import com.trackl.app.data.MediaType
import com.trackl.app.data.TracklRepository
import com.trackl.app.data.UiState
import com.trackl.app.data.loadUi
import com.trackl.app.ui.components.EmptyState
import com.trackl.app.ui.components.ErrorBox
import com.trackl.app.ui.components.LoadingBox
import com.trackl.app.ui.components.MediaRow
import com.trackl.app.ui.components.Poster
import com.trackl.app.ui.components.appViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class DiscoverViewModel(private val repo: TracklRepository) : ViewModel() {
    private val _sections = MutableStateFlow<UiState<List<DiscoverSection>>>(UiState.Loading)
    val sections = _sections.asStateFlow()

    private val _query = MutableStateFlow("")
    val query = _query.asStateFlow()

    val results: StateFlow<UiState<List<MediaItem>>> = _query
        .debounce(350)
        .map { it.trim() }
        .distinctUntilChanged()
        .flatMapLatest { q ->
            if (q.isEmpty()) flowOf(UiState.Success(emptyList()))
            else flow {
                emit(UiState.Loading)
                emit(loadUi { repo.search(q) })
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Success(emptyList()))

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _sections.value = UiState.Loading
            _sections.value = loadUi { repo.discoverSections() }
        }
    }

    fun onQueryChange(value: String) {
        _query.value = value
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(onOpen: (MediaItem) -> Unit) {
    val vm = appViewModel { DiscoverViewModel(it.repository) }
    val sections by vm.sections.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val results by vm.results.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Discover") }) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = query,
                onValueChange = vm::onQueryChange,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                placeholder = { Text("Search movies & TV shows") },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { vm.onQueryChange("") }) { Icon(Icons.Filled.Close, "Clear") }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
            )
            if (query.isBlank()) {
                when (val s = sections) {
                    UiState.Loading -> LoadingBox()
                    is UiState.Error -> ErrorBox(s.message, onRetry = vm::load)
                    is UiState.Success -> LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                        items(s.data, key = { it.title }) { section ->
                            MediaRow(section.title, section.items, onOpen)
                        }
                    }
                }
            } else {
                SearchResults(results, onOpen)
            }
        }
    }
}

@Composable
private fun SearchResults(state: UiState<List<MediaItem>>, onOpen: (MediaItem) -> Unit) {
    when (state) {
        UiState.Loading -> LoadingBox()
        is UiState.Error -> EmptyState(Icons.Outlined.SearchOff, "Search failed", state.message)
        is UiState.Success -> if (state.data.isEmpty()) {
            EmptyState(Icons.Outlined.SearchOff, "No results", "Try a different title.")
        } else {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 8.dp)) {
                items(state.data, key = { "${it.type}-${it.id}" }) { item ->
                    ListItem(
                        modifier = Modifier.clickable { onOpen(item) },
                        leadingContent = {
                            Poster(item.posterPath, item.title, Modifier.width(52.dp).aspectRatio(2f / 3f), size = "w154")
                        },
                        headlineContent = { Text(item.title, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                        supportingContent = {
                            Text(
                                listOfNotNull(
                                    if (item.type == MediaType.TV) "TV show" else "Movie",
                                    item.year,
                                    item.rating.takeIf { it > 0 }?.let { "★ %.1f".format(it) },
                                ).joinToString(" · "),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                    )
                }
            }
        }
    }
}
