package pt.guardiao.core.ai

import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import pt.guardiao.core.model.Confidence
import pt.guardiao.core.model.Evidence
import pt.guardiao.core.model.Finding
import pt.guardiao.core.model.FindingKind
import pt.guardiao.core.model.Provenance
import pt.guardiao.core.model.SignalType
import pt.guardiao.core.privacy.Redactor

/**
 * What a model receives. The message text is untrusted data: providers must
 * place it in a data field, never concatenate it into their instructions.
 */
class InferenceRequest(
    /** Redacted text: phone numbers, emails, IBANs and links are replaced with placeholders. */
    val redactedText: String,
    /** Signal types the model may report. Anything else in its answer is discarded. */
    val allowedSignals: Set<SignalType>,
) {
    override fun toString(): String = "InferenceRequest([redacted, length=${redactedText.length}])"
}

/** One signal reported by a model, before validation. */
data class ModelSignal(val signal: String, val confidence: String, val quote: String)

/** Raw model output, as parsed by the provider from its structured response. */
data class InferenceOutput(val signals: List<ModelSignal>)

/**
 * Provider-neutral interface for an optional AI model, on-device or remote.
 * The app works fully without one.
 */
interface InferenceProvider {
    /** True if the provider processes text off the device. Requires explicit consent. */
    val sendsDataOffDevice: Boolean

    suspend fun analyse(request: InferenceRequest): InferenceOutput
}

/** Default provider: no AI. */
object NoInference : InferenceProvider {
    override val sendsDataOffDevice = false
    override suspend fun analyse(request: InferenceRequest) = InferenceOutput(emptyList())
}

sealed interface InferenceOutcome {
    data class Completed(val findings: List<Finding>, val discarded: Int) : InferenceOutcome
    data object Skipped : InferenceOutcome
    data object Failed : InferenceOutcome
}

/**
 * Runs a provider inside strict limits and validates everything it returns.
 *
 * Safety properties, all covered by tests:
 * - input is redacted and size-limited before it reaches the provider;
 * - a timeout or exception degrades to "no AI findings", never to a crash;
 * - only known signal types and confidence values are accepted;
 * - each accepted signal must quote text that actually appears in the message,
 *   so a model cannot invent evidence;
 * - the model can add findings but can never remove or lower deterministic
 *   ones, and it has no way to declare a message or sender legitimate.
 */
class BoundedInference(
    private val provider: InferenceProvider,
    private val timeoutMillis: Long = 3_000,
    private val maxInputChars: Int = 2_000,
    private val maxSignals: Int = 5,
) {
    suspend fun run(originalText: String, allowed: Set<SignalType> = MODEL_SIGNALS): InferenceOutcome {
        if (provider === NoInference) return InferenceOutcome.Skipped
        val redacted = Redactor.redact(originalText).take(maxInputChars)
        val output = try {
            withTimeout(timeoutMillis) { provider.analyse(InferenceRequest(redacted, allowed)) }
        } catch (_: TimeoutCancellationException) {
            return InferenceOutcome.Failed
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (_: Exception) {
            return InferenceOutcome.Failed
        }
        val accepted = mutableListOf<Finding>()
        var discarded = 0
        val haystack = normalise(redacted)
        for (signal in output.signals) {
            val type = SignalType.entries.firstOrNull { it.name == signal.signal }
            val confidence = Confidence.entries.firstOrNull { it.name == signal.confidence }
            val quote = signal.quote.trim()
            val grounded = quote.length in 3..200 && haystack.contains(normalise(quote))
            if (type == null || type !in allowed || confidence == null || !grounded || accepted.size >= maxSignals) {
                discarded++
                continue
            }
            accepted += Finding(
                type = type,
                // Model findings are capped at MEDIUM: they support, but never alone decide, a warning.
                confidence = if (confidence == Confidence.HIGH) Confidence.MEDIUM else confidence,
                provenance = Provenance.AI_MODEL,
                kind = FindingKind.INFERRED,
                ruleId = "ai.${type.name.lowercase()}",
                evidence = listOf(Evidence(quote.take(120))),
            )
        }
        return InferenceOutcome.Completed(accepted, discarded)
    }

    private fun normalise(s: String) = s.lowercase().replace(Regex("\\s+"), " ")

    companion object {
        /** Signals a model may contribute: semantic tactics that phrase rules can miss. */
        val MODEL_SIGNALS: Set<SignalType> = setOf(
            SignalType.FAMILY_IDENTITY_CLAIM, SignalType.NEW_NUMBER_CLAIM, SignalType.BANK_CLAIM,
            SignalType.AUTHORITY_CLAIM, SignalType.URGENCY, SignalType.THREAT, SignalType.DISTRESS_STORY,
            SignalType.SECRECY_REQUEST, SignalType.MONEY_TRANSFER_REQUEST, SignalType.PAYMENT_DEMAND,
            SignalType.CREDENTIAL_REQUEST, SignalType.REMOTE_ACCESS_REQUEST, SignalType.PRIZE_REFUND_OR_INVESTMENT,
        )
    }
}
