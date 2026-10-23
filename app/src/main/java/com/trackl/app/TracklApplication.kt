package com.trackl.app

import android.app.Application
import com.trackl.app.data.TracklRepository
import com.trackl.app.data.local.TracklDatabase
import com.trackl.app.data.remote.ApiClient

class AppContainer(app: Application) {
    private val api = ApiClient.create(BuildConfig.TMDB_API_KEY, app.cacheDir, BuildConfig.DEBUG)
    private val database = TracklDatabase.build(app)
    val repository = TracklRepository(api, database)
}

class TracklApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
