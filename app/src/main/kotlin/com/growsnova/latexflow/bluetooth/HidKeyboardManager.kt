package com.growsnova.latexflow.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.Executors

/**
 * 蓝牙连接状态枚举
 */
enum class ConnectionStatus {
    DISCONNECTED, // 未连接
    CONNECTING,   // 正在连接 
    CONNECTED,    // 蓝牙已连接
    REGISTERED,   // HID 应用已注册且可用
    ERROR         // 出错
}

/**
 * 蓝牙 HID 设备管理器
 */
class HidKeyboardManager(private val context: Context) {
    private val TAG = "HidKeyboardManager"
    private var hidDevice: BluetoothHidDevice? = null
    private var connectedDevice: BluetoothDevice? = null
    
    private val _connectionStatus = MutableStateFlow(ConnectionStatus.DISCONNECTED)
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

    init {
        val adapter = BluetoothAdapter.getDefaultAdapter()
        adapter?.getProfileProxy(context, object : BluetoothProfile.ServiceListener {
            override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
                if (profile == BluetoothProfile.HID_DEVICE) {
                    hidDevice = proxy as BluetoothHidDevice
                    registerApp()
                }
            }

            override fun onServiceDisconnected(profile: Int) {
                if (profile == BluetoothProfile.HID_DEVICE) {
                    hidDevice = null
                }
            }
        }, BluetoothProfile.HID_DEVICE)
    }

    @SuppressLint("MissingPermission")
    private fun registerApp() {
        hidDevice?.registerApp(
            sdpSettings,
            null,
            null,
            Executors.newSingleThreadExecutor(),
            callback
        )
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
}
