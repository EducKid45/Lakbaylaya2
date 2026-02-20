package com.example.lakbaylaya.ui.screens.map.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.lakbaylaya.ui.screens.map.models.VoiceNoteItem

/**
 * Voice Notes section showing list of voice notes and add button.
 */
@Composable
fun VoiceNotesSection(
    voiceNotes: List<VoiceNoteItem>,
    onAddVoiceNote: () -> Unit,
    onPreview: (VoiceNoteItem) -> Unit,
    onEdit: (VoiceNoteItem) -> Unit,
    onDelete: (String) -> Unit
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
                    "Voice notes for this marker",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Add voice note button
            Button(
                onClick = onAddVoiceNote,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Voice Note")
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Show all saved voice notes
            if (voiceNotes.isEmpty()) {
                Text(
                    "No voice notes yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    voiceNotes.forEach { note ->
                        CompactVoiceNoteRow(
                            note = note,
                            onPreview = { onPreview(it) },
                            onEdit = { onEdit(it) },
                            onDelete = { onDelete(it) }
                        )
                    }
                }
            }
        }
    }
}

