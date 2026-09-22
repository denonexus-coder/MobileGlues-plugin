package com.fcl.plugin.mobileglues.ui.miuix

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fcl.plugin.mobileglues.DeviceInfo
import com.fcl.plugin.mobileglues.R
import com.fcl.plugin.mobileglues.settings.MGConfig
import com.fcl.plugin.mobileglues.ui.AppController
import com.fcl.plugin.mobileglues.ui.AppSubPage
import com.fcl.plugin.mobileglues.ui.AppTab
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 首页（Miuix）— dashboard.
 *
 * Layout: wordmark → auth pill → status card → device card → actions card →
 * benchmark nudge (only when the user hasn't tuned MultiDraw yet). Every block
 * is a separate card with a section title, so a glance down the page reads as
 * "am I authorized? what's my config? what's my device? what can I do next?"
 * instead of one long paragraph of settings.
 */
@Composable
fun MiuixHomePage(controller: AppController) {
    val auth by controller.auth.state.collectAsStateWithLifecycle()
    val deviceInfo by controller.deviceInfo.collectAsStateWithLifecycle()
    val config by controller.configStore.config.collectAsStateWithLifecycle()
    val cacheBytes by controller.configStore.glslCacheBytes.collectAsStateWithLifecycle()
    val untuned by controller.multidrawUntuned.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { controller.ensureDeviceInfo() }
    // 启动次数记在 MG 目录里，未授权时读不到；授权建立之后再问一次。
    LaunchedEffect(auth.granted) {
        if (auth.granted) controller.maybeShowSponsorPrompt()
    }

    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }

    val scroll = rememberScrollState()
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll),
    ) {
        Spacer(Modifier.height(24.dp))

        EnterUp(entered, delayMillis = 0) { Wordmark(controller.appVersionName) }

        Spacer(Modifier.height(20.dp))

        EnterUp(entered, delayMillis = 80) {
            AuthPill(
                granted = auth.granted,
                onClick = { if (!auth.granted) controller.requestAccess() },
            )
        }

        Spacer(Modifier.height(20.dp))

        val cfg = config
        if (auth.granted && cfg != null) {
            EnterUp(entered, delayMillis = 140) { StatusCard(cfg) }
            Spacer(Modifier.height(12.dp))
        }

        EnterUp(entered, delayMillis = 200) { DeviceCard(deviceInfo) }

        if (auth.granted) {
            Spacer(Modifier.height(12.dp))
            EnterUp(entered, delayMillis = 260) { ActionsCard(controller, cacheBytes) }
        }

        // 排序还是出厂那份，没人量过这台设备。两层动画各管各的：外层跟着首页那串
        // 进场依次上来，内层负责跑完分采用之后自己收走。
        if (untuned && auth.granted) {
            Spacer(Modifier.height(12.dp))
            EnterUp(entered, delayMillis = 320) {
                BenchmarkNudge(
                    onClick = {
                        controller.runMultidrawBench(AppController.BenchTarget.AllEntries)
                    },
                )
            }
        }

        Spacer(Modifier.height(72.dp))
    }
}

@Composable
private fun Wordmark(version: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = MiuixTheme.colorScheme.onBackground)) { append("Mobile") }
                withStyle(SpanStyle(color = MiuixTheme.colorScheme.primary)) { append("Glues") }
            },
            style = MiuixTheme.textStyles.title1,
            fontSize = 40.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
        Text(
            text = "v$version",
            style = MiuixTheme.textStyles.footnote1,
            color = MiuixTheme.colorScheme.onBackgroundVariant,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
private fun AuthPill(granted: Boolean, onClick: () -> Unit) {
    val container by animateColorAsState(
        targetValue = if (granted) {
            MiuixTheme.colorScheme.primaryContainer
        } else {
            MiuixTheme.colorScheme.errorContainer
        },
        animationSpec = tween(320),
        label = "pill-container",
    )
    val content by animateColorAsState(
        targetValue = if (granted) {
            MiuixTheme.colorScheme.onPrimaryContainer
        } else {
            MiuixTheme.colorScheme.error
        },
        animationSpec = tween(320),
        label = "pill-content",
    )

    Surface(shape = CircleShape, color = container) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(CircleShape)
                .clickable(enabled = !granted, onClick = onClick)
                .padding(horizontal = 20.dp, vertical = 10.dp),
        ) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(content))
            Spacer(Modifier.size(10.dp))
            Text(
                text = stringResource(
                    if (granted) R.string.home_status_granted else R.string.home_status_denied,
                ),
                style = MiuixTheme.textStyles.body2,
                color = content,
            )
        }
    }
}

/**
 * Current renderer configuration as three aligned key/value rows. Uses the
 * same vocabulary as the settings screen so a value read here can be found
 * there without translation.
 */
@Composable
private fun StatusCard(config: MGConfig) {
    val context = LocalContext.current

    // MultiDraw: engine name, plus the order summary only when that engine
    // actually honours an order (Legacy). VMDI/IMDBI manage dispatch internally.
    val engine = config.multidrawEngine.label(context).toString()
    val multidrawValue = if (config.multidrawEngine.usesBackendOrdering) {
        "$engine · ${miuixMultidrawSummary(config.multidraw)}"
    } else {
        engine
    }

    val fsrValue = if (config.fsr1Enabled) {
        "${stringResource(R.string.home_value_on)} · v${config.fsr1Version}"
    } else {
        stringResource(R.string.home_value_off)
    }

    val cacheValue = if (config.glslCache.mebibytesOrZero > 0) {
        stringResource(R.string.option_glsl_cache_value, config.glslCache.mebibytesOrZero)
    } else {
        stringResource(R.string.option_glsl_cache_off)
    }

    MiuixGroup(title = stringResource(R.string.home_section_status)) {
        MiuixStatRow(stringResource(R.string.home_row_multidraw), multidrawValue)
        MiuixStatRow(stringResource(R.string.home_row_fsr), fsrValue)
        MiuixStatRow(stringResource(R.string.home_row_cache), cacheValue)
    }
}

@Composable
private fun DeviceCard(info: DeviceInfo?) {
    val unknown = stringResource(R.string.home_device_unknown)
    val gpu = info?.gpuRenderer?.takeIf { it.isNotBlank() } ?: unknown
    val gles = info?.glesVersion?.takeIf { it.isNotBlank() } ?: unknown
    val ram = info?.let {
        stringResource(R.string.home_ram_value, it.totalRamBytes / GIBIBYTE)
    } ?: unknown

    MiuixGroup(title = stringResource(R.string.home_section_device)) {
        MiuixStatRow(stringResource(R.string.home_device_gpu), gpu)
        MiuixStatRow(stringResource(R.string.home_device_gles), gles)
        MiuixStatRow(stringResource(R.string.home_device_ram), ram)
    }
}

@Composable
private fun ActionsCard(controller: AppController, cacheBytes: Long?) {
    MiuixGroup(title = stringResource(R.string.home_section_actions)) {
        MiuixArrowRow(
            title = stringResource(R.string.nav_settings),
            summary = stringResource(R.string.home_config_hint),
            onClick = { controller.navigateTab(AppTab.Settings) },
        )
        MiuixArrowRow(
            title = stringResource(R.string.info_mg_info),
            onClick = { controller.openSubPage(AppSubPage.GlInfo) },
        )
        MiuixArrowRow(
            title = stringResource(R.string.md_bench_run_all),
            onClick = {
                controller.runMultidrawBench(AppController.BenchTarget.AllEntries)
            },
        )
        // 没有缓存文件时不摆一个删不了东西的按钮：它按需浮现，删完收回。
        AnimatedVisibility(
            visible = cacheBytes != null,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            MiuixArrowRow(
                title = stringResource(
                    R.string.option_glsl_cache_delete,
                    controller.formatCacheSize(cacheBytes ?: 0L),
                ),
                titleColor = MiuixTheme.colorScheme.error,
                onClick = controller::deleteGlslCache,
            )
        }
    }
}

/**
 * 「还没量过这台设备」的提示。
 *
 * 说一句、给一个按钮，不解释原理——首页不是讲道理的地方。一旦排序不再是默认（自己拖过，
 * 或采用了跑分结果），它自己就不出现了，所以不需要「不再提示」。
 */
@Composable
private fun BenchmarkNudge(onClick: () -> Unit) {
    MiuixGroup(
        title = stringResource(R.string.home_section_actions).let { null },
        modifier = Modifier.padding(horizontal = 0.dp),
    ) {
        MiuixArrowRow(
            title = stringResource(R.string.md_home_untuned),
            summary = stringResource(R.string.md_home_untuned_action),
            titleColor = MiuixTheme.colorScheme.primary,
            onClick = onClick,
        )
    }
}

@Composable
private fun EnterUp(visible: Boolean, delayMillis: Int, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(durationMillis = 420, delayMillis = delayMillis)) +
            slideInVertically(
                animationSpec = tween(
                    durationMillis = 420,
                    delayMillis = delayMillis,
                    easing = FastOutSlowInEasing,
                ),
            ) { it / 3 },
        exit = ExitTransition.None,
    ) {
        content()
    }
}

private const val GIBIBYTE = 1024.0 * 1024.0 * 1024.0
