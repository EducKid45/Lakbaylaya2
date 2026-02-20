package com.example.lakbaylaya.ui.screens.onboarding

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

/*
 ProfileSetupOnboardingScreen composable (connected to ViewModel):
 - Reads state from ProfileSetupViewModel
 - Name is required for saving; Finish button is disabled unless name is non-blank
 - Save uses ViewModel.saveProfile and shows a Snackbar on success
*/

private val PrimaryBlue = Color(0xFF1565C0)
private val AccentGreen = Color(0xFF2E7D32)
private val EmergencyRed = Color(0xFFD32F2F)
private val TextDark = Color(0xFF212121)

private const val NAME_PROMPT = "What is your name? This is optional."
private const val EMERGENCY_NAME_PROMPT = "Enter the name of your emergency contact."
private const val EMERGENCY_NUMBER_PROMPT = "Enter the phone number of your emergency contact."
private const val HOME_PROMPT =
    "Set your home location. This allows you to say 'Take me home' during navigation."
private const val COMPLETE_PROMPT =
    "Setup complete. You can now start navigation. Say 'Start navigation' or tap the button below."

@Composable
fun ProfileSetupOnboardingScreen(
    viewModel: ProfileSetupViewModel = viewModel(),
    onFinish: () -> Unit = {},
    onSetLocation: () -> Unit = {}
) {
    val TAG = "ProfileSetupScreen"
    val name by viewModel.name.collectAsState()
    val emergencyName by viewModel.emergencyName.collectAsState()
    val emergencyNumber by viewModel.emergencyNumber.collectAsState()
    val home by viewModel.home.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(30.dp)
        ) {
            Text(
                text = "Profile Setup",
                color = TextDark,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Please provide the information below. You can skip optional fields.",
                color = TextDark,
                fontSize = 18.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Name (optional) card with rounded corners
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = "Name icon",
                            tint = PrimaryBlue
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = "Name (required)", fontSize = 18.sp, color = TextDark)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = name,
                        onValueChange = { viewModel.onNameChange(it) },
                        placeholder = { Text(text = "Enter your name") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "Name input" },
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = NAME_PROMPT, fontSize = 14.sp, color = TextDark)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Emergency contact card with rounded fields and red tint
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Phone,
                            contentDescription = "Emergency contact icon",
                            tint = EmergencyRed
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = "Emergency contact", fontSize = 18.sp, color = TextDark)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = emergencyName,
                        onValueChange = { viewModel.onEmergencyNameChange(it) },
                        placeholder = { Text(text = "Contact name") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "Emergency contact name input" },
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = emergencyNumber,
                        onValueChange = { viewModel.onEmergencyNumberChange(it) },
                        placeholder = { Text(text = "Phone number") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "Emergency contact number input" },
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = EMERGENCY_NAME_PROMPT, fontSize = 14.sp, color = TextDark)
                    Text(text = EMERGENCY_NUMBER_PROMPT, fontSize = 14.sp, color = TextDark)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Home address / set location card
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Home,
                            contentDescription = "Home icon",
                            tint = PrimaryBlue
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = "Home address (optional)", fontSize = 18.sp, color = TextDark)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Use ifBlank to show a fallback when label is empty
                    Text(text = home.ifBlank { "No home location set" }, color = TextDark)

                    Spacer(modifier = Modifier.height(8.dp))

                    // Set Location as a prominent blue full-width rounded button
                    Button(
                        onClick = { onSetLocation() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .semantics { contentDescription = "Set home location" },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Home,
                            contentDescription = "Set location icon",
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Set Location", fontSize = 16.sp, color = Color.White)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = HOME_PROMPT, fontSize = 14.sp, color = TextDark)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            val isSaveEnabled = name.isNotBlank()

            Button(
                onClick = {
                    // save via ViewModel and show snackbar when saved
                    coroutineScope.launch {
                        Log.d(TAG, "Attempting to save profile from UI")
                        viewModel.saveProfile {
                            // show a confirmation snackbar and then call onFinish to exit onboarding
                            coroutineScope.launch {
                                Log.d(TAG, "Showing confirmation snackbar")
                                val contactInfo =
                                    if (emergencyName.isNotBlank() || emergencyNumber.isNotBlank()) {
                                        "Emergency: ${emergencyName.ifBlank { "(no name)" }} ${emergencyNumber.ifBlank { "(no number)" }}"
                                    } else {
                                        "No emergency contact set"
                                    }
                                snackbarHostState.showSnackbar("Profile saved — $contactInfo")
                                Log.d(TAG, "Calling onFinish callback")
                                onFinish()
                            }
                        }
                    }
                },
                enabled = isSaveEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .semantics { contentDescription = "Finish setup" },
                colors = ButtonDefaults.buttonColors(containerColor = AccentGreen)
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = "Finish icon")
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = "Finish", fontSize = 18.sp, color = Color.White)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = COMPLETE_PROMPT,
                fontSize = 14.sp,
                color = TextDark,
                modifier = Modifier.padding(top = 8.dp)
            )

            // Place SnackbarHost at the bottom of the Column
            Spacer(modifier = Modifier.weight(1f))
            SnackbarHost(hostState = snackbarHostState)
        }
    }
}
