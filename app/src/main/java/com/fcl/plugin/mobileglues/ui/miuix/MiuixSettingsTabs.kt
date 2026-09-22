@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.fcl.plugin.mobileglues.ui.miuix

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fcl.plugin.mobileglues.R
import com.fcl.plugin.mobileglues.settings.AngleConfig
import com.fcl.plugin.mobileglues.settings.BufferUploadMode
import com.fcl.plugin.mobileglues.settings.DepthClearFixMode
import com.fcl.plugin.mobileglues.settings.GlVersion
import com.fcl.plugin.mobileglues.settings.HideMGEnvLevel
import com.fcl.plugin.mobileglues.settings.MGConfig
import com.fcl.plugin.mobileglues.settings.MaxAnisotropyOverride
import com.fcl.plugin.mobileglues.settings.MultidrawEngine
import com.fcl.plugin.mobileglues.settings.NoErrorConfig
import com.fcl.plugin.mobileglues.settings.TextureSwizzleMode
import com.fcl.plugin.mobileglues.ui.AppController
import java.util.Locale
import kotlin.math.roundToInt
import top.yukonga.miuix.kmp.basic.TabRowWithContour
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** TabRow de topo da página de Settings. */
@Composable
fun MiuixSettingsTabSelector(
    tabs: List<String>,
    current: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    TabRowWithContour(
        tabs = tabs,
        selectedTabIndex = current,
        onTabSelected = onSelect,
        modifier = modifier.fillMaxWidth().padding(horizontal = MiuixScreenPadding),
    )
}

/** PERFORMANCE — o que faz o jogo rodar mais rápido. */
@Composable
fun MiuixPerformanceTab(controller: AppController, config: MGConfig) {
    val context = LocalContext.current
    val deviceInfo by controller.deviceInfo.collectAsStateWithLifecycle()
    val cacheBytes by controller.configStore.glslCacheBytes.collectAsStateWithLifecycle()
    var multidrawExpanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        MiuixGroup(title = stringResource(R.string.settings_group_advanced)) {
            MiuixDropdownRow(
                title = stringResource(R.string.option_multidraw_engine_title),
                options = MultidrawEngine.entries.map { it.label(context).toString() },
                selectedIndex = MultidrawEngine.entries.indexOf(config.multidrawEngine),
                onSelect = { i -> controller.selectMultidrawEngine(MultidrawEngine.entries[i]) },
            )
            MiuixHintText(stringResource(R.string.hint_multidraw_engine))
            MiuixExpandableSection(
                title = stringResource(R.string.option_multidraw),
                summary = miuixMultidrawSummary(config.multidraw),
                expanded = multidrawExpanded,
                onToggle = { multidrawExpanded = !multidrawExpanded },
            ) {
                MiuixMultidrawOrderContent(controller, config)
            }
        }

        MiuixGroup(title = stringResource(R.string.settings_group_cache)) {
            GlslCacheSlider(controller, config, deviceInfo?.totalRamBytes)
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
            MiuixSwitchRow(
                title = stringResource(R.string.option_program_binary_cache),
                summary = stringResource(R.string.option_program_binary_cache_desc),
                checked = config.useProgramBinaryCache,
                onCheckedChange = { v ->
                    controller.configStore.update { it.copy(useProgramBinaryCache = v) }
                },
            )
        }

        MiuixGroup(title = stringResource(R.string.settings_group_advanced_ext)) {
            MiuixSwitchRow(
                title = stringResource(R.string.option_ext_cs),
                checked = config.extComputeShader,
                onCheckedChange = controller::setExtComputeShader,
            )
            MiuixWarnText(stringResource(R.string.warn_ext_compute_shader))
            MiuixSwitchRow(
                title = stringResource(R.string.option_disable_compute_weak_gpu),
                checked = config.disableComputeOnWeakGpu,
                onCheckedChange = { v ->
                    controller.configStore.update { it.copy(disableComputeOnWeakGpu = v) }
                },
            )
            MiuixHintText(stringResource(R.string.hint_disable_compute_weak_gpu))
            MiuixDropdownRow(
                title = stringResource(R.string.option_buffer_upload_mode),
                options = BufferUploadMode.entries.map { it.label(context).toString() },
                selectedIndex = BufferUploadMode.entries.indexOf(config.bufferUploadMode),
                onSelect = { i ->
                    controller.configStore.update { it.copy(bufferUploadMode = BufferUploadMode.entries[i]) }
                },
            )
        }

        MiuixEngineSubmodesSection(controller, config)
    }
}

/** COMPATIBILITY — o que faz jogos chatos rodarem. */
@Composable
fun MiuixCompatibilityTab(controller: AppController, config: MGConfig) {
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxWidth()) {
        MiuixGroup(title = stringResource(R.string.settings_group_render)) {
            OptionRow(
                title = stringResource(R.string.option_angle),
                options = AngleConfig.entries,
                selected = config.angle,
                onSelect = controller::selectAngle,
            )
            OptionRow(
                title = stringResource(R.string.option_no_error),
                options = NoErrorConfig.entries,
                selected = config.noError,
                onSelect = controller::selectNoError,
            )
            OptionRow(
                title = stringResource(R.string.option_angle_clear_workaround),
                options = DepthClearFixMode.entries,
                selected = config.depthClearFix,
                onSelect = controller::selectDepthClearFix,
            )
            MiuixHintText(stringResource(R.string.hint_depth_clear_fix))
            OptionRow(
                title = stringResource(R.string.option_custom_gl_version),
                options = GlVersion.entries,
                selected = config.glVersion,
                onSelect = controller::selectGlVersion,
            )
            MiuixDropdownRow(
                title = stringResource(R.string.option_hide_mg_env_level),
                options = HideMGEnvLevel.entries.map { it.label(context).toString() },
                selectedIndex = HideMGEnvLevel.entries.indexOf(config.hideMGEnvLevel),
                onSelect = { i ->
                    controller.configStore.update { it.copy(hideMGEnvLevel = HideMGEnvLevel.entries[i]) }
                },
            )
            MiuixHintText(stringResource(R.string.hint_hide_mg_env))
        }

        MiuixGroup(title = stringResource(R.string.settings_group_ext)) {
            MiuixSwitchRow(
                title = stringResource(R.string.option_ext_timer_query),
                checked = !config.extTimerQuery,
                onCheckedChange = controller::setExtTimerQueryDisabled,
            )
            MiuixSwitchRow(
                title = stringResource(R.string.option_ext_direct_state_access),
                checked = config.extDirectStateAccess,
                onCheckedChange = controller::setExtDirectStateAccess,
            )
            MiuixHintText(stringResource(R.string.hint_ext_dsa))
            MiuixSwitchRow(
                title = stringResource(R.string.option_ext_gl43),
                checked = config.enableExtGL43,
                onCheckedChange = { v ->
                    controller.configStore.update { it.copy(enableExtGL43 = v) }
                },
            )
            MiuixWarnText(stringResource(R.string.warn_ext_gl43))
        }

        MiuixGroup(title = stringResource(R.string.settings_group_advanced_ext)) {
            MiuixSwitchRow(
                title = stringResource(R.string.option_force_gl_get_error_skip),
                checked = config.forceGlGetErrorSkip,
                onCheckedChange = { v ->
                    controller.configStore.update { it.copy(forceGlGetErrorSkip = v) }
                },
            )
            MiuixSwitchRow(
                title = stringResource(R.string.option_force_depth_precision_fix),
                checked = config.forceDepthPrecisionFix,
                onCheckedChange = { v ->
                    controller.configStore.update { it.copy(forceDepthPrecisionFix = v) }
                },
            )
            MiuixHintText(stringResource(R.string.hint_force_depth_precision))
        }
    }
}

/** VISUAL — o que muda o que você vê. */
@Composable
fun MiuixVisualTab(controller: AppController, config: MGConfig) {
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxWidth()) {
        MiuixGroup(title = stringResource(R.string.settings_group_render)) {
            MiuixSwitchRow(
                title = stringResource(R.string.option_enable_fsr1),
                checked = config.fsr1Enabled,
                onCheckedChange = controller::setFsr1,
            )
            MiuixWarnText(stringResource(R.string.warn_fsr_with_angle))
            AnimatedVisibility(
                visible = config.fsr1Enabled,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column {
                    val versions = listOf(1, 2)
                    MiuixDropdownRow(
                        title = stringResource(R.string.option_fsr1_version),
                        options = listOf(
                            stringResource(R.string.option_fsr1_version_1),
                            stringResource(R.string.option_fsr1_version_2),
                        ),
                        selectedIndex = versions.indexOf(config.fsr1Version).takeIf { it >= 0 } ?: 1,
                        onSelect = { i ->
                            controller.configStore.update { it.copy(fsr1Version = versions[i]) }
                        },
                    )
                    MiuixHintText(stringResource(R.string.hint_fsr_version))
                    MiuixSwitchRow(
                        title = stringResource(R.string.option_fsr_enable_sharpening),
                        summary = stringResource(R.string.option_fsr_enable_sharpening_desc),
                        checked = config.fsrEnableSharpening,
                        onCheckedChange = { v ->
                            controller.configStore.update { it.copy(fsrEnableSharpening = v) }
                        },
                    )
                    AnimatedVisibility(
                        visible = config.fsrEnableSharpening && config.fsr1Version == 1,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut(),
                    ) {
                        MiuixSliderRow(
                            title = stringResource(R.string.option_fsr1_sharpness),
                            valueLabel = String.format(Locale.US, "%.2f", config.fsr1Sharpness),
                            position = (config.fsr1Sharpness * 100).roundToInt(),
                            steps = 100,
                            onPositionChange = { pos ->
                                controller.configStore.update { it.copy(fsr1Sharpness = pos / 100f) }
                            },
                            onDragFinished = {},
                        )
                    }
                    AnimatedVisibility(
                        visible = config.fsrEnableSharpening && config.fsr1Version == 2,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut(),
                    ) {
                        MiuixSliderRow(
                            title = stringResource(R.string.option_fsr2_sharpness),
                            valueLabel = String.format(Locale.US, "%.2f", config.fsr2Sharpness),
                            position = (config.fsr2Sharpness * 100).roundToInt(),
                            steps = 100,
                            onPositionChange = { pos ->
                                controller.configStore.update { it.copy(fsr2Sharpness = pos / 100f) }
                            },
                            onDragFinished = {},
                        )
                    }
                }
            }
        }

        MiuixGroup(title = stringResource(R.string.settings_group_advanced_ext)) {
            MiuixDropdownRow(
                title = stringResource(R.string.option_texture_swizzle_mode),
                options = TextureSwizzleMode.entries.map { it.label(context).toString() },
                selectedIndex = TextureSwizzleMode.entries.indexOf(config.textureSwizzleMode),
                onSelect = { i ->
                    controller.configStore.update { it.copy(textureSwizzleMode = TextureSwizzleMode.entries[i]) }
                },
            )
            MiuixHintText(stringResource(R.string.hint_texture_swizzle))
            MiuixDropdownRow(
                title = stringResource(R.string.option_max_anisotropy),
                options = MaxAnisotropyOverride.entries.map { it.label(context).toString() },
                selectedIndex = MaxAnisotropyOverride.entries.indexOf(config.maxAnisotropyOverride),
                onSelect = { i ->
                    controller.configStore.update { it.copy(maxAnisotropyOverride = MaxAnisotropyOverride.entries[i]) }
                },
            )
            MiuixHintText(stringResource(R.string.hint_max_anisotropy))
        }
    }
}

/** TOOLS — diagnósticos, benchmark, logging e reset. */
@Composable
fun MiuixToolsTab(controller: AppController, config: MGConfig) {
    Column(modifier = Modifier.fillMaxWidth()) {
        MiuixDebugSection(controller, config)

        // Reset fica no fim de Tools — depois dos diagnósticos — porque é uma
        // operação destrutiva e deve exigir chegar até aqui. A confirmação (com
        // aviso explícito) fica em AppController.resetAllConfig().
        MiuixGroup(title = stringResource(R.string.settings_group_reset)) {
            MiuixArrowRow(
                title = stringResource(R.string.option_reset_to_defaults),
                summary = stringResource(R.string.option_reset_to_defaults_desc),
                titleColor = MiuixTheme.colorScheme.error,
                onClick = controller::resetAllConfig,
            )
        }
    }
}
