@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.fcl.plugin.mobileglues.ui.miuix

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import java.util.Locale
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fcl.plugin.mobileglues.R
import com.fcl.plugin.mobileglues.settings.AngleConfig
import com.fcl.plugin.mobileglues.settings.DepthClearFixMode
import com.fcl.plugin.mobileglues.settings.GlVersion
import com.fcl.plugin.mobileglues.settings.GlslCacheScale
import com.fcl.plugin.mobileglues.settings.MultidrawEngine
import com.fcl.plugin.mobileglues.settings.MGConfig
import com.fcl.plugin.mobileglues.settings.NoErrorConfig
import com.fcl.plugin.mobileglues.settings.SpinnerOption
import com.fcl.plugin.mobileglues.ui.AppController
import com.fcl.plugin.mobileglues.ui.SettingsLoadState
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.TabRowWithContour
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

/**
 * 设置页（Miuix）。
 *
 * 分组、权限门、每一处警告与倒计时都与 MD3 皮肤一致——它们都在 [AppController] 里。
 * 不同的只是表达方式：这里的选项用 Miuix 的就地下拉，而不是弹出一个对话框。
 */
@Composable
fun MiuixSettingsPage(controller: AppController) {
    val auth by controller.auth.state.collectAsStateWithLifecycle()
    val loadState by controller.loadState.collectAsStateWithLifecycle()
    val config by controller.configStore.config.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { controller.ensureDeviceInfo() }

    val ready = auth.granted && loadState == SettingsLoadState.Ready && config != null
    var tab by rememberSaveable { mutableStateOf(0) }
    val tabs = listOf(
        stringResource(R.string.settings_tab_performance),
        stringResource(R.string.settings_tab_compatibility),
        stringResource(R.string.settings_tab_visual),
        stringResource(R.string.settings_tab_tools),
    )

    Column(modifier = Modifier.fillMaxSize()) {
        MiuixPageTitle(stringResource(R.string.nav_settings))

        // Sticky: fora do scroll, para não sumirem quando o usuário rola até
        // o fim da aba Tools. Só aparecem quando há configuração pronta para
        // exibir — durante a permission gate ou o load inicial, não.
        if (ready) {
            MiuixSettingsTabSelector(
                tabs = tabs,
                current = tab,
                onSelect = { tab = it },
            )
            Spacer(Modifier.height(8.dp))
        }

        Crossfade(
            targetState = auth.granted to ready,
            label = "settings-gate",
        ) { (granted, isReady) ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    // 甩到顶或底时给一下振动——HyperOS 的滚动到此为止就是这个手感。
                    .scrollEndHaptic()
                    .verticalScroll(rememberScrollState()),
            ) {
                when {
                    !granted -> PermissionGate(onGrant = controller::requestAccess)
                    isReady -> ConfigSectionsContent(
                        controller = controller,
                        config = config ?: MGConfig.Default,
                        currentTab = tab,
                    )
                    else -> MiuixLoading(modifier = Modifier.padding(top = 48.dp))
                }
                MiuixBottomSpacer()
            }
        }
    }
}

@Composable
private fun PermissionGate(onGrant: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MiuixScreenPadding, vertical = 20.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Text(
                text = stringResource(R.string.settings_gate_title),
                style = MiuixTheme.textStyles.title4,
                color = MiuixTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.settings_gate_msg),
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.padding(top = 8.dp),
            )
            Spacer(Modifier.height(20.dp))
            Button(onClick = onGrant, colors = ButtonDefaults.buttonColorsPrimary()) {
                Text(
                    text = stringResource(R.string.settings_gate_grant),
                    style = MiuixTheme.textStyles.button,
                    color = MiuixTheme.colorScheme.onPrimary,
                )
            }
        }
    }
}

@Composable
private fun ConfigSectionsContent(
    controller: AppController,
    config: MGConfig,
    currentTab: Int,
) {
    when (currentTab) {
        0 -> MiuixPerformanceTab(controller, config)
        1 -> MiuixCompatibilityTab(controller, config)
        2 -> MiuixVisualTab(controller, config)
        3 -> MiuixToolsTab(controller, config)
    }
}

/**
 * 缓存上限滑块。
 *
 * 拖动期间用本地档位，松手才交还给配置：档位 → MiB → 档位 的换算有取整，
 * 直接跟着配置画的话手指底下的滑块会自己抖。
 */
@Composable
internal fun GlslCacheSlider(controller: AppController, config: MGConfig, totalRamBytes: Long?) {
    val mebibytes = config.glslCache.mebibytesOrZero
    val base = totalRamBytes?.let { GlslCacheScale.baseCeiling(it) }
        ?: GlslCacheScale.MIN_UPPER_BOUND_MIB.toInt()
    val ceiling = maxOf(base, mebibytes)
    var dragPosition by remember { mutableStateOf<Int?>(null) }

    MiuixSliderRow(
        title = stringResource(R.string.option_glsl_cache),
        valueLabel = if (mebibytes > 0) {
            stringResource(R.string.option_glsl_cache_value, mebibytes)
        } else {
            stringResource(R.string.option_glsl_cache_off)
        },
        position = dragPosition ?: GlslCacheScale.positionFor(mebibytes, ceiling),
        steps = GlslCacheScale.STEPS,
        onPositionChange = { position ->
            dragPosition = position
            controller.setGlslCacheSliderPosition(position, ceiling)
        },
        onDragFinished = { dragPosition = null },
    )
}

/** 枚举 → 下拉行。选项顺序就是枚举的声明顺序，不会出现「选项与取值对不上」。 */
@Composable
internal fun <T : SpinnerOption> OptionRow(
    title: String,
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
) {
    val context = LocalContext.current
    MiuixDropdownRow(
        title = title,
        options = options.map { it.label(context).toString() },
        selectedIndex = options.indexOf(selected),
        onSelect = { onSelect(options[it]) },
    )
}
