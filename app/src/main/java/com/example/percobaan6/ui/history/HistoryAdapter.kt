package com.example.percobaan6.ui.history

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.percobaan6.R

class HistoryAdapter(private val healthRecords: List<DailyHealthRecord>) :
    RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDate: TextView = view.findViewById(R.id.tvDate)

        // Heart Rate
        val tvHeartRateMinMax: TextView = view.findViewById(R.id.tvHeartRateMinMax)
        val tvHeartRateTime: TextView = view.findViewById(R.id.tvHeartRateTime)
        val tvHeartRateAbnormal: TextView = view.findViewById(R.id.tvHeartRateAbnormal)

        // Oxygen
        val tvOxygenMinMax: TextView = view.findViewById(R.id.tvOxygenMinMax)
        val tvOxygenTime: TextView = view.findViewById(R.id.tvOxygenTime)
        val tvOxygenAbnormal: TextView = view.findViewById(R.id.tvOxygenAbnormal)

        // Breath Rate
        val tvBreathRateMinMax: TextView = view.findViewById(R.id.tvBreathRateMinMax)
        val tvBreathRateTime: TextView = view.findViewById(R.id.tvBreathRateTime)
        val tvBreathRateAbnormal: TextView = view.findViewById(R.id.tvBreathRateAbnormal)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_medical_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val record = healthRecords[position]

        // Set date
        holder.tvDate.text = record.date

        // Set Heart Rate data
        holder.tvHeartRateMinMax.text = "Min: ${record.heartRate.min}, Max: ${record.heartRate.max}"
        holder.tvHeartRateTime.text = record.heartRate.time
        holder.tvHeartRateAbnormal.text = "Abnormal period: ${record.heartRate.abnormalPeriod}"

        // Set Oxygen data
        holder.tvOxygenMinMax.text = "Min: ${record.oxygenLevel.min}, Max: ${record.oxygenLevel.max}"
        holder.tvOxygenTime.text = record.oxygenLevel.time
        holder.tvOxygenAbnormal.text = "Abnormal period: ${record.oxygenLevel.abnormalPeriod}"

        // Set Breath Rate data
        holder.tvBreathRateMinMax.text = "Min: ${record.breathRate.min}, Max: ${record.breathRate.max}"
        holder.tvBreathRateTime.text = record.breathRate.time
        holder.tvBreathRateAbnormal.text = "Abnormal period: ${record.breathRate.abnormalPeriod}"
    }

    override fun getItemCount() = healthRecords.size
}
//package com.example.percobaan6.ui.history
//
//class HistoryAdapter {
//}