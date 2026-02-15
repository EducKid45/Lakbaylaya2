package com.example.lakbaylaya.ui.screens.map.components

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

/** Data class representing a voice note for a location. */
data class VoiceNoteItem(
    val id: String = "",
    val label: String = "",
    val text: String = "", // store transcription or note text
    val audioFilePath: String? = null // optional recorded audio file path
)

/** Data class for marker metadata. */
data class MarkerMetadata(
    val voiceNotes: List<VoiceNoteItem> = listOf(),
    // note ids attached to the 'Landmark' section (selected from saved voice notes)
    val landmarkNoteIds: List<String> = listOf(),
    val landmarkDescription: String = "",
    val landmarkName: String = "",
    val routeDifficulty: String = "None"
)

/**
 * Modal dialog for managing voice notes and location metadata.
 * Full-screen centered popup with semi-transparent scrim overlay.
 *
 * Layout: Fixed Header + Scrollable Marker Actions + Fixed Action Buttons
 */
@Composable
fun MarkerActionDialog(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onSaveNotes: (MarkerMetadata) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!isVisible) return

    var metadata by remember { mutableStateOf(MarkerMetadata()) }

    // Use a Dialog so the overlay is rendered in a top-level window above other Compose content
    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        // Full screen overlay with semi-transparent scrim - appears ABOVE bottom sheet and any other UI
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .pointerInput(Unit) {
                    detectTapGestures {
                        // Consume all taps on scrim to prevent clicks on elements below
                        onDismiss()
                    }
                }
                .semantics(mergeDescendants = false) {
                    contentDescription = "Marker action dialog overlay"
                },
            contentAlignment = Alignment.Center
        ) {
            // Centered card popup with marker actions - in FRONT of sheet
            // Using wrapContentSize to ensure dialog doesn't get clipped
            Box(
                modifier = Modifier.wrapContentSize(),
                contentAlignment = Alignment.Center
            ) {
                MarkerActionDialogContent(
                    metadata = metadata,
                    onMetadataChange = { metadata = it },
                    onDismiss = onDismiss,
                    onSaveNotes = onSaveNotes,
                    // make dialog content fill the available width to match requested design
                    modifier = modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.95f)
                )
            }
        }
    }
}

/**
 * Popup card content with three sections:
 * 1. Fixed Header (title + close button)
 * 2. Scrollable Marker Actions (voice notes, difficulty, descriptions)
 * 3. Fixed Action Buttons (Cancel / Save)
 */
@Composable
fun MarkerActionDialogContent(
    metadata: MarkerMetadata,
    onMetadataChange: (MarkerMetadata) -> Unit,
    onDismiss: () -> Unit,
    onSaveNotes: (MarkerMetadata) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .pointerInput(Unit) {
                detectTapGestures {
                    // Consume clicks inside dialog to prevent propagation
                }
            }
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

                // (Set current location button removed per design)
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
                                Text("Marker actions", style = MaterialTheme.typography.bodyMedium)
                                // Edit-pin row will be shown below — set current location button removed
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Edit pin location row (replaces the large text box)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Place,
                                    contentDescription = "Pin icon",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Edit pin location",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        "Tap to adjust marker pin",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = {
                                    // TODO: open map pin editor / allow dragging pin — wire to map logic
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit pin"
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Other marker-specific UI could go here
                        }
                    }
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
                                onClick = {
                                    editingNote = null
                                    showVoiceEditor = true
                                },
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

                            // Show all saved voice notes under this section
                            if (metadata.voiceNotes.isEmpty()) {
                                Text(
                                    "No voice notes yet.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    metadata.voiceNotes.forEach { note ->
                                        CompactVoiceNoteRow(
                                            note = note,
                                            onPreview = { showPreview = it },
                                            onEdit = { toEdit ->
                                                editingNote = toEdit
                                                showVoiceEditor = true
                                            },
                                            onDelete = { id ->
                                                // remove note from metadata
                                                val updatedNotes =
                                                    metadata.voiceNotes.filter { it.id != id }
                                                onMetadataChange(metadata.copy(voiceNotes = updatedNotes))
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Landmark header (grouping label)
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

                // Show landmark details if they have been set
                item {
                    if (metadata.landmarkName.isNotBlank() || metadata.landmarkNoteIds.isNotEmpty() || metadata.routeDifficulty != "None") {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                // Location Name
                                if (metadata.landmarkName.isNotBlank()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Place,
                                            contentDescription = "Location",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier
                                                .size(20.dp)
                                                .padding(end = 8.dp)
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                "Location Name",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                metadata.landmarkName,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                }

                                // Route Difficulty
                                if (metadata.routeDifficulty != "None") {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                            contentDescription = "Difficulty",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier
                                                .size(20.dp)
                                                .padding(end = 8.dp)
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                "Route Difficulty",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                metadata.routeDifficulty,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                }

                                // Attached Voice Notes
                                val attached =
                                    metadata.landmarkNoteIds.mapNotNull { id -> metadata.voiceNotes.find { it.id == id } }
                                if (attached.isNotEmpty()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AudioFile,
                                            contentDescription = "Voice Notes",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier
                                                .size(20.dp)
                                                .padding(end = 8.dp)
                                                .padding(top = 4.dp)
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                "Attached Voice Notes",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Column(
                                                modifier = Modifier.padding(top = 8.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                attached.forEach { note ->
                                                    Column(modifier = Modifier.fillMaxWidth()) {
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
                            }
                        }
                    } else {
                        // Empty state when no landmark details have been added
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "No landmark",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .padding(bottom = 8.dp)
                                )
                                Text(
                                    "No landmark added yet",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Click the + button to add landmark details",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider()

            // ===== FIXED ACTION BUTTONS (NOT SCROLLABLE) =====
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
                        onSaveNotes(metadata)
                        onDismiss()
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
                        // create new note (UI-only) and append to marker's notes
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
                        // update existing note
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

        // Landmark creation dialog with location name, attach voice notes, and route difficulty
        if (showLandmarkAttachDialog) {
            val currentSelection =
                remember { mutableStateListOf<String>().apply { addAll(metadata.landmarkNoteIds) } }
            var tempName by remember { mutableStateOf(metadata.landmarkName) }
            var tempDifficulty by remember { mutableStateOf(metadata.routeDifficulty) }

            Dialog(onDismissRequest = { showLandmarkAttachDialog = false }) {
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
                                    modifier = Modifier
                                        .padding(end = 8.dp, bottom = 8.dp)
                                        .semantics { contentDescription = "$difficulty difficulty" }
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
                            TextButton(onClick = {
                                showLandmarkAttachDialog = false
                            }) { Text("Cancel") }
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
                                showLandmarkAttachDialog = false
                            }) { Text("Save") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactVoiceNoteRow(
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

@Composable
private fun VoiceNoteEditorDialog(
    initial: VoiceNoteItem?,
    onDismiss: () -> Unit,
    onSave: (String, String?) -> Unit
) {
    val context = LocalContext.current
    var text by remember { mutableStateOf(initial?.text ?: "") }
    var recordedPath by remember { mutableStateOf(initial?.audioFilePath) }
    var isRecording by remember { mutableStateOf(false) }
    var seconds by remember { mutableStateOf(0) }
    var recorder: MediaRecorder? by remember { mutableStateOf(null) }
    val scope = rememberCoroutineScope()

    @Suppress("DEPRECATION")
    fun startRecording(ctx: Context) {
        try {
            val file = File(ctx.cacheDir, "voice_note_${System.currentTimeMillis()}.mp4")
            val path = file.absolutePath
            val r = MediaRecorder()
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setOutputFile(path)
            r.prepare()
            r.start()
            recorder = r
            recordedPath = path
            isRecording = true
            seconds = 0

            // Auto-stop after 10 seconds
            scope.launch {
                while (isRecording && seconds < 10) {
                    delay(1000)
                    seconds++
                }
                if (isRecording) {
                    // stop safely
                    try {
                        recorder?.stop()
                    } catch (_: Exception) {
                    }
                    recorder?.release()
                    recorder = null
                    isRecording = false
                }
            }
        } catch (_: Exception) {
            // ignore - permission issues or prepare failures will be surfaced by absence of recording
            recordedPath = null
            recorder = null
            isRecording = false
        }
    }

    fun stopRecording() {
        try {
            recorder?.stop()
        } catch (_: Exception) {
        } finally {
            try {
                recorder?.release()
            } catch (_: Exception) {
            }
            recorder = null
            isRecording = false
        }
    }

    // Playback helper for preview inside editor
    var player: MediaPlayer? by remember { mutableStateOf(null) }
    var isPlaying by remember { mutableStateOf(false) }

    fun play(path: String) {
        try {
            player?.release()
            player = MediaPlayer().apply {
                setDataSource(path)
                prepare()
                start()
                setOnCompletionListener {
                    isPlaying = false
                    player?.release()
                    player = null
                }
            }
            isPlaying = true
        } catch (_: Exception) {
            isPlaying = false
            player?.release(); player = null
        }
    }

    fun stopPlay() {
        try {
            player?.stop()
        } catch (_: Exception) {
        }
        player?.release(); player = null; isPlaying = false
    }

    AlertDialog(
        onDismissRequest = {
            stopRecording()
            stopPlay()
            onDismiss()
        },
        title = { Text(if (initial == null) "Add Voice Note" else "Edit Voice Note") },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Description / Transcription") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Recording controls
                // Icon-style recorder with circular progress + pulsing dot + countdown
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Progress and mic
                    val progress = (seconds.coerceIn(0, 10) / 10f)
                    val animatedProgress by animateFloatAsState(
                        targetValue = if (isRecording) progress else 0f,
                        animationSpec = tween(200)
                    )
                    val transition = rememberInfiniteTransition()
                    val pulse by transition.animateFloat(
                        initialValue = 1f,
                        targetValue = 1.25f,
                        animationSpec = infiniteRepeatable<Float>(
                            animation = tween(600),
                            repeatMode = RepeatMode.Reverse
                        )
                    )

                    Box(modifier = Modifier.size(64.dp), contentAlignment = Alignment.Center) {
                        // determinate circular progress while recording
                        if (isRecording) {
                            // use the lambda overload to avoid deprecated API
                            CircularProgressIndicator(
                                progress = { animatedProgress },
                                strokeWidth = 3.dp,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier
                                    .matchParentSize()
                                    .padding(6.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                if (isRecording) stopRecording() else startRecording(context)
                            },
                            modifier = Modifier.size(64.dp)
                        ) {
                            val micTint =
                                if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = if (isRecording) "Stop recording" else "Start recording",
                                tint = micTint,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        // small pulsing red dot at top-right while recording
                        if (isRecording) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(12.dp)
                                    .scale(pulse)
                                    .background(
                                        color = MaterialTheme.colorScheme.error,
                                        shape = CircleShape
                                    )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Show remaining time in a clear format while recording, else show a hint or 'Recorded'
                    val remaining = (10 - seconds).coerceAtLeast(0)
                    Text(
                        text = when {
                            isRecording -> "${remaining}s left"
                            recordedPath != null -> "Recorded"
                            else -> "Tap mic to record (max 10s)"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // After there is a completed recording (not while recording), show compact play + delete icons for preview/retake
                    if (recordedPath != null && !isRecording) {
                        IconButton(onClick = {
                            if (isPlaying) stopPlay() else play(recordedPath!!)
                        }, modifier = Modifier.size(44.dp)) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Stop preview" else "Play preview"
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        IconButton(onClick = {
                            // remove recorded audio so user can re-record
                            recordedPath = null
                        }, modifier = Modifier.size(44.dp)) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete recording"
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Recording will auto-stop at 10 seconds. If permission is not granted recording will not work.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                stopRecording()
                stopPlay()
                onSave(text, recordedPath)
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = {
                stopRecording()
                stopPlay()
                onDismiss()
            }) { Text("Cancel") }
        }
    )
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun MarkerActionDialogPreview() {
    MaterialTheme {
        MarkerActionDialog(
            isVisible = true,
            onDismiss = {},
            onSaveNotes = { }
        )
    }
}
