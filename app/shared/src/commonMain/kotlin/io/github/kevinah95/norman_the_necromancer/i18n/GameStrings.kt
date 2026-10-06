package io.github.kevinah95.norman_the_necromancer.i18n

enum class GameLanguage(val code: String, val label: String) {
    EN("en", "EN"),
    ES("es", "ES")
}

object GameStrings {
    var language: GameLanguage = GameLanguage.EN

    val isSpanish: Boolean get() = language == GameLanguage.ES

    fun toggleLanguage(): GameLanguage {
        language = if (language == GameLanguage.EN) GameLanguage.ES else GameLanguage.EN
        return language
    }

    fun getIntroDialogue(): List<String> = getIntroDialogueFor(language)

    fun getIntroDialogueFor(lang: GameLanguage): List<String> = if (lang == GameLanguage.ES) {
        listOf(
            "Norman no era un nigromante muy popular...",
            "Los otros aldeanos lo cazaban.",
            "A veces incluso terminaban el trabajo.",
            "Pero como buen nigromante con orgullo...",
            "Norman simplemente volvio a levantarse."
        )
    } else {
        listOf(
            "Norman wasn't a particularly popular necromancer...",
            "The other villagers hunted him.",
            "Sometimes they even finished the job.",
            "But like any self-respecting necromancer...",
            "Norman just brought himself back."
        )
    }

    fun getOutroDialogue(): List<String> = getOutroDialogueFor(language)

    fun getOutroDialogueFor(lang: GameLanguage): List<String> = if (lang == GameLanguage.ES) {
        listOf(
            "Habia terminado.",
            "Norman pudo estudiar en paz.",
            "Pero sabia que con el tiempo volverian.",
            "FIN",
            "Juego original por Dan Prince\nCreado para JS13k Games 2022\ndanthedev.com\n\nGracias por jugar!"
        )
    } else {
        listOf(
            "It was over.",
            "Norman was able to study peacefully.",
            "But he knew that eventually, they'd be back.",
            "THE END",
            "Original Game by Dan Prince\nCreated for JS13k Games 2022\ndanthedev.com\n\nThanks for playing!"
        )
    }

    val introTapContinue: String
        get() = if (isSpanish) "(Toca para continuar)" else "(Tap to continue)"

    val introTapToBegin: String
        get() = if (isSpanish) "(Toca en cualquier lugar para comenzar)" else "(Tap anywhere to begin)"

    val introSkipHint: String
        get() = if (isSpanish) "(Toca para continuar)" else "(Tap to continue)"

    val outroTapContinue: String
        get() = if (isSpanish) "(Toca para continuar)" else "(Tap to continue)"

    val outroTapPlayAgain: String
        get() = if (isSpanish) "(Toca en cualquier lugar para jugar de nuevo)" else "(Tap anywhere to play again)"

    val hudPause: String get() = if (isSpanish) "PAUSA" else "PAUSE"
    val hudPlay: String get() = if (isSpanish) "JUGAR" else "PLAY"

    val hudResurrectReady: String
        get() = if (isSpanish) "RESUCITAR (TOCA/ESPACIO)" else "RESURRECT (TAP/SPACE)"

    fun hudResurrectCooldown(seconds: Int): String =
        if (isSpanish) "Resucitar (${seconds}s)" else "Resurrect (${seconds}s)"

    val pauseTitle: String
        get() = if (isSpanish) "JUEGO PAUSADO" else "GAME PAUSED"

    val pauseResume: String
        get() = if (isSpanish) "REANUDAR (TOCA)" else "RESUME (TAP)"

    val shopTitle: String
        get() = if (isSpanish) "=== TIENDA DE RITUALES ===" else "=== RITUALS SHOP ==="

    val shopDesc: String
        get() = "Desc:"

    val shopFree: String
        get() = if (isSpanish) "[GRATIS]" else "[FREE]"

    val shopTapBuy: String
        get() = if (isSpanish) "[ TOCA AQUI PARA COMPRAR ]" else "[ TAP HERE TO BUY ]"

    val shopNeedSouls: String
        get() = if (isSpanish) "[ NECESITAS MAS ALMAS ]" else "[ NEED MORE SOULS ]"

    val shopNextWave: String
        get() = if (isSpanish) "[ TOCA AQUI PARA EL SIGUIENTE NIVEL ]" else "[ TAP HERE TO BEGIN NEXT LEVEL ]"

    // Language toggle badge on intro screen
    val langButtonText: String
        get() = if (isSpanish) " EN  [ES]" else "[EN]  ES "

    private val ritualDescriptionsEn = mapOf(
        // Core Shop Items
        "Heal" to "Heal 1 HP",
        "Renew" to "+1 Max HP",
        "Recharge" to "+1 Max Cast",
        "Continue" to "Begin next level",

        // Rituals
        "Streak" to "Increases reward for consecutive hits",
        "Bouncing" to "Spells bounce",
        "Doubleshot" to "Cast 2 spells",
        "Hunter" to "Spells seek targets",
        "Weightless" to "Spells are not affected by gravity",
        "Knockback" to "Spells knock backwards",
        "Ceiling" to "Adds a ceiling",
        "Rain" to "Spells split when they drop",
        "Drunkard" to "2x damage, wobbly aim",
        "Seer" to "Spells pass through the dead",
        "Tearstone" to "3x damage when < half HP",
        "Impatience" to "Resurrection recharges 2x faster",
        "Bleed" to "Inflicts bleed on hits",
        "Allegiance" to "Summon your honour guard after resurrections",
        "Salvage" to "Corpses become souls at end of level",
        "Studious" to "Rituals are 50% cheaper",
        "Electrodynamics" to "Lightning strikes after hits",
        "Chilly" to "10% chance to freeze enemies",
        "Giants" to "20% chance to resurrect giant skeletons",
        "Avarice" to "+1 soul for each corpse you resurrect",
        "Hardened" to "Undead have +1 HP"
    )

    private val ritualTranslationsEs = mapOf(
        // Core Shop Items
        "Heal" to ("Curar" to "Recupera 1 de Vida"),
        "Renew" to ("Vitalidad" to "+1 Vida Maxima"),
        "Recharge" to ("Carga Magica" to "+1 Hechizo Maximo"),
        "Continue" to ("Continuar" to "Comenzar siguiente nivel"),

        // Rituals
        "Streak" to ("Racha" to "Aumenta la recompensa por impactos consecutivos"),
        "Bouncing" to ("Rebote" to "Los hechizos rebotan"),
        "Doubleshot" to ("Doble Disparo" to "Lanza 2 hechizos"),
        "Hunter" to ("Cazador" to "Los hechizos buscan objetivos"),
        "Weightless" to ("Ingravidez" to "Los hechizos no tienen gravedad"),
        "Knockback" to ("Empuje" to "Los hechizos empujan hacia atras"),
        "Ceiling" to ("Techo" to "Anade un techo"),
        "Rain" to ("Lluvia" to "Los hechizos se dividen al caer"),
        "Drunkard" to ("Borracho" to "2x dano, punteria tambaleante"),
        "Seer" to ("Vidente" to "Los hechizos atraviesan a los muertos"),
        "Tearstone" to ("Piedra Lagrima" to "3x dano con menos de media vida"),
        "Impatience" to ("Impaciencia" to "La resurreccion carga 2x mas rapido"),
        "Bleed" to ("Sangrado" to "Inflige sangrado al impactar"),
        "Allegiance" to ("Lealtad" to "Invoca a tu guardia de honor al resucitar"),
        "Salvage" to ("Rescate" to "Los cadaveres dan almas al final del nivel"),
        "Studious" to ("Estudioso" to "Los rituales cuestan 50% menos"),
        "Electrodynamics" to ("Electrodinamica" to "Rayos caen tras los impactos"),
        "Chilly" to ("Gelido" to "10% de probabilidad de congelar enemigos"),
        "Giants" to ("Gigantes" to "20% de probabilidad de resucitar esqueletos gigantes"),
        "Avarice" to ("Avaricia" to "+1 alma por cada cadaver que resucites"),
        "Hardened" to ("Endurecido" to "Los no-muertos tienen +1 de vida")
    )

    fun getRitualName(englishName: String): String {
        if (!isSpanish) return englishName
        return ritualTranslationsEs[englishName]?.first ?: englishName
    }

    fun getRitualDesc(englishName: String, fallbackDesc: String = ""): String {
        if (!isSpanish) return ritualDescriptionsEn[englishName] ?: fallbackDesc
        return ritualTranslationsEs[englishName]?.second ?: (ritualDescriptionsEn[englishName] ?: fallbackDesc)
    }
}

/**
 * Type-safe constants for all ritual identifiers in the game.
 * Eliminates hardcoded string literals across rituals, shop items, and tests.
 */
object RitualKeys {
    const val STREAK = "Streak"
    const val BOUNCING = "Bouncing"
    const val DOUBLESHOT = "Doubleshot"
    const val HUNTER = "Hunter"
    const val WEIGHTLESS = "Weightless"
    const val KNOCKBACK = "Knockback"
    const val CEILING = "Ceiling"
    const val RAIN = "Rain"
    const val DRUNKARD = "Drunkard"
    const val SEER = "Seer"
    const val TEARSTONE = "Tearstone"
    const val IMPATIENCE = "Impatience"
    const val BLEED = "Bleed"
    const val ALLEGIANCE = "Allegiance"
    const val SALVAGE = "Salvage"
    const val STUDIOUS = "Studious"
    const val ELECTRODYNAMICS = "Electrodynamics"
    const val CHILLY = "Chilly"
    const val GIANTS = "Giants"
    const val AVARICE = "Avarice"
    const val HARDENED = "Hardened"
}

