package pt.guardiao.core.privacy

/**
 * Replaces personal identifiers with placeholders before any text leaves the
 * analysis core (for example, to an optional AI provider).
 */
object Redactor {
    private val EMAIL = Regex("""(?i)\b[a-z0-9._%+-]+@[a-z0-9.-]+\.[a-z]{2,24}\b""")
    private val URL = Regex("""(?i)\b(?:https?://|www\.)\S+""")
    private val IBAN = Regex("""(?i)\b[a-z]{2}\d{2}(?:[ ]?[a-z0-9]{4}){3,7}(?:[ ]?[a-z0-9]{1,3})?\b""")
    private val CARD = Regex("""\b(?:\d[ -]?){13,19}\b""")
    private val PHONE = Regex("""(?<![\w])(?:\+|00)?\d[\d\s.-]{7,16}\d(?![\w])""")
    private val LONG_DIGITS = Regex("""\b\d{4,}\b""")

    fun redact(text: String): String = text
        .replace(EMAIL, "[email]")
        .replace(URL) { m -> "[link:" + hostOf(m.value) + "]" }
        .replace(IBAN, "[iban]")
        .replace(CARD, "[cartao]")
        .replace(PHONE, "[telefone]")
        .replace(LONG_DIGITS, "[numero]")

    private fun hostOf(url: String): String =
        url.substringAfter("://").substringBefore('/').substringBefore('?').substringAfterLast('@').lowercase()
}
