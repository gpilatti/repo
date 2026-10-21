package pt.guardiao.core.model

/**
 * Where a message came from. Each source has different capability boundaries,
 * documented in docs/FEASIBILITY_AND_ARCHITECTURE.md.
 */
enum class MessageSource {
    /** Best-effort: text visible in a WhatsApp notification. */
    WHATSAPP_NOTIFICATION,

    /** Best-effort: text visible in a notification from a supported email app. */
    EMAIL_NOTIFICATION,

    /** The user shared text into the app with the Android share sheet. */
    USER_SHARED,

    /** The user typed or pasted text inside the app. */
    MANUAL_ENTRY,

    /** Built-in sample used for the practice alert and demos. */
    SAMPLE,
}

/** How much of the original message the app could actually see. */
enum class Completeness {
    COMPLETE,

    /** The notification only showed part of the text (for example, a long email). */
    TRUNCATED,

    /** The system or the sending app hid some or all of the content. */
    REDACTED,

    /** We cannot tell. */
    UNKNOWN,
}

/** A link as it appeared in an email, where the visible text may differ from the destination. */
data class LinkRef(val displayText: String?, val href: String)

/**
 * The minimum input needed for analysis. Lives in memory only and must never be
 * persisted, logged or sent anywhere without explicit consent.
 *
 * [toString] is overridden so that accidental logging cannot leak content.
 */
class IncomingMessage(
    val source: MessageSource,
    val body: String,
    /** Name shown by the sending app. A display name is not proof of identity. */
    val senderDisplayName: String? = null,
    /** Email address, when the source exposes it. */
    val senderAddress: String? = null,
    val subject: String? = null,
    val isGroupConversation: Boolean? = null,
    val links: List<LinkRef> = emptyList(),
    val attachmentNames: List<String> = emptyList(),
    val completeness: Completeness = Completeness.UNKNOWN,
    val receivedAtMillis: Long? = null,
) {
    override fun toString(): String =
        "IncomingMessage(source=$source, bodyLength=${body.length}, completeness=$completeness, [content redacted])"
}
