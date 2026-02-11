package com.example.lakbaylaya.ui.navigationbars.system

import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.Modifier

/**
 * Apply top-of-window (status bar) padding so a top app bar will not overlap the
 * system status bar. Prefer using this modifier on the root container of your
 * top app bar.
 *
 * Usage: Modifier.systemTopBarPadding()
 */
fun Modifier.systemTopBarPadding(): Modifier = this.statusBarsPadding()

/**
 * Apply bottom-of-window (navigation bar) padding so a bottom navigation will sit
 * above system navigation/gesture areas.
 *
 * Usage: Modifier.systemBottomBarPadding()
 */
fun Modifier.systemBottomBarPadding(): Modifier = this.navigationBarsPadding()
