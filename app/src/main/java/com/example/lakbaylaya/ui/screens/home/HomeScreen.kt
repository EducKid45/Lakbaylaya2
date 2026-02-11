package com.example.lakbaylaya.ui.screens.home

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
 * Home Screen
 *
 * Main landing screen of the app.
 * Displays welcome message and primary navigation options.
 *
 * Architecture:
 * - Stateless composable
 * - Receives all data as parameters
 * - Business logic handled by ViewModel (to be implemented)
 *
 * Accessibility:
 * - Large, readable text
 * - High contrast colors
 * - Semantic content descriptions
 */
@Composable
fun HomeScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.screen_title_home),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp
        )
    }
}
