package com.fcl.plugin.mobileglues.ui.miuix

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fcl.plugin.mobileglues.R
import com.fcl.plugin.mobileglues.settings.MultidrawBenchQuality
import com.fcl.plugin.mobileglues.settings.MultidrawEntry
import com.fcl.plugin.mobileglues.ui.AppController
import com.fcl.plugin.mobileglues.ui.Responsive
import java.util.Locale
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.TabRowWithContour
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.extra.SuperDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme

/*
 * MultiDraw benchmark dialogs.
 *
 * Split out of the old MiuixMultidraw.kt, which also held the per-entry order
 * editor. That editor is gone: config/settings.cpp has no MultiDraw key left
 * to write an order to, so an order the user could drag around was a control
 * that moved nothing. What stays is the benchmark itself — it measures the
 * device and reports it, which needs no config key to be true.
 */
@Composable
private fun MiuixAngleSourceDialog(controller: AppController) {
    val prompt by controller.angleSourcePrompt.collectAsStateWithLifecycle()
    val last = rememberLastNonNull(prompt)

    SuperDialog(
        show = prompt != null,
        title = stringResource(R.string.md_angle_title),
        onDismissRequest = controller::dismissAngleSourcePrompt,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = Responsive.dialogMaxContentHeight())
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = stringResource(R.string.md_angle_intro),
                    fontSize = MiuixTheme.textStyles.body2.fontSize,
                    color = MiuixTheme.colorScheme.onSurfaceSecondary,
                )
                Spacer(Modifier.height(12.dp))
                if (last?.sources.isNullOrEmpty()) {
                    Text(
                        text = stringResource(R.string.md_angle_none),
                        fontSize = MiuixTheme.textStyles.body2.fontSize,
                        color = MiuixTheme.colorScheme.onSurfaceSecondary,
                    )
                } else {
                    Text(
                        text = AnnotatedString.fromHtml(stringResource(R.string.md_angle_trust)),
                        fontSize = MiuixTheme.textStyles.body2.fontSize,
                        color = MiuixTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(8.dp))
                    last?.sources?.forEach { source ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MiuixTheme.colorScheme.secondaryContainer)
                                .clickable { controller.confirmAngleSource(source) }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = source.label,
                                    style = MiuixTheme.textStyles.body1,
                                    color = MiuixTheme.colorScheme.onSurface,
                                )
                                if (source.packageName == last?.lastChosen) {
                                    Text(
                                        text = stringResource(R.string.md_angle_last_used),
                                        style = MiuixTheme.textStyles.footnote2,
                                        color = MiuixTheme.colorScheme.primary,
                                        modifier = Modifier.padding(start = 8.dp),
                                    )
                                }
                            }
                            Text(
                                text = source.packageName +
                                    (source.versionName?.let {
                                        " · " + stringResource(R.string.md_angle_source_version, it)
                                    } ?: ""),
                                style = MiuixTheme.textStyles.footnote2,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(
                    text = stringResource(R.string.dialog_negative),
                    onClick = controller::dismissAngleSourcePrompt,
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    text = stringResource(R.string.md_angle_skip),
                    onClick = controller::continueWithoutAngle,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
fun MiuixMultidrawBenchDialogs(controller: AppController) {
    val context = LocalContext.current
    val state by controller.benchState.collectAsStateWithLifecycle()

    MiuixAngleSourceDialog(controller)

    // 退场动画期间内容要从上一份非空值里取。
    val lastState = rememberLastNonNull(state)

    SuperDialog(
        show = state is AppController.BenchState.Running,
        title = stringResource(R.string.md_bench_running_title),
        onDismissRequest = null,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            ) {
                InfiniteProgressIndicator(color = MiuixTheme.colorScheme.primary)
                Spacer(Modifier.width(20.dp))
                val running = state as? AppController.BenchState.Running
                val attempt = running?.attempt ?: 1
                val smaller = running?.retryingAtSections
                Text(
                    text = when {
                        // 退让优先于「第几次测量」：上一趟整份作废了，说「第 2 次」
                        // 会让人以为前面那趟还算数。
                        smaller != null ->
                            stringResource(R.string.md_bench_running_smaller, smaller)
                        attempt > 1 -> stringResource(
                            R.string.md_bench_running_retry,
                            attempt,
                            AppController.BENCH_MAX_ATTEMPTS,
                        )
                        else -> stringResource(R.string.md_bench_running_msg)
                    },
                    fontSize = MiuixTheme.textStyles.body1.fontSize,
                    color = MiuixTheme.colorScheme.onSurfaceSecondary,
                )
            }
            // 进度是 native 那边的原子计数器；老渲染器没有，那就只剩上面那圈。
            val progress = (state as? AppController.BenchState.Running)?.progress
            if (progress != null) {
                val animated by animateFloatAsState(progress, label = "bench-progress")
                LinearProgressIndicator(
                    progress = animated,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp),
                )
            }
        }
    }

    val doneState = lastState as? AppController.BenchState.Done
    SuperDialog(
        show = state is AppController.BenchState.Done,
        title = stringResource(R.string.md_bench_result_title),
        onDismissRequest = controller::dismissBench,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = Responsive.dialogMaxContentHeight())
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = when (val target = doneState?.target) {
                        is AppController.BenchTarget.Entry -> stringResource(
                            R.string.md_bench_result_entry_intro,
                            target.entry.glFunction,
                        )
                        else -> stringResource(R.string.md_bench_result_all_intro)
                    },
                    fontSize = MiuixTheme.textStyles.body1.fontSize,
                    color = MiuixTheme.colorScheme.onSurfaceSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                // 驱动错了就不只是「不够准」，是整份名次搬不过去，得说在最前面。
                val angleNote = doneState?.angleNote
                if (angleNote != null) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = stringResource(angleNote.messageRes),
                        fontSize = MiuixTheme.textStyles.body2.fontSize,
                        color = MiuixTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                // 单个函数时函数名已经在开头那句里了，再标一次是废话。
                val showHeadings = doneState?.target !is AppController.BenchTarget.Entry
                doneState?.rankings?.forEach { (entry, ranking) ->
                    Spacer(Modifier.heightIn(min = 12.dp))
                    if (showHeadings) {
                        Text(
                            text = entry.glFunction,
                            style = MiuixTheme.textStyles.subtitle,
                            color = MiuixTheme.colorScheme.primary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    ranking.forEachIndexed { index, ranked ->
                        MiuixRankedRow(index + 1, ranked.item.label(context).toString(), ranked.relativeCost)
                    }
                    // 只有一个方案测得出时，「排名」名不副实——没有可比较的对象。
                    if (ranking.count { it.relativeCost != null } == 1) {
                        Text(
                            text = stringResource(R.string.md_bench_single_candidate),
                            style = MiuixTheme.textStyles.footnote2,
                            color = MiuixTheme.colorScheme.onSurfaceSecondary,
                        )
                    }
                    // 成色跟着它描述的那份排名走：抖的是某个函数，不是整场跑分。
                    MiuixBenchQualityNote(doneState.quality[entry])
                }
            }
            Spacer(Modifier.heightIn(min = 12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(
                    text = stringResource(R.string.ok),
                    onClick = controller::dismissBench,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                )
            }
        }
    }

    val failedState = lastState as? AppController.BenchState.Failed
    SuperDialog(
        show = state is AppController.BenchState.Failed,
        title = stringResource(R.string.md_bench_result_title),
        onDismissRequest = controller::dismissBench,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = failedState?.message.orEmpty(),
                fontSize = MiuixTheme.textStyles.body1.fontSize,
                color = MiuixTheme.colorScheme.onSurfaceSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.heightIn(min = 12.dp))
            TextButton(
                text = stringResource(R.string.ok),
                onClick = controller::dismissBench,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.textButtonColorsPrimary(),
            )
        }
    }
}

/** 一份排名底下的一行小字：这个函数测了几轮、抖动多大。抖动盖过名次差时得说出来。 */
@Composable
private fun MiuixBenchQualityNote(quality: MultidrawBenchQuality?) {
    if (quality == null || quality.rounds <= 0) return
    val noise = String.format(Locale.US, "%.1f", quality.noise * 100)
    Text(
        text = stringResource(R.string.md_bench_quality, quality.rounds, noise),
        style = MiuixTheme.textStyles.footnote2,
        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
    if (quality.noisy) {
        Text(
            text = stringResource(R.string.md_bench_noisy, quality.attempts, noise),
            style = MiuixTheme.textStyles.footnote2,
            color = MiuixTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun MiuixRankedRow(position: Int, label: String, relativeCost: Double?) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    ) {
        Text(
            text = "$position.",
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            modifier = Modifier.padding(end = 8.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurface,
            )
            if (relativeCost == null) {
                Text(
                    text = stringResource(R.string.md_bench_unmeasured),
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
        }
        if (relativeCost != null) {
            Text(
                text = stringResource(
                    R.string.md_bench_cost,
                    String.format(Locale.US, "%.2f", relativeCost),
                ),
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.primary,
            )
        }
    }
}
