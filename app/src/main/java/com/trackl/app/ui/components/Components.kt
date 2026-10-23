package com.trackl.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import com.trackl.app.AppContainer
import com.trackl.app.TracklApplication
import com.trackl.app.data.MediaItem
import com.trackl.app.data.MediaType
import com.trackl.app.data.remote.ApiClient
import com.trackl.app.data.remote.CastDto
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Composable
inline fun <reified VM : ViewModel> appViewModel(crossinline create: (AppContainer) -> VM): VM {
    val app = LocalContext.current.applicationContext as TracklApplication
    return viewModel(factory = viewModelFactory { initializer { create(app.container) } })
}

@Composable
fun BackButton(onBack: () -> Unit) {
    IconButton(onClick = onBack) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
    }
}

@Composable
fun Poster(
    path: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: String = "w342",
    corner: Dp = 8.dp,
) {
    Box(
        modifier
            .clip(RoundedCornerShape(corner))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (path == null) {
            Icon(Icons.Outlined.Image, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            AsyncImage(
                model = ApiClient.image(path, size),
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
fun MediaCard(item: MediaItem, onClick: () -> Unit, modifier: Modifier = Modifier, width: Dp = 116.dp) {
    Column(
        modifier
            .width(width)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
    ) {
        Poster(item.posterPath, item.title, Modifier.fillMaxWidth().aspectRatio(2f / 3f))
        Spacer(Modifier.height(6.dp))
        Text(
            item.title,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            listOfNotNull(if (item.type == MediaType.TV) "TV" else "Movie", item.year).joinToString(" · "),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun SectionTitle(title: String, modifier: Modifier = Modifier) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
fun MediaRow(title: String, items: List<MediaItem>, onItemClick: (MediaItem) -> Unit) {
    if (items.isEmpty()) return
    Column(Modifier.padding(vertical = 4.dp)) {
        SectionTitle(title)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(items, key = { "${it.type}-${it.id}" }) { item ->
                MediaCard(item, onClick = { onItemClick(item) })
            }
        }
    }
}

@Composable
fun CastRow(cast: List<CastDto>) {
    if (cast.isEmpty()) return
    Column(Modifier.padding(vertical = 4.dp)) {
        SectionTitle("Cast")
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(cast.take(20), key = { it.id }) { person ->
                Column(Modifier.width(80.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (person.profilePath == null) {
                            Icon(Icons.Filled.Person, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            AsyncImage(
                                model = ApiClient.image(person.profilePath, "w185"),
                                contentDescription = person.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        person.name,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                    )
                    person.character?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DetailHeader(
    backdropPath: String?,
    posterPath: String?,
    title: String,
    subtitle: String,
    rating: Double,
    genres: List<String>,
) {
    val bg = MaterialTheme.colorScheme.background
    Box(Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f)) {
            if (backdropPath != null) {
                AsyncImage(
                    model = ApiClient.image(backdropPath, "w780"),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, bg.copy(alpha = 0.6f), bg)))
            )
        }
        Row(
            Modifier
                .padding(start = 16.dp, end = 16.dp, top = 120.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
        ) {
            Poster(posterPath, title, Modifier.width(110.dp).aspectRatio(2f / 3f))
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                if (subtitle.isNotBlank()) {
                    Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (rating > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Star, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("%.1f".format(rating), style = MaterialTheme.typography.bodyMedium)
                    }
                }
                if (genres.isNotEmpty()) {
                    Text(
                        genres.joinToString(", "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                    )
                }
            }
        }
    }
}

@Composable
fun LoadingBox(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}

@Composable
fun ErrorBox(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    EmptyState(Icons.Outlined.CloudOff, "Couldn't load", message, modifier, actionLabel = "Retry", onAction = onRetry)
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(16.dp))
            Button(onClick = onAction) { Text(actionLabel) }
        }
    }
}

private val prettyDate = DateTimeFormatter.ofPattern("d MMM yyyy")

fun formatDate(date: String?): String? =
    date?.let { runCatching { LocalDate.parse(it).format(prettyDate) }.getOrNull() }

fun relativeAirDate(date: String?): String? {
    val d = date?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return null
    val days = ChronoUnit.DAYS.between(LocalDate.now(), d)
    return when {
        days < 0 -> "Aired ${d.format(prettyDate)}"
        days == 0L -> "Today"
        days == 1L -> "Tomorrow"
        days < 7 -> "In $days days"
        else -> d.format(prettyDate)
    }
}

fun formatRuntime(minutes: Int?): String? = minutes?.takeIf { it > 0 }?.let {
    if (it >= 60) "${it / 60}h ${it % 60}m" else "${it}m"
}

fun formatWatchTime(minutes: Long): String {
    if (minutes <= 0) return "0h"
    val months = minutes / (60 * 24 * 30)
    val days = (minutes / (60 * 24)) % 30
    val hours = (minutes / 60) % 24
    val mins = minutes % 60
    return buildList {
        if (months > 0) add("${months}mo")
        if (days > 0) add("${days}d")
        if (hours > 0) add("${hours}h")
        if (months == 0L && days == 0L && mins > 0) add("${mins}m")
    }.joinToString(" ")
}


fun formatEpochDate(millis: Long): String =
    java.time.Instant.ofEpochMilli(millis).atZone(java.time.ZoneId.systemDefault()).toLocalDate().format(prettyDate)
