package com.example.lakbaylaya.ui.screens.map.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Utility class for debouncing user input
 * Prevents excessive API calls or computations during rapid user input
 */
class Debouncer(
    private val delayMillis: Long = 300L,
    private val coroutineScope: CoroutineScope
) {
    private var debounceJob: Job? = null

    /**
     * Debounces the execution of an action
     * Cancels previous action if called again within the delay period
     *
     * @param action The action to execute after the delay
     */
    fun debounce(action: () -> Unit) {
        debounceJob?.cancel()
        debounceJob = coroutineScope.launch {
            delay(delayMillis)
            action()
        }
    }

    /**
     * Cancels any pending debounced action
     */
    fun cancel() {
        debounceJob?.cancel()
    }
}
