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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fcl.plugin.mobileglues.R
import com.fcl.plugin.mobileglues.settings.AngleConfig
import com.fcl.plugin.mobileglues.settings.DepthClearFixMode
import com.fcl.plugin.mobileglues.settings.Fsr1Preset
import com.fcl.plugin.mobileglues.settings.GlVersion
import com.fcl.plugin.mobileglues.settings.HideMGEnvLevel
import com.fcl.plugin.mobileglues.settings.MGConfig
import com.fcl.plugin.mobileglues.settings.NoErrorConfig
import com.fcl.plugin.mobileglues.ui.AppController
import top.yukonga.miuix.kmp.basic.TabRowWithContour
import top.yukonga.miuix.kmp.theme.MiuixTheme

/*
 * Every row below maps to a top-level key `config/settings.cpp` reads with
 * `config_get_int`. There is no row for a setting the lib does not have: a
 * control that writes nothing the renderer will read is worse than no control.
 */

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
    val deviceInfo by controller.deviceInfo.collectAsStateWithLifecycle()
    val cacheBytes by controller.configStore.shaderCacheBytes.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxWidth()) {
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
                    onClick = controller::deleteShaderCache,
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
        }
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
        }
    }
}

/**
 * VISUAL — o que muda o que você vê.
 *
 * FSR is one preset, not a version + sharpening pair: `fsr1Setting` is the only
 * upscaling key the lib reads, and it is an enum (Disabled … Performance), not
 * a set of independent toggles.
 */
@Composable
fun MiuixVisualTab(controller: AppController, config: MGConfig) {
    Column(modifier = Modifier.fillMaxWidth()) {
        MiuixGroup(title = stringResource(R.string.settings_group_upscaling)) {
            OptionRow(
                title = stringResource(R.string.option_fsr1_preset),
                options = Fsr1Preset.entries,
                selected = config.fsr1Setting,
                onSelect = controller::selectFsr1,
            )
            MiuixHintText(stringResource(R.string.hint_fsr1_preset))
            MiuixWarnText(stringResource(R.string.warn_fsr_with_angle))
        }
    }
}

/** TOOLS — benchmark e reset. */
@Composable
fun MiuixToolsTab(controller: AppController, config: MGConfig) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Reset fica no fim de Tools porque é uma operação destrutiva e deve
        // exigir chegar até aqui. A confirmação (com aviso explícito) fica em
        // AppController.resetAllConfig().
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
