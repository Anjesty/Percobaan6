package com.example.percobaan6

import android.Manifest
import android.bluetooth.*
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.*
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.example.percobaan6.databinding.ActivityMainBinding
import com.example.percobaan6.ui.home.HomeViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.util.*
// Imports for SoundPlayer
import android.media.AudioManager
import android.media.MediaPlayer
// Imports for PhoneAndSmsManager
import android.net.Uri
import android.telephony.SmsManager // NEW IMPORT for automatic SMS sending // For showing toasts
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import kotlinx.coroutines.suspendCancellableCoroutine
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlin.coroutines.resume

class SoundPlayer(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    private val audioManager: AudioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    fun setMaxVolume() {
        audioManager.setStreamVolume(
            AudioManager.STREAM_MUSIC,
            audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC),
            0
        )
    }

    fun playSound(rawResId: Int) {
        stopSound()
        mediaPlayer = MediaPlayer.create(context, rawResId)
        mediaPlayer?.setOnCompletionListener { mp ->
            mp.release()
            mediaPlayer = null
        }
        mediaPlayer?.setOnErrorListener { mp, what, extra ->
            mp.release()
            mediaPlayer = null
            false
        }
        mediaPlayer?.start()
    }

    fun stopSound() {
        mediaPlayer?.apply {
            if (isPlaying) {
                stop()
            }
            release()
        }
        mediaPlayer = null
    }

    fun release() {
        stopSound()
    }
}
class LocationManager(
    private val context: Context,
    private val fusedLocationProviderClient: FusedLocationProviderClient
) {
    suspend fun getLocation(): Location? {
        val hasGrantedFineLocationPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasGrantedCoarseLocationPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val systemLocationManager = context.getSystemService(
            Context.LOCATION_SERVICE
        ) as LocationManager

        val isGpsEnabled = systemLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ||
                systemLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)

        if (!isGpsEnabled && !(hasGrantedCoarseLocationPermission || hasGrantedFineLocationPermission)) {
            return null
        }

        return suspendCancellableCoroutine { cont ->
            fusedLocationProviderClient.lastLocation.apply {
                if (isComplete) {
                    if (isSuccessful) {
                        cont.resume(result)
                    } else {
                        cont.resume(null)
                    }
                    return@suspendCancellableCoroutine
                }
                addOnSuccessListener {
                    cont.resume(result)
                }
                addOnFailureListener {
                    cont.resume(null)
                }
                addOnCanceledListener {
                    cont.cancel()
                }
            }
        }
    }
}
class PhoneAndSmsManager(private val context: Context) {
    // MODIFIED: This function now sends SMS automatically without launching a messaging app.
    // Ensure the SEND_SMS permission is granted.
    fun sendSms(phoneNumber: String, message: String) {
        // This check is good practice, though permission should be handled by your UI
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
            try {
                // Get the default SmsManager instance
                val smsManager: SmsManager = context.getSystemService(SmsManager::class.java)

                // You can divide the message if it's long, and send it in parts.
                // For simplicity here, we send a single text message.
                // For robust error handling (delivery reports), you'd use PendingIntents for sentIntent and deliveryIntent.
                smsManager.sendTextMessage(phoneNumber, null, message, null, null)

                Toast.makeText(context, "SMS sent automatically to $phoneNumber", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to send SMS automatically: ${e.message}", Toast.LENGTH_LONG).show()
                e.printStackTrace() // Print stack trace to logcat for debugging
            }
        } else {
            Toast.makeText(context, "SMS permission not granted. Cannot send automatically.", Toast.LENGTH_LONG).show()
        }
    }
}
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val deviceAddress = "44:1D:64:F7:A5:9A" // Ganti MAC ESP32
    private val serviceUUID = UUID.fromString("12345678-1234-1234-1234-1234567890ab")
    private val characteristicUUID = UUID.fromString("abcd1234-ab12-cd34-ef56-abcdef123456")
    private var bluetoothGatt: BluetoothGatt? = null

    private var retryCount = 0
    private val maxRetries = 10
    private val reconnectDelay = 3000L

    // Add reference to HomeViewModel to pass sensor data
    private lateinit var homeViewModel: HomeViewModel
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView
        navController = findNavController(R.id.nav_host_fragment_activity_main)
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications, R.id.navigation_history
            )
        )
        navView.setupWithNavController(navController)

        // Initialize ViewModel
        homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)

        // Initialize BLE connection
        requestBluetoothPermissions {
            toast("Permission granted, connecting to BLE...")
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
                        toast("Bluetooth permission denied")
                        // Update ViewModel about connection failure
                        homeViewModel.updateConnectionStatus(false, "Bluetooth permission denied")
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
            toast("Device not found")
            homeViewModel.updateConnectionStatus(false, "Device not found")
            return
        }

        bluetoothGatt?.close()
        bluetoothGatt = device.connectGatt(this, false, gattCallback)
        toast("Connecting to BLE device...")
        homeViewModel.updateConnectionStatus(false, "Connecting to BLE device...")
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    retryCount = 0
                    runOnUiThread {
                        toast("Connected to BLE, discovering services...")
                        homeViewModel.updateConnectionStatus(true, "Connected - Discovering services...")
                    }
                    gatt?.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    runOnUiThread {
                        toast("BLE disconnected. Retrying...")
                        homeViewModel.updateConnectionStatus(false, "BLE disconnected - Retrying...")
                    }
                    bluetoothGatt?.close()
                    bluetoothGatt = null
                    if (retryCount++ < maxRetries) {
                        Handler(Looper.getMainLooper()).postDelayed({ connectToBLE() }, reconnectDelay)
                    } else {
                        runOnUiThread {
                            toast("Failed to reconnect after $maxRetries attempts")
                            homeViewModel.updateConnectionStatus(false, "Connection failed - Using simulation")
                        }
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
                runOnUiThread {
                    toast("BLE notifications enabled")
                    homeViewModel.updateConnectionStatus(true, "Connected - Receiving sensor data")
                }
            } else {
                runOnUiThread {
                    toast("BLE characteristic not found")
                    homeViewModel.updateConnectionStatus(false, "BLE characteristic not found")
                }
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
            val value = characteristic?.getStringValue(0)
            value?.let { handleSensorData(it) }
        }
    }

    private fun handleSensorData(line: String) {
        val parts = line.split(",")
        println("Incoming data: $line")

        if (parts.size == 4) {
            try {
                val heartRate = parts[1].toInt()
                val spo2 = parts[2].toInt()

                val vitals = Vitals(heartRate, spo2)
                val measurement = Measurement(
                    vitals.bpm, 80,
                    vitals.spo2, 95
                )

                if (!measurement.isValid()) {
                    alert("Invalid data! BPM Error: %.2f%%, SpO₂ Error: %.2f%%".format(
                        measurement.bpmError(), measurement.spo2Error()
                    ))
                    return
                }

                // Update HomeViewModel with real sensor data
                runOnUiThread {
                    homeViewModel.updateSensorData(heartRate, spo2)
                }

                val result = AsthmaDetector.detect(vitals)

                runOnUiThread {
                    val statusText = when (result) {
                        AsthmaLevel.SEVERE, AsthmaLevel.MODERATE -> {
                            println("Buzzer ON")
                            "⚠️ Asthma ${result.name.lowercase()} detected!"
                        }
                        AsthmaLevel.NORMAL -> {
                            alert("Normal condition. BPM: ${vitals.bpm}, SpO₂: ${vitals.spo2}%")
                            "✅ Normal condition"
                        }
                    }
                    Toast.makeText(this@MainActivity, statusText, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                println("Error parsing BLE data: ${e.message}")
                runOnUiThread {
                    homeViewModel.updateConnectionStatus(false, "Error parsing sensor data")
                }
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
//package com.example.percobaan6
//
//import android.Manifest
//import android.bluetooth.*
//import android.content.pm.PackageManager
//import android.os.*
//import android.widget.Toast
//import androidx.activity.result.contract.ActivityResultContracts
//import androidx.appcompat.app.AppCompatActivity
//import androidx.core.app.ActivityCompat
//import androidx.navigation.findNavController
//import androidx.navigation.ui.AppBarConfiguration
//import androidx.navigation.ui.setupWithNavController
//import com.example.percobaan6.databinding.ActivityMainBinding
//import com.example.percobaan6.ui.home.HomeViewModel
//import androidx.lifecycle.ViewModelProvider
//import androidx.navigation.NavController
//import com.google.android.material.bottomnavigation.BottomNavigationView
//import java.util.*
//
//class MainActivity : AppCompatActivity() {
//
//    private lateinit var binding: ActivityMainBinding
//    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
//    private val deviceAddress = "8C:4F:00:3C:99:CE"
//    private val serviceUUID = UUID.fromString("12345678-1234-1234-1234-1234567890ab")
//    private val characteristicUUID = UUID.fromString("abcd1234-ab12-cd34-ef56-abcdef123456")
//    private var bluetoothGatt: BluetoothGatt? = null
//
//    private val phoneNumber = "+6281234567890"
//    private var retryCount = 0
//    private val maxRetries = 5
//    private val reconnectDelay = 3000L
//
//    private lateinit var homeViewModel: HomeViewModel
//    private lateinit var navController: NavController
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//
//        binding = ActivityMainBinding.inflate(layoutInflater)
//        setContentView(binding.root)
//
//        val navView: BottomNavigationView = binding.navView
//        navController = findNavController(R.id.nav_host_fragment_activity_main)
//        val appBarConfiguration = AppBarConfiguration(
//            setOf(
//                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications, R.id.navigation_history
//            )
//        )
//        navView.setupWithNavController(navController)
//
//        homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)
//
//        requestBluetoothPermissions {
//            toast("Permission granted, connecting to BLE...")
//            connectToBLE()
//        }
//    }
//
//    private fun requestBluetoothPermissions(onGranted: () -> Unit) {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
//            ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
//        ) {
//            val permissionLauncher =
//                registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
//                    if (permissions.all { it.value }) {
//                        onGranted()
//                    } else {
//                        toast("Bluetooth permission denied")
//                        homeViewModel.updateConnectionStatus(false, "Bluetooth permission denied")
//                    }
//                }
//
//            permissionLauncher.launch(
//                arrayOf(
//                    Manifest.permission.BLUETOOTH_CONNECT,
//                    Manifest.permission.BLUETOOTH_SCAN,
//                    Manifest.permission.ACCESS_FINE_LOCATION
//                )
//            )
//        } else {
//            onGranted()
//        }
//    }
//
//    private fun connectToBLE() {
//        val device = bluetoothAdapter?.getRemoteDevice(deviceAddress)
//        if (device == null) {
//            toast("Device not found")
//            homeViewModel.updateConnectionStatus(false, "Device not found")
//            return
//        }
//
//        bluetoothGatt?.close()
//        bluetoothGatt = device.connectGatt(this, false, gattCallback)
//        toast("Connecting to BLE device...")
//        homeViewModel.updateConnectionStatus(false, "Connecting...")
//    }
//
//    private val gattCallback = object : BluetoothGattCallback() {
//        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
//            when (newState) {
//                BluetoothProfile.STATE_CONNECTED -> {
//                    retryCount = 0
//                    runOnUiThread {
//                        toast("Connected to BLE")
//                        homeViewModel.updateConnectionStatus(true, "Connected - Discovering services...")
//                    }
//                    gatt?.discoverServices()
//                }
//                BluetoothProfile.STATE_DISCONNECTED -> {
//                    runOnUiThread {
//                        toast("BLE disconnected")
//                        homeViewModel.updateConnectionStatus(false, "Disconnected - Retrying...")
//                    }
//                    bluetoothGatt?.close()
//                    bluetoothGatt = null
//                    if (retryCount++ < maxRetries) {
//                        Handler(Looper.getMainLooper()).postDelayed({ connectToBLE() }, reconnectDelay)
//                    } else {
//                        runOnUiThread {
//                            toast("Connection failed")
//                            homeViewModel.updateConnectionStatus(false, "Connection failed - Using simulation")
//                        }
//                    }
//                }
//            }
//        }
//
//        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
//            val charac = gatt?.getService(serviceUUID)?.getCharacteristic(characteristicUUID)
//            if (charac != null) {
//                gatt.setCharacteristicNotification(charac, true)
//                val descriptor = charac.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
//                descriptor?.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
//                gatt.writeDescriptor(descriptor)
//                runOnUiThread {
//                    toast("BLE ready")
//                    homeViewModel.updateConnectionStatus(true, "Connected - Ready for data")
//                }
//            } else {
//                runOnUiThread {
//                    toast("BLE service not found")
//                    homeViewModel.updateConnectionStatus(false, "Service not found")
//                }
//            }
//        }
//
//        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
//            val value = characteristic?.getStringValue(0)
//            value?.let { handleSensorData(it) }
//        }
//    }
//
//    private fun handleSensorData(line: String) {
//        val parts = line.split(",")
//        println("Sensor data: $line")
//
//        if (parts.size >= 3) {
//            try {
//                val heartRate = parts[1].trim().toIntOrNull() ?: return
//                val spo2 = parts[2].trim().toIntOrNull() ?: return
//
//                if (heartRate in 30..200 && spo2 in 70..100) {
//                    runOnUiThread {
//                        homeViewModel.updateSensorData(heartRate, spo2)
//                    }
//
//                    val vitals = Vitals(heartRate, spo2)
//                    val result = AsthmaDetector.detect(vitals)
//
//                    runOnUiThread {
//                        val statusText = when (result) {
//                            AsthmaLevel.SEVERE -> "⚠️ Asthma SEVERE detected! BPM: $heartRate, SpO₂: $spo2%"
//                            AsthmaLevel.MODERATE -> "⚠️ Asthma MODERATE detected! BPM: $heartRate, SpO₂: $spo2%"
//                            AsthmaLevel.NORMAL -> "✅ Normal condition - BPM: $heartRate, SpO₂: $spo2%"
//                        }
//                        if (result != AsthmaLevel.NORMAL) {
//                            Toast.makeText(this@MainActivity, statusText, Toast.LENGTH_LONG).show()
//                        }
//                    }
//                }
//            } catch (e: Exception) {
//                println("Error parsing sensor data: ${e.message}")
//            }
//        }
//    }
//
//    private fun toast(msg: String) {
//        runOnUiThread {
//            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
//        }
//    }
//
//    override fun onDestroy() {
//        bluetoothGatt?.close()
//        bluetoothGatt = null
//        super.onDestroy()
//    }
//}
//package com.example.percobaan6
//
//import android.Manifest
//import android.bluetooth.*
//import android.content.pm.PackageManager
//import android.os.*
//import android.widget.Toast
//import androidx.activity.result.contract.ActivityResultContracts
//import androidx.appcompat.app.AppCompatActivity
//import androidx.core.app.ActivityCompat
//import androidx.navigation.findNavController
//import androidx.navigation.ui.AppBarConfiguration
//import androidx.navigation.ui.setupWithNavController
//import com.example.percobaan6.databinding.ActivityMainBinding
//import com.example.percobaan6.ui.home.HomeViewModel
//import androidx.lifecycle.ViewModelProvider
//import androidx.navigation.NavController
//import com.google.android.material.bottomnavigation.BottomNavigationView
//import java.util.*
//
//class MainActivity : AppCompatActivity() {
//
//    private lateinit var binding: ActivityMainBinding
//    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
//    private val deviceAddress = "8C:4F:00:3C:99:CE" // Ganti MAC ESP32
//    private val serviceUUID = UUID.fromString("12345678-1234-1234-1234-1234567890ab")
//    private val characteristicUUID = UUID.fromString("abcd1234-ab12-cd34-ef56-abcdef123456")
//    private var bluetoothGatt: BluetoothGatt? = null
//
//    private val phoneNumber = "+6281234567890"
//    private var retryCount = 0
//    private val maxRetries = 5
//    private val reconnectDelay = 3000L
//
//    // Add reference to HomeViewModel to pass sensor data
//    private lateinit var homeViewModel: HomeViewModel
//    private lateinit var navController: NavController
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//
//        binding = ActivityMainBinding.inflate(layoutInflater)
//        setContentView(binding.root)
//
//        val navView: BottomNavigationView = binding.navView
//        navController = findNavController(R.id.nav_host_fragment_activity_main)
//        val appBarConfiguration = AppBarConfiguration(
//            setOf(
//                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications, R.id.navigation_history
//            )
//        )
//        navView.setupWithNavController(navController)
//
//        // Initialize ViewModel
//        homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)
//
//        // Initialize BLE connection
//        requestBluetoothPermissions {
//            toast("Permission granted, connecting to BLE...")
//            connectToBLE()
//        }
//    }
//
//    private fun requestBluetoothPermissions(onGranted: () -> Unit) {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
//            ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
//        ) {
//            val permissionLauncher =
//                registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
//                    if (permissions.all { it.value }) {
//                        onGranted()
//                    } else {
//                        toast("Bluetooth permission denied")
//                        // Update ViewModel about connection failure
//                        homeViewModel.updateConnectionStatus(false, "Bluetooth permission denied")
//                    }
//                }
//
//            permissionLauncher.launch(
//                arrayOf(
//                    Manifest.permission.BLUETOOTH_CONNECT,
//                    Manifest.permission.BLUETOOTH_SCAN,
//                    Manifest.permission.ACCESS_FINE_LOCATION
//                )
//            )
//        } else {
//            onGranted()
//        }
//    }
//
//    private fun connectToBLE() {
//        val device = bluetoothAdapter?.getRemoteDevice(deviceAddress)
//        if (device == null) {
//            toast("Device not found")
//            homeViewModel.updateConnectionStatus(false, "Device not found")
//            return
//        }
//
//        bluetoothGatt?.close()
//        bluetoothGatt = device.connectGatt(this, false, gattCallback)
//        toast("Connecting to BLE device...")
//        homeViewModel.updateConnectionStatus(false, "Connecting to BLE device...")
//    }
//
//    private val gattCallback = object : BluetoothGattCallback() {
//        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
//            when (newState) {
//                BluetoothProfile.STATE_CONNECTED -> {
//                    retryCount = 0
//                    runOnUiThread {
//                        toast("Connected to BLE, discovering services...")
//                        homeViewModel.updateConnectionStatus(true, "Connected - Discovering services...")
//                    }
//                    gatt?.discoverServices()
//                }
//                BluetoothProfile.STATE_DISCONNECTED -> {
//                    runOnUiThread {
//                        toast("BLE disconnected. Retrying...")
//                        homeViewModel.updateConnectionStatus(false, "BLE disconnected - Retrying...")
//                    }
//                    bluetoothGatt?.close()
//                    bluetoothGatt = null
//                    if (retryCount++ < maxRetries) {
//                        Handler(Looper.getMainLooper()).postDelayed({ connectToBLE() }, reconnectDelay)
//                    } else {
//                        runOnUiThread {
//                            toast("Failed to reconnect after $maxRetries attempts")
//                            homeViewModel.updateConnectionStatus(false, "Connection failed - Using simulation")
//                        }
//                    }
//                }
//            }
//        }
//
//        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
//            val charac = gatt?.getService(serviceUUID)?.getCharacteristic(characteristicUUID)
//            if (charac != null) {
//                gatt.setCharacteristicNotification(charac, true)
//                val descriptor = charac.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
//                descriptor?.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
//                gatt.writeDescriptor(descriptor)
//                runOnUiThread {
//                    toast("BLE notifications enabled")
//                    homeViewModel.updateConnectionStatus(true, "Connected - Receiving sensor data")
//                }
//            } else {
//                runOnUiThread {
//                    toast("BLE characteristic not found")
//                    homeViewModel.updateConnectionStatus(false, "BLE characteristic not found")
//                }
//            }
//        }
//
//        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
//            val value = characteristic?.getStringValue(0)
//            value?.let { handleSensorData(it) }
//        }
//    }
//
//    private fun handleSensorData(line: String) {
//        val parts = line.split(",")
//        println("Incoming data: $line")
//
//        if (parts.size == 4) {
//            try {
//                val heartRate = parts[1].toInt()
//                val spo2 = parts[2].toInt()
//
//                val vitals = Vitals(heartRate, spo2)
//                val measurement = Measurement(
//                    vitals.bpm, 80,
//                    vitals.spo2, 95
//                )
//
//                if (!measurement.isValid()) {
//                    alert("Invalid data! BPM Error: %.2f%%, SpO₂ Error: %.2f%%".format(
//                        measurement.bpmError(), measurement.spo2Error()
//                    ))
//                    return
//                }
//
//                // Update HomeViewModel with real sensor data
//                runOnUiThread {
//                    homeViewModel.updateSensorData(heartRate, spo2)
//                }
//
//                val result = AsthmaDetector.detect(vitals)
//
//                runOnUiThread {
//                    val statusText = when (result) {
//                        AsthmaLevel.SEVERE, AsthmaLevel.MODERATE -> {
//                            println("Buzzer ON")
//                            "⚠️ Asthma ${result.name.lowercase()} detected!"
//                        }
//                        AsthmaLevel.NORMAL -> {
//                            alert("Normal condition. BPM: ${vitals.bpm}, SpO₂: ${vitals.spo2}%")
//                            "✅ Normal condition"
//                        }
//                    }
//                    Toast.makeText(this@MainActivity, statusText, Toast.LENGTH_LONG).show()
//                }
//            } catch (e: Exception) {
//                println("Error parsing BLE data: ${e.message}")
//                runOnUiThread {
//                    homeViewModel.updateConnectionStatus(false, "Error parsing sensor data")
//                }
//            }
//        }
//    }
//
//    private fun alert(msg: String) {
//        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
//    }
//
//    private fun toast(msg: String) {
//        runOnUiThread {
//            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
//        }
//    }
//
//    override fun onDestroy() {
//        bluetoothGatt?.close()
//        bluetoothGatt = null
//        super.onDestroy()
//    }
//}
//package com.example.percobaan4.ui.home
//
//
//import android.animation.ValueAnimator
//import android.graphics.Color
//import android.os.Bundle
//import android.os.Handler
//import android.os.Looper
//import android.util.Log
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import androidx.fragment.app.Fragment
//import androidx.lifecycle.ViewModelProvider
//import com.example.percobaan6.MainActivity
//import com.example.percobaan6.databinding.FragmentHomeBinding
//import com.github.mikephil.charting.charts.LineChart
//import com.github.mikephil.charting.components.XAxis
//import com.github.mikephil.charting.data.Entry
//import com.github.mikephil.charting.data.LineData
//import com.github.mikephil.charting.data.LineDataSet
//import com.github.mikephil.charting.formatter.ValueFormatter
//import kotlin.random.Random
//
//
//class HomeFragment : Fragment() {
//
//    private var _binding: FragmentHomeBinding? = null
//    private val binding get() = _binding!!
//
//    private lateinit var heartRateChart: LineChart
//    private lateinit var spo2Chart: LineChart
//    private lateinit var breathRateChart: LineChart
//    private lateinit var skinTempChart: LineChart
//    private lateinit var edaChart: LineChart
//
//    private val handler = Handler(Looper.getMainLooper())
//    private val updateInterval = 1000L
//    private var isUpdating = false
//
//    private var heartRateCounter = 0
//    private var spo2Counter = 0
//    private var breathRateCounter = 0
//    private var skinTempCounter = 0
//    private var edaCounter = 0
//
//    private val maxDataPoints = 20
//
//    override fun onCreateView(
//        inflater: LayoutInflater,
//        container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View {
//        val homeViewModel =
//            ViewModelProvider(this).get(HomeViewModel::class.java)
//
//        _binding = FragmentHomeBinding.inflate(inflater, container, false)
//        val root: View = binding.root
//
//        initializeCharts()
//        startRealTimeUpdates()
//
//        // ===================================================
//        // == BLOK INI DIPERBARUI DENGAN LOG DIAGNOSTIK ==
//        // ===================================================
//        binding.btnTestAsmaSuara.setOnClickListener {
//            // Log ini untuk memastikan blok setOnClickListener berjalan
//            Log.d("TAG_ASMA", "Tombol tes di HomeFragment ditekan!")
//
//            // Memanggil fungsi publik yang ada di MainActivity
//            if (activity is MainActivity) {
//                (activity as MainActivity).testAsthmaAlert()
//            } else {
//                Log.e("TAG_ASMA", "Error: Activity bukan instance dari MainActivity!")
//            }
//        }
//        // ===================================================
//
//        return root
//    }
//
//    // ... (sisa kode lainnya di HomeFragment tidak berubah) ...
//
//    private fun initializeCharts() {
//        heartRateChart = binding.heartRateChart
//        spo2Chart = binding.spo2Chart
//        breathRateChart = binding.breathRateChart
//        skinTempChart = binding.skinTempChart
//        edaChart = binding.edaChart
//
//        setupChart(heartRateChart, "Heart Rate (BPM)", Color.rgb(255, 102, 102))
//        setupChart(spo2Chart, "SPO2 (%)", Color.rgb(102, 178, 255))
//        setupChart(breathRateChart, "Breath Rate (BPM)", Color.rgb(102, 255, 178))
//        setupChart(skinTempChart, "Skin Temperature (°C)", Color.rgb(255, 178, 102))
//        setupChart(edaChart, "EDA (μS)", Color.rgb(178, 102, 255))
//    }
//
//    private fun setupChart(chart: LineChart, label: String, color: Int) {
//        chart.apply {
//            description.isEnabled = false
//            setTouchEnabled(false)
//            isDragEnabled = false
//            setScaleEnabled(false)
//            setPinchZoom(false)
//            setDrawGridBackground(false)
//
//            xAxis.apply {
//                position = XAxis.XAxisPosition.BOTTOM
//                setDrawGridLines(false)
//                granularity = 1f
//                labelCount = 5
//                textColor = Color.rgb(62, 44, 129)
//                valueFormatter = object : ValueFormatter() {
//                    override fun getFormattedValue(value: Float): String {
//                        return "${value.toInt()}s"
//                    }
//                }
//            }
//
//            axisLeft.apply {
//                setDrawGridLines(true)
//                gridColor = Color.rgb(230, 230, 230)
//                textColor = Color.rgb(62, 44, 129)
//            }
//
//            axisRight.isEnabled = false
//
//            legend.apply {
//                textColor = Color.rgb(62, 44, 129)
//                textSize = 12f
//            }
//        }
//
//        val entries = ArrayList<Entry>()
//        val dataSet = LineDataSet(entries, label).apply {
//            this.color = color
//            setCircleColor(color)
//            lineWidth = 2f
//            circleRadius = 3f
//            setDrawCircleHole(false)
//            valueTextSize = 0f
//            setDrawFilled(true)
//            fillAlpha = 50
//            fillColor = color
//        }
//
//        chart.data = LineData(dataSet)
//        chart.invalidate()
//    }
//
//    private fun startRealTimeUpdates() {
//        isUpdating = true
//        updateCharts()
//    }
//
//    private fun updateCharts() {
//        if (!isUpdating || _binding == null) return
//
//        updateChartData(heartRateChart, heartRateCounter, generateHeartRateValue())
//        heartRateCounter++
//
//        updateChartData(spo2Chart, spo2Counter, generateSPO2Value())
//        spo2Counter++
//
//        updateChartData(breathRateChart, breathRateCounter, generateBreathRateValue())
//        breathRateCounter++
//
//        updateChartData(skinTempChart, skinTempCounter, generateSkinTempValue())
//        skinTempCounter++
//
//        updateChartData(edaChart, edaCounter, generateEDAValue())
//        edaCounter++
//
//        handler.postDelayed({ updateCharts() }, updateInterval)
//    }
//
//    private fun updateChartData(chart: LineChart, counter: Int, value: Float) {
//        val data = chart.data ?: return
//        val dataSet = data.getDataSetByIndex(0) as LineDataSet
//
//        dataSet.addEntry(Entry(counter.toFloat(), value))
//
//        if (dataSet.entryCount > maxDataPoints) {
//            dataSet.removeFirst()
//
//            for (i in 0 until dataSet.entryCount) {
//                val entry = dataSet.getEntryForIndex(i)
//                entry.x = i.toFloat()
//            }
//        }
//
//        data.notifyDataChanged()
//        chart.notifyDataSetChanged()
//
//        chart.setVisibleXRangeMaximum(maxDataPoints.toFloat())
//        chart.moveViewToX(dataSet.entryCount.toFloat())
//
//        animateChartUpdate(chart)
//    }
//
//    private fun animateChartUpdate(chart: LineChart) {
//        val animator = ValueAnimator.ofFloat(0f, 1f)
//        animator.duration = 300
//        animator.addUpdateListener {
//            chart.invalidate()
//        }
//        animator.start()
//    }
//
//    private fun generateHeartRateValue(): Float {
//        val baseValue = 75f
//        val variation = Random.nextFloat() * 20f - 10f // ±10 BPM variation
//        return (baseValue + variation).coerceIn(50f, 120f)
//    }
//
//    private fun generateSPO2Value(): Float {
//        val baseValue = 98f
//        val variation = Random.nextFloat() * 4f - 2f // ±2% variation
//        return (baseValue + variation).coerceIn(90f, 100f)
//    }
//
//    private fun generateBreathRateValue(): Float {
//        val baseValue = 16f
//        val variation = Random.nextFloat() * 6f - 3f // ±3 BPM variation
//        return (baseValue + variation).coerceIn(10f, 25f)
//    }
//
//    private fun generateSkinTempValue(): Float {
//        val baseValue = 36.5f
//        val variation = Random.nextFloat() * 1f - 0.5f // ±0.5°C variation
//        return (baseValue + variation).coerceIn(35f, 38f)
//    }
//
//    private fun generateEDAValue(): Float {
//        val baseValue = 10f
//        val variation = Random.nextFloat() * 8f - 4f // ±4 μS variation
//        return (baseValue + variation).coerceIn(1f, 25f)
//    }
//
//    override fun onDestroyView() {
//        super.onDestroyView()
//        isUpdating = false
//        handler.removeCallbacksAndMessages(null)
//        _binding = null
//    }
//
//    override fun onPause() {
//        super.onPause()
//        isUpdating = false
//    }
//
//    override fun onResume() {
//        super.onResume()
//        if (_binding != null && !isUpdating) {
//            startRealTimeUpdates()
//        }
//    }
//}
//package com.example.percobaan6
//
//
//import android.Manifest
//import android.annotation.SuppressLint
//import android.bluetooth.*
//import android.content.Context
//import android.content.pm.PackageManager
//import android.media.AudioManager
//import android.os.*
//import android.speech.tts.TextToSpeech
//import android.util.Log
//import android.widget.Toast
//import androidx.activity.result.contract.ActivityResultContracts
//import androidx.appcompat.app.AppCompatActivity
//import androidx.core.app.ActivityCompat
//import androidx.navigation.findNavController
//import androidx.navigation.ui.AppBarConfiguration
//import androidx.navigation.ui.setupWithNavController
//import com.example.percobaan6.databinding.ActivityMainBinding
//import com.google.android.material.bottomnavigation.BottomNavigationView
//import java.util.*
//
//
//class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
//
//    private lateinit var binding: ActivityMainBinding
//    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
//    private val deviceAddress = "8C:4F:00:3C:99:CE" // Ganti MAC ESP32
//    private val serviceUUID = UUID.fromString("12345678-1234-1234-1234-1234567890ab")
//    private val characteristicUUID = UUID.fromString("abcd1234-ab12-cd34-ef56-abcdef123456")
//    private var bluetoothGatt: BluetoothGatt? = null
//
//    private val phoneNumber = "+6281234567890"
//    private var retryCount = 0
//    private val maxRetries = 5
//    private val reconnectDelay = 3000L
//
//    private var tts: TextToSpeech? = null
//    private var isTtsInitialized = false
//
//    // DITAMBAHKAN: Variabel untuk manajer audio
//    private lateinit var audioManager: AudioManager
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        binding = ActivityMainBinding.inflate(layoutInflater)
//        setContentView(binding.root)
//
//        // DITAMBAHKAN: Inisialisasi AudioManager
//        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
//
//        tts = TextToSpeech(this, this)
//        val navView: BottomNavigationView = binding.navView
//        val navController = findNavController(R.id.nav_host_fragment_activity_main)
//        val appBarConfiguration = AppBarConfiguration(
//            setOf(R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications, R.id.navigation_history)
//        )
//        navView.setupWithNavController(navController)
//        requestBluetoothPermissions {
//            toast("Izin diberikan, menghubungkan BLE...")
//            connectToBLE()
//        }
//    }
//
//    override fun onInit(status: Int) {
//        if (status == TextToSpeech.SUCCESS) {
//            val result = tts?.setLanguage(Locale("id", "ID"))
//            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
//                Toast.makeText(this, "Error: Paket Bahasa Indonesia untuk TTS tidak ditemukan.", Toast.LENGTH_LONG).show()
//            } else {
//                isTtsInitialized = true
//                Toast.makeText(this, "Notifikasi Suara Siap Digunakan.", Toast.LENGTH_SHORT).show()
//            }
//        } else {
//            Toast.makeText(this, "FATAL: Inisialisasi Text-to-Speech GAGAL.", Toast.LENGTH_LONG).show()
//        }
//    }
//
//    private fun speak(text: String, queueMode: Int = TextToSpeech.QUEUE_FLUSH) {
//        if (isTtsInitialized) {
//            val params = Bundle()
//            params.putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_ALARM)
//
//            // DITAMBAHKAN: Menambahkan Log untuk melihat status pemutaran suara
//            val result = tts?.speak(text, queueMode, params, null)
//            if (result == TextToSpeech.ERROR) {
//                Log.e("TAG_ASMA", "TTS Error: Gagal saat mencoba memutar suara.")
//            } else {
//                Log.i("TAG_ASMA", "TTS Info: Berhasil memanggil perintah speak untuk teks: $text")
//            }
//        } else {
//            Toast.makeText(this, "TTS belum siap, coba tekan lagi dalam beberapa detik.", Toast.LENGTH_SHORT).show()
//            Log.w("TAG_ASMA", "TTS Warning: Tombol tes ditekan tetapi TTS belum siap.")
//        }
//    }
//
//    // ===================================================
//    // == FUNGSI testAsthmaAlert DIPERBARUI SECARA SIGNIFIKAN ==
//    // ===================================================
//    fun testAsthmaAlert() {
//        Toast.makeText(this, "MEMICU TES SUARA ASMA...", Toast.LENGTH_SHORT).show()
//
//        // LANGKAH 1: Maksimalkan Volume Alarm secara paksa
//        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
//        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0)
//        Log.i("TAG_ASMA", "Volume Alarm diatur ke maksimum ($maxVolume)")
//
//        // LANGKAH 2: Meminta Fokus Audio
//        val focusRequest = AudioManager.OnAudioFocusChangeListener {}
//        val result = audioManager.requestAudioFocus(focusRequest, AudioManager.STREAM_ALARM, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
//
//        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
//            Log.i("TAG_ASMA", "Fokus Audio didapatkan. Memulai suara...")
//            // Jika fokus didapatkan, baru kita panggil handleSensorData untuk memutar suara
//            val fakeBleData = "ID,130,88,CONF"
//            handleSensorData(fakeBleData)
//        } else {
//            Log.e("TAG_ASMA", "Gagal mendapatkan Fokus Audio. Suara tidak bisa diputar.")
//            Toast.makeText(this, "Gagal mendapatkan fokus audio, mungkin ada aplikasi lain yang sedang bersuara.", Toast.LENGTH_LONG).show()
//        }
//    }
//
//    private fun handleSensorData(line: String) {
//        // ... (Fungsi ini tidak perlu diubah, biarkan seperti sebelumnya)
//        val parts = line.split(",")
//        println("Data masuk: $line")
//        if (parts.size == 4) {
//            try {
//                val vitals = Vitals(parts[1].toInt(), parts[2].toInt())
//                val measurement = Measurement(vitals.bpm, 80, vitals.spo2, 95)
//                if (!measurement.isValid()) {
//                    alert("Data tidak valid! BPM Error: %.2f%%, SpO₂ Error: %.2f%%".format(measurement.bpmError(), measurement.spo2Error()))
//                    return
//                }
//                val result = AsthmaDetector.detect(vitals)
//                runOnUiThread {
//                    val statusText = when (result) {
//                        AsthmaLevel.SEVERE, AsthmaLevel.MODERATE -> {
//                            println("Buzzer ON")
//                            speak(getString(R.string.asthma_alert_speech), TextToSpeech.QUEUE_FLUSH)
//                            speak(getString(R.string.asthma_instructions_speech), TextToSpeech.QUEUE_ADD)
//                            "⚠️ Asma ${result.name.lowercase()} terdeteksi!"
//                        }
//                        AsthmaLevel.NORMAL -> {
//                            tts?.stop()
//                            alert("Kondisi normal. BPM: ${vitals.bpm}, SpO₂: ${vitals.spo2}%")
//                            "✅ Kondisi normal"
//                        }
//                    }
//                    Toast.makeText(this, statusText, Toast.LENGTH_LONG).show()
//                }
//            } catch (e: Exception) {
//                // toast("Error parsing data BLE")
//            }
//        }
//    }
//
//    // ... Sisa fungsi lainnya (requestBluetoothPermissions, connectToBLE, gattCallback, alert, toast, onDestroy) biarkan seperti kode sebelumnya ...
//    // ... Saya sertakan lagi di bawah untuk kelengkapan ...
//
//    @SuppressLint("MissingPermission")
//    private fun requestBluetoothPermissions(onGranted: () -> Unit) {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
//            ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
//            val permissionLauncher =
//                registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){ permissions ->
//                    if (permissions.all{ it.value }) {
//                        onGranted()
//                    } else {
//                        toast("Izin Bluetooth ditolak")
//                    }
//                }
//            permissionLauncher.launch(arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.ACCESS_FINE_LOCATION))
//        } else {
//            onGranted()
//        }
//    }
//
//    @SuppressLint("MissingPermission")
//    private fun connectToBLE() {
//        val device = bluetoothAdapter?.getRemoteDevice(deviceAddress)
//        if (device == null) {
//            toast("Perangkat tidak ditemukan")
//            return
//        }
//        bluetoothGatt?.close()
//        bluetoothGatt = device.connectGatt(this, false, gattCallback)
//        toast("Menghubungkan ke perangkat BLE...")
//    }
//
//    @SuppressLint("MissingPermission")
//    private val gattCallback = object : BluetoothGattCallback() {
//        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
//            when (newState) {
//                BluetoothProfile.STATE_CONNECTED -> {
//                    retryCount = 0
//                    runOnUiThread { toast("Terkoneksi ke BLE, mencari layanan...") }
//                    gatt?.discoverServices()
//                }
//                BluetoothProfile.STATE_DISCONNECTED -> {
//                    runOnUiThread { toast("BLE terputus. Coba lagi...") }
//                    bluetoothGatt?.close()
//                    bluetoothGatt = null
//                    if (retryCount++ < maxRetries) {
//                        Handler(Looper.getMainLooper()).postDelayed({ connectToBLE() }, reconnectDelay)
//                    } else {
//                        toast("Gagal reconnect setelah $maxRetries kali")
//                    }
//                }
//            }
//        }
//        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
//            val charac = gatt?.getService(serviceUUID)?.getCharacteristic(characteristicUUID)
//            if (charac != null) {
//                gatt.setCharacteristicNotification(charac, true)
//                val descriptor = charac.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
//                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
//                gatt.writeDescriptor(descriptor)
//                toast("Notifikasi BLE diaktifkan")
//            } else {
//                toast("Karakteristik BLE tidak ditemukan")
//            }
//        }
//        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
//            val value = characteristic?.getStringValue(0)
//            value?.let { handleSensorData(it)}
//        }
//    }
//
//    private fun alert(msg: String) {
//        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
//    }
//
//    private fun toast(msg: String) {
//        runOnUiThread {
//            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
//        }
//    }
//
//    @SuppressLint("MissingPermission")
//    override fun onDestroy() {
//        if (tts != null) {
//            tts!!.stop()
//            tts!!.shutdown()
//        }
//        bluetoothGatt?.close()
//        bluetoothGatt = null
//        super.onDestroy()
//    }
//}
//package com.example.percobaan6
//
//
//import android.Manifest
//import android.annotation.SuppressLint
//import android.bluetooth.*
//import android.content.Context
//import android.content.pm.PackageManager
//import android.media.AudioManager
//import android.os.*
//import android.speech.tts.TextToSpeech
//import android.widget.Toast
//import androidx.activity.result.contract.ActivityResultContracts
//import androidx.appcompat.app.AppCompatActivity
//import androidx.core.app.ActivityCompat
//import androidx.navigation.findNavController
//import androidx.navigation.ui.AppBarConfiguration
//import androidx.navigation.ui.setupWithNavController
//import com.example.percobaan6.databinding.ActivityMainBinding
//import com.google.android.material.bottomnavigation.BottomNavigationView
//import java.util.*
//
//
//class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
//
//    private lateinit var binding: ActivityMainBinding
//    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
//    private val deviceAddress = "8C:4F:00:3C:99:CE" // Ganti MAC ESP32
//    private val serviceUUID = UUID.fromString("12345678-1234-1234-1234-1234567890ab")
//    private val characteristicUUID = UUID.fromString("abcd1234-ab12-cd34-ef56-abcdef123456")
//    private var bluetoothGatt: BluetoothGatt? = null
//
//    private val phoneNumber = "+6281234567890"
//    private var retryCount = 0
//    private val maxRetries = 5
//    private val reconnectDelay = 3000L
//
//    private var tts: TextToSpeech? = null
//    private var isTtsInitialized = false
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        binding = ActivityMainBinding.inflate(layoutInflater)
//        setContentView(binding.root)
//        tts = TextToSpeech(this, this)
//        val navView: BottomNavigationView = binding.navView
//        val navController = findNavController(R.id.nav_host_fragment_activity_main)
//        val appBarConfiguration = AppBarConfiguration(
//            setOf(R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications, R.id.navigation_history)
//        )
//        navView.setupWithNavController(navController)
//        requestBluetoothPermissions {
//            toast("Izin diberikan, menghubungkan BLE...")
//            connectToBLE()
//        }
//    }
//
//    // ===================================================
//    // == FUNGSI onInit DIPERBARUI DENGAN TOAST DIAGNOSTIK ==
//    // ===================================================
//    override fun onInit(status: Int) {
//        if (status == TextToSpeech.SUCCESS) {
//            val result = tts?.setLanguage(Locale("id", "ID"))
//            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
//                // Memberi tahu jika bahasa tidak ada
//                Toast.makeText(this, "Error: Paket Bahasa Indonesia untuk TTS tidak ditemukan.", Toast.LENGTH_LONG).show()
//            } else {
//                isTtsInitialized = true
//                // Memberi tahu jika TTS sudah siap
//                Toast.makeText(this, "Notifikasi Suara Siap Digunakan.", Toast.LENGTH_SHORT).show()
//            }
//        } else {
//            // Memberi tahu jika inisialisasi gagal total
//            Toast.makeText(this, "FATAL: Inisialisasi Text-to-Speech GAGAL.", Toast.LENGTH_LONG).show()
//        }
//    }
//
//    // ===================================================
//    // == FUNGSI speak DIPERBARUI UNTUK MEMAKSA SUARA KELUAR LEWAT ALARM ==
//    // ===================================================
//    private fun speak(text: String, queueMode: Int = TextToSpeech.QUEUE_FLUSH) {
//        if (isTtsInitialized) {
//            val params = Bundle()
//            // Menggunakan aliran audio ALARM untuk memastikan suara terdengar
//            params.putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_ALARM)
//
//            tts?.speak(text, queueMode, params, null)
//        } else {
//            // Memberi tahu jika tombol ditekan sebelum TTS siap
//            Toast.makeText(this, "TTS belum siap, coba tekan lagi dalam beberapa detik.", Toast.LENGTH_SHORT).show()
//        }
//    }
//
//    @SuppressLint("MissingPermission")
//    private fun requestBluetoothPermissions(onGranted: () -> Unit) {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
//            ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
//            val permissionLauncher =
//                registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){ permissions ->
//                    if (permissions.all{ it.value }) {
//                        onGranted()
//                    } else {
//                        toast("Izin Bluetooth ditolak")
//                    }
//                }
//            permissionLauncher.launch(arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.ACCESS_FINE_LOCATION))
//        } else {
//            onGranted()
//        }
//    }
//
//    @SuppressLint("MissingPermission")
//    private fun connectToBLE() {
//        val device = bluetoothAdapter?.getRemoteDevice(deviceAddress)
//        if (device == null) {
//            toast("Perangkat tidak ditemukan")
//            return
//        }
//        bluetoothGatt?.close()
//        bluetoothGatt = device.connectGatt(this, false, gattCallback)
//        toast("Menghubungkan ke perangkat BLE...")
//    }
//
//    @SuppressLint("MissingPermission")
//    private val gattCallback = object : BluetoothGattCallback() {
//        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
//            when (newState) {
//                BluetoothProfile.STATE_CONNECTED -> {
//                    retryCount = 0
//                    runOnUiThread { toast("Terkoneksi ke BLE, mencari layanan...") }
//                    gatt?.discoverServices()
//                }
//                BluetoothProfile.STATE_DISCONNECTED -> {
//                    runOnUiThread { toast("BLE terputus. Coba lagi...") }
//                    bluetoothGatt?.close()
//                    bluetoothGatt = null
//                    if (retryCount++ < maxRetries) {
//                        Handler(Looper.getMainLooper()).postDelayed({ connectToBLE() }, reconnectDelay)
//                    } else {
//                        toast("Gagal reconnect setelah $maxRetries kali")
//                    }
//                }
//            }
//        }
//        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
//            val charac = gatt?.getService(serviceUUID)?.getCharacteristic(characteristicUUID)
//            if (charac != null) {
//                gatt.setCharacteristicNotification(charac, true)
//                val descriptor = charac.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
//                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
//                gatt.writeDescriptor(descriptor)
//                toast("Notifikasi BLE diaktifkan")
//            } else {
//                toast("Karakteristik BLE tidak ditemukan")
//            }
//        }
//        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
//            val value = characteristic?.getStringValue(0)
//            value?.let { handleSensorData(it)}
//        }
//    }
//
//    private fun handleSensorData(line: String) {
//        val parts = line.split(",")
//        println("Data masuk: $line")
//        if (parts.size == 4) {
//            try {
//                val vitals = Vitals(parts[1].toInt(), parts[2].toInt())
//                val measurement = Measurement(vitals.bpm, 80, vitals.spo2, 95)
//                if (!measurement.isValid()) {
//                    alert("Data tidak valid! BPM Error: %.2f%%, SpO₂ Error: %.2f%%".format(measurement.bpmError(), measurement.spo2Error()))
//                    return
//                }
//                val result = AsthmaDetector.detect(vitals)
//                runOnUiThread {
//                    val statusText = when (result) {
//                        AsthmaLevel.SEVERE, AsthmaLevel.MODERATE -> {
//                            println("Buzzer ON")
//                            speak(getString(R.string.asthma_alert_speech), TextToSpeech.QUEUE_FLUSH)
//                            speak(getString(R.string.asthma_instructions_speech), TextToSpeech.QUEUE_ADD)
//                            "⚠️ Asma ${result.name.lowercase()} terdeteksi!"
//                        }
//                        AsthmaLevel.NORMAL -> {
//                            tts?.stop()
//                            alert("Kondisi normal. BPM: ${vitals.bpm}, SpO₂: ${vitals.spo2}%")
//                            "✅ Kondisi normal"
//                        }
//                    }
//                    Toast.makeText(this, statusText, Toast.LENGTH_LONG).show()
//                }
//            } catch (e: Exception) {
//                // toast("Error parsing data BLE")
//            }
//        }
//    }
//
//    private fun alert(msg: String) {
//        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
//    }
//
//    private fun toast(msg: String) {
//        runOnUiThread {
//            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
//        }
//    }
//
//    fun testAsthmaAlert() {
//        val fakeBleData = "ID,130,88,CONF"
//        handleSensorData(fakeBleData)
//        Toast.makeText(this, "MEMICU TES SUARA ASMA...", Toast.LENGTH_SHORT).show()
//    }
//
//    @SuppressLint("MissingPermission")
//    override fun onDestroy() {
//        if (tts != null) {
//            tts!!.stop()
//            tts!!.shutdown()
//        }
//        bluetoothGatt?.close()
//        bluetoothGatt = null
//        super.onDestroy()
//    }
//}
//package com.example.percobaan6
//
//
//import android.Manifest
//import android.annotation.SuppressLint
//import android.bluetooth.*
//import android.content.pm.PackageManager
//import android.os.*
//import android.speech.tts.TextToSpeech
//import android.widget.Toast
//import androidx.activity.result.contract.ActivityResultContracts
//import androidx.appcompat.app.AppCompatActivity
//import androidx.core.app.ActivityCompat
//import androidx.navigation.findNavController
//import androidx.navigation.ui.AppBarConfiguration
//import androidx.navigation.ui.setupWithNavController
//import com.example.percobaan6.databinding.ActivityMainBinding
//import com.google.android.material.bottomnavigation.BottomNavigationView
//import java.util.*
//
//
//class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
//
//    private lateinit var binding: ActivityMainBinding
//    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
//    private val deviceAddress = "8C:4F:00:3C:99:CE" // Ganti MAC ESP32
//    private val serviceUUID = UUID.fromString("12345678-1234-1234-1234-1234567890ab")
//    private val characteristicUUID = UUID.fromString("abcd1234-ab12-cd34-ef56-abcdef123456")
//    private var bluetoothGatt: BluetoothGatt? = null
//
//    private val phoneNumber = "+6281234567890"
//    private var retryCount = 0
//    private val maxRetries = 5
//    private val reconnectDelay = 3000L
//
//    // Variabel untuk Text-to-Speech
//    private var tts: TextToSpeech? = null
//    private var isTtsInitialized = false
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//
//        binding = ActivityMainBinding.inflate(layoutInflater)
//        setContentView(binding.root)
//
//        // Inisialisasi TextToSpeech
//        tts = TextToSpeech(this, this)
//
//        val navView: BottomNavigationView = binding.navView
//        val navController = findNavController(R.id.nav_host_fragment_activity_main)
//        val appBarConfiguration = AppBarConfiguration(
//            setOf(
//                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications, R.id.navigation_history
//            )
//        )
//        navView.setupWithNavController(navController)
//
//        // Inisialisasi koneksi BLE
//        requestBluetoothPermissions {
//            toast("Izin diberikan, menghubungkan BLE...")
//            connectToBLE()
//        }
//    }
//
//    override fun onInit(status: Int) {
//        if (status == TextToSpeech.SUCCESS) {
//            // Atur bahasa ke Bahasa Indonesia
//            val result = tts?.setLanguage(Locale("id", "ID"))
//            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
//                toast("Bahasa Indonesia tidak didukung di perangkat ini.")
//            } else {
//                isTtsInitialized = true // Tandai TTS sudah siap
//            }
//        } else {
//            toast("Inisialisasi Text-to-Speech gagal.")
//        }
//    }
//
//    private fun speak(text: String, queueMode: Int = TextToSpeech.QUEUE_FLUSH) {
//        if (isTtsInitialized) {
//            tts?.speak(text, queueMode, null, null)
//        }
//    }
//
//    private fun requestBluetoothPermissions(onGranted: () -> Unit) {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
//            ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
//        ) {
//            val permissionLauncher =
//                registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){ permissions ->
//                    if (permissions.all{ it.value }) {
//                        onGranted()
//                    } else {
//                        toast("Izin Bluetooth ditolak")
//                    }
//                }
//
//            permissionLauncher.launch(
//                arrayOf(
//                    Manifest.permission.BLUETOOTH_CONNECT,
//                    Manifest.permission.BLUETOOTH_SCAN,
//                    Manifest.permission.ACCESS_FINE_LOCATION
//                )
//            )
//        } else {
//            onGranted()
//        }
//    }
//
//    @SuppressLint("MissingPermission")
//    private fun connectToBLE() {
//        val device = bluetoothAdapter?.getRemoteDevice(deviceAddress)
//        if (device == null) {
//            toast("Perangkat tidak ditemukan")
//            return
//        }
//
//        bluetoothGatt?.close()
//        bluetoothGatt = device.connectGatt(this, false, gattCallback)
//        toast("Menghubungkan ke perangkat BLE...")
//    }
//
//    @SuppressLint("MissingPermission")
//    private val gattCallback = object : BluetoothGattCallback() {
//        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
//            when (newState) {
//                BluetoothProfile.STATE_CONNECTED -> {
//                    retryCount = 0
//                    runOnUiThread { toast("Terkoneksi ke BLE, mencari layanan...") }
//                    gatt?.discoverServices()
//                }
//                BluetoothProfile.STATE_DISCONNECTED -> {
//                    runOnUiThread { toast("BLE terputus. Coba lagi...") }
//                    bluetoothGatt?.close()
//                    bluetoothGatt = null
//                    if (retryCount++ < maxRetries) {
//                        Handler(Looper.getMainLooper()).postDelayed({ connectToBLE() }, reconnectDelay)
//                    } else {
//                        toast("Gagal reconnect setelah $maxRetries kali")
//                    }
//                }
//            }
//        }
//
//        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
//            val charac = gatt?.getService(serviceUUID)?.getCharacteristic(characteristicUUID)
//            if (charac != null) {
//                gatt.setCharacteristicNotification(charac, true)
//                val descriptor = charac.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
//                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
//                gatt.writeDescriptor(descriptor)
//                toast("Notifikasi BLE diaktifkan")
//            } else {
//                toast("Karakteristik BLE tidak ditemukan")
//            }
//        }
//
//        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
//            val value = characteristic?.getStringValue(0)
//            value?.let { handleSensorData(it)}
//        }
//    }
//
//    private fun handleSensorData(line: String) {
//        val parts = line.split(",")
//        println("Data masuk: $line")
//        if (parts.size == 4) {
//
//            try {
//                val vitals = Vitals(parts[1].toInt(), parts[2].toInt())
//                val measurement = Measurement(
//                    vitals.bpm, 80,
//                    vitals.spo2, 95
//                )
//                if (!measurement.isValid()) {
//                    alert("Data tidak valid! BPM Error: %.2f%%, SpO₂ Error: %.2f%%".format(
//                        measurement.bpmError(), measurement.spo2Error()
//                    ))
//                    return
//                }
//
//                val result = AsthmaDetector.detect(vitals)
//
//                runOnUiThread {
//                    val statusText = when (result) {
//                        AsthmaLevel.SEVERE, AsthmaLevel.MODERATE -> {
//                            println("Buzzer ON")
//                            // Panggil peringatan pertama, hentikan suara lain (FLUSH)
//                            speak(getString(R.string.asthma_alert_speech), TextToSpeech.QUEUE_FLUSH)
//                            // Tambahkan instruksi ke antrian untuk diputar setelah peringatan (ADD)
//                            speak(getString(R.string.asthma_instructions_speech), TextToSpeech.QUEUE_ADD)
//
//                            "⚠️ Asma ${result.name.lowercase()} terdeteksi!"
//                        }
//                        AsthmaLevel.NORMAL -> {
//                            // Hentikan suara jika kondisi kembali normal
//                            tts?.stop()
//                            alert("Kondisi normal. BPM: ${vitals.bpm}, SpO₂: ${vitals.spo2}%")
//                            "✅ Kondisi normal"
//                        }
//                    }
//                    Toast.makeText(this, statusText, Toast.LENGTH_LONG).show()
//                }
//            } catch (e: Exception) {
//                // toast("Error parsing data BLE")
//            }
//        }
//    }
//
//    private fun alert(msg: String) {
//        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
//    }
//
//    private fun toast(msg: String) {
//        runOnUiThread {
//            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
//        }
//    }
//
//    fun testAsthmaAlert() {
//        // Kita memalsukan data masuk seolah-olah dari BLE
//        // BPM > 120 atau SpO2 < 90 akan memicu SEVERE
//        val fakeBleData = "ID,130,88,CONF"
//        handleSensorData(fakeBleData)
//        Toast.makeText(this, "MEMICU TES SUARA ASMA...", Toast.LENGTH_SHORT).show()
//    }
//
//    // ===================================================
//    // == ANOTASI @SuppressLint DITAMBAHKAN DI SINI UNTUK MEMPERBAIKI ERROR BUILD ==
//    // ===================================================
//    @SuppressLint("MissingPermission")
//    override fun onDestroy() {
//        // Hentikan dan matikan TTS
//        if (tts != null) {
//            tts!!.stop()
//            tts!!.shutdown()
//        }
//
//        bluetoothGatt?.close()
//        bluetoothGatt = null
//        super.onDestroy()
//    }
//}
//package com.example.percobaan6
//
//
//import android.Manifest
//import android.annotation.SuppressLint
//import android.bluetooth.*
//import android.content.pm.PackageManager
//import android.os.*
//import android.speech.tts.TextToSpeech
//import android.widget.Toast
//import androidx.activity.result.contract.ActivityResultContracts
//import androidx.appcompat.app.AppCompatActivity
//import androidx.core.app.ActivityCompat
//import androidx.navigation.findNavController
//import androidx.navigation.ui.AppBarConfiguration
//import androidx.navigation.ui.setupWithNavController
//import com.example.percobaan6.databinding.ActivityMainBinding
//import com.google.android.material.bottomnavigation.BottomNavigationView
//import java.util.*
//
//
//class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
//
//    private lateinit var binding: ActivityMainBinding
//    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
//    private val deviceAddress = "8C:4F:00:3C:99:CE" // Ganti MAC ESP32
//    private val serviceUUID = UUID.fromString("12345678-1234-1234-1234-1234567890ab")
//    private val characteristicUUID = UUID.fromString("abcd1234-ab12-cd34-ef56-abcdef123456")
//    private var bluetoothGatt: BluetoothGatt? = null
//
//    private val phoneNumber = "+6281234567890"
//    private var retryCount = 0
//    private val maxRetries = 5
//    private val reconnectDelay = 3000L
//
//    // Variabel untuk Text-to-Speech
//    private var tts: TextToSpeech? = null
//    private var isTtsInitialized = false
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//
//        binding = ActivityMainBinding.inflate(layoutInflater)
//        setContentView(binding.root)
//
//        // Inisialisasi TextToSpeech
//        tts = TextToSpeech(this, this)
//
//        val navView: BottomNavigationView = binding.navView
//        val navController = findNavController(R.id.nav_host_fragment_activity_main)
//        val appBarConfiguration = AppBarConfiguration(
//            setOf(
//                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications, R.id.navigation_history
//            )
//        )
//        navView.setupWithNavController(navController)
//
//        // Inisialisasi koneksi BLE
//        requestBluetoothPermissions {
//            toast("Izin diberikan, menghubungkan BLE...")
//            connectToBLE()
//        }
//    }
//
//    override fun onInit(status: Int) {
//        if (status == TextToSpeech.SUCCESS) {
//            // Atur bahasa ke Bahasa Indonesia
//            val result = tts?.setLanguage(Locale("id", "ID"))
//            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
//                toast("Bahasa Indonesia tidak didukung di perangkat ini.")
//            } else {
//                isTtsInitialized = true // Tandai TTS sudah siap
//            }
//        } else {
//            toast("Inisialisasi Text-to-Speech gagal.")
//        }
//    }
//
//    private fun speak(text: String, queueMode: Int = TextToSpeech.QUEUE_FLUSH) {
//        if (isTtsInitialized) {
//            tts?.speak(text, queueMode, null, null)
//        }
//    }
//
//    private fun requestBluetoothPermissions(onGranted: () -> Unit) {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
//            ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
//        ) {
//            val permissionLauncher =
//                registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){ permissions ->
//                    if (permissions.all{ it.value }) {
//                        onGranted()
//                    } else {
//                        toast("Izin Bluetooth ditolak")
//                    }
//                }
//
//            permissionLauncher.launch(
//                arrayOf(
//                    Manifest.permission.BLUETOOTH_CONNECT,
//                    Manifest.permission.BLUETOOTH_SCAN,
//                    Manifest.permission.ACCESS_FINE_LOCATION
//                )
//            )
//        } else {
//            onGranted()
//        }
//    }
//
//    // ===================================================
//    // == ANOTASI @SuppressLint DITAMBAHKAN DI SINI ==
//    // ===================================================
//    @SuppressLint("MissingPermission")
//    private fun connectToBLE() {
//        val device = bluetoothAdapter?.getRemoteDevice(deviceAddress)
//        if (device == null) {
//            toast("Perangkat tidak ditemukan")
//            return
//        }
//
//        bluetoothGatt?.close()
//        bluetoothGatt = device.connectGatt(this, false, gattCallback)
//        toast("Menghubungkan ke perangkat BLE...")
//    }
//
//    // ===================================================
//    // == ANOTASI @SuppressLint DITAMBAHKAN DI SINI ==
//    // ===================================================
//    @SuppressLint("MissingPermission")
//    private val gattCallback = object : BluetoothGattCallback() {
//        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
//            when (newState) {
//                BluetoothProfile.STATE_CONNECTED -> {
//                    retryCount = 0
//                    runOnUiThread { toast("Terkoneksi ke BLE, mencari layanan...") }
//                    gatt?.discoverServices()
//                }
//                BluetoothProfile.STATE_DISCONNECTED -> {
//                    runOnUiThread { toast("BLE terputus. Coba lagi...") }
//                    bluetoothGatt?.close()
//                    bluetoothGatt = null
//                    if (retryCount++ < maxRetries) {
//                        Handler(Looper.getMainLooper()).postDelayed({ connectToBLE() }, reconnectDelay)
//                    } else {
//                        toast("Gagal reconnect setelah $maxRetries kali")
//                    }
//                }
//            }
//        }
//
//        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
//            val charac = gatt?.getService(serviceUUID)?.getCharacteristic(characteristicUUID)
//            if (charac != null) {
//                gatt.setCharacteristicNotification(charac, true)
//                val descriptor = charac.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
//                descriptor?.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
//                gatt.writeDescriptor(descriptor)
//                toast("Notifikasi BLE diaktifkan")
//            } else {
//                toast("Karakteristik BLE tidak ditemukan")
//            }
//        }
//
//        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
//            val value = characteristic?.getStringValue(0)
//            value?.let { handleSensorData(it)}
//        }
//    }
//
//    private fun handleSensorData(line: String) {
//        val parts = line.split(",")
//        println("Data masuk: $line")
//        if (parts.size == 4) {
//
//            try {
//                val vitals = Vitals(parts[1].toInt(), parts[2].toInt())
//                val measurement = Measurement(
//                    vitals.bpm, 80,
//                    vitals.spo2, 95
//                )
//                if (!measurement.isValid()) {
//                    alert("Data tidak valid! BPM Error: %.2f%%, SpO₂ Error: %.2f%%".format(
//                        measurement.bpmError(), measurement.spo2Error()
//                    ))
//                    return
//                }
//
//                val result = AsthmaDetector.detect(vitals)
//
//                runOnUiThread {
//                    val statusText = when (result) {
//                        AsthmaLevel.SEVERE, AsthmaLevel.MODERATE -> {
//                            println("Buzzer ON")
//                            // Panggil peringatan pertama, hentikan suara lain (FLUSH)
//                            speak(getString(R.string.asthma_alert_speech), TextToSpeech.QUEUE_FLUSH)
//                            // Tambahkan instruksi ke antrian untuk diputar setelah peringatan (ADD)
//                            speak(getString(R.string.asthma_instructions_speech), TextToSpeech.QUEUE_ADD)
//
//                            "⚠️ Asma ${result.name.lowercase()} terdeteksi!"
//                        }
//                        AsthmaLevel.NORMAL -> {
//                            // Hentikan suara jika kondisi kembali normal
//                            tts?.stop()
//                            alert("Kondisi normal. BPM: ${vitals.bpm}, SpO₂: ${vitals.spo2}%")
//                            "✅ Kondisi normal"
//                        }
//                    }
//                    Toast.makeText(this, statusText, Toast.LENGTH_LONG).show()
//                }
//            } catch (e: Exception) {
//                // toast("Error parsing data BLE")
//            }
//        }
//    }
//
//    private fun alert(msg: String) {
//        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
//    }
//
//    private fun toast(msg: String) {
//        runOnUiThread {
//            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
//        }
//    }
//
//    fun testAsthmaAlert() {
//        // Kita memalsukan data masuk seolah-olah dari BLE
//        // BPM > 120 atau SpO2 < 90 akan memicu SEVERE
//        val fakeBleData = "ID,130,88,CONF"
//        handleSensorData(fakeBleData)
//        Toast.makeText(this, "MEMICU TES SUARA ASMA...", Toast.LENGTH_SHORT).show()
//    }
//
//    override fun onDestroy() {
//        // Hentikan dan matikan TTS
//        if (tts != null) {
//            tts!!.stop()
//            tts!!.shutdown()
//        }
//
//        bluetoothGatt?.close()
//        bluetoothGatt = null
//        super.onDestroy()
//    }
//}
//package com.example.percobaan6
//
//
//import android.Manifest
//import android.bluetooth.*
//import android.content.pm.PackageManager
//import android.os.*
//import android.speech.tts.TextToSpeech
//import android.widget.Toast
//import androidx.activity.result.contract.ActivityResultContracts
//import androidx.appcompat.app.AppCompatActivity
//import androidx.core.app.ActivityCompat
//import androidx.navigation.findNavController
//import androidx.navigation.ui.AppBarConfiguration
//import androidx.navigation.ui.setupWithNavController
//import com.example.percobaan6.databinding.ActivityMainBinding
//import com.google.android.material.bottomnavigation.BottomNavigationView
//import java.util.*
//
//
//class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
//
//    private lateinit var binding: ActivityMainBinding
//    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
//    private val deviceAddress = "8C:4F:00:3C:99:CE" // Ganti MAC ESP32
//    private val serviceUUID = UUID.fromString("12345678-1234-1234-1234-1234567890ab")
//    private val characteristicUUID = UUID.fromString("abcd1234-ab12-cd34-ef56-abcdef123456")
//    private var bluetoothGatt: BluetoothGatt? = null
//
//    private val phoneNumber = "+6281234567890"
//    private var retryCount = 0
//    private val maxRetries = 5
//    private val reconnectDelay = 3000L
//
//    // Variabel untuk Text-to-Speech
//    private var tts: TextToSpeech? = null
//    private var isTtsInitialized = false
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//
//        binding = ActivityMainBinding.inflate(layoutInflater)
//        setContentView(binding.root)
//
//        // Inisialisasi TextToSpeech
//        tts = TextToSpeech(this, this)
//
//        val navView: BottomNavigationView = binding.navView
//        val navController = findNavController(R.id.nav_host_fragment_activity_main)
//        val appBarConfiguration = AppBarConfiguration(
//            setOf(
//                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications, R.id.navigation_history
//            )
//        )
//        navView.setupWithNavController(navController)
//
//        // Inisialisasi koneksi BLE
//        requestBluetoothPermissions {
//            toast("Izin diberikan, menghubungkan BLE...")
//            connectToBLE()
//        }
//    }
//
//    override fun onInit(status: Int) {
//        if (status == TextToSpeech.SUCCESS) {
//            // Atur bahasa ke Bahasa Indonesia
//            val result = tts?.setLanguage(Locale("id", "ID"))
//            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
//                toast("Bahasa Indonesia tidak didukung di perangkat ini.")
//            } else {
//                isTtsInitialized = true // Tandai TTS sudah siap
//            }
//        } else {
//            toast("Inisialisasi Text-to-Speech gagal.")
//        }
//    }
//
//    private fun speak(text: String, queueMode: Int = TextToSpeech.QUEUE_FLUSH) {
//        if (isTtsInitialized) {
//            tts?.speak(text, queueMode, null, null)
//        }
//    }
//
//    private fun requestBluetoothPermissions(onGranted: () -> Unit) {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
//            ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
//        ) {
//            val permissionLauncher =
//                registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){ permissions ->
//                    if (permissions.all{ it.value }) {
//                        onGranted()
//                    } else {
//                        toast("Izin Bluetooth ditolak")
//                    }
//                }
//
//            permissionLauncher.launch(
//                arrayOf(
//                    Manifest.permission.BLUETOOTH_CONNECT,
//                    Manifest.permission.BLUETOOTH_SCAN,
//                    Manifest.permission.ACCESS_FINE_LOCATION
//                )
//            )
//        } else {
//            onGranted()
//        }
//    }
//
//    private fun connectToBLE() {
//        val device = bluetoothAdapter?.getRemoteDevice(deviceAddress)
//        if (device == null) {
//            toast("Perangkat tidak ditemukan")
//            return
//        }
//
//        bluetoothGatt?.close()
//        bluetoothGatt = device.connectGatt(this, false, gattCallback)
//        toast("Menghubungkan ke perangkat BLE...")
//    }
//
//    private val gattCallback = object : BluetoothGattCallback() {
//        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
//            when (newState) {
//                BluetoothProfile.STATE_CONNECTED -> {
//                    retryCount = 0
//                    runOnUiThread { toast("Terkoneksi ke BLE, mencari layanan...") }
//                    gatt?.discoverServices()
//                }
//                BluetoothProfile.STATE_DISCONNECTED -> {
//                    runOnUiThread { toast("BLE terputus. Coba lagi...") }
//                    bluetoothGatt?.close()
//                    bluetoothGatt = null
//                    if (retryCount++ < maxRetries) {
//                        Handler(Looper.getMainLooper()).postDelayed({ connectToBLE() }, reconnectDelay)
//                    } else {
//                        toast("Gagal reconnect setelah $maxRetries kali")
//                    }
//                }
//            }
//        }
//
//        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
//            val charac = gatt?.getService(serviceUUID)?.getCharacteristic(characteristicUUID)
//            if (charac != null) {
//                gatt.setCharacteristicNotification(charac, true)
//                val descriptor = charac.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
//                descriptor?.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
//                gatt.writeDescriptor(descriptor)
//                toast("Notifikasi BLE diaktifkan")
//            } else {
//                toast("Karakteristik BLE tidak ditemukan")
//            }
//        }
//
//        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
//            val value = characteristic?.getStringValue(0)
//            value?.let { handleSensorData(it)}
//        }
//    }
//
//    private fun handleSensorData(line: String) {
//        val parts = line.split(",")
//        println("Data masuk: $line")
//        if (parts.size == 4) {
//
//            try {
//                val vitals = Vitals(parts[1].toInt(), parts[2].toInt())
//                val measurement = Measurement(
//                    vitals.bpm, 80,
//                    vitals.spo2, 95
//                )
//                if (!measurement.isValid()) {
//                    alert("Data tidak valid! BPM Error: %.2f%%, SpO₂ Error: %.2f%%".format(
//                        measurement.bpmError(), measurement.spo2Error()
//                    ))
//                    return
//                }
//
//                val result = AsthmaDetector.detect(vitals)
//
//                runOnUiThread {
//                    val statusText = when (result) {
//                        AsthmaLevel.SEVERE, AsthmaLevel.MODERATE -> {
//                            println("Buzzer ON")
//                            // Panggil peringatan pertama, hentikan suara lain (FLUSH)
//                            speak(getString(R.string.asthma_alert_speech), TextToSpeech.QUEUE_FLUSH)
//                            // Tambahkan instruksi ke antrian untuk diputar setelah peringatan (ADD)
//                            speak(getString(R.string.asthma_instructions_speech), TextToSpeech.QUEUE_ADD)
//
//                            "⚠️ Asma ${result.name.lowercase()} terdeteksi!"
//                        }
//                        AsthmaLevel.NORMAL -> {
//                            // Hentikan suara jika kondisi kembali normal
//                            tts?.stop()
//                            alert("Kondisi normal. BPM: ${vitals.bpm}, SpO₂: ${vitals.spo2}%")
//                            "✅ Kondisi normal"
//                        }
//                    }
//                    Toast.makeText(this, statusText, Toast.LENGTH_LONG).show()
//                }
//            } catch (e: Exception) {
//                // toast("Error parsing data BLE")
//            }
//        }
//    }
//
//    private fun alert(msg: String) {
//        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
//    }
//
//    private fun toast(msg: String) {
//        runOnUiThread {
//            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
//        }
//    }
//
//    // ===================================================
//    // == FUNGSI TES SEMENTARA (DITAMBAHKAN) ==
//    // ===================================================
//    fun testAsthmaAlert() {
//        // Kita memalsukan data masuk seolah-olah dari BLE
//        // BPM > 120 atau SpO2 < 90 akan memicu SEVERE
//        val fakeBleData = "ID,130,88,CONF"
//        handleSensorData(fakeBleData)
//        Toast.makeText(this, "MEMICU TES SUARA ASMA...", Toast.LENGTH_SHORT).show()
//    }
//    // ===================================================
//    // ============ AKHIR DARI BAGIAN BARU =============
//    // ===================================================
//
//    override fun onDestroy() {
//        // Hentikan dan matikan TTS
//        if (tts != null) {
//            tts!!.stop()
//            tts!!.shutdown()
//        }
//
//        bluetoothGatt?.close()
//        bluetoothGatt = null
//        super.onDestroy()
//    }
//}
//package com.example.percobaan6
//
//import android.Manifest
//import android.bluetooth.*
//import android.content.pm.PackageManager
//import android.os.*
//import android.widget.Toast
//import androidx.activity.result.contract.ActivityResultContracts
//import androidx.appcompat.app.AppCompatActivity
//import androidx.core.app.ActivityCompat
//import androidx.navigation.findNavController
//import androidx.navigation.ui.AppBarConfiguration
//import androidx.navigation.ui.setupWithNavController
//import com.example.percobaan6.databinding.ActivityMainBinding
//import com.google.android.material.bottomnavigation.BottomNavigationView
//import java.util.*
//
//class MainActivity : AppCompatActivity() {
//
//    private lateinit var binding: ActivityMainBinding
//    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
//    private val deviceAddress = "8C:4F:00:3C:99:CE" // Ganti MAC ESP32
//    private val serviceUUID = UUID.fromString("12345678-1234-1234-1234-1234567890ab")
//    private val characteristicUUID = UUID.fromString("abcd1234-ab12-cd34-ef56-abcdef123456")
//    private var bluetoothGatt: BluetoothGatt? = null
//
//    private val phoneNumber = "+6281234567890"
//    private var retryCount = 0
//    private val maxRetries = 5
//    private val reconnectDelay = 3000L
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//
//        binding = ActivityMainBinding.inflate(layoutInflater)
//        setContentView(binding.root)
//
//        val navView: BottomNavigationView = binding.navView
//        val navController = findNavController(R.id.nav_host_fragment_activity_main)
//        val appBarConfiguration = AppBarConfiguration(
//            setOf(
//                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications, R.id.navigation_history
//            )
//        )
//        navView.setupWithNavController(navController)
//
//        // Inisialisasi koneksi BLE
//        requestBluetoothPermissions {
//            toast("Izin diberikan, menghubungkan BLE...")
//            connectToBLE()
//        }
//    }
//
//    private fun requestBluetoothPermissions(onGranted: () -> Unit) {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
//            ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
//        ) {
//            val permissionLauncher =
//                registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
//                    if (permissions.all { it.value }) {
//                        onGranted()
//                    } else {
//                        toast("Izin Bluetooth ditolak")
//                    }
//                }
//
//            permissionLauncher.launch(
//                arrayOf(
//                    Manifest.permission.BLUETOOTH_CONNECT,
//                    Manifest.permission.BLUETOOTH_SCAN,
//                    Manifest.permission.ACCESS_FINE_LOCATION
//                )
//            )
//        } else {
//            onGranted()
//        }
//    }
//
//    private fun connectToBLE() {
//        val device = bluetoothAdapter?.getRemoteDevice(deviceAddress)
//        if (device == null) {
//            toast("Perangkat tidak ditemukan")
//            return
//        }
//
//        bluetoothGatt?.close()
//        bluetoothGatt = device.connectGatt(this, false, gattCallback)
//        toast("Menghubungkan ke perangkat BLE...")
//    }
//
//    private val gattCallback = object : BluetoothGattCallback() {
//        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
//            when (newState) {
//                BluetoothProfile.STATE_CONNECTED -> {
//                    retryCount = 0
//                    runOnUiThread { toast("Terkoneksi ke BLE, mencari layanan...") }
//                    gatt?.discoverServices()
//                }
//                BluetoothProfile.STATE_DISCONNECTED -> {
//                    runOnUiThread { toast("BLE terputus. Coba lagi...") }
//                    bluetoothGatt?.close()
//                    bluetoothGatt = null
//                    if (retryCount++ < maxRetries) {
//                        Handler(Looper.getMainLooper()).postDelayed({ connectToBLE() }, reconnectDelay)
//                    } else {
//                        toast("Gagal reconnect setelah $maxRetries kali")
//                    }
//                }
//            }
//        }
//
//        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
//            val charac = gatt?.getService(serviceUUID)?.getCharacteristic(characteristicUUID)
//            if (charac != null) {
//                gatt.setCharacteristicNotification(charac, true)
//                val descriptor = charac.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
//                descriptor?.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
//                gatt.writeDescriptor(descriptor)
//                toast("Notifikasi BLE diaktifkan")
//            } else {
//                toast("Karakteristik BLE tidak ditemukan")
//            }
//        }
//
//        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
//            val value = characteristic?.getStringValue(0)
//            value?.let { handleSensorData(it) }
//        }
//    }
//
//    private fun handleSensorData(line: String) {
//        val parts = line.split(",")
//        println("Data masuk: $line")
//        if (parts.size == 4) {
//
//            try {
//                val vitals = Vitals(parts[1].toInt(), parts[2].toInt())
//                val measurement = Measurement(
//                    vitals.bpm, 80,
//                    vitals.spo2, 95
//                )
//                if (!measurement.isValid()) {
//                    alert("Data tidak valid! BPM Error: %.2f%%, SpO₂ Error: %.2f%%".format(
//                        measurement.bpmError(), measurement.spo2Error()
//                    ))
//                    return
//                }
//
//                val result = AsthmaDetector.detect(vitals)
//
//                runOnUiThread {
//                    val statusText = when (result) {
//                        AsthmaLevel.SEVERE, AsthmaLevel.MODERATE -> {
//                            println("Buzzer ON")
//                            "⚠️ Asma ${result.name.lowercase()} terdeteksi!"
//                        }
//                        AsthmaLevel.NORMAL -> {
//                            alert("Kondisi normal. BPM: ${vitals.bpm}, SpO₂: ${vitals.spo2}%")
//                            "✅ Kondisi normal"
//                        }
//                    }
//                    Toast.makeText(this, statusText, Toast.LENGTH_LONG).show()
//                }
//            } catch (e: Exception) {
//               // toast("Error parsing data BLE")
//            }
//        }
//    }
//
//    private fun alert(msg: String) {
//        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
//    }
//
//    private fun toast(msg: String) {
//        runOnUiThread {
//            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
//        }
//    }
//
//    override fun onDestroy() {
//        bluetoothGatt?.close()
//        bluetoothGatt = null
//        super.onDestroy()
//    }
//}
