package com.fcl.plugin.mobileglues.settings

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser

object MGConfigCodec {
    private val gson = Gson()

    // ── Encode ────────────────────────────────────────────────────────
    fun encode(config: MGConfig): String {
        val root = JsonObject()

        root.add("_meta", obj(
            "profile" to "balanced-baseline",
            "version" to "3.0",
            "schema"  to "mobileglues.config.v3",
        ))

        root.add("_layer1_core", obj(
            "gl_error_interception"     to true,
            "gl_state_shadowing"        to true,
            "log_infrastructure"        to true,
            "glsl_to_es_translation"    to true,
            "egl_gles_loader"           to true,
            "legacy_multidraw_fallback" to true,
        ))

        root.addProperty("enableNoError",               config.noError.wire)
        root.addProperty("forceGlGetErrorSkip",         if (config.forceGlGetErrorSkip) 1 else 0)
        root.addProperty("enableANGLE",                 config.angle.wire)
        root.addProperty("angleDepthClearFixMode",      config.depthClearFix.wire)
        root.addProperty("multidrawEngine",             config.multidrawEngine.key)
        root.add("multidrawGlobalOrder", JsonArray().apply {
            config.multidrawGlobalOrder.forEach { add(it) }
        })
        root.addProperty("fsr1Setting",                 config.fsr1.wire)
        root.addProperty("fsr1Sharpness",               config.fsr1Sharpness)
        root.addProperty("customGLVersion",             config.glVersion.wire)
        root.addProperty("enableExtTimerQuery",         if (config.extTimerQuery) 1 else 0)
        root.addProperty("enableExtComputeShader",      if (config.extComputeShader) 1 else 0)
        root.addProperty("enableExtDirectStateAccess",  if (config.extDirectStateAccess) 1 else 0)
        root.addProperty("enableExtGL43",               if (config.extGL43) 1 else 0)
        root.addProperty("maxGlslCacheSize",            config.glslCacheSize.wire)
        root.addProperty("forceDepthPrecisionFix",      if (config.forceDepthPrecisionFix) 1 else 0)
        root.addProperty("bufferUploadMode",            config.bufferUploadMode)
        root.addProperty("textureSwizzleMode",          config.textureSwizzleMode)
        root.addProperty("maxAnisotropyOverride",       config.maxAnisotropyOverride)
        root.addProperty("hideMGEnvLevel",              config.hideMGEnvLevel.wire)

        root.add("diag", JsonObject().apply {
            addProperty("enabled", config.diag.enabled)
            add("overlay", JsonObject().apply {
                addProperty("frameProfiler",    config.diag.overlay.frameProfiler)
                addProperty("drawCallCount",    config.diag.overlay.drawCallCount)
                addProperty("shaderRecompiles", config.diag.overlay.shaderRecompiles)
                addProperty("backendTier",      config.diag.overlay.backendTier)
                addProperty("cpuGpuLoad",       config.diag.overlay.cpuGpuLoad)
            })
            add("logging", JsonObject().apply {
                addProperty("backendSelection", config.diag.logging.backendSelection)
                addProperty("shaderRecompiles", config.diag.logging.shaderRecompiles)
                addProperty("drawCallCount",    config.diag.logging.drawCallCount)
                addProperty("glTrace",          config.diag.logging.glTrace)
                addProperty("level",            config.diag.logging.level)
            })
            addProperty("capabilityReport", config.diag.capabilityReport)
            add("perfetto", JsonObject().apply {
                addProperty("enabled",        config.diag.perfetto.enabled)
                addProperty("maxDurationSec", config.diag.perfetto.maxDurationSec)
                add("categories", JsonArray().apply {
                    config.diag.perfetto.categories.forEach { add(it) }
                })
            })
        })

        return gson.toJson(root)
    }

    // ── Decode ────────────────────────────────────────────────────────
    fun decode(json: String): MGConfig {
        val root = JsonParser.parseString(json).asJsonObject
        return MGConfig(
            angle = AngleConfig.entries.fromWire(
                root.intOr("enableANGLE", 1), AngleConfig.EnableIfPossible),
            noError = NoErrorConfig.entries.fromWire(
                root.intOr("enableNoError", 0), NoErrorConfig.Auto),
            depthClearFix = AngleDepthClearFixMode.entries.fromWire(
                root.intOr("angleDepthClearFixMode", 0), AngleDepthClearFixMode.Disabled),
            glVersion = GlVersion.fromWire(root.intOr("customGLVersion", 0)),
            fsr1 = Fsr1Preset.entries.fromWire(
                root.intOr("fsr1Setting", 0), Fsr1Preset.Disabled),
            fsr1Sharpness = root.get("fsr1Sharpness")?.asFloat ?: 0.75f,
            glslCacheSize = GlslCacheSize.fromWire(root.intOr("maxGlslCacheSize", 32)),
            hideMGEnvLevel = HideMGEnvLevel.entries.fromWire(
                root.intOr("hideMGEnvLevel", 0), HideMGEnvLevel.Disabled),
            multidrawEngine = MultidrawEngine.entries.firstOrNull {
                it.key.equals(root.str("multidrawEngine"), ignoreCase = true)
            } ?: MultidrawEngine.Legacy,
            multidrawGlobalOrder = root.getAsJsonArray("multidrawGlobalOrder")
                ?.mapNotNull { it.asString }
                ?: MultidrawOrderItem.DefaultOrder.map { it.key },
            extTimerQuery        = root.intOr("enableExtTimerQuery", 0) > 0,
            extComputeShader     = root.intOr("enableExtComputeShader", 0) > 0,
            extDirectStateAccess = root.intOr("enableExtDirectStateAccess", 0) > 0,
            extGL43              = root.intOr("enableExtGL43", 0) > 0,
            bufferUploadMode       = root.intOr("bufferUploadMode", 0),
            textureSwizzleMode     = root.intOr("textureSwizzleMode", 0),
            maxAnisotropyOverride  = root.intOr("maxAnisotropyOverride", 0),
            forceGlGetErrorSkip    = root.intOr("forceGlGetErrorSkip", 1) > 0,
            forceDepthPrecisionFix = root.intOr("forceDepthPrecisionFix", 0) > 0,
            diag = decodeDiag(root.getAsJsonObject("diag")),
        )
    }

    private fun decodeDiag(obj: JsonObject?): DiagConfig {
        if (obj == null) return DiagConfig()
        val ov = obj.getAsJsonObject("overlay")
        val lg = obj.getAsJsonObject("logging")
        val pf = obj.getAsJsonObject("perfetto")
        return DiagConfig(
            enabled = obj.boolOr("enabled", false),
            overlay = DiagOverlay(
                frameProfiler    = ov?.boolOr("frameProfiler", false) ?: false,
                drawCallCount    = ov?.boolOr("drawCallCount", false) ?: false,
                shaderRecompiles = ov?.boolOr("shaderRecompiles", false) ?: false,
                backendTier      = ov?.boolOr("backendTier", false) ?: false,
                cpuGpuLoad       = ov?.boolOr("cpuGpuLoad", false) ?: false,
            ),
            logging = DiagLogging(
                backendSelection = lg?.boolOr("backendSelection", false) ?: false,
                shaderRecompiles = lg?.boolOr("shaderRecompiles", false) ?: false,
                drawCallCount    = lg?.boolOr("drawCallCount", false) ?: false,
                glTrace          = lg?.boolOr("glTrace", false) ?: false,
                level            = lg?.str("level") ?: "info",
            ),
            capabilityReport = obj.boolOr("capabilityReport", false),
            perfetto = DiagPerfetto(
                enabled        = pf?.boolOr("enabled", false) ?: false,
                maxDurationSec = pf?.intOr("maxDurationSec", 30) ?: 30,
                categories     = pf?.getAsJsonArray("categories")?.mapNotNull { it.asString }
                    ?: listOf("gfx", "sched", "freq"),
            ),
        )
    }

    // ── Helpers ───────────────────────────────────────────────────────
    private fun JsonObject.intOr(k: String, fb: Int): Int =
        get(k)?.takeIf { it.isJsonPrimitive }?.asInt ?: fb

    private fun JsonObject.boolOr(k: String, fb: Boolean): Boolean =
        get(k)?.takeIf { it.isJsonPrimitive }?.asBoolean ?: fb

    private fun JsonObject.str(k: String): String? =
        get(k)?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }?.asString

    private fun obj(vararg pairs: Pair<String, Any?>): JsonObject =
        JsonObject().apply {
            pairs.forEach { (k, v) ->
                when (v) {
                    null           -> add(k, com.google.gson.JsonNull.INSTANCE)
                    is Boolean     -> addProperty(k, v)
                    is Number      -> addProperty(k, v)
                    is String      -> addProperty(k, v)
                    is JsonElement -> add(k, v)
                }
            }
        }
}
