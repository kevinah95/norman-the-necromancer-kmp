package io.github.kevinah95.norman_the_necromancer.levels

import io.github.kevinah95.norman_the_necromancer.core.*
import io.github.kevinah95.norman_the_necromancer.entities.*

/**
 * Manages wave and level progression using an interpreter pattern.
 *
 * ## Script Format:
 * The [defaultLevelScript] array contains sequential command tokens:
 * - Pairs of `[quantity, enemyId]`: Spawns `quantity` enemies of type `enemyId`.
 * - [END_OF_WAVE]: Halts execution until all living enemies on the battlefield are eliminated.
 * - [END_OF_LEVEL]: Marks the completion of a level wave sequence, triggering the shop or victory screen.
 */
object LevelManager {
    const val END_OF_LEVEL: Int = 99
    const val END_OF_WAVE: Int = 98

    const val ID_VILLAGER: Int = 0
    const val ID_ARCHER: Int = 1
    const val ID_MONK: Int = 2
    const val ID_CHAMPION: Int = 3
    const val ID_PIPER: Int = 4
    const val ID_RAGE_KNIGHT: Int = 5
    const val ID_ROYAL_GUARD: Int = 6
    const val ID_SHELL_KNIGHT: Int = 7
    const val ID_WIZARD: Int = 8
    const val ID_THE_KING: Int = 9
    const val ID_RAT: Int = 10
    const val ID_MOB: Int = 11
    const val ID_BANDIT: Int = 12

    private val spawnLookup: List<() -> GameObject> = listOf(
        ::createVillager,
        ::createArcher,
        ::createMonk,
        ::createChampion,
        ::createPiper,
        ::createRageKnight,
        ::createRoyalGuard,
        ::createShellKnight,
        ::createWizard,
        ::createTheKing,
        ::createRat,
        ::createVillager,
        ::createBandit
    )

    private fun getDelay(id: Int): Double = when (id) {
        ID_RAT -> randomInt(500).toDouble()
        ID_VILLAGER -> randomInt(200).toDouble()
        ID_BANDIT -> randomInt(200).toDouble()
        ID_MOB -> (-randomInt(500)).toDouble()
        else -> 0.0
    }

    private val defaultLevelScript: IntArray = intArrayOf(
        // Level 1
        4, ID_VILLAGER, END_OF_WAVE,
        4, ID_VILLAGER, END_OF_WAVE,
        2, ID_VILLAGER, 1, ID_ARCHER, END_OF_WAVE,
        2, ID_VILLAGER, 1, ID_ARCHER, 4, ID_VILLAGER, END_OF_LEVEL,

        // Level 2
        2, ID_ARCHER, 4, ID_VILLAGER, END_OF_WAVE,
        3, ID_ARCHER, 4, ID_VILLAGER, END_OF_WAVE,
        8, ID_VILLAGER, 2, ID_ARCHER, END_OF_WAVE,
        1, ID_CHAMPION, END_OF_LEVEL,

        // Level 3
        1, ID_MONK, END_OF_WAVE,
        4, ID_BANDIT, END_OF_WAVE,
        2, ID_BANDIT, 1, ID_MONK, END_OF_WAVE,
        2, ID_ARCHER, 1, ID_MONK, END_OF_WAVE,
        4, ID_VILLAGER, 2, ID_BANDIT, 2, ID_ARCHER, 1, ID_MONK, END_OF_LEVEL,

        // Level 4
        1, ID_SHELL_KNIGHT, END_OF_WAVE,
        4, ID_VILLAGER, 3, ID_BANDIT, END_OF_WAVE,
        1, ID_SHELL_KNIGHT, 1, ID_MONK, END_OF_WAVE,
        2, ID_ARCHER, 1, ID_MONK, 1, ID_SHELL_KNIGHT, END_OF_WAVE,
        8, ID_VILLAGER, END_OF_WAVE,
        1, ID_SHELL_KNIGHT, 1, ID_CHAMPION, 1, ID_SHELL_KNIGHT, END_OF_LEVEL,

        // Level 5 - Pied Piper (Miniboss)
        1, ID_RAT, END_OF_WAVE,
        3, ID_RAT, END_OF_WAVE,
        7, ID_RAT, 1, ID_PIPER, END_OF_LEVEL,

        // Level 6
        4, ID_BANDIT, END_OF_WAVE,
        1, ID_RAGE_KNIGHT, END_OF_WAVE,
        4, ID_BANDIT, 1, ID_CHAMPION, 2, ID_ARCHER, END_OF_WAVE,
        4, ID_BANDIT, 1, ID_RAGE_KNIGHT, END_OF_WAVE,
        2, ID_RAGE_KNIGHT, 1, ID_MONK, END_OF_WAVE,
        1, ID_WIZARD, END_OF_LEVEL,

        // Level 7 - Angry Mob
        20, ID_MOB, 1, ID_RAGE_KNIGHT, 20, ID_MOB, 1, ID_RAGE_KNIGHT, 20, ID_MOB, END_OF_WAVE,
        20, ID_MOB, 1, ID_RAGE_KNIGHT, 20, ID_MOB, 1, ID_RAGE_KNIGHT, 20, ID_MOB, END_OF_WAVE,
        3, ID_CHAMPION, END_OF_LEVEL,

        // Level 8
        10, ID_BANDIT, 1, ID_MONK, 10, ID_BANDIT, 1, ID_MONK, END_OF_WAVE,
        10, ID_BANDIT, 1, ID_WIZARD, 1, ID_SHELL_KNIGHT, END_OF_WAVE,
        5, ID_BANDIT, 3, ID_ARCHER, 3, ID_RAGE_KNIGHT, END_OF_WAVE,
        1, ID_CHAMPION, 1, ID_WIZARD, 1, ID_CHAMPION, END_OF_LEVEL,

        // Level 9 - Guards Approaching
        1, ID_VILLAGER, END_OF_WAVE,
        2, ID_ROYAL_GUARD, END_OF_WAVE,
        2, ID_ARCHER, END_OF_WAVE,
        10, ID_ROYAL_GUARD, END_OF_WAVE,
        10, ID_ROYAL_GUARD, 2, ID_MONK, 10, ID_ROYAL_GUARD, END_OF_WAVE,
        2, ID_ROYAL_GUARD, 1, ID_SHELL_KNIGHT, 1, ID_CHAMPION, 1, ID_MONK, END_OF_WAVE,
        2, ID_ROYAL_GUARD, 1, ID_SHELL_KNIGHT, 1, ID_CHAMPION, 1, ID_WIZARD, END_OF_LEVEL,

        // Level 10 - The King (Boss Fight)
        1, ID_THE_KING, END_OF_LEVEL
    )

    var levelScript: IntArray = defaultLevelScript.copyOf()
        private set

    private var cursor: Int = 0
    private var timer: Double = 0.0
    private var currentGame: Game? = null

    fun init(game: Game) {
        currentGame = game
        cursor = 0
        timer = 0.0
        levelScript = defaultLevelScript.copyOf()
    }

    fun isLevelFinished(): Boolean {
        val game = currentGame ?: return false
        if (game.state != GameState.PLAYING || game.player.hp <= 0) return false
        if (cursor >= levelScript.size) return true
        return levelScript[cursor] == END_OF_LEVEL && isCleared()
    }

    fun isComplete(): Boolean {
        return cursor >= levelScript.size - 1
    }

    fun nextLevel() {
        if (cursor < levelScript.size) {
            cursor++
        }
        val game = currentGame ?: return
        game.level++
        game.onLevelStart()
    }

    fun updateLevel(dtMs: Double) {
        val game = currentGame ?: return
        if (cursor >= levelScript.size) return

        timer -= dtMs
        if (timer > 0) {
            return
        }

        val cmd = levelScript[cursor]
        if (cmd == END_OF_WAVE) {
            if (isCleared()) {
                cursor++
            }
        } else if (cmd == END_OF_LEVEL) {
            // Waiting for level finish condition
        } else if (cmd > 0) {
            levelScript[cursor]-- // decrement quantity
            val id = levelScript[cursor + 1]
            val unit = spawnLookup[id]()
            game.spawn(unit)
            timer = unit.updateSpeed + getDelay(id)
        } else {
            cursor += 2
        }
    }

    private fun isCleared(): Boolean {
        val game = currentGame ?: return true
        return !game.objects.any { it.isTagged(Tags.LIVING) }
    }
}
