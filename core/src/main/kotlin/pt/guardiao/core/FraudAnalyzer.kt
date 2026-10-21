package pt.guardiao.core

import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import pt.guardiao.core.ai.BoundedInference
import pt.guardiao.core.ai.InferenceOutcome
import pt.guardiao.core.ai.InferenceProvider
import pt.guardiao.core.ai.NoInference
import pt.guardiao.core.context.ContextProvider
import pt.guardiao.core.context.ContextSignalDetector
import pt.guardiao.core.context.DisplayNameContextProvider
import pt.guardiao.core.detect.AnalysisInput
import pt.guardiao.core.detect.FraudSignalDetector
import pt.guardiao.core.detect.PhraseRuleDetector
import pt.guardiao.core.detect.SenderMetadataDetector
import pt.guardiao.core.detect.StructuredDataDetector
import pt.guardiao.core.intel.NoThreatIntelligence
import pt.guardiao.core.intel.Reputation
import pt.guardiao.core.intel.ThreatIntelligenceProvider
import pt.guardiao.core.links.LinkParser
import pt.guardiao.core.links.UrlAnalyzer
import pt.guardiao.core.model.Confidence
import pt.guardiao.core.model.Evidence
import pt.guardiao.core.model.Finding
import pt.guardiao.core.model.FindingKind
import pt.guardiao.core.model.IncomingMessage
import pt.guardiao.core.model.Limitation
import pt.guardiao.core.model.Provenance
import pt.guardiao.core.model.RiskAssessment
import pt.guardiao.core.model.SignalType
import pt.guardiao.core.policy.RiskPolicy
import pt.guardiao.core.privacy.LogEvent
import pt.guardiao.core.privacy.SafeLogger
import pt.guardiao.core.text.EntityExtractor
import pt.guardiao.core.text.NormalizedText

/**
 * The bounded analysis pipeline:
 * normalise → extract entities → context → deterministic detectors →
 * optional link reputation → optional AI → risk policy.
 *
 * Nothing in this class stores, logs or transmits message content. Optional
 * providers receive only what their interfaces allow (host names, redacted text).
 */
class FraudAnalyzer(
    private val detectors: List<FraudSignalDetector> = DEFAULT_DETECTORS,
    private val contextProvider: ContextProvider = DisplayNameContextProvider,
    private val threatIntelligence: ThreatIntelligenceProvider = NoThreatIntelligence,
    inferenceProvider: InferenceProvider = NoInference,
    private val policy: RiskPolicy = RiskPolicy(),
    private val logger: SafeLogger = SafeLogger.NONE,
    private val threatIntelTimeoutMillis: Long = 2_000,
) {
    private val inference = BoundedInference(inferenceProvider)
    private val usesInference = inferenceProvider !== NoInference

    suspend fun analyse(message: IncomingMessage): RiskAssessment {
        val text = NormalizedText.of(message.body + (message.subject?.let { "\n$it" } ?: ""))
        val entities = EntityExtractor.extract(text)
        val context = contextProvider.contextFor(message)
        val input = AnalysisInput(message, text, entities, context)

        val findings = mutableListOf<Finding>()
        for (detector in detectors) {
            // A broken detector must not break the analysis.
            findings += runCatching { detector.detect(input) }.getOrDefault(emptyList())
        }

        val limitations = mutableSetOf<Limitation>()
        if (message.body.length > NormalizedText.MAX_LENGTH) limitations += Limitation.MESSAGE_TOO_LONG

        val hosts = (entities.urls.map { it.value } + message.links.map { it.href })
            .mapNotNull { LinkParser.parse(it)?.host }.distinct()
        if (hosts.isNotEmpty()) {
            findings += checkReputation(hosts, limitations)
        }

        if (usesInference) {
            when (val outcome = inference.run(text.original)) {
                is InferenceOutcome.Completed -> findings += outcome.findings
                InferenceOutcome.Failed -> {
                    limitations += Limitation.AI_ANALYSIS_UNAVAILABLE
                    logger.log(LogEvent.AI_FAILED)
                }
                InferenceOutcome.Skipped -> Unit
            }
        }

        val assessment = policy.assess(
            findings = findings,
            context = context,
            completeness = message.completeness,
            hasLinks = hosts.isNotEmpty(),
            extraLimitations = limitations,
        )
        logger.log(LogEvent.ANALYSIS_COMPLETED, assessment.level.ordinal, assessment.findings.size)
        return assessment
    }

    private suspend fun checkReputation(hosts: List<String>, limitations: MutableSet<Limitation>): List<Finding> {
        if (threatIntelligence === NoThreatIntelligence) {
            limitations += Limitation.LINK_REPUTATION_NOT_CHECKED
            return emptyList()
        }
        return try {
            withTimeout(threatIntelTimeoutMillis) {
                hosts.take(5).filter { threatIntelligence.reputationOf(it) == Reputation.KNOWN_MALICIOUS }.map { host ->
                    Finding(
                        SignalType.KNOWN_MALICIOUS_LINK, Confidence.HIGH, Provenance.THREAT_INTELLIGENCE,
                        FindingKind.OBSERVED, "intel.listed_host", listOf(Evidence(host)),
                    )
                }
            }
        } catch (_: TimeoutCancellationException) {
            limitations += Limitation.LINK_REPUTATION_NOT_CHECKED
            logger.log(LogEvent.THREAT_INTEL_FAILED)
            emptyList()
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (_: Exception) {
            limitations += Limitation.LINK_REPUTATION_NOT_CHECKED
            logger.log(LogEvent.THREAT_INTEL_FAILED)
            emptyList()
        }
    }

    companion object {
        val DEFAULT_DETECTORS: List<FraudSignalDetector> = listOf(
            PhraseRuleDetector(),
            UrlAnalyzer(),
            StructuredDataDetector(),
            SenderMetadataDetector(),
            ContextSignalDetector(),
        )
    }
}
