package pt.guardiao.core.ingest

import pt.guardiao.core.model.Completeness
import pt.guardiao.core.model.IncomingMessage
import pt.guardiao.core.model.MessageSource

/**
 * The fields the Android notification listener copies out of a notification.
 * A plain data holder so the mapping rules can be tested without Android.
 * Instances live only for the duration of one analysis.
 */
class NotificationFields(
    val packageName: String,
    val title: String?,
    val text: String?,
    val bigText: String?,
    val textLines: List<String> = emptyList(),
    /** MessagingStyle messages, newest last: (sender name, text). */
    val messages: List<Pair<String?, String>> = emptyList(),
    val conversationTitle: String? = null,
    val isGroupConversation: Boolean? = null,
    val isGroupSummary: Boolean = false,
    val isOngoing: Boolean = false,
    val postedAtMillis: Long? = null,
) {
    override fun toString() = "NotificationFields(package=$packageName, [content redacted])"
}

/** Declares what a source can and cannot provide. Shown in the "Proteção" screen. */
data class SourceCapabilities(
    val source: MessageSource,
    val automatic: Boolean,
    val seesFullText: Boolean,
    val seesSenderAddress: Boolean,
    val seesHistory: Boolean,
)

/** One adapter per source. Returns null when the input is not something to analyse. */
interface MessageSourceAdapter<T> {
    val capabilities: SourceCapabilities
    fun toMessage(input: T): IncomingMessage?
}

/** Apps whose notifications we read. Everything else is ignored without being inspected. */
object SupportedApps {
    val WHATSAPP = setOf("com.whatsapp", "com.whatsapp.w4b")
    val EMAIL = setOf(
        "com.google.android.gm",
        "com.microsoft.office.outlook",
        "com.samsung.android.email.provider",
        "pt.sapo.mobile.android.sapomail",
        "com.yahoo.mobile.client.android.mail",
    )

    fun isSupported(packageName: String) = packageName in WHATSAPP || packageName in EMAIL
}

/**
 * Turns a notification into a message, conservatively:
 * - only supported apps; group summaries and ongoing notifications are skipped;
 * - only the newest message of a conversation is analysed, so earlier messages
 *   in the same notification are not re-read on every update;
 * - content hidden by the system or the app is marked REDACTED, not guessed.
 */
object NotificationAdapter : MessageSourceAdapter<NotificationFields> {

    override val capabilities = SourceCapabilities(
        source = MessageSource.WHATSAPP_NOTIFICATION,
        automatic = true,
        seesFullText = false,
        seesSenderAddress = false,
        seesHistory = false,
    )

    val emailCapabilities = SourceCapabilities(
        source = MessageSource.EMAIL_NOTIFICATION,
        automatic = true,
        seesFullText = false,
        seesSenderAddress = false,
        seesHistory = false,
    )

    // Placeholders shown when previews are hidden by WhatsApp or by Android's sensitive-content protection.
    private val HIDDEN_PLACEHOLDERS = listOf(
        Regex("""(?i)^\s*\d*\s*(nova|novas)?\s*mensage(m|ns)( nova| novas)?\s*$"""),
        Regex("""(?i)^\s*\d*\s*new messages?\s*$"""),
        Regex("""(?i)sensitive notification content hidden"""),
        Regex("""(?i)conte[uú]do (da notifica[cç][aã]o )?(sens[ií]vel|confidencial) (oculto|ocultado)"""),
        Regex("""(?i)^\s*(conte[uú]do oculto|contents hidden)\s*$"""),
    )

    override fun toMessage(input: NotificationFields): IncomingMessage? {
        if (!SupportedApps.isSupported(input.packageName)) return null
        if (input.isGroupSummary || input.isOngoing) return null
        val isWhatsApp = input.packageName in SupportedApps.WHATSAPP

        val latest = input.messages.lastOrNull()
        val body = (latest?.second ?: input.bigText ?: input.textLines.lastOrNull() ?: input.text)?.trim().orEmpty()
        if (body.isEmpty()) return null

        val redacted = HIDDEN_PLACEHOLDERS.any { it.containsMatchIn(body) }
        val completeness = when {
            redacted -> Completeness.REDACTED
            // Email notifications show a preview, not the whole email.
            !isWhatsApp -> Completeness.TRUNCATED
            body.endsWith("…") || body.endsWith("...") -> Completeness.TRUNCATED
            else -> Completeness.UNKNOWN
        }
        val sender = if (isWhatsApp) latest?.first ?: input.title else input.title
        return IncomingMessage(
            source = if (isWhatsApp) MessageSource.WHATSAPP_NOTIFICATION else MessageSource.EMAIL_NOTIFICATION,
            body = if (redacted) "" else body,
            senderDisplayName = sender?.trim(),
            subject = if (!isWhatsApp && input.bigText != null && input.text != null && input.text != input.bigText) input.text else null,
            isGroupConversation = input.isGroupConversation,
            completeness = completeness,
            receivedAtMillis = input.postedAtMillis,
        )
    }
}

/** Text the user shares into the app or selects in another app. */
object SharedTextAdapter : MessageSourceAdapter<String> {
    const val MAX_SHARED_CHARS = 20_000

    override val capabilities = SourceCapabilities(
        source = MessageSource.USER_SHARED,
        automatic = false,
        seesFullText = true,
        seesSenderAddress = false,
        seesHistory = false,
    )

    override fun toMessage(input: String): IncomingMessage? {
        val text = input.trim()
        if (text.isEmpty()) return null
        return IncomingMessage(
            source = MessageSource.USER_SHARED,
            body = text.take(MAX_SHARED_CHARS),
            completeness = if (text.length > MAX_SHARED_CHARS) Completeness.TRUNCATED else Completeness.COMPLETE,
        )
    }
}
