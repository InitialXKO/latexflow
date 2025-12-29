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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.growsnova.latexflow.bluetooth.HidKeyboardManager
import com.growsnova.latexflow.logic.KeyMapper
import com.growsnova.latexflow.ui.HandwritingCanvas
import com.growsnova.latexflow.ui.LatexView
import com.growsnova.latexflow.data.HistoryRepository

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.HorizontalDivider
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
    
    val scope = rememberCoroutineScope()
    
    // 1. Runtime Permissions (Android 12+)
    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val connectGranted = perms[android.Manifest.permission.BLUETOOTH_CONNECT] == true
        if (connectGranted) {
            hidManager.register()
        }
    }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val hasConnect = androidx.core.content.ContextCompat.checkSelfPermission(
                context, 
                android.Manifest.permission.BLUETOOTH_CONNECT
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            
            if (hasConnect) {
                hidManager.register()
            } else {
                permissionLauncher.launch(arrayOf(
                    android.Manifest.permission.BLUETOOTH_CONNECT,
                    android.Manifest.permission.BLUETOOTH_ADVERTISE
                ))
            }
        } else {
            hidManager.register()
        }
    }
    
    var strokes by remember { mutableStateOf(listOf<HandwritingStroke>()) }
    var recognizedLatex by remember { mutableStateOf("") }
    var showHistory by remember { mutableStateOf(false) }
    var showMathKeyboard by remember { mutableStateOf(false) }
    
    val connectionStatus by hidManager.connectionStatus.collectAsState()
    val isConnected = connectionStatus == ConnectionStatus.CONNECTED

    // 2. Bluetooth Guide Dialog
    var showBluetoothGuide by remember { mutableStateOf(false) }
    
    if (showBluetoothGuide || connectionStatus == ConnectionStatus.UNAVAILABLE) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showBluetoothGuide = false },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    val intent = android.content.Intent(android.bluetooth.BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
                        putExtra(android.bluetooth.BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
                    }
                    context.startActivity(intent)
                    showBluetoothGuide = false
                }) { Text("开启被发现 (Make Discoverable)") }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showBluetoothGuide = false }) {
                    Text("取消")
                }
            },
            title = { Text(if (connectionStatus == ConnectionStatus.UNAVAILABLE) "蓝牙未开启" else "连接电脑指引") },
            text = { 
                Text(
                    if (connectionStatus == ConnectionStatus.UNAVAILABLE) 
                        "请先在系统设置中开启蓝牙，然后返回应用。" 
                    else 
                        "1. 点击下方按钮使手机可被发现。\n" +
                        "2. 在电脑蓝牙设置中搜索 'LatexFlow Keyboard'。\n" +
                        "3. 点击连接即可开始注入公式。"
                ) 
            }
        )
    }

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
                                ConnectionStatus.CONNECTING -> Color(0xFFFFF3E0)
                                ConnectionStatus.DISCONNECTED -> Color(0xFFE3F2FD)
                                ConnectionStatus.REGISTERED -> Color(0xFFE3F2FD)
                                ConnectionStatus.REGISTERING -> Color(0xFFF3E5F5)
                                else -> Color(0xFFFFEBEE)
                            }
                        )
                        .clickable { showBluetoothGuide = true }
                        .padding(vertical = 6.dp),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center
                ) {
                    val statusText = when (connectionStatus) {
                        ConnectionStatus.CONNECTED -> "● 已连接至电脑"
                        ConnectionStatus.CONNECTING -> "○ 正在尝试连接..."
                        ConnectionStatus.DISCONNECTED -> "○ 已就绪，等待电脑连接"
                        ConnectionStatus.REGISTERED -> "○ 已就绪，等待电脑连接"
                        ConnectionStatus.REGISTERING -> "○ 正在初始化服务..."
                        ConnectionStatus.UNAVAILABLE -> "⚠ 蓝牙不可用"
                        ConnectionStatus.ERROR -> "⚠ 蓝牙初始化失败"
                    }
                    val statusColor = when (connectionStatus) {
                        ConnectionStatus.CONNECTED -> Color(0xFF2E7D32)
                        ConnectionStatus.CONNECTING -> Color(0xFFF57C00)
                        ConnectionStatus.DISCONNECTED -> Color(0xFF1976D2)
                        ConnectionStatus.REGISTERED -> Color(0xFF1976D2)
                        ConnectionStatus.REGISTERING -> Color(0xFF7B1FA2)
                        else -> Color(0xFFC62828)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelSmall,
                            color = statusColor
                        )
                        if (connectionStatus == ConnectionStatus.DISCONNECTED || connectionStatus == ConnectionStatus.REGISTERED) {
                            Text(
                                text = "点击查看连接指引",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                                color = statusColor.copy(alpha = 0.6f)
                            )
                        }
                    }
                }

                if (connectionStatus == ConnectionStatus.DISCONNECTED || connectionStatus == ConnectionStatus.CONNECTING) {
                    // Show paired devices to connect to
                    val pairedDevices = remember(connectionStatus) { hidManager.getPairedDevices().toList() }
                    if (pairedDevices.isNotEmpty()) {
                         androidx.compose.foundation.lazy.LazyRow(
                             modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                             horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
                         ) {
                             items(pairedDevices.size) { index ->
                                 val device = pairedDevices[index]
                                 // Simple name formatting
                                 val name = try { device.name ?: "Unknown" } catch(e: SecurityException) { "Unknown" }
                                 androidx.compose.material3.OutlinedButton(
                                     onClick = { hidManager.connect(device) },
                                     modifier = Modifier.padding(top = 4.dp)
                                 ) {
                                     Text("连接 (Connect): $name")
                                 }
                             }
                         }
                    }
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

                    if (!isConnected) {
                        androidx.compose.material3.TextButton(onClick = {
                            val intent = android.content.Intent(android.bluetooth.BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
                                putExtra(android.bluetooth.BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
                            }
                            context.startActivity(intent)
                        }) {
                            Text("被发现 (Make Discoverable)")
                        }
                    }
                    
                    IconButton(onClick = {
                        if (strokes.isNotEmpty()) {
                            strokes = strokes.dropLast(1)
                            scope.launch {
                                recognizedLatex = ocrEngine.recognize(strokes)
                            }
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
                            // Run injection in a coroutine to avoid blocking UI
                            scope.launch {
                                val actions = keyMapper.mapToActions(recognizedLatex)
                                withContext(Dispatchers.IO) {
                                    actions.forEach { action ->
                                        hidManager.sendKey(action.scanCode, action.modifiers)
                                        // 增加动态延迟以提高稳定性
                                        delay(if (action.scanCode == 0x28) 100L else 30L) 
                                    }
                                }
                                historyRepo.addRecord(recognizedLatex)
                            }
                        },
                        modifier = Modifier.padding(end = 16.dp),
                        enabled = recognizedLatex.isNotEmpty() && 
                                  !recognizedLatex.startsWith("Error:") && 
                                  !recognizedLatex.startsWith("OCR Error:") && 
                                  isConnected
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
                    .background(Color.White)
                    .padding(16.dp)
            ) {
                if (recognizedLatex.isNotEmpty()) {
                    val isError = recognizedLatex.startsWith("Error:") || recognizedLatex.startsWith("OCR Error:")
                    
                    if (isError) {
                        Text(
                            text = "识别错误 (Error):",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.Red,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Text(
                            text = recognizedLatex,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Red,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    } else {
                        Text(
                            text = "渲染预览 (Rendered):",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        LatexView(
                            latex = recognizedLatex,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 60.dp)
                                .padding(bottom = 12.dp)
                        )
                        
                        androidx.compose.material3.HorizontalDivider(
                            modifier = Modifier.padding(bottom = 8.dp),
                            thickness = 0.5.dp,
                            color = Color.LightGray
                        )
                        
                        Text(
                            text = "实际输出预览 (Raw Output):",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Text(
                            text = recognizedLatex,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            ),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                } else {
                    Text(
                        text = "请开始书写公式...",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    
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
                    // 3. Fix disappearing strokes: Async OCR
                    scope.launch {
                        recognizedLatex = ocrEngine.recognize(strokes)
                    }
                }
            )
            
            if (showMathKeyboard) {
                com.growsnova.latexflow.ui.MathKeyboard(
                    onSymbolSelected = { symbol ->
                        when (symbol) {
                            "BACKSPACE" -> {
                                if (recognizedLatex.isNotEmpty()) {
                                    if (recognizedLatex.startsWith("Error:") || recognizedLatex.startsWith("OCR Error:")) {
                                        recognizedLatex = ""
                                    } else {
                                        recognizedLatex = recognizedLatex.dropLast(1)
                                    }
                                }
                            }
                            "ENTER" -> {
                                // Maybe trigger injection?
                            }
                            "LEFT", "RIGHT" -> {
                                // Handle cursor movement if we had a TextField
                            }
                            else -> {
                                if (recognizedLatex.startsWith("Error:") || recognizedLatex.startsWith("OCR Error:")) {
                                    recognizedLatex = symbol
                                } else {
                                    recognizedLatex += symbol
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}
