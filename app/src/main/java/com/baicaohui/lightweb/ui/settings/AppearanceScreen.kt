package com.baicaohui.lightweb.ui.settings

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import com.baicaohui.lightweb.ui.theme.DarkMode
import com.baicaohui.lightweb.ui.theme.PRESET_SEEDS
import com.baicaohui.lightweb.ui.theme.ShapeStyle
import com.baicaohui.lightweb.ui.theme.ThemeConfig
import com.baicaohui.lightweb.ui.theme.TonalPaletteGenerator
import com.baicaohui.lightweb.ui.theme.toSeedLong
import kotlinx.coroutines.launch

@Composable
fun AppearanceScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as BchApp
    val prefs = app.themePrefs
    val config by prefs.config.collectAsStateWithLifecycle(initialValue = ThemeConfig.DEFAULT)
    val scope = rememberCoroutineScope()
    fun update(transform: (ThemeConfig) -> ThemeConfig) {
        scope.launch { prefs.update(transform) }
    }

    var hue by remember { mutableFloatStateOf(220f) }
    var sat by remember { mutableFloatStateOf(0.8f) }
    var value by remember { mutableFloatStateOf(1f) }
    val customColor = remember(hue, sat, value) {
        TonalPaletteGenerator.fromHsvColor(hue, sat, value)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(16.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_appearance),
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(Modifier.height(16.dp))

        SectionTitle(stringResource(R.string.appearance_presets))
        ColorRow(
            selected = config.seedColor,
            onSelect = { color ->
                update { it.copy(seedColor = color, useDynamicColor = false) }
            },
        )

        Spacer(Modifier.height(16.dp))
        SectionTitle(stringResource(R.string.appearance_custom))
        Text(stringResource(R.string.appearance_hue))
        Slider(value = hue, onValueChange = { hue = it }, valueRange = 0f..360f)
        Text(stringResource(R.string.appearance_saturation))
        Slider(value = sat, onValueChange = { sat = it }, valueRange = 0f..1f)
        Text(stringResource(R.string.appearance_value))
        Slider(value = value, onValueChange = { value = it }, valueRange = 0f..1f)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(customColor),
            )
            Spacer(Modifier.size(12.dp))
            Button(
                onClick = {
                    update {
                        it.copy(seedColor = customColor.toSeedLong(), useDynamicColor = false)
                    }
                },
            ) {
                Text(stringResource(R.string.appearance_apply))
            }
        }

        Spacer(Modifier.height(16.dp))
        SettingSwitch(
            label = stringResource(R.string.appearance_dynamic),
            checked = config.useDynamicColor,
            onCheckedChange = { enabled -> update { it.copy(useDynamicColor = enabled) } },
        )

        Spacer(Modifier.height(16.dp))
        SectionTitle(stringResource(R.string.appearance_dark_mode))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(DarkMode.SYSTEM, DarkMode.LIGHT, DarkMode.DARK).forEach { mode ->
                FilterChip(
                    selected = config.darkMode == mode,
                    onClick = { update { it.copy(darkMode = mode) } },
                    label = { Text(mode.name) },
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        SectionTitle(stringResource(R.string.appearance_font_scale))
        Slider(
            value = config.fontScale,
            onValueChange = { scale -> update { it.copy(fontScale = scale) } },
            valueRange = 0.85f..1.3f,
        )

        Spacer(Modifier.height(16.dp))
        SectionTitle(stringResource(R.string.appearance_shape))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = config.shapeStyle == ShapeStyle.STANDARD,
                onClick = { update { it.copy(shapeStyle = ShapeStyle.STANDARD) } },
                label = { Text(stringResource(R.string.appearance_standard)) },
            )
            FilterChip(
                selected = config.shapeStyle == ShapeStyle.ROUNDED,
                onClick = { update { it.copy(shapeStyle = ShapeStyle.ROUNDED) } },
                label = { Text(stringResource(R.string.appearance_rounded)) },
            )
        }

        Spacer(Modifier.height(16.dp))
        SettingSwitch(
            label = stringResource(R.string.appearance_density),
            checked = config.compact,
            onCheckedChange = { enabled -> update { it.copy(compact = enabled) } },
        )
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
fun SettingSwitch(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun ColorRow(selected: Long, onSelect: (Long) -> Unit) {
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
