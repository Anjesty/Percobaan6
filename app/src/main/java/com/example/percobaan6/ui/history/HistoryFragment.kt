package com.example.percobaan6.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.percobaan6.databinding.FragmentHistoryBinding
import com.example.percobaan6.ui.home.HomeViewModel

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private val homeViewModel: HomeViewModel by lazy {
        ViewModelProvider(requireActivity()).get(HomeViewModel::class.java)
    }

    // Perubahan: Adapter dibuat sekali sebagai properti kelas
    private lateinit var historyAdapter: HistoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        val root: View = binding.root

        setupRecyclerView()
        observeHistoryData()

        return root
    }

    private fun setupRecyclerView() {
        // Inisialisasi adapter di sini, hanya sekali
        historyAdapter = HistoryAdapter()
        binding.recyclerViewHistory.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = historyAdapter
        }
    }

    private fun observeHistoryData() {
        binding.textHistory.text = "Health Parameters History"

        homeViewModel.dailyHealthHistory.observe(viewLifecycleOwner) { healthRecords ->
            // Perubahan: Gunakan submitList untuk update yang efisien
            // Tidak perlu lagi .reversed(), urutan dari ViewModel sudah benar
            historyAdapter.submitList(healthRecords)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
//package com.example.percobaan6.ui.history HAMPIR BENER 2222222222222222222
//
//import android.os.Bundle
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import androidx.fragment.app.Fragment
//import androidx.lifecycle.ViewModelProvider
//import androidx.recyclerview.widget.LinearLayoutManager
//import com.example.percobaan6.databinding.FragmentHistoryBinding
//import com.example.percobaan6.ui.home.HomeViewModel
//
//class HistoryFragment : Fragment() {
//
//    private var _binding: FragmentHistoryBinding? = null
//    private val binding get() = _binding!!
//
//    // Menggunakan HomeViewModel yang sama dengan Activity dan HomeFragment
//    private val homeViewModel: HomeViewModel by lazy {
//        ViewModelProvider(requireActivity()).get(HomeViewModel::class.java)
//    }
//
//    override fun onCreateView(
//        inflater: LayoutInflater,
//        container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View {
//        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
//        val root: View = binding.root
//
//        // Mengatur heading text
//        binding.textHistory.text= "Health Parameters History"
//
//        // Setup RecyclerView
//        val recyclerView = binding.recyclerViewHistory
//        recyclerView.layoutManager = LinearLayoutManager(context)
//
//        // Amati data riwayat dari HomeViewModel
//        homeViewModel.dailyHealthHistory.observe(viewLifecycleOwner) { healthRecords ->
//            // Balik urutan list agar "Today" selalu di atas jika ada data lain
//            recyclerView.adapter = HistoryAdapter(healthRecords.reversed())
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
//package com.example.percobaan6.ui.history NIERRRRRRR
//
//import android.os.Bundle
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import androidx.fragment.app.Fragment
//import androidx.lifecycle.ViewModelProvider
//import androidx.recyclerview.widget.LinearLayoutManager
//import com.example.percobaan6.databinding.FragmentHistoryBinding
//import java.text.SimpleDateFormat
//import java.util.*
//
//class HistoryFragment : Fragment() {
//
//    private var _binding: FragmentHistoryBinding? = null
//    private val binding get() = _binding!!
//
//    override fun onCreateView(
//        inflater: LayoutInflater,
//        container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View {
//        val medicalHistoryViewModel =
//            ViewModelProvider(this).get(HistoryViewModel::class.java)
//
//        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
//        val root: View = binding.root
//
//        // Mengatur heading text
//        medicalHistoryViewModel.text.observe(viewLifecycleOwner) {
//            binding.textHistory.text = it
//        }
//
//        // Setup RecyclerView
//        val recyclerView = binding.recyclerViewHistory
//        recyclerView.layoutManager = LinearLayoutManager(context)
//
//        // Mendapatkan data dummy untuk 6 hari terakhir
//        val healthRecords = generateDummyData()
//
//        // Set adapter untuk RecyclerView
//        recyclerView.adapter = HistoryAdapter(healthRecords)
//
//        return root
//    }
//
//    private fun generateDummyData(): List<DailyHealthRecord> {
//        val healthRecords = mutableListOf<DailyHealthRecord>()
//
//        // Format tanggal
//        val dateFormat = SimpleDateFormat("EEEE, MMM d", Locale.getDefault())
//        val calendar = Calendar.getInstance()
//
//        // Buat data untuk hari ini dan 5 hari sebelumnya
//        for (i in 0 until 6) {
//            val date = if (i == 0) "Today" else dateFormat.format(calendar.time)
//
//            // Data Heart Rate
//            val heartRate = HealthParameter(
//                min = "${60 + (Math.random() * 10).toInt()} bpm",
//                max = "${100 + (Math.random() * 40).toInt()} bpm",
//                time = String.format("%02d:%02d", (6 + (Math.random() * 12).toInt()), (Math.random() * 59).toInt()),
//                abnormalPeriod = "${(Math.random() * 30).toInt()} minutes"
//            )
//
//            // Data Oxygen Level
//            val oxygenLevel = HealthParameter(
//                min = "${90 + (Math.random() * 5).toInt()}%",
//                max = "${95 + (Math.random() * 5).toInt()}%",
//                time = String.format("%02d:%02d", (6 + (Math.random() * 12).toInt()), (Math.random() * 59).toInt()),
//                abnormalPeriod = "${(Math.random() * 15).toInt()} minutes"
//            )
//
//            // Data Breath Rate
//            val breathRate = HealthParameter(
//                min = "${10 + (Math.random() * 6).toInt()} bpm",
//                max = "${16 + (Math.random() * 10).toInt()} bpm",
//                time = String.format("%02d:%02d", (6 + (Math.random() * 12).toInt()), (Math.random() * 59).toInt()),
//                abnormalPeriod = "${(Math.random() * 20).toInt()} minutes"
//            )
//
//            // Tambahkan record ke list
//            healthRecords.add(
//                DailyHealthRecord(
//                    date = date,
//                    heartRate = heartRate,
//                    oxygenLevel = oxygenLevel,
//                    breathRate = breathRate
//                )
//            )
//
//            // Kurangi satu hari untuk record selanjutnya
//            calendar.add(Calendar.DAY_OF_YEAR, -1)
//        }
//
//        return healthRecords
//    }
//
//    override fun onDestroyView() {
//        super.onDestroyView()
//        _binding = null
//    }
//}
//package com.example.percobaan6.ui.history
//
//class HistoryFragment {
//}