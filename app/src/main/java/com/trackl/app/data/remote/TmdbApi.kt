package com.trackl.app.data.remote

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TmdbApi {
    @GET("trending/{type}/week")
    suspend fun trending(@Path("type") type: String): PagedResponse<MediaItemDto>

    @GET("movie/popular")
    suspend fun popularMovies(@Query("page") page: Int = 1): PagedResponse<MediaItemDto>

    @GET("movie/top_rated")
    suspend fun topRatedMovies(@Query("page") page: Int = 1): PagedResponse<MediaItemDto>

    @GET("movie/now_playing")
    suspend fun nowPlayingMovies(@Query("page") page: Int = 1): PagedResponse<MediaItemDto>

    @GET("tv/popular")
    suspend fun popularTv(@Query("page") page: Int = 1): PagedResponse<MediaItemDto>

    @GET("tv/top_rated")
    suspend fun topRatedTv(@Query("page") page: Int = 1): PagedResponse<MediaItemDto>

    @GET("tv/on_the_air")
    suspend fun onTheAirTv(@Query("page") page: Int = 1): PagedResponse<MediaItemDto>

    @GET("search/multi")
    suspend fun searchMulti(
        @Query("query") query: String,
        @Query("page") page: Int = 1,
        @Query("include_adult") includeAdult: Boolean = false,
    ): PagedResponse<MediaItemDto>

    @GET("movie/{id}")
    suspend fun movie(
        @Path("id") id: Int,
        @Query("append_to_response") append: String? = "credits,recommendations",
    ): MovieDetailsDto

    @GET("tv/{id}")
    suspend fun tv(
        @Path("id") id: Int,
        @Query("append_to_response") append: String? = "credits,recommendations",
    ): TvDetailsDto

    @GET("tv/{id}/season/{season}")
    suspend fun season(
        @Path("id") id: Int,
        @Path("season") season: Int,
    ): SeasonDetailsDto
}
