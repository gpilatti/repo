package pt.guardiao.core.model

/** The four user-facing levels. Ordered from lowest to highest. */
enum class RiskLevel {
    /** No warning signs found. This is never presented as "safe". */
    NO_WARNING,
    CAUTION,
    HIGH,
    CRITICAL,
}

/** Short codes for the actions the app can recommend. Text lives in the explanation catalog. */
enum class RecommendedAction {
    DO_NOT_SEND_MONEY,
    CALL_KNOWN_NUMBER,
    NEVER_SHARE_CODES,
    DO_NOT_OPEN_LINK,
    CONTACT_ORGANISATION_OFFICIAL_CHANNEL,
    DO_NOT_INSTALL_APPS,
    TALK_TO_TRUSTED_PERSON,
    IF_ALREADY_PAID_CALL_BANK,
    DO_NOT_OPEN_ATTACHMENT,
    WHEN_IN_DOUBT_VERIFY,
}

/** Why an assessment may be missing information. */
enum class Limitation {
    CONTENT_TRUNCATED,
    CONTENT_REDACTED,
    COMPLETENESS_UNKNOWN,
    NO_CONVERSATION_HISTORY,
    SENDER_IDENTITY_UNVERIFIABLE,
    LINK_REPUTATION_NOT_CHECKED,
    AI_ANALYSIS_UNAVAILABLE,
    MESSAGE_TOO_LONG,
}

data class RiskAssessment(
    val level: RiskLevel,
    /** How sure the engine is about the level, in words. Not a calibrated probability. */
    val confidence: Confidence,
    val findings: List<Finding>,
    /** Identifiers of the policy rules that decided the level, for tests and audits. */
    val policyRules: List<String>,
    val actions: List<RecommendedAction>,
    val verificationAdvised: Boolean,
    val limitations: Set<Limitation>,
) {
    val isIncomplete: Boolean
        get() = Limitation.CONTENT_TRUNCATED in limitations ||
            Limitation.CONTENT_REDACTED in limitations ||
            Limitation.MESSAGE_TOO_LONG in limitations

    val signalTypes: Set<SignalType> get() = findings.map { it.type }.toSet()
}
