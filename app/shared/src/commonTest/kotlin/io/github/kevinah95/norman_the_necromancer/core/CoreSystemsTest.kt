package io.github.kevinah95.norman_the_necromancer.core

import io.github.kevinah95.norman_the_necromancer.assets.GameSprites
import io.github.kevinah95.norman_the_necromancer.entities.createCorpse
import io.github.kevinah95.norman_the_necromancer.entities.createPlayer
import io.github.kevinah95.norman_the_necromancer.entities.createVillager
import io.github.kevinah95.norman_the_necromancer.rituals.BouncingRitual
import io.github.kevinah95.norman_the_necromancer.rituals.CeilingRitual
import io.github.kevinah95.norman_the_necromancer.rituals.DoubleshotRitual
import io.github.kevinah95.norman_the_necromancer.rituals.RainRitual
import kotlin.math.PI
import kotlin.math.abs
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class CoreSystemsTest {

    @BeforeTest
    fun setUp() {
        gameRandom = Random(seed = 42)
        TweenManager.reset()
    }

    @AfterTest
    fun tearDown() {
        gameRandom = Random.Default
        TweenManager.reset()
    }

    @Test
    fun testDeterministicRandomWithSeed42() {
        val rng1 = Random(seed = 42)
        val seq1 = List(5) { randomInt(100, rng1) }
        val floats1 = List(5) { randomFloat(10.0, rng1) }
        val ranges1 = List(5) { randomRange(5.0, 2.0, rng1) }
        val list = listOf("a", "b", "c", "d")
        val elems1 = List(5) { list.randomElement(rng1) }

        val rng2 = Random(seed = 42)
        val seq2 = List(5) { randomInt(100, rng2) }
        val floats2 = List(5) { randomFloat(10.0, rng2) }
        val ranges2 = List(5) { randomRange(5.0, 2.0, rng2) }
        val elems2 = List(5) { list.randomElement(rng2) }

        assertEquals(seq1, seq2)
        assertEquals(floats1, floats2)
        assertEquals(ranges1, ranges2)
        assertEquals(elems1, elems2)

        // Edge cases for randomInt bounds
        assertEquals(0, randomInt(0))
        assertEquals(0, randomInt(-5))
        assertEquals(10, randomInt(10, 10))
        assertEquals(10, randomInt(10, 5))
        assertTrue(randomInt(5, 10) in 5 until 10)
    }

    @Test
    fun testGeometryCalculationsAndClamp() {
        val p1 = Point2D(0.0, 0.0)
        val p2 = Point2D(3.0, 4.0)
        assertEquals(5.0, p1.distanceTo(p2), 1e-6)
        assertEquals(0.0, Point2D(0.0, 0.0).angleTo(Point2D(10.0, 0.0)), 1e-6)

        val (vx, vy) = vectorFromAngle(0.0)
        assertEquals(1.0, vx, 1e-6)
        assertEquals(0.0, vy, 1e-6)
        assertEquals(DEG_90, vectorToAngle(0.0, 1.0), 1e-6)
        assertEquals(PI, DEG_180, 1e-6)
        assertEquals(PI * 1.5, DEG_270, 1e-6)
        assertEquals(PI * 2.0, DEG_360, 1e-6)

        val r1 = Rect2D(0.0, 0.0, 10.0, 10.0)
        val r2 = Rect2D(5.0, 5.0, 10.0, 10.0)
        val r3 = Rect2D(20.0, 20.0, 5.0, 5.0)
        assertTrue(r1.contains(Point2D(5.0, 5.0)))
        assertFalse(r1.contains(Point2D(15.0, 5.0)))
        assertTrue(r1.overlaps(r2))
        assertFalse(r1.overlaps(r3))

        assertEquals(5.0, clamp(2.0, 5.0, 10.0))
        assertEquals(10.0, clamp(15.0, 5.0, 10.0))
        assertEquals(7.0, clamp(7.0, 5.0, 10.0))

        assertEquals(5, clamp(2, 5, 10))
        assertEquals(10, clamp(15, 5, 10))
        assertEquals(7, clamp(7, 5, 10))
    }

    @Test
    fun testGameObjectBehaviourChainAndCircuitBreaker() {
        val obj = GameObject().apply {
            updateSpeed = 100.0
            updateClock = 100.0
        }

        var firstUpdated = 0
        var secondUpdated = 0

        class BlockingBehaviour(go: GameObject) : Behaviour(go) {
            var shouldBlock = false
            override fun onUpdate(): Boolean {
                firstUpdated++
                return shouldBlock
            }
        }

        class FollowUpBehaviour(go: GameObject) : Behaviour(go) {
            override fun onUpdate(): Boolean {
                secondUpdated++
                return false
            }
        }

        val b1 = BlockingBehaviour(obj)
        val b1Duplicate = BlockingBehaviour(obj)
        val b2 = FollowUpBehaviour(obj)

        assertSame(b1, obj.addBehaviour(b1))
        // Adding duplicate concrete behaviour class returns existing instance
        assertSame(b1, obj.addBehaviour(b1Duplicate))
        obj.addBehaviour(b2)
        assertNotNull(obj.getBehaviour<BlockingBehaviour>())

        // Tick 1: b1 does not block, so b2 also runs
        obj.update(100.0)
        assertEquals(1, firstUpdated)
        assertEquals(1, secondUpdated)

        // Tick 2: b1 returns true (circuit breaker), stopping b2 from running
        b1.shouldBlock = true
        obj.update(100.0)
        assertEquals(2, firstUpdated)
        assertEquals(1, secondUpdated)

        obj.removeBehaviour(b1)
        assertNull(obj.getBehaviour<BlockingBehaviour>())
    }

    @Test
    fun testPhysicsBoundariesBounceFrictionAndGravity() {
        val player = createPlayer()
        val game = Game().apply {
            this.player = player
            spawn(player)
        }

        var bounced = false
        val ball = GameObject().apply {
            x = 50.0
            y = 10.0
            vx = 20.0
            vy = -50.0
            mass = 100.0
            bounce = 0.5
            friction = 0.2
            onBounceAction = { bounced = true }
        }
        game.spawn(ball, 50.0, 10.0)

        // Advance 500ms so ball hits the floor (y < 0) and bounces
        game.update(500.0)
        assertEquals(0.0, ball.y, 1e-6)
        assertTrue(bounced, "Object should trigger onBounce when hitting floor at speed >= 10")
        assertTrue(ball.vy > 0.0, "Velocity Y should invert after bouncing")
        assertEquals(16.0, ball.vx, 1e-6) // 20.0 * (1 - 0.2)
    }

    @Test
    fun testRitualExclusivityAndRequirements() {
        val player = createPlayer()
        val game = Game().apply {
            this.player = player
            spawn(player)
        }

        // CeilingRitual requires BOUNCING tag
        assertFalse(game.canAddRitual(CeilingRitual))
        game.addRitual(BouncingRitual)
        assertTrue(game.canAddRitual(CeilingRitual))

        // DoubleshotRitual and RainRitual share exclusive SPLITTING tag
        assertTrue(game.canAddRitual(DoubleshotRitual))
        game.addRitual(DoubleshotRitual)
        assertFalse(game.canAddRitual(RainRitual))
    }

    @Test
    fun testTweenManagerCustomEasingAndScreenshake() {
        TweenManager.screenshake(100.0)
        assertEquals(100.0, TweenManager.screenShakeTimer)

        var latestVal = 0.0
        TweenManager.tween(
            startValue = 10.0,
            endValue = 30.0,
            duration = 100.0,
            ease = { t -> t * t }
        ) { value, _ ->
            latestVal = value
        }

        TweenManager.update(50.0)
        assertEquals(50.0, TweenManager.screenShakeTimer)
        assertEquals(15.0, latestVal, 1e-4) // 10 + 20 * (0.5^2) = 15

        TweenManager.update(50.0)
        assertEquals(0.0, TweenManager.screenShakeTimer)
        assertEquals(30.0, latestVal, 1e-4)
        assertTrue(TweenManager.activeTweens.isEmpty(), "Completed tween must be removed")
    }

    @Test
    fun testCustomRitualSecondaryConstructorAndCallbacks() {
        val custom = Ritual(
            name = "CustomRitual",
            description = "Custom description",
            rarity = Rarity.RARE
        )
        val defaultRarityRitual = Ritual(
            name = "DefaultRarityRitual",
            description = "Default rarity description"
        )
        assertEquals(Rarity.COMMON, defaultRarityRitual.rarity)
        assertEquals("CustomRitual", custom.name)
        assertEquals("Custom description", custom.description)
        assertEquals(Rarity.RARE, custom.rarity)

        val player = createPlayer()
        val game = Game().apply {
            this.player = player
            spawn(player)
            addRitual(custom)
        }
        game.onLevelStart()
        game.onLevelEnd()
        game.onShopEnter()
        game.castSpell()
        assertEquals(500.0, game.castAnimationTimer)
        game.ability.timer = game.ability.cooldown
        game.spawn(createCorpse(), 20.0, 0.0)
        game.resurrect()
        val enemy = createVillager()
        game.spawn(enemy, 50.0, 0.0)
        game.die(enemy)
        game.update(16.0)

        // Test base Behaviour default no-op hooks, out-of-bounds addBehaviour index, onDamageAction, and despawnOnBounce
        var damageCallbackFired = false
        val testObj = GameObject().apply {
            hp = 2
            maxHp = 2
            despawnOnBounce = true
            onDamageAction = { damageCallbackFired = true }
        }
        game.spawn(testObj, 10.0, 0.0)
        val baseBehaviour = Behaviour(testObj)
        testObj.addBehaviour(baseBehaviour, index = -1)
        game.damage(testObj, 1)
        assertTrue(damageCallbackFired)
        testObj.onCollision(player)
        testObj.onBounce()
        assertFalse(testObj in game.objects, "Object with despawnOnBounce should despawn on bounce")

        // Re-attach baseBehaviour to verify onDeath default hook when hp reaches 0
        val dyingObj = GameObject().apply {
            hp = 1
            maxHp = 1
        }
        game.spawn(dyingObj, 15.0, 0.0)
        dyingObj.addBehaviour(Behaviour(dyingObj))
        game.damage(dyingObj, 1)

        // Tween with default Easing parameter
        TweenManager.tween(0.0, 10.0, 50.0) { _, _ -> }
        TweenManager.update(50.0)
    }

    @Test
    fun testCorpseSpawnOnEnemyDeathWithSeededRandom() {
        val player = createPlayer()
        val game = Game().apply {
            this.player = player
            spawn(player)
        }
        val villager = createVillager().apply {
            corpseChance = 1.0
            souls = 10
        }
        game.spawn(villager, 120.0, 0.0)
        game.streak = 5 // 50% bonus souls

        game.damage(villager, 10, player)
        assertEquals(15.0, game.souls, 1e-6)
        assertTrue(game.objects.any { it.isTagged(Tags.CORPSE) })
    }

    @Test
    fun testCastAnimationTimerResetsNormanArmsDown() {
        val player = createPlayer()
        val game = Game().apply {
            this.player = player
            spawn(player)
        }
        assertTrue(game.castSpell())
        assertEquals(GameSprites.NORMAN_ARMS_UP, player.spriteName)

        game.update(500.0)
        assertEquals(GameSprites.NORMAN_ARMS_DOWN, player.spriteName)
    }
}
