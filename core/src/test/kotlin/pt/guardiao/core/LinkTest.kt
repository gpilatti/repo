package pt.guardiao.core

import org.junit.Test
import pt.guardiao.core.links.LinkParser
import pt.guardiao.core.model.SignalType
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LinkTest {
    private fun signals(text: String) = analyse(text).signalTypes

    @Test fun `official domains are not lookalikes`() {
        assertFalse(SignalType.LOOKALIKE_DOMAIN in signals("Veja https://www.cgd.pt/Particulares"))
        assertFalse(SignalType.LOOKALIKE_DOMAIN in signals("https://www.portaldasfinancas.gov.pt/at/"))
        assertFalse(SignalType.LOOKALIKE_DOMAIN in signals("https://www.mbway.pt/seguranca"))
    }

    @Test fun `brand names on other domains are lookalikes`() {
        assertTrue(SignalType.LOOKALIKE_DOMAIN in signals("https://cgd-seguranca.example/login"))
        assertTrue(SignalType.LOOKALIKE_DOMAIN in signals("https://ctt.pt.entregas.example/pagar"))
        assertTrue(SignalType.LOOKALIKE_DOMAIN in signals("https://rnbway-apoio.example"))
        assertTrue(SignalType.LOOKALIKE_DOMAIN in signals("https://cgd.pt@evil.example/"))
    }

    @Test fun `substring brand matches are avoided for exact-only brands`() {
        assertFalse(SignalType.LOOKALIKE_DOMAIN in signals("Receita em https://pineapple-recipes.example/bolo"))
    }

    @Test fun `bare domains are found`() {
        assertTrue(SignalType.LOOKALIKE_DOMAIN in signals("Pague em ctt-taxas.com hoje"))
    }

    @Test fun `structural link signals`() {
        assertTrue(SignalType.IP_ADDRESS_LINK in signals("http://192.0.2.1/login"))
        assertTrue(SignalType.PUNYCODE_OR_MIXED_SCRIPT_DOMAIN in signals("https://xn--80ak6aa92e.example"))
        assertTrue(SignalType.LINK_SHORTENER in signals("veja bit.ly/abc123"))
        assertTrue(SignalType.UNUSUAL_DOMAIN_ENDING in signals("https://loja.top/x"))
        assertTrue(SignalType.INSECURE_LINK in signals("http://example.org/a"))
    }

    @Test fun `malformed links do not crash`() {
        assertNull(LinkParser.parse("http://[::"))
        assertNull(LinkParser.parse(""))
        assertNull(LinkParser.parse("https://..."))
        val r = analyse("abra http://[malformed e https:// e www.")
        assertTrue(r.level >= pt.guardiao.core.model.RiskLevel.NO_WARNING)
    }

    @Test fun `registrable domain handles two-label suffixes`() {
        assertEquals("portaldasfinancas.gov.pt", LinkParser.registrableDomain("www.portaldasfinancas.gov.pt"))
        assertEquals("example.com", LinkParser.registrableDomain("a.b.example.com"))
    }

    @Test fun `email link text mismatch`() {
        val m = pt.guardiao.core.model.IncomingMessage(
            pt.guardiao.core.model.MessageSource.USER_SHARED, "Aceda à sua conta",
            links = listOf(pt.guardiao.core.model.LinkRef("www.bancobpi.pt", "https://bpi-login.example/x")),
        )
        assertTrue(SignalType.LINK_TEXT_MISMATCH in analyse(m).signalTypes)
    }
}
