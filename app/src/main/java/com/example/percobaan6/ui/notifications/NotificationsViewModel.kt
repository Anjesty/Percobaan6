package com.example.percobaan6.ui.notifications

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.util.*

class NotificationsViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "Health Alerts"
    }
    val text: LiveData<String> = _text

    private val _notifications = MutableLiveData<List<HealthNotification>>()
    val notifications: LiveData<List<HealthNotification>> = _notifications

    init {
        // Load dummy notifications data
        loadDummyNotifications()
    }

    private fun loadDummyNotifications() {
        val dummyNotifications = mutableListOf<HealthNotification>()
        val calendar = Calendar.getInstance()

        // Critical notification - current time
        dummyNotifications.add(
            HealthNotification(
                id = UUID.randomUUID().toString(),
                title = "Critical Low Oxygen",
                message = "Critical oxygen drop, please contact your doctor immediately!",
                timestamp = calendar.time,
                severity = NotificationSeverity.CRITICAL
            )
        )

        // Move calendar back 2 hours
        calendar.add(Calendar.HOUR, -2)

        // Asthma notification
        dummyNotifications.add(
            HealthNotification(
                id = UUID.randomUUID().toString(),
                title = "Asthma Detected",
                message = "Asthma detected, calling nearby hospital...",
                timestamp = calendar.time,
                severity = NotificationSeverity.CRITICAL
            )
        )

        // Move calendar back 8 hours
        calendar.add(Calendar.HOUR, -8)

        // Warning notification
        dummyNotifications.add(
            HealthNotification(
                id = UUID.randomUUID().toString(),
                title = "High Heart Rate",
                message = "Your heart rate has been above normal for 45 minutes. Consider resting.",
                timestamp = calendar.time,
                severity = NotificationSeverity.WARNING,
                isRead = true
            )
        )

        // Move calendar back 1 day
        calendar.add(Calendar.DAY_OF_YEAR, -1)

        // Info notification
        dummyNotifications.add(
            HealthNotification(
                id = UUID.randomUUID().toString(),
                title = "Battery Low",
                message = "Your device battery is at 15%. Please charge soon.",
                timestamp = calendar.time,
                severity = NotificationSeverity.INFO,
                isRead = true
            )
        )

        // Move calendar back 2 days
        calendar.add(Calendar.DAY_OF_YEAR, -2)

        // Info notification
        dummyNotifications.add(
            HealthNotification(
                id = UUID.randomUUID().toString(),
                title = "Weekly Report Ready",
                message = "Your weekly health report is ready. Tap to view details.",
                timestamp = calendar.time,
                severity = NotificationSeverity.INFO,
                isRead = true
            )
        )

        _notifications.value = dummyNotifications
    }

    // Function to mark notification as read
    fun markAsRead(notificationId: String) {
        val currentList = _notifications.value?.toMutableList() ?: mutableListOf()
        val updatedList = currentList.map { notification ->
            if (notification.id == notificationId) {
                notification.copy(isRead = true)
            } else {
                notification
            }
        }
        _notifications.value = updatedList
    }
}
//package com.example.percobaan6.ui.notifications
//
//import androidx.lifecycle.LiveData
//import androidx.lifecycle.MutableLiveData
//import androidx.lifecycle.ViewModel
//
//class NotificationsViewModel : ViewModel() {
//
//    private val _text = MutableLiveData<String>().apply {
//        value = "This is notifications Fragment"
//    }
//    val text: LiveData<String> = _text
//}