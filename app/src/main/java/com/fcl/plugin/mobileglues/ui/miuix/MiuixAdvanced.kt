@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.fcl.plugin.mobileglues.ui.miuix

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fcl.plugin.mobileglues.R
import com.fcl.plugin.mobileglues.settings.BufferUploadMode
import com.fcl.plugin.mobileglues.settings.HideMGEnvLevel
import com.fcl.plugin.mobileglues.settings.MGConfig
import com.fcl.plugin.mobileglues.settings.MaxAnisotropyOverride
import com.fcl.plugin.mobileglues.settings.SpinnerOption
import com.fcl.plugin.mobileglues.settings.TextureSwizzleMode
import com.fcl.plugin.mobileglues.ui.AppController
import kotlin.math.roundToInt
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun MiuixAdvancedSection(controller: AppController, config: MGConfig) {
    val context = LocalContext.current

    MiuixGroup(title = stringResource(R.string.settings_group_advanced_ext)) {
        MiuixDropdownRow(
            title = stringResource(R.string.option_hide_mg_env_level, "Hide MG env"),
            options = HideMGEnvLevel.entries.map { it.label(context).toString() },
            selectedIndex = HideMGEnvLevel.entries.indexOf(config.hideMGEnvLevel),
            onSelect = { i -> controller.configStore.update { it.copy(hideMGEnvLevel = HideMGEnvLevel.entries[i]) } },
        )
        MiuixSwitchRow(
            title = stringResource(R.string.option_ext_gl43),
            checked = config.enableExtGL43,
            onCheckedChange = { controller.configStore.update { c -> c.copy(enableExtGL43 = it) } },
        )
        MiuixSwitchRow(
            title = stringResource(R.string.option_force_gl_get_error_skip),
            checked = config.forceGlGetErrorSkip,
            onCheckedChange = { controller.configStore.update { c -> c.copy(forceGlGetErrorSkip = it) } },
        )
        MiuixDropdownRow(
            title = stringResource(R.string.option_buffer_upload_mode, "Buffer upload"),
            options = BufferUploadMode.entries.map { it.label(context).toString() },
            selectedIndex = BufferUploadMode.entries.indexOf(config.bufferUploadMode),
            onSelect = { i -> controller.configStore.update { it.copy(bufferUploadMode = BufferUploadMode.entries[i]) } },
        )
        MiuixDropdownRow(
            title = stringResource(R.string.option_texture_swizzle_mode, "Texture swizzle"),
            options = TextureSwizzleMode.entries.map { it.label(context).toString() },
            selectedIndex = TextureSwizzleMode.entries.indexOf(config.textureSwizzleMode),
            onSelect = { i -> controller.configStore.update { it.copy(textureSwizzleMode = TextureSwizzleMode.entries[i]) } },
        )
        MiuixDropdownRow(
            title = stringResource(R.string.option_max_anisotropy, "Max anisotropy"),
            options = MaxAnisotropyOverride.entries.map { it.label(context).toString() },
            selectedIndex = MaxAnisotropyOverride.entries.indexOf(config.maxAnisotropyOverride),
            onSelect = { i -> controller.configStore.update { it.copy(maxAnisotropyOverride = MaxAnisotropyOverride.entries[i]) } },
        )
        MiuixSwitchRow(
            title = stringResource(R.string.option_force_depth_precision_fix),
            checked = config.forceDepthPrecisionFix,
            onCheckedChange = { controller.configStore.update { c -> c.copy(forceDepthPrecisionFix = it) } },
        )
    }
}

@Composable
fun MiuixDebugSection(controller: AppController, config: MGConfig) {
    val diag = config.diag

    MiuixGroup(title = stringResource(R.string.settings_group_debug)) {
        MiuixSwitchRow(
            title = stringResource(R.string.diag_enabled),
            checked = diag.enabled,
            onCheckedChange = { v -> controller.configStore.update { it.copy(diag = it.diag.copy(enabled = v)) } },
        )
    }

    AnimatedVisibility(
        visible = diag.enabled,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            MiuixGroup(title = stringResource(R.string.diag_overlay_title)) {
                MiuixSwitchRow(stringResource(R.string.diag_overlay_frame_profiler), diag.overlay.frameProfiler) { v ->
                    controller.configStore.update { it.copy(diag = it.diag.copy(overlay = it.diag.overlay.copy(frameProfiler = v))) }
                }
                MiuixSwitchRow(stringResource(R.string.diag_overlay_draw_call_count), diag.overlay.drawCallCount) { v ->
                    controller.configStore.update { it.copy(diag = it.diag.copy(overlay = it.diag.overlay.copy(drawCallCount = v))) }
                }
                MiuixSwitchRow(stringResource(R.string.diag_overlay_shader_recompiles), diag.overlay.shaderRecompiles) { v ->
                    controller.configStore.update { it.copy(diag = it.diag.copy(overlay = it.diag.overlay.copy(shaderRecompiles = v))) }
                }
                MiuixSwitchRow(stringResource(R.string.diag_overlay_backend_tier), diag.overlay.backendTier) { v ->
                    controller.configStore.update { it.copy(diag = it.diag.copy(overlay = it.diag.overlay.copy(backendTier = v))) }
                }
                MiuixSwitchRow(stringResource(R.string.diag_overlay_cpu_gpu_load), diag.overlay.cpuGpuLoad) { v ->
                    controller.configStore.update { it.copy(diag = it.diag.copy(overlay = it.diag.overlay.copy(cpuGpuLoad = v))) }
                }
            }
            MiuixGroup(title = stringResource(R.string.diag_logging_title)) {
                MiuixSwitchRow(stringResource(R.string.diag_logging_backend_selection), diag.logging.backendSelection) { v ->
                    controller.configStore.update { it.copy(diag = it.diag.copy(logging = it.diag.logging.copy(backendSelection = v))) }
                }
                MiuixSwitchRow(stringResource(R.string.diag_logging_shader_recompiles), diag.logging.shaderRecompiles) { v ->
                    controller.configStore.update { it.copy(diag = it.diag.copy(logging = it.diag.logging.copy(shaderRecompiles = v))) }
                }
                MiuixSwitchRow(stringResource(R.string.diag_logging_draw_call_count), diag.logging.drawCallCount) { v ->
                    controller.configStore.update { it.copy(diag = it.diag.copy(logging = it.diag.logging.copy(drawCallCount = v))) }
                }
                MiuixSwitchRow(stringResource(R.string.diag_logging_gl_trace), diag.logging.glTrace) { v ->
                    controller.configStore.update { it.copy(diag = it.diag.copy(logging = it.diag.logging.copy(glTrace = v))) }
                }
            }
            MiuixGroup(title = stringResource(R.string.diag_perfetto_title)) {
                MiuixSwitchRow(stringResource(R.string.diag_perfetto_enabled), diag.perfetto.enabled) { v ->
                    controller.configStore.update { it.copy(diag = it.diag.copy(perfetto = it.diag.perfetto.copy(enabled = v))) }
                }
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = stringResource(R.string.diag_perfetto_max_duration),
                            style = MiuixTheme.textStyles.body1,
                            color = MiuixTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = "${diag.perfetto.maxDurationSec} s",
                            style = MiuixTheme.textStyles.body2,
                            color = MiuixTheme.colorScheme.primary,
                        )
                    }
                    Slider(
                        value = diag.perfetto.maxDurationSec.toFloat(),
                        onValueChange = { v ->
                            controller.configStore.update {
                                it.copy(diag = it.diag.copy(perfetto = it.diag.perfetto.copy(maxDurationSec = v.roundToInt())))
                            }
                        },
                        valueRange = 5f..120f,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            MiuixGroup(title = "") {
                MiuixSwitchRow(stringResource(R.string.diag_capability_report), diag.capabilityReport) { v ->
                    controller.configStore.update { it.copy(diag = it.diag.copy(capabilityReport = v)) }
                }
            }
        }
    }
}
