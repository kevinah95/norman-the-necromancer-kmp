package io.github.kevinah95.norman_the_necromancer.core

/**
 * Single source of truth (SSOT) for game canvas dimensions, UI positions,
 * and clickable button hitboxes.
 *
 * Sharing these definitions between GameRenderer and NormanGameScene guarantees
 * that visual UI elements and their touch/click interaction zones are always 100% aligned.
 */
object GameLayout {
    // Virtual Canvas Dimensions
    const val VIRTUAL_WIDTH: Double = 400.0
    const val VIRTUAL_HEIGHT: Double = 200.0
    const val VIRTUAL_WIDTH_INT: Int = 400
    const val VIRTUAL_HEIGHT_INT: Int = 200
    const val SCENE_ORIGIN_Y: Double = 150.0

    // Top Bar HUD & Controls
    val PAUSE_BUTTON: Rect2D = Rect2D(x = 346.0, y = 8.0, w = 46.0, h = 18.0)
    val LANG_BUTTON: Rect2D = Rect2D(x = 332.0, y = 8.0, w = 60.0, h = 18.0)
    val WIN_SHORTCUT: Rect2D = Rect2D(x = 295.0, y = 5.0, w = 40.0, h = 20.0) // Level indicator debug tap

    // Bottom In-Game Action Button
    val RESURRECT_BUTTON: Rect2D = Rect2D(x = 140.0, y = 158.0, w = 120.0, h = 15.0)

    // Pause Modal Dialog
    const val PAUSE_MODAL_WIDTH: Double = 160.0
    const val PAUSE_MODAL_HEIGHT: Double = 68.0
    val PAUSE_MODAL: Rect2D = Rect2D(
        x = (VIRTUAL_WIDTH - PAUSE_MODAL_WIDTH) / 2.0,
        y = (VIRTUAL_HEIGHT - PAUSE_MODAL_HEIGHT) / 2.0,
        w = PAUSE_MODAL_WIDTH,
        h = PAUSE_MODAL_HEIGHT
    )
    val RESUME_BUTTON: Rect2D = Rect2D(
        x = PAUSE_MODAL.x + (PAUSE_MODAL.w - 96.0) / 2.0,
        y = PAUSE_MODAL.y + 34.0,
        w = 96.0,
        h = 18.0
    )

    // Touch Target Bounds (expanded for mobile accessibility)
    val PAUSE_TOUCH_BOUNDS: Rect2D = PAUSE_BUTTON.expanded(8.0)
    val LANG_TOUCH_BOUNDS: Rect2D = LANG_BUTTON.expanded(8.0)
    val RESURRECT_TOUCH_BOUNDS: Rect2D = RESURRECT_BUTTON.expanded(10.0)
    val RESUME_TOUCH_BOUNDS: Rect2D = RESUME_BUTTON.expanded(12.0)
    val WIN_SHORTCUT_TOUCH_BOUNDS: Rect2D = WIN_SHORTCUT

    // Shop Layout
    const val SHOP_ITEM_START_Y: Double = 40.0
    const val SHOP_ITEM_HEIGHT: Double = 12.0
    const val SHOP_ITEM_LIST_X: Double = 100.0
    const val SHOP_ITEM_TOUCH_X_MIN: Double = 80.0
    const val SHOP_ITEM_TOUCH_X_MAX: Double = 320.0
    const val SHOP_ACTION_TOUCH_X_MIN: Double = 90.0
    const val SHOP_ACTION_TOUCH_X_MAX: Double = 310.0
}
