package pt.guardiao.app.data

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings as AndroidSettings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import pt.guardiao.app.notify.MessageNotificationListener

/** What the app can actually do right now. The UI never claims more than this. */
data class ProtectionStatus(
    val notificationAccessGranted: Boolean,
    val canPostNotifications: Boolean,
    val paused: Boolean,
) {
    val automaticActive: Boolean get() = notificationAccessGranted && canPostNotifications && !paused

    companion object {
        fun read(context: Context, settings: Settings): ProtectionStatus {
            val access = NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
            val permission = Build.VERSION.SDK_INT < 33 ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            val enabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
            return ProtectionStatus(access, permission && enabled, settings.monitoringPaused)
        }

        /** Opens Android's notification-access page, for this app directly where supported. */
        fun notificationAccessIntent(context: Context): Intent {
            if (Build.VERSION.SDK_INT >= 30) {
                val component = ComponentName(context, MessageNotificationListener::class.java)
                val direct = Intent(AndroidSettings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
                    .putExtra(AndroidSettings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, component.flattenToString())
                if (direct.resolveActivity(context.packageManager) != null) return direct
            }
            return Intent(AndroidSettings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        }

        fun appNotificationSettingsIntent(context: Context): Intent =
            Intent(AndroidSettings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(AndroidSettings.EXTRA_APP_PACKAGE, context.packageName)
    }
}
