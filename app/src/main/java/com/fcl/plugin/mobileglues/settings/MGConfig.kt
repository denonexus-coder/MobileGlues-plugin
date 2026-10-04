package com.fcl.plugin.mobileglues.settings

import android.content.Context
import androidx.annotation.StringRes
import com.fcl.plugin.mobileglues.R

interface WireValue { val wire: Int }

interface SpinnerOption : WireValue {
    fun label(context: Context): CharSequence
}

internal fun <T : WireValue> List<T>.fromWire(wire: Int?, fallback: T): T =
    firstOrNull { it.wire == wire } ?: fallback

// ═══════════════════════════════════════════════════════════════════════════
//  Every field of MGConfig below maps 1:1 onto a top-level key that
//  `config/settings.cpp` actually reads with `config_get_int`.
//
//  The C++ reader is `cJSON_GetObjectItem(root, name)` — top level only, no
//  nested lookup. So this model must stay flat and must stay in sync with
//  those ten keys: a key the lib does not read has no place here.
// ═══════════════════════════════════════════════════════════════════════════

enum class AngleConfig(override val wire: Int, @param:StringRes private val labelRes: Int) : SpinnerOption {
    DisableIfPossible(0, R.string.option_angle_disable_if_possible),
    EnableIfPossible(1, R.string.option_angle_enable_if_possible),
    ForceDisable(2, R.string.option_angle_disable),
    ForceEnable(3, R.string.option_angle_enable);
    override fun label(context: Context): CharSequence = context.getString(labelRes)
}

/**
 * C++ `Version custom_gl_version`. `settings.cpp` reads this with
 * `config_get_int`, so the wire form — the int (40, 42, 46) — is what goes on
 * disk. [jsonString] only survives for [fromString]'s dotted-literal input.
 */
enum class GlVersion(override val wire: Int, private val literal: String?) : SpinnerOption {
    Default(0, null),
    Gl46(46, "OpenGL 4.6"), Gl45(45, "OpenGL 4.5"), Gl44(44, "OpenGL 4.4"),
    Gl43(43, "OpenGL 4.3"), Gl42(42, "OpenGL 4.2"), Gl41(41, "OpenGL 4.1"),
    Gl40(40, "OpenGL 4.0"), Gl33(33, "OpenGL 3.3"), Gl32(32, "OpenGL 3.2");

    override fun label(context: Context): CharSequence =
        literal ?: context.getString(R.string.option_custom_gl_version_default)

    /** Dotted literal: "0", "4.0", "4.6". */
    val jsonString: String get() = literal?.removePrefix("OpenGL ") ?: "0"

    companion object {
        fun fromWire(wire: Int?): GlVersion {
            if (wire == null) return Default
            entries.firstOrNull { it.wire == wire }?.let { return it }
            return when {
                wire > 46 -> Gl46
                wire in 34..39 -> Gl33
                wire in 1..31 -> Gl32
                else -> Default
            }
        }

        /** Accepts "0", "4.0", "4.6", or an int-as-string ("46"). */
        fun fromString(raw: String?): GlVersion {
            val trimmed = raw?.trim().orEmpty()
            if (trimmed.isEmpty()) return Default
            entries.firstOrNull { it.jsonString == trimmed }?.let { return it }
            val parts = trimmed.split('.')
            if (parts.size >= 2) {
                val major = parts[0].toIntOrNull() ?: return Default
                val minor = parts[1].toIntOrNull() ?: return Default
                return fromWire(major * 10 + minor)
            }
            return trimmed.toIntOrNull()?.let(::fromWire) ?: Default
        }
    }
}

enum class HideMGEnvLevel(override val wire: Int, @param:StringRes private val labelRes: Int) : SpinnerOption {
    Disabled(0, R.string.option_hide_mg_disabled),
    Level1(1, R.string.option_hide_mg_level1);
    override fun label(context: Context): CharSequence = context.getString(labelRes)
}

/**
 * C++ `NoErrorConfig` in config/settings.h, verbatim: `Auto = 0, Disable = 1,
 * Level1 = 2, Level2 = 3`.
 *
 * The names are the C++ names because the wires have to match, and the labels
 * are the four `option_no_error_*` strings that already described them: Auto /
 * Do not ignore / Ignore shader/program error / Ignore shader/program and
 * framebuffer error. An earlier three-entry spelling (`None`, `Partial`,
 * `Full`) named wire 1 "Partial" and wire 2 "Full", which read backwards from
 * what the renderer does with them — wire 1 is the one that ignores *nothing*.
 */
enum class NoErrorConfig(override val wire: Int, @param:StringRes private val labelRes: Int) : SpinnerOption {
    Auto(0, R.string.option_no_error_auto),
    Disable(1, R.string.option_no_error_enable),
    Level1(2, R.string.option_no_error_disable_pri),
    Level2(3, R.string.option_no_error_disable_sec);
    override fun label(context: Context): CharSequence = context.getString(labelRes)
}

enum class DepthClearFixMode(override val wire: Int, @param:StringRes private val labelRes: Int) : SpinnerOption {
    Disabled(0, R.string.option_angle_clear_workaround_disable),
    Mode1(1, R.string.option_angle_clear_workaround_enable_1),
    Mode2(2, R.string.option_angle_clear_workaround_enable_2);
    override fun label(context: Context): CharSequence = context.getString(labelRes)
}

// ═══════════════════════════════════════════════════════════════════════════
//  shaderCache — wire `maxShaderCacheSize` (MiB)
// ═══════════════════════════════════════════════════════════════════════════

sealed interface GlslCacheSize {
    val wire: Int
    /** Wire 0 = off (C++ `max_shader_cache_size = 0`). */
    data object Disabled : GlslCacheSize { override val wire: Int get() = 0 }
    data class Limited(val mebibytes: Int) : GlslCacheSize {
        init { require(mebibytes > 0) { "Shader cache size must be positive, was $mebibytes" } }
        override val wire: Int get() = mebibytes
    }
    val mebibytesOrZero: Int get() = (this as? Limited)?.mebibytes ?: 0
    companion object {
        val Default: GlslCacheSize = Limited(32)
        fun ofMebibytes(mebibytes: Int): GlslCacheSize =
            if (mebibytes > 0) Limited(mebibytes) else Disabled
        fun fromWire(wire: Int?): GlslCacheSize = when {
            wire == null -> Default
            wire > 0 -> Limited(wire)
            else -> Disabled
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  upscaling — wire `fsr1Setting`
//
//  Mirrors C++ `FSR1_Quality_Preset` in config/settings.h, where the numeric
//  values are the enum order: Disabled = 0, UltraQuality = 1 … Performance = 4,
//  MaxValue = 5 (sentinel, never a valid setting).
// ═══════════════════════════════════════════════════════════════════════════

enum class Fsr1Preset(override val wire: Int, @param:StringRes private val labelRes: Int) : SpinnerOption {
    Disabled(0, R.string.option_fsr1_preset_off),
    UltraQuality(1, R.string.option_fsr1_preset_ultra_quality),
    Quality(2, R.string.option_fsr1_preset_quality),
    Balanced(3, R.string.option_fsr1_preset_balanced),
    Performance(4, R.string.option_fsr1_preset_performance);
    override fun label(context: Context): CharSequence = context.getString(labelRes)
    companion object {
        fun fromWire(wire: Int?): Fsr1Preset = entries.firstOrNull { it.wire == wire } ?: Disabled
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  MultiDraw bench vocabulary
//
//  Not configuration: nothing here is written to config.json, because
//  config/settings.cpp has no MultiDraw key left to write to (the backend
//  table was removed on the C++ side). This is the vocabulary the micro-
//  benchmark reports in — which entry point, which backend, which ones this
//  build actually implements.
// ═══════════════════════════════════════════════════════════════════════════

enum class MultidrawBackend(val key: String, @param:StringRes private val labelRes: Int) {
    Unroll("unroll", R.string.md_backend_unroll),
    BaseVertex("basevertex", R.string.md_backend_basevertex),
    Indirect("indirect", R.string.md_backend_indirect),
    MultiArrays("multiarrays", R.string.md_backend_multiarrays),
    MultiBaseVertex("multibasevertex", R.string.md_backend_multibasevertex),
    MultiIndirect("multiindirect", R.string.md_backend_multiindirect),
    Compute("compute", R.string.md_backend_compute);
    fun label(context: Context): CharSequence = context.getString(labelRes)
    companion object {
        fun parse(raw: String?): MultidrawBackend? {
            val normalized = raw
                ?.filterNot { it == ' ' || it == '\t' || it == '_' || it == '-' }
                ?.lowercase()
                ?.takeIf { it.isNotEmpty() }
                ?: return null
            return entries.firstOrNull { it.key == normalized }
        }
    }
}

enum class MultidrawEntry(
    val glFunction: String,
    val implemented: List<MultidrawBackend>,
    val nativeBackend: MultidrawBackend,
) {
    Arrays(
        "glMultiDrawArrays",
        listOf(MultidrawBackend.MultiArrays, MultidrawBackend.MultiIndirect, MultidrawBackend.Unroll),
        MultidrawBackend.MultiArrays,
    ),
    Elements(
        "glMultiDrawElements",
        listOf(
            MultidrawBackend.MultiArrays, MultidrawBackend.MultiIndirect,
            MultidrawBackend.Indirect, MultidrawBackend.Unroll,
        ),
        MultidrawBackend.MultiArrays,
    ),
    ElementsBaseVertex(
        "glMultiDrawElementsBaseVertex",
        listOf(
            MultidrawBackend.MultiBaseVertex, MultidrawBackend.MultiIndirect,
            MultidrawBackend.Indirect, MultidrawBackend.BaseVertex,
            MultidrawBackend.Unroll, MultidrawBackend.Compute,
        ),
        MultidrawBackend.MultiBaseVertex,
    ),
    ArraysIndirect(
        "glMultiDrawArraysIndirect",
        listOf(MultidrawBackend.MultiIndirect, MultidrawBackend.Indirect),
        MultidrawBackend.MultiIndirect,
    ),
    ElementsIndirect(
        "glMultiDrawElementsIndirect",
        listOf(MultidrawBackend.MultiIndirect, MultidrawBackend.Indirect),
        MultidrawBackend.MultiIndirect,
    );

    fun normalize(backends: List<MultidrawBackend>): List<MultidrawBackend> =
        (backends.filter { it in implemented }.distinct() + implemented).distinct()
}

// ═══════════════════════════════════════════════════════════════════════════
//  root
// ═══════════════════════════════════════════════════════════════════════════

data class MGConfig(
    val angle: AngleConfig = AngleConfig.EnableIfPossible,
    val glVersion: GlVersion = GlVersion.Default,
    val hideMGEnvLevel: HideMGEnvLevel = HideMGEnvLevel.Disabled,

    val noError: NoErrorConfig = NoErrorConfig.Auto,
    val depthClearFix: DepthClearFixMode = DepthClearFixMode.Disabled,

    val glslCache: GlslCacheSize = GlslCacheSize.Default,
    val useProgramBinaryCache: Boolean = false,

    val extComputeShader: Boolean = false,
    val extTimerQuery: Boolean = true,
    val extDirectStateAccess: Boolean = false,

    val fsr1Setting: Fsr1Preset = Fsr1Preset.Disabled,
) {
    /** Master FSR switch, derived: anything but Disabled means the lib turns it on. */
    val fsr1Enabled: Boolean get() = fsr1Setting != Fsr1Preset.Disabled

    companion object { val Default = MGConfig() }
}
