package com.fcl.plugin.mobileglues.settings

import com.google.gson.JsonObject

/**
 * `MG/config.json` — schema v3 (nested).
 *
 * Canonical layout, matching the C++ reader in `config/settings.cpp`:
 *
 *   meta, opengl_egl, errorHandling, gpuOptimization, shaderCache,
 *   textureBuffer, multidrawEngine{vmdi,imdbi}, multidrawOrder,
 *   extensions, upscaling, diagnostics
 *
 * Reading follows `mg_cfg_int_compat` semantics: a value under the nested path
 * wins; if absent, a same-named top-level key (legacy flat form) is used; if
 * still absent or ill-typed, the Kotlin default is used. Encoding always
 * writes the nested form. `diagnostics` additionally accepts the pre-v3
 * `diag` object as a fallback on read; it is not written back.
 *
 * Unknown keys are preserved by [foreignKeysOf] so a future C++ field, or a
 * key the user added by hand, survives a save.
 */
internal object MGConfigCodec {

    // ── sections ────────────────────────────────────────────────────────
    private const val S_META = "meta"
    private const val S_OPENGL_EGL = "opengl_egl"
    private const val S_ERROR_HANDLING = "errorHandling"
    private const val S_GPU_OPTIMIZATION = "gpuOptimization"
    private const val S_SHADER_CACHE = "shaderCache"
    private const val S_TEXTURE_BUFFER = "textureBuffer"
    private const val S_MULTIDRAW_ENGINE = "multidrawEngine"
    private const val S_MULTIDRAW_ORDER = "multidrawOrder"
    private const val S_EXTENSIONS = "extensions"
    private const val S_UPSCALING = "upscaling"
    private const val S_DIAGNOSTICS = "diagnostics"
    /** Pre-v3 name for [S_DIAGNOSTICS]; read-only fallback. */
    private const val S_DIAG_LEGACY = "diag"

    // ── meta ────────────────────────────────────────────────────────────
    private const val K_META_VERSION = "version"
    private const val K_META_SCHEMA = "schema"
    private const val META_VERSION_VALUE = "3.0"
    private const val META_SCHEMA_VALUE = "mobileglues-v3"

    // ── leaf keys ───────────────────────────────────────────────────────
    private const val K_ANGLE = "enableANGLE"
    private const val K_GL_VERSION = "customGLVersion"
    private const val K_HIDE_MG_ENV = "hideMGEnvLevel"

    private const val K_NO_ERROR = "enableNoError"
    private const val K_FORCE_GL_GET_ERROR_SKIP = "forceGlGetErrorSkip"
    private const val K_FORCE_DEPTH_PRECISION_FIX = "forceDepthPrecisionFix"
    private const val K_DEPTH_CLEAR_FIX = "angleDepthClearFixMode"

    private const val K_DISABLE_COMPUTE_WEAK_GPU = "disableComputeOnWeakGpu"
    private const val K_EXT_GL43 = "enableExtGL43"

    private const val K_GLSL_CACHE = "maxGlslCacheSize"
    private const val K_USE_PROGRAM_BINARY_CACHE = "useProgramBinaryCache"

    private const val K_BUFFER_UPLOAD = "bufferUploadMode"
    private const val K_TEXTURE_SWIZZLE = "textureSwizzleMode"
    private const val K_MAX_ANISOTROPY = "maxAnisotropyOverride"

    private const val K_MD_ENGINE = "multidrawEngine"
    private const val S_VMDI = "vmdi"
    private const val S_IMDBI = "imdbi"
    private const val K_VMDI_AUTOTUNE = "vmdiEnableAutotune"
    private const val K_VMDI_TIER = "vmdiBackendTier"
    private const val K_IMDBI_BACKEND = "imdbiBackend"
    private const val K_IMDBI_UNROLL = "imdbiUnrollFactor"
    private const val K_IMDBI_PERSIST = "imdbiPersistentMapping"
    private const val K_IMDBI_PINNING = "imdbiRegisterPinning"
    private const val K_IMDBI_RESTART = "imdbiPrimitiveRestart"
    private const val K_IMDBI_RING = "imdbiRingSize"

    /**
     * Global MultiDraw preference order, inside the `multidrawOrder` section.
     * The leading underscore marks it plugin-managed: the C++ reads per-entry
     * keys only and ignores this. It exists so the UI can round-trip a
     * "apply to all entries" edit without exploding it into five exceptions.
     */
    private const val K_MD_GLOBAL_ORDER = "_global"

    private const val K_EXT_CS = "enableExtComputeShader"
    private const val K_EXT_TIMER_QUERY = "enableExtTimerQuery"
    private const val K_EXT_DSA = "enableExtDirectStateAccess"

    private const val K_FSR_ENABLE_SHARP = "fsrEnableSharpening"
    private const val K_FSR1_VERSION = "fsr1Version"
    private const val K_FSR1_SHARPNESS = "fsr1Sharpness"
    private const val K_FSR2_SHARPNESS = "fsr2Sharpness"

    private const val K_DIAG_ENABLED = "enabled"
    private const val K_DIAG_OVERLAY = "overlay"
    private const val K_DIAG_LOGGING = "logging"
    private const val K_DIAG_CAPABILITY = "capabilityReport"
    private const val K_DIAG_PERFETTO = "perfetto"

    // ── legacy flat keys (read-only fallback) ───────────────────────────
    private val LEGACY_FLAT_KEYS = listOf(
        K_ANGLE, K_NO_ERROR, K_EXT_TIMER_QUERY, K_EXT_CS, K_EXT_DSA, K_EXT_GL43,
        K_DISABLE_COMPUTE_WEAK_GPU, K_GLSL_CACHE, K_DEPTH_CLEAR_FIX, K_GL_VERSION,
        K_FSR_ENABLE_SHARP, K_FSR1_VERSION, K_FSR1_SHARPNESS, K_FSR2_SHARPNESS,
        K_MD_ENGINE, K_HIDE_MG_ENV, K_FORCE_GL_GET_ERROR_SKIP, K_BUFFER_UPLOAD,
        K_TEXTURE_SWIZZLE, K_MAX_ANISOTROPY, K_FORCE_DEPTH_PRECISION_FIX,
        K_IMDBI_BACKEND, K_IMDBI_UNROLL, K_IMDBI_PERSIST, K_IMDBI_PINNING,
        K_IMDBI_RESTART, K_IMDBI_RING, K_VMDI_TIER, K_VMDI_AUTOTUNE,
        K_USE_PROGRAM_BINARY_CACHE,
        // Pre-v3 MultiDraw keys. Not written back.
        "multidrawMode", "multidrawDisableBackends",
    ) + MultidrawEntry.entries.flatMap { listOf(it.orderKey, it.legacyModeKey) }

    // ═══════════════════════════════════════════════════════════════════
    //  Decode
    // ═══════════════════════════════════════════════════════════════════

    fun decode(root: JsonObject): MGConfig = MGConfig(
        angle = AngleConfig.entries.fromWire(
            root.intAt(S_OPENGL_EGL, K_ANGLE) ?: root.intOrNull(K_ANGLE),
            AngleConfig.EnableIfPossible,
        ),
        glVersion = GlVersion.fromString(
            root.stringAt(S_OPENGL_EGL, K_GL_VERSION) ?: root.stringOrNull(K_GL_VERSION),
        ),
        hideMGEnvLevel = HideMGEnvLevel.entries.fromWire(
            root.intAt(S_OPENGL_EGL, K_HIDE_MG_ENV) ?: root.intOrNull(K_HIDE_MG_ENV),
            HideMGEnvLevel.Disabled,
        ),

        noError = NoErrorConfig.entries.fromWire(
            root.intAt(S_ERROR_HANDLING, K_NO_ERROR) ?: root.intOrNull(K_NO_ERROR),
            NoErrorConfig.None,
        ),
        forceGlGetErrorSkip = root.intAt(S_ERROR_HANDLING, K_FORCE_GL_GET_ERROR_SKIP)
            ?.let { it > 0 }
            ?: root.intOrNull(K_FORCE_GL_GET_ERROR_SKIP)?.let { it > 0 }
            ?: true,
        forceDepthPrecisionFix = root.intAt(S_ERROR_HANDLING, K_FORCE_DEPTH_PRECISION_FIX)
            ?.let { it > 0 }
            ?: root.intOrNull(K_FORCE_DEPTH_PRECISION_FIX)?.let { it > 0 }
            ?: false,
        depthClearFix = DepthClearFixMode.entries.fromWire(
            root.intAt(S_ERROR_HANDLING, K_DEPTH_CLEAR_FIX) ?: root.intOrNull(K_DEPTH_CLEAR_FIX),
            DepthClearFixMode.Disabled,
        ),

        disableComputeOnWeakGpu = root.intAt(S_GPU_OPTIMIZATION, K_DISABLE_COMPUTE_WEAK_GPU)
            ?.let { it > 0 }
            ?: root.intOrNull(K_DISABLE_COMPUTE_WEAK_GPU)?.let { it > 0 }
            ?: true,
        enableExtGL43 = root.intAt(S_GPU_OPTIMIZATION, K_EXT_GL43)
            ?.let { it > 0 }
            ?: root.intOrNull(K_EXT_GL43)?.let { it > 0 }
            ?: false,

        glslCache = GlslCacheSize.fromWire(
            root.intAt(S_SHADER_CACHE, K_GLSL_CACHE) ?: root.intOrNull(K_GLSL_CACHE),
        ),
        useProgramBinaryCache = root.intAt(S_SHADER_CACHE, K_USE_PROGRAM_BINARY_CACHE)
            ?.let { it > 0 }
            ?: root.intOrNull(K_USE_PROGRAM_BINARY_CACHE)?.let { it > 0 }
            ?: false,

        bufferUploadMode = BufferUploadMode.fromWire(
            root.intAt(S_TEXTURE_BUFFER, K_BUFFER_UPLOAD) ?: root.intOrNull(K_BUFFER_UPLOAD),
        ),
        textureSwizzleMode = TextureSwizzleMode.fromWire(
            root.intAt(S_TEXTURE_BUFFER, K_TEXTURE_SWIZZLE) ?: root.intOrNull(K_TEXTURE_SWIZZLE),
        ),
        maxAnisotropyOverride = MaxAnisotropyOverride.fromWire(
            root.intAt(S_TEXTURE_BUFFER, K_MAX_ANISOTROPY) ?: root.intOrNull(K_MAX_ANISOTROPY),
        ),

        multidrawEngine = MultidrawEngine.fromKey(
            root.stringAt(S_MULTIDRAW_ENGINE, K_MD_ENGINE) ?: root.stringOrNull(K_MD_ENGINE),
        ),
        vmdiEnableAutotune = root.intAt(S_MULTIDRAW_ENGINE, S_VMDI, K_VMDI_AUTOTUNE)
            ?.let { it > 0 }
            ?: root.intOrNull(K_VMDI_AUTOTUNE)?.let { it > 0 }
            ?: true,
        vmdiBackendTier = VmdiBackendTier.fromKey(
            root.stringAt(S_MULTIDRAW_ENGINE, S_VMDI, K_VMDI_TIER) ?: root.stringOrNull(K_VMDI_TIER),
        ),
        imdbiBackend = ImdbiBackend.fromKey(
            root.stringAt(S_MULTIDRAW_ENGINE, S_IMDBI, K_IMDBI_BACKEND) ?: root.stringOrNull(K_IMDBI_BACKEND),
        ),
        imdbiUnrollFactor = root.intAt(S_MULTIDRAW_ENGINE, S_IMDBI, K_IMDBI_UNROLL)
            ?: root.intOrNull(K_IMDBI_UNROLL)
            ?: 4,
        imdbiPersistentMapping = root.intAt(S_MULTIDRAW_ENGINE, S_IMDBI, K_IMDBI_PERSIST)
            ?.let { it > 0 }
            ?: root.intOrNull(K_IMDBI_PERSIST)?.let { it > 0 }
            ?: true,
        imdbiRegisterPinning = root.intAt(S_MULTIDRAW_ENGINE, S_IMDBI, K_IMDBI_PINNING)
            ?.let { it > 0 }
            ?: root.intOrNull(K_IMDBI_PINNING)?.let { it > 0 }
            ?: true,
        imdbiPrimitiveRestart = root.intAt(S_MULTIDRAW_ENGINE, S_IMDBI, K_IMDBI_RESTART)
            ?.let { it > 0 }
            ?: root.intOrNull(K_IMDBI_RESTART)?.let { it > 0 }
            ?: true,
        imdbiRingSize = root.intAt(S_MULTIDRAW_ENGINE, S_IMDBI, K_IMDBI_RING)
            ?: root.intOrNull(K_IMDBI_RING)
            ?: (4 * 1024 * 1024),

        multidraw = decodeMultidraw(root),

        extComputeShader = root.intAt(S_EXTENSIONS, K_EXT_CS)
            ?.let { it > 0 }
            ?: root.intOrNull(K_EXT_CS)?.let { it > 0 }
            ?: false,
        extTimerQuery = root.intAt(S_EXTENSIONS, K_EXT_TIMER_QUERY)
            ?.let { it > 0 }
            ?: root.intOrNull(K_EXT_TIMER_QUERY)?.let { it > 0 }
            ?: true,
        extDirectStateAccess = root.intAt(S_EXTENSIONS, K_EXT_DSA)
            ?.let { it > 0 }
            ?: root.intOrNull(K_EXT_DSA)?.let { it > 0 }
            ?: false,

        fsrEnableSharpening = root.intAt(S_UPSCALING, K_FSR_ENABLE_SHARP)
            ?.let { it > 0 }
            ?: root.intOrNull(K_FSR_ENABLE_SHARP)?.let { it > 0 }
            ?: true,
        fsr1Version = root.intAt(S_UPSCALING, K_FSR1_VERSION)
            ?: root.intOrNull(K_FSR1_VERSION)
            ?: 2,
        fsr1Sharpness = root.floatAt(S_UPSCALING, K_FSR1_SHARPNESS)
            ?: root.floatOrNull(K_FSR1_SHARPNESS)
            ?: 0.4f,
        fsr2Sharpness = root.floatAt(S_UPSCALING, K_FSR2_SHARPNESS)
            ?: root.floatOrNull(K_FSR2_SHARPNESS)
            ?: 0.5f,

        diag = decodeDiag(root),
    )

    private fun decodeMultidraw(root: JsonObject): MultidrawSettings {
        val section = root.getAsJsonObject(S_MULTIDRAW_ORDER)

        val globalOrder = section?.stringOrNull(K_MD_GLOBAL_ORDER)
            ?.split(',', ';')
            ?.mapNotNull { MultidrawOrderItem.parse(it) }
            ?.let { MultidrawOrderItem.normalize(it) }
            ?: MultidrawOrderItem.DefaultOrder

        val exceptions = MultidrawEntry.entries.mapNotNull { entry ->
            val raw = section?.stringOrNull(entry.orderKey)
                ?: root.stringOrNull(entry.orderKey)
                ?: return@mapNotNull null
            val parsed = entry.normalize(
                raw.split(',', ';').mapNotNull { MultidrawBackend.parse(it) },
            )
            // Storing what the default expansion would produce is not an
            // exception; treat it as "user has not overridden this entry".
            if (parsed == entry.normalize(globalOrder.mapNotNull { it.backend ?: entry.nativeBackend })) {
                null
            } else {
                entry to parsed
            }
        }.toMap()

        return MultidrawSettings(globalOrder = globalOrder, exceptions = exceptions)
    }

    private fun decodeDiag(root: JsonObject): DiagConfig {
        // `diagnostics` is canonical; `diag` is the pre-v3 name and only read.
        val source = root.getAsJsonObject(S_DIAGNOSTICS)
            ?: root.getAsJsonObject(S_DIAG_LEGACY)
            ?: return DiagConfig()

        val overlay = source.getAsJsonObject(K_DIAG_OVERLAY)
        val logging = source.getAsJsonObject(K_DIAG_LOGGING)
        val perfetto = source.getAsJsonObject(K_DIAG_PERFETTO)

        return DiagConfig(
            enabled = source.boolOrNull(K_DIAG_ENABLED) ?: false,
            overlay = DiagOverlay(
                frameProfiler    = overlay?.boolOrNull("frameProfiler")    ?: false,
                drawCallCount    = overlay?.boolOrNull("drawCallCount")    ?: false,
                shaderRecompiles = overlay?.boolOrNull("shaderRecompiles") ?: false,
                backendTier      = overlay?.boolOrNull("backendTier")      ?: false,
                cpuGpuLoad       = overlay?.boolOrNull("cpuGpuLoad")       ?: false,
            ),
            logging = DiagLogging(
                backendSelection = logging?.boolOrNull("backendSelection") ?: false,
                shaderRecompiles = logging?.boolOrNull("shaderRecompiles") ?: false,
                drawCallCount    = logging?.boolOrNull("drawCallCount")    ?: false,
                glTrace          = logging?.boolOrNull("glTrace")          ?: false,
                level            = DiagLogLevel.fromKey(logging?.stringOrNull("level")),
            ),
            perfetto = DiagPerfetto(
                enabled        = perfetto?.boolOrNull("enabled") ?: false,
                maxDurationSec = perfetto?.intOrNull("maxDurationSec") ?: 30,
            ),
            capabilityReport = source.boolOrNull(K_DIAG_CAPABILITY) ?: false,
        )
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Encode
    // ═══════════════════════════════════════════════════════════════════

    fun encode(config: MGConfig, foreignKeys: JsonObject?): JsonObject {
        val root = foreignKeys?.deepCopy() ?: JsonObject()

        // meta
        root.section(S_META).apply {
            addProperty(K_META_VERSION, META_VERSION_VALUE)
            addProperty(K_META_SCHEMA, META_SCHEMA_VALUE)
        }

        // opengl_egl
        root.section(S_OPENGL_EGL).apply {
            addProperty(K_ANGLE, config.angle.wire)
            addProperty(K_GL_VERSION, config.glVersion.jsonString)
            addProperty(K_HIDE_MG_ENV, config.hideMGEnvLevel.wire)
        }

        // errorHandling
        root.section(S_ERROR_HANDLING).apply {
            addProperty(K_NO_ERROR, config.noError.wire)
            addProperty(K_FORCE_GL_GET_ERROR_SKIP, config.forceGlGetErrorSkip.wire)
            addProperty(K_FORCE_DEPTH_PRECISION_FIX, config.forceDepthPrecisionFix.wire)
            addProperty(K_DEPTH_CLEAR_FIX, config.depthClearFix.wire)
        }

        // gpuOptimization
        root.section(S_GPU_OPTIMIZATION).apply {
            addProperty(K_DISABLE_COMPUTE_WEAK_GPU, config.disableComputeOnWeakGpu.wire)
            addProperty(K_EXT_GL43, config.enableExtGL43.wire)
        }

        // shaderCache
        root.section(S_SHADER_CACHE).apply {
            addProperty(K_GLSL_CACHE, config.glslCache.wire)
            addProperty(K_USE_PROGRAM_BINARY_CACHE, config.useProgramBinaryCache.wire)
        }

        // textureBuffer
        root.section(S_TEXTURE_BUFFER).apply {
            addProperty(K_BUFFER_UPLOAD, config.bufferUploadMode.wire)
            addProperty(K_TEXTURE_SWIZZLE, config.textureSwizzleMode.wire)
            addProperty(K_MAX_ANISOTROPY, config.maxAnisotropyOverride.wire)
        }

        // multidrawEngine { vmdi, imdbi }
        root.section(S_MULTIDRAW_ENGINE).apply {
            addProperty(K_MD_ENGINE, config.multidrawEngine.key)
            section(S_VMDI).apply {
                addProperty(K_VMDI_AUTOTUNE, config.vmdiEnableAutotune.wire)
                addProperty(K_VMDI_TIER, config.vmdiBackendTier.key)
            }
            section(S_IMDBI).apply {
                addProperty(K_IMDBI_BACKEND, config.imdbiBackend.key)
                addProperty(K_IMDBI_UNROLL, config.imdbiUnrollFactor)
                addProperty(K_IMDBI_PERSIST, config.imdbiPersistentMapping.wire)
                addProperty(K_IMDBI_PINNING, config.imdbiRegisterPinning.wire)
                addProperty(K_IMDBI_RESTART, config.imdbiPrimitiveRestart.wire)
                addProperty(K_IMDBI_RING, config.imdbiRingSize)
            }
        }

        // multidrawOrder
        root.section(S_MULTIDRAW_ORDER).apply {
            // Only write the global order when it is non-default; otherwise the
            // per-entry effective orders already say everything.
            if (config.multidraw.globalOrder != MultidrawOrderItem.DefaultOrder) {
                addProperty(
                    K_MD_GLOBAL_ORDER,
                    config.multidraw.globalOrder.joinToString(",") { it.key },
                )
            } else {
                remove(K_MD_GLOBAL_ORDER)
            }
            MultidrawEntry.entries.forEach { entry ->
                addProperty(
                    entry.orderKey,
                    config.multidraw.effectiveOrderFor(entry).joinToString(",") { it.key },
                )
            }
        }

        // extensions
        root.section(S_EXTENSIONS).apply {
            addProperty(K_EXT_CS, config.extComputeShader.wire)
            addProperty(K_EXT_TIMER_QUERY, config.extTimerQuery.wire)
            addProperty(K_EXT_DSA, config.extDirectStateAccess.wire)
        }

        // upscaling
        root.section(S_UPSCALING).apply {
            addProperty(K_FSR_ENABLE_SHARP, config.fsrEnableSharpening.wire)
            addProperty(K_FSR1_VERSION, config.fsr1Version)
            addProperty(K_FSR1_SHARPNESS, config.fsr1Sharpness)
            addProperty(K_FSR2_SHARPNESS, config.fsr2Sharpness)
        }

        // diagnostics
        root.section(S_DIAGNOSTICS).apply {
            addProperty(K_DIAG_ENABLED, config.diag.enabled)
            section(K_DIAG_OVERLAY).apply {
                addProperty("frameProfiler",    config.diag.overlay.frameProfiler)
                addProperty("drawCallCount",    config.diag.overlay.drawCallCount)
                addProperty("shaderRecompiles", config.diag.overlay.shaderRecompiles)
                addProperty("backendTier",      config.diag.overlay.backendTier)
                addProperty("cpuGpuLoad",       config.diag.overlay.cpuGpuLoad)
            }
            section(K_DIAG_LOGGING).apply {
                addProperty("backendSelection", config.diag.logging.backendSelection)
                addProperty("shaderRecompiles", config.diag.logging.shaderRecompiles)
                addProperty("drawCallCount",    config.diag.logging.drawCallCount)
                addProperty("glTrace",          config.diag.logging.glTrace)
                addProperty("level",            config.diag.logging.level.key)
            }
            addProperty(K_DIAG_CAPABILITY, config.diag.capabilityReport)
            section(K_DIAG_PERFETTO).apply {
                addProperty("enabled",        config.diag.perfetto.enabled)
                addProperty("maxDurationSec", config.diag.perfetto.maxDurationSec)
            }
        }

        return root
    }

    // ═══════════════════════════════════════════════════════════════════
    //  foreignKeysOf — what survives a save untouched
    // ═══════════════════════════════════════════════════════════════════

    /**
     * A copy of [root] with every leaf the encoder is about to write removed.
     * Whatever is left — unknown top-level keys, unknown keys inside known
     * sections, unknown sub-objects — gets carried through [encode] untouched.
     *
     * Legacy flat keys are also removed: they are read once, then never
     * rewritten, so a migrated file does not end up carrying both shapes.
     */
    fun foreignKeysOf(root: JsonObject): JsonObject = root.deepCopy().apply {
        LEGACY_FLAT_KEYS.forEach { remove(it) }

        removePath(S_META, K_META_VERSION)
        removePath(S_META, K_META_SCHEMA)

        removePath(S_OPENGL_EGL, K_ANGLE)
        removePath(S_OPENGL_EGL, K_GL_VERSION)
        removePath(S_OPENGL_EGL, K_HIDE_MG_ENV)

        removePath(S_ERROR_HANDLING, K_NO_ERROR)
        removePath(S_ERROR_HANDLING, K_FORCE_GL_GET_ERROR_SKIP)
        removePath(S_ERROR_HANDLING, K_FORCE_DEPTH_PRECISION_FIX)
        removePath(S_ERROR_HANDLING, K_DEPTH_CLEAR_FIX)

        removePath(S_GPU_OPTIMIZATION, K_DISABLE_COMPUTE_WEAK_GPU)
        removePath(S_GPU_OPTIMIZATION, K_EXT_GL43)

        removePath(S_SHADER_CACHE, K_GLSL_CACHE)
        removePath(S_SHADER_CACHE, K_USE_PROGRAM_BINARY_CACHE)

        removePath(S_TEXTURE_BUFFER, K_BUFFER_UPLOAD)
        removePath(S_TEXTURE_BUFFER, K_TEXTURE_SWIZZLE)
        removePath(S_TEXTURE_BUFFER, K_MAX_ANISOTROPY)

        removePath(S_MULTIDRAW_ENGINE, K_MD_ENGINE)
        removePath(S_MULTIDRAW_ENGINE, S_VMDI, K_VMDI_AUTOTUNE)
        removePath(S_MULTIDRAW_ENGINE, S_VMDI, K_VMDI_TIER)
        removePath(S_MULTIDRAW_ENGINE, S_IMDBI, K_IMDBI_BACKEND)
        removePath(S_MULTIDRAW_ENGINE, S_IMDBI, K_IMDBI_UNROLL)
        removePath(S_MULTIDRAW_ENGINE, S_IMDBI, K_IMDBI_PERSIST)
        removePath(S_MULTIDRAW_ENGINE, S_IMDBI, K_IMDBI_PINNING)
        removePath(S_MULTIDRAW_ENGINE, S_IMDBI, K_IMDBI_RESTART)
        removePath(S_MULTIDRAW_ENGINE, S_IMDBI, K_IMDBI_RING)

        removePath(S_MULTIDRAW_ORDER, K_MD_GLOBAL_ORDER)
        MultidrawEntry.entries.forEach { removePath(S_MULTIDRAW_ORDER, it.orderKey) }

        removePath(S_EXTENSIONS, K_EXT_CS)
        removePath(S_EXTENSIONS, K_EXT_TIMER_QUERY)
        removePath(S_EXTENSIONS, K_EXT_DSA)

        removePath(S_UPSCALING, K_FSR_ENABLE_SHARP)
        removePath(S_UPSCALING, K_FSR1_VERSION)
        removePath(S_UPSCALING, K_FSR1_SHARPNESS)
        removePath(S_UPSCALING, K_FSR2_SHARPNESS)

        removePath(S_DIAGNOSTICS, K_DIAG_ENABLED)
        removePath(S_DIAGNOSTICS, K_DIAG_OVERLAY)
        removePath(S_DIAGNOSTICS, K_DIAG_LOGGING)
        removePath(S_DIAGNOSTICS, K_DIAG_CAPABILITY)
        removePath(S_DIAGNOSTICS, K_DIAG_PERFETTO)

        // The legacy `diag` object is fully migrated to `diagnostics`.
        remove(S_DIAG_LEGACY)
    }

    // ═══════════════════════════════════════════════════════════════════
    //  JsonObject helpers
    // ═══════════════════════════════════════════════════════════════════

    private val Boolean.wire: Int get() = if (this) 1 else 0

    private fun JsonObject.section(name: String): JsonObject {
        val existing = get(name)
        if (existing != null && existing.isJsonObject) return existing.asJsonObject
        return JsonObject().also { add(name, it) }
    }


    private fun JsonObject.removePath(vararg path: String) {
        if (path.isEmpty()) return
        var current: JsonObject = this
        for (i in 0 until path.size - 1) {
            val next = current.get(path[i])
            current = if (next != null && next.isJsonObject) next.asJsonObject else return
        }
        current.remove(path.last())
    }

    private fun JsonObject.intOrNull(key: String): Int? {
        val element = get(key) ?: return null
        if (!element.isJsonPrimitive) return null
        val primitive = element.asJsonPrimitive
        return runCatching {
            if (primitive.isNumber) primitive.asInt else primitive.asString.trim().toInt()
        }.getOrNull()
    }

    private fun JsonObject.floatOrNull(key: String): Float? {
        val element = get(key) ?: return null
        if (!element.isJsonPrimitive) return null
        val primitive = element.asJsonPrimitive
        return runCatching {
            if (primitive.isNumber) primitive.asFloat else primitive.asString.trim().toFloat()
        }.getOrNull()
    }

    private fun JsonObject.boolOrNull(key: String): Boolean? {
        val element = get(key) ?: return null
        if (!element.isJsonPrimitive) return null
        val primitive = element.asJsonPrimitive
        return runCatching {
            if (primitive.isBoolean) primitive.asBoolean
            else if (primitive.isNumber) primitive.asInt > 0
            else when (primitive.asString.trim().lowercase()) {
                "true", "1", "yes" -> true
                "false", "0", "no" -> false
                else -> return null
            }
        }.getOrNull()
    }

    private fun JsonObject.stringOrNull(key: String): String? {
        val element = get(key) ?: return null
        if (!element.isJsonPrimitive) return null
        return runCatching { element.asString }.getOrNull()
    }

    // ── dotted-path readers: section.key or section.sub.key ─────────────
    private fun JsonObject.intAt(vararg path: String): Int? =
        at(*path)?.let { element ->
            if (!element.isJsonPrimitive) return@let null
            val primitive = element.asJsonPrimitive
            runCatching {
                if (primitive.isNumber) primitive.asInt
                else primitive.asString.trim().toInt()
            }.getOrNull()
        }

    private fun JsonObject.floatAt(vararg path: String): Float? =
        at(*path)?.let { element ->
            if (!element.isJsonPrimitive) return@let null
            val primitive = element.asJsonPrimitive
            runCatching {
                if (primitive.isNumber) primitive.asFloat
                else primitive.asString.trim().toFloat()
            }.getOrNull()
        }

    private fun JsonObject.stringAt(vararg path: String): String? =
        at(*path)?.let { element ->
            if (!element.isJsonPrimitive) return@let null
            runCatching { element.asString }.getOrNull()
        }

    private fun JsonObject.at(vararg path: String): com.google.gson.JsonElement? {
        if (path.isEmpty()) return null
        var current: JsonObject = this
        for (i in 0 until path.size - 1) {
            val next = current.get(path[i])
            current = if (next != null && next.isJsonObject) next.asJsonObject else return null
        }
        return current.get(path.last())
    }
}
