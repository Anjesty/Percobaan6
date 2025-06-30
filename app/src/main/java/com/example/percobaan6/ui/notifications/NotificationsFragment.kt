package com.example.percobaan6.ui.notifications

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.percobaan6.databinding.FragmentNotificationsBinding

class NotificationsFragment : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!

    // --- ▼▼▼ PERUBAHAN UTAMA DI SINI ▼▼▼ ---
    // Gunakan 'activityViewModels()' untuk mendapatkan ViewModel yang sama dengan MainActivity.
    // Ini adalah cara modern dan yang direkomendasikan.
    private val notificationsViewModel: NotificationsViewModel by activityViewModels()
    // --- ▲▲▲ AKHIR PERUBAHAN ▲▲▲ ---

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
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

        // Buat instance adapter dengan click handler
        val adapter = NotificationsAdapter { notification ->
            // Tandai notifikasi sebagai sudah dibaca saat diklik
            notificationsViewModel.markAsRead(notification.id)

            // Tampilkan toast atau lakukan aksi lain
            Toast.makeText(
                context,
                "Notification: ${notification.title}",
                Toast.LENGTH_SHORT
            ).show()
        }
        // Pasang adapter ke RecyclerView
        recyclerView.adapter = adapter

        // Observe data notifikasi
        notificationsViewModel.notifications.observe(viewLifecycleOwner) { notifications ->
            // Kirim list baru ke adapter
            adapter.submitList(notifications)

            // Tampilkan atau sembunyikan pesan "kosong"
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

// Jangan lupa untuk mengupdate NotificationsAdapter agar menggunakan ListAdapter
// Jika belum, berikut kodenya:
// File: NotificationsAdapter.kt
/*
package com.example.percobaan6.ui.notifications

// ... import lainnya ...
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.DiffUtil

class NotificationsAdapter(
    private val onNotificationClick: (HealthNotification) -> Unit
) : ListAdapter<HealthNotification, NotificationsAdapter.ViewHolder>(DiffCallback()) {

    // ... ViewHolder class ...

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // ... kode sama ...
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val notification = getItem(position)
        // ... logika binding sama ...
    }

    class DiffCallback : DiffUtil.ItemCallback<HealthNotification>() {
        override fun areItemsTheSame(oldItem: HealthNotification, newItem: HealthNotification): Boolean {
            return oldItem.id == newItem.id
        }
        override fun areContentsTheSame(oldItem: HealthNotification, newItem: HealthNotification): Boolean {
            return oldItem == newItem
        }
    }
}
*/
//package com.example.percobaan6.ui.notifications HAMPIR BENER 444444444444
//
//import android.os.Bundle
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.TextView
//import android.widget.Toast
//import androidx.fragment.app.Fragment
//import androidx.lifecycle.ViewModelProvider
//import androidx.recyclerview.widget.LinearLayoutManager
//import com.example.percobaan6.databinding.FragmentNotificationsBinding
//
//class NotificationsFragment : Fragment() {
//
//    private var _binding: FragmentNotificationsBinding? = null
//    private val binding get() = _binding!!
//    private lateinit var notificationsViewModel: NotificationsViewModel
//
//    override fun onCreateView(
//        inflater: LayoutInflater,
//        container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View {
//        notificationsViewModel =
//            ViewModelProvider(this).get(NotificationsViewModel::class.java)
//
//        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
//        val root: View = binding.root
//
//        // Setup header text
//        val textView: TextView = binding.textNotifications
//        notificationsViewModel.text.observe(viewLifecycleOwner) {
//            textView.text = it
//        }
//
//        // Setup RecyclerView
//        val recyclerView = binding.recyclerViewNotifications
//        recyclerView.layoutManager = LinearLayoutManager(context)
//
//        // Observe notifications data
//        notificationsViewModel.notifications.observe(viewLifecycleOwner) { notifications ->
//            // Set adapter for RecyclerView with click handler
//            recyclerView.adapter = NotificationsAdapter(notifications) { notification ->
//                // Mark notification as read when clicked
//                notificationsViewModel.markAsRead(notification.id)
//
//                // Show a toast with the notification title
//                Toast.makeText(
//                    context,
//                    "Notification: ${notification.title}",
//                    Toast.LENGTH_SHORT
//                ).show()
//
//                // In a real app, you might navigate to a detail view or perform an action
//            }
//
//            // Show or hide empty state
//            binding.emptyNotificationsView.visibility =
//                if (notifications.isEmpty()) View.VISIBLE else View.GONE
//        }
//
//        return root
//    }
//
//    override fun onDestroyView() {
//        super.onDestroyView()
//        _binding = null
//    }
//}
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