package pt.guardiao.core

import kotlinx.coroutines.runBlocking
import org.junit.Test
import pt.guardiao.core.model.RiskLevel
import java.io.File
import kotlin.test.assertTrue

/**
 * Precision, recall and false-positive rate on the synthetic corpus.
 * A "positive" is an automatic alert (HIGH or CRITICAL). These numbers describe
 * the synthetic set only and say nothing about real-world performance.
 */
class EvaluationTest {

    @Test
    fun `report metrics on the synthetic corpus`() = runBlocking {
        val analyzer = FraudAnalyzer()
        var tp = 0; var fp = 0; var fn = 0; var tn = 0
        var benignCaution = 0
        val rows = mutableListOf<String>()
        val labelled = Scenarios.all.filter { it.label == "scam" || it.label == "benign" }
        for (s in labelled) {
            val level = analyzer.analyse(s.message).level
            val alerted = level >= RiskLevel.HIGH
            val scam = s.label == "scam"
            when {
                scam && alerted -> tp++
                scam && !alerted -> fn++
                !scam && alerted -> fp++
                else -> tn++
            }
            if (!scam && level == RiskLevel.CAUTION) benignCaution++
            rows += "| ${s.id} | ${s.label} | ${s.expectedMin}..${s.expectedMax} | $level |"
        }
        val precision = tp.toDouble() / (tp + fp).coerceAtLeast(1)
        val recall = tp.toDouble() / (tp + fn).coerceAtLeast(1)
        val fpr = fp.toDouble() / (fp + tn).coerceAtLeast(1)
        val report = buildString {
            appendLine("# Synthetic evaluation report")
            appendLine()
            appendLine("Synthetic data only. Not evidence of real-world detection performance.")
            appendLine()
            appendLine("Cases: ${labelled.size} (scam ${tp + fn}, benign ${fp + tn}); ambiguous cases excluded.")
            appendLine("Positive = automatic alert (HIGH or CRITICAL).")
            appendLine()
            appendLine("- True positives: $tp, false negatives: $fn")
            appendLine("- False positives: $fp, true negatives: $tn")
            appendLine("- Benign messages shown as CAUTION (no automatic alert): $benignCaution")
            appendLine("- Precision: ${"%.2f".format(precision)}")
            appendLine("- Recall: ${"%.2f".format(recall)}")
            appendLine("- False-positive rate: ${"%.2f".format(fpr)}")
            appendLine()
            appendLine("| Case | Label | Expected | Result |")
            appendLine("|---|---|---|---|")
            rows.forEach { appendLine(it) }
        }
        System.getProperty("evaluation.report")?.let { File(it).apply { parentFile.mkdirs() }.writeText(report) }
        println(report)
        // Guard rails, not targets: a regression below these fails the build.
        assertTrue(fpr == 0.0, "no benign control may trigger an automatic alert")
        assertTrue(recall >= 0.9, "recall on synthetic scams dropped to $recall")
    }
}
