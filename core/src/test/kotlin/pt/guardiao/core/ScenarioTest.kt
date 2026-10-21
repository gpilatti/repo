package pt.guardiao.core

import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertTrue
import kotlin.test.fail

/** Every labelled scenario must land inside its expected risk range with the expected signals. */
class ScenarioTest {
    private val analyzer = FraudAnalyzer()

    @Test
    fun `there are at least ten documented scenarios plus benign controls`() {
        val scenarios = Scenarios.all
        assertTrue((1..10).all { n -> scenarios.any { it.scenario == n } }, "every scenario 1 to 10 has a fixture")
        assertTrue(scenarios.count { it.label == "benign" } >= 10, "at least ten benign controls")
        assertTrue(scenarios.any { it.language == "en" }, "English cases present")
    }

    @Test
    fun `all scenarios match their expected range`() = runBlocking {
        val failures = mutableListOf<String>()
        for (s in Scenarios.all) {
            val result = analyzer.analyse(s.message)
            val types = result.signalTypes
            if (result.level < s.expectedMin || result.level > s.expectedMax) {
                failures += "${s.id}: got ${result.level}, expected ${s.expectedMin}..${s.expectedMax}; " +
                    "signals=$types rules=${result.policyRules} (${s.why})"
            }
            s.mustInclude.filterNot { it in types }.forEach { failures += "${s.id}: missing $it; signals=$types" }
            s.mustNotInclude.filter { it in types }.forEach { failures += "${s.id}: unexpected $it" }
        }
        if (failures.isNotEmpty()) fail(failures.joinToString("\n"))
    }
}
