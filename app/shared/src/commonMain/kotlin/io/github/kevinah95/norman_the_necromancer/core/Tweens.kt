package io.github.kevinah95.norman_the_necromancer.core

import korlibs.math.interpolation.Easing

class Tween(
    val startValue: Double,
    val endValue: Double,
    val duration: Double,
    val ease: Easing = Easing.LINEAR,
    val callback: (value: Double, progress: Double) -> Unit
) {
    var elapsed: Double = 0.0
}

object TweenManager {
    val activeTweens: MutableList<Tween> = mutableListOf()
    var screenShakeTimer: Double = 0.0

    fun reset() {
        activeTweens.clear()
        screenShakeTimer = 0.0
    }

    fun screenshake(timeMs: Double) {
        screenShakeTimer = timeMs
    }

    fun tween(
        startValue: Double,
        endValue: Double,
        duration: Double,
        ease: Easing = Easing.LINEAR,
        callback: (value: Double, progress: Double) -> Unit
    ) {
        activeTweens.add(Tween(startValue, endValue, duration, ease, callback))
    }

    /** Overload for custom lambda easing support */
    fun tween(
        startValue: Double,
        endValue: Double,
        duration: Double,
        ease: (Double) -> Double,
        callback: (value: Double, progress: Double) -> Unit
    ) {
        tween(startValue, endValue, duration, Easing { ease(it.toDouble()).toFloat() }, callback)
    }

    fun update(dtMs: Double) {
        if (screenShakeTimer > 0) {
            screenShakeTimer -= dtMs
        }

        val iterator = activeTweens.iterator()
        while (iterator.hasNext()) {
            val tw = iterator.next()
            tw.elapsed += dtMs
            val progress = clamp(tw.elapsed / tw.duration, 0.0, 1.0)
            val t = tw.ease(progress.toFloat()).toDouble()
            val value = tw.startValue + (tw.endValue - tw.startValue) * t
            tw.callback(value, t)
            if (progress >= 1.0) {
                iterator.remove()
            }
        }
    }
}
