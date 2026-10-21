package pt.guardiao.core

import org.junit.Test
import pt.guardiao.core.context.ConversationContext
import pt.guardiao.core.model.Completeness
import pt.guardiao.core.model.Confidence
import pt.guardiao.core.model.Finding
import pt.guardiao.core.model.FindingKind
import pt.guardiao.core.model.Limitation
import pt.guardiao.core.model.Provenance
import pt.guardiao.core.model.RecommendedAction
import pt.guardiao.core.model.RiskLevel
import pt.guardiao.core.model.SignalType
import pt.guardiao.core.policy.RiskPolicy
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PolicyTest {
    private val policy = RiskPolicy()

    private fun f(type: SignalType, provenance: Provenance = Provenance.RULE) =
        Finding(type, Confidence.MEDIUM, provenance, FindingKind.OBSERVED, "test.${type.name}")

    private fun level(vararg types: SignalType, context: ConversationContext = ConversationContext.UNKNOWN) =
        policy.assess(types.map { f(it) }, context, Completeness.COMPLETE, hasLinks = false).level

    @Test fun `single weak tactics stay low`() {
        assertEquals(RiskLevel.NO_WARNING, level(SignalType.URGENCY))
        assertEquals(RiskLevel.NO_WARNING, level(SignalType.FAMILY_ADDRESS))
        assertEquals(RiskLevel.NO_WARNING, level(SignalType.BANK_CLAIM))
        assertEquals(RiskLevel.NO_WARNING, level(SignalType.PAYMENT_DEMAND))
    }

    @Test fun `money from a known contact without pressure is not a warning`() {
        val known = ConversationContext(senderSavedAsContact = true)
        assertEquals(RiskLevel.NO_WARNING, level(SignalType.MONEY_TRANSFER_REQUEST, SignalType.FAMILY_ADDRESS, context = known))
        assertEquals(RiskLevel.CAUTION, level(SignalType.MONEY_TRANSFER_REQUEST))
        assertEquals(RiskLevel.HIGH, level(SignalType.MONEY_TRANSFER_REQUEST, SignalType.URGENCY, context = known))
    }

    @Test fun `classic combinations are critical`() {
        assertEquals(RiskLevel.CRITICAL, level(SignalType.NEW_NUMBER_CLAIM, SignalType.MONEY_TRANSFER_REQUEST))
        assertEquals(RiskLevel.CRITICAL, level(SignalType.MONEY_TRANSFER_REQUEST, SignalType.SECRECY_REQUEST))
        assertEquals(RiskLevel.CRITICAL, level(SignalType.CREDENTIAL_REQUEST, SignalType.BANK_CLAIM))
        assertEquals(RiskLevel.CRITICAL, level(SignalType.REMOTE_ACCESS_REQUEST, SignalType.TECH_SUPPORT_CLAIM))
    }

    @Test fun `ai findings alone cannot raise a warning`() {
        val ai = listOf(f(SignalType.CREDENTIAL_REQUEST, Provenance.AI_MODEL), f(SignalType.URGENCY, Provenance.AI_MODEL))
        assertEquals(RiskLevel.NO_WARNING, policy.assess(ai, ConversationContext.UNKNOWN, Completeness.COMPLETE, false).level)
    }

    @Test fun `ai findings can support a rule of another tactic`() {
        val findings = listOf(f(SignalType.BANK_CLAIM), f(SignalType.CREDENTIAL_REQUEST, Provenance.AI_MODEL))
        assertEquals(RiskLevel.CRITICAL, policy.assess(findings, ConversationContext.UNKNOWN, Completeness.COMPLETE, false).level)
    }

    @Test fun `truncated content is flagged and lowers confidence`() {
        val r = policy.assess(
            listOf(f(SignalType.CREDENTIAL_REQUEST), f(SignalType.BANK_CLAIM), f(SignalType.URGENCY)),
            ConversationContext.UNKNOWN, Completeness.TRUNCATED, false,
        )
        assertTrue(r.isIncomplete)
        assertTrue(Limitation.CONTENT_TRUNCATED in r.limitations)
        assertTrue(r.confidence != Confidence.HIGH)
    }

    @Test fun `no warning still recommends verification when in doubt and never claims safety`() {
        val r = policy.assess(emptyList(), ConversationContext.UNKNOWN, Completeness.REDACTED, false)
        assertEquals(RiskLevel.NO_WARNING, r.level)
        assertEquals(Confidence.LOW, r.confidence)
        assertEquals(listOf(RecommendedAction.WHEN_IN_DOUBT_VERIFY), r.actions)
    }

    @Test fun `high risk money cases include what to do if already paid`() {
        val r = policy.assess(
            listOf(f(SignalType.NEW_NUMBER_CLAIM), f(SignalType.MONEY_TRANSFER_REQUEST)),
            ConversationContext.UNKNOWN, Completeness.COMPLETE, false,
        )
        assertTrue(RecommendedAction.IF_ALREADY_PAID_CALL_BANK in r.actions)
        assertTrue(RecommendedAction.CALL_KNOWN_NUMBER in r.actions)
        assertFalse(r.actions.size > 4)
    }

    @Test fun `history is never assumed`() {
        val r = policy.assess(emptyList(), ConversationContext.UNKNOWN, Completeness.COMPLETE, false)
        assertTrue(Limitation.NO_CONVERSATION_HISTORY in r.limitations)
    }
}
