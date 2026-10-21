package pt.guardiao.app.data

import android.content.Context

/**
 * Non-sensitive preferences only. Message content is never written here.
 * The optional trusted-person contact is entered by the user and stays on the
 * phone (backups are disabled for this app).
 */
class Settings(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var onboardingDone: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDING, value).apply()

    var monitoringPaused: Boolean
        get() = prefs.getBoolean(KEY_PAUSED, false)
        set(value) = prefs.edit().putBoolean(KEY_PAUSED, value).apply()

    var trustedName: String
        get() = prefs.getString(KEY_TRUSTED_NAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_TRUSTED_NAME, value.trim().take(60)).apply()

    var trustedNumber: String
        get() = prefs.getString(KEY_TRUSTED_NUMBER, "") ?: ""
        set(value) = prefs.edit().putString(KEY_TRUSTED_NUMBER, value.filter { it.isDigit() || it == '+' || it == ' ' }.take(20)).apply()

    private companion object {
        const val KEY_ONBOARDING = "onboarding_done"
        const val KEY_PAUSED = "monitoring_paused"
        const val KEY_TRUSTED_NAME = "trusted_name"
        const val KEY_TRUSTED_NUMBER = "trusted_number"
    }
}
