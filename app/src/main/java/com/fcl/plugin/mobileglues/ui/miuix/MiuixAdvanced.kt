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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fcl.plugin.mobileglues.R
import com.fcl.plugin.mobileglues.settings.BufferUploadMode
import com.fcl.plugin.mobileglues.settings.HideMGEnvLevel
import com.fcl.plugin.mobileglues.settings.ImdbiBackend
import com.fcl.plugin.mobileglues.settings.MGConfig
import com.fcl.plugin.mobileglues.settings.MaxAnisotropyOverride
import com.fcl.plugin.mobileglues.settings.MultidrawEngine
import com.fcl.plugin.mobileglues.settings.TextureSwizzleMode
import com.fcl.plugin.mobileglues.settings.VmdiBackendTier
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
            title = stringResource(R.string.option_hide_mg_env_level),
            options = HideMGEnvLevel.entries.map { it.label(context).toString() },
            selectedIndex = HideMGEnvLevel.entries.indexOf(config.hideMGEnvLevel),
            onSelect = { i ->
                controller.configStore.update { it.copy(hideMGEnvLevel = HideMGEnvLevel.entries[i]) }
            },
        )
        MiuixSwitchRow(
            title = stringResource(R.string.option_ext_gl43),
            checked = config.enableExtGL43,
            onCheckedChange = { v -> controller.configStore.update { it.copy(enableExtGL43 = v) } },
        )
        MiuixSwitchRow(
            title = stringResource(R.string.option_force_gl_get_error_skip),
            checked = config.forceGlGetErrorSkip,
            onCheckedChange = { v -> controller.configStore.update { it.copy(forceGlGetErrorSkip = v) } },
        )
        MiuixDropdownRow(
            title = stringResource(R.string.option_buffer_upload_mode),
            options = BufferUploadMode.entries.map { it.label(context).toString() },
            selectedIndex = BufferUploadMode.entries.indexOf(config.bufferUploadMode),
            onSelect = { i ->
                controller.configStore.update { it.copy(bufferUploadMode = BufferUploadMode.entries[i]) }
            },
        )
        MiuixDropdownRow(
            title = stringResource(R.string.option_texture_swizzle_mode),
            options = TextureSwizzleMode.entries.map { it.label(context).toString() },
            selectedIndex = TextureSwizzleMode.entries.indexOf(config.textureSwizzleMode),
            onSelect = { i ->
                controller.configStore.update { it.copy(textureSwizzleMode = TextureSwizzleMode.entries[i]) }
            },
        )
        MiuixDropdownRow(
            title = stringResource(R.string.option_max_anisotropy),
            options = MaxAnisotropyOverride.entries.map { it.label(context).toString() },
            selectedIndex = MaxAnisotropyOverride.entries.indexOf(config.maxAnisotropyOverride),
            onSelect = { i ->
                controller.configStore.update { it.copy(maxAnisotropyOverride = MaxAnisotropyOverride.entries[i]) }
            },
        )
        MiuixSwitchRow(
            title = stringResource(R.string.option_force_depth_precision_fix),
            checked = config.forceDepthPrecisionFix,
            onCheckedChange = { v -> controller.configStore.update { it.copy(forceDepthPrecisionFix = v) } },
        )
    }
}

/** Seções condicionais IMDBI / VMDI (Fase 3B) — espelho Miuix. Aparecem apenas com o engine correspondente. */
@Composable
fun MiuixEngineSubmodesSection(controller: AppController, config: MGConfig) {
    val context = LocalContext.current

    // ── IMDBI submodes ────────────────────────────────────────────────────
    AnimatedVisibility(
        visible = config.multidrawEngine == MultidrawEngine.Imdbi,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            MiuixGroup(title = stringResource(R.string.settings_group_imdbi_submodes)) {
                MiuixDropdownRow(
                    title = stringResource(R.string.option_imdbi_backend),
                    options = ImdbiBackend.entries.map { it.label(context).toString() },
                    selectedIndex = ImdbiBackend.entries.indexOf(config.imdbiBackend),
                    onSelect = { i ->
                        controller.configStore.update { it.copy(imdbiBackend = ImdbiBackend.entries[i]) }
                    },
                )
                MiuixDropdownRow(
                    title = stringResource(R.string.option_imdbi_unroll_factor),
                    options = listOf("4×", "8×"),
                    selectedIndex = if (config.imdbiUnrollFactor == 8) 1 else 0,
                    onSelect = { i ->
                        controller.configStore.update { it.copy(imdbiUnrollFactor = if (i == 1) 8 else 4) }
                    },
                )
                MiuixSwitchRow(
                    title = stringResource(R.string.option_imdbi_persistent),
                    checked = config.imdbiPersistentMapping,
                    onCheckedChange = { v ->
                        controller.configStore.update { it.copy(imdbiPersistentMapping = v) }
                    },
                )
                MiuixSwitchRow(
                    title = stringResource(R.string.option_imdbi_pinning),
                    checked = config.imdbiRegisterPinning,
                    onCheckedChange = { v ->
                        controller.configStore.update { it.copy(imdbiRegisterPinning = v) }
                    },
                )
                MiuixSwitchRow(
                    title = stringResource(R.string.option_imdbi_restart),
                    checked = config.imdbiPrimitiveRestart,
                    onCheckedChange = { v ->
                        controller.configStore.update { it.copy(imdbiPrimitiveRestart = v) }
                    },
                )
                MiuixTextRow(
                    title = stringResource(R.string.option_imdbi_ring),
                    summary = "${config.imdbiRingSizeKb} KiB",
                )
            }
        }
    }

    // ── VMDI submodes ─────────────────────────────────────────────────────
    AnimatedVisibility(
        visible = config.multidrawEngine == MultidrawEngine.Vmdi,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            MiuixGroup(title = stringResource(R.string.settings_group_vmdi_submodes)) {
                MiuixDropdownRow(
                    title = stringResource(R.string.option_vmdi_tier),
                    options = VmdiBackendTier.entries.map { it.label(context).toString() },
                    selectedIndex = VmdiBackendTier.entries.indexOf(config.vmdiBackendTier),
                    onSelect = { i ->
                        controller.configStore.update { it.copy(vmdiBackendTier = VmdiBackendTier.entries[i]) }
                    },
                )
                MiuixSwitchRow(
                    title = stringResource(R.string.option_vmdi_autotune),
                    checked = config.vmdiEnableAutotune,
                    onCheckedChange = { v ->
                        controller.configStore.update { it.copy(vmdiEnableAutotune = v) }
                    },
                )
            }
        }
    }
}

@Composable
fun MiuixDebugSection(controller: AppController, config: MGConfig) {
    val diag = config.diag

    MiuixGroup(title = stringResource(R.string.settings_group_debug)) {
        MiuixSwitchRow(
            title = stringResource(R.string.diag_enabled),
            checked = diag.enabled,
            onCheckedChange = { v -> controller.configStore.update { c -> c.copy(diag = c.diag.copy(enabled = v)) } },
        )
    }

    AnimatedVisibility(
        visible = diag.enabled,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            MiuixGroup(title = stringResource(R.string.diag_overlay_title)) {
                MiuixSwitchRow(
                    title = stringResource(R.string.diag_overlay_frame_profiler),
                    checked = diag.overlay.frameProfiler,
                    onCheckedChange = { v ->
                        controller.configStore.update { c ->
                            c.copy(diag = c.diag.copy(overlay = c.diag.overlay.copy(frameProfiler = v)))
                        }
                    },
                )
                MiuixSwitchRow(
                    title = stringResource(R.string.diag_overlay_draw_call_count),
                    checked = diag.overlay.drawCallCount,
                    onCheckedChange = { v ->
                        controller.configStore.update { c ->
                            c.copy(diag = c.diag.copy(overlay = c.diag.overlay.copy(drawCallCount = v)))
                        }
                    },
                )
                MiuixSwitchRow(
                    title = stringResource(R.string.diag_overlay_shader_recompiles),
                    checked = diag.overlay.shaderRecompiles,
                    onCheckedChange = { v ->
                        controller.configStore.update { c ->
                            c.copy(diag = c.diag.copy(overlay = c.diag.overlay.copy(shaderRecompiles = v)))
                        }
                    },
                )
                MiuixSwitchRow(
                    title = stringResource(R.string.diag_overlay_backend_tier),
                    checked = diag.overlay.backendTier,
                    onCheckedChange = { v ->
                        controller.configStore.update { c ->
                            c.copy(diag = c.diag.copy(overlay = c.diag.overlay.copy(backendTier = v)))
                        }
                    },
                )
                MiuixSwitchRow(
                    title = stringResource(R.string.diag_overlay_cpu_gpu_load),
                    checked = diag.overlay.cpuGpuLoad,
                    onCheckedChange = { v ->
                        controller.configStore.update { c ->
                            c.copy(diag = c.diag.copy(overlay = c.diag.overlay.copy(cpuGpuLoad = v)))
                        }
                    },
                )
            }
            MiuixGroup(title = stringResource(R.string.diag_logging_title)) {
                MiuixSwitchRow(
                    title = stringResource(R.string.diag_logging_backend_selection),
                    checked = diag.logging.backendSelection,
                    onCheckedChange = { v ->
                        controller.configStore.update { c ->
                            c.copy(diag = c.diag.copy(logging = c.diag.logging.copy(backendSelection = v)))
                        }
                    },
                )
                MiuixSwitchRow(
                    title = stringResource(R.string.diag_logging_shader_recompiles),
                    checked = diag.logging.shaderRecompiles,
                    onCheckedChange = { v ->
                        controller.configStore.update { c ->
                            c.copy(diag = c.diag.copy(logging = c.diag.logging.copy(shaderRecompiles = v)))
                        }
                    },
                )
                MiuixSwitchRow(
                    title = stringResource(R.string.diag_logging_draw_call_count),
                    checked = diag.logging.drawCallCount,
                    onCheckedChange = { v ->
                        controller.configStore.update { c ->
                            c.copy(diag = c.diag.copy(logging = c.diag.logging.copy(drawCallCount = v)))
                        }
                    },
                )
                MiuixSwitchRow(
                    title = stringResource(R.string.diag_logging_gl_trace),
                    checked = diag.logging.glTrace,
                    onCheckedChange = { v ->
                        controller.configStore.update { c ->
                            c.copy(diag = c.diag.copy(logging = c.diag.logging.copy(glTrace = v)))
                        }
                    },
                )
            }
            MiuixGroup(title = stringResource(R.string.diag_perfetto_title)) {
                MiuixSwitchRow(
                    title = stringResource(R.string.diag_perfetto_enabled),
                    checked = diag.perfetto.enabled,
                    onCheckedChange = { v ->
                        controller.configStore.update { c ->
                            c.copy(diag = c.diag.copy(perfetto = c.diag.perfetto.copy(enabled = v)))
                        }
                    },
                )
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
                            controller.configStore.update { c ->
                                c.copy(diag = c.diag.copy(perfetto = c.diag.perfetto.copy(maxDurationSec = v.roundToInt())))
                            }
                        },
                        valueRange = 5f..120f,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            MiuixGroup(title = "") {
                MiuixSwitchRow(
                    title = stringResource(R.string.diag_capability_report),
                    checked = diag.capabilityReport,
                    onCheckedChange = { v ->
                        controller.configStore.update { c ->
                            c.copy(diag = c.diag.copy(capabilityReport = v))
                        }
                    },
                )
            }
        }
    }
}
