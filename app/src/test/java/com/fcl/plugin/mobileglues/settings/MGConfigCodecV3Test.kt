package com.fcl.plugin.mobileglues.settings

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MGConfigCodecV3Test {

    private fun parse(json: String): JsonObject =
        JsonParser.parseString(json).asJsonObject

    private fun roundTrip(config: MGConfig): MGConfig {
        val encoded = MGConfigCodec.encode(config, foreignKeys = null)
        return MGConfigCodec.decode(encoded)
    }

    // ── defaults ────────────────────────────────────────────────────────

    @Test fun `empty object decodes to defaults`() {
        assertEquals(MGConfig.Default, MGConfigCodec.decode(JsonObject()))
    }

    @Test fun `all top-level sections are present in a fresh encode`() {
        val out = MGConfigCodec.encode(MGConfig.Default, foreignKeys = null)
        val expected = listOf(
            "meta", "opengl_egl", "errorHandling", "gpuOptimization",
            "shaderCache", "textureBuffer", "multidrawEngine", "multidrawOrder",
            "extensions", "upscaling", "diagnostics",
        )
        expected.forEach { assertTrue("missing $it", out.has(it)) }
    }

    @Test fun `meta carries schema version`() {
        val out = MGConfigCodec.encode(MGConfig.Default, null)
        val meta = out.getAsJsonObject("meta")
        assertEquals("3.0", meta.get("version").asString)
        assertEquals("mobileglues-v3", meta.get("schema").asString)
    }

    // ── round-trip: every field, no exceptions ──────────────────────────

    @Test fun `full round-trip preserves every field`() {
        val config = MGConfig(
            angle = AngleConfig.ForceDisable,
            glVersion = GlVersion.Gl43,
            hideMGEnvLevel = HideMGEnvLevel.Level1,

            noError = NoErrorConfig.Full,
            forceGlGetErrorSkip = false,
            forceDepthPrecisionFix = true,
            depthClearFix = DepthClearFixMode.Mode2,

            disableComputeOnWeakGpu = false,
            enableExtGL43 = true,

            glslCache = GlslCacheSize.Limited(128),
            useProgramBinaryCache = true,

            bufferUploadMode = BufferUploadMode.Ring,
            textureSwizzleMode = TextureSwizzleMode.Bgra,
            maxAnisotropyOverride = MaxAnisotropyOverride.X8,

            multidrawEngine = MultidrawEngine.Imdbi,
            vmdiEnableAutotune = false,
            vmdiBackendTier = VmdiBackendTier.MultiBaseVertex,
            imdbiBackend = ImdbiBackend.ComputeDispatch,
            imdbiUnrollFactor = 16,
            imdbiPersistentMapping = false,
            imdbiRegisterPinning = false,
            imdbiPrimitiveRestart = false,
            imdbiRingSize = 16 * 1024 * 1024,

            multidraw = MultidrawSettings.Default
                .withGlobalOrder(
                    listOf(
                        MultidrawOrderItem.Native,
                        MultidrawOrderItem.Unroll,
                        MultidrawOrderItem.MultiIndirect,
                    ),
                )
                .withExceptionOrder(
                    MultidrawEntry.Arrays,
                    listOf(MultidrawBackend.Unroll, MultidrawBackend.MultiArrays),
                ),

            extComputeShader = true,
            extTimerQuery = false,
            extDirectStateAccess = true,

            fsrEnableSharpening = false,
            fsr1Version = 1,
            fsr1Sharpness = 0.75f,
            fsr2Sharpness = 0.25f,

            diag = DiagConfig(
                enabled = true,
                overlay = DiagOverlay(
                    frameProfiler = true,
                    drawCallCount = true,
                    shaderRecompiles = false,
                    backendTier = true,
                    cpuGpuLoad = false,
                ),
                logging = DiagLogging(
                    backendSelection = true,
                    shaderRecompiles = true,
                    drawCallCount = false,
                    glTrace = true,
                    level = DiagLogLevel.Trace,
                ),
                perfetto = DiagPerfetto(enabled = true, maxDurationSec = 60),
                capabilityReport = true,
            ),
        )
        assertEquals(config, roundTrip(config))
    }

    // ── wrong types / missing / malformed ───────────────────────────────

    @Test fun `wrong-typed int falls back to default`() {
        val json = parse("""{"opengl_egl":{"enableANGLE":"not-a-number"}}""")
        assertEquals(AngleConfig.EnableIfPossible, MGConfigCodec.decode(json).angle)
    }

    @Test fun `missing file semantics - empty root`() {
        assertEquals(MGConfig.Default, MGConfigCodec.decode(JsonObject()))
    }

    @Test fun `legacy flat key is read when nested key absent`() {
        val flat = parse("""{"enableANGLE":2}""")
        assertEquals(AngleConfig.ForceDisable, MGConfigCodec.decode(flat).angle)
    }

    @Test fun `nested key wins over legacy flat key`() {
        val both = parse("""{"enableANGLE":2,"opengl_egl":{"enableANGLE":3}}""")
        assertEquals(AngleConfig.ForceEnable, MGConfigCodec.decode(both).angle)
    }

    @Test fun `legacy diag object migrates to diagnostics`() {
        val legacy = parse("""{"diag":{"enabled":true}}""")
        assertTrue(MGConfigCodec.decode(legacy).diag.enabled)
    }

    @Test fun `diagnostics wins over legacy diag`() {
        val both = parse(
            """{"diag":{"enabled":false},"diagnostics":{"enabled":true}}""",
        )
        assertTrue(MGConfigCodec.decode(both).diag.enabled)
    }

    // ── forward compatibility ───────────────────────────────────────────

    @Test fun `unknown top-level key survives a save`() {
        val original = parse("""{"futureKey":"x"}""")
        val foreign = MGConfigCodec.foreignKeysOf(original)
        val saved = MGConfigCodec.encode(MGConfig.Default, foreign)
        assertEquals("x", saved.get("futureKey").asString)
    }

    @Test fun `unknown key inside a known section survives a save`() {
        val original = parse("""{"opengl_egl":{"enableANGLE":2,"futureSub":"y"}}""")
        val foreign = MGConfigCodec.foreignKeysOf(original)
        val saved = MGConfigCodec.encode(MGConfig.Default, foreign)
        assertEquals(
            "y",
            saved.getAsJsonObject("opengl_egl").get("futureSub").asString,
        )
    }

    @Test fun `legacy flat keys do not survive a save`() {
        val original = parse("""{"enableANGLE":2,"customGLVersion":"4.0"}""")
        val foreign = MGConfigCodec.foreignKeysOf(original)
        val saved = MGConfigCodec.encode(MGConfig.Default, foreign)
        assertFalse(saved.has("enableANGLE"))
        assertFalse(saved.has("customGLVersion"))
    }

    // ── multidrawOrder round-trip ───────────────────────────────────────

    @Test fun `multidraw global order survives round-trip`() {
        val config = MGConfig.Default.copy(
            multidraw = MultidrawSettings.Default
                .withGlobalOrder(listOf(MultidrawOrderItem.Unroll, MultidrawOrderItem.Native)),
        )
        assertEquals(config.multidraw, roundTrip(config).multidraw)
    }

    @Test fun `multidraw per-entry exception survives round-trip`() {
        val config = MGConfig.Default.copy(
            multidraw = MultidrawSettings.Default
                .withExceptionOrder(
                    MultidrawEntry.ElementsBaseVertex,
                    listOf(MultidrawBackend.Compute, MultidrawBackend.MultiIndirect),
                ),
        )
        assertEquals(config.multidraw, roundTrip(config).multidraw)
    }

    @Test fun `default per-entry order is not written as an exception`() {
        val out = MGConfigCodec.encode(MGConfig.Default, null)
        assertFalse(out.getAsJsonObject("multidrawOrder").has("_global"))
        // The section still carries all five per-entry keys (the C++ needs them).
        MultidrawEntry.entries.forEach {
            assertTrue(out.getAsJsonObject("multidrawOrder").has(it.orderKey))
        }
    }

    @Test fun `AngleConfig default matches C++ EnableIfPossible`() {
        assertEquals(1, AngleConfig.EnableIfPossible.wire)
    }

    @Test fun `GlslCacheSize disabled is wire zero`() {
        assertEquals(0, GlslCacheSize.Disabled.wire)
        val encoded = MGConfigCodec.encode(
            MGConfig.Default.copy(glslCache = GlslCacheSize.Disabled),
            null,
        )
        assertEquals(0, encoded.getAsJsonObject("shaderCache").get("maxGlslCacheSize").asInt)
    }
}
