package pt.guardiao.app

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import pt.guardiao.app.data.ProtectionStatus
import pt.guardiao.app.data.RecentAlerts
import pt.guardiao.app.data.Samples
import pt.guardiao.app.ui.CheckScreen
import pt.guardiao.app.ui.GuardiaoTheme
import pt.guardiao.app.ui.HelpScreen
import pt.guardiao.app.ui.HomeScreen
import pt.guardiao.app.ui.OnboardingScreen
import pt.guardiao.app.ui.ProtectionScreen
import pt.guardiao.app.ui.ResultModel
import pt.guardiao.app.ui.ResultScreen
import pt.guardiao.core.alert.AlertPayload
import pt.guardiao.core.explain.ExplanationCatalog
import pt.guardiao.core.explain.PtPtCatalog
import pt.guardiao.core.ingest.SharedTextAdapter
import pt.guardiao.core.model.IncomingMessage
import pt.guardiao.core.model.MessageSource

/**
 * Single activity. Navigation is a small state machine rather than a library:
 * there are only a handful of screens and no deep back stack.
 */
class MainActivity : ComponentActivity() {

    private sealed interface Screen {
        data object Onboarding : Screen
        data class Tabs(val tab: Int) : Screen
        data class Check(val prefill: String) : Screen
        class Result(val model: ResultModel, val back: Screen) : Screen
    }

    private val app get() = application as GuardiaoApp
    private var screen by mutableStateOf<Screen>(Screen.Tabs(0))
    private var onboardingStep by mutableIntStateOf(0)
    private var status by mutableStateOf(ProtectionStatus(false, false, false))
    private var busy by mutableStateOf(false)

    // The UI is European Portuguese only for now. EnglishCatalog exists in core for a future full translation.
    private val catalog: ExplanationCatalog = PtPtCatalog

    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        refreshStatus()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!app.settings.onboardingDone) screen = Screen.Onboarding
        handleIntent(intent)
        setContent { GuardiaoTheme { Root() } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    private fun refreshStatus() {
        status = ProtectionStatus.read(this, app.settings)
    }

    /** Text arriving from the share sheet or the text-selection menu goes straight to the check screen. */
    private fun handleIntent(intent: Intent?) {
        intent ?: return
        val shared = when (intent.action) {
            Intent.ACTION_SEND -> intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()
            Intent.ACTION_PROCESS_TEXT -> intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()
            else -> null
        }
        when {
            shared != null -> {
                screen = Screen.Check(shared.take(SharedTextAdapter.MAX_SHARED_CHARS))
                SharedTextAdapter.toMessage(shared)?.let { analyseAndShow(it, Screen.Check(shared)) }
            }
            intent.action == ACTION_SHOW_ALERT -> AlertPayload.decode(intent.getStringExtra(EXTRA_ALERT))?.let {
                app.reminders.acknowledgeAll()
                screen = Screen.Result(ResultModel.fromAlert(it, catalog), Screen.Tabs(0))
            }
        }
        // Drop the content from the stored intent so it is not kept or redelivered.
        intent.removeExtra(Intent.EXTRA_TEXT)
        intent.removeExtra(Intent.EXTRA_PROCESS_TEXT)
    }

    private fun analyseAndShow(message: IncomingMessage, back: Screen) {
        busy = true
        lifecycleScope.launch {
            val assessment = app.analyzer.analyse(message)
            screen = Screen.Result(ResultModel.fromAssessment(assessment, catalog), back)
            busy = false
        }
    }

    private fun showPractice(back: Screen) {
        analyseAndShow(IncomingMessage(MessageSource.SAMPLE, Samples.PRACTICE), back)
    }

    private fun callTrusted() {
        val number = app.settings.trustedNumber
        val uri = if (number.isNotBlank()) Uri.parse("tel:${Uri.encode(number)}") else Uri.parse("tel:")
        // ACTION_DIAL opens the phone app without calling: the user stays in control. No permission needed.
        safeStart(Intent(Intent.ACTION_DIAL, uri))
    }

    /** Shares a short help request with a person the user picks. Never includes the message itself. */
    private fun shareWarning(model: ResultModel) {
        val text = buildString {
            appendLine(getString(R.string.share_intro))
            appendLine(model.explanation.headline)
            model.explanation.reasons.forEach { appendLine("• $it") }
            appendLine()
            append(getString(R.string.share_outro))
        }
        val send = Intent(Intent.ACTION_SEND).setType("text/plain")
            .putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_subject))
            .putExtra(Intent.EXTRA_TEXT, text)
        safeStart(Intent.createChooser(send, getString(R.string.result_share)))
    }

    private fun requestNotifications() {
        if (Build.VERSION.SDK_INT >= 33) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun safeStart(intent: Intent) {
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            // Some phones lack a settings page or a phone app; the screen keeps working.
        }
    }

    @Composable
    private fun Root() {
        val last by RecentAlerts.last.collectAsState()
        val current = screen
        BackHandler(enabled = current !is Screen.Tabs && current !is Screen.Onboarding) {
            screen = when (current) {
                is Screen.Result -> current.back
                else -> Screen.Tabs(0)
            }
        }
        when (current) {
            Screen.Onboarding -> Scaffold { pad ->
                androidx.compose.foundation.layout.Box(Modifier.padding(pad)) {
                    OnboardingScreen(
                        step = onboardingStep,
                        onStep = { onboardingStep = it },
                        onRequestNotifications = ::requestNotifications,
                        onOpenNotificationAccess = { safeStart(ProtectionStatus.notificationAccessIntent(this@MainActivity)) },
                        onShowPractice = { showPractice(Screen.Onboarding) },
                        onFinish = {
                            app.settings.onboardingDone = true
                            onboardingStep = 0
                            screen = Screen.Tabs(0)
                        },
                    )
                }
            }
            is Screen.Tabs -> Scaffold(bottomBar = { BottomBar(current.tab) }) { pad ->
                androidx.compose.foundation.layout.Box(Modifier.padding(pad)) {
                    when (current.tab) {
                        0 -> HomeScreen(
                            status, last,
                            onCheck = { screen = Screen.Check("") },
                            onOpenAlert = {
                                last?.let {
                                    app.reminders.acknowledgeAll()
                                    screen = Screen.Result(ResultModel.fromAlert(it.payload, catalog), current)
                                }
                            },
                        )
                        1 -> ProtectionScreen(
                            status,
                            onOpenAccess = { safeStart(ProtectionStatus.notificationAccessIntent(this@MainActivity)) },
                            onOpenNotificationSettings = {
                                if (Build.VERSION.SDK_INT >= 33 && !status.canPostNotifications) requestNotifications()
                                safeStart(ProtectionStatus.appNotificationSettingsIntent(this@MainActivity))
                            },
                            onTogglePause = {
                                app.settings.monitoringPaused = !app.settings.monitoringPaused
                                refreshStatus()
                            },
                        )
                        else -> HelpScreen(
                            app.settings.trustedName, app.settings.trustedNumber,
                            onSaveTrusted = { n, p -> app.settings.trustedName = n; app.settings.trustedNumber = p },
                            onPractice = { showPractice(current) },
                            onOnboarding = { onboardingStep = 0; screen = Screen.Onboarding },
                        )
                    }
                }
            }
            is Screen.Check -> Scaffold(topBar = { BackBar { screen = Screen.Tabs(0) } }) { pad ->
                androidx.compose.foundation.layout.Box(Modifier.padding(pad)) {
                    CheckScreen(current.prefill, busy) { text ->
                        SharedTextAdapter.toMessage(text)?.let {
                            analyseAndShow(IncomingMessage(MessageSource.MANUAL_ENTRY, it.body, completeness = it.completeness), Screen.Check(text))
                        }
                    }
                }
            }
            is Screen.Result -> Scaffold(topBar = { BackBar { screen = current.back } }) { pad ->
                androidx.compose.foundation.layout.Box(Modifier.padding(pad)) {
                    ResultScreen(
                        current.model,
                        onCallTrusted = ::callTrusted,
                        onShare = { shareWarning(current.model) },
                        onHelp = { screen = Screen.Tabs(2) },
                        onDone = { screen = if (current.back is Screen.Check) Screen.Tabs(0) else current.back },
                    )
                }
            }
        }
    }

    @Composable
    private fun BackBar(onBack: () -> Unit) {
        TextButton(onClick = onBack, modifier = Modifier.padding(top = 8.dp)) {
            Text("‹ " + stringResource(R.string.back), style = androidx.compose.material3.MaterialTheme.typography.labelLarge)
        }
    }

    @Composable
    private fun BottomBar(selected: Int) {
        NavigationBar {
            listOf(
                Triple(R.string.tab_home, Icons.Filled.Home, 0),
                Triple(R.string.tab_protection, Icons.Filled.Lock, 1),
                Triple(R.string.tab_help, Icons.Filled.Info, 2),
            ).forEach { (label, icon, index) ->
                NavigationBarItem(
                    selected = selected == index,
                    onClick = { screen = Screen.Tabs(index) },
                    icon = { Icon(icon, contentDescription = null) },
                    label = { Text(stringResource(label)) },
                )
            }
        }
    }

    companion object {
        const val ACTION_SHOW_ALERT = "pt.guardiao.app.SHOW_ALERT"
        const val EXTRA_ALERT = "alert"
    }
}
