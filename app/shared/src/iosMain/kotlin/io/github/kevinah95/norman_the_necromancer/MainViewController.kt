package io.github.kevinah95.norman_the_necromancer

import io.github.kevinah95.norman_the_necromancer.core.GameLayout
import io.github.kevinah95.norman_the_necromancer.scene.NormanGameScene
import korlibs.korge.BaseKorgeIosUIViewProvider
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController {
    return BaseKorgeIosUIViewProvider().createViewInfo(
        NormanGameScene(),
        GameLayout.VIRTUAL_WIDTH_INT,
        GameLayout.VIRTUAL_HEIGHT_INT
    ).controller
}