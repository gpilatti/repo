package pt.guardiao.core.links

import pt.guardiao.core.detect.AnalysisInput
import pt.guardiao.core.detect.FraudSignalDetector
import pt.guardiao.core.model.Confidence
import pt.guardiao.core.model.Evidence
import pt.guardiao.core.model.Finding
import pt.guardiao.core.model.FindingKind
import pt.guardiao.core.model.Provenance
import pt.guardiao.core.model.SignalType
import java.net.IDN
import java.net.URI

/** Parsed view of a link. Parsing is local: links are never opened or fetched. */
data class ParsedLink(
    val raw: String,
    val host: String,
    val registrableDomain: String,
    val scheme: String?,
    val hasUserInfo: Boolean,
)

object LinkParser {
    private val IPV4 = Regex("""^\d{1,3}(\.\d{1,3}){3}$""")

    fun parse(raw: String): ParsedLink? {
        val trimmed = raw.trim().trimEnd('.', ',', ';', ':', '!', '?', ')')
        if (trimmed.isEmpty()) return null
        val withScheme = if (Regex("(?i)^[a-z][a-z0-9+.-]*://").containsMatchIn(trimmed)) trimmed else "http://$trimmed"
        val uri = try {
            URI(withScheme.replace(" ", "%20"))
        } catch (_: Exception) {
            return null
        }
        val rawAuthority = uri.rawAuthority ?: return null
        val hasUserInfo = rawAuthority.contains('@')
        val hostPart = rawAuthority.substringAfterLast('@').substringBefore(':').trim('[', ']').lowercase()
        if (hostPart.isEmpty() || hostPart.startsWith('.') || hostPart.contains("..")) return null
        val scheme = if (trimmed.contains("://")) uri.scheme?.lowercase() else null
        return ParsedLink(trimmed, hostPart, registrableDomain(hostPart), scheme, hasUserInfo)
    }

    fun isIpAddress(host: String): Boolean = IPV4.matches(host) || host.contains(':')

    fun registrableDomain(host: String): String {
        if (isIpAddress(host)) return host
        val labels = host.trimEnd('.').split('.')
        if (labels.size <= 2) return labels.joinToString(".")
        val lastTwo = labels.takeLast(2).joinToString(".")
        return if (lastTwo in OfficialDomains.MULTI_LABEL_SUFFIXES) labels.takeLast(3).joinToString(".") else lastTwo
    }

    fun isOnDomain(host: String, domain: String): Boolean = host == domain || host.endsWith(".$domain")
}

/**
 * Looks at links in the message without opening them. Each link can produce
 * several findings; the policy decides how much they matter in combination.
 */
class UrlAnalyzer : FraudSignalDetector {

    override fun detect(input: AnalysisInput): List<Finding> {
        val findings = mutableListOf<Finding>()
        val candidates = input.entities.urls.map { it.value } + input.message.links.map { it.href }
        for (raw in candidates.distinct().take(MAX_LINKS)) {
            val parsed = LinkParser.parse(raw)
            if (parsed == null) {
                findings += finding(SignalType.MALFORMED_LINK, Confidence.LOW, "link.malformed", raw)
                continue
            }
            findings += analyse(parsed)
        }
        for (link in input.message.links.take(MAX_LINKS)) {
            mismatch(link.displayText, link.href)?.let { findings += it }
        }
        // Keep one finding per signal type, with up to two examples.
        return findings.groupBy { it.type }.map { (_, group) ->
            group.first().copy(evidence = group.flatMap { it.evidence }.distinctBy { it.excerpt }.take(2))
        }
    }

    fun analyse(link: ParsedLink): List<Finding> {
        val out = mutableListOf<Finding>()
        val host = link.host
        if (LinkParser.isIpAddress(host)) {
            out += finding(SignalType.IP_ADDRESS_LINK, Confidence.MEDIUM, "link.ip_address", link.raw)
        }
        if (host.split('.').any { it.startsWith("xn--") } || host.any { it.code > 127 }) {
            out += finding(SignalType.PUNYCODE_OR_MIXED_SCRIPT_DOMAIN, Confidence.HIGH, "link.punycode", link.raw)
        }
        if (link.hasUserInfo) {
            // "https://cgd.pt@evil.example" really goes to evil.example.
            out += finding(SignalType.LOOKALIKE_DOMAIN, Confidence.HIGH, "link.userinfo_trick", link.raw)
        }
        if (link.registrableDomain in OfficialDomains.SHORTENERS) {
            out += finding(SignalType.LINK_SHORTENER, Confidence.LOW, "link.shortener", link.raw)
        }
        val tld = host.substringAfterLast('.')
        if (tld in OfficialDomains.UNUSUAL_TLDS) {
            out += finding(SignalType.UNUSUAL_DOMAIN_ENDING, Confidence.LOW, "link.unusual_tld", link.raw)
        }
        if (link.scheme == "http") {
            out += finding(SignalType.INSECURE_LINK, Confidence.LOW, "link.no_tls", link.raw)
        }
        lookalike(link)?.let { out += it }
        return out
    }

    private fun lookalike(link: ParsedLink): Finding? {
        val host = deobfuscate(IDN.toUnicode(link.host, IDN.ALLOW_UNASSIGNED))
        val compact = host.replace("-", "").replace(".", "")
        for (brand in OfficialDomains.BRANDS) {
            val mentioned = brand.tokens.any { token ->
                if (brand.exactOnly || token.length <= 4) host.split('.', '-').any { it == token } else compact.contains(token)
            }
            if (!mentioned) continue
            val official = brand.domains.any { LinkParser.isOnDomain(link.host, it) }
            if (!official) return finding(SignalType.LOOKALIKE_DOMAIN, Confidence.HIGH, "link.brand_lookalike", link.raw)
        }
        return null
    }

    private fun mismatch(displayText: String?, href: String): Finding? {
        val text = displayText?.trim().orEmpty()
        if (text.isEmpty() || !text.contains('.') || text.contains(' ')) return null
        val shown = LinkParser.parse(text) ?: return null
        val real = LinkParser.parse(href) ?: return null
        if (shown.registrableDomain == real.registrableDomain) return null
        return finding(SignalType.LINK_TEXT_MISMATCH, Confidence.HIGH, "link.text_mismatch", "$text → ${real.host}")
    }

    /** Undo common character swaps such as "cttpagamentos" written as "c77" or "rnbway". */
    private fun deobfuscate(host: String): String = host.lowercase()
        .replace('0', 'o').replace('1', 'l').replace('3', 'e').replace('5', 's').replace('7', 't')
        .replace("rn", "m").replace("vv", "w")

    private fun finding(type: SignalType, confidence: Confidence, ruleId: String, raw: String) = Finding(
        type = type,
        confidence = confidence,
        provenance = Provenance.LINK_ANALYSIS,
        kind = FindingKind.OBSERVED,
        ruleId = ruleId,
        evidence = listOf(Evidence(raw.take(80))),
    )

    private companion object {
        const val MAX_LINKS = 10
    }
}
