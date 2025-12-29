package com.growsnova.latexflow.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.scilab.forge.jlatexmath.TeXFormula
import org.scilab.forge.jlatexmath.TeXIcon
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
                    val formula = TeXFormula(latex)
                    val icon = formula.createTeXIcon(TeXFormula.SERIF, textSize)
                    
                    val bitmap = Bitmap.createBitmap(
                        icon.iconWidth.coerceAtLeast(1),
                        icon.iconHeight.coerceAtLeast(1),
                        Bitmap.Config.ARGB_8888
                    )
                    val canvas = Canvas(bitmap)
                    icon.paintIcon(canvas, 0, 0)
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
