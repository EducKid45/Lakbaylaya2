package com.example.lakbaylaya.ui.screens.help

import android.app.Application
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
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lakbaylaya.ui.screens.map.navigation.voice.NavigationVoiceManager

// ─────────────────────────────────────────────────────────────────────────────
// HelpScreen
//
// Purpose : Provides clear voice-command instructions for visually impaired
//           users. Opened via the Help icon (?) in the TopBar.
//
// Sections:
//   1. Quick Voice Commands  – icon + command examples
//   2. How Voice Control Works – step-by-step explanation
//   3. Full Text Guide          – expandable detailed guide button
//
// Accessibility:
//   - All sections have semantics / contentDescription
//   - TTS reads the intro on first open
//   - Large touch targets (≥ 48 dp)
//   - High-contrast light palette (no dark theme)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun HelpScreen(
    onViewFullGuide: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val application = context.applicationContext as Application

    // TTS manager — reuse the same voice engine used throughout the app
    val ttsManager = remember {
        NavigationVoiceManager(application, object :
            com.example.lakbaylaya.ui.screens.map.navigation.voice.VoiceCommandHandler {
            override fun onRepeatInstruction() {}
            override fun onPauseNavigation() {}
            override fun onResumeNavigation() {}
            override fun onCheckCurrentPosition(latitude: Double, longitude: Double) {}
            override fun onDistanceToDestination(distanceMeters: Double) {}
            override fun onSwitchRoute() {}
            override fun onCancelNavigation() {}
            override fun onActivateEmergencyMode() {}
        })
    }

    // Announce screen open via TTS so visually impaired users know where they are
    DisposableEffect(Unit) {
        ttsManager.speak(
            "Help screen opened. This screen explains how to use voice commands. " +
            "Swipe up to read the sections."
        )
        onDispose { /* TTS continues naturally; no need to stop */ }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
            .semantics { contentDescription = "Help and Voice Guide screen" }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(vertical = 24.dp)
        ) {
            // ── Page header ───────────────────────────────────────────────────
            item { HelpPageHeader() }

            // ── Section 1: Quick Voice Commands ──────────────────────────────
            item { QuickVoiceCommandsSection() }

            // ── Section 2: How Voice Control Works ───────────────────────────
            item { HowVoiceControlWorksSection() }

            // ── Section 3: Full Text Guide button ────────────────────────────
            item { FullGuideSection(onViewFullGuide = onViewFullGuide) }

            // Bottom spacing
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Page header
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HelpPageHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { heading() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(72.dp),
            shape = CircleShape,
            color = Color(0xFF006C4C).copy(alpha = 0.12f)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                    contentDescription = null,
                    tint = Color(0xFF006C4C),
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Voice Guide",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp
            ),
            color = Color(0xFF1A1A1A),
            modifier = Modifier.semantics { contentDescription = "Voice Guide heading" }
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Everything you need to navigate with your voice",
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFF666666),
            modifier = Modifier.semantics {
                contentDescription = "Everything you need to navigate with your voice"
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Section 1 — Quick Voice Commands
// ─────────────────────────────────────────────────────────────────────────────

private data class QuickCommand(val command: String, val hint: String)

@Composable
private fun QuickVoiceCommandsSection() {
    val commands = listOf(
        QuickCommand("check device status", "Reports your internet, GPS and Bluetooth status aloud via voice."),
        QuickCommand("start navigation", "Activates the listen mode — say \"listen\" to start, then speak your destination.")
    )

    HelpSectionCard(
        sectionTitle = "Quick Voice Commands",
        sectionIcon = Icons.Default.Mic,
        iconTint = Color(0xFF1976D2),
        sectionDescription = "Quick Voice Commands section. Tap any command to hear it read aloud."
    ) {
        commands.forEachIndexed { index, item ->
            QuickCommandRow(command = item)
            if (index < commands.size - 1) {
                HorizontalDivider(
                    color = Color(0xFFEEEEEE),
                    thickness = 1.dp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun QuickCommandRow(command: QuickCommand) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "Command: ${command.command}. ${command.hint}"
            },
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF1976D2).copy(alpha = 0.10f),
            modifier = Modifier.size(36.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = null,
                    tint = Color(0xFF1976D2),
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "\"${command.command}\"",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                ),
                color = Color(0xFF1A1A1A)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = command.hint,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                color = Color(0xFF666666)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Section 2 — How Voice Control Works
// ─────────────────────────────────────────────────────────────────────────────

private data class VoiceStep(val stepNumber: Int, val title: String, val description: String)

@Composable
private fun HowVoiceControlWorksSection() {
    val steps = listOf(
        VoiceStep(1, "Say the wake word", "Say \"listen\" — the app will activate and wait for your command. You can also tap the microphone button on the Home screen."),
        VoiceStep(2, "Speak your command", "Clearly say your command such as \"navigate to market\" or \"show routes\". Speak naturally — no special phrasing needed."),
        VoiceStep(3, "System responds", "The app will speak a voice response confirming your command and take the appropriate action immediately.")
    )

    HelpSectionCard(
        sectionTitle = "How Voice Control Works",
        sectionIcon = Icons.Default.RecordVoiceOver,
        iconTint = Color(0xFF006C4C),
        sectionDescription = "How Voice Control Works section. Three steps explained."
    ) {
        steps.forEach { step ->
            VoiceStepRow(step = step)
            if (step.stepNumber < steps.size) {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun VoiceStepRow(step: VoiceStep) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = "Step ${step.stepNumber}: ${step.title}. ${step.description}"
            },
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            modifier = Modifier.size(36.dp),
            shape = CircleShape,
            color = Color(0xFF006C4C)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Text(
                    text = step.stepNumber.toString(),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    ),
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = step.title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                ),
                color = Color(0xFF1A1A1A)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = step.description,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                color = Color(0xFF424242)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Section 3 — Full Text Guide Button
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun FullGuideSection(onViewFullGuide: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Full guide section" },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Route,
                contentDescription = null,
                tint = Color(0xFF006C4C),
                modifier = Modifier.size(40.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Full Navigation Guide",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                ),
                color = Color(0xFF1A1A1A)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "A complete step-by-step guide covering all navigation features, accessibility tips, and troubleshooting.",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                color = Color(0xFF424242)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onViewFullGuide,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .semantics { contentDescription = "View Full Guide button. Opens the detailed navigation guide." },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006C4C))
            ) {
                Icon(
                    imageVector = Icons.Default.TouchApp,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "View Full Guide",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    ),
                    color = Color.White
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared section card wrapper
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HelpSectionCard(
    sectionTitle: String,
    sectionIcon: ImageVector,
    iconTint: Color,
    sectionDescription: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = sectionDescription },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Section header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = iconTint.copy(alpha = 0.12f),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = sectionIcon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = sectionTitle,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ),
                    color = Color(0xFF1A1A1A),
                    modifier = Modifier.semantics { heading() }
                )
            }

            HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp)
            Spacer(modifier = Modifier.height(16.dp))

            content()
        }
    }
}







