package com.trackl.app.data

import com.trackl.app.data.local.MovieEntity
import com.trackl.app.data.local.SeasonEntity
import com.trackl.app.data.local.ShowEntity
import com.trackl.app.data.local.WatchedEpisodeEntity
import com.trackl.app.data.remote.MediaItemDto
import com.trackl.app.data.remote.userMessage
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.serialization.Serializable

enum class MediaType { MOVIE, TV }

data class MediaItem(
    val id: Int,
    val title: String,
    val posterPath: String?,
    val type: MediaType,
    val year: String?,
    val rating: Double,
)

fun MediaItemDto.toMediaItem(defaultType: MediaType? = null): MediaItem? {
    val type = when (mediaType) {
        "movie" -> MediaType.MOVIE
        "tv" -> MediaType.TV
        null -> defaultType
        else -> null
    } ?: return null
    val title = (if (type == MediaType.MOVIE) title else name) ?: title ?: name ?: return null
    val date = if (type == MediaType.MOVIE) releaseDate else firstAirDate
    return MediaItem(id, title, posterPath, type, date?.take(4)?.takeIf { it.length == 4 }, voteAverage)
}

data class DiscoverSection(val title: String, val items: List<MediaItem>)

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

suspend fun <T> loadUi(block: suspend () -> T): UiState<T> = try {
    UiState.Success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    UiState.Error(e.userMessage())
}

data class EpisodeRef(val season: Int, val episode: Int) {
    val code: String get() = "S%02dE%02d".format(season, episode)
}

data class ShowProgress(
    val show: ShowEntity,
    val watchedCount: Int,
    val airedCount: Int,
    val next: EpisodeRef?,
    val lastWatchedAt: Long?,
) {
    val fraction: Float get() = if (airedCount == 0) 0f else (watchedCount.toFloat() / airedCount).coerceIn(0f, 1f)
}

data class Stats(
    val showsFollowed: Int = 0,
    val episodesWatched: Int = 0,
    val episodeMinutes: Long = 0,
    val moviesWatched: Int = 0,
    val movieMinutes: Long = 0,
    val watchlistCount: Int = 0,
)

@Serializable
data class Backup(
    val app: String = "trackl",
    val version: Int = 1,
    val exportedAt: Long,
    val shows: List<ShowEntity> = emptyList(),
    val seasons: List<SeasonEntity> = emptyList(),
    val watchedEpisodes: List<WatchedEpisodeEntity> = emptyList(),
    val movies: List<MovieEntity> = emptyList(),
)
