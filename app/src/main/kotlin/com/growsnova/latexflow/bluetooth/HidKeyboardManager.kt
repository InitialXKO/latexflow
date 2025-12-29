package com.growsnova.latexflow.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.Executors

/**
 * 蓝牙连接状态枚举
 */
enum class ConnectionStatus {
    UNAVAILABLE,    // 蓝牙不可用
    REGISTERING,    // 正在注册服务
    DISCONNECTED,   // 已就绪但未连接
    CONNECTING,     // 正在尝试连接主机
    CONNECTED,      // 已连接
    ERROR           // 发生错误
}

class HidKeyboardManager(private val context: Context) {
    private val TAG = "HidKeyboardManager"
    private var hidDevice: BluetoothHidDevice? = null
    private var connectedDevice: BluetoothDevice? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    private val _connectionStatus = MutableStateFlow(ConnectionStatus.UNAVAILABLE)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()



    // 标准键盘 HID Report Map
    private val REPORT_MAP = byteArrayOf(
        0x05.toByte(), 0x01.toByte(), // Usage Page (Generic Desktop)
        0x09.toByte(), 0x06.toByte(), // Usage (Keyboard)
        0xA1.toByte(), 0x01.toByte(), // Collection (Application)
        0x85.toByte(), 0x01.toByte(), // Report Id (1)
        0x05.toByte(), 0x07.toByte(), // Usage Page (Key Codes)
        0x19.toByte(), 0xE0.toByte(), // Usage Minimum (224)
        0x29.toByte(), 0xE7.toByte(), // Usage Maximum (231)
        0x15.toByte(), 0x00.toByte(), // Logical Minimum (0)
        0x25.toByte(), 0x01.toByte(), // Logical Maximum (1)
        0x75.toByte(), 0x01.toByte(), // Report Size (1)
        0x95.toByte(), 0x08.toByte(), // Report Count (8)
        0x81.toByte(), 0x02.toByte(), // Input (Data, Variable, Absolute) ; Modifier byte
        0x95.toByte(), 0x01.toByte(), // Report Count (1)
        0x75.toByte(), 0x08.toByte(), // Report Size (8)
        0x81.toByte(), 0x01.toByte(), // Input (Constant) ; Reserved byte
        0x95.toByte(), 0x05.toByte(), // Report Count (5)
        0x75.toByte(), 0x01.toByte(), // Report Size (1)
        0x05.toByte(), 0x08.toByte(), // Usage Page (LEDs)
        0x19.toByte(), 0x01.toByte(), // Usage Minimum (1)
        0x29.toByte(), 0x05.toByte(), // Usage Maximum (5)
        0x91.toByte(), 0x02.toByte(), // Output (Data, Variable, Absolute) ; LED report
        0x95.toByte(), 0x01.toByte(), // Report Count (1)
        0x75.toByte(), 0x03.toByte(), // Report Size (3)
        0x91.toByte(), 0x01.toByte(), // Output (Constant) ; LED report padding
        0x95.toByte(), 0x06.toByte(), // Report Count (6)
        0x75.toByte(), 0x08.toByte(), // Report Size (8)
        0x15.toByte(), 0x00.toByte(), // Logical Minimum (0)
        0x25.toByte(), 0x65.toByte(), // Logical Maximum (101)
        0x05.toByte(), 0x07.toByte(), // Usage Page (Key Codes)
        0x19.toByte(), 0x00.toByte(), // Usage Minimum (0)
        0x29.toByte(), 0x65.toByte(), // Usage Maximum (101)
        0x81.toByte(), 0x00.toByte(), // Input (Data, Array) ; Key codes
        0xC0.toByte()                 // End Collection
    )

    private val sdpSettings = BluetoothHidDeviceAppSdpSettings(
        "LatexFlow Keyboard",
        "Android HID Keyboard",
        "LatexFlow",
        BluetoothHidDevice.SUBCLASS1_KEYBOARD,
        REPORT_MAP
    )

    private val callback = object : BluetoothHidDevice.Callback() {
        override fun onAppStatusChanged(device: BluetoothDevice?, registered: Boolean) {
            Log.d(TAG, "onAppStatusChanged: registered=$registered")
            if (registered) {
                _connectionStatus.value = ConnectionStatus.REGISTERED
            } else {
                _connectionStatus.value = ConnectionStatus.DISCONNECTED
            }
        }

        override fun onConnectionStateChanged(device: BluetoothDevice?, state: Int) {
            Log.d(TAG, "onConnectionStateChanged: device=$device state=$state")
            if (state == BluetoothProfile.STATE_CONNECTED) {
                connectedDevice = device
                _connectionStatus.value = ConnectionStatus.CONNECTED
            } else if (state == BluetoothProfile.STATE_DISCONNECTED) {
                connectedDevice = null
                _connectionStatus.value = ConnectionStatus.REGISTERED // 回退到已注册状态
            } else if (state == BluetoothProfile.STATE_CONNECTING) {
                _connectionStatus.value = ConnectionStatus.CONNECTING
            }
        }
    }

    private var pendingRegistration = false

    fun register() {
        pendingRegistration = true
        _connectionStatus.value = ConnectionStatus.REGISTERING
        if (hidDevice != null) {
            doRegister()
        }
    }

    @SuppressLint("MissingPermission")
    private fun doRegister() {
        try {
            // 先尝试注销以清理旧状态
            try { hidDevice?.unregisterApp() } catch (e: Exception) {}
            
            hidDevice?.registerApp(
                sdpSettings,
                null,
                null,
                Executors.newSingleThreadExecutor(),
                callback
            )
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException: Missing BLUETOOTH_CONNECT permission", e)
            _connectionStatus.value = ConnectionStatus.ERROR
        } catch (e: Exception) {
            Log.e(TAG, "Error registering HID app", e)
            _connectionStatus.value = ConnectionStatus.ERROR
        }
    }

    init {
        val adapter = BluetoothAdapter.getDefaultAdapter()
        adapter?.getProfileProxy(context, object : BluetoothProfile.ServiceListener {
            override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
                if (profile == BluetoothProfile.HID_DEVICE) {
                    hidDevice = proxy as BluetoothHidDevice
                    if (pendingRegistration) {
                        doRegister()
                    }
                }
            }
            override fun onServiceDisconnected(profile: Int) {
                if (profile == BluetoothProfile.HID_DEVICE) {
                    hidDevice = null
                }
            }
        }, BluetoothProfile.HID_DEVICE)
    }

    /**
     * 发送按键事件
     */
    @SuppressLint("MissingPermission")
    fun sendKey(scanCode: Int, modifiers: Int = 0) {
        val device = connectedDevice ?: return
        val hid = hidDevice ?: return

        // 按下
        val reportDown = ByteArray(8)
        reportDown[0] = modifiers.toByte()
        reportDown[2] = scanCode.toByte()
        hid.sendReport(device, 1, reportDown)

        // Delay to ensure the host registers the "down" state before the "up" state
        try { Thread.sleep(15) } catch (e: Exception) {}

        // 抬起
        val reportUp = ByteArray(8)
        hid.sendReport(device, 1, reportUp)
        
        // Short delay after "up" to prevent sequence collisions
        try { Thread.sleep(5) } catch (e: Exception) {}
    }
    /**
     * 主动发起连接 (增加重试逻辑)
     */
    @SuppressLint("MissingPermission")
    fun connect(device: BluetoothDevice) {
        scope.launch {
            var retry = 0
            while (retry < 3) {
                _connectionStatus.value = ConnectionStatus.CONNECTING
                val success = try {
                    hidDevice?.connect(device) ?: false
                } catch (e: Exception) {
                    false
                }
                
                if (success) return@launch
                
                retry++
                delay(2000)
            }
            _connectionStatus.value = ConnectionStatus.DISCONNECTED
        }
    }

    @SuppressLint("MissingPermission")
    fun getPairedDevices(): Set<BluetoothDevice> {
        return BluetoothAdapter.getDefaultAdapter()?.bondedDevices ?: emptySet()
    }
}
