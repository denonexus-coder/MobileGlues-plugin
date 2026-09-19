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

// ── Enums ─────────────────────────────────────────────────────────────
enum class AngleConfig(
    override val wire: Int,
    @param:StringRes private val labelRes: Int,
) : SpinnerOption {
    DisableIfPossible(0, R.string.option_angle_disable_if_possible),
    EnableIfPossible(1, R.string.option_angle_enable_if_possible),
    ForceDisable(2, R.string.option_angle_disable),
    ForceEnable(3, R.string.option_angle_enable);
    override fun label(context: Context): CharSequence = context.getString(labelRes)
}

enum class NoErrorConfig(
    override val wire: Int,
    @param:StringRes private val labelRes: Int,
) : SpinnerOption {
    Auto(0, R.string.option_no_error_auto),
    DoNotIgnore(1, R.string.option_no_error_enable),
    IgnoreShaderProgram(2, R.string.option_no_error_disable_pri),
    IgnoreShaderProgramFramebuffer(3, R.string.option_no_error_disable_sec);
    override fun label(context: Context): CharSequence = context.getString(labelRes)
}

enum class AngleDepthClearFixMode(
    override val wire: Int,
    @param:StringRes private val labelRes: Int,
) : SpinnerOption {
    Disabled(0, R.string.option_depth_fix_disabled),
    Mode1(1, R.string.option_depth_fix_mode1);
    override fun label(context: Context): CharSequence = context.getString(labelRes)
}

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
}

enum class Fsr1Preset(
    override val wire: Int,
    @param:StringRes private val labelRes: Int,
) : SpinnerOption {
    Disabled(0, R.string.option_fsr1_disabled),
    UltraQuality(1, R.string.option_fsr1_ultra_quality),
    Quality(2, R.string.option_fsr1_quality),
    Balanced(3, R.string.option_fsr1_balanced),
    Performance(4, R.string.option_fsr1_performance);
    override fun label(context: Context): CharSequence = context.getString(labelRes)
}

enum class HideMGEnvLevel(
    override val wire: Int,
    @param:StringRes private val labelRes: Int,
) : SpinnerOption {
    Disabled(0, R.string.option_hide_mg_disabled),
    Level1(1, R.string.option_hide_mg_level1);
    override fun label(context: Context): CharSequence = context.getString(labelRes)
}

enum class GlVersion(override val wire: Int, private val literal: String?) : SpinnerOption {
    Default(0, null),
    Gl46(46, "OpenGL 4.6"), Gl43(43, "OpenGL 4.3"), Gl42(42, "OpenGL 4.2"),
    Gl41(41, "OpenGL 4.1"), Gl40(40, "OpenGL 4.0"), Gl33(33, "OpenGL 3.3"),
    Gl32(32, "OpenGL 3.2");
    override fun label(context: Context): CharSequence =
        literal ?: context.getString(R.string.option_custom_gl_version_default)
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
    }
}

// ── Multidraw backend / order ─────────────────────────────────────────
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
            val n = raw?.filterNot { it == ' ' || it == '\t' || it == '_' || it == '-' }
                ?.lowercase()?.takeIf { it.isNotEmpty() } ?: return null
            return entries.firstOrNull { it.key == n }
        }
    }
}

enum class MultidrawOrderItem(
    val key: String,
    val backend: MultidrawBackend?,
    @param:StringRes private val labelRes: Int,
) {
    Auto("auto", null, R.string.md_order_auto),
    Unroll("unroll", MultidrawBackend.Unroll, R.string.md_backend_unroll),
    BaseVertex("basevertex", MultidrawBackend.BaseVertex, R.string.md_backend_basevertex),
    Indirect("indirect", MultidrawBackend.Indirect, R.string.md_backend_indirect),
    MultiArrays("multiarrays", MultidrawBackend.MultiArrays, R.string.md_backend_multiarrays),
    MultiBaseVertex("multibasevertex", MultidrawBackend.MultiBaseVertex, R.string.md_backend_multibasevertex),
    MultiIndirect("multiindirect", MultidrawBackend.MultiIndirect, R.string.md_backend_multiindirect),
    Compute("compute", MultidrawBackend.Compute, R.string.md_backend_compute);

    fun label(context: Context): CharSequence = context.getString(labelRes)

    companion object {
        val DefaultOrder: List<MultidrawOrderItem> = entries.toList()
        fun parse(raw: String?): MultidrawOrderItem? {
            val n = raw?.filterNot { it == ' ' || it == '\t' || it == '_' || it == '-' }
                ?.lowercase()?.takeIf { it.isNotEmpty() } ?: return null
            return entries.firstOrNull { it.key == n }
        }
    }
}

// ── GLSL cache size ───────────────────────────────────────────────────
sealed interface GlslCacheSize {
    val wire: Int

    data object Disabled : GlslCacheSize {
        override val wire: Int get() = -1
    }

    data class Limited(val mebibytes: Int) : GlslCacheSize {
        init { require(mebibytes > 0) { "must be positive, got $mebibytes" } }
        override val wire: Int get() = mebibytes
    }

    val mebibytesOrZero: Int get() = (this as? Limited)?.mebibytes ?: 0

    companion object {
        val Default: GlslCacheSize = Limited(32)
        fun ofMebibytes(mb: Int): GlslCacheSize = if (mb > 0) Limited(mb) else Disabled
        fun fromWire(wire: Int?): GlslCacheSize = when {
            wire == null -> Default
            wire > 0 -> Limited(wire)
            else -> Disabled
        }
    }
}

// ── Layer 3 — Debug ───────────────────────────────────────────────────
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
    val level: String = "info",
)

data class DiagPerfetto(
    val enabled: Boolean = false,
    val maxDurationSec: Int = 30,
    val categories: List<String> = listOf("gfx", "sched", "freq"),
)

data class DiagConfig(
    val enabled: Boolean = false,
    val overlay: DiagOverlay = DiagOverlay(),
    val logging: DiagLogging = DiagLogging(),
    val capabilityReport: Boolean = false,
    val perfetto: DiagPerfetto = DiagPerfetto(),
)

// ── Config raiz ───────────────────────────────────────────────────────
data class MGConfig(
    val angle: AngleConfig = AngleConfig.EnableIfPossible,
    val noError: NoErrorConfig = NoErrorConfig.Auto,
    val depthClearFix: AngleDepthClearFixMode = AngleDepthClearFixMode.Disabled,
    val glVersion: GlVersion = GlVersion.Default,
    val fsr1: Fsr1Preset = Fsr1Preset.Disabled,
    val fsr1Sharpness: Float = 0.75f,
    val glslCacheSize: GlslCacheSize = GlslCacheSize.Default,
    val hideMGEnvLevel: HideMGEnvLevel = HideMGEnvLevel.Disabled,
    val multidrawEngine: MultidrawEngine = MultidrawEngine.Legacy,
    val multidrawGlobalOrder: List<String> = MultidrawOrderItem.DefaultOrder.map { it.key },
    val extTimerQuery: Boolean = false,
    val extComputeShader: Boolean = false,
    val extDirectStateAccess: Boolean = false,
    val extGL43: Boolean = false,
    val bufferUploadMode: Int = 0,
    val textureSwizzleMode: Int = 0,
    val maxAnisotropyOverride: Int = 0,
    val forceGlGetErrorSkip: Boolean = true,
    val forceDepthPrecisionFix: Boolean = false,
    val diag: DiagConfig = DiagConfig(),
) {
    companion object { val Default = MGConfig() }
}
