package pt.guardiao.core.detect

import pt.guardiao.core.model.Confidence
import pt.guardiao.core.model.Evidence
import pt.guardiao.core.model.Finding
import pt.guardiao.core.model.FindingKind
import pt.guardiao.core.model.Provenance
import pt.guardiao.core.model.SignalType

/**
 * A phrase rule matches folded text (lower case, no accents). Patterns are
 * written in folded form: "telemovel", not "telemóvel".
 *
 * When [negatable] is true, a match preceded by a negation such as "nunca" or
 * "não" is ignored, so that "nunca partilhe este código" (a legitimate safety
 * notice) does not count as a request for a code.
 */
class PhraseRule(
    val id: String,
    val type: SignalType,
    val confidence: Confidence,
    patterns: List<String>,
    val negatable: Boolean = false,
) {
    val regexes: List<Regex> = patterns.map { Regex(it) }
}

class PhraseRuleDetector(private val rules: List<PhraseRule> = Lexicon.ALL) : FraudSignalDetector {

    override fun detect(input: AnalysisInput): List<Finding> {
        val folded = input.text.folded
        val findings = mutableListOf<Finding>()
        for (rule in rules) {
            val excerpts = mutableListOf<Evidence>()
            for (regex in rule.regexes) {
                for (match in regex.findAll(folded)) {
                    if (rule.negatable && isNegated(folded, match.range.first)) continue
                    excerpts += Evidence(input.text.excerpt(match.range.first, match.range.last + 1))
                    if (excerpts.size >= MAX_EVIDENCE_PER_RULE) break
                }
                if (excerpts.size >= MAX_EVIDENCE_PER_RULE) break
            }
            if (excerpts.isNotEmpty()) {
                findings += Finding(
                    type = rule.type,
                    confidence = rule.confidence,
                    provenance = Provenance.RULE,
                    kind = FindingKind.OBSERVED,
                    ruleId = rule.id,
                    evidence = excerpts,
                )
            }
        }
        return findings
    }

    private fun isNegated(folded: String, matchStart: Int): Boolean {
        val windowStart = (matchStart - NEGATION_WINDOW).coerceAtLeast(0)
        val before = folded.substring(windowStart, matchStart)
        return NEGATION.containsMatchIn(before)
    }

    private companion object {
        const val MAX_EVIDENCE_PER_RULE = 2
        const val NEGATION_WINDOW = 30
        val NEGATION = Regex("""\b(nunca|nao|jamais|nem|never|not|don't|do not|nobody|ninguem)\b""")
    }
}
