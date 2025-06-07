package com.example.percobaan6.ui.history

data class HealthParameter(
    val min: String,
    val max: String,
    val time: String,
    val abnormalPeriod: String
)

data class DailyHealthRecord(
    val date: String,
    val heartRate: HealthParameter,
    val oxygenLevel: HealthParameter,
    val breathRate: HealthParameter
)

// New models for real-time data
data class RealTimeHealthData(
    val timestamp: Long,
    val heartRate: Float,
    val spo2: Float,
    val breathRate: Float,
    val skinTemperature: Float,
    val eda: Float
)

data class HealthThresholds(
    val heartRateMin: Float = 60f,
    val heartRateMax: Float = 100f,
    val spo2Min: Float = 95f,
    val spo2Max: Float = 100f,
    val breathRateMin: Float = 12f,
    val breathRateMax: Float = 20f,
    val skinTempMin: Float = 36f,
    val skinTempMax: Float = 37f,
    val edaMin: Float = 1f,
    val edaMax: Float = 20f
)

data class HealthStatus(
    val parameter: String,
    val value: Float,
    val status: AlertLevel,
    val message: String
)

enum class AlertLevel {
    NORMAL,
    WARNING,
    CRITICAL
}

// Extension functions for health analysis
fun RealTimeHealthData.analyzeHeartRate(): HealthStatus {
    val thresholds = HealthThresholds()
    return when {
        heartRate < thresholds.heartRateMin -> HealthStatus(
            "Heart Rate", heartRate, AlertLevel.WARNING,
            "Heart rate below normal range (${heartRate.toInt()} BPM)"
        )
        heartRate > thresholds.heartRateMax -> HealthStatus(
            "Heart Rate", heartRate, AlertLevel.WARNING,
            "Heart rate above normal range (${heartRate.toInt()} BPM)"
        )
        else -> HealthStatus(
            "Heart Rate", heartRate, AlertLevel.NORMAL,
            "Heart rate normal (${heartRate.toInt()} BPM)"
        )
    }
}

fun RealTimeHealthData.analyzeSPO2(): HealthStatus {
    val thresholds = HealthThresholds()
    return when {
        spo2 < thresholds.spo2Min -> HealthStatus(
            "SPO2", spo2, AlertLevel.CRITICAL,
            "Blood oxygen level critically low (${spo2.toInt()}%)"
        )
        spo2 < 97f -> HealthStatus(
            "SPO2", spo2, AlertLevel.WARNING,
            "Blood oxygen level below optimal (${spo2.toInt()}%)"
        )
        else -> HealthStatus(
            "SPO2", spo2, AlertLevel.NORMAL,
            "Blood oxygen level normal (${spo2.toInt()}%)"
        )
    }
}

fun RealTimeHealthData.analyzeBreathRate(): HealthStatus {
    val thresholds = HealthThresholds()
    return when {
        breathRate < thresholds.breathRateMin -> HealthStatus(
            "Breath Rate", breathRate, AlertLevel.WARNING,
            "Breathing rate below normal (${breathRate.toInt()} BPM)"
        )
        breathRate > thresholds.breathRateMax -> HealthStatus(
            "Breath Rate", breathRate, AlertLevel.WARNING,
            "Breathing rate above normal (${breathRate.toInt()} BPM)"
        )
        else -> HealthStatus(
            "Breath Rate", breathRate, AlertLevel.NORMAL,
            "Breathing rate normal (${breathRate.toInt()} BPM)"
        )
    }
}

fun RealTimeHealthData.analyzeSkinTemperature(): HealthStatus {
    val thresholds = HealthThresholds()
    return when {
        skinTemperature < thresholds.skinTempMin -> HealthStatus(
            "Skin Temperature", skinTemperature, AlertLevel.WARNING,
            "Skin temperature below normal (${String.format("%.1f", skinTemperature)}°C)"
        )
        skinTemperature > thresholds.skinTempMax -> HealthStatus(
            "Skin Temperature", skinTemperature, AlertLevel.WARNING,
            "Skin temperature above normal (${String.format("%.1f", skinTemperature)}°C)"
        )
        else -> HealthStatus(
            "Skin Temperature", skinTemperature, AlertLevel.NORMAL,
            "Skin temperature normal (${String.format("%.1f", skinTemperature)}°C)"
        )
    }
}

fun RealTimeHealthData.analyzeEDA(): HealthStatus {
    return when {
        eda > 15f -> HealthStatus(
            "EDA", eda, AlertLevel.WARNING,
            "High stress level detected (${String.format("%.1f", eda)} μS)"
        )
        eda > 10f -> HealthStatus(
            "EDA", eda, AlertLevel.WARNING,
            "Moderate stress level (${String.format("%.1f", eda)} μS)"
        )
        else -> HealthStatus(
            "EDA", eda, AlertLevel.NORMAL,
            "Stress level normal (${String.format("%.1f", eda)} μS)"
        )
    }
}
//package com.example.percobaan6.ui.health
//
//class HealthDataModels {
//}