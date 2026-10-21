package pt.guardiao.app.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import pt.guardiao.core.alert.AlertPayload

/**
 * The most recent automatic alert, in memory only: it disappears when the app
 * process ends. Holds codes (level, signal types), never message content.
 * A persistent history is deliberately not implemented by default.
 */
object RecentAlerts {
    data class Entry(val payload: AlertPayload, val atMillis: Long)

    private val _last = MutableStateFlow<Entry?>(null)
    val last: StateFlow<Entry?> = _last

    fun record(payload: AlertPayload) {
        _last.value = Entry(payload, System.currentTimeMillis())
    }
}
