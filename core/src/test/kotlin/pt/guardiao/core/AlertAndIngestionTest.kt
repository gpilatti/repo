package pt.guardiao.core

import org.junit.Test
import pt.guardiao.core.alert.AlertDeduplicator
import pt.guardiao.core.alert.AlertThreshold
import pt.guardiao.core.ingest.NotificationAdapter
import pt.guardiao.core.ingest.NotificationFields
import pt.guardiao.core.ingest.SharedTextAdapter
import pt.guardiao.core.model.Completeness
import pt.guardiao.core.model.MessageSource
import pt.guardiao.core.model.RiskLevel
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AlertAndIngestionTest {

    @Test fun `deduplicator suppresses repeats within the window`() {
        var now = 0L
        val d = AlertDeduplicator(ttlMillis = 1000, clock = { now })
        val m = msg("Olá mãe, mudei de número", MessageSource.WHATSAPP_NOTIFICATION, sender = "+351 900")
        assertTrue(d.firstTime(m))
        assertFalse(d.firstTime(msg("olá  MÃE, mudei de número ", MessageSource.WHATSAPP_NOTIFICATION, sender = "+351 900")))
        now = 2000
        assertTrue(d.firstTime(m))
    }

    @Test fun `deduplicator is bounded`() {
        val d = AlertDeduplicator(maxEntries = 10)
        repeat(50) { d.firstTime(msg("mensagem $it")) }
        assertEquals(10, d.size())
    }

    @Test fun `different senders are different events`() {
        val d = AlertDeduplicator()
        assertTrue(d.firstTime(msg("olá", sender = "A")))
        assertTrue(d.firstTime(msg("olá", sender = "B")))
    }

    @Test fun `only high and critical trigger automatic alerts by default`() {
        assertFalse(AlertThreshold.shouldAlert(RiskLevel.CAUTION))
        assertTrue(AlertThreshold.shouldAlert(RiskLevel.HIGH))
        assertTrue(AlertThreshold.shouldAlert(RiskLevel.CRITICAL))
    }

    @Test fun `unsupported apps are ignored`() {
        assertNull(NotificationAdapter.toMessage(NotificationFields("com.example.game", "Oferta", "ganhou!", null)))
    }

    @Test fun `group summaries and ongoing notifications are ignored`() {
        assertNull(NotificationAdapter.toMessage(NotificationFields("com.whatsapp", "WhatsApp", "3 mensagens", null, isGroupSummary = true)))
        assertNull(NotificationAdapter.toMessage(NotificationFields("com.whatsapp", "A ligar", "chamada", null, isOngoing = true)))
    }

    @Test fun `whatsapp uses the newest messaging style message`() {
        val m = NotificationAdapter.toMessage(
            NotificationFields(
                "com.whatsapp", "+351 900 000 000", "2 mensagens", null,
                messages = listOf("+351 900 000 000" to "Olá mãe", "+351 900 000 000" to "mudei de número"),
                isGroupConversation = false,
            ),
        )
        assertNotNull(m)
        assertEquals("mudei de número", m.body)
        assertEquals(MessageSource.WHATSAPP_NOTIFICATION, m.source)
    }

    @Test fun `hidden previews are marked redacted and not analysed as text`() {
        listOf("Nova mensagem", "2 novas mensagens", "Sensitive notification content hidden").forEach { text ->
            val m = NotificationAdapter.toMessage(NotificationFields("com.whatsapp", "WhatsApp", text, null))
            assertNotNull(m, text)
            assertEquals(Completeness.REDACTED, m.completeness, text)
            assertEquals("", m.body)
            val r = analyse(m)
            assertEquals(RiskLevel.NO_WARNING, r.level)
            assertTrue(r.isIncomplete)
        }
    }

    @Test fun `email notifications are treated as truncated previews`() {
        val m = NotificationAdapter.toMessage(
            NotificationFields("com.google.android.gm", "CGD", "Acesso bloqueado", "Acesso bloqueado\nReative em https://cgd-x.example"),
        )
        assertNotNull(m)
        assertEquals(Completeness.TRUNCATED, m.completeness)
        assertEquals(MessageSource.EMAIL_NOTIFICATION, m.source)
        assertTrue(analyse(m).isIncomplete)
    }

    @Test fun `empty notifications are ignored`() {
        assertNull(NotificationAdapter.toMessage(NotificationFields("com.whatsapp", "A", "   ", null)))
    }

    @Test fun `shared text is bounded`() {
        assertNull(SharedTextAdapter.toMessage("  "))
        val long = SharedTextAdapter.toMessage("a".repeat(30_000))!!
        assertEquals(Completeness.TRUNCATED, long.completeness)
        assertEquals(SharedTextAdapter.MAX_SHARED_CHARS, long.body.length)
    }
}
