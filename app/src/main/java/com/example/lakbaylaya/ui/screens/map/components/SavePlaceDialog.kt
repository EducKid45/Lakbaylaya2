package com.example.lakbaylaya.ui.screens.map.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

/**
 * Dialog for saving a place — pre-filled with the place name as default.
 */
@Composable
fun SavePlaceDialog(
    placeName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    // Pre-fill with the actual place name so the user can save with one tap
    var label by remember { mutableStateOf(placeName) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Save Place",
                    style = MaterialTheme.typography.headlineSmall
                )

                Text(
                    text = "The name is pre-filled. You can edit it before saving.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Place Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (label.isNotBlank()) {
                                onConfirm(label.trim())
                            }
                        },
                        enabled = label.isNotBlank()
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}
