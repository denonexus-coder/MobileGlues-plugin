// MobileGlues - ui/miuix/MiuixInfoComponents.kt
// Small building blocks for the settings/dashboard redesign.
// All non-breaking: pure additions to the ui/miuix package.
//
// Components:
//   MiuixHintText    — quiet secondary text, 1 line under an option
//   MiuixWarnText    — error-tinted text, for "incompatible with X" notes
//   MiuixImpactChip  — small rounded chip ("+40%", "-10%", "OFF")
//   MiuixDeltaText   — "+2.7 ▲" / "-4.1 ▼" with color
//   MiuixStatBar     — label + linear progress + value
//   MiuixStatRow     — aligned key/value line for dashboards
package com.fcl.plugin.mobileglues.ui.miuix

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.util.Locale

/**
 * One quiet line under an option. Same color as "onSurfaceVariantSummary" so
 * it reads as supporting text, not as another control.
 */
@Composable
fun MiuixHintText(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MiuixTheme.textStyles.footnote2,
        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        modifier = modifier.padding(start = 16.dp, top = 2.dp, end = 16.dp, bottom = 4.dp),
    )
}

/**
 * A line that must be read before touching the control above it — wrong driver,
 * incompatible combination, feature unavailable on this device. Error color so
 * it visually breaks from hints.
 */
@Composable
fun MiuixWarnText(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MiuixTheme.textStyles.footnote2,
        color = MiuixTheme.colorScheme.error,
        modifier = modifier.padding(start = 16.dp, top = 2.dp, end = 16.dp, bottom = 4.dp),
    )
}

enum class ImpactTone { Positive, Negative, Neutral }

/**
 * Tiny rounded chip. Use next to a slider or switch to state the measured cost
 * or benefit at the current value ("+40-80% FPS", "-5% FPS", "OFF"). Never
 * without a real number or a real range — an empty chip trains users to ignore
 * all of them.
 */
@Composable
fun MiuixImpactChip(
    text: String,
    tone: ImpactTone = ImpactTone.Neutral,
    modifier: Modifier = Modifier,
) {
    val bg = when (tone) {
        ImpactTone.Positive -> MiuixTheme.colorScheme.primaryContainer
        ImpactTone.Negative -> MiuixTheme.colorScheme.errorContainer
        ImpactTone.Neutral  -> MiuixTheme.colorScheme.secondaryContainer
    }
    val fg = when (tone) {
        ImpactTone.Positive -> MiuixTheme.colorScheme.onPrimaryContainer
        ImpactTone.Negative -> MiuixTheme.colorScheme.onErrorContainer
        ImpactTone.Neutral  -> MiuixTheme.colorScheme.onSecondaryContainer
    }
    Surface(shape = RoundedCornerShape(6.dp), color = bg, modifier = modifier) {
        Text(
            text = text,
            style = MiuixTheme.textStyles.footnote2,
            color = fg,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}

/**
 * Signed delta with a direction marker. Threshold 0.05 absorbs the rounding of
 * a single sample so a "+0.0" does not color as improvement. Suffix is appended
 * verbatim (e.g. "%", " FPS") so callers do not need a second formatter.
 */
@Composable
fun MiuixDeltaText(
    delta: Float,
    suffix: String = "",
    modifier: Modifier = Modifier,
) {
    val positive = delta > 0.05f
    val negative = delta < -0.05f
    val text = when {
        positive -> String.format(Locale.US, "+%.1f%s \u25B2", delta, suffix)
        negative -> String.format(Locale.US, "%.1f%s \u25BC", delta, suffix)
        else     -> "\u2014"
    }
    val color = when {
        positive -> MiuixTheme.colorScheme.primary
        negative -> MiuixTheme.colorScheme.error
        else     -> MiuixTheme.colorScheme.onSurfaceVariantSummary
    }
    Text(
        text = text,
        style = MiuixTheme.textStyles.footnote2,
        color = color,
        modifier = modifier,
    )
}

/**
 * Labeled bar. fraction is clamped to [0, 1] because a live counter can be
 * briefly over 1 while two threads update it. valueLabel is passed pre-formatted
 * so the caller decides whether it wants "47%" or "0.47" or "1.2 GB / 2.7 GB".
 */
@Composable
fun MiuixStatBar(
    label: String,
    fraction: Float,
    valueLabel: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = valueLabel,
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.primary,
            )
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = fraction.coerceIn(0f, 1f),
            modifier = Modifier.fillMaxWidth().height(6.dp),
        )
    }
}

/**
 * One row of a dashboard: label on the left, value on the right. Value carries
 * primary color so a scan of the whole card reads the values, not the labels.
 */
@Composable
fun MiuixStatRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = value,
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.primary,
        )
    }
}
