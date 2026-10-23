package com.trackl.app.data

import androidx.room.withTransaction
import com.trackl.app.data.local.MovieEntity
import com.trackl.app.data.local.SeasonEntity
import com.trackl.app.data.local.ShowEntity
import com.trackl.app.data.local.TracklDatabase
import com.trackl.app.data.local.WatchedEpisodeEntity
import com.trackl.app.data.remote.ApiClient
import com.trackl.app.data.remote.EpisodeDto
import com.trackl.app.data.remote.MovieDetailsDto
import com.trackl.app.data.remote.SeasonDetailsDto
import com.trackl.app.data.remote.TmdbApi
import com.trackl.app.data.remote.TvDetailsDto
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

class TracklRepository(
    private val api: TmdbApi,
    private val db: TracklDatabase,
) {
    private val showDao = db.showDao()
    private val movieDao = db.movieDao()

    // ---------- Remote ----------

    suspend fun discoverSections(): List<DiscoverSection> = coroutineScope {
        val trending = async { api.trending("all").results.mapNotNull { it.toMediaItem() } }
        val popularTv = async { api.popularTv().results.mapNotNull { it.toMediaItem(MediaType.TV) } }
        val popularMovies = async { api.popularMovies().results.mapNotNull { it.toMediaItem(MediaType.MOVIE) } }
        val onAir = async { api.onTheAirTv().results.mapNotNull { it.toMediaItem(MediaType.TV) } }
        val nowPlaying = async { api.nowPlayingMovies().results.mapNotNull { it.toMediaItem(MediaType.MOVIE) } }
        val topTv = async { api.topRatedTv().results.mapNotNull { it.toMediaItem(MediaType.TV) } }
        val topMovies = async { api.topRatedMovies().results.mapNotNull { it.toMediaItem(MediaType.MOVIE) } }
        listOf(
            DiscoverSection("Trending this week", trending.await()),
            DiscoverSection("Popular shows", popularTv.await()),
            DiscoverSection("Popular movies", popularMovies.await()),
            DiscoverSection("On the air", onAir.await()),
            DiscoverSection("In theaters", nowPlaying.await()),
            DiscoverSection("Top rated shows", topTv.await()),
            DiscoverSection("Top rated movies", topMovies.await()),
        ).filter { it.items.isNotEmpty() }
    }

    suspend fun search(query: String): List<MediaItem> =
        api.searchMulti(query).results.mapNotNull { it.toMediaItem() }

    suspend fun movieDetails(id: Int): MovieDetailsDto = api.movie(id)

    suspend fun tvDetails(id: Int): TvDetailsDto = api.tv(id).also { details ->
        // Keep tracked shows fresh whenever we see new data.
        if (showDao.getShow(id) != null) saveShow(details)
    }

    suspend fun seasonDetails(showId: Int, season: Int): SeasonDetailsDto = api.season(showId, season)

    // ---------- Shows ----------

    fun observeShow(id: Int): Flow<ShowEntity?> = showDao.observeShow(id)

    fun observeWatched(showId: Int): Flow<List<WatchedEpisodeEntity>> = showDao.observeWatched(showId)

    fun observeShowProgress(): Flow<List<ShowProgress>> = combine(
        showDao.observeShows(),
        showDao.observeAllSeasons(),
        showDao.observeAllWatched(),
    ) { shows, seasons, watched ->
        val seasonsByShow = seasons.groupBy { it.showId }
        val watchedByShow = watched.groupBy { it.showId }
        shows.map { computeProgress(it, seasonsByShow[it.id].orEmpty(), watchedByShow[it.id].orEmpty()) }
    }.flowOn(Dispatchers.Default)

    fun observeProgress(showId: Int): Flow<ShowProgress?> =
        observeShowProgress().map { list -> list.firstOrNull { it.show.id == showId } }

    suspend fun follow(details: TvDetailsDto) = saveShow(details)

    suspend fun unfollow(showId: Int) = db.withTransaction {
        showDao.deleteAllWatched(showId)
        showDao.deleteSeasons(showId)
        showDao.deleteShow(showId)
    }

    private suspend fun ensureFollowed(showId: Int): ShowEntity =
        showDao.getShow(showId) ?: run {
            saveShow(api.tv(showId, append = null))
            showDao.getShow(showId)!!
        }

    suspend fun setEpisodeWatched(showId: Int, season: Int, episode: Int, runtime: Int?, watched: Boolean) {
        if (watched) {
            val show = ensureFollowed(showId)
            showDao.upsertWatched(
                listOf(WatchedEpisodeEntity(showId, season, episode, runtime ?: show.avgRuntime, System.currentTimeMillis()))
            )
        } else {
            showDao.deleteWatched(showId, season, episode)
        }
    }

    suspend fun setSeasonWatched(showId: Int, season: Int, episodes: List<EpisodeDto>, watched: Boolean) {
        if (watched) {
            val show = ensureFollowed(showId)
            val now = System.currentTimeMillis()
            showDao.upsertWatched(
                episodes.filter { isAired(it.airDate) }.map {
                    WatchedEpisodeEntity(showId, season, it.episodeNumber, it.runtime ?: show.avgRuntime, now)
                }
            )
        } else {
            showDao.deleteWatchedSeason(showId, season)
        }
    }

    suspend fun markNext(progress: ShowProgress) {
        val next = progress.next ?: return
        setEpisodeWatched(progress.show.id, next.season, next.episode, null, true)
    }

    /** Refreshes followed shows from TMDb. Returns number of failures. */
    suspend fun refreshShows(force: Boolean): Int = coroutineScope {
        val staleBefore = System.currentTimeMillis() - 6 * 60 * 60 * 1000L
        val gate = Semaphore(4)
        showDao.getShows()
            .filter { force || it.updatedAt < staleBefore }
            .map { show ->
                async {
                    gate.withPermit {
                        runCatching { saveShow(api.tv(show.id, append = null)) }.isFailure
                    }
                }
            }
            .awaitAll()
            .count { it }
    }

    private suspend fun saveShow(d: TvDetailsDto) {
        val existing = showDao.getShow(d.id)
        val now = System.currentTimeMillis()
        val runtime = d.episodeRunTime.firstOrNull { it > 0 }
            ?: d.lastEpisodeToAir?.runtime?.takeIf { it > 0 }
            ?: existing?.avgRuntime
            ?: 30
        val entity = ShowEntity(
            id = d.id,
            name = d.name,
            posterPath = d.posterPath,
            backdropPath = d.backdropPath,
            status = d.status,
            avgRuntime = runtime,
            lastAiredSeason = d.lastEpisodeToAir?.seasonNumber ?: 0,
            lastAiredEpisode = d.lastEpisodeToAir?.episodeNumber ?: 0,
            nextAirDate = d.nextEpisodeToAir?.airDate,
            nextSeason = d.nextEpisodeToAir?.seasonNumber,
            nextEpisode = d.nextEpisodeToAir?.episodeNumber,
            nextEpisodeName = d.nextEpisodeToAir?.name,
            addedAt = existing?.addedAt ?: now,
            updatedAt = now,
        )
        db.withTransaction {
            showDao.upsertShow(entity)
            showDao.deleteSeasons(d.id)
            showDao.upsertSeasons(d.seasons.map { SeasonEntity(d.id, it.seasonNumber, it.name, it.episodeCount) })
        }
    }

    // ---------- Movies ----------

    fun observeMovie(id: Int): Flow<MovieEntity?> = movieDao.observeMovie(id)
    fun observeMovieWatchlist(): Flow<List<MovieEntity>> = movieDao.observeWatchlist()
    fun observeWatchedMovies(): Flow<List<MovieEntity>> = movieDao.observeWatched()

    suspend fun toggleWatchlist(d: MovieDetailsDto) {
        val existing = movieDao.getMovie(d.id)
        val updated = (existing ?: d.toEntity()).let { it.copy(inWatchlist = !(existing?.inWatchlist ?: false)) }
        saveOrDelete(updated)
    }

    suspend fun toggleMovieWatched(d: MovieDetailsDto) {
        val existing = movieDao.getMovie(d.id)
        val base = existing ?: d.toEntity()
        val updated = if (base.watchedAt != null) {
            base.copy(watchedAt = null)
        } else {
            base.copy(watchedAt = System.currentTimeMillis(), inWatchlist = false, runtime = d.runtime ?: base.runtime)
        }
        saveOrDelete(updated)
    }

    private suspend fun saveOrDelete(movie: MovieEntity) {
        if (!movie.inWatchlist && movie.watchedAt == null) movieDao.delete(movie.id) else movieDao.upsert(movie)
    }

    private fun MovieDetailsDto.toEntity() = MovieEntity(
        id = id,
        title = title,
        posterPath = posterPath,
        releaseDate = releaseDate,
        runtime = runtime ?: 0,
        inWatchlist = false,
        watchedAt = null,
        addedAt = System.currentTimeMillis(),
    )

    // ---------- Stats & backup ----------

    fun observeStats(): Flow<Stats> = combine(
        showDao.observeShows(),
        showDao.observeAllWatched(),
        movieDao.observeAll(),
    ) { shows, watched, movies ->
        val watchedMovies = movies.filter { it.watchedAt != null }
        Stats(
            showsFollowed = shows.size,
            episodesWatched = watched.size,
            episodeMinutes = watched.sumOf { it.runtime.toLong() },
            moviesWatched = watchedMovies.size,
            movieMinutes = watchedMovies.sumOf { it.runtime.toLong() },
            watchlistCount = movies.count { it.inWatchlist },
        )
    }.flowOn(Dispatchers.Default)

    suspend fun exportBackup(): String {
        val backup = Backup(
            exportedAt = System.currentTimeMillis(),
            shows = showDao.getShows(),
            seasons = showDao.getAllSeasons(),
            watchedEpisodes = showDao.getAllWatched(),
            movies = movieDao.getAll(),
        )
        return ApiClient.json.encodeToString(Backup.serializer(), backup)
    }

    /** Merges a backup into the current library. Returns a short summary. */
    suspend fun importBackup(text: String): String {
        val backup = ApiClient.json.decodeFromString(Backup.serializer(), text)
        require(backup.app == "trackl") { "Not a Trackl backup file" }
        db.withTransaction {
            showDao.upsertShows(backup.shows)
            showDao.upsertSeasons(backup.seasons)
            showDao.upsertWatched(backup.watchedEpisodes)
            movieDao.upsertAll(backup.movies)
        }
        return "Imported ${backup.shows.size} shows, ${backup.watchedEpisodes.size} episodes, ${backup.movies.size} movies"
    }

    companion object {
        fun isAired(airDate: String?): Boolean =
            airDate != null && runCatching { !LocalDate.parse(airDate).isAfter(LocalDate.now()) }.getOrDefault(false)

        fun computeProgress(
            show: ShowEntity,
            seasons: List<SeasonEntity>,
            watched: List<WatchedEpisodeEntity>,
        ): ShowProgress {
            val watchedSet = watched.mapTo(HashSet()) { it.season to it.episode }
            var aired = 0
            var next: EpisodeRef? = null
            for (s in seasons.filter { it.seasonNumber > 0 }.sortedBy { it.seasonNumber }) {
                val airedInSeason = when {
                    s.seasonNumber < show.lastAiredSeason -> s.episodeCount
                    s.seasonNumber == show.lastAiredSeason -> show.lastAiredEpisode
                    else -> 0
                }
                aired += airedInSeason
                if (next == null) {
                    for (e in 1..airedInSeason) {
                        if ((s.seasonNumber to e) !in watchedSet) {
                            next = EpisodeRef(s.seasonNumber, e)
                            break
                        }
                    }
                }
            }
            return ShowProgress(
                show = show,
                watchedCount = watched.count { it.season > 0 },
                airedCount = aired,
                next = next,
                lastWatchedAt = watched.maxOfOrNull { it.watchedAt },
            )
        }
    }
}
