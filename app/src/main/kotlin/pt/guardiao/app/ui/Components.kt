package pt.guardiao.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import pt.guardiao.app.R
import pt.guardiao.core.model.RiskLevel

/** Minimum touch target for primary actions: larger than the 48dp guideline. */
val BigTouch = 64.dp

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    Button(onClick = onClick, modifier = modifier.fillMaxWidth().heightIn(min = BigTouch)) {
        if (icon != null) {
            Icon(icon, contentDescription = null)
            Spacer(Modifier.width(12.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    OutlinedButton(onClick = onClick, modifier = modifier.fillMaxWidth().heightIn(min = BigTouch)) {
        if (icon != null) {
            Icon(icon, contentDescription = null)
            Spacer(Modifier.width(12.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun ScreenTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.headlineMedium,
        modifier = Modifier.padding(bottom = 8.dp).semantics { heading() },
    )
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp).semantics { heading() },
    )
}

@Composable
fun InfoCard(title: String, body: String, container: Color = MaterialTheme.colorScheme.surfaceVariant, content: Color = MaterialTheme.colorScheme.onSurfaceVariant, icon: ImageVector? = null) {
    Card(
        colors = CardDefaults.cardColors(containerColor = container, contentColor = content),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.Top) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(36.dp))
                Spacer(Modifier.width(16.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                if (body.isNotEmpty()) Text(body, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

/** Risk banner: colour, icon and a written label, so colour is never the only cue. */
@Composable
fun RiskBanner(level: RiskLevel, headline: String, summary: String) {
    val style = riskStyle(level)
    val icon = when (level) {
        RiskLevel.CRITICAL, RiskLevel.HIGH -> Icons.Filled.Warning
        RiskLevel.CAUTION -> Icons.Filled.Info
        RiskLevel.NO_WARNING -> Icons.Filled.CheckCircle
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = style.container, contentColor = style.content),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(40.dp))
                Spacer(Modifier.width(12.dp))
                Text(levelLabel(level), style = MaterialTheme.typography.titleLarge)
            }
            Text(headline, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
            Text(summary, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
fun levelLabel(level: RiskLevel): String = stringResource(
    when (level) {
        RiskLevel.CRITICAL -> R.string.level_critical
        RiskLevel.HIGH -> R.string.level_high
        RiskLevel.CAUTION -> R.string.level_caution
        RiskLevel.NO_WARNING -> R.string.level_none
    },
)

@Composable
fun Bullets(items: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { item ->
            Row {
                Text("•", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.width(10.dp))
                Text(item, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
