package pt.guardiao.core

import org.junit.Test
import pt.guardiao.core.explain.EnglishCatalog
import pt.guardiao.core.explain.ExplanationGenerator
import pt.guardiao.core.explain.PtPtCatalog
import pt.guardiao.core.model.FindingKind
import pt.guardiao.core.model.RecommendedAction
import pt.guardiao.core.model.RiskLevel
import pt.guardiao.core.model.SignalType
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExplanationTest {
    private val catalogs = listOf(PtPtCatalog, EnglishCatalog)

    @Test fun `every signal and action has text in every language`() {
        for (c in catalogs) {
            SignalType.entries.forEach { assertTrue(c.reason(it, FindingKind.OBSERVED).isNotBlank()) }
            RecommendedAction.entries.forEach { assertTrue(c.action(it).isNotBlank()) }
            RiskLevel.entries.forEach { assertTrue(c.headline(it).isNotBlank()) }
        }
    }

    @Test fun `wording is short and never promises safety`() {
        for (c in catalogs) {
            val texts = SignalType.entries.map { c.reason(it, FindingKind.OBSERVED) } +
                RecommendedAction.entries.map { c.action(it) } +
                RiskLevel.entries.flatMap { listOf(c.headline(it), c.summary(it)) }
            texts.forEach { assertTrue(it.length <= 140, "too long: $it") }
            val all = texts.joinToString(" ").lowercase()
            listOf("é segura", "está seguro", "totalmente protegido", "is safe", "fully protected", "100%").forEach {
                assertFalse(all.contains(it), it)
            }
        }
    }

    @Test fun `no level relies on colour words alone`() {
        assertTrue(PtPtCatalog.headline(RiskLevel.CRITICAL).startsWith("PARE"))
    }

    @Test fun `never tells the user to call a number from the message`() {
        val call = PtPtCatalog.action(RecommendedAction.CALL_KNOWN_NUMBER)
        assertTrue(call.contains("já tinha guardado"))
        assertTrue(call.contains("Não use o número desta mensagem"))
    }

    @Test fun `inferred findings are labelled as estimates`() {
        assertTrue(PtPtCatalog.reason(SignalType.URGENCY, FindingKind.INFERRED).contains("estimado"))
    }

    @Test fun `reasons are limited to three and no warning has none`() {
        val r = analyse("CGD: a sua conta será bloqueada hoje. Indique o PIN e o código em https://cgd-x.example, não ligue ao banco.")
        val e = ExplanationGenerator().explain(r)
        assertEquals(3, e.reasons.size)
        val none = ExplanationGenerator().explain(analyse("Olá, jantar às 8?"))
        assertTrue(none.reasons.isEmpty())
        assertTrue(none.summary.contains("não garante"))
    }
}
