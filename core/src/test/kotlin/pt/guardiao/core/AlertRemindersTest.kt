package pt.guardiao.core

import org.junit.Test
import pt.guardiao.core.alert.AlertPayload
import pt.guardiao.core.alert.AlertReminders
import pt.guardiao.core.model.Confidence
import pt.guardiao.core.model.MessageSource
import pt.guardiao.core.model.RiskLevel
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AlertRemindersTest {
    private val payload = AlertPayload(
        RiskLevel.CRITICAL, Confidence.HIGH, MessageSource.WHATSAPP_NOTIFICATION, emptyList(), emptyList(), emptySet(),
    )
    private var now = 0L
    private val minute = 60_000L
    private fun reminders() = AlertReminders(clock = { now })

    @Test fun `no reminder on an unlock right after the warning`() {
        val r = reminders()
        r.track(payload, "key")
        now = 2 * minute
        assertTrue(r.onDeviceUnlocked().isEmpty())
    }

    @Test fun `unlock reminders are spaced and capped at three`() {
        val r = reminders()
        r.track(payload, "key")
        var shown = 0
        repeat(10) {
            now += 11 * minute
            shown += r.onDeviceUnlocked().size
            now += 1 * minute
            shown += r.onDeviceUnlocked().size // too soon after the previous one
        }
        assertEquals(3, shown)
    }

    @Test fun `opening the original message reminds once`() {
        val r = reminders()
        r.track(payload, "0|com.whatsapp|1|chat@s.whatsapp.net|10123")
        now = minute
        assertNotNull(r.onSourceOpened("0|com.whatsapp|1|chat@s.whatsapp.net|10123"))
        assertNull(r.onSourceOpened("0|com.whatsapp|1|chat@s.whatsapp.net|10123"))
        assertNull(r.onSourceOpened("another notification"))
    }

    @Test fun `opening the warning stops all reminders`() {
        val r = reminders()
        r.track(payload, "key")
        r.acknowledgeAll()
        now = 60 * minute
        assertTrue(r.onDeviceUnlocked().isEmpty())
        assertNull(r.onSourceOpened("key"))
    }

    @Test fun `reminders expire after a day`() {
        val r = reminders()
        r.track(payload, "key")
        now = 25 * 60 * minute
        assertTrue(r.onDeviceUnlocked().isEmpty())
        assertEquals(0, r.pendingCount())
    }

    @Test fun `pending list is bounded and holds no raw key`() {
        val r = AlertReminders(maxPending = 5, clock = { now })
        val entries = (1..10).map { r.track(payload, "+351 900 000 00$it") }
        assertEquals(5, r.pendingCount())
        assertFalse(entries.last().toString().contains("900"))
    }
}
