package pt.guardiao.core.privacy

/**
 * The only logging channel allowed in analysis code. It accepts an event code
 * and numbers, never free text, so message content cannot reach logs by mistake.
 */
enum class LogEvent {
    ANALYSIS_COMPLETED,
    NOTIFICATION_IGNORED_UNSUPPORTED_APP,
    NOTIFICATION_IGNORED_DUPLICATE,
    NOTIFICATION_IGNORED_EMPTY,
    ALERT_SHOWN,
    AI_FAILED,
    THREAT_INTEL_FAILED,
}

fun interface SafeLogger {
    fun log(event: LogEvent, vararg numbers: Int)

    companion object {
        val NONE = SafeLogger { _, _ -> }
    }
}
