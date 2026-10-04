package com.fcl.plugin.mobileglues.settings

import com.google.gson.JsonObject

/**
 * `MG/config.json`.
 *
 * The C++ reader is `config_get_int(name)` → `cJSON_GetObjectItem(config_json,
 * name)` — **top level only**, no nested lookup, no dotted paths. So this codec
 * writes every key flat, at the root, and every key is one the lib reads:
 *
 *     enableANGLE, customGLVersion, hideMGEnvLevel, enableNoError,
 *     angleDepthClearFixMode, maxShaderCacheSize, useProgramBinaryCache,
 *     enableExtComputeShader, enableExtTimerQuery, enableExtDirectStateAccess,
 *     fsr1Setting
 *
 * `useProgramBinaryCache` is written ahead of the reader — it is the switch for
 * the program-binary cache that lands in the lib in the same pass that fixes
 * `maxShaderCacheSize`; until then the reader simply skips it.
 *
 * Reading still accepts the nested v3 layout this file used to carry, so an
 * upgrade does not silently drop settings; [foreignKeysOf] then drops those
 * sections on the next save, which is what makes the file flat again.
 *
 * Unknown top-level keys the lib might grow later are preserved by
 * [foreignKeysOf]. Keys the lib provably never reads are scrubbed instead of
 * preserved — see [DEAD_FLAT_KEYS].
 */
internal object MGConfigCodec {

    // ── live keys (must mirror config/settings.cpp) ─────────────────────
    private const val K_ANGLE = "enableANGLE"
    private const val K_GL_VERSION = "customGLVersion"
    private const val K_HIDE_MG_ENV = "hideMGEnvLevel"
    private const val K_NO_ERROR = "enableNoError"
    private const val K_DEPTH_CLEAR_FIX = "angleDepthClearFixMode"
    private const val K_SHADER_CACHE = "maxShaderCacheSize"
    private const val K_USE_PROGRAM_BINARY_CACHE = "useProgramBinaryCache"
    private const val K_EXT_CS = "enableExtComputeShader"
    private const val K_EXT_TIMER_QUERY = "enableExtTimerQuery"
    private const val K_EXT_DSA = "enableExtDirectStateAccess"
    private const val K_FSR1_SETTING = "fsr1Setting"

    /** Section each key used to live in, under the old nested v3 layout. */
    private val LEGACY_SECTION = mapOf(
        K_ANGLE to "opengl_egl",
        K_GL_VERSION to "opengl_egl",
        K_HIDE_MG_ENV to "opengl_egl",
        K_NO_ERROR to "errorHandling",
        K_DEPTH_CLEAR_FIX to "errorHandling",
        K_SHADER_CACHE to "shaderCache",
        K_USE_PROGRAM_BINARY_CACHE to "shaderCache",
        K_EXT_CS to "extensions",
        K_EXT_TIMER_QUERY to "extensions",
        K_EXT_DSA to "extensions",
        K_FSR1_SETTING to "upscaling",
    )

    /**
     * Everything the lib provably does not read, dropped on save.
     *
     * A key missing from `config/settings.cpp` has no business surviving in the
     * file: it is a setting that cannot be set, and leaving it behind makes the
     * next reader guess about intent. `maxGlslCacheSize` is here rather than
     * being renamed in place so the old spelling is scrubbed on the first save.
     */
    private val DEAD_FLAT_KEYS = listOf(
        "maxGlslCacheSize",
        "forceGlGetErrorSkip", "forceDepthPrecisionFix",
        "disableComputeOnWeakGpu", "enableExtGL43",
        "bufferUploadMode", "textureSwizzleMode", "maxAnisotropyOverride",
        "multidrawEngine", "vmdiEnableAutotune", "vmdiBackendTier",
        "imdbiBackend", "imdbiUnrollFactor", "imdbiPersistentMapping",
        "imdbiRegisterPinning", "imdbiPrimitiveRestart", "imdbiRingSize",
        "multidrawOrder", "multidrawMode", "multidrawDisableBackends",
        "fsrEnableSharpening", "fsr1Version", "fsr1Sharpness", "fsr2Sharpness",
        // Per-entry MultiDraw order/mode leaves.
        "multidrawOrderArrays", "multidrawOrderElements",
        "multidrawOrderElementsBaseVertex", "multidrawOrderArraysIndirect",
        "multidrawOrderElementsIndirect",
        "multidrawModeArrays", "multidrawModeElements",
        "multidrawModeElementsBaseVertex", "multidrawModeArraysIndirect",
        "multidrawModeElementsIndirect",
    )

    /** Whole sections this encoder used to own; gone now that keys are flat. */
    private val DEAD_SECTIONS = listOf(
        "meta", "opengl_egl", "errorHandling", "gpuOptimization",
        "shaderCache", "textureBuffer", "multidrawEngine", "multidrawOrder",
        "extensions", "upscaling", "diagnostics", "diag",
    )

    /** Every key this encoder writes — removed so encode is the only writer. */
    private val LIVE_KEYS = listOf(
        K_ANGLE, K_GL_VERSION, K_HIDE_MG_ENV, K_NO_ERROR, K_DEPTH_CLEAR_FIX,
        K_SHADER_CACHE, K_USE_PROGRAM_BINARY_CACHE,
        K_EXT_CS, K_EXT_TIMER_QUERY, K_EXT_DSA, K_FSR1_SETTING,
    )

    // ═══════════════════════════════════════════════════════════════════
    //  Decode — flat first, then the old nested section
    // ═══════════════════════════════════════════════════════════════════

    fun decode(root: JsonObject): MGConfig = MGConfig(
        angle = AngleConfig.entries.fromWire(
            readInt(root, K_ANGLE),
            AngleConfig.EnableIfPossible,
        ),
        glVersion = GlVersion.fromWire(readInt(root, K_GL_VERSION)),
        hideMGEnvLevel = HideMGEnvLevel.entries.fromWire(
            readInt(root, K_HIDE_MG_ENV),
            HideMGEnvLevel.Disabled,
        ),

        noError = NoErrorConfig.entries.fromWire(
            readInt(root, K_NO_ERROR),
            NoErrorConfig.Auto,
        ),
        depthClearFix = DepthClearFixMode.entries.fromWire(
            readInt(root, K_DEPTH_CLEAR_FIX),
            DepthClearFixMode.Disabled,
        ),

        glslCache = GlslCacheSize.fromWire(
            // The old spelling, still accepted so a 56 MiB setting written by
            // an older build does not silently become 32.
            readInt(root, K_SHADER_CACHE) ?: readInt(root, "maxGlslCacheSize"),
        ),
        useProgramBinaryCache = (readInt(root, K_USE_PROGRAM_BINARY_CACHE) ?: 0) > 0,

        extComputeShader = (readInt(root, K_EXT_CS) ?: 0) > 0,
        extTimerQuery = (readInt(root, K_EXT_TIMER_QUERY) ?: 1) > 0,
        extDirectStateAccess = (readInt(root, K_EXT_DSA) ?: 0) > 0,

        fsr1Setting = Fsr1Preset.fromWire(readInt(root, K_FSR1_SETTING)),
    )

    private fun readInt(root: JsonObject, key: String): Int? =
        root.intOrNull(key) ?: LEGACY_SECTION[key]?.let { section ->
            // getAsJsonObject throws when the section is present but scalar — one
            // hand-edited `"opengl_egl": null` must not take the whole config down.
            root.get(section)
                ?.takeIf { it.isJsonObject }
                ?.asJsonObject
                ?.intOrNull(key)
        }

    // ═══════════════════════════════════════════════════════════════════
    //  Encode — flat, top level, ints only
    // ═══════════════════════════════════════════════════════════════════

    fun encode(config: MGConfig, foreignKeys: JsonObject?): JsonObject {
        val root = foreignKeys?.deepCopy() ?: JsonObject()

        root.addProperty(K_ANGLE, config.angle.wire)
        root.addProperty(K_GL_VERSION, config.glVersion.wire)
        root.addProperty(K_HIDE_MG_ENV, config.hideMGEnvLevel.wire)
        root.addProperty(K_NO_ERROR, config.noError.wire)
        root.addProperty(K_DEPTH_CLEAR_FIX, config.depthClearFix.wire)
        root.addProperty(K_SHADER_CACHE, config.glslCache.wire)
        root.addProperty(K_USE_PROGRAM_BINARY_CACHE, config.useProgramBinaryCache.wire)
        root.addProperty(K_EXT_CS, config.extComputeShader.wire)
        root.addProperty(K_EXT_TIMER_QUERY, config.extTimerQuery.wire)
        root.addProperty(K_EXT_DSA, config.extDirectStateAccess.wire)
        root.addProperty(K_FSR1_SETTING, config.fsr1Setting.wire)

        return root
    }

    private val Boolean.wire: Int get() = if (this) 1 else 0

    // ═══════════════════════════════════════════════════════════════════
    //  foreignKeysOf — what survives a save untouched
    // ═══════════════════════════════════════════════════════════════════

    /**
     * A copy of [root] with this encoder's own leaves removed — so encode is
     * the only writer — and with every key the lib provably never reads
     * removed too. What is left, unknown keys of a future reader, is carried
     * through [encode] untouched.
     */
    fun foreignKeysOf(root: JsonObject): JsonObject = root.deepCopy().apply {
        LIVE_KEYS.forEach { remove(it) }
        DEAD_FLAT_KEYS.forEach { remove(it) }
        DEAD_SECTIONS.forEach { remove(it) }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  JsonObject helpers
    // ═══════════════════════════════════════════════════════════════════

    private fun JsonObject.intOrNull(key: String): Int? {
        val element = get(key) ?: return null
        if (!element.isJsonPrimitive) return null
        val primitive = element.asJsonPrimitive
        return runCatching {
            if (primitive.isNumber) primitive.asInt else primitive.asString.trim().toInt()
        }.getOrNull()
    }
}
