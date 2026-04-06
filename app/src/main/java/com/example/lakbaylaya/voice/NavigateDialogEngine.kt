package com.example.lakbaylaya.voice

import android.util.Log
import com.example.lakbaylaya.ui.screens.route.SavedPlace
import com.example.lakbaylaya.ui.screens.route.SavedRoute

private const val TAG = "NavDialogEngine"

/**
 * NavigateDialogEngine â€” multi-turn spoken dialog for navigation.
 *
 * Real search flow:
 *  1. "Do you want to search or use a saved location?"
 *  2a. "search" â†’ map opens â†’ "Where do you want to go?"
 *      â†’ user speaks â†’ onSearchQuery fires real API search
 *      â†’ MapScreen delivers results â†’ user picks by number â†’ navigate
 *  2b. "saved location" â†’ reads ALL saved locations (routes + places) by name
 *      â†’ user says name â†’ confirm â†’ navigate
 *
 *  Every step: 6 retries, remaining count spoken on each fail.
 */
class NavigateDialogEngine(
    private val tts: VoiceManager,
    private val recognizer: VoiceRecognizer,
    private val getSavedRoutes: () -> List<SavedRoute>,
    private val getSavedPlaces: () -> List<SavedPlace> = { emptyList() },
    private val onOpenSearchMap: () -> Unit,
    private val onSearchQuery: (query: String, onResults: (List<SearchResultItem>) -> Unit) -> Unit,
    private val onNavigateToResult: (SearchResultItem) -> Unit,
    private val onStartSavedRoute: (SavedRoute) -> Unit,
    private val onCancel: () -> Unit
) {
    companion object { private const val MAX_RETRIES = 6 }

    /** Internal unified holder â€” route or place, both have a name + coords. */
    private sealed class SavedLocation {
        abstract val name: String
        data class Route(val route: SavedRoute) : SavedLocation() {
            override val name get() = route.name
        }
        data class Place(val place: SavedPlace) : SavedLocation() {
            override val name get() = place.placeName
        }
    }

    fun start() { Log.d(TAG, "start()"); askRouteOrSearch(0) }

    /** Skip the opening question â€” go straight to the search branch. */
    fun startSearchOnly() {
        Log.d(TAG, "startSearchOnly()")
        onOpenSearchMap()
        startSearch(0)
    }

    /** Skip the opening question â€” go straight to the saved-location branch. */
    fun startSavedRouteOnly() {
        Log.d(TAG, "startSavedRouteOnly()")
        startSavedLocation()
    }

    // â”€â”€ Step 1 â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private fun askRouteOrSearch(attempt: Int) {
        val p = if (attempt == 0)
            "Do you want to search for a new place, or use a saved location? Say search, or say saved location."
        else
            "Please say search for a new place, or saved location to use one you already saved. You have ${rem(attempt)} left."
        speak(p) {
            listen({ text ->
                when {
                    matchesSearch(text)     -> { onOpenSearchMap(); startSearch(0) }
                    matchesSavedRoute(text) -> startSavedLocation()
                    matchesCancel(text)     -> cancel("Okay, cancelling navigation.")
                    attempt + 1 >= MAX_RETRIES -> cancel("I couldn't understand. Please try again.")
                    else -> askRouteOrSearch(attempt + 1)
                }
            }, {
                if (attempt + 1 >= MAX_RETRIES) cancel("I couldn't hear you. Please try again.")
                else askRouteOrSearch(attempt + 1)
            })
        }
    }

    // â”€â”€ Search branch â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private fun startSearch(attempt: Int) {
        val p = if (attempt == 0) "Where do you want to go?"
                else "Please tell me where you want to go. You have ${rem(attempt)} left."
        speak(p) {
            listenSearch({ text ->   // ← WEB_SEARCH model for place names
                when {
                    matchesCancel(text) -> cancel("Okay, cancelling navigation.")
                    text.isNotBlank() -> {
                        val query = text.trim()
                        speak("Searching for $query.") {
                            onSearchQuery(query) { results -> onRealResults(results, query) }
                        }
                    }
                    attempt + 1 >= MAX_RETRIES ->
                        cancel("I couldn't understand the destination. Please try again.")
                    else -> startSearch(attempt + 1)
                }
            }, {
                if (attempt + 1 >= MAX_RETRIES) cancel("I couldn't hear you. Please try again.")
                else startSearch(attempt + 1)
            })
        }
    }

    private fun onRealResults(results: List<SearchResultItem>, query: String) {
        Log.d(TAG, "onRealResults: ${results.size} for '$query'")
        if (results.isEmpty()) {
            speak("I couldn't find any results for $query. Do you want to search for something else?") {
                yesNo("Say yes to search again, or no to cancel.",
                    onYes = { startSearch(0) },
                    onNo  = { cancel("Okay, cancelling navigation.") }
                )
            }
            return
        }
        val top = results.take(5)
        val optionsList = top.mapIndexed { i, r -> "Option ${i + 1}: ${r.name}" }.joinToString(". ")
        val intro = if (top.size == 1) "I found one result. Option 1: ${top[0].name}."
                    else "I found ${top.size} results. $optionsList."
        speak("$intro Say a number to navigate, say repeat to hear again, or say search again.") {
            pickResult(top, query, attempt = 0)
        }
    }

    private fun pickResult(results: List<SearchResultItem>, query: String, attempt: Int) {
        listenSearch({ text ->   // ← WEB_SEARCH so numbers AND place names resolve
            val n = parseNumber(text)
            val spokenWords = text.lowercase().split(" ").filter { it.length >= 4 }.toSet()
            val nameMatch = if (spokenWords.isNotEmpty())
                results.indexOfFirst { r ->
                    r.name.lowercase().split(" ").any { w -> w in spokenWords }
                }.takeIf { it >= 0 }
            else null

            when {
                matchesCancel(text) -> cancel("Okay, cancelling navigation.")
                matchesSearchAgain(text) -> startSearch(0)
                matchesRepeat(text) -> {
                    val fullList = results.mapIndexed { i, r -> "Option ${i + 1}: ${r.name}" }.joinToString(". ")
                    speak("$fullList. Say a number to navigate, say repeat to hear again, or say search again.") {
                        pickResult(results, query, attempt)
                    }
                }
                n != null && n in 1..results.size -> {
                    val chosen = results[n - 1]
                    speak("Navigating to ${chosen.name}.") { onNavigateToResult(chosen) }
                }
                nameMatch != null -> {
                    val chosen = results[nameMatch]
                    speak("Navigating to ${chosen.name}.") { onNavigateToResult(chosen) }
                }
                attempt + 1 >= MAX_RETRIES ->
                    cancel("I couldn't understand your selection. Please try again.")
                else -> {
                    speak("Say a number from 1 to ${results.size}, say repeat, search again, or cancel. You have ${rem(attempt)} left.") {
                        pickResult(results, query, attempt + 1)
                    }
                }
            }
        }, {
            if (attempt + 1 >= MAX_RETRIES) cancel("I couldn't hear you. Please try again.")
            else {
                speak("I didn't hear you. Say a number, repeat, search again, or cancel. You have ${rem(attempt)} left.") {
                    pickResult(results, query, attempt + 1)
                }
            }
        })
    }

    // â”€â”€ Saved location branch (routes + places) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private fun startSavedLocation() {
        // Combine routes and places into a single unified list, sorted by name
        val all = buildAllLocations()
        if (all.isEmpty()) {
            speak("You have no saved locations yet. Do you want to search instead?") {
                yesNo("Say yes to search, or no to cancel.",
                    onYes = { onOpenSearchMap(); startSearch(0) },
                    onNo  = { cancel("Okay, returning.") }
                )
            }
        } else {
            announceLocations(all, 0)
        }
    }

    private fun buildAllLocations(): List<SavedLocation> {
        val routes = getSavedRoutes().map { SavedLocation.Route(it) }
        val places = getSavedPlaces().map { SavedLocation.Place(it) }
        return (routes + places).sortedBy { it.name.lowercase() }
    }

    private fun announceLocations(locations: List<SavedLocation>, attempt: Int) {
        // Read only the names â€” no subtitles, no addresses
        val nameList = locations.joinToString(". ") { it.name }
        val header = if (locations.size == 1)
            "You have one saved location: ${locations[0].name}."
        else
            "You have ${numWord(locations.size)} saved locations: $nameList."

        val p = if (attempt == 0)
            "$header Say the name to navigate to it, or say repeat to hear the list again."
        else
            "$header Please say the name again, or say repeat. You have ${rem(attempt)} left."

        speak(p) {
            listen({ text ->
                val match = findLocation(text, locations)
                when {
                    match != null       -> confirmLocation(match, locations)
                    matchesRepeat(text) -> announceLocations(locations, attempt) // re-read, no retry consumed
                    matchesCancel(text) -> cancel("Okay, cancelling.")
                    attempt + 1 >= MAX_RETRIES -> cancel("I couldn't find that location. Please try again.")
                    else -> announceLocations(locations, attempt + 1)
                }
            }, {
                if (attempt + 1 >= MAX_RETRIES) cancel("I couldn't hear you. Please try again.")
                else announceLocations(locations, attempt + 1)
            })
        }
    }

    private fun confirmLocation(location: SavedLocation, all: List<SavedLocation>) {
        speak("You selected ${location.name}. Do you want to start navigation?") {
            yesNo("Say yes to start, or no to go back.",
                onYes = {
                    speak("Starting navigation to ${location.name}.") {
                        when (location) {
                            is SavedLocation.Route -> onStartSavedRoute(location.route)
                            is SavedLocation.Place -> {
                                // Navigate to place using SearchResultItem so the same
                                // onNavigateToResult â†’ VoiceMapEvent.StartSavedRoute path is used
                                onNavigateToResult(
                                    SearchResultItem(
                                        name    = location.place.placeName,
                                        address = location.place.address,
                                        lat     = location.place.latitude,
                                        lon     = location.place.longitude
                                    )
                                )
                            }
                        }
                    }
                },
                onNo = { announceLocations(all, 0) }
            )
        }
    }

    // â”€â”€ Yes/No with retries â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private fun yesNo(
        prompt: String, attempt: Int = 0,
        onYes: () -> Unit, onNo: () -> Unit,
        onExhaust: () -> Unit = { cancel("I couldn't hear you. Please try again.") }
    ) {
        listen({ text ->
            when {
                matchesYes(text)                       -> onYes()
                matchesNo(text) || matchesCancel(text) -> onNo()
                attempt + 1 >= MAX_RETRIES             -> onExhaust()
                else -> speak("$prompt You have ${rem(attempt)} left.") {
                    yesNo(prompt, attempt + 1, onYes, onNo, onExhaust)
                }
            }
        }, {
            if (attempt + 1 >= MAX_RETRIES) onExhaust()
            else speak("$prompt You have ${rem(attempt)} left.") {
                yesNo(prompt, attempt + 1, onYes, onNo, onExhaust)
            }
        })
    }

    // â”€â”€ Helpers â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private fun cancel(msg: String) { Log.d(TAG, "cancel: $msg"); speak(msg) { onCancel() } }

    private fun speak(text: String, onDone: () -> Unit) {
        Log.d(TAG, "SPEAK: $text"); tts.speakThenDo(text, onDone)
    }

    /** Use for command/yes-no listening — FREE_FORM model, standard gate. */
    private fun listen(onResult: (String) -> Unit, onError: () -> Unit) {
        Log.d(TAG, "LISTEN"); recognizer.listenDialog(onFinal = onResult, onError = onError)
    }

    /**
     * Use ONLY when asking for a place/destination name.
     * WEB_SEARCH model + online + shorter gate — fixes ERROR_NO_MATCH on
     * Filipino place names and proper nouns.
     */
    private fun listenSearch(onResult: (String) -> Unit, onError: () -> Unit) {
        Log.d(TAG, "LISTEN_SEARCH"); recognizer.listenSearch(onFinal = onResult, onError = onError)
    }

    private fun rem(attempt: Int): String {
        val left = MAX_RETRIES - (attempt + 1)
        return "$left ${if (left == 1) "try" else "tries"}"
    }

    private fun parseNumber(text: String): Int? {
        val t = text.lowercase().trim()
            .removePrefix("option ").removePrefix("number ")
            .removePrefix("pick ").removePrefix("choose ")
            .removePrefix("select ").removePrefix("the ")
            .trim()
        val map = mapOf(
            "one" to 1, "first" to 1, "1" to 1,
            "two" to 2, "second" to 2, "2" to 2,
            "three" to 3, "third" to 3, "3" to 3,
            "four" to 4, "fourth" to 4, "4" to 4,
            "five" to 5, "fifth" to 5, "5" to 5
        )
        map[t]?.let { return it }
        for ((w, n) in map) { if (t.contains(w)) return n }
        val digits = t.filter { it.isDigit() }
        if (digits.isNotEmpty()) {
            if (digits.all { it == digits[0] }) return digits[0].digitToInt().takeIf { it in 1..5 }
            return digits.toIntOrNull()?.takeIf { it in 1..5 }
        }
        return null
    }

    // â”€â”€ Matchers â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private fun matchesSearch(t: String): Boolean {
        val l = t.lowercase()
        return (l == "search" || l.contains("find") || l.contains("look for") || l.contains("search for"))
                && !matchesSearchAgain(t)
    }
    /** "saved route/location/place", "my route/location", "familiar", or bare "route" */
    private fun matchesSavedRoute(t: String): Boolean {
        // Normalise common typos first
        val l = t.lowercase()
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
        return listOf(
            "saved route", "saved routes", "saved location", "saved place",
            "my route", "my location", "familiar"
        ).any { l.contains(it) }
            || (l.contains("route") && !l.contains("search"))
            || (l.contains("saved") && !l.contains("search"))
    }

    private fun matchesYes(t: String): Boolean {
        val l = t.lowercase().trim()
        return listOf("yes", "yeah", "yep", "sure", "go ahead", "proceed",
            "confirm", "of course", "do it", "start").any { l == it || l.startsWith("$it ") || l.endsWith(" $it") }
                || l == "ok" || l == "okay"
    }

    private fun matchesNo(t: String): Boolean {
        val l = t.lowercase().trim()
        return listOf("no", "nope", "nah", "don't", "dont", "go back").any {
            l == it || l.startsWith("$it ") || l.endsWith(" $it")
        }
    }

    private fun matchesCancel(t: String): Boolean {
        val l = t.lowercase()
        return listOf("cancel", "stop", "exit", "quit", "never mind", "nevermind").any { l.contains(it) }
    }

    private fun matchesRepeat(t: String): Boolean {
        val l = t.lowercase()
        return listOf("repeat", "say again", "read again", "list again",
            "what were", "what was", "read that").any { l.contains(it) }
    }

    private fun matchesSearchAgain(t: String): Boolean {
        val l = t.lowercase()
        return listOf("search again", "try again", "different", "another place",
            "other place", "somewhere else", "something else").any { l.contains(it) }
    }

    /** Find a saved location by matching spoken words against the name. */
    private fun findLocation(spoken: String, locations: List<SavedLocation>): SavedLocation? {
        val sl = spoken.lowercase()
        // Exact name match first
        locations.firstOrNull { it.name.lowercase() == sl }?.let { return it }
        // Name contained in spoken text
        locations.firstOrNull { it.name.lowercase() in sl }?.let { return it }
        // Word overlap
        val spokenWords = sl.split(" ").filter { it.length > 2 }.toSet()
        return locations.firstOrNull {
            val nameWords = it.name.lowercase().split(" ").filter { w -> w.length > 2 }.toSet()
            spokenWords.intersect(nameWords).isNotEmpty() && nameWords.isNotEmpty()
        }
    }

    private fun numWord(n: Int) = when(n) {
        1 -> "one"; 2 -> "two"; 3 -> "three"; 4 -> "four"; 5 -> "five"
        6 -> "six"; 7 -> "seven"; 8 -> "eight"; 9 -> "nine"; 10 -> "ten"
        else -> n.toString()
    }
}
