package com.example.percobaan6.ui.notifications

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.percobaan6.ui.history.AlertLevel
import com.example.percobaan6.ui.history.HealthStatus
import java.util.*

class NotificationsViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "Health Alerts"
    }
    val text: LiveData<String> = _text

    private val _notifications = MutableLiveData<List<HealthNotification>>().apply {
        value = emptyList()
    }
    val notifications: LiveData<List<HealthNotification>> = _notifications

    // --- FUNGSI BARU UNTUK NOTIFIKASI LIVE ---
    fun updateActiveNotifications(latestStatuses: List<HealthStatus>) {
        // 1. Filter hanya status yang abnormal (WARNING atau CRITICAL)
        val activeAlerts = latestStatuses.filter {
            it.status == AlertLevel.WARNING || it.status == AlertLevel.CRITICAL
        }

        // 2. Ubah (map) setiap HealthStatus yang abnormal menjadi HealthNotification
        val newNotifications = activeAlerts.map { status ->
            HealthNotification(
                id = status.parameter, // Gunakan parameter ("Heart Rate", "SPO2") sebagai ID unik
                title = "Abnormal ${status.parameter}",
                message = status.message,
                timestamp = Date(),
                severity = if (status.status == AlertLevel.CRITICAL) NotificationSeverity.CRITICAL else NotificationSeverity.WARNING
            )
        }

        // 3. Perbarui LiveData dengan daftar notifikasi yang aktif saat ini.
        // Jika tidak ada yang abnormal, daftarnya akan kosong dan UI akan update.
        _notifications.value = newNotifications
    }
    // --- AKHIR FUNGSI BARU ---

    // Fungsi markAsRead bisa dihapus jika tidak lagi relevan dengan notifikasi live,
    // atau dibiarkan jika Anda ingin fungsionalitas "telah dilihat" di masa depan.
    // Untuk saat ini kita biarkan.
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
//package com.example.percobaan6.ui.notifications HAMPIR BENER 444444444444444444444444
//
//import androidx.lifecycle.LiveData
//import androidx.lifecycle.MutableLiveData
//import androidx.lifecycle.ViewModel
//import java.util.*
//
//class NotificationsViewModel : ViewModel() {
//
//    private val _text = MutableLiveData<String>().apply {
//        value = "Health Alerts"
//    }
//    val text: LiveData<String> = _text
//
//    private val _notifications = MutableLiveData<List<HealthNotification>>().apply {
//        value = emptyList() // Mulai dengan list kosong
//    }
//    val notifications: LiveData<List<HealthNotification>> = _notifications
//
//    // Fungsi untuk menambah notifikasi dari luar (dipanggil oleh MainActivity)
//    fun addNotification(alert: com.example.percobaan6.ui.history.HealthStatus) {
//        val newNotification = HealthNotification(
//            id = UUID.randomUUID().toString(),
//            title = alert.parameter, // Misal: "Heart Rate" atau "SPO2"
//            message = alert.message,
//            timestamp = Date(), // Waktu saat ini
//            severity = when(alert.status) {
//                com.example.percobaan6.ui.history.AlertLevel.CRITICAL -> NotificationSeverity.CRITICAL
//                com.example.percobaan6.ui.history.AlertLevel.WARNING -> NotificationSeverity.WARNING
//                else -> NotificationSeverity.INFO // Seharusnya tidak terjadi, tapi sebagai fallback
//            },
//            isRead = false
//        )
//
//        val currentList = _notifications.value?.toMutableList() ?: mutableListOf()
//        // Tambahkan notifikasi baru ke awal list agar muncul paling atas
//        currentList.add(0, newNotification)
//        _notifications.value = currentList
//    }
//
//    // Fungsi untuk menandai notifikasi sebagai sudah dibaca
//    fun markAsRead(notificationId: String) {
//        val currentList = _notifications.value?.toMutableList() ?: mutableListOf()
//        val updatedList = currentList.map { notification ->
//            if (notification.id == notificationId) {
//                notification.copy(isRead = true)
//            } else {
//                notification
//            }
//        }
//        _notifications.value = updatedList
//    }
//}
//package com.example.percobaan6.ui.notifications
//
//import androidx.lifecycle.LiveData
//import androidx.lifecycle.MutableLiveData
//import androidx.lifecycle.ViewModel
//import java.util.*
//
//class NotificationsViewModel : ViewModel() {
//
//    private val _text = MutableLiveData<String>().apply {
//        value = "Health Alerts"
//    }
//    val text: LiveData<String> = _text
//
//    private val _notifications = MutableLiveData<List<HealthNotification>>()
//    val notifications: LiveData<List<HealthNotification>> = _notifications
//
//    init {
//        // Load dummy notifications data
//        loadDummyNotifications()
//    }
//
//    private fun loadDummyNotifications() {
//        val dummyNotifications = mutableListOf<HealthNotification>()
//        val calendar = Calendar.getInstance()
//
//        // Critical notification - current time
//        dummyNotifications.add(
//            HealthNotification(
//                id = UUID.randomUUID().toString(),
//                title = "Critical Low Oxygen",
//                message = "Critical oxygen drop, please contact your doctor immediately!",
//                timestamp = calendar.time,
//                severity = NotificationSeverity.CRITICAL
//            )
//        )
//
//        // Move calendar back 2 hours
//        calendar.add(Calendar.HOUR, -2)
//
//        // Asthma notification
//        dummyNotifications.add(
//            HealthNotification(
//                id = UUID.randomUUID().toString(),
//                title = "Asthma Detected",
//                message = "Asthma detected, calling nearby hospital...",
//                timestamp = calendar.time,
//                severity = NotificationSeverity.CRITICAL
//            )
//        )
//
//        // Move calendar back 8 hours
//        calendar.add(Calendar.HOUR, -8)
//
//        // Warning notification
//        dummyNotifications.add(
//            HealthNotification(
//                id = UUID.randomUUID().toString(),
//                title = "High Heart Rate",
//                message = "Your heart rate has been above normal for 45 minutes. Consider resting.",
//                timestamp = calendar.time,
//                severity = NotificationSeverity.WARNING,
//                isRead = true
//            )
//        )
//
//        // Move calendar back 1 day
//        calendar.add(Calendar.DAY_OF_YEAR, -1)
//
//        // Info notification
//        dummyNotifications.add(
//            HealthNotification(
//                id = UUID.randomUUID().toString(),
//                title = "Battery Low",
//                message = "Your device battery is at 15%. Please charge soon.",
//                timestamp = calendar.time,
//                severity = NotificationSeverity.INFO,
//                isRead = true
//            )
//        )
//
//        // Move calendar back 2 days
//        calendar.add(Calendar.DAY_OF_YEAR, -2)
//
//        // Info notification
//        dummyNotifications.add(
//            HealthNotification(
//                id = UUID.randomUUID().toString(),
//                title = "Weekly Report Ready",
//                message = "Your weekly health report is ready. Tap to view details.",
//                timestamp = calendar.time,
//                severity = NotificationSeverity.INFO,
//                isRead = true
//            )
//        )
//
//        _notifications.value = dummyNotifications
//    }
//
//    // Function to mark notification as read
//    fun markAsRead(notificationId: String) {
//        val currentList = _notifications.value?.toMutableList() ?: mutableListOf()
//        val updatedList = currentList.map { notification ->
//            if (notification.id == notificationId) {
//                notification.copy(isRead = true)
//            } else {
//                notification
//            }
//        }
//        _notifications.value = updatedList
//    }
//}
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