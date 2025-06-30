package com.example.percobaan6

/**
 * Enum class representing the detected level of asthma distress.
 */
enum class AsthmaLevel {
    NORMAL,    // Indicates normal physiological parameters.
    MODERATE,  // Suggests potential moderate asthma exacerbation or significant concern.
    SEVERE     // Indicates potential severe asthma exacerbation or critical condition.
}

/**
 * A class responsible for detecting potential asthma attack levels based on physiological
 * sensor data and activity context using a rule-based approach.
 *
 * This model leverages common clinical indicators for asthma exacerbation.
 *
 * Based on research and guidelines:
 * - SpO2 and Heart Rate are primary indicators for respiratory distress.
 * - Thresholds for moderate and severe asthma exacerbations often involve
 * specific ranges for Heart Rate and SpO2. For instance, some guidelines suggest:
 * - Moderate exacerbation: Heart Rate 100-120 BPM, SpO2 90-95%.
 * - Severe exacerbation: Heart Rate >120 BPM, SpO2 <90%.
 * - Accelerometer data provides crucial contextual information about physical activity,
 * helping to differentiate normal exercise responses from signs of distress.
 */
object AsthmaDetector {

    /**
     * Determines the current asthma level based on vital signs and activity.
     *
     * @param vitals The current physiological readings, including BPM and SpO2.
     * @param isRunning A boolean flag indicating if the person is currently engaged in strenuous activity like running.
     * @return An [AsthmaLevel] enum value (NORMAL, MODERATE, or SEVERE).
     */
    fun detect(vitals: Vitals, isRunning: Boolean): AsthmaLevel {
        val currentBPM = vitals.bpm
        val currentSpO2 = vitals.spo2

        // --- Define Thresholds (adjustable based on specific needs or deeper clinical validation) ---
        // SpO2 thresholds (Blood Oxygen Saturation) - critical for respiratory assessment.
        // Values below 95% typically warrant attention, and below 90% is more serious.
        val SPO2_NORMAL_MIN_THRESHOLD = 95
        val SPO2_MODERATE_MIN_THRESHOLD = 90

        // Heart Rate (BPM) thresholds for adults.
        // Normal resting BPM is typically 60-100. Tachycardia often accompanies respiratory distress.
        val BPM_NORMAL_MAX_THRESHOLD = 100
        val BPM_MODERATE_MIN_THRESHOLD = 100 // Elevated heart rate
        val BPM_SEVERE_MIN_THRESHOLD = 120   // Significantly elevated heart rate

        // --- Rule-Based Logic ---

        // Prioritize critical SpO2 levels, as severe hypoxemia is a direct indicator of distress.
        // These conditions are usually severe regardless of activity.
        // GINA guidelines: SpO2 < 90% indicates severe asthma exacerbation.
        if (currentSpO2 < SPO2_MODERATE_MIN_THRESHOLD) {
            return AsthmaLevel.SEVERE
        }

        // Evaluate based on activity context
        if (isRunning) {
            // When running, elevated BPM is expected and SpO2 might drop slightly due to exertion.
            // The system should be more tolerant of deviations that would be concerning at rest.
            // If SpO2 is in the 90-94% range while running, it's still a concern, but less critical
            // than if at rest. Combined with very high BPM, it might be moderate.
            if (currentSpO2 < SPO2_NORMAL_MIN_THRESHOLD && currentBPM > BPM_SEVERE_MIN_THRESHOLD) {
                return AsthmaLevel.MODERATE
            }

            // If high BPM but good SpO2, and running, assume normal exercise.
            return AsthmaLevel.NORMAL

        } else {
            // User is not running (at rest or light activity) - deviations are more concerning.

            // Severe Conditions (at rest/light activity)
            // GINA guidelines: BPM > 120 indicates severe asthma exacerbation.
            if (currentBPM > BPM_SEVERE_MIN_THRESHOLD) {
                return AsthmaLevel.SEVERE
            }

            // Moderate Conditions (at rest/light activity)
            // GINA guidelines: SpO2 90-95% or BPM 100-120 indicates moderate asthma exacerbation.
            if ((currentSpO2 >= SPO2_MODERATE_MIN_THRESHOLD && currentSpO2 < SPO2_NORMAL_MIN_THRESHOLD) ||
                (currentBPM >= BPM_MODERATE_MIN_THRESHOLD && currentBPM <= BPM_SEVERE_MIN_THRESHOLD && currentSpO2 < SPO2_NORMAL_MIN_THRESHOLD)) {
                return AsthmaLevel.MODERATE
            }

            // Normal Condition (at rest/light activity)
            // BPM is within normal resting range and SpO2 is healthy.
            if (currentSpO2 >= SPO2_NORMAL_MIN_THRESHOLD && currentBPM <= BPM_NORMAL_MAX_THRESHOLD) {
                return AsthmaLevel.NORMAL
            }
        }

        // Fallback: If no specific condition is met, assume normal.
        return AsthmaLevel.NORMAL
    }
}