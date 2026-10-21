package pt.guardiao.core.detect

import pt.guardiao.core.links.LinkParser
import pt.guardiao.core.links.OfficialDomains
import pt.guardiao.core.model.Confidence
import pt.guardiao.core.model.Evidence
import pt.guardiao.core.model.Finding
import pt.guardiao.core.model.FindingKind
import pt.guardiao.core.model.Provenance
import pt.guardiao.core.model.SignalType
import pt.guardiao.core.text.NormalizedText

/**
 * Checks sender information, when the source exposes it. Email notifications
 * usually show only a display name, so most of these checks apply to content
 * the user shares into the app.
 */
class SenderMetadataDetector : FraudSignalDetector {

    override fun detect(input: AnalysisInput): List<Finding> {
        val out = mutableListOf<Finding>()
        val name = input.message.senderDisplayName?.let { NormalizedText.fold(it) }
        val address = input.message.senderAddress?.trim()?.lowercase()
        if (name != null && address != null && address.contains('@')) {
            nameDomainMismatch(name, address)?.let { out += it }
        }
        val risky = input.message.attachmentNames.firstOrNull { attachment ->
            RISKY_EXTENSIONS.any { attachment.lowercase().endsWith(it) }
        }
        if (risky != null) {
            out += Finding(
                SignalType.RISKY_ATTACHMENT, Confidence.MEDIUM, Provenance.SENDER_METADATA,
                FindingKind.OBSERVED, "sender.risky_attachment", listOf(Evidence(risky.takeLast(40))),
            )
        }
        return out
    }

    private fun nameDomainMismatch(name: String, address: String): Finding? {
        val domain = address.substringAfterLast('@')
        for (brand in OfficialDomains.ORGANISATION_SENDERS) {
            val compactName = name.replace(" ", "")
            val claimsBrand = brand.tokens.any { token ->
                Regex("""\b${Regex.escape(token)}\b""").containsMatchIn(name) || (token.length > 4 && compactName.contains(token))
            }
            if (!claimsBrand) continue
            val onOfficial = brand.domains.any { LinkParser.isOnDomain(domain, it) }
            val freeMail = domain in OfficialDomains.FREE_MAIL
            if ((brand.domains.isNotEmpty() && !onOfficial) || freeMail) {
                return Finding(
                    SignalType.SENDER_NAME_DOMAIN_MISMATCH, Confidence.HIGH, Provenance.SENDER_METADATA,
                    FindingKind.OBSERVED, "sender.name_domain_mismatch", listOf(Evidence("@$domain")),
                )
            }
        }
        return null
    }

    private companion object {
        val RISKY_EXTENSIONS = listOf(
            ".apk", ".exe", ".scr", ".js", ".vbs", ".bat", ".cmd", ".msi", ".jar", ".iso", ".img",
            ".html", ".htm", ".lnk", ".hta", ".zip", ".rar", ".7z",
        )
    }
}
