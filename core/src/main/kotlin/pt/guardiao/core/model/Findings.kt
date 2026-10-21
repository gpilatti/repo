package pt.guardiao.core.model

/** The manipulation tactic a signal belongs to. Used by the risk policy. */
enum class Tactic {
    IMPERSONATION,
    PRESSURE,
    SECRECY,
    MONEY,
    CREDENTIALS,
    REMOTE_ACCESS,
    LURE,
    LINK,
    SENDER,
    CONTEXT,
    TOOL_MANIPULATION,
}

/** Every signal the engine can raise. The explanation catalog has text for each one. */
enum class SignalType(val tactic: Tactic) {
    FAMILY_ADDRESS(Tactic.IMPERSONATION),
    FAMILY_IDENTITY_CLAIM(Tactic.IMPERSONATION),
    NEW_NUMBER_CLAIM(Tactic.IMPERSONATION),
    BANK_CLAIM(Tactic.IMPERSONATION),
    AUTHORITY_CLAIM(Tactic.IMPERSONATION),
    COURIER_CLAIM(Tactic.IMPERSONATION),
    TECH_SUPPORT_CLAIM(Tactic.IMPERSONATION),

    URGENCY(Tactic.PRESSURE),
    THREAT(Tactic.PRESSURE),
    DISTRESS_STORY(Tactic.PRESSURE),
    SECRECY_REQUEST(Tactic.SECRECY),
    CHANNEL_SWITCH(Tactic.PRESSURE),

    MONEY_TRANSFER_REQUEST(Tactic.MONEY),
    PAYMENT_DEMAND(Tactic.MONEY),
    UNUSUAL_PAYMENT_METHOD(Tactic.MONEY),
    BANK_ACCOUNT_NUMBER(Tactic.MONEY),

    CREDENTIAL_REQUEST(Tactic.CREDENTIALS),
    ACCOUNT_VERIFICATION_REQUEST(Tactic.CREDENTIALS),
    REMOTE_ACCESS_REQUEST(Tactic.REMOTE_ACCESS),

    PRIZE_REFUND_OR_INVESTMENT(Tactic.LURE),

    LOOKALIKE_DOMAIN(Tactic.LINK),
    LINK_TEXT_MISMATCH(Tactic.LINK),
    PUNYCODE_OR_MIXED_SCRIPT_DOMAIN(Tactic.LINK),
    IP_ADDRESS_LINK(Tactic.LINK),
    LINK_SHORTENER(Tactic.LINK),
    UNUSUAL_DOMAIN_ENDING(Tactic.LINK),
    INSECURE_LINK(Tactic.LINK),
    MALFORMED_LINK(Tactic.LINK),
    KNOWN_MALICIOUS_LINK(Tactic.LINK),

    SENDER_NAME_DOMAIN_MISMATCH(Tactic.SENDER),
    RISKY_ATTACHMENT(Tactic.SENDER),

    SENDER_NOT_IN_CONTACTS(Tactic.CONTEXT),

    INSTRUCTIONS_TO_AUTOMATED_TOOLS(Tactic.TOOL_MANIPULATION),
}

enum class Confidence { LOW, MEDIUM, HIGH }

/** Who produced a finding. */
enum class Provenance { RULE, LINK_ANALYSIS, SENDER_METADATA, CONTEXT, THREAT_INTELLIGENCE, AI_MODEL }

/**
 * Whether a finding is something we directly saw in the message or metadata,
 * or a conclusion drawn from it. The UI never presents an inference as a fact.
 */
enum class FindingKind { OBSERVED, INFERRED }

/**
 * A short excerpt of the message supporting a finding. Kept in memory only and
 * shown on the in-app analysis screen; never placed in notifications or intents.
 */
class Evidence(val excerpt: String) {
    override fun toString(): String = "Evidence([redacted, length=${excerpt.length}])"
}

data class Finding(
    val type: SignalType,
    val confidence: Confidence,
    val provenance: Provenance,
    val kind: FindingKind,
    /** Identifier of the rule or component that produced the finding, for tests and audits. */
    val ruleId: String,
    val evidence: List<Evidence> = emptyList(),
)
