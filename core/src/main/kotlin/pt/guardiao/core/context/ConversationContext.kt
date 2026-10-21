package pt.guardiao.core.context

import pt.guardiao.core.detect.AnalysisInput
import pt.guardiao.core.detect.FraudSignalDetector
import pt.guardiao.core.model.Confidence
import pt.guardiao.core.model.Evidence
import pt.guardiao.core.model.Finding
import pt.guardiao.core.model.FindingKind
import pt.guardiao.core.model.IncomingMessage
import pt.guardiao.core.model.MessageSource
import pt.guardiao.core.model.Provenance
import pt.guardiao.core.model.SignalType

/**
 * What we know about the conversation around a message. Every field is nullable:
 * null means "we cannot know", which is different from "no".
 */
data class ConversationContext(
    /** True if the sending app shows a saved contact name; false if it shows only a number. */
    val senderSavedAsContact: Boolean? = null,
    /** Messages from this sender seen earlier while the app was running (in memory only). */
    val earlierMessagesThisSession: Int? = null,
    /** The app never has access to past conversations. Kept explicit so no rule assumes otherwise. */
    val historyAvailable: Boolean = false,
) {
    companion object {
        val UNKNOWN = ConversationContext()
    }
}

/** Supplies context for a message. Implementations must not store message content. */
fun interface ContextProvider {
    fun contextFor(message: IncomingMessage): ConversationContext
}

/**
 * Infers whether a WhatsApp sender is a saved contact from the notification
 * title. WhatsApp shows the saved name for known contacts and the phone number
 * otherwise. This is an inference from what is displayed, not a contacts lookup:
 * the app does not request access to the user's contacts.
 */
object DisplayNameContextProvider : ContextProvider {
    private val PHONE_LIKE = Regex("""^[+\d][\d\s().-]{6,}$""")

    override fun contextFor(message: IncomingMessage): ConversationContext {
        if (message.source != MessageSource.WHATSAPP_NOTIFICATION) return ConversationContext.UNKNOWN
        val name = message.senderDisplayName?.trim() ?: return ConversationContext.UNKNOWN
        if (message.isGroupConversation == true) return ConversationContext.UNKNOWN
        return ConversationContext(senderSavedAsContact = !PHONE_LIKE.matches(name))
    }
}

class ContextSignalDetector : FraudSignalDetector {
    override fun detect(input: AnalysisInput): List<Finding> {
        if (input.context.senderSavedAsContact != false) return emptyList()
        return listOf(
            Finding(
                type = SignalType.SENDER_NOT_IN_CONTACTS,
                confidence = Confidence.MEDIUM,
                provenance = Provenance.CONTEXT,
                // We see a number instead of a name; that the person is unknown is an inference.
                kind = FindingKind.INFERRED,
                ruleId = "context.sender_shown_as_number",
                evidence = listOf(Evidence("número sem nome guardado")),
            ),
        )
    }
}
