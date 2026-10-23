package com.trackl.app.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ShowDao {
    @Query("SELECT * FROM shows ORDER BY name COLLATE NOCASE")
    fun observeShows(): Flow<List<ShowEntity>>

    @Query("SELECT * FROM shows WHERE id = :id")
    fun observeShow(id: Int): Flow<ShowEntity?>

    @Query("SELECT * FROM shows WHERE id = :id")
    suspend fun getShow(id: Int): ShowEntity?

    @Query("SELECT * FROM shows")
    suspend fun getShows(): List<ShowEntity>

    @Upsert
    suspend fun upsertShow(show: ShowEntity)

    @Upsert
    suspend fun upsertShows(shows: List<ShowEntity>)

    @Query("DELETE FROM shows WHERE id = :id")
    suspend fun deleteShow(id: Int)

    @Query("SELECT * FROM seasons")
    fun observeAllSeasons(): Flow<List<SeasonEntity>>

    @Query("SELECT * FROM seasons")
    suspend fun getAllSeasons(): List<SeasonEntity>

    @Upsert
    suspend fun upsertSeasons(seasons: List<SeasonEntity>)

    @Query("DELETE FROM seasons WHERE showId = :showId")
    suspend fun deleteSeasons(showId: Int)

    @Query("SELECT * FROM watched_episodes")
    fun observeAllWatched(): Flow<List<WatchedEpisodeEntity>>

    @Query("SELECT * FROM watched_episodes")
    suspend fun getAllWatched(): List<WatchedEpisodeEntity>

    @Query("SELECT * FROM watched_episodes WHERE showId = :showId")
    fun observeWatched(showId: Int): Flow<List<WatchedEpisodeEntity>>

    @Upsert
    suspend fun upsertWatched(episodes: List<WatchedEpisodeEntity>)

    @Query("DELETE FROM watched_episodes WHERE showId = :showId AND season = :season AND episode = :episode")
    suspend fun deleteWatched(showId: Int, season: Int, episode: Int)

    @Query("DELETE FROM watched_episodes WHERE showId = :showId AND season = :season")
    suspend fun deleteWatchedSeason(showId: Int, season: Int)

    @Query("DELETE FROM watched_episodes WHERE showId = :showId")
    suspend fun deleteAllWatched(showId: Int)
}

@Dao
interface MovieDao {
    @Query("SELECT * FROM movies WHERE inWatchlist = 1 ORDER BY addedAt DESC")
    fun observeWatchlist(): Flow<List<MovieEntity>>

    @Query("SELECT * FROM movies WHERE watchedAt IS NOT NULL ORDER BY watchedAt DESC")
    fun observeWatched(): Flow<List<MovieEntity>>

    @Query("SELECT * FROM movies")
    fun observeAll(): Flow<List<MovieEntity>>

    @Query("SELECT * FROM movies WHERE id = :id")
    fun observeMovie(id: Int): Flow<MovieEntity?>

    @Query("SELECT * FROM movies WHERE id = :id")
    suspend fun getMovie(id: Int): MovieEntity?

    @Query("SELECT * FROM movies")
    suspend fun getAll(): List<MovieEntity>

    @Upsert
    suspend fun upsert(movie: MovieEntity)

    @Upsert
    suspend fun upsertAll(movies: List<MovieEntity>)

    @Query("DELETE FROM movies WHERE id = :id")
    suspend fun delete(id: Int)
}
