package com.trackl.app.data

import com.trackl.app.data.local.SeasonEntity
import com.trackl.app.data.local.ShowEntity
import com.trackl.app.data.local.WatchedEpisodeEntity
import com.trackl.app.data.remote.ApiClient
import com.trackl.app.data.remote.MediaItemDto
import com.trackl.app.data.remote.PagedResponse
import com.trackl.app.data.remote.TvDetailsDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TracklLogicTest {
    private fun show(lastSeason: Int, lastEpisode: Int) = ShowEntity(
        id = 1, name = "Show", posterPath = null, backdropPath = null, status = null, avgRuntime = 45,
        lastAiredSeason = lastSeason, lastAiredEpisode = lastEpisode, nextAirDate = null, nextSeason = null,
        nextEpisode = null, nextEpisodeName = null, addedAt = 0, updatedAt = 0,
    )

    private val seasons = listOf(
        SeasonEntity(1, 0, "Specials", 3),
        SeasonEntity(1, 1, "S1", 3),
        SeasonEntity(1, 2, "S2", 4),
    )

    private fun w(s: Int, e: Int) = WatchedEpisodeEntity(1, s, e, 45, (s * 100 + e).toLong())

    @Test
    fun nextEpisodeIsFirstUnwatchedAiredEpisode() {
        val p = TracklRepository.computeProgress(show(2, 2), seasons, listOf(w(1, 1), w(1, 2), w(1, 3)))
        assertEquals(EpisodeRef(2, 1), p.next)
        assertEquals(5, p.airedCount)
        assertEquals(3, p.watchedCount)
        assertEquals("S02E01", p.next!!.code)
    }

    @Test
    fun gapsAreSuggestedFirstAndSpecialsIgnored() {
        val p = TracklRepository.computeProgress(show(2, 2), seasons, listOf(w(0, 1), w(1, 1), w(1, 3)))
        assertEquals(EpisodeRef(1, 2), p.next)
        assertEquals(2, p.watchedCount)
    }

    @Test
    fun caughtUpWhenAllAiredWatched() {
        val watched = listOf(w(1, 1), w(1, 2), w(1, 3), w(2, 1), w(2, 2))
        val p = TracklRepository.computeProgress(show(2, 2), seasons, watched)
        assertNull(p.next)
        assertEquals(1f, p.fraction)
    }

    @Test
    fun parsesTmdbTvDetails() {
        val json = """
            {"id":1399,"name":"Game of Thrones","poster_path":"/p.jpg","status":"Ended","unknown_field":true,
             "episode_run_time":[],"genres":[{"id":18,"name":"Drama"}],
             "seasons":[{"id":1,"name":"Season 1","season_number":1,"episode_count":10,"air_date":null}],
             "last_episode_to_air":{"id":9,"name":"The Iron Throne","season_number":8,"episode_number":6,"runtime":80,"air_date":"2019-05-19"},
             "next_episode_to_air":null,"vote_average":null}
        """.trimIndent()
        val d = ApiClient.json.decodeFromString(TvDetailsDto.serializer(), json)
        assertEquals("Game of Thrones", d.name)
        assertEquals(8, d.lastEpisodeToAir!!.seasonNumber)
        assertEquals(0.0, d.voteAverage, 0.0)
        assertEquals(10, d.seasons.first().episodeCount)
    }

    @Test
    fun searchMultiMapsTypesAndDropsPeople() {
        val json = """
            {"page":1,"total_pages":1,"results":[
              {"id":1,"media_type":"movie","title":"Inception","release_date":"2010-07-15"},
              {"id":2,"media_type":"tv","name":"Dark","first_air_date":"2017-12-01"},
              {"id":3,"media_type":"person","name":"Someone"}]}
        """.trimIndent()
        val page = ApiClient.json.decodeFromString(PagedResponse.serializer(MediaItemDto.serializer()), json)
        val items = page.results.mapNotNull { it.toMediaItem() }
        assertEquals(listOf("Inception" to MediaType.MOVIE, "Dark" to MediaType.TV), items.map { it.title to it.type })
        assertEquals("2010", items.first().year)
    }

    @Test
    fun backupRoundTrips() {
        val backup = Backup(exportedAt = 5, shows = listOf(show(1, 1)), watchedEpisodes = listOf(w(1, 1)))
        val text = ApiClient.json.encodeToString(Backup.serializer(), backup)
        assertEquals(backup, ApiClient.json.decodeFromString(Backup.serializer(), text))
    }
}
