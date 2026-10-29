package dev.navalcode.rumbo.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke

/** Original vector landscapes. No network assets or third-party photos. */
@Composable
fun Landscape(variant: Int, modifier: Modifier = Modifier) {
    Canvas(modifier.clipToBounds()) {
        val w = size.width
        val h = size.height
        val coast = variant % 3 == 1
        val forest = variant % 3 == 2
        val sky = when {
            coast -> Color(0xFFC5DADB)
            forest -> Color(0xFFD6DDC9)
            else -> Color(0xFFDADBC8)
        }
        drawRect(Brush.verticalGradient(listOf(sky, Color(0xFFF0EAD2))))
        drawCircle(Color(0xFFF8EAC3), h * .17f, Offset(w * .76f, h * .26f))
        fun ridge(color: Color, vararg points: Pair<Float, Float>) {
            val path = Path().apply {
                moveTo(0f, h)
                points.forEach { (x, y) -> lineTo(w * x, h * y) }
                lineTo(w, h)
                close()
            }
            drawPath(path, color)
        }
        if (coast) {
            drawRect(Color(0xFF719D9D), topLeft = Offset(0f, h * .48f), size = androidx.compose.ui.geometry.Size(w, h * .52f))
            for (i in 0..5) {
                val y = h * (.57f + i * .055f)
                drawLine(Color(0xFFB9D0C8).copy(alpha = .5f), Offset(w * .12f, y), Offset(w * (.55f + i * .045f), y), 1.7f)
            }
            ridge(Color(0xFF607C68), 0f to .58f, .12f to .32f, .31f to .45f, .43f to .64f, .65f to .77f, 1f to .91f)
            ridge(Color(0xFFDBCCA2), 0f to .76f, .17f to .67f, .31f to .75f, .58f to .88f, 1f to .96f)
            ridge(Color(0xFF4D6853), 0f to .66f, .12f to .56f, .27f to .66f, .32f to .86f, .18f to 1f)
        } else {
            ridge(Color(0xFF9AAE9C), 0f to .69f, .16f to .36f, .28f to .52f, .48f to .18f, .69f to .55f, .83f to .34f, 1f to .6f)
            ridge(Color(0xFF688572), 0f to .7f, .2f to .51f, .38f to .7f, .57f to .43f, .78f to .67f, 1f to .49f)
            ridge(Color(0xFF395D4D), 0f to .76f, .19f to .7f, .44f to .86f, .64f to .67f, .86f to .77f, 1f to .71f)
            val trail = Path().apply {
                moveTo(w * .46f, h * 1.08f)
                cubicTo(w * .67f, h * .91f, w * .27f, h * .88f, w * .57f, h * .74f)
            }
            drawPath(trail, Color(0xFFE7D7AE), style = Stroke(width = h * .035f))
        }
        if (forest) {
            for (i in 0..11) {
                val x = w * i / 10f
                val bottom = h * (.85f + (i % 3) * .05f)
                val height = h * (.27f + (i % 4) * .045f)
                val tree = Path().apply {
                    moveTo(x, bottom - height)
                    lineTo(x - height * .32f, bottom)
                    lineTo(x + height * .32f, bottom)
                    close()
                }
                drawPath(tree, Color(0xFF234C3C).copy(alpha = .9f))
            }
        }
        // Fine contour lines suggest a field notebook rather than photographic realism.
        for (i in 0..3) {
            val contour = Path().apply {
                moveTo(w * -.1f, h * (.89f + i * .065f))
                cubicTo(w * .12f, h * (.67f + i * .07f), w * .72f, h * (1.06f + i * .03f), w * 1.1f, h * (.75f + i * .08f))
            }
            drawPath(contour, Color.White.copy(alpha = .12f), style = Stroke(1f))
        }
    }
}
