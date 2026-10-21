package pt.guardiao.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalClipboard
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import pt.guardiao.app.R
import pt.guardiao.app.data.ProtectionStatus
import pt.guardiao.app.data.RecentAlerts
import pt.guardiao.app.data.Samples
import pt.guardiao.core.model.RiskLevel
import java.text.DateFormat
import java.util.Date

@Composable
fun ScrollColumn(content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) { content() }
}

@Composable
fun HomeScreen(status: ProtectionStatus, lastAlert: RecentAlerts.Entry?, onCheck: () -> Unit, onOpenAlert: () -> Unit) {
    ScrollColumn {
        ScreenTitle(stringResource(R.string.app_name))
        // 1. Is protection active? Stated only as far as the real capabilities allow.
        when {
            status.automaticActive -> InfoCard(
                stringResource(R.string.home_status_active), stringResource(R.string.home_status_active_detail),
                icon = Icons.Filled.CheckCircle,
            )
            status.paused -> InfoCard(
                stringResource(R.string.home_status_paused), stringResource(R.string.home_status_paused_detail),
                icon = Icons.Filled.Info,
            )
            else -> InfoCard(
                stringResource(R.string.home_status_off), stringResource(R.string.home_status_off_detail),
                icon = Icons.Filled.Info,
            )
        }
        // 2. Has something important been detected?
        if (lastAlert == null) {
            Text(stringResource(R.string.home_no_alerts), style = MaterialTheme.typography.bodyLarge)
        } else {
            val style = riskStyle(lastAlert.payload.level)
            val time = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(lastAlert.atMillis))
            InfoCard(
                levelLabel(lastAlert.payload.level),
                stringResource(R.string.home_last_alert, time),
                container = style.container, content = style.content, icon = Icons.Filled.Warning,
            )
            SecondaryButton(stringResource(R.string.home_open_last_alert), onOpenAlert)
        }
        // 3. What should I do now?
        PrimaryButton(stringResource(R.string.home_check_message), onCheck, icon = Icons.Filled.Search)
        Text(stringResource(R.string.home_check_message_detail), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun CheckScreen(initialText: String, busy: Boolean, onCheck: (String) -> Unit) {
    var text by remember(initialText) { mutableStateOf(initialText) }
    var showEmpty by remember { mutableStateOf(false) }
    var exampleIndex by remember { mutableStateOf(0) }
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    ScrollColumn {
        ScreenTitle(stringResource(R.string.check_title))
        OutlinedTextField(
            value = text,
            onValueChange = { text = it; showEmpty = false },
            label = { Text(stringResource(R.string.check_hint)) },
            textStyle = MaterialTheme.typography.bodyLarge,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            modifier = Modifier.fillMaxWidth().heightIn(min = 180.dp),
        )
        if (showEmpty) {
            Text(
                stringResource(R.string.check_empty),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
        if (busy) {
            CircularProgressIndicator()
            Text(stringResource(R.string.check_working), style = MaterialTheme.typography.bodyLarge)
        } else {
            PrimaryButton(stringResource(R.string.check_run), {
                if (text.isBlank()) showEmpty = true else onCheck(text)
            }, icon = Icons.Filled.Search)
        }
        SecondaryButton(stringResource(R.string.check_paste), {
            scope.launch {
                val clip = clipboard.getClipEntry()?.clipData
                val pasted = if (clip != null && clip.itemCount > 0) clip.getItemAt(0).text?.toString() else null
                if (pasted != null) { text = pasted; showEmpty = false }
            }
        })
        TextButton(onClick = {
            text = Samples.EXAMPLES[exampleIndex % Samples.EXAMPLES.size]
            exampleIndex++
        }, modifier = Modifier.heightIn(min = BigTouch)) {
            Text(stringResource(R.string.check_try_example), style = MaterialTheme.typography.labelLarge)
        }
        InfoCard(stringResource(R.string.check_privacy_note), "", icon = Icons.Filled.Lock)
    }
}

@Composable
fun ResultScreen(model: ResultModel, onCallTrusted: () -> Unit, onShare: () -> Unit, onHelp: () -> Unit, onDone: () -> Unit) {
    val e = model.explanation
    ScrollColumn {
        RiskBanner(model.level, e.headline, e.summary)
        if (e.reasons.isNotEmpty()) {
            SectionTitle(stringResource(R.string.result_why))
            Bullets(e.reasons)
        }
        SectionTitle(stringResource(R.string.result_what_to_do))
        Bullets(e.actions)
        if (model.evidence.isNotEmpty()) {
            SectionTitle(stringResource(R.string.result_evidence))
            Bullets(model.evidence.map { "\"$it\"" })
        }
        Text(e.confidenceNote, style = MaterialTheme.typography.bodyMedium)
        if (e.limitations.isNotEmpty()) {
            SectionTitle(stringResource(R.string.result_limits))
            Bullets(e.limitations)
        }
        if (model.fromAlert) Text(stringResource(R.string.result_from_alert), style = MaterialTheme.typography.bodyMedium)
        if (model.level >= RiskLevel.CAUTION) {
            PrimaryButton(stringResource(R.string.result_call_trusted), onCallTrusted, icon = Icons.Filled.Call)
            SecondaryButton(stringResource(R.string.result_share), onShare, icon = Icons.Filled.Share)
            SecondaryButton(stringResource(R.string.result_what_should_i_do), onHelp, icon = Icons.Filled.Info)
        }
        SecondaryButton(stringResource(R.string.result_understood), onDone)
    }
}

@Composable
fun ProtectionScreen(
    status: ProtectionStatus,
    onOpenAccess: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onTogglePause: () -> Unit,
) {
    val automatic = when {
        !status.notificationAccessGranted -> stringResource(R.string.state_off)
        status.paused -> stringResource(R.string.state_paused)
        else -> stringResource(R.string.state_on)
    }
    ScrollColumn {
        ScreenTitle(stringResource(R.string.protection_title))
        InfoCard("${stringResource(R.string.protection_whatsapp)}: $automatic", stringResource(R.string.protection_whatsapp_detail))
        InfoCard("${stringResource(R.string.protection_email)}: $automatic", stringResource(R.string.protection_email_detail))
        InfoCard(
            "${stringResource(R.string.protection_manual)}: ${stringResource(R.string.state_always)}",
            stringResource(R.string.protection_manual_detail),
        )
        if (!status.canPostNotifications) {
            InfoCard(stringResource(R.string.protection_notifications_off), "", icon = Icons.Filled.Warning)
            PrimaryButton(stringResource(R.string.protection_allow_notifications), onOpenNotificationSettings)
        }
        if (!status.notificationAccessGranted) {
            PrimaryButton(stringResource(R.string.protection_enable_access), onOpenAccess)
        } else {
            SecondaryButton(
                stringResource(if (status.paused) R.string.protection_resume else R.string.protection_pause),
                onTogglePause,
            )
            SecondaryButton(stringResource(R.string.protection_manage_access), onOpenAccess)
        }
        SectionTitle(stringResource(R.string.protection_privacy_title))
        Text(stringResource(R.string.protection_privacy_body), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun HelpScreen(
    trustedName: String,
    trustedNumber: String,
    onSaveTrusted: (String, String) -> Unit,
    onPractice: () -> Unit,
    onOnboarding: () -> Unit,
) {
    var name by remember(trustedName) { mutableStateOf(trustedName) }
    var number by remember(trustedNumber) { mutableStateOf(trustedNumber) }
    var saved by remember { mutableStateOf(false) }
    ScrollColumn {
        ScreenTitle(stringResource(R.string.help_title))
        InfoCard(stringResource(R.string.help_already_paid_title), stringResource(R.string.help_already_paid_body), icon = Icons.Filled.Warning)
        SectionTitle(stringResource(R.string.help_rules_title))
        Text(stringResource(R.string.help_rules_body), style = MaterialTheme.typography.bodyLarge)
        SectionTitle(stringResource(R.string.help_trusted_title))
        Text(stringResource(R.string.help_trusted_body), style = MaterialTheme.typography.bodyMedium)
        OutlinedTextField(
            value = name, onValueChange = { name = it; saved = false },
            label = { Text(stringResource(R.string.help_trusted_name)) },
            textStyle = MaterialTheme.typography.bodyLarge, singleLine = true, modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = number, onValueChange = { number = it; saved = false },
            label = { Text(stringResource(R.string.help_trusted_number)) },
            textStyle = MaterialTheme.typography.bodyLarge, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth(),
        )
        PrimaryButton(stringResource(R.string.help_trusted_save), { onSaveTrusted(name, number); saved = true })
        if (trustedNumber.isNotEmpty()) {
            SecondaryButton(stringResource(R.string.help_trusted_remove), { onSaveTrusted("", ""); name = ""; number = "" })
        }
        if (saved) {
            Text(
                stringResource(R.string.help_trusted_saved), style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
        SecondaryButton(stringResource(R.string.help_practice), onPractice)
        SecondaryButton(stringResource(R.string.help_onboarding_again), onOnboarding)
    }
}
