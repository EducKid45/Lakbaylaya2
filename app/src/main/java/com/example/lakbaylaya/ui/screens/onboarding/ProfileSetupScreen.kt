package com.example.lakbaylaya.ui.screens.onboarding

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

/*
 * ProfileSetupOnboardingScreen
 *
 * Rules enforced:
 *  - Name is REQUIRED; Finish disabled until non-blank
 *  - Emergency contact name + number are optional but shown here for convenience
 *  - Home / Work location NOT settable here — user sets those later via Profile screen map pins
 *  - NO "Set Home Location" button
 */

private val PrimaryBlue   = Color(0xFF1565C0)
private val AccentGreen   = Color(0xFF2E7D32)
private val EmergencyRed  = Color(0xFFD32F2F)
private val TextDark      = Color(0xFF212121)

@Composable
fun ProfileSetupOnboardingScreen(
    viewModel: ProfileSetupViewModel = viewModel(),
    onFinish: () -> Unit = {},
    // onSetLocation no longer used — kept for call-site compatibility but ignored
    @Suppress("UNUSED_PARAMETER") onSetLocation: () -> Unit = {}
) {
    val TAG = "ProfileSetupScreen"
    val name            by viewModel.name.collectAsState()
    val emergencyName   by viewModel.emergencyName.collectAsState()
    val emergencyNumber by viewModel.emergencyNumber.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope    = rememberCoroutineScope()

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(30.dp)
        ) {
            Text("Profile Setup", color = TextDark, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("Enter your name and emergency contact. Home and Work locations can be set later from your Profile.",
                color = TextDark, fontSize = 15.sp)
            Spacer(Modifier.height(20.dp))

            // ── Name (required) ───────────────────────────────────────────────
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryBlue)
                        Spacer(Modifier.width(12.dp))
                        Text("Name (required)", fontSize = 18.sp, color = TextDark)
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = { viewModel.onNameChange(it) },
                        placeholder = { Text("Enter your name") },
                        singleLine = true,
                        isError = name.isBlank(),
                        supportingText = { if (name.isBlank()) Text("Name is required", color = MaterialTheme.colorScheme.error) },
                        modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Name input" },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Emergency contact (optional here, required later in Settings) ─
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Phone, contentDescription = null, tint = EmergencyRed)
                        Spacer(Modifier.width(12.dp))
                        Text("Emergency Contact (optional)", fontSize = 18.sp, color = TextDark)
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = emergencyName,
                        onValueChange = { viewModel.onEmergencyNameChange(it) },
                        placeholder = { Text("Contact name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Emergency contact name input" },
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = emergencyNumber,
                        onValueChange = { viewModel.onEmergencyNumberChange(it) },
                        placeholder = { Text("Phone number") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Emergency contact number input" },
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text("You can also update this anytime in Settings → Safety & Emergency.",
                        fontSize = 13.sp, color = Color(0xFF666666))
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Finish button ─────────────────────────────────────────────────
            Button(
                onClick = {
                    coroutineScope.launch {
                        Log.d(TAG, "Saving profile from onboarding")
                        viewModel.saveProfile {
                            coroutineScope.launch {
                                val contactInfo = if (emergencyName.isNotBlank() || emergencyNumber.isNotBlank())
                                    "Emergency: ${emergencyName.ifBlank { "(no name)" }} ${emergencyNumber.ifBlank { "(no number)" }}"
                                else "No emergency contact set"
                                snackbarHostState.showSnackbar("Profile saved — $contactInfo")
                                Log.d(TAG, "Calling onFinish")
                                onFinish()
                            }
                        }
                    }
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(56.dp)
                    .semantics { contentDescription = "Finish setup" },
                colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null)
                Spacer(Modifier.width(12.dp))
                Text("Finish", fontSize = 18.sp, color = Color.White)
            }

            Spacer(Modifier.weight(1f))
            SnackbarHost(hostState = snackbarHostState)
        }
    }
}
