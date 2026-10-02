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

    fun getRitualDesc(englishName: String, fallbackDesc: String): String {
        if (!isSpanish) return fallbackDesc
        return ritualTranslationsEs[englishName]?.second ?: fallbackDesc
    }
}
