package pt.guardiao.app

import android.app.Application
import pt.guardiao.app.data.Settings
import pt.guardiao.app.notify.AlertNotifier
import pt.guardiao.core.FraudAnalyzer
import pt.guardiao.core.alert.AlertDeduplicator
import pt.guardiao.core.alert.AlertReminders

/** Holds the few app-wide objects. No dependency-injection framework: the graph is tiny. */
class GuardiaoApp : Application() {
    lateinit var settings: Settings
        private set
    lateinit var notifier: AlertNotifier
        private set

    /** On-device analysis only. No threat-intelligence or AI provider is configured in the MVP. */
    val analyzer = FraudAnalyzer()
    val deduplicator = AlertDeduplicator()

    /** Unopened warnings that should pop up again when the person looks at the phone. */
    val reminders = AlertReminders()

    override fun onCreate() {
        super.onCreate()
        settings = Settings(this)
        notifier = AlertNotifier(this)
        notifier.createChannel()
    }
}
