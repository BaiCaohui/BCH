package com.baicaohui.lightweb.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.baicaohui.lightweb.browser.UrlSecurity
import java.io.File

@Composable
fun Favicon(
    url: String,
    title: String,
    size: Dp = 40.dp,
    color: Long? = null,
    iconUrl: String? = null,
    modifier: Modifier = Modifier,
) {
    val host = remember(url) { UrlSecurity.extractHost(url) }
    val bgColor = color?.let { Color(it) } ?: MaterialTheme.colorScheme.primaryContainer
    val model: Any? = iconUrl?.let { File(it) }
        ?: host?.let { "https://$it/favicon.ico" }
    Box(
        modifier = modifier.size(size).clip(CircleShape).background(bgColor),
        contentAlignment = Alignment.Center,
    ) {
        if (model != null) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(model)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = { LetterAvatar(title, bgColor) },
                error = { LetterAvatar(title, bgColor) },
            )
        } else {
            LetterAvatar(title, bgColor)
        }
    }
}

@Composable
private fun LetterAvatar(title: String, background: Color) {
    Box(
        modifier = Modifier.fillMaxSize().background(background),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = title.firstOrNull()?.uppercase() ?: "?",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}
