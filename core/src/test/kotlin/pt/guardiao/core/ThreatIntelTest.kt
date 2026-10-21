package pt.guardiao.core

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Test
import pt.guardiao.core.intel.LocalBlocklist
import pt.guardiao.core.intel.Reputation
import pt.guardiao.core.intel.ThreatIntelligenceProvider
import pt.guardiao.core.model.Limitation
import pt.guardiao.core.model.RiskLevel
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ThreatIntelTest {
    @Test fun `listed hosts are critical`() = runBlocking {
        val analyzer = FraudAnalyzer(threatIntelligence = LocalBlocklist(listOf("bad.example")))
        assertEquals(RiskLevel.CRITICAL, analyzer.analyse(msg("veja https://login.bad.example/x")).level)
    }

    @Test fun `provider receives host names only`() = runBlocking {
        val seen = mutableListOf<String>()
        val spy = object : ThreatIntelligenceProvider {
            override val sendsDataOffDevice = true
            override suspend fun reputationOf(host: String): Reputation { seen += host; return Reputation.UNKNOWN }
        }
        FraudAnalyzer(threatIntelligence = spy).analyse(msg("https://site.example/reset?token=SECRET123&email=a@b.pt"))
        assertEquals(listOf("site.example"), seen)
    }

    @Test fun `not listed is not treated as safe and failures degrade`() = runBlocking {
        val failing = object : ThreatIntelligenceProvider {
            override val sendsDataOffDevice = true
            override suspend fun reputationOf(host: String): Reputation = throw java.io.IOException()
        }
        val r = FraudAnalyzer(threatIntelligence = failing).analyse(msg("https://cgd-login.example"))
        assertTrue(r.level >= RiskLevel.CAUTION)
        assertTrue(Limitation.LINK_REPUTATION_NOT_CHECKED in r.limitations)
    }

    @Test fun `slow providers time out`() = runBlocking {
        val slow = object : ThreatIntelligenceProvider {
            override val sendsDataOffDevice = true
            override suspend fun reputationOf(host: String): Reputation { delay(5_000); return Reputation.KNOWN_MALICIOUS }
        }
        val r = FraudAnalyzer(threatIntelligence = slow, threatIntelTimeoutMillis = 50).analyse(msg("https://x.example"))
        assertFalse(r.level == RiskLevel.CRITICAL)
        assertTrue(Limitation.LINK_REPUTATION_NOT_CHECKED in r.limitations)
    }
}
