package pt.guardiao.app.ui

import pt.guardiao.core.alert.AlertPayload
import pt.guardiao.core.explain.Explanation
import pt.guardiao.core.explain.ExplanationCatalog
import pt.guardiao.core.explain.ExplanationGenerator
import pt.guardiao.core.model.RiskAssessment
import pt.guardiao.core.model.RiskLevel

/** What the result screen shows. Held in memory only while the screen is open. */
class ResultModel(
    val level: RiskLevel,
    val explanation: Explanation,
    /** Excerpts from the message, only for checks the user started in the app. */
    val evidence: List<String>,
    val fromAlert: Boolean,
) {
    companion object {
        fun fromAssessment(a: RiskAssessment, catalog: ExplanationCatalog) = ResultModel(
            level = a.level,
            explanation = ExplanationGenerator(catalog).explain(a),
            evidence = if (a.level == RiskLevel.NO_WARNING) emptyList() else
                a.findings.flatMap { it.evidence }.map { it.excerpt }.distinct().take(3),
            fromAlert = false,
        )

        fun fromAlert(p: AlertPayload, catalog: ExplanationCatalog) = ResultModel(
            level = p.level,
            explanation = ExplanationGenerator(catalog).explain(p.level, p.confidence, p.signals, p.actions, p.limitations),
            evidence = emptyList(),
            fromAlert = true,
        )
    }
}
