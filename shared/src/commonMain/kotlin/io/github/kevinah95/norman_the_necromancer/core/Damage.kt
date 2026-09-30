package io.github.kevinah95.norman_the_necromancer.core

data class Damage(
    var amount: Int,
    val dealer: GameObject? = null
)

data class Death(
    val gameObject: GameObject,
    val killer: GameObject? = null,
    val souls: Int
)
