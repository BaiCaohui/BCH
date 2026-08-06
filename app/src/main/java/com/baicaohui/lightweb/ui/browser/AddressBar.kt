package com.baicaohui.lightweb.ui.browser

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.baicaohui.lightweb.R
import com.baicaohui.lightweb.ui.navigation.BchIcons

@Composable
fun AddressBar(
    value: String,
    editing: Boolean,
    canReload: Boolean,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onReload: () -> Unit,
    onClear: () -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    progress: Int,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .onFocusChanged { onFocusChanged(it.isFocused) },
            shape = RoundedCornerShape(26.dp),
            singleLine = true,
            readOnly = !editing,
            placeholder = { Text(stringResource(R.string.search_hint)) },
            leadingIcon = { Icon(BchIcons.Search, contentDescription = null) },
            trailingIcon = {
                if (editing) {
                    IconButton(onClick = onClear, enabled = value.isNotEmpty()) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = stringResource(R.string.action_clear),
                        )
                    }
                } else if (canReload) {
                    IconButton(onClick = onReload) {
                        Icon(
                            BchIcons.Refresh,
                            contentDescription = stringResource(R.string.action_reload),
                        )
                    }
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { onSubmit() }),
        )
        if (progress in 1..99) {
            LinearProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier.fillMaxWidth().height(2.dp),
            )
        }
    }
}
