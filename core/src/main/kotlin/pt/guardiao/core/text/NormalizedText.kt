package pt.guardiao.core.text

import java.text.Normalizer

/**
 * Text prepared for matching: lower case, accents removed and common lookalike
 * characters folded, while keeping a one-to-one character mapping with the
 * original so that evidence excerpts can be cut from the original text.
 */
class NormalizedText private constructor(val original: String, val folded: String) {

    fun excerpt(start: Int, end: Int, padding: Int = 20, maxLength: Int = 120): String {
        val from = (start - padding).coerceAtLeast(0)
        val to = (end + padding).coerceAtMost(original.length)
        var text = original.substring(from, to).replace(Regex("\\s+"), " ").trim()
        if (text.length > maxLength) text = text.take(maxLength).trimEnd() + "…"
        if (from > 0) text = "…$text"
        if (to < original.length && !text.endsWith("…")) text = "$text…"
        return text
    }

    override fun toString(): String = "NormalizedText([redacted, length=${original.length}])"

    companion object {
        /** Messages longer than this are cut before analysis to bound processing time. */
        const val MAX_LENGTH = 8_000

        fun of(raw: String): NormalizedText {
            val bounded = if (raw.length > MAX_LENGTH) raw.substring(0, MAX_LENGTH) else raw
            val folded = StringBuilder(bounded.length)
            for (ch in bounded) folded.append(fold(ch))
            return NormalizedText(bounded, folded.toString())
        }

        /** Folds a whole string, for short metadata such as sender names. */
        fun fold(text: String): String = buildString(text.length) { for (ch in text) append(fold(ch)) }

        fun fold(ch: Char): Char {
            CONFUSABLES[ch]?.let { return it }
            if (ch.code < 128) return ch.lowercaseChar()
            if (Character.isWhitespace(ch) || ch == ' ') return ' '
            val decomposed = Normalizer.normalize(ch.toString(), Normalizer.Form.NFD)
            val base = decomposed.firstOrNull() ?: ch
            val lower = base.lowercaseChar()
            return CONFUSABLES[lower] ?: lower
        }

        /**
         * Characters from other scripts that look like Latin letters, plus
         * typographic variants. Not exhaustive: a full Unicode confusables
         * table would be needed for complete coverage.
         */
        private val CONFUSABLES: Map<Char, Char> = mapOf(
            'а' to 'a', 'е' to 'e', 'о' to 'o', 'р' to 'p', 'с' to 'c', 'х' to 'x', 'у' to 'y',
            'і' to 'i', 'ј' to 'j', 'ѕ' to 's', 'ԁ' to 'd', 'ɡ' to 'g', 'ո' to 'n', 'ս' to 'u',
            'А' to 'a', 'Е' to 'e', 'О' to 'o', 'Р' to 'p', 'С' to 'c', 'Х' to 'x', 'В' to 'b',
            'М' to 'm', 'Н' to 'h', 'Т' to 't', 'К' to 'k',
            'ο' to 'o', 'α' to 'a', 'ε' to 'e', 'ι' to 'i', 'ν' to 'v', 'τ' to 't', 'κ' to 'k',
            '’' to '\'', '‘' to '\'', '“' to '"', '”' to '"', '\u2013' to '-', '\u2014' to '-',
            '€' to '€',
        )
    }
}
