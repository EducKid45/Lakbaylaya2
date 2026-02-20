package com.example.lakbaylaya.ui.screens.map.components

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp

/**
 * Floating search bar component with enhanced UX
 *
 * Features:
 * - Back button when overlay is active
 * - Search icon when inactive
 * - Auto-focus on activation
 * - Cursor positioned at end when clicking with text
 * - Mic icon when empty, Clear icon when typing
 * - Minimal padding for close positioning to top
 * - Proper window insets handling
 * - Keyboard dismissal on search action
 * - Accessibility support
 * - Minimum 48dp touch targets
 *
 * @param query Current search query
 * @param isOverlayActive Whether the search overlay is active
 * @param onQueryChange Callback when query changes
 * @param onSearchBarClick Callback when search bar is clicked (activates overlay)
 * @param onBackClick Callback when back button is clicked (deactivates overlay)
 * @param onClearClick Callback when clear button is clicked
 * @param onMicClick Callback when microphone button is clicked
 * @param modifier Modifier for customization
 */
@Composable
fun FloatingSearchBar(
    query: String,
    isOverlayActive: Boolean,
    isMicActive: Boolean = false,
    onQueryChange: (String) -> Unit,
    onSearchBarClick: () -> Unit,
    onBackClick: () -> Unit,
    onClearClick: () -> Unit,
    onMicClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    // Use TextFieldValue to control cursor position
    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(text = query, selection = TextRange(query.length)))
    }

    // Update TextFieldValue when query changes externally
    LaunchedEffect(query) {
        if (textFieldValue.text != query) {
            // Position cursor at end when text is updated externally
            textFieldValue = TextFieldValue(
                text = query,
                selection = TextRange(query.length)
            )
        }
    }

    // Auto-focus when overlay becomes active and position cursor at end
    LaunchedEffect(isOverlayActive) {
        if (isOverlayActive) {
            // Position cursor at end when activating search
            textFieldValue = TextFieldValue(
                text = query,
                selection = TextRange(query.length)
            )
            focusRequester.requestFocus()
        } else {
            focusManager.clearFocus()
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 0.dp)
            .semantics {
                contentDescription = if (isOverlayActive) {
                    "Search bar active, type to search"
                } else {
                    "Search bar, tap to search places"
                }
            },
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Left icon: Back button when active, Search icon when inactive
            Crossfade(
                targetState = isOverlayActive,
                label = "left_icon_transition"
            ) { active ->
                if (active) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .size(48.dp)
                            .semantics {
                                contentDescription = "Close search"
                            }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Text input field
            Box(
                modifier = Modifier
                    .weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                BasicTextField(
                    value = textFieldValue,
                    onValueChange = { newValue ->
                        textFieldValue = newValue
                        onQueryChange(newValue.text)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .onFocusChanged { focusState ->
                            if (focusState.isFocused && !isOverlayActive) {
                                onSearchBarClick()
                            }
                        },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Search
                    ),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            focusManager.clearFocus()
                        }
                    ),
                    decorationBox = { innerTextField ->
                        Box(
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (textFieldValue.text.isEmpty()) {
                                Text(
                                    text = "Search places...",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            innerTextField()
                        }
                    }
                )
            }

            // Right icon: Mic when empty, Clear when typing
            Crossfade(
                targetState = textFieldValue.text.isNotEmpty(),
                label = "right_icon_transition"
            ) { hasText ->
                if (hasText) {
                    IconButton(
                        onClick = onClearClick,
                        modifier = Modifier
                            .size(48.dp)
                            .semantics {
                                contentDescription = "Clear search"
                            }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    // Show mic as active/inactive by tint and content description
                    IconButton(
                        onClick = onMicClick,
                        modifier = Modifier
                            .size(48.dp)
                            .semantics {
                                contentDescription =
                                    if (isMicActive) "Stop voice search" else "Start voice search"
                            }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Microphone",
                            tint = if (isMicActive)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
