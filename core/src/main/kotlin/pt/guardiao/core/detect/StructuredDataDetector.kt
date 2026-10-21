package pt.guardiao.core.detect

import pt.guardiao.core.model.Confidence
import pt.guardiao.core.model.Evidence
import pt.guardiao.core.model.Finding
import pt.guardiao.core.model.FindingKind
import pt.guardiao.core.model.Provenance
import pt.guardiao.core.model.SignalType

/** Flags bank account numbers (IBAN) written in the message. */
class StructuredDataDetector : FraudSignalDetector {
    override fun detect(input: AnalysisInput): List<Finding> {
        val iban = input.entities.ibans.firstOrNull() ?: return emptyList()
        return listOf(
            Finding(
                type = SignalType.BANK_ACCOUNT_NUMBER,
                confidence = Confidence.MEDIUM,
                provenance = Provenance.RULE,
                kind = FindingKind.OBSERVED,
                ruleId = "money.iban_present",
                // Mask the number: the user already has the message; the app does not need to repeat it.
                evidence = listOf(Evidence(iban.value.take(4) + " …")),
            ),
        )
    }
}
