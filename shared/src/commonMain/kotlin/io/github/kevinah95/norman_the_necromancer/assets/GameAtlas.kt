package io.github.kevinah95.norman_the_necromancer.assets

import korlibs.image.bitmap.Bitmap
import korlibs.image.bitmap.Bitmap32
import korlibs.image.bitmap.BmpSlice
import korlibs.image.bitmap.sliceWithSize
import korlibs.image.format.readBitmap
import korlibs.io.serialization.json.Json
import korlibs.io.stream.openAsync
import normanthenecromancer.shared.generated.resources.Res

data class SpriteRect(val x: Int, val y: Int, val w: Int, val h: Int)

object GameAtlas {
    lateinit var bitmap: Bitmap
    val sprites: MutableMap<String, BmpSlice> = mutableMapOf()
    val spriteRects: MutableMap<String, SpriteRect> = mutableMapOf()

    // Font metrics
    const val GLYPH_WIDTH: Int = 5
    const val GLYPH_HEIGHT: Int = 6
    const val LINE_HEIGHT: Int = 7
    val glyphWidths: MutableMap<Char, Int> = mutableMapOf()
    val glyphSlices: MutableMap<Char, BmpSlice> = mutableMapOf()

    suspend fun load() {
        val pngBytes = Res.readBytes("files/sprites.png")
        bitmap = pngBytes.openAsync().readBitmap()

        val jsonStr = Res.readBytes("files/sprites.json").decodeToString()
        val parsed = Json.parse(jsonStr) as? Map<*, *> ?: emptyMap<String, Any>()

        for ((key, value) in parsed) {
            val name = key.toString()
            val list = value as? List<*> ?: continue
            if (list.size >= 4) {
                val x = (list[0] as Number).toInt()
                val y = (list[1] as Number).toInt()
                val w = (list[2] as Number).toInt()
                val h = (list[3] as Number).toInt()
                spriteRects[name] = SpriteRect(x, y, w, h)
                sprites[name] = bitmap.sliceWithSize(x, y, w, h, name = name)
            }
        }

        // Setup font overrides from font.json
        initFont()
    }

    private fun initFont() {
        val overrides = mapOf(
            "mMWTVw/$%" to 6,
            "I1f-=*+?{}\"" to 4,
            "lj[]()|'`, " to 3,
            "i:.!" to 2
        )
        for ((chars, width) in overrides) {
            for (c in chars) {
                glyphWidths[c] = width
            }
        }

        // Build a padded font atlas (1px transparent padding around each glyph)
        // to prevent texture bleeding from adjacent glyphs and sprites on GPU scaling
        val bmp32 = bitmap.toBMP32()
        val pad = 1
        val cellW = GLYPH_WIDTH + pad * 2 // 7
        val cellH = GLYPH_HEIGHT + pad * 2 // 8
        val fontBmp = Bitmap32(32 * cellW, 3 * cellH)

        for (i in 0 until 96) {
            val char = (32 + i).toChar()
            val col = i % 32
            val row = i / 32
            val sx = col * GLYPH_WIDTH
            val sy = row * GLYPH_HEIGHT
            val dx = col * cellW + pad
            val dy = row * cellH + pad

            for (gy in 0 until GLYPH_HEIGHT) {
                for (gx in 0 until GLYPH_WIDTH) {
                    fontBmp.setRgba(dx + gx, dy + gy, bmp32.getRgba(sx + gx, sy + gy))
                }
            }
            glyphSlices[char] = fontBmp.sliceWithSize(dx, dy, GLYPH_WIDTH, GLYPH_HEIGHT, name = "glyph_$char")
        }
    }

    fun getSprite(name: String): BmpSlice? = sprites[name]

    fun getGlyphWidth(char: Char): Int = glyphWidths[char] ?: GLYPH_WIDTH
}
