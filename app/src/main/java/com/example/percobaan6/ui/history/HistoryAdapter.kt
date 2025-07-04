package com.example.percobaan6.ui.history

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.percobaan6.R

// Perubahan: Menggunakan ListAdapter, bukan RecyclerView.Adapter biasa
class HistoryAdapter : ListAdapter<DailyHealthRecord, HistoryAdapter.ViewHolder>(DiffCallback()) {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val tvHeartRateMinMax: TextView = view.findViewById(R.id.tvHeartRateMinMax)
        val tvHeartRateTime: TextView = view.findViewById(R.id.tvHeartRateTime)
        val tvHeartRateAbnormal: TextView = view.findViewById(R.id.tvHeartRateAbnormal)
        val tvOxygenMinMax: TextView = view.findViewById(R.id.tvOxygenMinMax)
        val tvOxygenTime: TextView = view.findViewById(R.id.tvOxygenTime)
        val tvOxygenAbnormal: TextView = view.findViewById(R.id.tvOxygenAbnormal)

        fun bind(record: DailyHealthRecord) {
            tvDate.text = record.date

            // Tampilkan data Heart Rate atau sembunyikan detail jika tidak ada data
            tvHeartRateMinMax.text = "Min: ${record.heartRate.min} bpm, Max: ${record.heartRate.max} bpm"
            if (record.heartRate.time == "-") {
                tvHeartRateTime.visibility = View.GONE
                tvHeartRateAbnormal.visibility = View.GONE
            } else {
                tvHeartRateTime.visibility = View.VISIBLE
                tvHeartRateAbnormal.visibility = View.VISIBLE
                tvHeartRateTime.text = record.heartRate.time
                tvHeartRateAbnormal.text = "Abnormal period: ${record.heartRate.abnormalPeriod}"
            }

            // Tampilkan data Oxygen Level atau sembunyikan detail jika tidak ada data
            tvOxygenMinMax.text = "Min: ${record.oxygenLevel.min}%, Max: ${record.oxygenLevel.max}%"
            if (record.oxygenLevel.time == "-") {
                tvOxygenTime.visibility = View.GONE
                tvOxygenAbnormal.visibility = View.GONE
            } else {
                tvOxygenTime.visibility = View.VISIBLE
                tvOxygenAbnormal.visibility = View.VISIBLE
                tvOxygenTime.text = record.oxygenLevel.time
                tvOxygenAbnormal.text = "Abnormal period: ${record.oxygenLevel.abnormalPeriod}"
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_medical_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // Ambil item menggunakan getItem() dari ListAdapter
        val record = getItem(position)
        holder.bind(record)
    }

    // Perubahan: Kelas DiffCallback untuk perbandingan yang efisien
    class DiffCallback : DiffUtil.ItemCallback<DailyHealthRecord>() {
        override fun areItemsTheSame(oldItem: DailyHealthRecord, newItem: DailyHealthRecord): Boolean {
            // Item dianggap sama jika tanggalnya sama (ini adalah ID unik)
            return oldItem.date == newItem.date
        }

        override fun areContentsTheSame(oldItem: DailyHealthRecord, newItem: DailyHealthRecord): Boolean {
            // Konten dianggap sama jika semua data di dalamnya sama
            return oldItem == newItem
        }
    }
}
//package com.example.percobaan6.ui.history HAMPIR BENER 222222
//
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.TextView
//import androidx.recyclerview.widget.RecyclerView
//import com.example.percobaan6.R
//
//class HistoryAdapter(private val healthRecords: List<DailyHealthRecord>) :
//    RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {
//
//    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
//        val tvDate: TextView = view.findViewById(R.id.tvDate)
//
//        // Heart Rate
//        val tvHeartRateMinMax: TextView = view.findViewById(R.id.tvHeartRateMinMax)
//        val tvHeartRateTime: TextView = view.findViewById(R.id.tvHeartRateTime)
//        val tvHeartRateAbnormal: TextView = view.findViewById(R.id.tvHeartRateAbnormal)
//
//        // Oxygen
//        val tvOxygenMinMax: TextView = view.findViewById(R.id.tvOxygenMinMax)
//        val tvOxygenTime: TextView = view.findViewById(R.id.tvOxygenTime)
//        val tvOxygenAbnormal: TextView = view.findViewById(R.id.tvOxygenAbnormal)
//
//        // Bagian Breath Rate sudah tidak di-binding karena di-comment di XML
//    }
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
//        val view =
//            LayoutInflater.from(parent.context)
//                .inflate(R.layout.item_medical_history, parent, false)
//        return ViewHolder(view)
//    }
//
//    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
//        val record = healthRecords[position]
//
//        // Set date
//        holder.tvDate.text = record.date
//
//        // Set Heart Rate data, tampilkan "-" jika data tidak ada
//        holder.tvHeartRateMinMax.text = "Min: ${record.heartRate.min} bpm, Max: ${record.heartRate.max} bpm"
//        if (record.heartRate.time == "-") {
//            holder.tvHeartRateTime.visibility = View.GONE
//            holder.tvHeartRateAbnormal.visibility = View.GONE
//        } else {
//            holder.tvHeartRateTime.visibility = View.VISIBLE
//            holder.tvHeartRateAbnormal.visibility = View.VISIBLE
//            holder.tvHeartRateTime.text = record.heartRate.time
//            holder.tvHeartRateAbnormal.text = "Abnormal period: ${record.heartRate.abnormalPeriod}"
//        }
//
//        // Set Oxygen data, tampilkan "-" jika data tidak ada
//        holder.tvOxygenMinMax.text = "Min: ${record.oxygenLevel.min}%, Max: ${record.oxygenLevel.max}%"
//        if (record.oxygenLevel.time == "-") {
//            holder.tvOxygenTime.visibility = View.GONE
//            holder.tvOxygenAbnormal.visibility = View.GONE
//        } else {
//            holder.tvOxygenTime.visibility = View.VISIBLE
//            holder.tvOxygenAbnormal.visibility = View.VISIBLE
//            holder.tvOxygenTime.text = record.oxygenLevel.time
//            holder.tvOxygenAbnormal.text = "Abnormal period: ${record.oxygenLevel.abnormalPeriod}"
//        }
//
//        // Bagian Breath Rate tidak perlu di-set
//    }
//
//    override fun getItemCount() = healthRecords.size
//}
//package com.example.percobaan6.ui.history NIERRRRRRRRRRRRRRRRRRRRRR
//
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.TextView
//import androidx.recyclerview.widget.RecyclerView
//import com.example.percobaan6.R
//
//class HistoryAdapter(private val healthRecords: List<DailyHealthRecord>) :
//    RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {
//
//    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
//        val tvDate: TextView = view.findViewById(R.id.tvDate)
//
//        // Heart Rate
//        val tvHeartRateMinMax: TextView = view.findViewById(R.id.tvHeartRateMinMax)
//        val tvHeartRateTime: TextView = view.findViewById(R.id.tvHeartRateTime)
//        val tvHeartRateAbnormal: TextView = view.findViewById(R.id.tvHeartRateAbnormal)
//
//        // Oxygen
//        val tvOxygenMinMax: TextView = view.findViewById(R.id.tvOxygenMinMax)
//        val tvOxygenTime: TextView = view.findViewById(R.id.tvOxygenTime)
//        val tvOxygenAbnormal: TextView = view.findViewById(R.id.tvOxygenAbnormal)
//
//        // Breath Rate
//        val tvBreathRateMinMax: TextView = view.findViewById(R.id.tvBreathRateMinMax)
//        val tvBreathRateTime: TextView = view.findViewById(R.id.tvBreathRateTime)
//        val tvBreathRateAbnormal: TextView = view.findViewById(R.id.tvBreathRateAbnormal)
//    }
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
//        val view = LayoutInflater.from(parent.context)
//            .inflate(R.layout.item_medical_history, parent, false)
//        return ViewHolder(view)
//    }
//
//    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
//        val record = healthRecords[position]
//
//        // Set date
//        holder.tvDate.text = record.date
//
//        // Set Heart Rate data
//        holder.tvHeartRateMinMax.text = "Min: ${record.heartRate.min}, Max: ${record.heartRate.max}"
//        holder.tvHeartRateTime.text = record.heartRate.time
//        holder.tvHeartRateAbnormal.text = "Abnormal period: ${record.heartRate.abnormalPeriod}"
//
//        // Set Oxygen data
//        holder.tvOxygenMinMax.text = "Min: ${record.oxygenLevel.min}, Max: ${record.oxygenLevel.max}"
//        holder.tvOxygenTime.text = record.oxygenLevel.time
//        holder.tvOxygenAbnormal.text = "Abnormal period: ${record.oxygenLevel.abnormalPeriod}"
//
//        // Set Breath Rate data
//        holder.tvBreathRateMinMax.text = "Min: ${record.breathRate.min}, Max: ${record.breathRate.max}"
//        holder.tvBreathRateTime.text = record.breathRate.time
//        holder.tvBreathRateAbnormal.text = "Abnormal period: ${record.breathRate.abnormalPeriod}"
//    }
//
//    override fun getItemCount() = healthRecords.size
//}
//package com.example.percobaan6.ui.history
//
//class HistoryAdapter {
//}