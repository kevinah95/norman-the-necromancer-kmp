package io.github.kevinah95.norman_the_necromancer.core

import kotlin.math.*
import kotlin.random.Random

const val DEG_180: Double = PI
const val DEG_90: Double = DEG_180 / 2.0
const val DEG_270: Double = DEG_180 + DEG_90
const val DEG_360: Double = DEG_180 * 2.0

data class Point2D(var x: Double = 0.0, var y: Double = 0.0) {
    fun distanceTo(other: Point2D): Double = hypot(other.x - x, other.y - y)
    fun angleTo(other: Point2D): Double = atan2(other.y - y, other.x - x)
}

data class Rect2D(var x: Double = 0.0, var y: Double = 0.0, var w: Double = 0.0, var h: Double = 0.0) {
    val centerX: Double get() = x + w / 2.0
    val centerY: Double get() = y + h / 2.0
    val right: Double get() = x + w
    val bottom: Double get() = y + h

    fun contains(px: Double, py: Double): Boolean = px in x..right && py in y..bottom
    fun contains(point: Point2D): Boolean = contains(point.x, point.y)

    fun overlaps(other: Rect2D): Boolean {
        return x < other.x + other.w &&
                y < other.y + other.h &&
                x + w > other.x &&
                y + h > other.y
    }

    /**
     * Returns a new [Rect2D] expanded symmetrically by [padding] in all four directions.
     * Useful for enlarging mobile touch targets without modifying visual boundaries.
     */
    fun expanded(padding: Double): Rect2D =
        Rect2D(x - padding, y - padding, w + padding * 2.0, h + padding * 2.0)
}

/** Interoperability extensions with KorGE's native Geometry primitives */
fun Point2D.toKorGe(): korlibs.math.geom.Point = korlibs.math.geom.Point(x, y)
fun korlibs.math.geom.Point.toPoint2D(): Point2D = Point2D(x, y)

fun Rect2D.toKorGe(): korlibs.math.geom.Rectangle = korlibs.math.geom.Rectangle(x, y, w, h)
fun korlibs.math.geom.Rectangle.toRect2D(): Rect2D = Rect2D(x, y, width, height)

fun clamp(value: Double, min: Double, max: Double): Double =
    if (value < min) min else if (value > max) max else value

fun clamp(value: Int, min: Int, max: Int): Int =
    if (value < min) min else if (value > max) max else value

fun vectorFromAngle(radians: Double): Pair<Double, Double> =
    Pair(cos(radians), sin(radians))

fun vectorToAngle(x: Double, y: Double): Double = atan2(y, x)

fun randomInt(max: Int): Int = if (max <= 0) 0 else Random.nextInt(max)

fun randomInt(min: Int, max: Int): Int = if (max <= min) min else Random.nextInt(min, max)

fun randomFloat(max: Double = 1.0): Double = Random.nextDouble() * max

fun randomRange(base: Double, spread: Double): Double = base + Random.nextDouble() * spread

fun <T> List<T>.randomElement(): T = this[Random.nextInt(size)]
