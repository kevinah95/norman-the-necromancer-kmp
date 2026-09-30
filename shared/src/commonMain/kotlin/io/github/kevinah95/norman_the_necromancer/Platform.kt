package io.github.kevinah95.norman_the_necromancer

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform