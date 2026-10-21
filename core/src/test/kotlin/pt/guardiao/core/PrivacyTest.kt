package pt.guardiao.core

import kotlinx.coroutines.runBlocking
import org.junit.Test
import pt.guardiao.core.alert.AlertPayload
import pt.guardiao.core.explain.ExplanationGenerator
import pt.guardiao.core.ingest.NotificationFields
import pt.guardiao.core.model.MessageSource
import pt.guardiao.core.privacy.LogEvent
import pt.guardiao.core.privacy.SafeLogger
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Message content must not leak through toString, logs, alert payloads or explanations. */
class PrivacyTest {
    private val secret = "Mãe, sou eu, transfere 480€ para PT50 0000 0000 0000 0000 0000 0, código 482913, ana@example.com"

    @Test fun `toString never contains content`() = runBlocking {
        val m = msg(secret, MessageSource.WHATSAPP_NOTIFICATION, sender = "+351 900 000 000")
        val r = FraudAnalyzer().analyse(m)
        val dumps = listOf(m.toString(), r.toString(), r.findings.toString(), pt.guardiao.core.text.NormalizedText.of(secret).toString())
        for (d in dumps) {
            assertFalse(d.contains("480"), d)
            assertFalse(d.contains("PT50"), d)
            assertFalse(d.contains("example.com"), d)
            assertFalse(d.contains("900 000"), d)
        }
        assertFalse(NotificationFields("com.whatsapp", "+351 900", secret, null).toString().contains("480"))
    }

    @Test fun `logger receives only codes and numbers`() = runBlocking {
        val logged = mutableListOf<Pair<LogEvent, List<Int>>>()
        FraudAnalyzer(logger = SafeLogger { e, n -> logged += e to n.toList() }).analyse(msg(secret))
        assertTrue(logged.isNotEmpty())
        // The API makes free text impossible; this asserts the events are what we expect.
        assertTrue(logged.all { it.first == LogEvent.ANALYSIS_COMPLETED })
    }

    @Test fun `alert payload carries no content and round-trips`() = runBlocking {
        val r = FraudAnalyzer().analyse(msg(secret))
        val encoded = AlertPayload.from(MessageSource.USER_SHARED, r).encode()
        listOf("480", "PT50", "482913", "example", "Mãe", "sou eu").forEach { assertFalse(encoded.contains(it), it) }
        assertTrue(encoded.matches(Regex("[A-Z_:,|]*")))
        val decoded = AlertPayload.decode(encoded)!!
        assertTrue(decoded.level == r.level)
    }

    @Test fun `malformed payloads are rejected`() {
        listOf(null, "", "garbage", "HIGH|X|Y|||", "CRITICAL|HIGH|SAMPLE|NOT_A_SIGNAL:OBSERVED||").forEach {
            assertTrue(AlertPayload.decode(it) == null, "$it")
        }
    }

    @Test fun `explanations are built from codes and contain no message text`() = runBlocking {
        val r = FraudAnalyzer().analyse(msg(secret))
        val e = ExplanationGenerator().explain(r)
        val all = (listOf(e.headline, e.summary, e.confidenceNote) + e.reasons + e.actions + e.limitations).joinToString(" ")
        listOf("480", "PT50", "482913", "example.com").forEach { assertFalse(all.contains(it), it) }
    }

    @Test fun `core code has no logging, file or network calls`() {
        val src = java.io.File("src/main/kotlin")
        val forbidden = listOf("println(", "System.out", "System.err", "java.util.logging", "FileWriter", "FileOutputStream",
            "java.net.URL(", "HttpURLConnection", "Socket(", "printStackTrace")
        val offenders = src.walkTopDown().filter { it.isFile && it.extension == "kt" }.flatMap { f ->
            val text = f.readText()
            forbidden.filter { text.contains(it) }.map { "${f.name}: $it" }
        }.toList()
        assertTrue(offenders.isEmpty(), offenders.joinToString())
    }
}
