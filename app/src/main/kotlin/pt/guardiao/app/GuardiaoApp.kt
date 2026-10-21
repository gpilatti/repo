package pt.guardiao.app

import android.app.Application
import pt.guardiao.app.data.Settings
import pt.guardiao.app.notify.AlertNotifier
import pt.guardiao.core.FraudAnalyzer
import pt.guardiao.core.alert.AlertDeduplicator

/** Holds the few app-wide objects. No dependency-injection framework: the graph is tiny. */
class GuardiaoApp : Application() {
    lateinit var settings: Settings
        private set
    lateinit var notifier: AlertNotifier
        private set

    /** On-device analysis only. No threat-intelligence or AI provider is configured in the MVP. */
    val analyzer = FraudAnalyzer()
    val deduplicator = AlertDeduplicator()

    override fun onCreate() {
        super.onCreate()
        settings = Settings(this)
        notifier = AlertNotifier(this)
        notifier.createChannel()
    }
}
