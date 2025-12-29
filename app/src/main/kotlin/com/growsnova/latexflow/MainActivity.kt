package com.growsnova.latexflow

import android.os.Bundle
import kotlinx.coroutines.*
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.growsnova.latexflow.bluetooth.HidKeyboardManager
import com.growsnova.latexflow.logic.KeyMapper
import com.growsnova.latexflow.ui.HandwritingCanvas
import com.growsnova.latexflow.data.HistoryRepository

import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import com.growsnova.latexflow.ocr.LocalIinkOcrEngine
import com.growsnova.latexflow.ocr.OcrEngine
import com.growsnova.latexflow.bluetooth.ConnectionStatus
import com.growsnova.latexflow.ui.HandwritingStroke

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                LatexFlowApp()
            }
        }
    }
}

@Composable
fun LatexFlowApp() {
    val context = LocalContext.current
    val hidManager = remember { HidKeyboardManager(context) }
    val keyMapper = remember { KeyMapper() }
    val ocrEngine = remember { LocalIinkOcrEngine(context) }
    val historyRepo = remember { com.growsnova.latexflow.data.HistoryRepository(context) }
    
    var strokes by remember { mutableStateOf(listOf<HandwritingStroke>()) }
    var recognizedLatex by remember { mutableStateOf("") }
    var showHistory by remember { mutableStateOf(false) }
    var showMathKeyboard by remember { mutableStateOf(false) }
    
    val connectionStatus by hidManager.connectionStatus.collectAsState()
    val isConnected = connectionStatus == ConnectionStatus.CONNECTED

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Column {
                // 状态提示横幅
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            when (connectionStatus) {
                                ConnectionStatus.CONNECTED -> Color(0xFFE8F5E9)
                                ConnectionStatus.REGISTERED -> Color(0xFFE3F2FD)
                                ConnectionStatus.CONNECTING -> Color(0xFFFFF3E0)
                                else -> Color(0xFFFFEBEE)
                            }
                        )
                        .padding(vertical = 4.dp),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center
                ) {
                    val statusText = when (connectionStatus) {
                        ConnectionStatus.CONNECTED -> "● 已连接至电脑 (HID Connected)"
                        ConnectionStatus.REGISTERED -> "○ 已就绪，待连接主机 (Ready)"
                        ConnectionStatus.CONNECTING -> "○ 正在尝试连接... (Connecting)"
                        ConnectionStatus.DISCONNECTED -> "○ 未连接 (请在电脑蓝牙中选择 LatexFlow)"
                        ConnectionStatus.ERROR -> "⚠ 蓝牙服务初始化失败"
                    }
                    val statusColor = when (connectionStatus) {
                        ConnectionStatus.CONNECTED -> Color(0xFF2E7D32)
                        ConnectionStatus.REGISTERED -> Color(0xFF1976D2)
                        ConnectionStatus.CONNECTING -> Color(0xFFF57C00)
                        else -> Color(0xFFC62828)
                    }
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor
                    )
                }
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "LatexFlow",
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 16.dp),
                        style = MaterialTheme.typography.headlineSmall
                    )
                    
                    IconButton(onClick = {
                        if (strokes.isNotEmpty()) {
                            strokes = strokes.dropLast(1)
                            recognizedLatex = ocrEngine.recognize(strokes)
                        }
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Undo")
                    }
                    
                    IconButton(onClick = {
                        strokes = emptyList()
                        recognizedLatex = ""
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear")
                    }

                    IconButton(onClick = { showMathKeyboard = !showMathKeyboard }) {
                        Icon(
                            imageVector = if (showMathKeyboard) Icons.Default.KeyboardArrowDown else Icons.Default.Keyboard,
                            contentDescription = "Toggle Keyboard"
                        )
                    }

                    Button(
                        onClick = {
                            // Run injection in a coroutine to avoid blocking UI and allow delays
                            val scope = CoroutineScope(Dispatchers.Main)
                            scope.launch {
                                val actions = keyMapper.mapToActions(recognizedLatex)
                                withContext(Dispatchers.IO) {
                                    actions.forEach { action ->
                                        hidManager.sendKey(action.scanCode, action.modifiers)
                                        delay(50) // Delay between characters
                                    }
                                }
                                historyRepo.addRecord(recognizedLatex)
                            }
                        },
                        modifier = Modifier.padding(end = 16.dp),
                        enabled = recognizedLatex.isNotEmpty()
                    ) {
                        Text("注入 (Inject)")
                    }
                }
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = if (recognizedLatex.isEmpty()) "请开始书写公式..." else "预览: $recognizedLatex",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                
                if (recognizedLatex.isEmpty()) {
                    val recent = historyRepo.getHistory()
                    if (recent.isNotEmpty()) {
                        Text(
                            text = "近期记录: ${recent.first()}",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.Gray,
                            modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp)
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            HandwritingCanvas(
                modifier = Modifier.weight(1f),
                strokes = strokes,
                onStrokesChanged = { newStrokes ->
                    strokes = newStrokes
                    recognizedLatex = ocrEngine.recognize(strokes)
                }
            )
            
            if (showMathKeyboard) {
                com.growsnova.latexflow.ui.MathKeyboard(
                    onSymbolSelected = { symbol ->
                        when (symbol) {
                            "BACKSPACE" -> {
                                if (recognizedLatex.isNotEmpty()) {
                                    recognizedLatex = recognizedLatex.dropLast(1)
                                }
                            }
                            "ENTER" -> {
                                // Maybe trigger injection?
                            }
                            "LEFT", "RIGHT" -> {
                                // Handle cursor movement if we had a TextField
                            }
                            else -> recognizedLatex += symbol
                        }
                    }
                )
            }
        }
    }
}
