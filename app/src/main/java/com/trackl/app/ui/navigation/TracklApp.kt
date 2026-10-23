package com.trackl.app.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.trackl.app.data.MediaItem
import com.trackl.app.data.MediaType
import com.trackl.app.ui.discover.DiscoverScreen
import com.trackl.app.ui.movie.MovieDetailScreen
import com.trackl.app.ui.movies.MoviesScreen
import com.trackl.app.ui.profile.ProfileScreen
import com.trackl.app.ui.show.SeasonScreen
import com.trackl.app.ui.show.ShowDetailScreen
import com.trackl.app.ui.shows.ShowsScreen

private data class TopDestination(val route: String, val label: String, val icon: ImageVector)

private val topDestinations = listOf(
    TopDestination("shows", "Shows", Icons.Filled.Tv),
    TopDestination("movies", "Movies", Icons.Filled.Movie),
    TopDestination("discover", "Discover", Icons.Filled.Explore),
    TopDestination("profile", "Profile", Icons.Filled.Person),
)

private fun NavHostController.switchTab(route: String) = navigate(route) {
    popUpTo(graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState = true
}

@Composable
fun TracklApp() {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBottomBar = topDestinations.any { it.route == currentRoute }

    val openMedia: (MediaItem) -> Unit = { item ->
        nav.navigate(if (item.type == MediaType.MOVIE) "movie/${item.id}" else "show/${item.id}")
    }
    val openShow: (Int) -> Unit = { nav.navigate("show/$it") }
    val openMovie: (Int) -> Unit = { nav.navigate("movie/$it") }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    topDestinations.forEach { dest ->
                        NavigationBarItem(
                            selected = currentRoute == dest.route,
                            onClick = { nav.switchTab(dest.route) },
                            icon = { Icon(dest.icon, contentDescription = null) },
                            label = { Text(dest.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(nav, startDestination = "shows", modifier = Modifier.padding(padding)) {
            composable("shows") {
                ShowsScreen(onOpenShow = openShow, onDiscover = { nav.switchTab("discover") })
            }
            composable("movies") {
                MoviesScreen(onOpenMovie = openMovie, onDiscover = { nav.switchTab("discover") })
            }
            composable("discover") {
                DiscoverScreen(onOpen = openMedia)
            }
            composable("profile") {
                ProfileScreen()
            }
            composable("movie/{id}", arguments = listOf(navArgument("id") { type = NavType.IntType })) { entry ->
                MovieDetailScreen(
                    movieId = entry.arguments!!.getInt("id"),
                    onBack = { nav.popBackStack() },
                    onOpen = openMedia,
                )
            }
            composable("show/{id}", arguments = listOf(navArgument("id") { type = NavType.IntType })) { entry ->
                val id = entry.arguments!!.getInt("id")
                ShowDetailScreen(
                    showId = id,
                    onBack = { nav.popBackStack() },
                    onOpen = openMedia,
                    onOpenSeason = { season -> nav.navigate("season/$id/$season") },
                )
            }
            composable(
                "season/{showId}/{season}",
                arguments = listOf(
                    navArgument("showId") { type = NavType.IntType },
                    navArgument("season") { type = NavType.IntType },
                ),
            ) { entry ->
                SeasonScreen(
                    showId = entry.arguments!!.getInt("showId"),
                    season = entry.arguments!!.getInt("season"),
                    onBack = { nav.popBackStack() },
                )
            }
        }
    }
}
