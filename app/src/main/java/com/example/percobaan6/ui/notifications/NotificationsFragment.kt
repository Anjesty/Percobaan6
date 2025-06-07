package com.example.percobaan6.ui.notifications

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.percobaan6.databinding.FragmentNotificationsBinding

class NotificationsFragment : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!
    private lateinit var notificationsViewModel: NotificationsViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        notificationsViewModel =
            ViewModelProvider(this).get(NotificationsViewModel::class.java)

        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Setup header text
        val textView: TextView = binding.textNotifications
        notificationsViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }

        // Setup RecyclerView
        val recyclerView = binding.recyclerViewNotifications
        recyclerView.layoutManager = LinearLayoutManager(context)

        // Observe notifications data
        notificationsViewModel.notifications.observe(viewLifecycleOwner) { notifications ->
            // Set adapter for RecyclerView with click handler
            recyclerView.adapter = NotificationsAdapter(notifications) { notification ->
                // Mark notification as read when clicked
                notificationsViewModel.markAsRead(notification.id)

                // Show a toast with the notification title
                Toast.makeText(
                    context,
                    "Notification: ${notification.title}",
                    Toast.LENGTH_SHORT
                ).show()

                // In a real app, you might navigate to a detail view or perform an action
            }

            // Show or hide empty state
            binding.emptyNotificationsView.visibility =
                if (notifications.isEmpty()) View.VISIBLE else View.GONE
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
//package com.example.percobaan6.ui.notifications
//
//import android.os.Bundle
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.TextView
//import androidx.fragment.app.Fragment
//import androidx.lifecycle.ViewModelProvider
//import com.example.percobaan6.databinding.FragmentNotificationsBinding
//
//class NotificationsFragment : Fragment() {
//
//    private var _binding: FragmentNotificationsBinding? = null
//
//    // This property is only valid between onCreateView and
//    // onDestroyView.
//    private val binding get() = _binding!!
//
//    override fun onCreateView(
//        inflater: LayoutInflater,
//        container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View {
//        val notificationsViewModel =
//            ViewModelProvider(this).get(NotificationsViewModel::class.java)
//
//        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
//        val root: View = binding.root
//
//        val textView: TextView = binding.textNotifications
//        notificationsViewModel.text.observe(viewLifecycleOwner) {
//            textView.text = it
//        }
//        return root
//    }
//
//    override fun onDestroyView() {
//        super.onDestroyView()
//        _binding = null
//    }
//}