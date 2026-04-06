package com.example.lakbaylaya.voice

/**
 * State machine for the global voice command system.
 *
 * Transitions:
 *
 *  ┌─────────────────────────────────────────────────────────────────┐
 *  │                        IDLE                                     │
 *  │   App is in background / voice fully disabled                   │
 *  └──────────────────────┬──────────────────────────────────────────┘
 *                         │  startWakeListening()
 *                         ▼
 *  ┌─────────────────────────────────────────────────────────────────┐
 *  │                   WAKE_LISTENING                                │
 *  │   Short-burst SpeechRecognizer. Checking only for the wake word (e.g. "listen")  │
 *  └──────────────────────┬──────────────────────────────────────────┘
 *        wake word        │                  │  toggleListening()
 *        detected         │                  │
 *                         ▼                  ▼
 *  ┌─────────────────────────────────────────────────────────────────┐
 *  │                 COMMAND_LISTENING                               │
 *  │   Recognizer listens for full commands.                         │
 *  │   Wake listening is STOPPED here.                               │
 *  └──────────────────────┬──────────────────────────────────────────┘
 *      command executed   │
 *                         │
 *          ┌──────────────┘
 *          │ (if toggled listening is OFF) → back to WAKE_LISTENING
 *          │ (if toggled listening is ON)  → back to COMMAND_LISTENING
 *          ▼
 *  ┌─────────────────────────────────────────────────────────────────┐
 *  │               TOGGLED_LISTENING                                 │
 *  │   User explicitly toggled continuous command listening on.      │
 *  │   Wake listening is STOPPED.                                    │
 *  │   Re-listens for commands automatically after each execution.   │
 *  └─────────────────────────────────────────────────────────────────┘
 */
enum class WakeWordState {
    /** Voice system fully off. */
    IDLE,

    /** Short-burst listening for wake word "LakbayLaya" only. */
    WAKE_LISTENING,

    /**
     * Full command listening. Entered after wake word detection.
     * Wake listening is stopped while in this state.
     */
    COMMAND_LISTENING,

    /**
     * User-toggled continuous command mode.
     * Wake listening is stopped while in this state.
     * After each command, returns to COMMAND_LISTENING (re-listens).
     */
    TOGGLED_LISTENING
}
