package pt.guardiao.core.explain

import pt.guardiao.core.model.Confidence
import pt.guardiao.core.model.FindingKind
import pt.guardiao.core.model.Limitation
import pt.guardiao.core.model.RecommendedAction
import pt.guardiao.core.model.RiskLevel
import pt.guardiao.core.model.SignalType
import pt.guardiao.core.model.SignalType.*

/** English wording, for future localisation and for English-language messages. */
object EnglishCatalog : ExplanationCatalog {
    override val languageTag = "en"

    override fun headline(level: RiskLevel) = when (level) {
        RiskLevel.CRITICAL -> "STOP. This message may be a scam."
        RiskLevel.HIGH -> "Careful: this message has signs of a scam."
        RiskLevel.CAUTION -> "This message deserves attention."
        RiskLevel.NO_WARNING -> "We found no warning signs."
    }

    override fun summary(level: RiskLevel) = when (level) {
        RiskLevel.CRITICAL -> "Do not do what the message asks. Do not send money or codes."
        RiskLevel.HIGH -> "Do not send money yet. Check first, another way."
        RiskLevel.CAUTION -> "It may be genuine, but check before you act."
        RiskLevel.NO_WARNING -> "This does not prove the message is genuine. If in doubt, ask someone you trust."
    }

    override fun reason(type: SignalType, kind: FindingKind): String {
        val text = when (type) {
            FAMILY_ADDRESS -> "It addresses you as family, but we cannot confirm who wrote it."
            FAMILY_IDENTITY_CLAIM -> "Someone says they are family. We cannot confirm it."
            NEW_NUMBER_CLAIM -> "It says they changed number. This is a common trick."
            BANK_CLAIM -> "It claims to be from a bank or payment service."
            AUTHORITY_CLAIM -> "It claims to be from the police, tax office or another authority."
            COURIER_CLAIM -> "It mentions a parcel or delivery."
            TECH_SUPPORT_CLAIM -> "It claims to be technical support."
            URGENCY -> "It asks you to hurry."
            THREAT -> "It threatens blocks, fines or legal trouble."
            DISTRESS_STORY -> "It tells a story of distress to make you act fast."
            SECRECY_REQUEST -> "It asks you to keep it secret or not to talk to family or the bank."
            CHANNEL_SWITCH -> "It asks you to talk on another number or app."
            MONEY_TRANSFER_REQUEST -> "It asks for money or a transfer."
            PAYMENT_DEMAND -> "It asks for a payment or fee."
            UNUSUAL_PAYMENT_METHOD -> "It asks for an unusual payment, such as gift cards, crypto or a \"safe account\"."
            BANK_ACCOUNT_NUMBER -> "It gives a bank account to send money to."
            CREDENTIAL_REQUEST -> "It asks for a code, PIN or password. No one you can trust asks for this."
            ACCOUNT_VERIFICATION_REQUEST -> "It asks you to confirm or update your details."
            REMOTE_ACCESS_REQUEST -> "It asks you to install an app or give access to your phone."
            PRIZE_REFUND_OR_INVESTMENT -> "It promises a prize, refund or easy money."
            LOOKALIKE_DOMAIN -> "The link imitates a known company's address, but is not the real one."
            LINK_TEXT_MISMATCH -> "The link shows one address but goes to another."
            PUNYCODE_OR_MIXED_SCRIPT_DOMAIN -> "The link uses disguised letters to look like another address."
            IP_ADDRESS_LINK -> "The link uses numbers instead of a site name."
            LINK_SHORTENER -> "The link is shortened and hides where it goes."
            UNUSUAL_DOMAIN_ENDING -> "The link's address has an unusual ending."
            INSECURE_LINK -> "The link does not use a secure connection."
            MALFORMED_LINK -> "There is a link we could not read."
            KNOWN_MALICIOUS_LINK -> "The link is on a list of dangerous sites."
            SENDER_NAME_DOMAIN_MISMATCH -> "The sender's name does not match the address the email came from."
            RISKY_ATTACHMENT -> "It has an attachment that can install programs."
            SENDER_NOT_IN_CONTACTS -> "It seems to come from a number not saved in your contacts."
            INSTRUCTIONS_TO_AUTOMATED_TOOLS -> "The message tries to fool checking tools like this one."
        }
        return if (kind == FindingKind.INFERRED && type != SENDER_NOT_IN_CONTACTS) "$text (estimated)" else text
    }

    override fun action(action: RecommendedAction) = when (action) {
        RecommendedAction.DO_NOT_SEND_MONEY -> "Do not send money or make payments for now."
        RecommendedAction.CALL_KNOWN_NUMBER -> "Call the person on the number you already had. Do not use the number in this message."
        RecommendedAction.NEVER_SHARE_CODES -> "Never share codes, PINs or passwords. Banks and police never ask for them."
        RecommendedAction.DO_NOT_OPEN_LINK -> "Do not open the link. Use the official app or the address you already know."
        RecommendedAction.CONTACT_ORGANISATION_OFFICIAL_CHANNEL -> "Contact the organisation using the number on your card, statement or official website."
        RecommendedAction.DO_NOT_INSTALL_APPS -> "Do not install any app this message asks for."
        RecommendedAction.TALK_TO_TRUSTED_PERSON -> "Talk to someone you trust before doing anything."
        RecommendedAction.IF_ALREADY_PAID_CALL_BANK -> "If you already sent money or details, call your bank now on its official number."
        RecommendedAction.DO_NOT_OPEN_ATTACHMENT -> "Do not open the attachment."
        RecommendedAction.WHEN_IN_DOUBT_VERIFY -> "If in doubt, check with the person or organisation another way."
    }

    override fun confidence(level: RiskLevel, confidence: Confidence) = when {
        level == RiskLevel.NO_WARNING -> "This is an automatic check and can be wrong."
        confidence == Confidence.HIGH -> "We found several strong signs."
        confidence == Confidence.MEDIUM -> "We found clear signs, but we are not certain."
        else -> "We found few signs. The message may be genuine."
    }

    override fun limitation(limitation: Limitation) = when (limitation) {
        Limitation.CONTENT_TRUNCATED -> "We could only see part of the message."
        Limitation.CONTENT_REDACTED -> "Your phone hid part of the content."
        Limitation.SENDER_IDENTITY_UNVERIFIABLE -> "We cannot confirm who sent the message."
        Limitation.MESSAGE_TOO_LONG -> "The message is very long; we only checked the start."
        Limitation.COMPLETENESS_UNKNOWN, Limitation.NO_CONVERSATION_HISTORY,
        Limitation.LINK_REPUTATION_NOT_CHECKED, Limitation.AI_ANALYSIS_UNAVAILABLE -> null
    }
}
