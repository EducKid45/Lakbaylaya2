package com.example.lakbaylaya.ui.screens.map.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.lakbaylaya.ui.screens.map.models.VoiceNoteItem

/**
 * Compact row display for a voice note with preview, edit, and delete actions.
 */
@Composable
fun CompactVoiceNoteRow(
    note: VoiceNoteItem,
    onPreview: (VoiceNoteItem) -> Unit,
    onEdit: (VoiceNoteItem) -> Unit,
    onDelete: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 0.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    note.label,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                )
                if (note.text.isNotBlank()) {
                    Text(
                        text = note.text.take(80),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                } else {
                    Text(
                        text = "(No transcription)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            IconButton(onClick = { onPreview(note) }, modifier = Modifier.size(40.dp)) {
                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Preview")
            }

            IconButton(onClick = { onEdit(note) }, modifier = Modifier.size(40.dp)) {
                Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit voice note")
            }

            IconButton(onClick = { onDelete(note.id) }, modifier = Modifier.size(40.dp)) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete voice note")
            }
        }
    }
}

