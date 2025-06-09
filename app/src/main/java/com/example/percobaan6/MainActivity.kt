package com.example.percobaan6

import android.Manifest
import android.bluetooth.*
import android.content.pm.PackageManager
import android.os.*
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.example.percobaan6.databinding.ActivityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val deviceAddress = "8C:4F:00:3C:99:CE" // Ganti MAC ESP32
    private val serviceUUID = UUID.fromString("12345678-1234-1234-1234-1234567890ab")
    private val characteristicUUID = UUID.fromString("abcd1234-ab12-cd34-ef56-abcdef123456")
    private var bluetoothGatt: BluetoothGatt? = null

    private val phoneNumber = "+6281234567890"
    private var retryCount = 0
    private val maxRetries = 5
    private val reconnectDelay = 3000L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications, R.id.navigation_history
            )
        )
        navView.setupWithNavController(navController)

        // Inisialisasi koneksi BLE
        requestBluetoothPermissions {
            toast("Izin diberikan, menghubungkan BLE...")
            connectToBLE()
        }
    }

    private fun requestBluetoothPermissions(onGranted: () -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
        ) {
            val permissionLauncher =
                registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                    if (permissions.all { it.value }) {
                        onGranted()
                    } else {
                        toast("Izin Bluetooth ditolak")
                    }
                }

            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            )
        } else {
            onGranted()
        }
    }

    private fun connectToBLE() {
        val device = bluetoothAdapter?.getRemoteDevice(deviceAddress)
        if (device == null) {
            toast("Perangkat tidak ditemukan")
            return
        }

        bluetoothGatt?.close()
        bluetoothGatt = device.connectGatt(this, false, gattCallback)
        toast("Menghubungkan ke perangkat BLE...")
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    retryCount = 0
                    runOnUiThread { toast("Terkoneksi ke BLE, mencari layanan...") }
                    gatt?.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    runOnUiThread { toast("BLE terputus. Coba lagi...") }
                    bluetoothGatt?.close()
                    bluetoothGatt = null
                    if (retryCount++ < maxRetries) {
                        Handler(Looper.getMainLooper()).postDelayed({ connectToBLE() }, reconnectDelay)
                    } else {
                        toast("Gagal reconnect setelah $maxRetries kali")
                    }
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            val charac = gatt?.getService(serviceUUID)?.getCharacteristic(characteristicUUID)
            if (charac != null) {
                gatt.setCharacteristicNotification(charac, true)
                val descriptor = charac.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
                descriptor?.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                gatt.writeDescriptor(descriptor)
                toast("Notifikasi BLE diaktifkan")
            } else {
                toast("Karakteristik BLE tidak ditemukan")
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
            val value = characteristic?.getStringValue(0)
            value?.let { handleSensorData(it) }
        }
    }

    private fun handleSensorData(line: String) {
        val parts = line.split(",")
        println("Data masuk: $line")
        if (parts.size == 4) {
            try {
                val vitals = Vitals(parts[1].toInt(), parts[2].toInt())
                val measurement = Measurement(
                    vitals.bpm, 80,
                    vitals.spo2, 95
                )
                if (!measurement.isValid()) {
                    alert("Data tidak valid! BPM Error: %.2f%%, SpO₂ Error: %.2f%%".format(
                        measurement.bpmError(), measurement.spo2Error()
                    ))
                    return
                }

                val result = AsthmaDetector.detect(vitals)

                runOnUiThread {
                    val statusText = when (result) {
                        AsthmaLevel.SEVERE, AsthmaLevel.MODERATE -> {
                            println("Buzzer ON")
                            "⚠️ Asma ${result.name.lowercase()} terdeteksi!"
                        }
                        AsthmaLevel.NORMAL -> {
                            alert("Kondisi normal. BPM: ${vitals.bpm}, SpO₂: ${vitals.spo2}%")
                            "✅ Kondisi normal"
                        }
                    }
                    Toast.makeText(this, statusText, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
               // toast("Error parsing data BLE")
            }
        }
    }

    private fun alert(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }

    private fun toast(msg: String) {
        runOnUiThread {
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        bluetoothGatt?.close()
        bluetoothGatt = null
        super.onDestroy()
    }
}
