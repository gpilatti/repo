package pt.guardiao.core

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Test
import pt.guardiao.core.model.IncomingMessage
import pt.guardiao.core.model.MessageSource
import pt.guardiao.core.model.RiskLevel
import java.io.File

/**
 * Cases written after the rules were tuned, and deliberately not used to tune
 * them. The test reports results without asserting thresholds: it is a more
 * honest (still synthetic, still small) view of generalisation. If a rule is
 * changed because of a holdout case, that case must move to the main corpus.
 */
class HoldoutEvaluationTest {
    @Test
    fun `report holdout results`() = runBlocking {
        val file = File(javaClass.classLoader.getResource("holdout/holdout.json")!!.toURI())
        val analyzer = FraudAnalyzer()
        var tp = 0; var fn = 0; var fp = 0; var tn = 0
        val rows = mutableListOf<String>()
        for (e in Json.parseToJsonElement(file.readText()).jsonArray) {
            val o = e.jsonObject
            val m = IncomingMessage(
                MessageSource.valueOf(o["source"]!!.jsonPrimitive.content),
                o["body"]!!.jsonPrimitive.content,
                senderDisplayName = o["sender"]?.jsonPrimitive?.content,
                isGroupConversation = false,
            )
            val r = analyzer.analyse(m)
            val scam = o["label"]!!.jsonPrimitive.content == "scam"
            val alerted = r.level >= RiskLevel.HIGH
            when { scam && alerted -> tp++; scam -> fn++; alerted -> fp++; else -> tn++ }
            rows += "| ${o["id"]!!.jsonPrimitive.content} | ${if (scam) "scam" else "benign"} | ${r.level} | ${r.policyRules.joinToString()} |"
        }
        val report = buildString {
            appendLine("# Holdout evaluation (not used for tuning)")
            appendLine()
            appendLine("Scam ${tp + fn}, benign ${fp + tn}. Alerts (HIGH+): TP $tp, FN $fn, FP $fp, TN $tn.")
            appendLine("Recall ${"%.2f".format(tp.toDouble() / (tp + fn).coerceAtLeast(1))}, " +
                "false-positive rate ${"%.2f".format(fp.toDouble() / (fp + tn).coerceAtLeast(1))}.")
            appendLine()
            appendLine("| Case | Label | Result | Deciding rules |")
            appendLine("|---|---|---|---|")
            rows.forEach { appendLine(it) }
        }
        System.getProperty("evaluation.report")?.let {
            File(File(it).parentFile, "holdout.md").apply { parentFile.mkdirs() }.writeText(report)
        }
        println(report)
    }
}
