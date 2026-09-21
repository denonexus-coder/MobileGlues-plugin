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
//  opengl_egl
// ═══════════════════════════════════════════════════════════════════════════

enum class AngleConfig(override val wire: Int, @param:StringRes private val labelRes: Int) : SpinnerOption {
    DisableIfPossible(0, R.string.option_angle_disable_if_possible),
    EnableIfPossible(1, R.string.option_angle_enable_if_possible),
    ForceDisable(2, R.string.option_angle_disable),
    ForceEnable(3, R.string.option_angle_enable);
    override fun label(context: Context): CharSequence = context.getString(labelRes)
}

/**
 * C++ `Version custom_gl_version`. Wire is the int (40, 42, 46); the JSON
 * carries the dotted literal ("0", "4.0", "4.6") which C++ `Version(string)`
 * parses. [jsonString] is what goes on disk; [fromString] is the read side.
 */
enum class GlVersion(override val wire: Int, private val literal: String?) : SpinnerOption {
    Default(0, null),
    Gl46(46, "OpenGL 4.6"), Gl45(45, "OpenGL 4.5"), Gl44(44, "OpenGL 4.4"),
    Gl43(43, "OpenGL 4.3"), Gl42(42, "OpenGL 4.2"), Gl41(41, "OpenGL 4.1"),
    Gl40(40, "OpenGL 4.0"), Gl33(33, "OpenGL 3.3"), Gl32(32, "OpenGL 3.2");

    override fun label(context: Context): CharSequence =
        literal ?: context.getString(R.string.option_custom_gl_version_default)

    /** JSON representation: "0", "4.0", "4.6". */
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

// ═══════════════════════════════════════════════════════════════════════════
//  errorHandling
// ═══════════════════════════════════════════════════════════════════════════

enum class NoErrorConfig(override val wire: Int, @param:StringRes private val labelRes: Int) : SpinnerOption {
    None(0, R.string.option_no_error_none),
    Partial(1, R.string.option_no_error_partial),
    Full(2, R.string.option_no_error_full);
    override fun label(context: Context): CharSequence = context.getString(labelRes)
}

enum class DepthClearFixMode(override val wire: Int, @param:StringRes private val labelRes: Int) : SpinnerOption {
    Disabled(0, R.string.option_angle_clear_workaround_disable),
    Mode1(1, R.string.option_angle_clear_workaround_enable_1),
    Mode2(2, R.string.option_angle_clear_workaround_enable_2);
    override fun label(context: Context): CharSequence = context.getString(labelRes)
}

// ═══════════════════════════════════════════════════════════════════════════
//  shaderCache
// ═══════════════════════════════════════════════════════════════════════════

sealed interface GlslCacheSize {
    val wire: Int
    /** Wire 0 = off (C++ `max_glsl_cache_size = 0`). */
    data object Disabled : GlslCacheSize { override val wire: Int get() = 0 }
    data class Limited(val mebibytes: Int) : GlslCacheSize {
        init { require(mebibytes > 0) { "GLSL cache size must be positive, was $mebibytes" } }
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
//  textureBuffer
// ═══════════════════════════════════════════════════════════════════════════

/** C++ `buffer_upload_mode`: 0 Auto, 1 Streaming, 2 Persistent, 3 Ring. */
enum class BufferUploadMode(override val wire: Int, @param:StringRes private val labelRes: Int) : SpinnerOption {
    Auto(0, R.string.option_buffer_upload_auto),
    Streaming(1, R.string.option_buffer_upload_streaming),
    Persistent(2, R.string.option_buffer_upload_persistent),
    Ring(3, R.string.option_buffer_upload_ring);
    override fun label(context: Context): CharSequence = context.getString(labelRes)
    companion object {
        fun fromWire(wire: Int?): BufferUploadMode = entries.firstOrNull { it.wire == wire } ?: Auto
    }
}

/** C++ `texture_swizzle_mode`: 0 Auto, 1 RGBA, 2 BGRA. */
enum class TextureSwizzleMode(override val wire: Int, @param:StringRes private val labelRes: Int) : SpinnerOption {
    Auto(0, R.string.option_texture_swizzle_auto),
    Rgba(1, R.string.option_texture_swizzle_rgba),
    Bgra(2, R.string.option_texture_swizzle_bgra);
    override fun label(context: Context): CharSequence = context.getString(labelRes)
    companion object {
        fun fromWire(wire: Int?): TextureSwizzleMode = entries.firstOrNull { it.wire == wire } ?: Auto
    }
}

enum class MaxAnisotropyOverride(override val wire: Int, @param:StringRes private val labelRes: Int) : SpinnerOption {
    Default(0, R.string.option_max_anisotropy_default),
    X2(2, R.string.option_max_anisotropy_2),
    X4(4, R.string.option_max_anisotropy_4),
    X8(8, R.string.option_max_anisotropy_8),
    X16(16, R.string.option_max_anisotropy_16);
    override fun label(context: Context): CharSequence = context.getString(labelRes)
    companion object {
        fun fromWire(wire: Int?): MaxAnisotropyOverride = entries.firstOrNull { it.wire == wire } ?: Default
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  multidrawEngine (top + vmdi + imdbi sub-objects)
// ═══════════════════════════════════════════════════════════════════════════

enum class MultidrawEngine(
    val key: String,
    override val wire: Int,
    @param:StringRes private val labelRes: Int,
) : SpinnerOption {
    Legacy("legacy", 0, R.string.option_multidraw_engine_legacy),
    Vmdi("vmdi", 1, R.string.option_multidraw_engine_vmdi),
    Imdbi("imdbi", 2, R.string.option_multidraw_engine_imdbi);
    override fun label(context: Context): CharSequence = context.getString(labelRes)
    val usesBackendOrdering: Boolean get() = this == Legacy
    companion object {
        fun fromKey(key: String?): MultidrawEngine =
            entries.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: Legacy
        fun fromWire(wire: Int?): MultidrawEngine =
            entries.firstOrNull { it.wire == wire } ?: Legacy
    }
}

enum class ImdbiBackend(val key: String, @StringRes private val labelRes: Int) {
    Stitching("stitching", R.string.option_imdbi_stitching),
    FastIndirectRing("fast_indirect_ring", R.string.option_imdbi_fast_ring),
    UnrolledLoop("unrolled_loop", R.string.option_imdbi_unrolled),
    ComputeDispatch("compute_dispatch", R.string.option_imdbi_compute);
    fun label(context: Context): CharSequence = context.getString(labelRes)
    companion object {
        fun fromKey(key: String?): ImdbiBackend =
            entries.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: FastIndirectRing
    }
}

enum class VmdiBackendTier(val key: String, @StringRes private val labelRes: Int) {
    Auto("auto", R.string.option_vmdi_auto),
    NativeMdi("native_mdi", R.string.option_vmdi_native),
    MultiBaseVertex("multi_base_vertex", R.string.option_vmdi_basevertex),
    IndirectUnrolled("indirect_unrolled", R.string.option_vmdi_indirect),
    DirectFallback("direct_fallback", R.string.option_vmdi_fallback);
    fun label(context: Context): CharSequence = context.getString(labelRes)
    companion object {
        fun fromKey(key: String?): VmdiBackendTier =
            entries.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: Auto
    }
}

/** Discrete set of unroll factors C++ accepts. Order = UI order. */
val ImdbiUnrollFactors: List<Int> = listOf(4, 8, 16)

/**
 * Discrete set of ring sizes, in bytes. C++ `imdbi_ring_size` default is 4 MiB;
 * 4 KiB is the "I know what I am doing" small setting.
 */
val ImdbiRingSizes: List<Int> = listOf(4096, 4 * 1024 * 1024, 8 * 1024 * 1024, 16 * 1024 * 1024)

// ═══════════════════════════════════════════════════════════════════════════
//  multidrawOrder (backends + per-entry exceptions)
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

enum class MultidrawOrderItem(
    val key: String,
    val backend: MultidrawBackend?,
    @param:StringRes private val labelRes: Int,
) {
    Native("native", null, R.string.md_item_native),
    MultiIndirect("multiindirect", MultidrawBackend.MultiIndirect, R.string.md_backend_multiindirect),
    MultiBaseVertex("multibasevertex", MultidrawBackend.MultiBaseVertex, R.string.md_backend_multibasevertex),
    MultiArrays("multiarrays", MultidrawBackend.MultiArrays, R.string.md_backend_multiarrays),
    Indirect("indirect", MultidrawBackend.Indirect, R.string.md_backend_indirect),
    BaseVertex("basevertex", MultidrawBackend.BaseVertex, R.string.md_backend_basevertex),
    Unroll("unroll", MultidrawBackend.Unroll, R.string.md_backend_unroll),
    Compute("compute", MultidrawBackend.Compute, R.string.md_backend_compute);
    fun label(context: Context): CharSequence = context.getString(labelRes)
    companion object {
        val DefaultOrder: List<MultidrawOrderItem> = entries.toList()
        fun parse(raw: String?): MultidrawOrderItem? {
            val normalized = raw
                ?.filterNot { it == ' ' || it == '\t' || it == '_' || it == '-' }
                ?.lowercase()
                ?.takeIf { it.isNotEmpty() }
                ?: return null
            return entries.firstOrNull { it.key == normalized }
        }
        fun normalize(items: List<MultidrawOrderItem>): List<MultidrawOrderItem> =
            (items.distinct() + DefaultOrder).distinct()
    }
}

enum class MultidrawEntry(
    val orderKey: String,
    val legacyModeKey: String,
    val glFunction: String,
    val implemented: List<MultidrawBackend>,
    val nativeBackend: MultidrawBackend,
) {
    Arrays(
        "multidrawOrderArrays", "multidrawModeArrays", "glMultiDrawArrays",
        listOf(MultidrawBackend.MultiArrays, MultidrawBackend.MultiIndirect, MultidrawBackend.Unroll),
        MultidrawBackend.MultiArrays,
    ),
    Elements(
        "multidrawOrderElements", "multidrawModeElements", "glMultiDrawElements",
        listOf(
            MultidrawBackend.MultiArrays, MultidrawBackend.MultiIndirect,
            MultidrawBackend.MultiBaseVertex, MultidrawBackend.Indirect, MultidrawBackend.Unroll,
        ),
        MultidrawBackend.MultiArrays,
    ),
    ElementsBaseVertex(
        "multidrawOrderElementsBaseVertex", "multidrawModeElementsBaseVertex", "glMultiDrawElementsBaseVertex",
        listOf(
            MultidrawBackend.MultiBaseVertex, MultidrawBackend.MultiIndirect,
            MultidrawBackend.Indirect, MultidrawBackend.BaseVertex,
            MultidrawBackend.Unroll, MultidrawBackend.Compute,
        ),
        MultidrawBackend.MultiBaseVertex,
    ),
    ArraysIndirect(
        "multidrawOrderArraysIndirect", "multidrawModeArraysIndirect", "glMultiDrawArraysIndirect",
        listOf(MultidrawBackend.MultiIndirect, MultidrawBackend.Indirect),
        MultidrawBackend.MultiIndirect,
    ),
    ElementsIndirect(
        "multidrawOrderElementsIndirect", "multidrawModeElementsIndirect", "glMultiDrawElementsIndirect",
        listOf(MultidrawBackend.MultiIndirect, MultidrawBackend.Indirect),
        MultidrawBackend.MultiIndirect,
    );

    fun normalize(backends: List<MultidrawBackend>): List<MultidrawBackend> =
        (backends.filter { it in implemented }.distinct() + implemented).distinct()
}

data class MultidrawSettings(
    val globalOrder: List<MultidrawOrderItem> = MultidrawOrderItem.DefaultOrder,
    val exceptions: Map<MultidrawEntry, List<MultidrawBackend>> = emptyMap(),
) {
    fun globalOrderFor(entry: MultidrawEntry): List<MultidrawBackend> = entry.normalize(
        globalOrder.mapNotNull { item -> item.backend ?: entry.nativeBackend },
    )
    fun effectiveOrderFor(entry: MultidrawEntry): List<MultidrawBackend> =
        exceptions[entry] ?: globalOrderFor(entry)
    fun hasException(entry: MultidrawEntry): Boolean = entry in exceptions
    fun exceptionCustomized(entry: MultidrawEntry): Boolean =
        exceptions[entry]?.let { it != globalOrderFor(entry) } == true
    fun withGlobalOrder(order: List<MultidrawOrderItem>): MultidrawSettings =
        copy(globalOrder = MultidrawOrderItem.normalize(order))
    fun withException(entry: MultidrawEntry, enabled: Boolean): MultidrawSettings = copy(
        exceptions = if (enabled) exceptions + (entry to effectiveOrderFor(entry)) else exceptions - entry,
    )
    fun withExceptionOrder(entry: MultidrawEntry, order: List<MultidrawBackend>): MultidrawSettings =
        copy(exceptions = exceptions + (entry to entry.normalize(order)))
    val globalCustomized: Boolean get() = globalOrder != MultidrawOrderItem.DefaultOrder
    companion object { val Default = MultidrawSettings() }
}

// ═══════════════════════════════════════════════════════════════════════════
//  diagnostics
// ═══════════════════════════════════════════════════════════════════════════

enum class DiagLogLevel(val key: String, @StringRes private val labelRes: Int) {
    Info("info", R.string.diag_log_level_info),
    Debug("debug", R.string.diag_log_level_debug),
    Trace("trace", R.string.diag_log_level_trace),
    Error("error", R.string.diag_log_level_error);
    fun label(context: Context): CharSequence = context.getString(labelRes)
    companion object {
        fun fromKey(key: String?): DiagLogLevel =
            entries.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: Info
    }
}

data class DiagOverlay(
    val frameProfiler: Boolean = false,
    val drawCallCount: Boolean = false,
    val shaderRecompiles: Boolean = false,
    val backendTier: Boolean = false,
    val cpuGpuLoad: Boolean = false,
)

data class DiagLogging(
    val backendSelection: Boolean = false,
    val shaderRecompiles: Boolean = false,
    val drawCallCount: Boolean = false,
    val glTrace: Boolean = false,
    val level: DiagLogLevel = DiagLogLevel.Info,
)

data class DiagPerfetto(
    val enabled: Boolean = false,
    val maxDurationSec: Int = 30,
)

data class DiagConfig(
    val enabled: Boolean = false,
    val overlay: DiagOverlay = DiagOverlay(),
    val logging: DiagLogging = DiagLogging(),
    val perfetto: DiagPerfetto = DiagPerfetto(),
    val capabilityReport: Boolean = false,
)

// ═══════════════════════════════════════════════════════════════════════════
//  root
// ═══════════════════════════════════════════════════════════════════════════

data class MGConfig(
    // opengl_egl
    val angle: AngleConfig = AngleConfig.EnableIfPossible,
    val glVersion: GlVersion = GlVersion.Default,
    val hideMGEnvLevel: HideMGEnvLevel = HideMGEnvLevel.Disabled,

    // errorHandling
    val noError: NoErrorConfig = NoErrorConfig.None,
    val forceGlGetErrorSkip: Boolean = true,
    val forceDepthPrecisionFix: Boolean = false,
    val depthClearFix: DepthClearFixMode = DepthClearFixMode.Disabled,

    // gpuOptimization
    val disableComputeOnWeakGpu: Boolean = true,
    val enableExtGL43: Boolean = false,

    // shaderCache
    val glslCache: GlslCacheSize = GlslCacheSize.Default,
    val useProgramBinaryCache: Boolean = false,

    // textureBuffer
    val bufferUploadMode: BufferUploadMode = BufferUploadMode.Auto,
    val textureSwizzleMode: TextureSwizzleMode = TextureSwizzleMode.Auto,
    val maxAnisotropyOverride: MaxAnisotropyOverride = MaxAnisotropyOverride.Default,

    // multidrawEngine
    val multidrawEngine: MultidrawEngine = MultidrawEngine.Legacy,
    val vmdiEnableAutotune: Boolean = true,
    val vmdiBackendTier: VmdiBackendTier = VmdiBackendTier.Auto,
    val imdbiBackend: ImdbiBackend = ImdbiBackend.FastIndirectRing,
    val imdbiUnrollFactor: Int = 4,
    val imdbiPersistentMapping: Boolean = true,
    val imdbiRegisterPinning: Boolean = true,
    val imdbiPrimitiveRestart: Boolean = true,
    val imdbiRingSize: Int = 4 * 1024 * 1024,

    // multidrawOrder
    val multidraw: MultidrawSettings = MultidrawSettings.Default,

    // extensions
    val extComputeShader: Boolean = false,
    val extTimerQuery: Boolean = true,
    val extDirectStateAccess: Boolean = false,

    // upscaling
    val fsrEnableSharpening: Boolean = true,
    val fsr1Version: Int = 2,
    val fsr1Sharpness: Float = 0.4f,
    val fsr2Sharpness: Float = 0.5f,

    // diagnostics
    val diag: DiagConfig = DiagConfig(),
) {
    /** Alias kept for the existing UI/controller call sites: master FSR switch. */
    val fsr1Enabled: Boolean get() = fsrEnableSharpening
    val multidrawOrderingActive: Boolean get() = multidrawEngine.usesBackendOrdering

    companion object { val Default = MGConfig() }
}
