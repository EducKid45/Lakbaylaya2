package com.example.lakbaylaya.ui.screens.help

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.SpeakerPhone
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─────────────────────────────────────────────────────────────────────────────
// FullGuideScreen — detailed navigation reference
//
// Opened from HelpScreen → "View Full Guide" button.
// Provides a comprehensive, accessible guide with icons, clear headings,
// simple language, and large touch targets.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun FullGuideScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
            .semantics { contentDescription = "Full Navigation Guide screen" }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(vertical = 24.dp)
        ) {
            item { FullGuideHeader() }
            item { VoiceCommandsGuideSection() }
            item { NavigationGuideSection() }
            item { DeviceStatusGuideSection() }
            item { TroubleshootingGuideSection() }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Header
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun FullGuideHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { heading() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(72.dp),
            shape = CircleShape,
            color = Color(0xFF1976D2).copy(alpha = 0.12f)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = Icons.Default.Route,
                    contentDescription = null,
                    tint = Color(0xFF1976D2),
                    modifier = Modifier.size(40.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Full Navigation Guide",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 26.sp
            ),
            color = Color(0xFF1A1A1A)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "A complete reference for using LakbayLaya",
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFF666666)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Section: Voice Commands
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun VoiceCommandsGuideSection() {
    GuideSection(
        icon = Icons.Default.Mic,
        iconTint = Color(0xFF1976D2),
        title = "All Voice Commands",
        description = "Full list of voice commands you can use in LakbayLaya"
    ) {
        val items = listOf(
            // Wake word
            GuideItem("\"listen\"", "Wake word. Say this at any time to activate voice input."),
            GuideItem("\"stop listening\"", "Deactivates voice input mode. Also: \"stop voice\", \"quiet\"."),
            // Navigation
            GuideItem("\"navigate to [place]\"", "Opens the navigation dialog to search for a destination. Also: \"start navigation\", \"get directions\", \"find me a place\"."),
            GuideItem("\"show routes\"", "Opens your Saved Locations screen. Also: \"my routes\", \"saved routes\", \"familiar routes\"."),
            GuideItem("\"open map\"", "Goes to the Map screen. Also: \"go to map\", \"show map\"."),
            GuideItem("\"go home\"", "Returns to the Home screen. Also: \"home\", \"main screen\"."),
            GuideItem("\"open settings\"", "Opens the Settings screen. Also: \"settings\", \"preferences\"."),
            // In-navigation
            GuideItem("\"repeat\"", "Repeats the last navigation instruction aloud. Also: \"say again\", \"again\"."),
            GuideItem("\"stop navigation\"", "Ends the current navigation session. Also: \"cancel navigation\", \"cancel route\"."),
            GuideItem("\"pause navigation\"", "Temporarily pauses voice guidance."),
            GuideItem("\"resume navigation\"", "Resumes paused voice guidance."),
            // Device & status
            GuideItem("\"check device status\"", "Reports internet, GPS and Bluetooth status. Also: \"device status\", \"status\"."),
            GuideItem("\"bluetooth\"", "Opens the Bluetooth device connection panel. Also: \"check bluetooth\", \"connect device\".")
        )
        items.forEach { item ->
            GuideItemRow(item = item)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Section: Navigation
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun NavigationGuideSection() {
    GuideSection(
        icon = Icons.Default.Navigation,
        iconTint = Color(0xFF006C4C),
        title = "Starting Navigation",
        description = "How to start a navigation session"
    ) {
        val steps = listOf(
            GuideItem("Step 1 - Open the Map", "Tap the Map tab in the bottom navigation bar, or say \"open map\" to go directly to the map."),
            GuideItem("Step 2 - Search for destination", "Type in the search box at the top of the map, or use voice by saying \"navigate to\" followed by your destination name."),
            GuideItem("Step 3 - Confirm destination", "Tap the result from the search list or the pin on the map. A preview panel will appear at the bottom."),
            GuideItem("Step 4 - Start navigation", "Tap \"Start Navigation\" in the preview panel. Voice guidance will begin immediately.")
        )
        steps.forEach { item ->
            GuideItemRow(item = item)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Section: Device Status
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DeviceStatusGuideSection() {
    GuideSection(
        icon = Icons.Default.GpsFixed,
        iconTint = Color(0xFFF57C00),
        title = "Device Status",
        description = "Understanding the status indicators on the Home screen"
    ) {
        val items = listOf(
            GuideItem("Internet — Online / Offline", "Shows whether your phone has an active internet connection. Navigation requires internet for map loading."),
            GuideItem("GPS — Ready / Searching / Unavailable", "GPS Ready means your location is known. Searching means wait a moment. Unavailable means GPS is off — enable it in phone Settings."),
            GuideItem("Wearable — Connected / Not Connected", "Shows whether an ESP32 vibration device is connected via Bluetooth. Tap the Bluetooth icon in the top bar to connect.")
        )
        items.forEach { item -> GuideItemRow(item = item) }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Section: Troubleshooting
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TroubleshootingGuideSection() {
    GuideSection(
        icon = Icons.Default.SpeakerPhone,
        iconTint = Color(0xFFE53935),
        title = "Troubleshooting",
        description = "Common issues and how to fix them"
    ) {
        val items = listOf(
            GuideItem("Voice not responding", "Make sure the microphone permission is granted. Go to Settings → App → Permissions → Microphone."),
            GuideItem("No voice feedback (TTS silent)", "Check that your phone volume is turned up. TTS uses the Media volume channel."),
            GuideItem("GPS not finding location", "Move to an open outdoor area away from buildings. GPS needs a clear sky view to get a fix."),
            GuideItem("Map not loading", "Check your internet connection. A data connection is required for map tiles."),
            GuideItem("Bluetooth device not connecting", "Tap the Bluetooth icon in the top bar and tap your device name. Make sure the ESP32 device is powered on and nearby.")
        )
        items.forEach { item -> GuideItemRow(item = item) }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared components
// ─────────────────────────────────────────────────────────────────────────────

private data class GuideItem(val title: String, val body: String)

@Composable
private fun GuideItemRow(item: GuideItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "${item.title}. ${item.body}"
            },
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            shape = CircleShape,
            color = Color(0xFF006C4C).copy(alpha = 0.12f),
            modifier = Modifier.size(8.dp).padding(top = 8.dp)
        ) {}

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                ),
                color = Color(0xFF1A1A1A)
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = item.body,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                color = Color(0xFF424242)
            )
            HorizontalDivider(
                color = Color(0xFFEEEEEE),
                thickness = 1.dp,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}

@Composable
private fun GuideSection(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    description: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "$title section: $description" },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 4.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = iconTint.copy(alpha = 0.12f),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = Color(0xFF1A1A1A),
                        modifier = Modifier.semantics { heading() }
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = Color(0xFF666666)
                    )
                }
            }

            HorizontalDivider(
                color = Color(0xFFEEEEEE),
                thickness = 1.dp,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            content()
        }
    }
}







