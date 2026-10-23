package pt.guardiao.core.alert

import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Decides when an unopened warning should pop up again, so that a warning that
 * arrived while the phone was locked is not lost in the notification list.
 *
 * Two moments trigger a reminder, both moments when the person is looking at the phone:
 * - the phone is unlocked: at most [maxUnlockReminders] times per warning, at
 *   least [minIntervalMillis] apart;
 * - the original WhatsApp or email notification is opened (the person is about
 *   to read the suspicious message): once per warning.
 *
 * Reminders stop when the person opens the warning, or after [ttlMillis].
 *
 * Memory only. The original notification is identified by a salted SHA-256 of
 * its system key (which can contain a phone number), never by the key itself.
 */
class AlertReminders(
    private val maxUnlockReminders: Int = 3,
    private val minIntervalMillis: Long = 10 * 60 * 1000L,
    private val ttlMillis: Long = 24 * 60 * 60 * 1000L,
    private val maxPending: Int = 20,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    class Pending internal constructor(
        val payload: AlertPayload,
        internal val sourceFingerprint: String?,
        internal val createdAt: Long,
    ) {
        /** The notification currently showing this warning, so a reminder can replace it. */
        var notificationId: Int? = null
        internal var unlockReminders = 0
        internal var openReminderUsed = false
        internal var lastShownAt = createdAt

        override fun toString() = "Pending(level=${payload.level}, unlockReminders=$unlockReminders)"
    }

    private val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
    private val pending = ArrayDeque<Pending>()

    /** Starts tracking a warning that has just been shown. */
    @Synchronized
    fun track(payload: AlertPayload, sourceKey: String?): Pending {
        expire()
        val entry = Pending(payload, sourceKey?.let { fingerprint(it) }, clock())
        pending.addLast(entry)
        while (pending.size > maxPending) pending.removeFirst()
        return entry
    }

    /** Warnings to show again because the phone was just unlocked. */
    @Synchronized
    fun onDeviceUnlocked(): List<Pending> {
        expire()
        val now = clock()
        return pending.filter { it.unlockReminders < maxUnlockReminders && now - it.lastShownAt >= minIntervalMillis }
            .onEach {
                it.unlockReminders++
                it.lastShownAt = now
            }
    }

    /** The warning to show again because its original notification was just opened, if any. */
    @Synchronized
    fun onSourceOpened(sourceKey: String): Pending? {
        expire()
        val key = fingerprint(sourceKey)
        val entry = pending.lastOrNull { it.sourceFingerprint == key && !it.openReminderUsed } ?: return null
        entry.openReminderUsed = true
        entry.lastShownAt = clock()
        return entry
    }

    /** The person opened a warning: stop all reminders. */
    @Synchronized
    fun acknowledgeAll() = pending.clear()

    @Synchronized
    fun pendingCount() = pending.size

    private fun expire() {
        val now = clock()
        pending.removeAll { now - it.createdAt > ttlMillis }
    }

    private fun fingerprint(key: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt)
        digest.update(key.toByteArray())
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
