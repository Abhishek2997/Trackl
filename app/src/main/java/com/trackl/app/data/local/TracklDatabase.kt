package com.trackl.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ShowEntity::class, SeasonEntity::class, WatchedEpisodeEntity::class, MovieEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class TracklDatabase : RoomDatabase() {
    abstract fun showDao(): ShowDao
    abstract fun movieDao(): MovieDao

    companion object {
        fun build(context: Context): TracklDatabase =
            Room.databaseBuilder(context, TracklDatabase::class.java, "trackl.db").build()
    }
}
