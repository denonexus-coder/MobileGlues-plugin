package com.fcl.plugin.mobileglues.settings

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * `MG/config.json` 是和 native 端共享的格式，这些用例锁住的是那份契约。
 *
 * The reader is `config_get_int(name)` → `cJSON_GetObjectItem(config_json,
 * name)` — top level only, no dotted paths, no nested lookup. So every key
 * assertion below reads from the root: a key the renderer cannot reach is not
 * a setting, whatever it is called or where it is nested.
 */
class MGConfigCodecTest {

    private fun parse(json: String): JsonObject = JsonParser.parseString(json).asJsonObject

    private fun encode(config: MGConfig, foreign: JsonObject? = null): JsonObject =
        MGConfigCodec.encode(config, foreign)

    // ═══════════════════════════════════════════════════════════════════
    //  wires
    // ═══════════════════════════════════════════════════════════════════

    @Test
    fun `wire values match the native settings header`() {
        // AngleConfig, config/settings.h
        assertEquals(0, AngleConfig.DisableIfPossible.wire)
        assertEquals(1, AngleConfig.EnableIfPossible.wire)
        assertEquals(2, AngleConfig.ForceDisable.wire)
        assertEquals(3, AngleConfig.ForceEnable.wire)

        // NoErrorConfig: Auto, Disable, Level1, Level2
        assertEquals(0, NoErrorConfig.Auto.wire)
        assertEquals(1, NoErrorConfig.Disable.wire)
        assertEquals(2, NoErrorConfig.Level1.wire)
        assertEquals(3, NoErrorConfig.Level2.wire)

        // AngleDepthClearFixMode: Disabled, Mode1, Mode2 (MaxValue is a sentinel)
        assertEquals(0, DepthClearFixMode.Disabled.wire)
        assertEquals(2, DepthClearFixMode.entries.last().wire)

        // HideMGEnvLevel: Disabled, Level1
        assertEquals(0, HideMGEnvLevel.Disabled.wire)
        assertEquals(1, HideMGEnvLevel.entries.last().wire)

        // FSR1_Quality_Preset: Disabled, UltraQuality, Quality, Balanced, Performance
        assertEquals(0, Fsr1Preset.Disabled.wire)
        assertEquals(4, Fsr1Preset.entries.last().wire)

        assertEquals(46, GlVersion.Gl46.wire)
        assertEquals(0, GlVersion.Default.wire)

        // settings.cpp: `maxShaderCacheSize * 1024 * 1024` — the wire is MiB.
        assertEquals(32, GlslCacheSize.Default.wire)
        assertEquals(0, GlslCacheSize.Disabled.wire)
    }

    @Test
    fun `spinner position equals ordinal for every option`() {
        // 适配器是按 entries 的顺序构建的，render() 依赖 ordinal 就是位置。
        listOf(
            AngleConfig.entries,
            NoErrorConfig.entries,
            DepthClearFixMode.entries,
            HideMGEnvLevel.entries,
            GlVersion.entries,
            Fsr1Preset.entries,
        ).forEach { entries ->
            entries.forEachIndexed { index, option -> assertEquals(index, (option as Enum<*>).ordinal) }
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  decode
    // ═══════════════════════════════════════════════════════════════════

    @Test
    fun `an empty object decodes to the defaults`() {
        assertEquals(MGConfig.Default, MGConfigCodec.decode(parse("{}")))
    }

    @Test
    fun `every field survives a round trip`() {
        val config = MGConfig(
            angle = AngleConfig.ForceDisable,
            glVersion = GlVersion.Gl33,
            hideMGEnvLevel = HideMGEnvLevel.Level1,
            noError = NoErrorConfig.Level2,
            depthClearFix = DepthClearFixMode.Mode1,
            glslCache = GlslCacheSize.Disabled,
            useProgramBinaryCache = true,
            extComputeShader = true,
            extTimerQuery = false,
            extDirectStateAccess = true,
            fsr1Setting = Fsr1Preset.Balanced,
        )

        val json = Gson().toJson(encode(config))
        assertEquals(config, MGConfigCodec.decode(parse(json)))
    }

    @Test
    fun `out of range values fall back to the defaults`() {
        val decoded = MGConfigCodec.decode(
            parse("""{"enableANGLE":99,"enableNoError":-4,"fsr1Setting":77}""")
        )

        assertEquals(MGConfig.Default.angle, decoded.angle)
        assertEquals(MGConfig.Default.noError, decoded.noError)
        assertEquals(MGConfig.Default.fsr1Setting, decoded.fsr1Setting)
    }

    @Test
    fun `an unlisted customGLVersion is clamped exactly like native does`() {
        // settings.cpp: <0 -> 0, >46 -> 46, 34..39 -> 33, 1..31 -> 32,
        // 0 -> DEFAULT_GL_VERSION. The <0 step runs first, which is why -3 and
        // 0 both resolve to Default here and to 40 there: the plugin writes 0
        // back and settings.cpp does the promoting, once, on the C++ side.
        assertEquals(GlVersion.Gl46, GlVersion.fromWire(47))
        assertEquals(GlVersion.Gl46, GlVersion.fromWire(99))
        assertEquals(GlVersion.Gl33, GlVersion.fromWire(38))
        assertEquals(GlVersion.Gl33, GlVersion.fromWire(34))
        assertEquals(GlVersion.Gl33, GlVersion.fromWire(39))
        assertEquals(GlVersion.Gl32, GlVersion.fromWire(31))
        assertEquals(GlVersion.Gl32, GlVersion.fromWire(1))
        assertEquals(GlVersion.Default, GlVersion.fromWire(0))
        assertEquals(GlVersion.Default, GlVersion.fromWire(-3))
        assertEquals(GlVersion.Default, GlVersion.fromWire(null))

        // 已知档位必须原样通过。
        GlVersion.entries.forEach { assertEquals(it, GlVersion.fromWire(it.wire)) }

        // 读进来再写回去，落到磁盘上的必须是 native 会夹到的那一档，不能是 0。
        assertEquals(
            33,
            encode(MGConfigCodec.decode(parse("""{"customGLVersion":38}""")))
                .get("customGLVersion").asInt,
        )
    }

    @Test
    fun `one broken field does not discard the rest of the config`() {
        // 以前 applyFromJson 里任何一个 asInt 抛异常都会让整份配置作废并被默认值覆盖。
        val decoded = MGConfigCodec.decode(
            parse(
                """{"enableANGLE":{"nope":true},"customGLVersion":"not-a-number",""" +
                    """"maxShaderCacheSize":128}"""
            )
        )

        assertEquals(MGConfig.Default.angle, decoded.angle)
        assertEquals(MGConfig.Default.glVersion, decoded.glVersion)
        assertEquals(GlslCacheSize.Limited(128), decoded.glslCache)
    }

    @Test
    fun `numbers written as strings are accepted and normalised`() {
        val decoded = MGConfigCodec.decode(parse("""{"maxShaderCacheSize":"128"}"""))
        assertEquals(GlslCacheSize.Limited(128), decoded.glslCache)
        assertTrue(encode(decoded).get("maxShaderCacheSize").asJsonPrimitive.isNumber)
    }

    @Test
    fun `booleans follow the native greater-than-zero rule`() {
        val decoded = MGConfigCodec.decode(
            parse(
                """{"enableExtComputeShader":2,"enableExtTimerQuery":0,""" +
                    """"enableExtDirectStateAccess":-1,"useProgramBinaryCache":9}"""
            )
        )

        assertTrue(decoded.extComputeShader)
        assertEquals(false, decoded.extTimerQuery)
        assertEquals(false, decoded.extDirectStateAccess)
        assertTrue(decoded.useProgramBinaryCache)

        val encoded = encode(decoded)
        assertEquals(1, encoded.get("enableExtComputeShader").asInt)
        assertEquals(0, encoded.get("enableExtTimerQuery").asInt)
        assertEquals(1, encoded.get("useProgramBinaryCache").asInt)
    }

    @Test
    fun `every non-positive cache size means disabled, exactly like native reads it`() {
        // settings.cpp: `if (config_get_int("maxShaderCacheSize") > 0)` —— 否则
        // max_shader_cache_size = 0，即不缓存。
        assertEquals(GlslCacheSize.Disabled, GlslCacheSize.fromWire(-1))
        assertEquals(GlslCacheSize.Disabled, GlslCacheSize.fromWire(0))
        assertEquals(GlslCacheSize.Disabled, GlslCacheSize.fromWire(-7))
        assertEquals(GlslCacheSize.Limited(64), GlslCacheSize.fromWire(64))
        assertEquals(GlslCacheSize.Default, GlslCacheSize.fromWire(null))

        assertEquals(0, GlslCacheSize.Disabled.wire)
        assertEquals(
            0,
            encode(MGConfigCodec.decode(parse("""{"maxShaderCacheSize":0}""")))
                .get("maxShaderCacheSize").asInt,
        )

        assertThrows(IllegalArgumentException::class.java) { GlslCacheSize.Limited(0) }
    }

    @Test
    fun `the old cache key still sets the size and is scrubbed on save`() {
        // The bug this guards against: the plugin wrote `maxGlslCacheSize` while
        // settings.cpp reads `maxShaderCacheSize`, so the reader saw -1, took
        // `> 0` as false, and every conversion cache in the renderer was off.
        val root = parse("""{"maxGlslCacheSize":128}""")

        val config = MGConfigCodec.decode(root)
        assertEquals(GlslCacheSize.Limited(128), config.glslCache)

        val out = encode(config, MGConfigCodec.foreignKeysOf(root))
        assertEquals(128, out.get("maxShaderCacheSize").asInt)
        assertNull(out.get("maxGlslCacheSize"))
    }

    @Test
    fun `the nested layout this file used to carry is still read`() {
        val root = parse(
            """{"opengl_egl":{"enableANGLE":3},"errorHandling":{"enableNoError":2},""" +
                """"shaderCache":{"maxGlslCacheSize":64},"extensions":{"enableExtComputeShader":1}}"""
        )

        val config = MGConfigCodec.decode(root)
        assertEquals(AngleConfig.ForceEnable, config.angle)
        assertEquals(NoErrorConfig.Level1, config.noError)
        assertEquals(GlslCacheSize.Limited(64), config.glslCache)
        assertTrue(config.extComputeShader)

        // …and the flat key still wins over the nested one it shadows.
        assertEquals(
            AngleConfig.DisableIfPossible,
            MGConfigCodec.decode(
                parse("""{"enableANGLE":0,"opengl_egl":{"enableANGLE":3}}""")
            ).angle,
        )
    }

    @Test
    fun `slider mebibytes map to config values and back`() {
        assertEquals(GlslCacheSize.Disabled, GlslCacheSize.ofMebibytes(0))
        assertEquals(GlslCacheSize.Limited(1), GlslCacheSize.ofMebibytes(1))
        assertEquals(GlslCacheSize.Limited(512), GlslCacheSize.ofMebibytes(512))

        assertEquals(0, GlslCacheSize.Disabled.mebibytesOrZero)
        assertEquals(512, GlslCacheSize.Limited(512).mebibytesOrZero)
    }

    // ═══════════════════════════════════════════════════════════════════
    //  shape — flat, at the root, and nothing else
    // ═══════════════════════════════════════════════════════════════════

    @Test
    fun `the defaults are written flat, at the root, and nothing else`() {
        val encoded = encode(MGConfig.Default)

        assertEquals(1, encoded.get("enableANGLE").asInt)
        assertEquals(0, encoded.get("customGLVersion").asInt)
        assertEquals(0, encoded.get("hideMGEnvLevel").asInt)
        assertEquals(0, encoded.get("enableNoError").asInt)
        assertEquals(0, encoded.get("angleDepthClearFixMode").asInt)
        assertEquals(32, encoded.get("maxShaderCacheSize").asInt)
        assertEquals(0, encoded.get("useProgramBinaryCache").asInt)
        assertEquals(0, encoded.get("enableExtComputeShader").asInt)
        assertEquals(1, encoded.get("enableExtTimerQuery").asInt)
        assertEquals(0, encoded.get("enableExtDirectStateAccess").asInt)
        assertEquals(0, encoded.get("fsr1Setting").asInt)

        // Exactly the keys the renderer reads, all at the root. A nested key is
        // one config_get_int can never see, so an extra one here is a setting
        // that silently does nothing.
        assertEquals(11, encoded.size())
    }

    @Test
    fun `nothing nested survives a save`() {
        val root = parse(
            """{"meta":{"schema":3},"opengl_egl":{"enableANGLE":3},""" +
                """"shaderCache":{"maxGlslCacheSize":64},"multidrawOrder":{"_global":"unroll"},""" +
                """"diagnostics":{"enabled":true},"somethingFromTheFuture":7}"""
        )

        val out = encode(MGConfigCodec.decode(root), MGConfigCodec.foreignKeysOf(root))

        assertNull(out.get("meta"))
        assertNull(out.get("opengl_egl"))
        assertNull(out.get("shaderCache"))
        assertNull(out.get("multidrawOrder"))
        assertNull(out.get("diagnostics"))
        assertNull(out.get("maxGlslCacheSize"))

        // The value carried across; only its container changed.
        assertEquals(3, out.get("enableANGLE").asInt)
        assertEquals(64, out.get("maxShaderCacheSize").asInt)

        // A key of a future reader still survives a save untouched.
        assertEquals(7, out.get("somethingFromTheFuture").asInt)
    }

    @Test
    fun `keys the app does not know survive a save`() {
        val root = parse("""{"enableANGLE":3,"somethingFromTheFuture":7}""")

        val config = MGConfigCodec.decode(root)
        val encoded = encode(config, MGConfigCodec.foreignKeysOf(root))

        assertEquals(7, encoded.get("somethingFromTheFuture").asInt)
        assertEquals(3, encoded.get("enableANGLE").asInt)
    }

    @Test
    fun `foreign keys never include keys the app owns`() {
        val root = parse("""{"enableANGLE":3,"maxGlslCacheSize":64,"hideMGEnvLevel":1}""")
        val foreign = MGConfigCodec.foreignKeysOf(root)

        assertNull(foreign.get("enableANGLE"))
        assertNull(foreign.get("hideMGEnvLevel"))
        assertNull(foreign.get("maxGlslCacheSize"))
    }

    @Test
    fun `keys the lib never reads are scrubbed on save`() {
        // 控制一个 settings.cpp 不读的键，等于给用户一个什么都不做的开关。
        val root = parse(
            """{"forceDepthPrecisionFix":true,"disableComputeOnWeakGpu":false,""" +
                """"bufferUploadMode":2,"textureSwizzleMode":1,"maxAnisotropyOverride":8,""" +
                """"fsr1Version":2,"fsr1Sharpness":0.4,"multidrawEngine":"imdbi",""" +
                """"somethingFromTheFuture":7}"""
        )

        val out = encode(MGConfigCodec.decode(root), MGConfigCodec.foreignKeysOf(root))

        listOf(
            "forceDepthPrecisionFix", "disableComputeOnWeakGpu", "bufferUploadMode",
            "textureSwizzleMode", "maxAnisotropyOverride", "fsr1Version", "fsr1Sharpness",
            "multidrawEngine",
        ).forEach { assertNull(it, out.get(it)) }

        assertEquals(7, out.get("somethingFromTheFuture").asInt)
    }

    @Test
    fun `all three generations of legacy multidraw keys are dropped on save`() {
        val root = parse(
            """{"multidrawMode":5,"multidrawModeElements":"multibasevertex",""" +
                """"multidrawDisableBackends":"compute","multidrawOrder":"unroll","hideMGEnvLevel":1}"""
        )
        val encoded = encode(MGConfigCodec.decode(root), MGConfigCodec.foreignKeysOf(root))

        assertNull(encoded.get("multidrawMode"))
        assertNull(encoded.get("multidrawModeElements"))
        assertNull(encoded.get("multidrawDisableBackends"))
        assertNull(encoded.get("multidrawOrder"))
        assertEquals(1, encoded.get("hideMGEnvLevel").asInt)
    }

    // ═══════════════════════════════════════════════════════════════════
    //  MultiDraw bench vocabulary — reported, never written
    // ═══════════════════════════════════════════════════════════════════

    @Test
    fun `order names are parsed the way native parses them`() {
        // md_parse_order_list：忽略大小写以及空格 / 下划线 / 连字符
        assertEquals(MultidrawBackend.MultiIndirect, MultidrawBackend.parse("MultiIndirect"))
        assertEquals(MultidrawBackend.MultiIndirect, MultidrawBackend.parse("  multi_indirect "))
        assertEquals(MultidrawBackend.MultiArrays, MultidrawBackend.parse("multi-arrays"))
        assertNull(MultidrawBackend.parse("nonsense"))
        assertNull(MultidrawBackend.parse(""))
        assertNull(MultidrawBackend.parse(null))
    }

    @Test
    fun `backend keys are exactly the names native accepts`() {
        // 这些名字是和 native 的 k_md_backend_names / k_md_entries 共享的契约。
        assertEquals(
            listOf(
                "unroll", "basevertex", "indirect",
                "multiarrays", "multibasevertex", "multiindirect", "compute",
            ),
            MultidrawBackend.entries.map { it.key },
        )
        assertEquals(
            listOf(
                "glMultiDrawArrays",
                "glMultiDrawElements",
                "glMultiDrawElementsBaseVertex",
                "glMultiDrawArraysIndirect",
                "glMultiDrawElementsIndirect",
            ),
            MultidrawEntry.entries.map { it.glFunction },
        )
    }

    @Test
    fun `the native backend lands first per entry and unsupported ones drop`() {
        assertEquals(
            MultidrawBackend.MultiArrays,
            MultidrawEntry.Arrays.normalize(emptyList()).first(),
        )
        assertEquals(
            MultidrawBackend.MultiBaseVertex,
            MultidrawEntry.ElementsBaseVertex.normalize(emptyList()).first(),
        )
        // basevertex 对 glMultiDrawElements 不是独立实现。
        assertTrue(MultidrawBackend.BaseVertex !in MultidrawEntry.Elements.implemented)
        // normalize keeps only implemented backends, then puts them in native order.
        assertEquals(
            MultidrawEntry.Elements.implemented,
            MultidrawEntry.Elements.normalize(listOf(MultidrawBackend.BaseVertex)),
        )
    }
}
