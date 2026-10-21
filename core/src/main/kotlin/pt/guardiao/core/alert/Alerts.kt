package pt.guardiao.core.alert

import pt.guardiao.core.model.Confidence
import pt.guardiao.core.model.FindingKind
import pt.guardiao.core.model.IncomingMessage
import pt.guardiao.core.model.Limitation
import pt.guardiao.core.model.MessageSource
import pt.guardiao.core.model.RecommendedAction
import pt.guardiao.core.model.RiskAssessment
import pt.guardiao.core.model.RiskLevel
import pt.guardiao.core.model.SignalType
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * What an alert carries: codes only. No message text, sender name, number or
 * link, so nothing sensitive ends up in the notification, the intent extras or
 * the system's notification history.
 */
data class AlertPayload(
    val level: RiskLevel,
    val confidence: Confidence,
    val source: MessageSource,
    val signals: List<Pair<SignalType, FindingKind>>,
    val actions: List<RecommendedAction>,
    val limitations: Set<Limitation>,
) {
    /** Compact string form for an Android Intent extra. Contains only enum names. */
    fun encode(): String = listOf(
        level.name,
        confidence.name,
        source.name,
        signals.joinToString(",") { "${it.first.name}:${it.second.name}" },
        actions.joinToString(",") { it.name },
        limitations.joinToString(",") { it.name },
    ).joinToString("|")

    companion object {
        fun from(source: MessageSource, assessment: RiskAssessment) = AlertPayload(
            level = assessment.level,
            confidence = assessment.confidence,
            source = source,
            signals = assessment.findings.map { it.type to it.kind }.distinctBy { it.first },
            actions = assessment.actions,
            limitations = assessment.limitations,
        )

        /** Returns null for anything that is not a well-formed payload. */
        fun decode(encoded: String?): AlertPayload? = runCatching {
            val parts = encoded!!.split("|")
            require(parts.size == 6)
            fun <T> list(s: String, f: (String) -> T) = if (s.isEmpty()) emptyList() else s.split(",").map(f)
            AlertPayload(
                level = RiskLevel.valueOf(parts[0]),
                confidence = Confidence.valueOf(parts[1]),
                source = MessageSource.valueOf(parts[2]),
                signals = list(parts[3]) { p ->
                    val (t, k) = p.split(":")
                    SignalType.valueOf(t) to FindingKind.valueOf(k)
                },
                actions = list(parts[4]) { RecommendedAction.valueOf(it) },
                limitations = list(parts[5]) { Limitation.valueOf(it) }.toSet(),
            )
        }.getOrNull()
    }
}

/** Delivers an alert through an approved Android interface (a notification). */
fun interface AlertDispatcher {
    fun dispatch(payload: AlertPayload)
}

/** Which levels produce an automatic alert. CAUTION is shown only when the user checks a message. */
object AlertThreshold {
    val DEFAULT = RiskLevel.HIGH
    fun shouldAlert(level: RiskLevel, threshold: RiskLevel = DEFAULT) = level >= threshold
}

/**
 * Prevents repeated alerts for the same message (WhatsApp and email apps often
 * repost or update a notification several times).
 *
 * Remembers only salted SHA-256 fingerprints, in memory, for a limited time and
 * a limited count. The salt is random per process, so fingerprints cannot be
 * compared across restarts or matched against a list of known texts.
 */
class AlertDeduplicator(
    private val ttlMillis: Long = 6 * 60 * 60 * 1000L,
    private val maxEntries: Int = 200,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
    private val seen = LinkedHashMap<String, Long>()

    /** Returns true the first time a message is seen within the time window. */
    @Synchronized
    fun firstTime(message: IncomingMessage): Boolean {
        val now = clock()
        seen.entries.removeAll { now - it.value > ttlMillis }
        val key = fingerprint(message)
        if (seen.containsKey(key)) return false
        seen[key] = now
        while (seen.size > maxEntries) seen.remove(seen.keys.first())
        return true
    }

    @Synchronized
    fun clear() = seen.clear()

    @Synchronized
    fun size() = seen.size

    private fun fingerprint(message: IncomingMessage): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt)
        digest.update(message.source.name.toByteArray())
        digest.update(0)
        digest.update((message.senderDisplayName ?: "").trim().lowercase().toByteArray())
        digest.update(0)
        digest.update(message.body.trim().replace(Regex("\\s+"), " ").lowercase().toByteArray())
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
