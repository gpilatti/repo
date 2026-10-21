package pt.guardiao.core.detect

import pt.guardiao.core.context.ConversationContext
import pt.guardiao.core.model.Finding
import pt.guardiao.core.model.IncomingMessage
import pt.guardiao.core.text.ExtractedEntities
import pt.guardiao.core.text.NormalizedText

/** Everything a detector may look at. Built once per message, in memory. */
class AnalysisInput(
    val message: IncomingMessage,
    val text: NormalizedText,
    val entities: ExtractedEntities,
    val context: ConversationContext,
) {
    override fun toString(): String = "AnalysisInput([redacted])"
}

/** A deterministic detector. Must be fast, side-effect free and never throw on odd input. */
fun interface FraudSignalDetector {
    fun detect(input: AnalysisInput): List<Finding>
}
