package com.example.percobaan6.ui.notifications

import java.util.*

data class HealthNotification(
    val id: String,
    val title: String,
    val message: String,
    val timestamp: Date,
    val severity: NotificationSeverity,
    val isRead: Boolean = false
)

enum class NotificationSeverity {
    CRITICAL,
    WARNING,
    INFO
}
//package com.example.percobaan6.ui.notifications
//
//class NotificationModels {
//}