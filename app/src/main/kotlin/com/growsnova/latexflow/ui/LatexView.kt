package com.growsnova.latexflow.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import ru.noties.jlatexmath.JLatexMathDrawable
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.widget.ImageView

@Composable
fun LatexView(
    latex: String,
    modifier: Modifier = Modifier,
    textSize: Float = 50f,
    color: Int = Color.BLACK
) {
    AndroidView(
        factory = { context ->
            ImageView(context).apply {
                scaleType = ImageView.ScaleType.FIT_CENTER
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp, max = 200.dp),
        update = { imageView ->
            if (latex.isNotEmpty()) {
                try {
                    val drawable = JLatexMathDrawable.builder(latex)
                        .textSize(textSize)
                        .color(color)
                        .build()
                    
                    val bitmap = Bitmap.createBitmap(
                        drawable.intrinsicWidth.coerceAtLeast(1),
                        drawable.intrinsicHeight.coerceAtLeast(1),
                        Bitmap.Config.ARGB_8888
                    )
                    val canvas = Canvas(bitmap)
                    drawable.draw(canvas)
                    imageView.setImageBitmap(bitmap)
                } catch (e: Exception) {
                    // Handle parsing error silently or show placeholder
                    imageView.setImageBitmap(null)
                }
            } else {
                imageView.setImageBitmap(null)
            }
        }
    )
}
