package com.example.lakbaylaya.ui.screens.map.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.lakbaylaya.ui.screens.map.models.MarkerMetadata

/**
 * Dialog for adding landmark details: location name, route difficulty, and attaching voice notes.
 */
@Composable
fun LandmarkDialog(
    metadata: MarkerMetadata,
    onMetadataChange: (MarkerMetadata) -> Unit,
    onDismiss: () -> Unit
) {
    val currentSelection =
        remember { mutableStateListOf<String>().apply { addAll(metadata.landmarkNoteIds) } }
    var tempName by remember { mutableStateOf(metadata.landmarkName) }
    var tempDifficulty by remember { mutableStateOf(metadata.routeDifficulty) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .padding(16.dp)
                .widthIn(min = 520.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Add Landmark Details",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Location name field
                OutlinedTextField(
                    value = tempName,
                    onValueChange = { tempName = it },
                    label = { Text("Location Name") },
                    placeholder = { Text("Enter location name") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Route difficulty buttons
                Text(
                    "Route Difficulty",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                val difficulties = listOf("None", "Easy", "Moderate", "Hard")
                Column(modifier = Modifier.fillMaxWidth()) {
                    difficulties.forEach { difficulty ->
                        FilterChip(
                            selected = tempDifficulty == difficulty,
                            onClick = {
                                tempDifficulty = difficulty
                            },
                            label = { Text(difficulty) },
                            modifier = Modifier.padding(end = 8.dp, bottom = 8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Attach voice notes section
                Text(
                    "Attach Voice Notes",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                ) {
                    if (metadata.voiceNotes.isEmpty()) {
                        Text(
                            "No saved voice notes to attach.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        LazyColumn {
                            items(metadata.voiceNotes.size) { index ->
                                val note = metadata.voiceNotes[index]
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = currentSelection.contains(note.id),
                                        onCheckedChange = { checked ->
                                            if (checked) currentSelection.add(note.id) else currentSelection.remove(
                                                note.id
                                            )
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            note.label,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        )
                                        if (note.text.isNotBlank()) {
                                            Text(
                                                note.text.take(80),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        // Save all landmark details
                        onMetadataChange(
                            metadata.copy(
                                landmarkName = tempName,
                                routeDifficulty = tempDifficulty,
                                landmarkNoteIds = currentSelection.toList()
                            )
                        )
                        onDismiss()
                    }) { Text("Save") }
                }
            }
        }
    }
}

