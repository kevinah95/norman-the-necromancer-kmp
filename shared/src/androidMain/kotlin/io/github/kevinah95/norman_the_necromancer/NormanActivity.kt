package io.github.kevinah95.norman_the_necromancer

import korlibs.render.KorgwActivity

open class NormanActivity : KorgwActivity() {
    override suspend fun activityMain() {
        launchNormanGame()
    }
}
