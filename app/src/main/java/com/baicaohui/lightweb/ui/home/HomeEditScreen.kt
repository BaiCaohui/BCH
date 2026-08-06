package com.baicaohui.lightweb.ui.home

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.baicaohui.lightweb.BchApp
import com.baicaohui.lightweb.R
import com.baicaohui.lightweb.ui.theme.PRESET_SEEDS
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun HomeEditScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as BchApp
    val prefs = app.homePrefs
    val config by prefs.config.collectAsStateWithLifecycle(initialValue = HomeConfig.DEFAULT)
    val scope = rememberCoroutineScope()
    fun update(transform: (HomeConfig) -> HomeConfig) {
        scope.launch { prefs.update(transform) }
    }

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val saved = copyImageToPrivateStorage(context, uri)
                if (saved != null) {
                    update {
                        it.copy(
                            background = it.background.copy(
                                type = BackgroundType.IMAGE,
                                imageUri = saved.toString(),
                            ),
                        )
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(16.dp),
    ) {
        Text(
            text = stringResource(R.string.home_edit_title),
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.home_edit_widgets), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        config.widgets.forEachIndexed { index, widget ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = widgetLabel(widget.type),
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = { moveWidget(index, index - 1, config, ::update) },
                    enabled = index > 0,
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.home_edit_move_up),
                    )
                }
                IconButton(
                    onClick = { moveWidget(index, index + 1, config, ::update) },
                    enabled = index < config.widgets.lastIndex,
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = stringResource(R.string.home_edit_move_down),
                    )
                }
                Switch(
                    checked = widget.enabled,
                    onCheckedChange = { enabled ->
                        update {
                            it.copy(
                                widgets = it.widgets.mapIndexed { i, w ->
                                    if (i == index) w.copy(enabled = enabled) else w
                                },
                            )
                        }
                    },
                )
            }
            if (widget.type == HomeWidgetType.SPEED_DIAL) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.home_edit_columns),
                        modifier = Modifier.weight(1f),
                    )
                    listOf(3, 4, 5).forEach { columns ->
                        FilterChip(
                            selected = widget.columns == columns,
                            onClick = {
                                update {
                                    it.copy(
                                        widgets = it.widgets.mapIndexed { i, w ->
                                            if (i == index) w.copy(columns = columns) else w
                                        },
                                    )
                                }
                            },
                            label = { Text("$columns") },
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Text(stringResource(R.string.home_edit_background), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BackgroundType.entries.forEach { type ->
                FilterChip(
                    selected = config.background.type == type,
                    onClick = {
                        update { it.copy(background = it.background.copy(type = type)) }
                    },
                    label = { Text(backgroundLabel(type)) },
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        when (config.background.type) {
            BackgroundType.COLOR -> ColorRow(
                selected = config.background.color,
                onSelect = { color ->
                    update { it.copy(background = it.background.copy(color = color)) }
                },
            )
            BackgroundType.GRADIENT -> {
                Text(stringResource(R.string.home_edit_bg_gradient))
                ColorRow(
                    selected = config.background.gradientStart,
                    onSelect = { color ->
                        update { it.copy(background = it.background.copy(gradientStart = color)) }
                    },
                )
                ColorRow(
                    selected = config.background.gradientEnd,
                    onSelect = { color ->
                        update { it.copy(background = it.background.copy(gradientEnd = color)) }
                    },
                )
            }
            BackgroundType.IMAGE -> {
                OutlinedButton(
                    onClick = {
                        imagePicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    },
                ) {
                    Text(stringResource(R.string.home_edit_pick_image))
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.home_edit_overlay))
        Slider(
            value = config.overlayAlpha,
            onValueChange = { alpha ->
                update { it.copy(overlayAlpha = alpha) }
            },
            valueRange = 0f..0.6f,
        )
    }
}

/** 把相册选中的图片复制到应用私有目录，避免重启后相册 URI 权限失效导致背景丢失。 */
private suspend fun copyImageToPrivateStorage(context: Context, uri: Uri): Uri? =
    withContext(Dispatchers.IO) {
        runCatching {
            val input = context.contentResolver.openInputStream(uri) ?: return@runCatching null
            val file = File(context.filesDir, "home_background.jpg")
            input.use { source ->
                file.outputStream().use { target -> source.copyTo(target) }
            }
            Uri.fromFile(file)
        }.getOrNull()
    }

private fun moveWidget(
    from: Int,
    to: Int,
    config: HomeConfig,
    update: ((HomeConfig) -> HomeConfig) -> Unit,
) {
    if (to !in config.widgets.indices) return
    val list = config.widgets.toMutableList()
    val item = list.removeAt(from)
    list.add(to, item)
    update { config.copy(widgets = list) }
}

@Composable
private fun ColorRow(selected: Long, onSelect: (Long) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PRESET_SEEDS.forEach { (_, value) ->
            val isSelected = selected == value
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(value))
                    .then(
                        if (isSelected) {
                            Modifier.border(3.dp, Color.Black.copy(alpha = 0.6f), CircleShape)
                        } else {
                            Modifier
                        },
                    )
                    .clickable { onSelect(value) },
            )
        }
    }
}

@Composable
private fun widgetLabel(type: HomeWidgetType): String = when (type) {
    HomeWidgetType.SEARCH -> stringResource(R.string.home_widget_search)
    HomeWidgetType.SPEED_DIAL -> stringResource(R.string.widget_speed_dial)
    HomeWidgetType.RECENT -> stringResource(R.string.widget_recent)
    HomeWidgetType.BOOKMARKS -> stringResource(R.string.widget_bookmarks)
    HomeWidgetType.CLOCK -> stringResource(R.string.home_widget_clock)
}

@Composable
private fun backgroundLabel(type: BackgroundType): String = when (type) {
    BackgroundType.COLOR -> stringResource(R.string.home_edit_bg_color)
    BackgroundType.GRADIENT -> stringResource(R.string.home_edit_bg_gradient)
    BackgroundType.IMAGE -> stringResource(R.string.home_edit_bg_image)
}
