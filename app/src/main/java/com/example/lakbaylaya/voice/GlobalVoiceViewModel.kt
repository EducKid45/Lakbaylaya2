@file:Suppress("unused", "MemberVisibilityCanBePrivate", "LeakingThis")
package com.example.lakbaylaya.voice

import android.app.Application
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

private const val TAG = "GlobalVoiceVM"

/** Lightweight result item passed from MapScreen back to NavigateDialogEngine. */
data class SearchResultItem(
    val name: String,
    val address: String,
    val lat: Double,
    val lon: Double
)

/**
 * Event emitted by the navigate-dialog for map-specific actions.
 * Collected by MainAppHost to drive navController with the right params.
 */
sealed class VoiceMapEvent {
    /** Open map immediately and activate the search bar — fired before asking "where to go?". */
    object OpenSearchMap : VoiceMapEvent()
    /** Open map and immediately start navigation to a saved route's destination. */
    data class StartSavedRoute(
        val destLat: Double,
        val destLon: Double,
        val name: String
    ) : VoiceMapEvent()
}

/**
 * GlobalVoiceViewModel — owns the complete voice-command state machine.
 *
 * State flow:
 *   IDLE → WAKE_LISTENING → COMMAND_LISTENING → WAKE_LISTENING
 */
class GlobalVoiceViewModel(application: Application) : AndroidViewModel(application) {

    @Suppress("StaticFieldLeak")
    private val context = application.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())

    // Processing cue configuration — tweak as needed
    private val processingTts      = true
    private val processingVibrate  = true

    // core components
    private val tts               = VoiceManager(context)
    private val wakeEngine        = WakeWordEngine(context) { onWakeDetected() }
    private val commandRecognizer = VoiceRecognizer(context)
    private val commandRepo       = AppVoiceCommandRepository(context)

    // Providers injected from MainAppHost so NavigateDialogEngine reads live Room data.
    var savedRoutesProvider: (() -> List<com.example.lakbaylaya.ui.screens.route.SavedRoute>) = { emptyList() }
    var savedPlacesProvider: (() -> List<com.example.lakbaylaya.ui.screens.route.SavedPlace>) = { emptyList() }

    /**
     * Returns the full unified saved-locations list (routes + places) for "show routes"
     * TTS reading. Injected from MainAppHost.
     */
    var savedLocationsProvider: (() -> List<com.example.lakbaylaya.ui.screens.route.LocationItem>) = { emptyList() }

    // state
    private val _voiceState = MutableStateFlow(WakeWordState.IDLE)
    val voiceState: StateFlow<WakeWordState> = _voiceState.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val navigationEvent: SharedFlow<String> = _navigationEvent.asSharedFlow()

    private val _actionEvent = MutableSharedFlow<AppVoiceCommand.ActionCommand>(extraBufferCapacity = 1)
    val actionEvent: SharedFlow<AppVoiceCommand.ActionCommand> = _actionEvent.asSharedFlow()

    /** Emitted by the navigate-dialog for map-specific routing (search or saved route). */
    private val _voiceMapEvent = MutableSharedFlow<VoiceMapEvent>(replay = 1, extraBufferCapacity = 1)
    val voiceMapEvent: SharedFlow<VoiceMapEvent> = _voiceMapEvent.asSharedFlow()

    /**
     * Called by MapScreen after it has consumed a VoiceMapEvent.
     * Resets the replay cache so the event isn't re-processed on recompose.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun consumeVoiceMapEvent() {
        _voiceMapEvent.resetReplayCache()
    }

    /**
     * Dedicated StateFlow for voice-triggered search queries.
     * MapScreen observes this directly — no SharedFlow race conditions.
     * Non-null = "run this search now". MapScreen sets it back to null after consuming.
     */
    private val _pendingVoiceSearch = MutableStateFlow<String?>(null)
    val pendingVoiceSearch: StateFlow<String?> = _pendingVoiceSearch.asStateFlow()

    /** Called by MapScreen once the real search results are available. */
    fun deliverSearchResults(results: List<SearchResultItem>) {
        Log.d(TAG, "deliverSearchResults: ${results.size} results")
        val cb = pendingSearchResultsCallback
        pendingSearchResultsCallback = null
        cb?.invoke(results)
    }

    /** Called by MapScreen after it has consumed the pending search query. */
    fun clearPendingVoiceSearch() {
        _pendingVoiceSearch.value = null
    }

    /**
     * Pending callback registered by NavigateDialogEngine when it fires a search query.
     * MapScreen calls deliverSearchResults() once the real API search resolves.
     */
    private var pendingSearchResultsCallback: ((List<SearchResultItem>) -> Unit)? = null

    /** Last spoken transcript — used by handleResolvedCommand to inspect original phrasing. */
    private var lastTranscript: String = ""

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Shortcut used by quick-command buttons and the mic-toggle on Home screen.
     * Bypasses wake-word and mic listening — goes directly through the same
     * [handleResolvedCommand] pipeline that the voice engine uses after recognition.
     * Speaks the command's TTS feedback before executing so the user hears confirmation.
     */
    fun executeCommandDirectly(command: AppVoiceCommand) {
        Log.d(TAG, "executeCommandDirectly: ${command.commandName}")
        mainHandler.post {
            maybeProcessingCue()
            if (command.ttsFeedback.isNotBlank()) {
                tts.speakThenPauseThenDo(command.ttsFeedback, pauseMs = 400L) {
                    handleResolvedCommand(command)
                }
            } else {
                handleResolvedCommand(command)
            }
        }
    }

    /**
     * Jump directly into the SEARCH branch of the navigate dialog
     * (skips the "search or saved route?" question).
     * Used by the "Search Destination" quick-command shortcut.
     */
    fun startSearchDialog() {
        Log.d(TAG, "startSearchDialog()")
        mainHandler.post {
            wakeEngine.stop()
            transitionTo(WakeWordState.COMMAND_LISTENING)
            NavigateDialogEngine(
                tts               = tts,
                recognizer        = commandRecognizer,
                getSavedRoutes    = savedRoutesProvider,
                getSavedPlaces    = savedPlacesProvider,
                onOpenSearchMap   = { _voiceMapEvent.tryEmit(VoiceMapEvent.OpenSearchMap) },
                onSearchQuery     = { query, onResults ->
                    pendingSearchResultsCallback = onResults
                    _pendingVoiceSearch.value = query
                },
                onStartSavedRoute = { route ->
                    _voiceMapEvent.tryEmit(
                        VoiceMapEvent.StartSavedRoute(route.endLatitude, route.endLongitude, route.name)
                    )
                    returnToWake()
                },
                onNavigateToResult = { result ->
                    _voiceMapEvent.tryEmit(
                        VoiceMapEvent.StartSavedRoute(result.lat, result.lon, result.name)
                    )
                    returnToWake()
                },
                onCancel = { pendingSearchResultsCallback = null; _pendingVoiceSearch.value = null; returnToWake() }
            ).startSearchOnly()
        }
    }

    /**
     * Jump directly into the SAVED ROUTE branch of the navigate dialog
     * (skips the "search or saved route?" question).
     * Used by the "Use Saved Route" quick-command shortcut.
     */
    fun startSavedRouteDialog() {
        Log.d(TAG, "startSavedRouteDialog()")
        mainHandler.post {
            wakeEngine.stop()
            transitionTo(WakeWordState.COMMAND_LISTENING)
            NavigateDialogEngine(
                tts               = tts,
                recognizer        = commandRecognizer,
                getSavedRoutes    = savedRoutesProvider,
                getSavedPlaces    = savedPlacesProvider,
                onOpenSearchMap   = { _voiceMapEvent.tryEmit(VoiceMapEvent.OpenSearchMap) },
                onSearchQuery     = { query, onResults ->
                    pendingSearchResultsCallback = onResults
                    _pendingVoiceSearch.value = query
                },
                onStartSavedRoute = { route ->
                    _voiceMapEvent.tryEmit(
                        VoiceMapEvent.StartSavedRoute(route.endLatitude, route.endLongitude, route.name)
                    )
                    returnToWake()
                },
                onNavigateToResult = { result ->
                    _voiceMapEvent.tryEmit(
                        VoiceMapEvent.StartSavedRoute(result.lat, result.lon, result.name)
                    )
                    returnToWake()
                },
                onCancel = { pendingSearchResultsCallback = null; _pendingVoiceSearch.value = null; returnToWake() }
            ).startSavedRouteOnly()
        }
    }

    fun start() {
        Log.d(TAG, "start() state=${_voiceState.value}")
        if (_voiceState.value == WakeWordState.IDLE) {
            transitionTo(WakeWordState.WAKE_LISTENING)
            wakeEngine.start()
        }
    }

    fun stop() {
        Log.d(TAG, "stop()")
        mainHandler.removeCallbacksAndMessages(null)
        wakeEngine.stop()
        commandRecognizer.destroy()
        tts.stop()
        transitionTo(WakeWordState.IDLE)
    }

    /**
     * Called when the user taps the mic button on Home screen.
     * Skips wake-word detection and enters command listening directly —
     * exactly the same as if the user had said "listen".
     * If already listening, stops the current session.
     */
    fun toggleCommandListening() {
        Log.d(TAG, "toggleCommandListening() state=${_voiceState.value}")
        when (_voiceState.value) {
            WakeWordState.COMMAND_LISTENING,
            WakeWordState.TOGGLED_LISTENING -> {
                // Already active — stop
                tts.speak("Voice commands stopped.")
                mainHandler.postDelayed({ stop() }, 700L)
            }
            else -> {
                // Stop wake engine and go straight to command mode
                wakeEngine.stop()
                onWakeDetected()
            }
        }
    }

    // ── Wake word flow ────────────────────────────────────────────────────────

    private fun onWakeDetected() {
        Log.d(TAG, "onWakeDetected()")
        transitionTo(WakeWordState.COMMAND_LISTENING)

        // Use pause between utterances so user can clearly distinguish each sentence
        tts.speakThenPauseThenDo("Yes, I'm listening", pauseMs = 600L) {
            tts.speakThenPauseThenDo("Please speak your command", pauseMs = 400L) {
                startCommandLoop(maxRetries = 6)
            }
        }
    }

    // ── Command loop ──────────────────────────────────────────────────────────

    private fun startCommandLoop(maxRetries: Int = 6, attempt: Int = 0) {
        if (_voiceState.value != WakeWordState.COMMAND_LISTENING &&
            _voiceState.value != WakeWordState.TOGGLED_LISTENING) {
            Log.w(TAG, "startCommandLoop() wrong state=${_voiceState.value}")
            returnToWake()
            return
        }

        Log.d(TAG, "startCommandLoop() attempt=${attempt + 1}/$maxRetries")

        commandRecognizer.listenDialog(
            onFinal = { text ->
                Log.d(TAG, "command received: '$text'")
                val command = commandRepo.resolve(text)

                if (command is AppVoiceCommand.UnknownCommand) {
                    if (attempt + 1 >= maxRetries) {
                        // All attempts used — let user know and return to wake so they can try again
                        tts.speakThenDo(
                            "I couldn't understand after ${maxRetries} tries. " +
                            "Returning to wake listening. Please try again."
                        ) { returnToWake() }
                    } else {
                        val remaining = maxRetries - (attempt + 1)
                        tts.speakThenDo(
                            "I didn't understand. Please speak your command again. " +
                            "You have $remaining ${if (remaining == 1) "try" else "tries"} left."
                        ) { startCommandLoop(maxRetries, attempt + 1) }
                    }
                    return@listenDialog
                }

                maybeProcessingCue()
                lastTranscript = text

                if (processingTts) {
                    tts.speakThenDo("Processing") { handleResolvedCommand(command) }
                } else {
                    handleResolvedCommand(command)
                }
            },
            onError = {
                Log.w(TAG, "command recognizer error attempt=${attempt + 1}")
                if (attempt + 1 >= maxRetries) {
                    tts.speakThenDo(
                        "I couldn't hear you after ${maxRetries} tries. " +
                        "Returning to wake listening. Please try again."
                    ) { returnToWake() }
                } else {
                    val remaining = maxRetries - (attempt + 1)
                    tts.speakThenDo(
                        "I didn't hear that. Please speak your command again. " +
                        "You have $remaining ${if (remaining == 1) "try" else "tries"} left."
                    ) { startCommandLoop(maxRetries, attempt + 1) }
                }
            }
        )
    }

    // ── Command dispatch ──────────────────────────────────────────────────────

    private fun handleResolvedCommand(command: AppVoiceCommand) {
        when (command) {
            // ── Navigate dialog: multi-turn spoken flow ───────────────────────
            AppVoiceCommand.ActionCommand.NavigateDialog -> {
                // Normalise common typos before checking — "save " → "saved ", etc.
                val t = lastTranscript.lowercase()
                    .replace(Regex("\\bsave\\b"),  "saved")
                    .replace(Regex("\\bsaves\\b"), "saved")
                    .replace("locaion",  "location")
                    .replace("locaton",  "location")
                    .replace("locatin",  "location")
                    .replace("locatn",   "location")
                    .replace("lacation", "location")
                    .replace("loacation","location")
                    .replace(Regex("\\brout\\b"), "route")
                    .replace(Regex("\\bplac\\b"), "place")

                val isSavedPhrase = listOf(
                    "saved route", "saved routes", "my routes", "my saved",
                    "saved location", "saved locations", "saved place", "saved places",
                    "use saved", "use my route", "my place", "my location",
                    "pick a route", "open saved route", "use a saved"
                ).any { t.contains(it) }

                if (isSavedPhrase) {
                    startSavedRouteDialog()
                } else {
                    startNavigateDialog()
                }
            }

            // ── GoRoutes: read names of all saved locations aloud, then navigate ─
            AppVoiceCommand.NavigationCommand.GoRoutes -> {
                val locations = savedLocationsProvider()
                val speech = buildString {
                    if (locations.isEmpty()) {
                        append("You have no saved locations yet. Save a route or place from the map first.")
                    } else {
                        append("Opening your saved locations. ")
                        append("You have ${locations.size} saved ${if (locations.size == 1) "location" else "locations"}. ")
                        locations.forEachIndexed { index, item ->
                            val typeWord = if (item.type == com.example.lakbaylaya.ui.screens.route.LocationItemType.ROUTE) "Route" else "Place"
                            append("${index + 1}. $typeWord: ${item.name}. ")
                        }
                    }
                }
                tts.speakThenPauseThenDo(speech, pauseMs = 300L) {
                    _navigationEvent.tryEmit(AppVoiceCommand.NavigationCommand.GoRoutes.targetRoute)
                    returnToWake()
                }
            }

            is AppVoiceCommand.NavigationCommand -> {
                tts.speakThenDo(command.ttsFeedback) {
                    _navigationEvent.tryEmit(command.targetRoute)
                    returnToWake()
                }
            }

            is AppVoiceCommand.ActionCommand -> {
                if (command == AppVoiceCommand.ActionCommand.StopListening) {
                    tts.speak(command.ttsFeedback)
                    mainHandler.postDelayed({ stop() }, 800L)
                } else {
                    tts.speakThenDo(command.ttsFeedback) {
                        _actionEvent.tryEmit(command)
                        returnToWake()
                    }
                }
            }

            AppVoiceCommand.UnknownCommand -> {
                tts.speakThenDo("I didn't understand. Returning to wake listening") { returnToWake() }
            }
        }
    }

    // ── Navigate dialog ───────────────────────────────────────────────────────

    private fun startNavigateDialog() {
        Log.d(TAG, "startNavigateDialog()")
        NavigateDialogEngine(
            tts               = tts,
            recognizer        = commandRecognizer,
            getSavedRoutes    = savedRoutesProvider,
            getSavedPlaces    = savedPlacesProvider,
            onOpenSearchMap   = {
                Log.d(TAG, "dialog → open search map immediately")
                _voiceMapEvent.tryEmit(VoiceMapEvent.OpenSearchMap)
            },
            onSearchQuery     = { query, onResults ->
                Log.d(TAG, "dialog → search query '$query'")
                pendingSearchResultsCallback = onResults
                _pendingVoiceSearch.value = query
            },
            onStartSavedRoute = { route ->
                Log.d(TAG, "dialog → saved route '${route.name}' lat=${route.endLatitude} lon=${route.endLongitude}")
                _voiceMapEvent.tryEmit(
                    VoiceMapEvent.StartSavedRoute(
                        destLat = route.endLatitude,
                        destLon = route.endLongitude,
                        name    = route.name
                    )
                )
                returnToWake()
            },
            onNavigateToResult = { result ->
                Log.d(TAG, "dialog → navigate to result '${result.name}' lat=${result.lat} lon=${result.lon}")
                _voiceMapEvent.tryEmit(
                    VoiceMapEvent.StartSavedRoute(
                        destLat = result.lat,
                        destLon = result.lon,
                        name    = result.name
                    )
                )
                returnToWake()
            },
            onCancel = {
                Log.d(TAG, "dialog → cancelled")
                pendingSearchResultsCallback = null
                _pendingVoiceSearch.value = null
                returnToWake()
            }
        ).start()
    }

    // ── Processing cue ────────────────────────────────────────────────────────

    private fun maybeProcessingCue() {
        if (processingVibrate) vibrateShort()
    }

    @Suppress("ObsoleteSdkInt")
    private fun vibrateShort() {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(Vibrator::class.java)
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? Vibrator
            }
            vibrator?.let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    it.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    it.vibrate(80)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "vibrateShort failed: ${e.message}")
        }
    }

    // ── Return to wake ────────────────────────────────────────────────────────

    private fun returnToWake() {
        Log.d(TAG, "returnToWake()")
        transitionTo(WakeWordState.WAKE_LISTENING)
        mainHandler.postDelayed({
            if (_voiceState.value == WakeWordState.WAKE_LISTENING) wakeEngine.start()
        }, 400L)
    }

    // ── State helpers ─────────────────────────────────────────────────────────

    private fun transitionTo(state: WakeWordState) {
        val prev = _voiceState.value
        if (prev == state) return
        _voiceState.value = state
        Log.d(TAG, "STATE $prev → $state")
    }

    override fun onCleared() {
        super.onCleared()
        stop()
        tts.shutdown()
    }

    // Legacy aliases
    fun startWakeListening() = start()
    fun stopAll() = stop()
}
