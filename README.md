# Trackl

A personal TV show & movie tracker for Android (a TV Time replacement), powered by [TMDb](https://www.themoviedb.org/).

## Features
- **My Shows**: *Watch next* (next unwatched episode per show, one-tap check-off), *Upcoming* (air dates of new episodes), *All shows* with progress bars. Pull to refresh.
- **Show details**: track/untrack, progress, seasons, and episode checklists (tick single episodes or a whole season).
- **My Movies**: watchlist and watched history.
- **Discover**: trending, popular, on the air, in theaters, top rated, plus search across movies and TV.
- **Profile**: watch-time stats, plus JSON **backup export/import** so your history is never locked in.
- All data stays on your device (Room database, also covered by Android auto-backup).

## Tech
Kotlin, Jetpack Compose (Material 3), Navigation Compose, Room, Retrofit + kotlinx.serialization, OkHttp (with HTTP cache), Coil. minSdk 26, targetSdk 35.

## Setup
1. Get a free TMDb API key: create an account at themoviedb.org, then go to **Settings → API** and request a key.
2. Put the key in `local.properties`. You can use either the v3 **API Key** or the v4 **API Read Access Token**:
   ```
   TMDB_API_KEY=your_key_here
   ```
3. Build:
   ```
   export JAVA_HOME=/opt/homebrew/opt/openjdk@17
   ./gradlew assembleDebug
   ```
   The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.
4. Install it on your phone. Either enable USB debugging and run `./gradlew installDebug`, or copy the APK to the phone and open it (allow "install unknown apps").

Run tests with `./gradlew testDebugUnitTest`.

## Project layout
```
app/src/main/java/com/trackl/app/
  data/remote   TMDb API, DTOs, client
  data/local    Room entities, DAOs, database
  data/         Repository (tracking, progress, stats, backup) and models
  ui/           Compose screens: shows, movies, discover, profile, show, movie, components
```

## Attribution
This product uses the TMDB API but is not endorsed or certified by TMDB.
