package com.fcl.plugin.mobileglues.settings

import com.google.gson.JsonObject

/**
 * `MG/config.json` 的读写格式.
 *
 * 这里是键名、以及「磁盘上的值不合法时用什么」的唯一定义处；除此之外没有第二个地方
 * 知道 config.json 长什么样。
 *
 * Schema v3: adiciona hideMGEnvLevel, enableExtGL43, forceGlGetErrorSkip,
 * bufferUploadMode, textureSwizzleMode, maxAnisotropyOverride,
 * forceDepthPrecisionFix e o objeto aninhado `diag` (Layer 3).
 */
internal object MGConfigCodec {

    // ── Layer 2 — básicos ────────────────────────────────────────────────
    private const val KEY_ANGLE = "enableANGLE"
    private const val KEY_NO_ERROR = "enableNoError"
    private const val KEY_EXT_TIMER_QUERY = "enableExtTimerQuery"
    private const val KEY_EXT_COMPUTE_SHADER = "enableExtComputeShader"
    private const val KEY_EXT_DIRECT_STATE_ACCESS = "enableExtDirectStateAccess"
    private const val KEY_EXT_GL43 = "enableExtGL43"
    private const val KEY_GLSL_CACHE = "maxGlslCacheSize"

    // ── MultiDraw ────────────────────────────────────────────────────────
    private const val KEY_MULTIDRAW_LEGACY = "multidrawMode"
    private const val KEY_MULTIDRAW_DISABLE_LEGACY = "multidrawDisableBackends"
    private const val KEY_MULTIDRAW_ORDER = "multidrawOrder"
    private const val KEY_MULTIDRAW_ENGINE = "multidrawEngine"
    private const val KEY_ENABLE_VMDI = "enableVMDI"
    private const val KEY_ENABLE_IMDBI = "enableIMDBI"

    // ── Driver / qualidade ───────────────────────────────────────────────
    private const val KEY_DEPTH_CLEAR_FIX = "angleDepthClearFixMode"
    private const val KEY_GL_VERSION = "customGLVersion"
    private const val KEY_FSR1 = "fsr1Setting"
    private const val KEY_FSR1_SHARPNESS = "fsr1Sharpness"

    // ── Avançado (novos v3) ──────────────────────────────────────────────
    private const val KEY_HIDE_MG_ENV = "hideMGEnvLevel"
    private const val KEY_FORCE_GL_GET_ERROR_SKIP = "forceGlGetErrorSkip"
    private const val KEY_BUFFER_UPLOAD = "bufferUploadMode"
    private const val KEY_TEXTURE_SWIZZLE = "textureSwizzleMode"
    private const val KEY_MAX_ANISOTROPY = "maxAnisotropyOverride"
    private const val KEY_FORCE_DEPTH_PRECISION_FIX = "forceDepthPrecisionFix"

    // ── Fase 3B — Submodes IMDBI / VMDI ──────────────────────────────────
    private const val KEY_IMDBI_BACKEND = "imdbiBackend"
    private const val KEY_IMDBI_UNROLL = "imdbiUnrollFactor"
    private const val KEY_IMDBI_PERSIST = "imdbiPersistentMapping"
    private const val KEY_IMDBI_PINNING = "imdbiRegisterPinning"
    private const val KEY_IMDBI_RESTART = "imdbiPrimitiveRestart"
    private const val KEY_IMDBI_RING = "imdbiRingSize"
    private const val KEY_VMDI_TIER = "vmdiBackendTier"
    private const val KEY_VMDI_AUTOTUNE = "vmdiEnableAutotune"
    private const val KEY_USE_PROGRAM_BINARY_CACHE = "useProgramBinaryCache"

    // ── Layer 3 — Debug ──────────────────────────────────────────────────
    private const val KEY_DIAG = "diag"

    private val KNOWN_KEYS = listOf(
        KEY_ANGLE,
        KEY_NO_ERROR,
        KEY_EXT_TIMER_QUERY,
        KEY_EXT_COMPUTE_SHADER,
        KEY_EXT_DIRECT_STATE_ACCESS,
        KEY_EXT_GL43,
        KEY_GLSL_CACHE,
        KEY_MULTIDRAW_LEGACY,
        KEY_MULTIDRAW_DISABLE_LEGACY,
        KEY_MULTIDRAW_ORDER,
        KEY_DEPTH_CLEAR_FIX,
        KEY_GL_VERSION,
        KEY_FSR1,
        KEY_FSR1_SHARPNESS,
        KEY_MULTIDRAW_ENGINE,
        KEY_ENABLE_VMDI,
        KEY_ENABLE_IMDBI,
        KEY_HIDE_MG_ENV,
        KEY_FORCE_GL_GET_ERROR_SKIP,
        KEY_BUFFER_UPLOAD,
        KEY_TEXTURE_SWIZZLE,
        KEY_MAX_ANISOTROPY,
        KEY_FORCE_DEPTH_PRECISION_FIX,
        KEY_IMDBI_BACKEND,
        KEY_IMDBI_UNROLL,
        KEY_IMDBI_PERSIST,
        KEY_IMDBI_PINNING,
        KEY_IMDBI_RESTART,
        KEY_IMDBI_RING,
        KEY_VMDI_TIER,
        KEY_VMDI_AUTOTUNE,
        KEY_USE_PROGRAM_BINARY_CACHE,
        KEY_DIAG,
    ) + MultidrawEntry.entries.flatMap { listOf(it.orderKey, it.legacyModeKey) }

    // ── Decode ────────────────────────────────────────────────────────────
    fun decode(root: JsonObject): MGConfig {
        val defaults = MGConfig.Default
        return MGConfig(
            angle = AngleConfig.entries.fromWire(root.intOrNull(KEY_ANGLE), AngleConfig.DisableIfPossible),
            noError = NoErrorConfig.entries.fromWire(root.intOrNull(KEY_NO_ERROR), defaults.noError),
            multidraw = decodeMultidraw(root),
            depthClearFix = DepthClearFixMode.entries
                .fromWire(root.intOrNull(KEY_DEPTH_CLEAR_FIX), defaults.depthClearFix),
            glVersion = GlVersion.fromWire(root.intOrNull(KEY_GL_VERSION)),
            glslCache = GlslCacheSize.fromWire(root.intOrNull(KEY_GLSL_CACHE)),
            extComputeShader = root.boolOrNull(KEY_EXT_COMPUTE_SHADER) ?: defaults.extComputeShader,
            extTimerQuery = root.boolOrNull(KEY_EXT_TIMER_QUERY) ?: defaults.extTimerQuery,
            extDirectStateAccess = root.boolOrNull(KEY_EXT_DIRECT_STATE_ACCESS) ?: defaults.extDirectStateAccess,
            fsr1 = Fsr1Preset.entries.fromWire(root.intOrNull(KEY_FSR1), defaults.fsr1),
            fsr1Sharpness = root.floatOrNull(KEY_FSR1_SHARPNESS) ?: defaults.fsr1Sharpness,
            multidrawEngine = MultidrawEngine.fromKey(root.stringOrNull(KEY_MULTIDRAW_ENGINE)),
            enableVMDI = root.boolOrNull(KEY_ENABLE_VMDI) ?: defaults.enableVMDI,
            enableIMDBI = root.boolOrNull(KEY_ENABLE_IMDBI) ?: defaults.enableIMDBI,

            // ── v3 ──
            hideMGEnvLevel = HideMGEnvLevel.entries
                .fromWire(root.intOrNull(KEY_HIDE_MG_ENV), defaults.hideMGEnvLevel),
            enableExtGL43 = root.boolOrNull(KEY_EXT_GL43) ?: defaults.enableExtGL43,
            forceGlGetErrorSkip = root.boolOrNull(KEY_FORCE_GL_GET_ERROR_SKIP) ?: defaults.forceGlGetErrorSkip,
            bufferUploadMode = BufferUploadMode.fromWire(root.intOrNull(KEY_BUFFER_UPLOAD)),
            textureSwizzleMode = TextureSwizzleMode.fromWire(root.intOrNull(KEY_TEXTURE_SWIZZLE)),
            maxAnisotropyOverride = MaxAnisotropyOverride.fromWire(root.intOrNull(KEY_MAX_ANISOTROPY)),
            forceDepthPrecisionFix = root.boolOrNull(KEY_FORCE_DEPTH_PRECISION_FIX) ?: defaults.forceDepthPrecisionFix,
            diag = decodeDiag(root.getAsJsonObject(KEY_DIAG)),

            // ── Fase 3B — Submodes IMDBI / VMDI ──
            imdbiBackend = ImdbiBackend.fromKey(root.stringOrNull(KEY_IMDBI_BACKEND)),
            imdbiUnrollFactor = root.intOrNull(KEY_IMDBI_UNROLL) ?: 4,
            imdbiPersistentMapping = root.boolOrNull(KEY_IMDBI_PERSIST) ?: true,
            imdbiRegisterPinning = root.boolOrNull(KEY_IMDBI_PINNING) ?: true,
            imdbiPrimitiveRestart = root.boolOrNull(KEY_IMDBI_RESTART) ?: true,
            imdbiRingSizeKb = root.intOrNull(KEY_IMDBI_RING) ?: 4096,
            vmdiBackendTier = VmdiBackendTier.fromKey(root.stringOrNull(KEY_VMDI_TIER)),
            vmdiEnableAutotune = root.boolOrNull(KEY_VMDI_AUTOTUNE) ?: true,
            useProgramBinaryCache = root.boolOrNull(KEY_USE_PROGRAM_BINARY_CACHE) ?: defaults.useProgramBinaryCache,
        )
    }

    // ── Encode ────────────────────────────────────────────────────────────
    fun encode(config: MGConfig, foreignKeys: JsonObject?): JsonObject =
        (foreignKeys?.deepCopy() ?: JsonObject()).apply {
            addProperty(KEY_ANGLE, config.angle.wire)
            addProperty(KEY_NO_ERROR, config.noError.wire)
            addProperty(KEY_EXT_TIMER_QUERY, config.extTimerQuery.wire)
            addProperty(KEY_EXT_COMPUTE_SHADER, config.extComputeShader.wire)
            addProperty(KEY_EXT_DIRECT_STATE_ACCESS, config.extDirectStateAccess.wire)
            addProperty(KEY_EXT_GL43, config.enableExtGL43.wire)
            addProperty(KEY_GLSL_CACHE, config.glslCache.wire)
            addProperty(KEY_DEPTH_CLEAR_FIX, config.depthClearFix.wire)
            addProperty(KEY_GL_VERSION, config.glVersion.wire)
            addProperty(KEY_FSR1, config.fsr1.wire)
            addProperty(KEY_FSR1_SHARPNESS, config.fsr1Sharpness)
            addProperty(KEY_MULTIDRAW_ENGINE, config.multidrawEngine.key)
            addProperty(KEY_ENABLE_VMDI, config.enableVMDI.wire)
            addProperty(KEY_ENABLE_IMDBI, config.enableIMDBI.wire)

            // ── v3 ──
            addProperty(KEY_HIDE_MG_ENV, config.hideMGEnvLevel.wire)
            addProperty(KEY_FORCE_GL_GET_ERROR_SKIP, config.forceGlGetErrorSkip.wire)
            addProperty(KEY_BUFFER_UPLOAD, config.bufferUploadMode.wire)
            addProperty(KEY_TEXTURE_SWIZZLE, config.textureSwizzleMode.wire)
            addProperty(KEY_MAX_ANISOTROPY, config.maxAnisotropyOverride.wire)
            addProperty(KEY_FORCE_DEPTH_PRECISION_FIX, config.forceDepthPrecisionFix.wire)
            add(KEY_DIAG, encodeDiag(config.diag))

            // ── Fase 3B — Submodes IMDBI / VMDI ──
            addProperty(KEY_IMDBI_BACKEND, config.imdbiBackend.key)
            addProperty(KEY_IMDBI_UNROLL, config.imdbiUnrollFactor)
            addProperty(KEY_IMDBI_PERSIST, config.imdbiPersistentMapping.wire)
            addProperty(KEY_IMDBI_PINNING, config.imdbiRegisterPinning.wire)
            addProperty(KEY_IMDBI_RESTART, config.imdbiPrimitiveRestart.wire)
            addProperty(KEY_IMDBI_RING, config.imdbiRingSizeKb)
            addProperty(KEY_VMDI_TIER, config.vmdiBackendTier.key)
            addProperty(KEY_VMDI_AUTOTUNE, config.vmdiEnableAutotune.wire)
            addProperty(KEY_USE_PROGRAM_BINARY_CACHE, config.useProgramBinaryCache.wire)

            encodeMultidraw(config.multidraw)
        }

    // ── MultiDraw ─────────────────────────────────────────────────────────
    private fun decodeMultidraw(root: JsonObject): MultidrawSettings = MultidrawSettings(
        globalOrder = MultidrawOrderItem.normalize(
            root.stringOrNull(KEY_MULTIDRAW_ORDER)
                .orEmpty()
                .split(',', ';')
                .mapNotNull { MultidrawOrderItem.parse(it) },
        ),
        exceptions = MultidrawEntry.entries.mapNotNull { entry ->
            val raw = root.stringOrNull(entry.orderKey) ?: return@mapNotNull null
            entry to entry.normalize(raw.split(',', ';').mapNotNull { MultidrawBackend.parse(it) })
        }.toMap(),
    )

    private fun JsonObject.encodeMultidraw(settings: MultidrawSettings) {
        if (settings.globalOrder == MultidrawOrderItem.DefaultOrder) {
            remove(KEY_MULTIDRAW_ORDER)
        } else {
            addProperty(KEY_MULTIDRAW_ORDER, settings.globalOrder.joinToString(",") { it.key })
        }
        MultidrawEntry.entries.forEach { entry ->
            val exception = settings.exceptions[entry]
            if (exception == null) {
                remove(entry.orderKey)
            } else {
                addProperty(entry.orderKey, exception.joinToString(",") { it.key })
            }
            remove(entry.legacyModeKey)
        }
        remove(KEY_MULTIDRAW_LEGACY)
        remove(KEY_MULTIDRAW_DISABLE_LEGACY)
    }

    // ── Diag (Layer 3, aninhado) ──────────────────────────────────────────
    private fun decodeDiag(obj: JsonObject?): DiagConfig {
        if (obj == null) return DiagConfig()
        val overlay = obj.getAsJsonObject("overlay")
        val logging = obj.getAsJsonObject("logging")
        val perfetto = obj.getAsJsonObject("perfetto")
        return DiagConfig(
            enabled = obj.boolOrNull("enabled") ?: false,
            overlay = DiagOverlay(
                frameProfiler    = overlay?.boolOrNull("frameProfiler") ?: false,
                drawCallCount    = overlay?.boolOrNull("drawCallCount") ?: false,
                shaderRecompiles = overlay?.boolOrNull("shaderRecompiles") ?: false,
                backendTier      = overlay?.boolOrNull("backendTier") ?: false,
                cpuGpuLoad       = overlay?.boolOrNull("cpuGpuLoad") ?: false,
            ),
            logging = DiagLogging(
                backendSelection = logging?.boolOrNull("backendSelection") ?: false,
                shaderRecompiles = logging?.boolOrNull("shaderRecompiles") ?: false,
                drawCallCount    = logging?.boolOrNull("drawCallCount") ?: false,
                glTrace          = logging?.boolOrNull("glTrace") ?: false,
                level            = logging?.stringOrNull("level") ?: "info",
            ),
            perfetto = DiagPerfetto(
                enabled        = perfetto?.boolOrNull("enabled") ?: false,
                maxDurationSec = perfetto?.intOrNull("maxDurationSec") ?: 30,
            ),
            capabilityReport = obj.boolOrNull("capabilityReport") ?: false,
        )
    }

    private fun encodeDiag(diag: DiagConfig): JsonObject = JsonObject().apply {
        addProperty("enabled", diag.enabled)
        add("overlay", JsonObject().apply {
            addProperty("frameProfiler",    diag.overlay.frameProfiler)
            addProperty("drawCallCount",    diag.overlay.drawCallCount)
            addProperty("shaderRecompiles", diag.overlay.shaderRecompiles)
            addProperty("backendTier",      diag.overlay.backendTier)
            addProperty("cpuGpuLoad",       diag.overlay.cpuGpuLoad)
        })
        add("logging", JsonObject().apply {
            addProperty("backendSelection", diag.logging.backendSelection)
            addProperty("shaderRecompiles", diag.logging.shaderRecompiles)
            addProperty("drawCallCount",    diag.logging.drawCallCount)
            addProperty("glTrace",          diag.logging.glTrace)
            addProperty("level",            diag.logging.level)
        })
        addProperty("capabilityReport", diag.capabilityReport)
        add("perfetto", JsonObject().apply {
            addProperty("enabled",        diag.perfetto.enabled)
            addProperty("maxDurationSec", diag.perfetto.maxDurationSec)
        })
    }

    // ── Utilitários ───────────────────────────────────────────────────────
    fun foreignKeysOf(root: JsonObject): JsonObject =
        root.deepCopy().apply { KNOWN_KEYS.forEach { remove(it) } }

    private val Boolean.wire: Int get() = if (this) 1 else 0

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
            if (primitive.isNumber) primitive.asFloat
            else primitive.asString.trim().toFloat()
        }.getOrNull()
    }

    private fun JsonObject.boolOrNull(key: String): Boolean? = intOrNull(key)?.let { it > 0 }

    private fun JsonObject.stringOrNull(key: String): String? {
        val element = get(key) ?: return null
        if (!element.isJsonPrimitive) return null
        return runCatching { element.asString }.getOrNull()
    }
}
