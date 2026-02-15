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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.lakbaylaya.ui.screens.map.models.VoiceNoteItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

/**
 * Dialog for recording and editing voice notes with transcription.
 */
@Composable
fun VoiceNoteEditorDialog(
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

                RecordingControls(
                    isRecording = isRecording,
                    seconds = seconds,
                    recordedPath = recordedPath,
                    isPlaying = isPlaying,
                    onStartRecording = { startRecording(context) },
                    onStopRecording = { stopRecording() },
                    onPlay = { play(recordedPath!!) },
                    onStopPlay = { stopPlay() },
                    onDeleteRecording = { recordedPath = null }
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

/**
 * Recording control UI with progress indicator and playback controls.
 */
@Composable
private fun RecordingControls(
    isRecording: Boolean,
    seconds: Int,
    recordedPath: String?,
    isPlaying: Boolean,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onPlay: () -> Unit,
    onStopPlay: () -> Unit,
    onDeleteRecording: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
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
            if (isRecording) {
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
                    if (isRecording) onStopRecording() else onStartRecording()
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

        if (recordedPath != null && !isRecording) {
            IconButton(onClick = {
                if (isPlaying) onStopPlay() else onPlay()
            }, modifier = Modifier.size(44.dp)) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Stop preview" else "Play preview"
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            IconButton(onClick = onDeleteRecording, modifier = Modifier.size(44.dp)) {
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

