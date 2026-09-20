@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.fcl.plugin.mobileglues.ui.material

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.window.DialogProperties
import com.fcl.plugin.mobileglues.R
import com.fcl.plugin.mobileglues.settings.BufferUploadMode
import com.fcl.plugin.mobileglues.settings.HideMGEnvLevel
import com.fcl.plugin.mobileglues.settings.ImdbiBackend
import com.fcl.plugin.mobileglues.settings.MGConfig
import com.fcl.plugin.mobileglues.settings.MaxAnisotropyOverride
import com.fcl.plugin.mobileglues.settings.MultidrawEngine
import com.fcl.plugin.mobileglues.settings.SpinnerOption
import com.fcl.plugin.mobileglues.settings.TextureSwizzleMode
import com.fcl.plugin.mobileglues.settings.VmdiBackendTier
import com.fcl.plugin.mobileglues.ui.AppController
import kotlin.math.roundToInt

/**
 * Seção Avançado (Material 3). Lê/escreve apenas MGConfig via AppController.
 * Separado de MaterialSettings para não tocar no arquivo original.
 */
@Composable
fun MaterialAdvancedSection(controller: AppController, config: MGConfig) {
    val context = LocalContext.current
    var dialog by remember { mutableStateOf<DialogKind?>(null) }

    PreferenceGroup(title = stringResource(R.string.settings_group_advanced_ext)) {
        TextPreferenceRow(
            title = stringResource(R.string.option_hide_mg_env_level, "Hide MG env"),
            summary = config.hideMGEnvLevel.label(context).toString(),
            onClick = { dialog = DialogKind.HideMG },
        )
        SwitchPreferenceRow(
            title = stringResource(R.string.option_ext_gl43),
            checked = config.enableExtGL43,
            onCheckedChange = { enabled ->
                controller.configStore.update { it.copy(enableExtGL43 = enabled) }
            },
        )
        SwitchPreferenceRow(
            title = stringResource(R.string.option_force_gl_get_error_skip),
            checked = config.forceGlGetErrorSkip,
            onCheckedChange = { v ->
                controller.configStore.update { it.copy(forceGlGetErrorSkip = v) }
            },
        )
        SwitchPreferenceRow(
            title = "Disable compute on weak GPU",
            summary = "Auto-disable compute on PowerVR/slow GPUs (recommended)",
            checked = config.disableComputeOnWeakGpu,
            onCheckedChange = { v ->
                controller.configStore.update { it.copy(disableComputeOnWeakGpu = v) }
            },
        )
        TextPreferenceRow(
            title = stringResource(R.string.option_buffer_upload_mode, "Buffer upload"),
            summary = config.bufferUploadMode.label(context).toString(),
            onClick = { dialog = DialogKind.BufferUpload },
        )
        TextPreferenceRow(
            title = stringResource(R.string.option_texture_swizzle_mode, "Texture swizzle"),
            summary = config.textureSwizzleMode.label(context).toString(),
            onClick = { dialog = DialogKind.TextureSwizzle },
        )
        TextPreferenceRow(
            title = stringResource(R.string.option_max_anisotropy, "Max anisotropy"),
            summary = config.maxAnisotropyOverride.label(context).toString(),
            onClick = { dialog = DialogKind.Anisotropy },
        )
        SwitchPreferenceRow(
            title = stringResource(R.string.option_force_depth_precision_fix),
            checked = config.forceDepthPrecisionFix,
            onCheckedChange = { v ->
                controller.configStore.update { it.copy(forceDepthPrecisionFix = v) }
            },
        )
    }

    when (dialog) {
        DialogKind.HideMG -> OptionDialogLocal(
            title = stringResource(R.string.option_hide_mg_env_level, "Hide MG env"),
            options = HideMGEnvLevel.entries,
            selected = config.hideMGEnvLevel,
            onSelect = { v -> controller.configStore.update { it.copy(hideMGEnvLevel = v) } },
            onDismiss = { dialog = null },
        )
        DialogKind.BufferUpload -> OptionDialogLocal(
            title = stringResource(R.string.option_buffer_upload_mode, "Buffer upload"),
            options = BufferUploadMode.entries,
            selected = config.bufferUploadMode,
            onSelect = { v -> controller.configStore.update { it.copy(bufferUploadMode = v) } },
            onDismiss = { dialog = null },
        )
        DialogKind.TextureSwizzle -> OptionDialogLocal(
            title = stringResource(R.string.option_texture_swizzle_mode, "Texture swizzle"),
            options = TextureSwizzleMode.entries,
            selected = config.textureSwizzleMode,
            onSelect = { v -> controller.configStore.update { it.copy(textureSwizzleMode = v) } },
            onDismiss = { dialog = null },
        )
        DialogKind.Anisotropy -> OptionDialogLocal(
            title = stringResource(R.string.option_max_anisotropy, "Max anisotropy"),
            options = MaxAnisotropyOverride.entries,
            selected = config.maxAnisotropyOverride,
            onSelect = { v -> controller.configStore.update { it.copy(maxAnisotropyOverride = v) } },
            onDismiss = { dialog = null },
        )
        DialogKind.ImdbiBackend -> SingleChoiceDialog(
            title = stringResource(R.string.option_imdbi_backend),
            options = ImdbiBackend.entries.map { it.label(context).toString() },
            selectedIndex = ImdbiBackend.entries.indexOf(config.imdbiBackend),
            onSelect = { i ->
                controller.configStore.update { it.copy(imdbiBackend = ImdbiBackend.entries[i]) }
            },
            onDismiss = { dialog = null },
        )
        DialogKind.ImdbiUnroll -> SingleChoiceDialog(
            title = stringResource(R.string.option_imdbi_unroll_factor),
            options = listOf("4×", "8×"),
            selectedIndex = if (config.imdbiUnrollFactor == 8) 1 else 0,
            onSelect = { i ->
                controller.configStore.update { it.copy(imdbiUnrollFactor = if (i == 1) 8 else 4) }
            },
            onDismiss = { dialog = null },
        )
        DialogKind.VmdiTier -> SingleChoiceDialog(
            title = stringResource(R.string.option_vmdi_tier),
            options = VmdiBackendTier.entries.map { it.label(context).toString() },
            selectedIndex = VmdiBackendTier.entries.indexOf(config.vmdiBackendTier),
            onSelect = { i ->
                controller.configStore.update { it.copy(vmdiBackendTier = VmdiBackendTier.entries[i]) }
            },
            onDismiss = { dialog = null },
        )
        null -> Unit
    }
}

/** Seções condicionais IMDBI / VMDI (Fase 3B). Só aparecem quando o engine correspondente está ativo. */
@Composable
fun MaterialEngineSubmodesSection(controller: AppController, config: MGConfig) {
    val context = LocalContext.current
    var dialog by remember { mutableStateOf<DialogKind?>(null) }

    // ── IMDBI submodes ────────────────────────────────────────────────────
    AnimatedVisibility(
        visible = config.multidrawEngine == MultidrawEngine.Imdbi,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            PreferenceGroup(title = stringResource(R.string.settings_group_imdbi_submodes)) {
                TextPreferenceRow(
                    title = stringResource(R.string.option_imdbi_backend),
                    summary = config.imdbiBackend.label(context).toString(),
                    onClick = { dialog = DialogKind.ImdbiBackend },
                )
                TextPreferenceRow(
                    title = stringResource(R.string.option_imdbi_unroll_factor),
                    summary = "${config.imdbiUnrollFactor}×",
                    onClick = { dialog = DialogKind.ImdbiUnroll },
                )
                SwitchPreferenceRow(
                    title = stringResource(R.string.option_imdbi_persistent),
                    checked = config.imdbiPersistentMapping,
                    onCheckedChange = { v ->
                        controller.configStore.update { it.copy(imdbiPersistentMapping = v) }
                    },
                )
                SwitchPreferenceRow(
                    title = stringResource(R.string.option_imdbi_pinning),
                    checked = config.imdbiRegisterPinning,
                    onCheckedChange = { v ->
                        controller.configStore.update { it.copy(imdbiRegisterPinning = v) }
                    },
                )
                SwitchPreferenceRow(
                    title = stringResource(R.string.option_imdbi_restart),
                    checked = config.imdbiPrimitiveRestart,
                    onCheckedChange = { v ->
                        controller.configStore.update { it.copy(imdbiPrimitiveRestart = v) }
                    },
                )
                TextPreferenceRow(
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
            PreferenceGroup(title = stringResource(R.string.settings_group_vmdi_submodes)) {
                TextPreferenceRow(
                    title = stringResource(R.string.option_vmdi_tier),
                    summary = config.vmdiBackendTier.label(context).toString(),
                    onClick = { dialog = DialogKind.VmdiTier },
                )
                SwitchPreferenceRow(
                    title = stringResource(R.string.option_vmdi_autotune),
                    checked = config.vmdiEnableAutotune,
                    onCheckedChange = { v ->
                        controller.configStore.update { it.copy(vmdiEnableAutotune = v) }
                    },
                )
            }
        }
    }

    // ── Diálogos de seleção ───────────────────────────────────────────────
    when (dialog) {
        DialogKind.ImdbiBackend -> SingleChoiceDialog(
            title = stringResource(R.string.option_imdbi_backend),
            options = ImdbiBackend.entries.map { it.label(context).toString() },
            selectedIndex = ImdbiBackend.entries.indexOf(config.imdbiBackend),
            onSelect = { i ->
                controller.configStore.update { it.copy(imdbiBackend = ImdbiBackend.entries[i]) }
            },
            onDismiss = { dialog = null },
        )
        DialogKind.ImdbiUnroll -> SingleChoiceDialog(
            title = stringResource(R.string.option_imdbi_unroll_factor),
            options = listOf("4×", "8×"),
            selectedIndex = if (config.imdbiUnrollFactor == 8) 1 else 0,
            onSelect = { i ->
                controller.configStore.update { it.copy(imdbiUnrollFactor = if (i == 1) 8 else 4) }
            },
            onDismiss = { dialog = null },
        )
        DialogKind.VmdiTier -> SingleChoiceDialog(
            title = stringResource(R.string.option_vmdi_tier),
            options = VmdiBackendTier.entries.map { it.label(context).toString() },
            selectedIndex = VmdiBackendTier.entries.indexOf(config.vmdiBackendTier),
            onSelect = { i ->
                controller.configStore.update { it.copy(vmdiBackendTier = VmdiBackendTier.entries[i]) }
            },
            onDismiss = { dialog = null },
        )
        else -> Unit
    }
}

/** Seção Debug (Material 3). Só revela os controles se diag.enabled = true. */
@Composable
fun MaterialDebugSection(controller: AppController, config: MGConfig) {
    val diag = config.diag

    PreferenceGroup(title = stringResource(R.string.settings_group_debug)) {
        SwitchPreferenceRow(
            title = stringResource(R.string.diag_enabled),
            checked = diag.enabled,
            onCheckedChange = { v ->
                controller.configStore.update { it.copy(diag = it.diag.copy(enabled = v)) }
            },
        )
    }

    AnimatedVisibility(
        visible = diag.enabled,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            PreferenceGroup(title = stringResource(R.string.diag_overlay_title)) {
                DiagSwitch(stringResource(R.string.diag_overlay_frame_profiler), diag.overlay.frameProfiler) { v ->
                    controller.configStore.update { it.copy(diag = it.diag.copy(overlay = it.diag.overlay.copy(frameProfiler = v))) }
                }
                DiagSwitch(stringResource(R.string.diag_overlay_draw_call_count), diag.overlay.drawCallCount) { v ->
                    controller.configStore.update { it.copy(diag = it.diag.copy(overlay = it.diag.overlay.copy(drawCallCount = v))) }
                }
                DiagSwitch(stringResource(R.string.diag_overlay_shader_recompiles), diag.overlay.shaderRecompiles) { v ->
                    controller.configStore.update { it.copy(diag = it.diag.copy(overlay = it.diag.overlay.copy(shaderRecompiles = v))) }
                }
                DiagSwitch(stringResource(R.string.diag_overlay_backend_tier), diag.overlay.backendTier) { v ->
                    controller.configStore.update { it.copy(diag = it.diag.copy(overlay = it.diag.overlay.copy(backendTier = v))) }
                }
                DiagSwitch(stringResource(R.string.diag_overlay_cpu_gpu_load), diag.overlay.cpuGpuLoad) { v ->
                    controller.configStore.update { it.copy(diag = it.diag.copy(overlay = it.diag.overlay.copy(cpuGpuLoad = v))) }
                }
            }
            PreferenceGroup(title = stringResource(R.string.diag_logging_title)) {
                DiagSwitch(stringResource(R.string.diag_logging_backend_selection), diag.logging.backendSelection) { v ->
                    controller.configStore.update { it.copy(diag = it.diag.copy(logging = it.diag.logging.copy(backendSelection = v))) }
                }
                DiagSwitch(stringResource(R.string.diag_logging_shader_recompiles), diag.logging.shaderRecompiles) { v ->
                    controller.configStore.update { it.copy(diag = it.diag.copy(logging = it.diag.logging.copy(shaderRecompiles = v))) }
                }
                DiagSwitch(stringResource(R.string.diag_logging_draw_call_count), diag.logging.drawCallCount) { v ->
                    controller.configStore.update { it.copy(diag = it.diag.copy(logging = it.diag.logging.copy(drawCallCount = v))) }
                }
                DiagSwitch(stringResource(R.string.diag_logging_gl_trace), diag.logging.glTrace) { v ->
                    controller.configStore.update { it.copy(diag = it.diag.copy(logging = it.diag.logging.copy(glTrace = v))) }
                }
                DiagText(stringResource(R.string.diag_logging_level), diag.logging.level)
            }
            PreferenceGroup(title = stringResource(R.string.diag_perfetto_title)) {
                DiagSwitch(stringResource(R.string.diag_perfetto_enabled), diag.perfetto.enabled) { v ->
                    controller.configStore.update { it.copy(diag = it.diag.copy(perfetto = it.diag.perfetto.copy(enabled = v))) }
                }
                DiagSlider(
                    title = stringResource(R.string.diag_perfetto_max_duration),
                    value = diag.perfetto.maxDurationSec.toFloat(),
                    range = 5f..120f,
                    onValueChange = { v ->
                        controller.configStore.update { it.copy(diag = it.diag.copy(perfetto = it.diag.perfetto.copy(maxDurationSec = v.roundToInt()))) }
                    },
                )
            }
            PreferenceGroup(title = "") {
                DiagSwitch(stringResource(R.string.diag_capability_report), diag.capabilityReport) { v ->
                    controller.configStore.update { it.copy(diag = it.diag.copy(capabilityReport = v)) }
                }
            }
        }
    }
}

// ── Helpers ─────────────────────────────────────────────────────────────

private enum class DialogKind { HideMG, BufferUpload, TextureSwizzle, Anisotropy, ImdbiBackend, ImdbiUnroll, VmdiTier }

@Composable
private fun <T : SpinnerOption> OptionDialogLocal(
    title: String,
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    SingleChoiceDialog(
        title = title,
        options = options.map { it.label(context).toString() },
        selectedIndex = options.indexOf(selected),
        onSelect = { onSelect(options[it]) },
        onDismiss = onDismiss,
    )
}

@Composable
private fun DiagSwitch(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    SwitchPreferenceRow(title = title, checked = checked, onCheckedChange = onChange)
}

@Composable
private fun DiagText(title: String, value: String) {
    TextPreferenceRow(title = title, summary = value)
}

@Composable
private fun DiagSlider(
    title: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text("${value.roundToInt()} s", style = MaterialTheme.typography.labelLarge)
        }
        Slider(value = value, onValueChange = onValueChange, valueRange = range)
    }
}
