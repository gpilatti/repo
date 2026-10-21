package pt.guardiao.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Guards the privacy promises made in the UI and in docs/PRIVACY_AND_DATA_FLOWS.md.
 * If one of these fails, the promise and the documentation must change first.
 */
class ManifestPolicyTest {
    private val manifest = File("src/main/AndroidManifest.xml").readText()

    @Test fun `no internet permission`() {
        assertFalse(manifest.contains("android.permission.INTERNET"))
    }

    @Test fun `no broad or restricted permissions`() {
        listOf(
            "READ_SMS", "RECEIVE_SMS", "READ_CONTACTS", "READ_CALL_LOG", "BIND_ACCESSIBILITY_SERVICE",
            "QUERY_ALL_PACKAGES", "READ_EXTERNAL_STORAGE", "MANAGE_EXTERNAL_STORAGE", "SYSTEM_ALERT_WINDOW",
        ).forEach { assertFalse(it, manifest.contains(it)) }
    }

    @Test fun `backups are disabled`() {
        assertTrue(manifest.contains("android:allowBackup=\"false\""))
        assertTrue(manifest.contains("@xml/data_extraction_rules"))
    }

    @Test fun `ui does not promise total protection`() {
        val strings = File("src/main/res/values/strings.xml").readText().lowercase()
        listOf("totalmente protegid", "100%", "está sempre protegid", "nunca será enganad").forEach {
            assertFalse(it, strings.contains(it))
        }
    }

    @Test fun `listener code never logs`() {
        val sources = File("src/main/kotlin").walkTopDown().filter { it.extension == "kt" }
        sources.forEach { f ->
            val text = f.readText()
            listOf("Log.", "println(", "printStackTrace").forEach { assertFalse("${f.name} uses $it", text.contains(it)) }
        }
    }
}
