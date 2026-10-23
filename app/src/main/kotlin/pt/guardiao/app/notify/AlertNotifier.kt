package pt.guardiao.app.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import pt.guardiao.app.MainActivity
import pt.guardiao.app.R
import pt.guardiao.app.data.RecentAlerts
import pt.guardiao.core.alert.AlertDispatcher
import pt.guardiao.core.alert.AlertPayload
import pt.guardiao.core.explain.PtPtCatalog
import pt.guardiao.core.model.MessageSource
import pt.guardiao.core.model.RiskLevel
import java.util.concurrent.atomic.AtomicInteger

/**
 * Shows warning notifications. They contain no message content: only the risk
 * headline. On the lock screen, a neutral public version is shown instead.
 */
class AlertNotifier(private val context: Context) : AlertDispatcher {

    fun createChannel() {
        if (Build.VERSION.SDK_INT < 26) return
        val channel = NotificationChannel(CHANNEL_ID, context.getString(R.string.channel_alerts), NotificationManager.IMPORTANCE_HIGH).apply {
            description = context.getString(R.string.channel_alerts_description)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PRIVATE
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    override fun dispatch(payload: AlertPayload) {
        show(payload, reminder = false)
    }

    /**
     * Shows a warning and returns its notification id, or null if notifications
     * are not allowed. A reminder repeats an unopened warning with a short note.
     */
    fun show(payload: AlertPayload, reminder: Boolean): Int? {
        if (!reminder) RecentAlerts.record(payload)
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return null

        val open = Intent(context, MainActivity::class.java)
            .setAction(MainActivity.ACTION_SHOW_ALERT)
            .putExtra(MainActivity.EXTRA_ALERT, payload.encode())
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val id = ids.incrementAndGet()
        val pending = PendingIntent.getActivity(context, id, open, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val source = context.getString(
            when (payload.source) {
                MessageSource.WHATSAPP_NOTIFICATION -> R.string.notif_source_whatsapp
                MessageSource.EMAIL_NOTIFICATION -> R.string.notif_source_email
                else -> R.string.notif_source_other
            },
        )
        val prefix = if (reminder) context.getString(R.string.notif_reminder) + " " else ""
        val publicVersion = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(context.getString(R.string.notif_public))
            .build()

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(PtPtCatalog.headline(payload.level))
            .setContentText("$prefix$source. ${context.getString(R.string.notif_open)}")
            .setStyle(NotificationCompat.BigTextStyle().bigText("$prefix$source. ${PtPtCatalog.summary(payload.level)} ${context.getString(R.string.notif_open)}"))
            .setPriority(if (payload.level == RiskLevel.CRITICAL) NotificationCompat.PRIORITY_MAX else NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(publicVersion)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        return try {
            NotificationManagerCompat.from(context).notify(id, notification)
            id
        } catch (_: SecurityException) {
            // Permission revoked between the check and the call: the in-app status will show it.
            null
        }
    }

    fun cancel(id: Int) {
        NotificationManagerCompat.from(context).cancel(id)
    }

    companion object {
        const val CHANNEL_ID = "scam_alerts"
        private val ids = AtomicInteger(1000)
    }
}
