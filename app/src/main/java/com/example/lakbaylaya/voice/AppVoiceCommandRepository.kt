@file:Suppress("unused", "MemberVisibilityCanBePrivate")
package com.example.lakbaylaya.voice

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val TAG = "AppVoiceCmdRepo"
private const val PREFS_NAME = "app_voice_commands"
private const val KEY_CUSTOM_COMMANDS = "custom_commands_json"

/**
 * Repository that stores and resolves all app-wide voice commands.
 *
 * - Built-in commands come from [AppVoiceCommand.ALL_BUILTIN].
 * - Custom / user-added commands are persisted in SharedPreferences as
 *   a simple pipe-delimited string: "trigger1|trigger2|...|commandName".
 * - The processor uses this repository to match spoken input at runtime.
 */
class AppVoiceCommandRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // All commands (built-in + custom labels) exposed for UI
    private val _allCommands = MutableStateFlow<List<AppVoiceCommand>>(AppVoiceCommand.ALL_BUILTIN)
    val allCommands: StateFlow<List<AppVoiceCommand>> = _allCommands.asStateFlow()

    init {
        // Load persisted state on creation
        refreshCommands()
    }

    private fun refreshCommands() {
        _allCommands.value = AppVoiceCommand.ALL_BUILTIN
        Log.d(TAG, "Commands loaded: ${_allCommands.value.size} total")
    }

    /**
     * Match a spoken transcript against all known commands.
     * Tries exact, contains, then prefix matching (case-insensitive).
     *
     * @param transcript Raw spoken text from SpeechRecognizer
     * @return Best matching [AppVoiceCommand], or [AppVoiceCommand.UnknownCommand]
     */
    fun resolve(transcript: String): AppVoiceCommand {
        val normalized = transcript.lowercase().trim()
        val commands = _allCommands.value

        // 1. Exact match
        commands.forEach { cmd ->
            if (cmd.triggers.any { it == normalized }) {
                Log.d(TAG, "Exact match: '${cmd.commandName}' for '$normalized'")
                return cmd
            }
        }

        // 2. Contains match (input contains trigger phrase)
        commands.forEach { cmd ->
            if (cmd.triggers.any { normalized.contains(it) }) {
                Log.d(TAG, "Contains match: '${cmd.commandName}' for '$normalized'")
                return cmd
            }
        }

        // 3. Normalise common typos then retry contains match
        //    "save " → "saved ", "locaion" → "location", "rout " → "route "
        val corrected = normalized
            .replace(Regex("\\bsave\\b"), "saved")   // "save route" → "saved route"
            .replace(Regex("\\bsaves\\b"), "saved")  // "saves route" → "saved route"
            .replace("locaion", "location")
            .replace("locaton", "location")
            .replace("locatin", "location")
            .replace("locatn",  "location")
            .replace("lacation","location")
            .replace("loacation","location")
            .replace(Regex("\\brout\\b"), "route")   // "rout" → "route"
            .replace(Regex("\\bplac\\b"), "place")   // "plac" → "place"
        if (corrected != normalized) {
            commands.forEach { cmd ->
                if (cmd.triggers.any { corrected.contains(it) || it == corrected }) {
                    Log.d(TAG, "Corrected match: '${cmd.commandName}' for '$normalized' → '$corrected'")
                    return cmd
                }
            }
        }

        // 4. Word-overlap match (trigger words found in input)
        val inputWords = normalized.split(" ").filter { it.length > 2 }
        commands.forEach { cmd ->
            val triggerWords = cmd.triggers.flatMap { it.split(" ") }.toSet()
            val matchCount = inputWords.count { triggerWords.contains(it) }
            if (matchCount >= 2 || (inputWords.size == 1 && matchCount == 1)) {
                Log.d(TAG, "Word match: '${cmd.commandName}' for '$normalized'")
                return cmd
            }
        }

        // 5. Fuzzy similarity — catches single-letter drops / swaps
        //    Score = shared characters / max(len(input), len(trigger))
        //    Threshold 0.82 — tight enough to avoid false positives
        var bestScore = 0.0
        var bestCmd: AppVoiceCommand = AppVoiceCommand.UnknownCommand
        commands.forEach { cmd ->
            cmd.triggers.forEach { trigger ->
                val score = similarity(normalized, trigger)
                if (score > bestScore) { bestScore = score; bestCmd = cmd }
            }
        }
        if (bestScore >= 0.82) {
            Log.d(TAG, "Fuzzy match (score=${"%.2f".format(bestScore)}): '${bestCmd.commandName}' for '$normalized'")
            return bestCmd
        }

        Log.d(TAG, "No match for: '$normalized'")
        return AppVoiceCommand.UnknownCommand
    }

    /**
     * Simple bigram-based similarity score in [0,1].
     * Returns 1.0 for identical strings, ~0.9 for one missing character.
     */
    private fun similarity(a: String, b: String): Double {
        if (a == b) return 1.0
        if (a.isEmpty() || b.isEmpty()) return 0.0
        val bigrams = { s: String -> (0 until s.length - 1).map { s.substring(it, it + 2) } }
        val ab = bigrams(a).toMutableList()
        val bb = bigrams(b).toMutableList()
        if (ab.isEmpty() || bb.isEmpty()) {
            // Single-char strings — fall back to char equality
            return if (a[0] == b[0]) 1.0 else 0.0
        }
        var hits = 0
        ab.forEach { g ->
            val idx = bb.indexOf(g)
            if (idx >= 0) { hits++; bb.removeAt(idx) }
        }
        return 2.0 * hits / (ab.size + bigrams(b).size)
    }

    /**
     * Returns a human-readable list of all supported commands for display or TTS.
     */
    fun getCommandSummary(): List<String> =
        _allCommands.value
            .filter { it !is AppVoiceCommand.UnknownCommand }
            .map { cmd -> "\"${cmd.triggers.firstOrNull() ?: cmd.commandName}\" → ${cmd.commandName}" }
}


