@file:Suppress("unused")
package com.example.lakbaylaya.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Inline profile editor — Name ONLY.
 *
 * Rules enforced:
 *  • Name must not be blank — Save is disabled and shows error until filled
 *  • Phone editing removed from this screen (phone is preserved in storage)
 *  • Home / Work location are NOT editable here → use ProfileScreen map pins
 *  • Emergency contact is NOT editable here → use Settings → Safety & Emergency
 *  • All changes persist to Room via [ProfileViewModel.updateProfile]
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditorScreen(
    viewModel: ProfileViewModel = viewModel(),
    onBack: () -> Unit,
    // Home/Work marker callbacks are kept for nav-graph compatibility but NOT shown in this screen
    @Suppress("UNUSED_PARAMETER") onOpenHomeMarker: () -> Unit = {},
    @Suppress("UNUSED_PARAMETER") onOpenWorkMarker: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val profile = uiState.userProfile

    val nameState       = remember(profile.name) { mutableStateOf(profile.name) }
    val nameTouchedState = remember { mutableStateOf(false) }

    val name        = nameState.value
    val nameTouched = nameTouchedState.value

    val nameError = nameTouched && name.trim().isBlank()
    val canSave   = name.trim().isNotBlank()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Profile", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            nameTouchedState.value = true
                            if (canSave) {
                                // Preserve existing phoneNumber from stored profile
                                viewModel.updateProfile(
                                    name         = name.trim(),
                                    phoneNumber  = profile.phoneNumber,
                                    homeLocation = profile.homeLocation,
                                    homeLat      = profile.homeLat,
                                    homeLon      = profile.homeLon,
                                    workLocation = profile.workLocation,
                                    workLat      = profile.workLat,
                                    workLon      = profile.workLon
                                )
                                onBack()
                            }
                        },
                        enabled = canSave
                    ) { Text("Save", fontWeight = FontWeight.Bold) }
                }
            )
        }
    ) { pad ->
        Column(
            Modifier
                .fillMaxSize()
                .background(Color(0xFFF8F9FA))
                .padding(pad)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            // ── Name ──────────────────────────────────────────────────────────
            OutlinedTextField(
                value          = name,
                onValueChange  = { nameState.value = it; nameTouchedState.value = true },
                label          = { Text("Full Name *") },
                leadingIcon    = { Icon(Icons.Default.Person, null) },
                singleLine     = true,
                isError        = nameError,
                supportingText = {
                    if (nameError) Text("Name cannot be empty", color = MaterialTheme.colorScheme.error)
                },
                modifier       = Modifier.fillMaxWidth(),
                shape          = RoundedCornerShape(12.dp)
            )

            HorizontalDivider(Modifier.padding(vertical = 4.dp))

            // Info notice — home/work editing is on Profile screen
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color    = Color(0xFFE3F2FD),
                shape    = RoundedCornerShape(10.dp)
            ) {
                Row(Modifier.padding(12.dp)) {
                    Icon(Icons.Default.Info, null, tint = Color(0xFF1976D2), modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Home and Work locations are set via map pins on the Profile screen.",
                        style = MaterialTheme.typography.bodySmall, color = Color(0xFF1565C0))
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color    = Color(0xFFFFEBEE),
                shape    = RoundedCornerShape(10.dp)
            ) {
                Row(Modifier.padding(12.dp)) {
                    Icon(Icons.Default.Security, null, tint = Color(0xFFE53935), modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Emergency contact is managed in Settings → Safety & Emergency.",
                        style = MaterialTheme.typography.bodySmall, color = Color(0xFFC62828))
                }
            }
        }
    }
}
