package pt.guardiao.app.notify

import android.app.Notification
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import pt.guardiao.app.GuardiaoApp
import pt.guardiao.core.alert.AlertPayload
import pt.guardiao.core.alert.AlertReminders
import pt.guardiao.core.alert.AlertThreshold
import pt.guardiao.core.ingest.NotificationAdapter
import pt.guardiao.core.ingest.NotificationFields
import pt.guardiao.core.ingest.SupportedApps

/**
 * Receives notifications after the user enables notification access in Android
 * settings. For each one:
 * 1. ignore it unless it comes from a supported app (checked before reading content);
 * 2. copy the visible text into memory, analyse it on the device, discard it;
 * 3. if the risk is HIGH or CRITICAL, show a warning that contains no content;
 * 4. until the warning is opened, show it again when the phone is unlocked and
 *    when the original message is opened (see [AlertReminders]).
 *
 * Never logs, stores or forwards notification content, and never dismisses or
 * changes the original notification.
 */
class MessageNotificationListener : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val app get() = application as GuardiaoApp
    private var unlockReceiverRegistered = false

    private val unlockReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != Intent.ACTION_USER_PRESENT || app.settings.monitoringPaused) return
            app.reminders.onDeviceUnlocked().forEach { remind(it) }
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        if (!unlockReceiverRegistered) {
            // The listener stays bound while access is on, so it can hear unlocks without a background service.
            ContextCompat.registerReceiver(
                this, unlockReceiver, IntentFilter(Intent.ACTION_USER_PRESENT), ContextCompat.RECEIVER_NOT_EXPORTED,
            )
            unlockReceiverRegistered = true
        }
    }

    override fun onListenerDisconnected() {
        unregisterUnlockReceiver()
        super.onListenerDisconnected()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (app.settings.monitoringPaused) return
        if (!SupportedApps.isSupported(sbn.packageName)) return

        val fields = runCatching { readFields(sbn) }.getOrNull() ?: return
        val message = NotificationAdapter.toMessage(fields) ?: return
        if (!app.deduplicator.firstTime(message)) return

        scope.launch {
            val assessment = app.analyzer.analyse(message)
            if (AlertThreshold.shouldAlert(assessment.level)) {
                val payload = AlertPayload.from(message.source, assessment)
                val entry = app.reminders.track(payload, sbn.key)
                entry.notificationId = app.notifier.show(payload, reminder = false)
            }
        }
    }

    /**
     * The person opened the original WhatsApp or email notification (or the chat,
     * which makes the app clear it): they are about to read the message, so an
     * unopened warning pops up again now.
     */
    override fun onNotificationRemoved(sbn: StatusBarNotification, rankingMap: RankingMap, reason: Int) {
        if (reason != REASON_CLICK && reason != REASON_APP_CANCEL) return
        if (app.settings.monitoringPaused || !SupportedApps.isSupported(sbn.packageName)) return
        app.reminders.onSourceOpened(sbn.key)?.let { remind(it) }
    }

    private fun remind(entry: AlertReminders.Pending) {
        entry.notificationId?.let { app.notifier.cancel(it) }
        entry.notificationId = app.notifier.show(entry.payload, reminder = true)
    }

    private fun unregisterUnlockReceiver() {
        if (unlockReceiverRegistered) {
            runCatching { unregisterReceiver(unlockReceiver) }
            unlockReceiverRegistered = false
        }
    }

    override fun onDestroy() {
        unregisterUnlockReceiver()
        scope.cancel()
        super.onDestroy()
    }

    private fun readFields(sbn: StatusBarNotification): NotificationFields {
        val n = sbn.notification
        val extras: Bundle = n.extras
        val style = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(n)
        return NotificationFields(
            packageName = sbn.packageName,
            title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString(),
            text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString(),
            bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString(),
            textLines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)?.map { it.toString() } ?: emptyList(),
            // Only the newest incoming message is analysed; earlier ones were seen before.
            // A message without a person was written by the phone's owner and is skipped.
            messages = style?.messages?.lastOrNull { it.person != null }?.let { listOf(it) }?.mapNotNull { m ->
                m.text?.toString()?.let { text -> m.person?.name?.toString() to text }
            } ?: emptyList(),
            conversationTitle = style?.conversationTitle?.toString(),
            isGroupConversation = style?.isGroupConversation,
            isGroupSummary = (n.flags and Notification.FLAG_GROUP_SUMMARY) != 0,
            isOngoing = sbn.isOngoing,
            postedAtMillis = sbn.postTime,
        )
    }
}
