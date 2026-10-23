package com.trackl.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "shows")
data class ShowEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val posterPath: String?,
    val backdropPath: String?,
    val status: String?,
    val avgRuntime: Int,
    val lastAiredSeason: Int,
    val lastAiredEpisode: Int,
    val nextAirDate: String?,
    val nextSeason: Int?,
    val nextEpisode: Int?,
    val nextEpisodeName: String?,
    val addedAt: Long,
    val updatedAt: Long,
)

@Serializable
@Entity(tableName = "seasons", primaryKeys = ["showId", "seasonNumber"])
data class SeasonEntity(
    val showId: Int,
    val seasonNumber: Int,
    val name: String?,
    val episodeCount: Int,
)

@Serializable
@Entity(tableName = "watched_episodes", primaryKeys = ["showId", "season", "episode"])
data class WatchedEpisodeEntity(
    val showId: Int,
    val season: Int,
    val episode: Int,
    val runtime: Int,
    val watchedAt: Long,
)

@Serializable
@Entity(tableName = "movies")
data class MovieEntity(
    @PrimaryKey val id: Int,
    val title: String,
    val posterPath: String?,
    val releaseDate: String?,
    val runtime: Int,
    val inWatchlist: Boolean,
    val watchedAt: Long?,
    val addedAt: Long,
)
