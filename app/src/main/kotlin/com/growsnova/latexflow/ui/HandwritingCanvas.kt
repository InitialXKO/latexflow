package com.growsnova.latexflow.ui

import androidx.compose.runtime.Composable

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput

/**
 * 笔迹数据模型
 */
data class HandwritingStroke(
    val points: List<Offset> = emptyList(),
    val color: Color = Color.Black,
    val width: Float = 5f,
    var path: Path? = null // Caching the Path object
)

/**
 * 手写画布组件
 */
@Composable
fun HandwritingCanvas(
    modifier: Modifier = Modifier,
    strokes: List<HandwritingStroke>,
    onStrokesChanged: (List<HandwritingStroke>) -> Unit
) {
    // Optimization: Use a mutable list for points to avoid frequent allocations
    val currentPoints = remember { mutableStateListOf<Offset>() }
    var currentPath by remember { mutableStateOf<Path?>(null) }

    Box(modifier = modifier.fillMaxSize().background(Color.White)) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val position = event.changes.first().position
                            
                            when (event.type) {
                                androidx.compose.ui.input.pointer.PointerEventType.Press -> {
                                    currentPoints.clear()
                                    currentPoints.add(position)
                                    currentPath = Path().apply { moveTo(position.x, position.y) }
                                }
                                androidx.compose.ui.input.pointer.PointerEventType.Move -> {
                                    val previous = currentPoints.lastOrNull()
                                    currentPoints.add(position)
                                    if (previous != null) {
                                        val midX = (previous.x + position.x) / 2
                                        val midY = (previous.y + position.y) / 2
                                        currentPath?.quadraticBezierTo(previous.x, previous.y, midX, midY)
                                    }
                                }
                                androidx.compose.ui.input.pointer.PointerEventType.Release -> {
                                    if (currentPoints.isNotEmpty()) {
                                        val finalPoints = currentPoints.toList()
                                        // Finalize the path for the released stroke
                                        val finalPath = createPath(finalPoints) 
                                        val newStroke = HandwritingStroke(points = finalPoints).apply {
                                            path = finalPath
                                        }
                                        onStrokesChanged(strokes + newStroke)
                                        currentPoints.clear()
                                        currentPath = null
                                    }
                                }
                            }
                        }
                    }
                }
        ) {
            // 绘制已完成的笔迹
            strokes.forEach { stroke ->
                val path = stroke.path ?: createPath(stroke.points).also { stroke.path = it }
                drawPath(
                    path = path,
                    color = stroke.color,
                    style = Stroke(
                        width = stroke.width,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }

            // 绘制当前正在书写的笔迹 (使用生成的 cached path)
            currentPath?.let {
                drawPath(
                    path = it,
                    color = Color.Black,
                    style = Stroke(
                        width = 5f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }
    }
}

/**
 * 创建平滑的笔迹路径 (贝塞尔曲线)
 */
fun createPath(points: List<Offset>): Path {
    val path = Path()
    if (points.isEmpty()) return path
    
    val firstPoint = points.first()
    path.moveTo(firstPoint.x, firstPoint.y)
    
    if (points.size == 1) {
        // 处理“点”
        path.lineTo(firstPoint.x + 0.1f, firstPoint.y + 0.1f)
    } else {
        for (i in 1 until points.size) {
            val prev = points[i - 1]
            val curr = points[i]
            val midX = (prev.x + curr.x) / 2
            val midY = (prev.y + curr.y) / 2
            if (i == 1) {
                path.lineTo(midX, midY)
            } else {
                path.quadraticBezierTo(prev.x, prev.y, midX, midY)
            }
        }
        val last = points.last()
        path.lineTo(last.x, last.y)
    }
    return path
}
