package com.swiftlydeveloped.feedbackkit.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.getfeedbackkit.feedbackkit.R
import com.swiftlydeveloped.feedbackkit.state.FeedbackListState

internal const val SEARCH_FIELD_TAG = "fbk-list-search"

/**
 * Search field for [state], styled as a Material 3 search bar. Matches title and description
 * as the reader types, ignoring case and accents; nothing is fetched. [FeedbackListScreen]
 * pins it under the top app bar.
 */
@Composable
fun FeedbackListSearchField(
    state: FeedbackListState,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val colors = MaterialTheme.colorScheme
    TextField(
        value = state.searchQuery,
        onValueChange = state::updateSearchQuery,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .testTag(SEARCH_FIELD_TAG),
        singleLine = true,
        shape = CircleShape,
        placeholder = { Text(stringResource(R.string.feedbackkit_search_placeholder)) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = if (state.searchQuery.isNotEmpty()) {
            {
                IconButton(onClick = { state.updateSearchQuery("") }) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.feedbackkit_search_clear))
                }
            }
        } else {
            null
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = {
            keyboard?.hide()
            focusManager.clearFocus()
        }),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = colors.surfaceContainerHigh,
            unfocusedContainerColor = colors.surfaceContainerHigh,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent
        )
    )
}
