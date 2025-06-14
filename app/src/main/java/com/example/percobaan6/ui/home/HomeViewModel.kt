package com.example.percobaan6.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.percobaan6.ui.history.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {
    private val _text = MutableLiveData<String>().apply {
        value = "Real-time Health Monitor"
    }
    val text: LiveData<String> = _text

    private val _currentHealthData = MutableLiveData<RealTimeHealthData>()
    val currentHealthData: LiveData<RealTimeHealthData> = _currentHealthData

    private val _healthAlerts = MutableLiveData<List<HealthStatus>>()
    val healthAlerts: LiveData<List<HealthStatus>> = _healthAlerts

    private val _isConnected = MutableLiveData<Boolean>().apply {
        value = false
    }
    val isConnected: LiveData<Boolean> = _isConnected

    private val _connectionStatus = MutableLiveData<String>().apply {
        value = "Waiting for sensor data..."
    }
    val connectionStatus: LiveData<String> = _connectionStatus

    private val _heartRateHistory = MutableLiveData<List<Float>>().apply {
        value = mutableListOf()
    }
    val heartRateHistory: LiveData<List<Float>> = _heartRateHistory

    private val _spo2History = MutableLiveData<List<Float>>().apply {
        value = mutableListOf()
    }
    val spo2History: LiveData<List<Float>> = _spo2History

    private val maxHistoryPoints = 20
    private var isSimulating = true

    init {
        // Start with simulation until real data comes
        startSimulatedDataGeneration()
    }

    /**
     * Method to receive real sensor data from MainActivity
     * Call this method when real BLE data is received
     */
    fun updateSensorData(heartRate: Int, spo2: Int) {
        // Stop simulation when real data comes
        isSimulating = false

        val realData = RealTimeHealthData(
            timestamp = System.currentTimeMillis(),
            heartRate = heartRate.toFloat(),
            spo2 = spo2.toFloat(),
            breathRate = 16f, // Default value since we're not using it
            skinTemperature = 36.5f, // Default value since we're not using it
            eda = 8f // Default value since we're not using it
        )

        _currentHealthData.value = realData
        _isConnected.value = true
        _connectionStatus.value = "Connected - Receiving real sensor data"

        updateHistoryData(realData)
        analyzeHealthData(realData)
    }

    /**
     * Method to handle connection status from MainActivity
     */
    fun updateConnectionStatus(isConnected: Boolean, message: String) {
        _isConnected.value = isConnected
        _connectionStatus.value = message

        if (!isConnected && !isSimulating) {
            // If disconnected and not simulating, start simulation again
            isSimulating = true
            startSimulatedDataGeneration()
        }
    }

    private fun updateHistoryData(data: RealTimeHealthData) {
        // Update heart rate history
        val heartRateList = _heartRateHistory.value?.toMutableList() ?: mutableListOf()
        heartRateList.add(data.heartRate)
        if (heartRateList.size > maxHistoryPoints) {
            heartRateList.removeAt(0)
        }
        _heartRateHistory.value = heartRateList

        // Update SPO2 history
        val spo2List = _spo2History.value?.toMutableList() ?: mutableListOf()
        spo2List.add(data.spo2)
        if (spo2List.size > maxHistoryPoints) {
            spo2List.removeAt(0)
        }
        _spo2History.value = spo2List
    }

    /**
     * Simulate real-time health data generation
     * This will be used as fallback when no real sensor data is available
     */
    private fun startSimulatedDataGeneration() {
        viewModelScope.launch {
            while (isSimulating) {
                val simulatedData = generateSimulatedHealthData()

                // Only update if we're still simulating (not receiving real data)
                if (isSimulating) {
                    _currentHealthData.value = simulatedData
                    _connectionStatus.value = "Simulating sensor data..."
                    updateHistoryData(simulatedData)
                    analyzeHealthData(simulatedData)
                }

                delay(1000)
            }
        }
    }

    private fun generateSimulatedHealthData(): RealTimeHealthData {
        return RealTimeHealthData(
            timestamp = System.currentTimeMillis(),
            heartRate = generateHeartRate(),
            spo2 = generateSPO2(),
            breathRate = 16f, // Default value
            skinTemperature = 36.5f, // Default value
            eda = 8f // Default value
        )
    }

    private fun generateHeartRate(): Float {
        val baseRate = 75f
        val variation = kotlin.random.Random.nextFloat() * 20f - 10f // ±10 BPM
        return (baseRate + variation).coerceIn(50f, 120f)
    }

    private fun generateSPO2(): Float {
        val baseLevel = 98f
        val variation = kotlin.random.Random.nextFloat() * 3f - 1.5f // ±1.5%
        return (baseLevel + variation).coerceIn(90f, 100f)
    }

    private fun analyzeHealthData(data: RealTimeHealthData) {
        val alerts = mutableListOf<HealthStatus>()

        // Only analyze Heart Rate and SPO2
        alerts.add(data.analyzeHeartRate())
        alerts.add(data.analyzeSPO2())

        _healthAlerts.value = alerts
    }

    /**
     * Get current health summary
     */
    fun getCurrentHealthSummary(): String {
        val data = _currentHealthData.value ?: return "No data available"
        val alerts = _healthAlerts.value ?: return "No alerts"

        val criticalAlerts = alerts.filter { it.status == AlertLevel.CRITICAL }
        val warningAlerts = alerts.filter { it.status == AlertLevel.WARNING }

        return when {
            criticalAlerts.isNotEmpty() -> "Critical: ${criticalAlerts.size} alert(s)"
            warningAlerts.isNotEmpty() -> "Warning: ${warningAlerts.size} alert(s)"
            else -> "All parameters normal"
        }
    }

    override fun onCleared() {
        super.onCleared()
        isSimulating = false
    }
}
//package com.example.percobaan6.ui.home
//
//import androidx.lifecycle.LiveData
//import androidx.lifecycle.MutableLiveData
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.viewModelScope
//import com.example.percobaan6.ui.history.*
//import kotlinx.coroutines.delay
//import kotlinx.coroutines.launch
//import kotlin.random.Random
//
//class HomeViewModel : ViewModel() {
//    private val _text = MutableLiveData<String>().apply {
//        value = "Real-time Health Monitor"
//    }
//    val text: LiveData<String> = _text
//
//    private val _currentHealthData = MutableLiveData<RealTimeHealthData>()
//    val currentHealthData: LiveData<RealTimeHealthData> = _currentHealthData
//
//    private val _healthAlerts = MutableLiveData<List<HealthStatus>>()
//    val healthAlerts: LiveData<List<HealthStatus>> = _healthAlerts
//
//    private val _isConnected = MutableLiveData<Boolean>().apply {
//        value = false // Will be true when connected to actual device
//    }
//    val isConnected: LiveData<Boolean> = _isConnected
//
//    private val _connectionStatus = MutableLiveData<String>().apply {
//        value = "Simulating sensor data..."
//    }
//    val connectionStatus: LiveData<String> = _connectionStatus
//
//    // Heart rate data for chart
//    private val _heartRateHistory = MutableLiveData<List<Float>>().apply {
//        value = mutableListOf()
//    }
//    val heartRateHistory: LiveData<List<Float>> = _heartRateHistory
//
//    // SPO2 data for chart
//    private val _spo2History = MutableLiveData<List<Float>>().apply {
//        value = mutableListOf()
//    }
//    val spo2History: LiveData<List<Float>> = _spo2History
//
//    // Breath rate data for chart
//    private val _breathRateHistory = MutableLiveData<List<Float>>().apply {
//        value = mutableListOf()
//    }
//    val breathRateHistory: LiveData<List<Float>> = _breathRateHistory
//
//    // Skin temperature data for chart
//    private val _skinTempHistory = MutableLiveData<List<Float>>().apply {
//        value = mutableListOf()
//    }
//    val skinTempHistory: LiveData<List<Float>> = _skinTempHistory
//
//    // EDA data for chart
//    private val _edaHistory = MutableLiveData<List<Float>>().apply {
//        value = mutableListOf()
//    }
//    val edaHistory: LiveData<List<Float>> = _edaHistory
//
//    // Maximum data points to keep in history
//    private val maxHistoryPoints = 20
//
//    init {
//        startSimulatedDataGeneration()
//    }
//
//    /**
//     * Simulate real-time health data generation
//     * This method will be replaced with actual sensor data when hardware is connected
//     */
//    private fun startSimulatedDataGeneration() {
//        viewModelScope.launch {
//            while (true) {
//                val simulatedData = generateSimulatedHealthData()
//                _currentHealthData.value = simulatedData
//
//                // Update history data for charts
//                updateHistoryData(simulatedData)
//
//                // Analyze data for alerts
//                analyzeHealthData(simulatedData)
//
//                delay(1000) // Update every second
//            }
//        }
//    }
//
//    private fun updateHistoryData(data: RealTimeHealthData) {
//        // Update heart rate history
//        val heartRateList = _heartRateHistory.value?.toMutableList() ?: mutableListOf()
//        heartRateList.add(data.heartRate)
//        if (heartRateList.size > maxHistoryPoints) {
//            heartRateList.removeAt(0)
//        }
//        _heartRateHistory.value = heartRateList
//
//        // Update SPO2 history
//        val spo2List = _spo2History.value?.toMutableList() ?: mutableListOf()
//        spo2List.add(data.spo2)
//        if (spo2List.size > maxHistoryPoints) {
//            spo2List.removeAt(0)
//        }
//        _spo2History.value = spo2List
//
//        // Update breath rate history
//        val breathRateList = _breathRateHistory.value?.toMutableList() ?: mutableListOf()
//        breathRateList.add(data.breathRate)
//        if (breathRateList.size > maxHistoryPoints) {
//            breathRateList.removeAt(0)
//        }
//        _breathRateHistory.value = breathRateList
//
//        // Update skin temperature history
//        val skinTempList = _skinTempHistory.value?.toMutableList() ?: mutableListOf()
//        skinTempList.add(data.skinTemperature)
//        if (skinTempList.size > maxHistoryPoints) {
//            skinTempList.removeAt(0)
//        }
//        _skinTempHistory.value = skinTempList
//
//        // Update EDA history
//        val edaList = _edaHistory.value?.toMutableList() ?: mutableListOf()
//        edaList.add(data.eda)
//        if (edaList.size > maxHistoryPoints) {
//            edaList.removeAt(0)
//        }
//        _edaHistory.value = edaList
//    }
//
//    private fun generateSimulatedHealthData(): RealTimeHealthData {
//        return RealTimeHealthData(
//            timestamp = System.currentTimeMillis(),
//            heartRate = generateHeartRate(),
//            spo2 = generateSPO2(),
//            breathRate = generateBreathRate(),
//            skinTemperature = generateSkinTemperature(),
//            eda = generateEDA()
//        )
//    }
//
//    private fun generateHeartRate(): Float {
//        // Simulate realistic heart rate with some variation
//        val baseRate = 75f
//        val variation = Random.nextFloat() * 20f - 10f // ±10 BPM
//        return (baseRate + variation).coerceIn(50f, 120f)
//    }
//
//    private fun generateSPO2(): Float {
//        // Simulate SPO2 with high accuracy (normally 97-100%)
//        val baseLevel = 98f
//        val variation = Random.nextFloat() * 3f - 1.5f // ±1.5%
//        return (baseLevel + variation).coerceIn(90f, 100f)
//    }
//
//    private fun generateBreathRate(): Float {
//        // Simulate breath rate (normal 12-20 breaths per minute)
//        val baseRate = 16f
//        val variation = Random.nextFloat() * 6f - 3f // ±3 BPM
//        return (baseRate + variation).coerceIn(8f, 30f)
//    }
//
//    private fun generateSkinTemperature(): Float {
//        // Simulate skin temperature (normal 36-37°C)
//        val baseTemp = 36.5f
//        val variation = Random.nextFloat() * 1f - 0.5f // ±0.5°C
//        return (baseTemp + variation).coerceIn(35f, 38f)
//    }
//
//    private fun generateEDA(): Float {
//        // Simulate EDA (Electrodermal Activity) - measures skin conductance
//        // Normal range is typically 1-20 μS (microsiemens)
//        // Higher values indicate higher stress/arousal
//        val baseEDA = 8f
//        val variation = Random.nextFloat() * 10f - 5f // ±5 μS
//        return (baseEDA + variation).coerceIn(1f, 25f)
//    }
//
//    private fun analyzeHealthData(data: RealTimeHealthData) {
//        val alerts = mutableListOf<HealthStatus>()
//
//        // Analyze each parameter
//        alerts.add(data.analyzeHeartRate())
//        alerts.add(data.analyzeSPO2())
//        alerts.add(data.analyzeBreathRate())
//        alerts.add(data.analyzeSkinTemperature())
//        alerts.add(data.analyzeEDA())
//
//        _healthAlerts.value = alerts
//    }
//
//    /**
//     * Method to connect to actual hardware device
//     * This will be implemented when hardware is ready
//     */
//    fun connectToDevice() {
//        _connectionStatus.value = "Connecting to device..."
//        // TODO: Implement actual device connection
//        viewModelScope.launch {
//            delay(2000) // Simulate connection time
//            _isConnected.value = true
//            _connectionStatus.value = "Connected to asthma detection device"
//        }
//    }
//
//    /**
//     * Method to disconnect from hardware device
//     */
//    fun disconnectDevice() {
//        _isConnected.value = false
//        _connectionStatus.value = "Disconnected - Using simulated data"
//    }
//
//    /**
//     * Get current health summary
//     */
//    fun getCurrentHealthSummary(): String {
//        val data = _currentHealthData.value ?: return "No data available"
//        val alerts = _healthAlerts.value ?: return "No alerts"
//
//        val criticalAlerts = alerts.filter { it.status == AlertLevel.CRITICAL }
//        val warningAlerts = alerts.filter { it.status == AlertLevel.WARNING }
//
//        return when {
//            criticalAlerts.isNotEmpty() -> "Critical: ${criticalAlerts.size} alert(s)"
//            warningAlerts.isNotEmpty() -> "Warning: ${warningAlerts.size} alert(s)"
//            else -> "All parameters normal"
//        }
//    }
//
//    override fun onCleared() {
//        super.onCleared()
//        // Clean up any resources if needed
//    }
//}
//package com.example.percobaan6.ui.home
//
//import androidx.lifecycle.LiveData
//import androidx.lifecycle.MutableLiveData
//import androidx.lifecycle.ViewModel
//
//class HomeViewModel : ViewModel() {
//
//    private val _text = MutableLiveData<String>().apply {
//        value = "This is home Fragment"
//    }
//    val text: LiveData<String> = _text
//}