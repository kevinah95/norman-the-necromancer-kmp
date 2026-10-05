package io.github.kevinah95.norman_the_necromancer.scene

import io.github.kevinah95.norman_the_necromancer.assets.GameAtlas
import io.github.kevinah95.norman_the_necromancer.audio.GameAudio
import io.github.kevinah95.norman_the_necromancer.behaviours.March
import io.github.kevinah95.norman_the_necromancer.core.*
import io.github.kevinah95.norman_the_necromancer.entities.createPlayer
import io.github.kevinah95.norman_the_necromancer.fx.Fx
import io.github.kevinah95.norman_the_necromancer.levels.LevelManager
import io.github.kevinah95.norman_the_necromancer.renderer.GameRenderer
import io.github.kevinah95.norman_the_necromancer.rituals.StreakRitual
import io.github.kevinah95.norman_the_necromancer.shop.ShopManager
import io.github.kevinah95.norman_the_necromancer.i18n.GameStrings
import korlibs.event.Key
import korlibs.korge.input.keys
import korlibs.korge.input.mouse
import korlibs.korge.input.onDown
import korlibs.korge.input.onMove
import korlibs.korge.input.onUp
import korlibs.korge.render.RenderContext
import korlibs.korge.scene.Scene
import korlibs.korge.view.SContainer
import korlibs.korge.view.View
import korlibs.korge.view.addUpdater
import korlibs.time.milliseconds
import kotlin.math.atan2

class NormanGameScene : Scene() {
    private val renderer = GameRenderer()
    private lateinit var game: Game
    private var isPaused = false
    private var touchAimActive = false
    private var dialogueTimer = 0.0
    private var dialogueCooldown = 0.0
    private var normanIsBouncing = false

    companion object {
        // UI Hitbox regions for pointer interactions (matching positions rendered by GameRenderer)
        private val LANG_BUTTON_BOUNDS = Rect2D(320.0, 5.0, 78.0, 27.0) // Top-right language toggle
        private val PAUSE_BUTTON_BOUNDS = Rect2D(335.0, 5.0, 63.0, 27.0) // Top-right pause button
        private val WIN_SHORTCUT_BOUNDS = Rect2D(295.0, 5.0, 40.0, 20.0) // Level indicator debug tap
        private val RESURRECT_BUTTON_BOUNDS = Rect2D(130.0, 150.0, 140.0, 28.0) // Bottom resurrect action
        private const val SHOP_ITEM_START_Y = 40.0
        private const val SHOP_ITEM_HEIGHT = 12.0
    }

    override suspend fun SContainer.sceneMain() {
        GameAtlas.load()
        initGame()
        GameAudio.play()

        // Dedicated rendering view that draws all layers with KorGE's BatchBuilder2D
        val gameView = object : View() {
            override fun renderInternal(ctx: RenderContext) {
                ctx.useBatcher { batch ->
                    renderer.render(batch, ctx, game, isPaused, touchAimActive)
                }
            }
        }
        addChild(gameView)

        // Main game update loop
        addUpdater { dt ->
            val dtMs = dt.milliseconds
            update(dtMs)
        }

        // Setup touch and mouse controls
        setupInput()
    }

    private fun initGame() {
        val player = createPlayer()
        player.spriteName = "skull" // Starts as a skull in the intro!
        game = Game().apply {
            this.player = player
            spawn(player)
            dialogue.addAll(GameStrings.getIntroDialogue())
            addRitual(StreakRitual)
        }
        ShopManager.init(game)
        LevelManager.init(game)
        TweenManager.reset()
        dialogueTimer = 0.0
        dialogueCooldown = 500.0
        normanIsBouncing = false
        isPaused = false
        touchAimActive = false
        Fx.activeEmitters.clear()
        Fx.dust(game.stage.width, game.stage.height).burst(150)
    }

    /**
     * Main update tick (Game Loop).
     * Dispatches updates based on the current [GameState] machine.
     */
    private fun update(dtMs: Double) {
        if (dialogueCooldown > 0.0) {
            dialogueCooldown -= dtMs
        }
        updateDialogue(dtMs)
        if (isPaused) return

        when (game.state) {
            GameState.LOSE -> {
                onLose()
                return
            }
            GameState.PLAYING -> {
                if (game.player.hp <= 0) {
                    onLose()
                    return
                }
                LevelManager.updateLevel(dtMs)
                game.update(dtMs)

                if (game.state == GameState.LOSE || game.player.hp <= 0) {
                    onLose()
                    return
                }

                checkLevelProgression()
                checkEasterEggBounce()
            }
            GameState.SHOPPING -> {
                game.update(dtMs)
            }
            GameState.INTRO, GameState.WIN -> {
                // Diálogo estático o cinemática; se actualizan tweens y partículas abajo
            }
        }

        TweenManager.update(dtMs)
        Fx.update(dtMs)
    }

    private fun checkLevelProgression() {
        if (LevelManager.isLevelFinished()) {
            if (LevelManager.isComplete()) {
                onWin()
            } else {
                game.onLevelEnd()
                ShopManager.enterShop()
            }
        }
    }

    private fun checkEasterEggBounce() {
        if (game.level == 2 && !normanIsBouncing) {
            game.player.addBehaviour(March(game.player, 0.0))
            game.player.updateClock = 100.0
            game.player.updateSpeed = (60_000.0 / 240.0) * 2.0
            normanIsBouncing = true
        }
    }

    private fun onWin() {
        game.state = GameState.WIN
        game.dialogue.clear()
        game.dialogue.addAll(GameStrings.getOutroDialogue())
        dialogueCooldown = 1500.0
        dialogueTimer = 0.0
        touchAimActive = false
        isPaused = false
        game.player.hop = 0.0
        game.player.spriteName = "norman_arms_down"
        GameAudio.useOutroSynths()
    }

    private fun onLose() {
        initGame()
        GameAudio.useLevelSynths(game.level)
    }

    /**
     * Automatically advances dialogue text after a display time limit.
     * In INTRO and WIN states, the final line is kept on screen until confirmed by the user.
     */
    private fun updateDialogue(dtMs: Double) {
        dialogueTimer += dtMs
        val limit = if (game.state == GameState.WIN) 8000.0 else 5500.0
        if (dialogueTimer > limit) {
            dialogueTimer = 0.0
            if (game.state == GameState.WIN || game.state == GameState.INTRO) {
                if (game.dialogue.size > 1) {
                    game.dialogue.removeAt(0)
                }
            } else if (game.dialogue.isNotEmpty()) {
                game.dialogue.removeAt(0)
            }
        }
    }

    /**
     * Advances dialogue to the next page, or invokes [onComplete] if at the end of the sequence.
     */
    private fun advanceDialogueOr(onComplete: () -> Unit) {
        if (dialogueCooldown > 0.0) return
        if (game.dialogue.size > 1) {
            game.dialogue.removeAt(0)
            dialogueTimer = 0.0
            dialogueCooldown = 250.0
        } else {
            onComplete()
        }
    }

    private fun advanceIntroOrStart() {
        advanceDialogueOr {
            game.state = GameState.PLAYING
            game.player.spriteName = "norman_arms_down"
            game.dialogue.clear()
            GameAudio.play()
            GameAudio.useLevelSynths(game.level)
        }
    }

    private fun advanceOutroOrRestart() {
        advanceDialogueOr {
            onLose()
        }
    }

    /**
     * Common confirmation action for intro dialogue, victory outro, and game over restart.
     */
    private fun handleConfirmAction() {
        when (game.state) {
            GameState.INTRO -> advanceIntroOrStart()
            GameState.WIN -> advanceOutroOrRestart()
            GameState.LOSE -> onLose()
            else -> Unit
        }
    }

    private fun togglePause() {
        isPaused = !isPaused
        if (isPaused) touchAimActive = false
    }

    private fun toggleLanguage() {
        val prevLang = GameStrings.language
        GameStrings.toggleLanguage()

        val (oldList, newList) = when (game.state) {
            GameState.INTRO -> GameStrings.getIntroDialogueFor(prevLang) to GameStrings.getIntroDialogue()
            GameState.WIN -> GameStrings.getOutroDialogueFor(prevLang) to GameStrings.getOutroDialogue()
            else -> return
        }

        val currentIdx = (oldList.size - game.dialogue.size).coerceIn(0, oldList.size - 1)
        game.dialogue.clear()
        game.dialogue.addAll(newList.drop(currentIdx))
        dialogueTimer = 0.0
    }

    /**
     * Normalizes touch or mouse pointer positions across stage and local coordinate systems.
     */
    private fun getPointerPos(evt: korlibs.korge.input.MouseEvents): Point2D {
        val posStage = evt.currentPosStage
        val posLocal = evt.currentPosLocal
        val px = if (posStage.x.isFinite()) posStage.x else posLocal.x
        val py = if (posStage.y.isFinite()) posStage.y else posLocal.y
        return Point2D(px, py)
    }

    private fun SContainer.setupInput() {
        // Pointer down (touch or mouse click)
        onDown { evt ->
            val (px, py) = getPointerPos(evt)

            if (isPaused) {
                // Any tap on screen unpauses cleanly
                isPaused = false
                touchAimActive = false
                return@onDown
            }

            when (game.state) {
                GameState.INTRO -> {
                    if (LANG_BUTTON_BOUNDS.contains(px, py)) {
                        toggleLanguage()
                        return@onDown
                    }
                    advanceIntroOrStart()
                }
                GameState.WIN -> advanceOutroOrRestart()
                GameState.LOSE -> onLose()
                GameState.PLAYING -> {
                    if (PAUSE_BUTTON_BOUNDS.contains(px, py)) {
                        isPaused = true
                        touchAimActive = false
                        return@onDown
                    }
                    if (WIN_SHORTCUT_BOUNDS.contains(px, py)) {
                        onWin()
                        return@onDown
                    }
                    if (RESURRECT_BUTTON_BOUNDS.contains(px, py)) {
                        if (game.resurrect()) {
                            GameAudio.playResurrect()
                        }
                        return@onDown
                    }

                    // Aim spell towards touch position
                    touchAimActive = true
                    updateAimAngle(px, py)
                }
                GameState.SHOPPING -> {
                    // Item list selection
                    var itemY = SHOP_ITEM_START_Y
                    for (i in ShopManager.items.indices) {
                        if (py in itemY..(itemY + SHOP_ITEM_HEIGHT) && px in 80.0..320.0) {
                            ShopManager.selectedIndex = i
                            return@onDown
                        }
                        itemY += SHOP_ITEM_HEIGHT
                    }

                    // Buy action button tap
                    if (py in (itemY + 15.0)..(itemY + 38.0) && px in 90.0..310.0) {
                        if (ShopManager.buyCurrent()) {
                            GameAudio.playBuy()
                        }
                    }
                }
            }
        }

        // Pointer move / dragging to aim
        onMove { evt ->
            if (isPaused) return@onMove
            val (px, py) = getPointerPos(evt)

            if (game.state == GameState.PLAYING) {
                updateAimAngle(px, py)
            }
        }

        // Pointer up / releasing to fire spell
        onUp {
            if (isPaused) {
                touchAimActive = false
                return@onUp
            }
            if (game.state == GameState.PLAYING && touchAimActive) {
                touchAimActive = false
                if (game.castSpell()) {
                    GameAudio.playCast()
                }
            }
        }

        // Desktop keyboard controls
        keys {
            down(Key.SPACE) {
                if (isPaused) {
                    isPaused = false
                    return@down
                }
                if (game.state == GameState.PLAYING) {
                    if (game.resurrect()) GameAudio.playResurrect()
                } else {
                    handleConfirmAction()
                }
            }
            down(Key.P) { togglePause() }
            down(Key.ESCAPE) { togglePause() }
            down(Key.W) { onWin() }
            down(Key.L) { toggleLanguage() }
            down(Key.UP) {
                if (game.state == GameState.SHOPPING) ShopManager.selectIndex(-1)
            }
            down(Key.DOWN) {
                if (game.state == GameState.SHOPPING) ShopManager.selectIndex(1)
            }
            down(Key.ENTER) {
                if (game.state == GameState.SHOPPING) {
                    if (ShopManager.buyCurrent()) GameAudio.playBuy()
                } else {
                    handleConfirmAction()
                }
            }
        }
    }

    private fun updateAimAngle(screenX: Double, screenY: Double) {
        val sceneX = screenX
        val sceneY = renderer.sceneOriginY - screenY
        val p1 = game.player.center()
        game.spell.targetAngle = atan2(sceneY - p1.y, sceneX - p1.x)
    }
}
