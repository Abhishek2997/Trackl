package com.trackl.app.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PagedResponse<T>(
    val page: Int = 1,
    val results: List<T> = emptyList(),
    @SerialName("total_pages") val totalPages: Int = 1,
)

@Serializable
data class MediaItemDto(
    val id: Int,
    val title: String? = null,
    val name: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("media_type") val mediaType: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("first_air_date") val firstAirDate: String? = null,
    @SerialName("vote_average") val voteAverage: Double = 0.0,
    val overview: String? = null,
)

@Serializable
data class GenreDto(val id: Int, val name: String)

@Serializable
data class CastDto(
    val id: Int,
    val name: String,
    val character: String? = null,
    @SerialName("profile_path") val profilePath: String? = null,
)

@Serializable
data class CreditsDto(val cast: List<CastDto> = emptyList())

@Serializable
data class NetworkDto(val id: Int, val name: String)

@Serializable
data class MovieDetailsDto(
    val id: Int,
    val title: String,
    val overview: String? = null,
    val tagline: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    val runtime: Int? = null,
    val status: String? = null,
    @SerialName("vote_average") val voteAverage: Double = 0.0,
    val genres: List<GenreDto> = emptyList(),
    val credits: CreditsDto? = null,
    val recommendations: PagedResponse<MediaItemDto>? = null,
)

@Serializable
data class EpisodeDto(
    val id: Int,
    val name: String? = null,
    val overview: String? = null,
    @SerialName("season_number") val seasonNumber: Int,
    @SerialName("episode_number") val episodeNumber: Int,
    @SerialName("air_date") val airDate: String? = null,
    val runtime: Int? = null,
    @SerialName("still_path") val stillPath: String? = null,
    @SerialName("vote_average") val voteAverage: Double = 0.0,
)

@Serializable
data class SeasonSummaryDto(
    val id: Int,
    val name: String? = null,
    @SerialName("season_number") val seasonNumber: Int,
    @SerialName("episode_count") val episodeCount: Int = 0,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("air_date") val airDate: String? = null,
)

@Serializable
data class SeasonDetailsDto(
    val id: Int,
    val name: String? = null,
    val overview: String? = null,
    @SerialName("season_number") val seasonNumber: Int,
    @SerialName("poster_path") val posterPath: String? = null,
    val episodes: List<EpisodeDto> = emptyList(),
)

@Serializable
data class TvDetailsDto(
    val id: Int,
    val name: String,
    val overview: String? = null,
    val tagline: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("first_air_date") val firstAirDate: String? = null,
    val status: String? = null,
    @SerialName("vote_average") val voteAverage: Double = 0.0,
    @SerialName("number_of_seasons") val numberOfSeasons: Int = 0,
    @SerialName("number_of_episodes") val numberOfEpisodes: Int = 0,
    @SerialName("episode_run_time") val episodeRunTime: List<Int> = emptyList(),
    val genres: List<GenreDto> = emptyList(),
    val networks: List<NetworkDto> = emptyList(),
    val seasons: List<SeasonSummaryDto> = emptyList(),
    @SerialName("last_episode_to_air") val lastEpisodeToAir: EpisodeDto? = null,
    @SerialName("next_episode_to_air") val nextEpisodeToAir: EpisodeDto? = null,
    val credits: CreditsDto? = null,
    val recommendations: PagedResponse<MediaItemDto>? = null,
)
