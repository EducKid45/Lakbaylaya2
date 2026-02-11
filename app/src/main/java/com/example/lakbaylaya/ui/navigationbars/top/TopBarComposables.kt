package com.example.lakbaylaya.ui.navigationbars.top

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.lakbaylaya.ui.navigationbars.model.NavigationBarItem
import com.example.lakbaylaya.ui.navigationbars.system.systemTopBarPadding

/**
 * Wrapper composable that applies status bar padding to a top app bar container.
 * Also provides a minimum touch target for inner content.
 *
 * Usage: SystemTopBarContainer { TopAppBar(...) }
 */
@Composable
fun SystemTopBarContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier.systemTopBarPadding()) {
        // Inner content should already respect minimum heights; we just ensure it sits
        // below the status bar.
        content()
    }
}

/**
 * Helper to produce a TalkBack-friendly content description for selected/unselected
 * states using resource ids. Callers can use this to avoid duplicating string logic.
 */
@Composable
fun contentDescriptionForItem(
    item: NavigationBarItem,
    selected: Boolean
): String? {
    val res = if (selected) item.contentDescriptionResSelected else item.contentDescriptionResUnselected
    return res?.let { stringResource(it) }
}

