package com.example.lakbaylaya.ui.screens.map.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.lakbaylaya.ui.screens.map.models.MarkerMetadata
import com.example.lakbaylaya.ui.screens.map.models.VoiceNoteItem

/**
 * Popup card content with three sections:
 * 1. Fixed Header (title)
 * 2. Scrollable Marker Actions (marker editing, voice notes, landmark)
 * 3. Fixed Action Buttons (Cancel / Save)
 */
@Composable
fun MarkerActionDialogContent(
    metadata: MarkerMetadata,
    onMetadataChange: (MarkerMetadata) -> Unit,
    onDismiss: () -> Unit,
    onSaveNotes: (MarkerMetadata) -> Unit,
    onShowMapEditor: () -> Unit,
    modifier: Modifier = Modifier // moved here
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .semantics {
                contentDescription = "Marker action dialog"
            },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 32.dp,
        shadowElevation = 32.dp
    ) {
        // Manage local UI state for voice note editor / preview
        var showVoiceEditor by remember { mutableStateOf(false) }
        var editingNote by remember { mutableStateOf<VoiceNoteItem?>(null) }
        var showPreview by remember { mutableStateOf<VoiceNoteItem?>(null) }
        // Attachment dialogs state
        var showLandmarkAttachDialog by remember { mutableStateOf(false) }

        Column(modifier = Modifier.fillMaxSize()) {
            // Header with title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Marker",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.semantics {
                        contentDescription = "Marker dialog title"
                    }
                )
            }

            HorizontalDivider()

            // ===== SCROLLABLE MARKER ACTIONS SECTION =====
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                // Edit Marker header
                item {
                    Text(
                        text = "Edit Marker",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        modifier = Modifier.semantics { contentDescription = "Edit marker section" }
                    )
                }

                item {
                    EditMarkerSection(
                        onEditLocationClick = onShowMapEditor,
                        markedPlaceName = metadata.landmarkName,
                        markedLatitude = metadata.latitude,
                        markedLongitude = metadata.longitude
                    )
                }

                // Voice Notes Header
                item {
                    Text(
                        text = "Voice Notes",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        modifier = Modifier.semantics { contentDescription = "Voice notes header" }
                    )
                }

                item {
                    VoiceNotesSection(
                        voiceNotes = metadata.voiceNotes,
                        onAddVoiceNote = { showVoiceEditor = true },
                        onPreview = { showPreview = it },
                        onEdit = { toEdit ->
                            editingNote = toEdit
                            showVoiceEditor = true
                        },
                        onDelete = { id ->
                            val updatedNotes = metadata.voiceNotes.filter { it.id != id }
                            onMetadataChange(metadata.copy(voiceNotes = updatedNotes))
                        }
                    )
                }

                // Landmark header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Landmark",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            modifier = Modifier.semantics { contentDescription = "Landmark header" }
                        )
                        IconButton(onClick = { showLandmarkAttachDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add landmark details"
                            )
                        }
                    }
                }

                // Landmark details or empty state
                item {
                    LandmarkDetailsDisplay(
                        metadata = metadata,
                        onMetadataChange = onMetadataChange
                    )
                }
            }

            HorizontalDivider()

            // ===== FIXED ACTION BUTTONS =====
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .semantics { contentDescription = "Cancel marker actions dialog" }
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = {
                        // Only call onSaveNotes; do not dismiss here. Caller will dismiss after save completes.
                        onSaveNotes(metadata)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .semantics { contentDescription = "Save all marker actions and metadata" }
                ) {
                    Text("Save All Notes")
                }
            }
        }

        // Voice note editor dialog (Add / Edit)
        if (showVoiceEditor) {
            VoiceNoteEditorDialog(
                initial = editingNote,
                onDismiss = { showVoiceEditor = false },
                onSave = { text, audioPath ->
                    if (editingNote == null) {
                        val id = System.currentTimeMillis().toString()
                        val label = "Voice Note ${metadata.voiceNotes.size + 1}"
                        val newNote = VoiceNoteItem(
                            id = id,
                            label = label,
                            text = text,
                            audioFilePath = audioPath
                        )
                        onMetadataChange(metadata.copy(voiceNotes = metadata.voiceNotes + newNote))
                    } else {
                        val updated = metadata.voiceNotes.map {
                            if (it.id == editingNote!!.id) it.copy(
                                text = text,
                                audioFilePath = audioPath
                            ) else it
                        }
                        onMetadataChange(metadata.copy(voiceNotes = updated))
                    }

                    showVoiceEditor = false
                    editingNote = null
                }
            )
        }

        // Voice note preview dialog
        showPreview?.let { note ->
            AlertDialog(
                onDismissRequest = { showPreview = null },
                title = { Text(note.label) },
                text = {
                    if (note.text.isNotBlank()) {
                        Text(note.text)
                    } else {
                        Text("(No text stored for this note)")
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showPreview = null }) { Text("Close") }
                }
            )
        }

        // Landmark creation dialog
        if (showLandmarkAttachDialog) {
            LandmarkDialog(
                metadata = metadata,
                onMetadataChange = onMetadataChange,
                onDismiss = { showLandmarkAttachDialog = false }
            )
        }
    }
}

/**
 * Edit Marker section showing pin location editing and marked place info.
 * @param onEditLocationClick Callback when user clicks edit button to open map editor
 * @param markedPlaceName The name of the marked place
 * @param markedLatitude The latitude of the marked place
 * @param markedLongitude The longitude of the marked place
 */
@Composable
private fun EditMarkerSection(
    onEditLocationClick: () -> Unit,
    markedPlaceName: String?,
    markedLatitude: Double?,
    markedLongitude: Double?
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp)),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Marker Location",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Display coordinates if available
            if (markedLatitude != null && markedLongitude != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        if (markedPlaceName?.isNotBlank() == true) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Place,
                                    contentDescription = "Location",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    markedPlaceName,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        Text(
                            "Latitude: ${String.format("%.6f", markedLatitude)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Longitude: ${String.format("%.6f", markedLongitude)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Edit pin location row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit icon",
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Edit pin location",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "Tap to adjust marker position",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onEditLocationClick) {
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Edit pin"
                    )
                }
            }
        }
    }
}
