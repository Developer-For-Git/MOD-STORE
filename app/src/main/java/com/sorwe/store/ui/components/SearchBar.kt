package com.sorwe.store.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.launch
import com.sorwe.store.R
import com.sorwe.store.ui.theme.CrimsonRed
import com.sorwe.store.ui.theme.glassPill
import com.sorwe.store.ui.theme.bounceOnClick
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.background
import androidx.compose.material3.DropdownMenuItem

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val userPreferences = androidx.compose.runtime.remember { 
        dagger.hilt.android.EntryPointAccessors.fromApplication(
            context.applicationContext, 
            com.sorwe.store.ui.screens.settings.SettingsEntryPoint::class.java
        ).userPreferences() 
    }
    val searchHistory by userPreferences.searchHistory.collectAsState(initial = emptyList<String>())
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var isFocused by androidx.compose.runtime.mutableStateOf(false)
    val focusManager = LocalFocusManager.current

    val filteredHistory = if (query.isBlank()) {
        searchHistory
    } else {
        searchHistory.filter { it.contains(query, ignoreCase = true) }
    }

    androidx.compose.material3.ExposedDropdownMenuBox(
        expanded = isFocused && filteredHistory.isNotEmpty(),
        onExpandedChange = { },
        modifier = modifier.padding(horizontal = 16.dp)
    ) {
        TextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
                .onFocusChanged { isFocused = it.isFocused }
                .glassPill(cornerRadius = 24.dp),
            placeholder = {
                Text(
                    text = stringResource(R.string.search_apps),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            },
            trailingIcon = {
                AnimatedVisibility(
                    visible = query.isNotEmpty(),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    IconButton(
                        onClick = { onQueryChange("") },
                        modifier = Modifier.bounceOnClick { onQueryChange("") }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = CrimsonRed,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            ),
            textStyle = MaterialTheme.typography.bodyMedium,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { 
                focusManager.clearFocus()
                if (query.isNotBlank()) {
                    scope.launch { userPreferences.addSearchQuery(query) }
                }
            })
        )

        ExposedDropdownMenu(
            expanded = isFocused && filteredHistory.isNotEmpty(),
            onDismissRequest = { isFocused = false },
            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 1.0f))
        ) {
            filteredHistory.forEach { historyItem ->
                DropdownMenuItem(
                    text = { Text(historyItem) },
                    onClick = {
                        onQueryChange(historyItem)
                        focusManager.clearFocus()
                        scope.launch { userPreferences.addSearchQuery(historyItem) }
                    }
                )
            }
        }
    }
}
