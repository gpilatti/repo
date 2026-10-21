package pt.guardiao.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import pt.guardiao.app.R

/**
 * Short onboarding: one idea per step, permissions asked one at a time and only
 * after explaining the benefit. Every permission step can be skipped; the app
 * still works for manual checks.
 */
@Composable
fun OnboardingScreen(
    step: Int,
    onStep: (Int) -> Unit,
    onRequestNotifications: () -> Unit,
    onOpenNotificationAccess: () -> Unit,
    onShowPractice: () -> Unit,
    onFinish: () -> Unit,
) {
    val next = { onStep(step + 1) }
    ScrollColumn {
        Text(
            "${step + 1} / $STEPS",
            style = MaterialTheme.typography.bodyMedium,
        )
        when (step) {
            0 -> Step(R.string.onb_1_title, R.string.onb_1_body) { PrimaryButton(stringResource(R.string.onb_continue), next) }
            1 -> Step(R.string.onb_2_title, R.string.onb_2_body) { PrimaryButton(stringResource(R.string.onb_continue), next) }
            2 -> Step(R.string.onb_3_title, R.string.onb_3_body) { PrimaryButton(stringResource(R.string.onb_continue), next) }
            3 -> Step(R.string.onb_4_title, R.string.onb_4_body) {
                PrimaryButton(stringResource(R.string.onb_4_allow), { onRequestNotifications(); next() })
                NotNow(next)
            }
            4 -> Step(R.string.onb_5_title, R.string.onb_5_body) {
                PrimaryButton(stringResource(R.string.onb_5_open), { onOpenNotificationAccess(); next() })
                NotNow(next)
            }
            5 -> Step(R.string.onb_6_title, R.string.onb_6_body) {
                PrimaryButton(stringResource(R.string.onb_6_show), { onShowPractice(); next() })
                NotNow(next)
            }
            else -> Step(R.string.onb_7_title, R.string.onb_7_body) {
                PrimaryButton(stringResource(R.string.onb_finish), onFinish)
            }
        }
    }
}

private const val STEPS = 7

@Composable
private fun Step(title: Int, body: Int, actions: @Composable () -> Unit) {
    ScreenTitle(stringResource(title))
    Text(stringResource(body), style = MaterialTheme.typography.bodyLarge)
    actions()
}

@Composable
private fun NotNow(onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = BigTouch)) {
        Text(stringResource(R.string.onb_not_now), style = MaterialTheme.typography.labelLarge)
    }
}
