package com.example.percobaan6.ui.notifications

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.percobaan6.R
import java.text.SimpleDateFormat
import java.util.*

class NotificationsAdapter(
    private val notifications: List<HealthNotification>,
    private val onNotificationClick: (HealthNotification) -> Unit
) : RecyclerView.Adapter<NotificationsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardView: CardView = view.findViewById(R.id.cardNotification)
        val tvTitle: TextView = view.findViewById(R.id.tvNotificationTitle)
        val tvMessage: TextView = view.findViewById(R.id.tvNotificationMessage)
        val tvTime: TextView = view.findViewById(R.id.tvNotificationTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val notification = notifications[position]

        // Set text content
        holder.tvTitle.text = notification.title
        holder.tvMessage.text = notification.message

        // Format timestamp
        val dateFormat = SimpleDateFormat("HH:mm, dd MMM", Locale.getDefault())
        holder.tvTime.text = dateFormat.format(notification.timestamp)

        // Set card background color based on severity and read status
        when (notification.severity) {
            NotificationSeverity.CRITICAL -> {
                holder.cardView.setCardBackgroundColor(Color.parseColor("#FFEBEE")) // Light Red
                holder.tvTitle.setTextColor(Color.parseColor("#D32F2F")) // Dark Red
            }
            NotificationSeverity.WARNING -> {
                holder.cardView.setCardBackgroundColor(Color.parseColor("#FFF8E1")) // Light Yellow
                holder.tvTitle.setTextColor(Color.parseColor("#F57F17")) // Dark Orange
            }
            NotificationSeverity.INFO -> {
                holder.cardView.setCardBackgroundColor(Color.parseColor("#E8F5E9")) // Light Green
                holder.tvTitle.setTextColor(Color.parseColor("#2E7D32")) // Dark Green
            }
        }

        // Apply alpha if the notification is read
        if (notification.isRead) {
            holder.cardView.alpha = 0.7f
        } else {
            holder.cardView.alpha = 1.0f
        }

        // Set click listener
        holder.cardView.setOnClickListener {
            onNotificationClick(notification)
        }
    }

    override fun getItemCount() = notifications.size
}
//package com.example.percobaan6.ui.notifications
//
//class NotificationsAdapter {
//}