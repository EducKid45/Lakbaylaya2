package com.example.lakbaylaya.ui.screens.route

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lakbaylaya.R

/**
 * Route Screen
 *
 * Displays route planning and navigation instructions.
 * Shows turn-by-turn directions with accessibility features.
 *
 * Architecture:
 * - Stateless composable
 * - Route data from ViewModel
 * - Navigation logic in domain layer
 *
 * Accessibility:
 * - Voice guidance for directions
 * - Haptic feedback for turns
 * - Large, readable instructions
 */
@Composable
fun RouteScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.screen_title_route),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp
        )
    }
}
