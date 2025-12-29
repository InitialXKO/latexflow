package com.growsnova.latexflow.ocr

import com.growsnova.latexflow.ui.HandwritingStroke
import android.util.Log

/**
 * 手写识别引擎接口
 */
interface OcrEngine {
    /**
     * 将笔迹数据转换为 LaTeX 字符串
     */
    suspend fun recognize(strokes: List<HandwritingStroke>): String
}


/**
 * 真实的基于 Local MyScript iink 的 OCR 引擎
 */


/**
 * 本地 MyScript iink OCR 引擎
 */
class LocalIinkOcrEngine(private val context: android.content.Context) : OcrEngine {
    private var engine: com.myscript.iink.Engine? = null
    private var editor: com.myscript.iink.Editor? = null
    private var package_: com.myscript.iink.ContentPackage? = null
    private var part: com.myscript.iink.ContentPart? = null

    init {
        try {
            // 初始化 Engine
            val certificate = com.growsnova.latexflow.MyCertificate.getBytes()
            engine = com.myscript.iink.Engine.create(certificate)
            
            // 配置 Engine (设置资产目录等)
            val conf = engine?.configuration
            val confDir = "zip://" + context.packageCodePath + "!/assets/conf"
            conf?.setStringArray("configuration-manager.search-path", arrayOf(confDir))
            conf?.setBoolean("math.solver.enable", false) // 仅识别，不求解
            
            // 创建并打开一个空的 ContentPackage
            val packageFile = java.io.File(context.filesDir, "math.iink")
            val safeEngine = engine
            if (safeEngine != null) {
                val contentPackage = safeEngine.createPackage(packageFile)
                package_ = contentPackage
                val contentPart = contentPackage.createPart("Math")
                part = contentPart
                
                // 创建 Editor
                val renderer = safeEngine.createRenderer(context.resources.displayMetrics.xdpi, context.resources.displayMetrics.ydpi, null)
                if (renderer != null) {
                    val safeEditor = safeEngine.createEditor(renderer)
                    editor = safeEditor
                    safeEditor.part = contentPart
                }
            }
        } catch (e: Exception) {
            Log.e("LocalIinkOcrEngine", "Failed to initialize iink engine", e)
        }
    }

    override suspend fun recognize(strokes: List<HandwritingStroke>): String = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
        val editor = editor ?: return@withContext "Error: Engine not init"
        if (strokes.isEmpty()) return@withContext ""

        synchronized(this) { 
            try {
                // 清空当前内容
                editor.clear()
    
                // 将 HandwritingStroke 转换为 iink PointerEvents 并添加
                strokes.forEach { stroke ->
                    val points = stroke.points
                    val timestamps = stroke.timestamps
                    
                    if (points.isNotEmpty()) {
                        // Start stroke
                        var t = if (timestamps.isNotEmpty()) timestamps[0] else System.currentTimeMillis()
                        editor.pointerDown(points[0].x, points[0].y, t, 0.5f, com.myscript.iink.PointerType.PEN, stroke.hashCode())
                        
                        // Move
                        for (i in 1 until points.size) {
                            val nextT = if (i < timestamps.size) timestamps[i] else t + 1
                            // Ensure strictly increasing time if fallback loop runs fast
                            t = if (nextT > t) nextT else t + 1
                            editor.pointerMove(points[i].x, points[i].y, t, 0.5f, com.myscript.iink.PointerType.PEN, stroke.hashCode())
                        }
                        
                        // End stroke
                        val lastIndex = points.size - 1
                        val lastT = if (lastIndex < timestamps.size) timestamps[lastIndex] else t
                        val finalT = if (lastT >= t) lastT else t
                        editor.pointerUp(points.last().x, points.last().y, finalT, 0.5f, com.myscript.iink.PointerType.PEN, stroke.hashCode())
                    }
                }
    
                // 获取识别结果 (LaTeX)
                return@withContext editor.export_(null, com.myscript.iink.MimeType.LATEX)
            } catch (e: Exception) {
                Log.e("LocalIinkOcrEngine", "OCR Error", e)
                return@withContext "OCR Error: ${e.toString()}"
            }
        }
    }
}
