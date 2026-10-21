package pt.guardiao.core.text

/** Spans of structured data found in a message. */
data class Span(val start: Int, val end: Int, val value: String)

data class ExtractedEntities(
    val urls: List<Span>,
    val phoneNumbers: List<Span>,
    val ibans: List<Span>,
    val amounts: List<Span>,
    val emailAddresses: List<Span>,
)

object EntityExtractor {
    // Explicit links (with scheme or www.) and bare domains followed by a path.
    private val URL = Regex(
        """(?i)\b(?:https?://[^\s<>"')\]]+|www\.[^\s<>"')\]]+|(?:[a-z0-9-]+\.)+[a-z]{2,24}/[^\s<>"')\]]*)""",
    )

    // Bare domains with no path ("ctt-pagamentos.com"), limited to plausible endings.
    private val BARE_DOMAIN = Regex(
        """(?i)\b(?:[a-z0-9-]+\.)+(?:com|pt|net|org|info|top|xyz|icu|online|site|shop|click|link|live|me|ly|io|co|eu|app|cfd|sbs|buzz|vip)\b""",
    )
    private val EMAIL = Regex("""(?i)\b[a-z0-9._%+-]+@[a-z0-9.-]+\.[a-z]{2,24}\b""")
    private val PHONE = Regex("""(?<![\w])(?:\+|00)?\d[\d\s.-]{7,16}\d(?![\w])""")
    private val IBAN = Regex("""(?i)\b[a-z]{2}\d{2}(?:[ ]?[a-z0-9]{4}){3,7}(?:[ ]?[a-z0-9]{1,3})?\b""")
    private val AMOUNT = Regex("""(?i)(?:€\s?\d[\d.\s]*(?:,\d{1,2})?|\b\d[\d.\s]*(?:[.,]\d{1,2})?\s?(?:€|eur\b|euros?\b|reais\b|r\$|usd\b|\$))""")

    fun extract(text: NormalizedText): ExtractedEntities {
        val source = text.original
        val emails = EMAIL.findAll(source).map { it.toSpan() }.toList()
        val explicitUrls = URL.findAll(source).map { it.toSpan(trimTrailing = true) }.toList()
        val bareDomains = BARE_DOMAIN.findAll(source)
            .map { it.toSpan(trimTrailing = true) }
            .filter { span -> explicitUrls.none { overlaps(it, span) } && emails.none { overlaps(it, span) } }
            .toList()
        val ibans = IBAN.findAll(source).map { it.toSpan() }
            .filter { span -> span.value.count { it.isDigit() } >= 12 }
            .toList()
        val phones = PHONE.findAll(source).map { it.toSpan() }
            .filter { span ->
                val digits = span.value.count { it.isDigit() }
                digits in 9..15 && ibans.none { overlaps(it, span) }
            }
            .toList()
        val amounts = AMOUNT.findAll(source).map { it.toSpan() }.toList()
        return ExtractedEntities(explicitUrls + bareDomains, phones, ibans, amounts, emails)
    }

    private fun overlaps(a: Span, b: Span) = a.start < b.end && b.start < a.end

    private fun MatchResult.toSpan(trimTrailing: Boolean = false): Span {
        var value = value
        if (trimTrailing) value = value.trimEnd('.', ',', ';', ':', '!', '?')
        return Span(range.first, range.first + value.length, value)
    }
}
