package pt.guardiao.core.intel

/** Result of a reputation lookup. Unknown is the normal answer and does not mean safe. */
enum class Reputation { KNOWN_MALICIOUS, UNKNOWN }

/**
 * A source of link reputation. Implementations receive only the host name, never
 * the full link, because paths and query strings can contain personal tokens.
 * A reputation source can only raise risk; "not listed" is never treated as
 * evidence that a site is legitimate.
 */
interface ThreatIntelligenceProvider {
    /** True if the provider sends anything off the device. Shown in the privacy screen. */
    val sendsDataOffDevice: Boolean

    suspend fun reputationOf(host: String): Reputation
}

/** Default: no lookups. */
object NoThreatIntelligence : ThreatIntelligenceProvider {
    override val sendsDataOffDevice = false
    override suspend fun reputationOf(host: String) = Reputation.UNKNOWN
}

/** An on-device list of known bad hosts, matched on the host and its parent domains. */
class LocalBlocklist(hosts: Collection<String>) : ThreatIntelligenceProvider {
    private val blocked = hosts.map { it.lowercase().trim('.') }.toSet()
    override val sendsDataOffDevice = false

    override suspend fun reputationOf(host: String): Reputation {
        val h = host.lowercase().trim('.')
        val listed = blocked.any { h == it || h.endsWith(".$it") }
        return if (listed) Reputation.KNOWN_MALICIOUS else Reputation.UNKNOWN
    }
}
