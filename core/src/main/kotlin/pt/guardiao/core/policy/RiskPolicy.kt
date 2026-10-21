package pt.guardiao.core.policy

import pt.guardiao.core.context.ConversationContext
import pt.guardiao.core.detect.Lexicon
import pt.guardiao.core.model.Completeness
import pt.guardiao.core.model.Confidence
import pt.guardiao.core.model.Finding
import pt.guardiao.core.model.Limitation
import pt.guardiao.core.model.Provenance
import pt.guardiao.core.model.RecommendedAction
import pt.guardiao.core.model.RecommendedAction.CALL_KNOWN_NUMBER
import pt.guardiao.core.model.RecommendedAction.CONTACT_ORGANISATION_OFFICIAL_CHANNEL
import pt.guardiao.core.model.RecommendedAction.DO_NOT_INSTALL_APPS
import pt.guardiao.core.model.RecommendedAction.DO_NOT_OPEN_ATTACHMENT
import pt.guardiao.core.model.RecommendedAction.DO_NOT_OPEN_LINK
import pt.guardiao.core.model.RecommendedAction.DO_NOT_SEND_MONEY
import pt.guardiao.core.model.RecommendedAction.IF_ALREADY_PAID_CALL_BANK
import pt.guardiao.core.model.RecommendedAction.NEVER_SHARE_CODES
import pt.guardiao.core.model.RecommendedAction.TALK_TO_TRUSTED_PERSON
import pt.guardiao.core.model.RecommendedAction.WHEN_IN_DOUBT_VERIFY
import pt.guardiao.core.model.RiskAssessment
import pt.guardiao.core.model.RiskLevel
import pt.guardiao.core.model.SignalType
import pt.guardiao.core.model.SignalType.*

/**
 * Turns findings into a risk level using explicit combination rules.
 *
 * Design choices (see docs/TESTING_AND_EVALUATION.md):
 * - Levels come from combinations of tactics, not from counting keywords. Urgency
 *   or money vocabulary alone never produces a warning above CAUTION.
 * - HIGH and CRITICAL need at least two independent tactics, or one tactic that is
 *   close to unique to fraud (asking for a code, remote access, gift cards, a
 *   lookalike link to a bank).
 * - AI findings can support a rule but are only counted if a deterministic finding
 *   of a different tactic is also present, so a model alone cannot raise a
 *   message above CAUTION.
 * - Nothing here can label a message "safe". The lowest level is "no warning".
 */
class RiskPolicy {

    private class Facts(findings: List<Finding>, context: ConversationContext) {
        private val ruleIds = findings.filter { it.provenance != Provenance.AI_MODEL }.map { it.ruleId }.toSet()
        private val ruleTypes = findings.filter { it.provenance != Provenance.AI_MODEL }.map { it.type }.toSet()
        private val aiTypes = findings.filter { it.provenance == Provenance.AI_MODEL }.map { it.type }.toSet()
        private val ruleTactics = ruleTypes.map { it.tactic }.toSet()

        // An AI signal counts only when rules already found some other tactic.
        private val types: Set<SignalType> = ruleTypes + aiTypes.filter { ai -> ruleTactics.any { it != ai.tactic } }

        fun has(vararg t: SignalType) = t.any { it in types }

        val familyClaim = has(FAMILY_IDENTITY_CLAIM) || (has(FAMILY_ADDRESS) && has(NEW_NUMBER_CLAIM))
        val familyAddress = has(FAMILY_ADDRESS, FAMILY_IDENTITY_CLAIM)
        val newNumber = has(NEW_NUMBER_CLAIM)
        val organisation = has(BANK_CLAIM, AUTHORITY_CLAIM, COURIER_CLAIM, TECH_SUPPORT_CLAIM)
        val urgency = has(URGENCY, THREAT, DISTRESS_STORY)
        val threat = has(THREAT)
        val secrecy = has(SECRECY_REQUEST)
        val channelSwitch = has(CHANNEL_SWITCH)
        val moneyRequest = has(MONEY_TRANSFER_REQUEST) || (has(BANK_ACCOUNT_NUMBER) && (familyAddress || urgency))
        val paymentDemand = has(PAYMENT_DEMAND)
        val unusualPayment = has(UNUSUAL_PAYMENT_METHOD)
        val anyMoney = moneyRequest || paymentDemand || unusualPayment
        val credential = has(CREDENTIAL_REQUEST)
        val codeToSender = Lexicon.CODE_TO_SENDER_ID in ruleIds
        val verify = has(ACCOUNT_VERIFICATION_REQUEST)
        val remote = has(REMOTE_ACCESS_REQUEST)
        val lure = has(PRIZE_REFUND_OR_INVESTMENT)
        val maliciousLink = has(KNOWN_MALICIOUS_LINK)
        val strongLink = has(LOOKALIKE_DOMAIN, LINK_TEXT_MISMATCH, PUNYCODE_OR_MIXED_SCRIPT_DOMAIN, IP_ADDRESS_LINK) || maliciousLink
        val weakLink = has(LINK_SHORTENER, UNUSUAL_DOMAIN_ENDING, INSECURE_LINK, MALFORMED_LINK)
        val senderMismatch = has(SENDER_NAME_DOMAIN_MISMATCH)
        val attachment = has(RISKY_ATTACHMENT)
        val notInContacts = has(SENDER_NOT_IN_CONTACTS)
        val knownContact = context.senderSavedAsContact == true
        val toolManipulation = has(INSTRUCTIONS_TO_AUTOMATED_TOOLS)
        val hasAnyLink = strongLink || weakLink
    }

    private data class Rule(val id: String, val level: RiskLevel, val condition: (Facts, Boolean) -> Boolean)

    // The Boolean passed to each rule is "the message contains a link".
    private val rules: List<Rule> = listOf(
        // CRITICAL: combinations documented in real campaigns that almost never occur in normal life.
        Rule("critical.code_request_with_pretext", RiskLevel.CRITICAL) { f, link ->
            f.credential && (f.organisation || link || f.newNumber || f.notInContacts || f.urgency || f.familyClaim)
        },
        Rule("critical.code_to_sender", RiskLevel.CRITICAL) { f, _ -> f.codeToSender },
        Rule("critical.remote_access_with_pretext", RiskLevel.CRITICAL) { f, _ -> f.remote && (f.organisation || f.urgency || f.anyMoney) },
        Rule("critical.new_number_money", RiskLevel.CRITICAL) { f, _ -> f.newNumber && f.moneyRequest },
        Rule("critical.money_secrecy", RiskLevel.CRITICAL) { f, _ -> f.anyMoney && f.secrecy },
        Rule("critical.unusual_payment_with_pretext", RiskLevel.CRITICAL) { f, _ -> f.unusualPayment && (f.organisation || f.urgency || f.newNumber || f.lure) },
        Rule("critical.known_malicious_link", RiskLevel.CRITICAL) { f, _ -> f.maliciousLink },
        Rule("critical.organisation_link_disguised", RiskLevel.CRITICAL) { f, _ -> f.has(LINK_TEXT_MISMATCH) && f.organisation },
        Rule("critical.fake_organisation_link_for_data_or_payment", RiskLevel.CRITICAL) { f, _ ->
            f.strongLink && f.organisation && (f.verify || f.paymentDemand || f.credential)
        },

        // HIGH: clear manipulation pattern, but legitimate explanations are conceivable.
        Rule("high.code_request", RiskLevel.HIGH) { f, _ -> f.credential },
        Rule("high.remote_access", RiskLevel.HIGH) { f, _ -> f.remote },
        Rule("high.unusual_payment", RiskLevel.HIGH) { f, _ -> f.unusualPayment },
        Rule("high.money_from_unknown_or_under_pressure", RiskLevel.HIGH) { f, _ ->
            f.moneyRequest && (f.notInContacts || f.urgency || f.channelSwitch || f.familyClaim && !f.knownContact)
        },
        Rule("high.suspicious_link_with_pretext", RiskLevel.HIGH) { f, _ ->
            f.strongLink && (f.organisation || f.verify || f.paymentDemand || f.urgency || f.lure)
        },
        Rule("high.sender_mismatch_with_request", RiskLevel.HIGH) { f, link ->
            f.senderMismatch && (f.verify || f.anyMoney || link || f.credential || f.attachment)
        },
        Rule("high.sender_mismatch_with_pressure", RiskLevel.HIGH) { f, _ -> f.senderMismatch && (f.threat || f.urgency) },
        Rule("high.organisation_payment_link", RiskLevel.HIGH) { f, link -> f.paymentDemand && f.organisation && (link || f.urgency) },
        Rule("high.lure_with_action", RiskLevel.HIGH) { f, link -> f.lure && (link || f.anyMoney || f.verify) },
        Rule("high.organisation_threat_link", RiskLevel.HIGH) { f, link -> f.organisation && f.threat && (link || f.verify) },
        Rule("high.new_number_secrecy", RiskLevel.HIGH) { f, _ -> f.newNumber && (f.secrecy || f.channelSwitch && f.urgency) },

        // CAUTION: one notable sign. The advice is to verify, not an accusation.
        Rule("caution.new_number", RiskLevel.CAUTION) { f, _ -> f.newNumber },
        Rule("caution.money_request_unconfirmed_sender", RiskLevel.CAUTION) { f, _ -> f.moneyRequest && !f.knownContact },
        Rule("caution.suspicious_link", RiskLevel.CAUTION) { f, _ -> f.strongLink },
        Rule("caution.weak_link_with_pressure", RiskLevel.CAUTION) { f, _ -> f.weakLink && (f.urgency || f.organisation || f.verify) },
        Rule("caution.sender_mismatch", RiskLevel.CAUTION) { f, _ -> f.senderMismatch },
        Rule("caution.risky_attachment", RiskLevel.CAUTION) { f, _ -> f.attachment },
        Rule("caution.lure", RiskLevel.CAUTION) { f, _ -> f.lure && (f.organisation || f.urgency || f.notInContacts) },
        Rule("caution.verification_link", RiskLevel.CAUTION) { f, link -> f.verify && link },
        Rule("caution.organisation_threat", RiskLevel.CAUTION) { f, _ -> f.organisation && f.threat },
        Rule("caution.payment_demand", RiskLevel.CAUTION) { f, link -> f.paymentDemand && (link || f.organisation) },
        Rule("caution.secrecy", RiskLevel.CAUTION) { f, _ -> f.secrecy },
        Rule("caution.family_channel_switch", RiskLevel.CAUTION) { f, _ -> f.familyAddress && f.channelSwitch },
        Rule("caution.instructions_to_tools", RiskLevel.CAUTION) { f, _ -> f.toolManipulation },
    )

    fun assess(
        findings: List<Finding>,
        context: ConversationContext,
        completeness: Completeness,
        hasLinks: Boolean,
        extraLimitations: Set<Limitation> = emptySet(),
    ): RiskAssessment {
        val facts = Facts(findings, context)
        val link = hasLinks || facts.hasAnyLink
        val matched = rules.filter { it.condition(facts, link) }
        val level = matched.maxOfOrNull { it.level } ?: RiskLevel.NO_WARNING
        val decisive = matched.filter { it.level == level }.map { it.id }

        val limitations = buildSet {
            addAll(extraLimitations)
            when (completeness) {
                Completeness.TRUNCATED -> add(Limitation.CONTENT_TRUNCATED)
                Completeness.REDACTED -> add(Limitation.CONTENT_REDACTED)
                Completeness.UNKNOWN -> add(Limitation.COMPLETENESS_UNKNOWN)
                Completeness.COMPLETE -> Unit
            }
            if (!context.historyAvailable) add(Limitation.NO_CONVERSATION_HISTORY)
            if (facts.familyAddress || facts.organisation || facts.newNumber) add(Limitation.SENDER_IDENTITY_UNVERIFIABLE)
        }

        return RiskAssessment(
            level = level,
            confidence = confidenceOf(level, matched.size, findings, limitations),
            findings = findings,
            policyRules = decisive,
            actions = actionsFor(level, facts, link),
            verificationAdvised = level >= RiskLevel.CAUTION || Limitation.SENDER_IDENTITY_UNVERIFIABLE in limitations,
            limitations = limitations,
        )
    }

    private fun confidenceOf(level: RiskLevel, matchedRules: Int, findings: List<Finding>, limitations: Set<Limitation>): Confidence {
        val partial = Limitation.CONTENT_TRUNCATED in limitations || Limitation.CONTENT_REDACTED in limitations
        if (level == RiskLevel.NO_WARNING) {
            // Absence of signs is weak evidence, and weaker still when we saw only part of the message.
            return if (partial || findings.isNotEmpty()) Confidence.LOW else Confidence.MEDIUM
        }
        val strong = findings.count { it.confidence == Confidence.HIGH && it.provenance != Provenance.AI_MODEL }
        val base = when {
            matchedRules >= 3 && strong >= 1 -> Confidence.HIGH
            matchedRules >= 2 || strong >= 1 -> Confidence.MEDIUM
            else -> Confidence.LOW
        }
        return if (partial && base == Confidence.HIGH) Confidence.MEDIUM else base
    }

    private fun actionsFor(level: RiskLevel, f: Facts, link: Boolean): List<RecommendedAction> {
        if (level == RiskLevel.NO_WARNING) return listOf(WHEN_IN_DOUBT_VERIFY)
        val actions = mutableListOf<RecommendedAction>()
        if (f.credential || f.verify) actions += NEVER_SHARE_CODES
        if (f.anyMoney) actions += DO_NOT_SEND_MONEY
        if (f.familyAddress || f.newNumber || f.notInContacts && f.moneyRequest) actions += CALL_KNOWN_NUMBER
        if (f.organisation || f.senderMismatch) actions += CONTACT_ORGANISATION_OFFICIAL_CHANNEL
        if (link) actions += DO_NOT_OPEN_LINK
        if (f.remote) actions += DO_NOT_INSTALL_APPS
        if (f.attachment) actions += DO_NOT_OPEN_ATTACHMENT
        if (f.secrecy || level == RiskLevel.CRITICAL) actions += TALK_TO_TRUSTED_PERSON
        if (actions.isEmpty()) actions += WHEN_IN_DOUBT_VERIFY
        val top = actions.distinct().take(3).toMutableList()
        if (level >= RiskLevel.HIGH && (f.anyMoney || f.credential || f.verify)) top += IF_ALREADY_PAID_CALL_BANK
        return top
    }
}
