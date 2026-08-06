package com.baicaohui.lightweb.ui.console

import android.widget.Toast
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.baicaohui.lightweb.BchApp
import com.baicaohui.lightweb.R
import com.baicaohui.lightweb.browser.ConsoleCommands
import com.baicaohui.lightweb.ui.components.PlaceholderScreen

private data class ConsoleLog(
    val command: String?,
    val output: String?,
)

private val QUICK_COMMANDS = listOf(
    "document.title",
    "location.href",
    "navigator.userAgent",
    "document.cookie",
    "document.body.innerText",
)

@Composable
fun ConsoleScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as BchApp
    val currentId by app.tabManager.currentId.collectAsStateWithLifecycle()
    val clipboard = LocalClipboardManager.current
    var tab by remember { mutableStateOf(0) }
    var source by remember { mutableStateOf("") }
    var loadingSource by remember { mutableStateOf(false) }
    val logs = remember { mutableStateListOf<ConsoleLog>() }
    var command by remember { mutableStateOf("") }

    fun currentWebView() = currentId?.let { app.webViewStore.get(it) }

    fun loadSource() {
        val wv = currentWebView()
        if (wv == null) {
            source = ""
            return
        }
        loadingSource = true
        wv.evaluateJavascript(ConsoleCommands.sourceExpression()) { raw ->
            source = ConsoleCommands.unescapeJsResult(raw ?: "")
            loadingSource = false
        }
    }

    LaunchedEffect(currentId) { loadSource() }

    fun runCommand() {
        val cmd = command.trim()
        if (cmd.isEmpty()) return
        val wv = currentWebView()
        command = ""
        if (wv == null) {
            logs.add(ConsoleLog(command = cmd, output = null))
            logs.add(ConsoleLog(command = null, output = context.getString(R.string.console_no_page)))
            return
        }
        logs.add(ConsoleLog(command = cmd, output = null))
        wv.evaluateJavascript(ConsoleCommands.evaluateExpression(cmd)) { raw ->
            logs.add(ConsoleLog(command = null, output = ConsoleCommands.unescapeJsResult(raw ?: "")))
        }
    }

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.nav_console),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
            FilterChip(
                selected = tab == 0,
                onClick = { tab = 0 },
                label = { Text(stringResource(R.string.console_source)) },
            )
            Spacer(Modifier.width(8.dp))
            FilterChip(
                selected = tab == 1,
                onClick = { tab = 1 },
                label = { Text(stringResource(R.string.console_console)) },
            )
        }

        if (currentWebView() == null) {
            PlaceholderScreen(text = stringResource(R.string.console_no_page))
            return@Column
        }

        when (tab) {
            0 -> SourcePanel(
                source = source,
                loading = loadingSource,
                onRefresh = { loadSource() },
                onCopy = {
                    if (source.isNotBlank()) {
                        clipboard.setText(AnnotatedString(source))
                        Toast.makeText(context, R.string.console_copied, Toast.LENGTH_SHORT).show()
                    }
                },
            )
            1 -> ConsolePanel(
                logs = logs,
                command = command,
                onCommandChange = { command = it },
                onRun = { runCommand() },
                onClear = { logs.clear() },
                onQuick = { command = it },
            )
        }
    }
}

@Composable
private fun SourcePanel(
    source: String,
    loading: Boolean,
    onRefresh: () -> Unit,
    onCopy: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onRefresh, enabled = !loading) {
                Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.console_refresh))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.console_refresh))
            }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onCopy, enabled = source.isNotBlank()) {
                Text(stringResource(R.string.console_copy))
            }
        }
        HorizontalDivider()
        Box(modifier = Modifier.fillMaxSize()) {
            if (source.isBlank() && !loading) {
                PlaceholderScreen(text = stringResource(R.string.console_empty))
            } else {
                Text(
                    text = source,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                )
            }
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        }
    }
}

@Composable
private fun ConsolePanel(
    logs: List<ConsoleLog>,
    command: String,
    onCommandChange: (String) -> Unit,
    onRun: () -> Unit,
    onClear: () -> Unit,
    onQuick: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = command,
                onValueChange = onCommandChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                placeholder = { Text(stringResource(R.string.console_input_hint)) },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    imeAction = androidx.compose.ui.text.input.ImeAction.Go,
                ),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                    onGo = { onRun() },
                ),
            )
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = onRun, enabled = command.isNotBlank()) {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = stringResource(R.string.console_run),
                )
            }
            TextButton(onClick = onClear, enabled = logs.isNotEmpty()) {
                Text(stringResource(R.string.console_clear))
            }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
            items(QUICK_COMMANDS) { quick ->
                AssistChip(
                    onClick = { onQuick(quick) },
                    label = { Text(quick, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                )
            }
        }
        HorizontalDivider()
        if (logs.isEmpty()) {
            PlaceholderScreen(text = stringResource(R.string.console_empty))
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(top = 8.dp)) {
                items(logs.size) { index ->
                    val log = logs[index]
                    if (log.command != null) {
                        Text(
                            text = "> ${log.command}",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 2.dp),
                        )
                    } else {
                        Text(
                            text = log.output.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(bottom = 6.dp, start = 8.dp),
                        )
                    }
                }
            }
        }
    }
}
