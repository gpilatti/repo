package pt.guardiao.core.explain

import pt.guardiao.core.model.Confidence
import pt.guardiao.core.model.FindingKind
import pt.guardiao.core.model.Limitation
import pt.guardiao.core.model.RecommendedAction
import pt.guardiao.core.model.RiskAssessment
import pt.guardiao.core.model.RiskLevel
import pt.guardiao.core.model.SignalType

/** A plain-language explanation ready to show. Built only from codes, never from message text. */
data class Explanation(
    val headline: String,
    val summary: String,
    val reasons: List<String>,
    val actions: List<String>,
    val confidenceNote: String,
    val limitations: List<String>,
)

/** All user-facing wording for one language. Adding a language means adding a catalog. */
interface ExplanationCatalog {
    val languageTag: String
    fun headline(level: RiskLevel): String
    fun summary(level: RiskLevel): String
    fun reason(type: SignalType, kind: FindingKind): String
    fun action(action: RecommendedAction): String
    fun confidence(level: RiskLevel, confidence: Confidence): String
    fun limitation(limitation: Limitation): String?
}

class ExplanationGenerator(private val catalog: ExplanationCatalog = PtPtCatalog) {

    fun explain(assessment: RiskAssessment): Explanation = explain(
        level = assessment.level,
        confidence = assessment.confidence,
        signals = assessment.findings.sortedWith(compareByDescending<pt.guardiao.core.model.Finding> { it.confidence }
            .thenBy { REASON_PRIORITY.indexOf(it.type.tactic).let { i -> if (i < 0) 99 else i } })
            .map { it.type to it.kind },
        actions = assessment.actions,
        limitations = assessment.limitations,
    )

    /**
     * Rebuilds an explanation from codes only. Used when an alert notification is
     * opened: the notification carries codes, never the message.
     */
    fun explain(
        level: RiskLevel,
        confidence: Confidence,
        signals: List<Pair<SignalType, FindingKind>>,
        actions: List<RecommendedAction>,
        limitations: Set<Limitation>,
    ): Explanation {
        val reasons = if (level == RiskLevel.NO_WARNING) emptyList() else
            signals.distinctBy { it.first }.map { (type, kind) -> catalog.reason(type, kind) }.distinct().take(MAX_REASONS)
        return Explanation(
            headline = catalog.headline(level),
            summary = catalog.summary(level),
            reasons = reasons,
            actions = actions.map { catalog.action(it) },
            confidenceNote = catalog.confidence(level, confidence),
            limitations = limitations.sortedBy { it.ordinal }.mapNotNull { catalog.limitation(it) },
        )
    }

    private companion object {
        const val MAX_REASONS = 3
        val REASON_PRIORITY = listOf(
            pt.guardiao.core.model.Tactic.CREDENTIALS, pt.guardiao.core.model.Tactic.MONEY,
            pt.guardiao.core.model.Tactic.IMPERSONATION, pt.guardiao.core.model.Tactic.REMOTE_ACCESS,
            pt.guardiao.core.model.Tactic.LINK, pt.guardiao.core.model.Tactic.SECRECY,
            pt.guardiao.core.model.Tactic.SENDER, pt.guardiao.core.model.Tactic.PRESSURE,
            pt.guardiao.core.model.Tactic.LURE, pt.guardiao.core.model.Tactic.CONTEXT,
        )
    }
}
