package io.github.kevinah95.norman_the_necromancer.assets

import io.github.kevinah95.norman_the_necromancer.GameVersion
import io.github.kevinah95.norman_the_necromancer.audio.GameAudio
import io.github.kevinah95.norman_the_necromancer.audio.NormanMusicStream
import io.github.kevinah95.norman_the_necromancer.behaviours.Bleeding
import io.github.kevinah95.norman_the_necromancer.behaviours.Frozen
import io.github.kevinah95.norman_the_necromancer.core.*
import io.github.kevinah95.norman_the_necromancer.entities.*
import io.github.kevinah95.norman_the_necromancer.fx.Fx
import io.github.kevinah95.norman_the_necromancer.i18n.GameLanguage
import io.github.kevinah95.norman_the_necromancer.i18n.GameStrings
import io.github.kevinah95.norman_the_necromancer.renderer.GameRenderer
import io.github.kevinah95.norman_the_necromancer.shop.ShopManager
import korlibs.audio.sound.AudioSamples
import korlibs.io.async.suspendTest
import korlibs.io.file.std.MemoryVfsMix
import korlibs.io.serialization.json.Json
import korlibs.korge.tests.ViewsForTesting
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import normanthenecromancer.shared.generated.resources.Res

class AssetsAndMemoryVfsTest : ViewsForTesting() {

    private val allCatalogSprites = listOf(
        GameSprites.NORMAN_ARMS_DOWN,
        GameSprites.NORMAN_ARMS_UP,
        GameSprites.NORMAN_ICON,
        GameSprites.SKULL,
        GameSprites.SKELETON,
        GameSprites.BIG_SKELETON,
        GameSprites.VILLAGER_1,
        GameSprites.VILLAGER_2,
        GameSprites.VILLAGER_3,
        GameSprites.VILLAGER_4,
        GameSprites.ARCHER,
        GameSprites.MONK,
        GameSprites.CHAMPION,
        GameSprites.SHELL_KNIGHT_UP,
        GameSprites.SHELL_KNIGHT_DOWN,
        GameSprites.PIPER,
        GameSprites.RAT,
        GameSprites.RAGE_KNIGHT,
        GameSprites.RAGE_KNIGHT_ENRAGED,
        GameSprites.ROYAL_GUARD,
        GameSprites.ROYAL_GUARD_SHIELDED,
        GameSprites.WIZARD,
        GameSprites.PORTAL,
        GameSprites.THE_KING,
        GameSprites.THE_KING_ON_FOOT,
        GameSprites.P_GREEN_SKULL,
        GameSprites.P_RED_SKULL,
        GameSprites.P_SKULL_YELLOW,
        GameSprites.P_SKULL,
        GameSprites.YELLOW_ORB,
        GameSprites.YELLOW_ORB_EMPTY,
        GameSprites.HEALTH_ORB,
        GameSprites.HEALTH_ORB_EMPTY,
        GameSprites.HEALTH_PIP,
        GameSprites.CAST_ORB,
        GameSprites.CAST_ORB_EMPTY,
        GameSprites.PINK_FRAME,
        GameSprites.RETICLE,
        GameSprites.STATUS_SHIELDED,
        GameSprites.STATUS_BLEEDING,
        GameSprites.STATUS_ENRAGED,
        GameSprites.ICE,
        GameSprites.FLOOR,
        GameSprites.WALL,
        GameSprites.CEILING,
        GameSprites.DOOR,
        GameSprites.FONT,
        GameSprites.P_BONE_1,
        GameSprites.P_BONE_2,
        GameSprites.P_BONE_3,
        GameSprites.P_GREEN_1,
        GameSprites.P_GREEN_2,
        GameSprites.P_GREEN_3,
        GameSprites.P_GREEN_4,
        GameSprites.P_GREEN_5,
        GameSprites.P_RED_1,
        GameSprites.P_RED_2,
        GameSprites.P_RED_3,
        GameSprites.P_RED_4,
        GameSprites.P_PURPLE_1,
        GameSprites.P_PURPLE_2,
        GameSprites.P_PURPLE_3,
        GameSprites.P_PURPLE_4,
        GameSprites.P_PURPLE_5,
        GameSprites.P_STAR_1,
        GameSprites.P_STAR_2,
        GameSprites.P_STAR_3,
        GameSprites.P_STAR_4,
        GameSprites.P_LIGHTNING_1,
        GameSprites.P_LIGHTNING_2,
        GameSprites.P_LIGHTNING_3,
        GameSprites.P_LIGHTNING_4,
        GameSprites.P_LIGHTNING_5,
        GameSprites.P_LIGHTNING_6,
        GameSprites.P_BLUE_1,
        GameSprites.P_BLUE_2,
        GameSprites.P_BLUE_3,
        GameSprites.P_ICE_1,
        GameSprites.P_ICE_2,
        GameSprites.P_ICE_3,
        GameSprites.P_DUST_1,
        GameSprites.P_DUST_2,
        GameSprites.P_DUST_3
    )

    @BeforeTest
    fun setUp() {
        gameRandom = Random(seed = 42)
        Fx.activeEmitters.clear()
        TweenManager.reset()
    }

    @AfterTest
    fun tearDown() {
        gameRandom = Random.Default
        Fx.activeEmitters.clear()
        TweenManager.reset()
        GameStrings.language = GameLanguage.EN
    }

    @Test
    fun `smoke test de todos los recursos y catalogo completo de GameSprites`() = suspendTest {
        // Verify raw resource files exist and are non-empty
        val resourcePaths = listOf(
            "files/sprites.png",
            "files/sprites.json",
            "files/font.png",
            "files/font.json"
        )
        for (path in resourcePaths) {
            val bytes = Res.readBytes(path)
            assertTrue(bytes.isNotEmpty(), "Resource $path must exist and not be empty")
        }

        // Load GameAtlas and verify every sprite constant in GameSprites resolves to a valid slice
        GameAtlas.load()
        for (spriteName in allCatalogSprites) {
            val slice = GameAtlas.getSprite(spriteName)
            val rect = GameAtlas.spriteRects[spriteName]
            assertNotNull(slice, "Sprite '$spriteName' is missing from GameAtlas.sprites")
            assertNotNull(rect, "Sprite '$spriteName' is missing from GameAtlas.spriteRects")
            assertTrue(rect.w > 0 && rect.h > 0, "Sprite '$spriteName' must have positive dimensions")
        }

        // Verify font glyph widths & version metadata
        assertEquals(6, GameAtlas.getGlyphWidth('W'))
        assertEquals(2, GameAtlas.getGlyphWidth('!'))
        assertEquals(GameAtlas.GLYPH_WIDTH, GameAtlas.getGlyphWidth('A'))
        assertTrue(GameVersion.VERSION_NAME.isNotBlank())
        assertTrue(GameVersion.VERSION_CODE > 0)
    }

    @Test
    fun `MemoryVfs simula guardado y carga de partida y configuracion en memoria sin tocar disco`() = suspendTest {
        val vfs = MemoryVfsMix(
            "config/settings.json" to """{"language":"ES","audioEnabled":false}""",
            "assets/sprites.png" to Res.readBytes("files/sprites.png"),
            "assets/sprites.json" to Res.readBytes("files/sprites.json")
        )

        assertTrue(vfs["config/settings.json"].exists())
        assertTrue(vfs["assets/sprites.png"].exists())
        assertTrue(vfs["assets/sprites.json"].exists())

        // Read config from MemoryVfs
        val settingsMap = Json.parse(vfs["config/settings.json"].readString()) as Map<*, *>
        assertEquals("ES", settingsMap["language"])
        assertEquals(false, settingsMap["audioEnabled"])

        // Save a game state snapshot to MemoryVfs and read it back
        val savePayload = """{"level":4,"souls":250,"streak":7,"rituals":["Bouncing","Doubleshot"]}"""
        vfs["saves"].mkdir()
        vfs["saves/slot1.json"].writeString(savePayload)
        assertTrue(vfs["saves/slot1.json"].exists())

        val loadedSave = Json.parse(vfs["saves/slot1.json"].readString()) as Map<*, *>
        assertEquals(4, (loadedSave["level"] as Number).toInt())
        assertEquals(250, (loadedSave["souls"] as Number).toInt())
        assertEquals(7, (loadedSave["streak"] as Number).toInt())
    }

    @Test
    fun `sistema de particulas Fx y fisicas de ParticleEmitter`() {
        val area = Rect2D(10.0, 10.0, 20.0, 20.0)
        val emitters = listOf(
            Fx.bones(15.0, 15.0).burst(4),
            Fx.trail().burst(3),
            Fx.royalty().burst(3),
            Fx.dust(400.0, 200.0).burst(5),
            Fx.resurrect(area).burst(4),
            Fx.blood(area).burst(4),
            Fx.holy(area).burst(4),
            Fx.portal(area).burst(4)
        )
        assertEquals(8, Fx.activeEmitters.size)
        assertTrue(emitters.all { it.particles.isNotEmpty() })

        // Update particles so they fall to y <= 0, bounce, and eventually expire
        Fx.bones(15.0, 1.0).apply {
            particles.add(
                io.github.kevinah95.norman_the_necromancer.fx.Particle(
                    x = 10.0,
                    y = 1.0,
                    vx = 5.0,
                    vy = -20.0,
                    bounce = 0.5,
                    elapsed = 0.0,
                    duration = 5000.0,
                    mass = 50.0,
                    friction = 0.2
                )
            )
        }
        Fx.update(100.0)
        for (e in Fx.activeEmitters) {
            e.remove()
        }
        Fx.update(20_000.0)
        assertTrue(Fx.activeEmitters.isEmpty(), "Done emitters with expired particles must be cleaned up")
    }

    @Test
    fun `motor de audio procedural cambia mezcla dinamica segun nivel tienda y jefe`() = suspendTest {
        val stream = NormanMusicStream(8000) // Use 8kHz sample rate for ultra-fast unit test synthesis
        val samples = AudioSamples(2, 256)
        assertEquals(256, stream.read(samples, 0, 256))
        assertEquals(440.0, GameAudio.freq(0), 1e-4)

        // Inject stream into GameAudio and verify adaptive synth transitions across levels, shop, boss, and outro
        GameAudio.musicStream = stream
        GameAudio.useLevelSynths(0)
        assertEquals(0.45f, stream.targetOrganVol)
        assertEquals(0.0f, stream.targetBassVol)
        assertEquals(0.0f, stream.targetKickVol)

        GameAudio.useLevelSynths(1)
        assertEquals(0.5f, stream.targetBassVol)
        assertEquals(0.0f, stream.targetKickVol)

        GameAudio.useLevelSynths(2)
        assertEquals(0.8f, stream.targetKickVol)

        GameAudio.useShopSynths()
        assertEquals(0.0f, stream.targetKickVol)

        GameAudio.useLevelSynths(9)
        assertEquals(0.55f, stream.targetKingVol)
        assertEquals(0.0f, stream.targetOrganVol)

        GameAudio.onKingPhase4()
        assertEquals(0.8f, stream.targetKickVol)

        GameAudio.useOutroSynths()
        assertEquals(0.45f, stream.targetOrganVol)
        assertEquals(0.0f, stream.targetKingVol)
        assertEquals(0.0f, stream.targetKickVol)

        GameAudio.playCast()
        GameAudio.playResurrect()
        GameAudio.playBuy()
        GameAudio.playHit()
        assertEquals(false, stream.finished)
        stream.seek(kotlin.time.Duration.ZERO)
        assertEquals(stream, stream.clone())
        assertTrue(stream.currentKickVol >= 0.0f)
        assertTrue(stream.currentOrganVol >= 0.0f)
        assertTrue(stream.currentBassVol >= 0.0f)
        assertTrue(stream.currentKingVol >= 0.0f)
        assertTrue(stream.masterGain >= 0.0f)
        stream.currentKickVol = 0.1f
        stream.currentOrganVol = 0.1f
        stream.currentBassVol = 0.1f
        stream.currentKingVol = 0.1f
        stream.masterGain = 0.2f
        stream.targetMasterGain = 0.5f

        assertTrue(GameStrings.introSkipHint.isNotBlank())
        assertTrue(GameStrings.shopNeedSouls.isNotBlank())
        GameStrings.language = GameLanguage.ES
        assertTrue(GameStrings.introSkipHint.isNotBlank())
        assertTrue(GameStrings.shopNeedSouls.isNotBlank())
        GameStrings.language = GameLanguage.EN
        GameAudio.stop()

        // Exercise GameAudio.play() coroutine launch when enabled = true
        GameAudio.enabled = true
        GameAudio.play()
        kotlinx.coroutines.delay(50)
        GameAudio.stop()
    }

    @Test
    fun `renderizado headless completo de WorldRenderer UiRenderer y GraphicsRenderer`() = viewsTest {
        GameAtlas.load()
        val renderer = GameRenderer()
        assertEquals(GameLayout.SCENE_ORIGIN_Y, renderer.sceneOriginY)
        renderer.sceneOriginY = 150.0

        val player = createPlayer().apply { hp = 3 }
        val game = Game().apply {
            this.player = player
            spawn(player, 10.0, 0.0)
            souls = 120.0
            streak = 5
        }

        // Spawn entities with different HP ranges (<10 orbs, >=10 text), Frozen overlay, and status icons
        val bandit = createBandit().apply { hp = 1 }
        bandit.addBehaviour(Frozen(bandit), 0)
        bandit.addBehaviour(Bleeding(bandit))
        val champion = createChampion().apply { hp = 8 } // maxHp = 10 -> renders "8/10" text
        game.spawn(bandit, 80.0, 0.0)
        game.spawn(champion, 140.0, 0.0)
        Fx.bones(50.0, 10.0).burst(3)
        TweenManager.screenshake(50.0)

        testRenderContext(ag) { ctx ->
            ctx.useBatcher { batch ->
                // 1. Render INTRO state (EN & ES)
                game.state = GameState.INTRO
                game.dialogue.addAll(GameStrings.getIntroDialogue())
                renderer.render(batch, ctx, game, isPaused = false, touchAimActive = false)
                GameStrings.language = GameLanguage.ES
                renderer.render(batch, ctx, game, isPaused = false, touchAimActive = false)

                // 2. Render PLAYING state (with reticle, HUD, cooldown bar, frozen & status overlays, paused modal)
                game.state = GameState.PLAYING
                game.ability.timer = 5_000.0
                renderer.render(batch, ctx, game, isPaused = false, touchAimActive = true)
                renderer.render(batch, ctx, game, isPaused = true, touchAimActive = false)

                // 3. Render SHOPPING state
                ShopManager.init(game)
                ShopManager.enterShop()
                renderer.render(batch, ctx, game, isPaused = false, touchAimActive = false)

                // 4. Render WIN state
                game.state = GameState.WIN
                game.dialogue.clear()
                game.dialogue.addAll(GameStrings.getOutroDialogue())
                renderer.render(batch, ctx, game, isPaused = false, touchAimActive = false)

                // Exercise delegation helpers on GameRenderer and default args on GraphicsRenderer
                renderer.write(batch, ctx, "Test\nMultiline", 10.0, 10.0)
                renderer.writeCentered(batch, ctx, "Centered\nText", 50.0)
                renderer.graphics.writeCentered(batch, ctx, "Centered Default Spacing", 50.0)
                renderer.writeCenteredHorizontally(batch, ctx, "Horizontal", 30.0)
                renderer.drawSprite(batch, ctx, GameSprites.SKULL, 5.0, 5.0)
                renderer.drawSceneSprite(batch, ctx, GameSprites.SKULL, 5.0, 0.0)
                renderer.graphics.drawSceneSprite(batch, ctx, GameSprites.SKULL, 5.0, 0.0)
                renderer.graphics.drawOrbs(batch, ctx, 50.0, 0.0, 1, 2, GameSprites.HEALTH_ORB, GameSprites.HEALTH_ORB_EMPTY)
                renderer.drawNineSlice(batch, ctx, GameSprites.PINK_FRAME, 10.0, 10.0, 40.0, 20.0)
                renderer.drawButton(batch, ctx, GameLayout.PAUSE_BUTTON, "BTN")
            }
        }
    }
}
