package com.example.percobaan6.ui.home

import android.animation.ValueAnimator
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.percobaan6.databinding.FragmentHomeBinding
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var heartRateChart: LineChart
    private lateinit var spo2Chart: LineChart

    // Menggunakan 'by lazy' dan 'requireActivity()' agar ViewModel sama dengan yang ada di MainActivity
    private val homeViewModel: HomeViewModel by lazy {
        ViewModelProvider(requireActivity()).get(HomeViewModel::class.java)
    }

    private var heartRateCounter = 0
    private var spo2Counter = 0
    private val maxDataPoints = 20

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        initializeCharts()
        observeViewModel()

        return root
    }

    private fun initializeCharts() {
        heartRateChart = binding.heartRateChart
        spo2Chart = binding.spo2Chart

        binding.breathRateChart.visibility = View.GONE
        binding.skinTempChart.visibility = View.GONE
        binding.edaChart.visibility = View.GONE

        setupChart(heartRateChart, "Heart Rate (BPM)", Color.rgb(255, 102, 102))
        setupChart(spo2Chart, "SPO2 (%)", Color.rgb(102, 178, 255))

        clearCharts()
    }

    private fun setupChart(chart: LineChart, label: String, color: Int) {
        chart.apply {
            description.isEnabled = false
            setTouchEnabled(false)
            isDragEnabled = false
            setScaleEnabled(false)
            setPinchZoom(false)
            setDrawGridBackground(false)

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                labelCount = 5
                textColor = Color.rgb(62, 44, 129)
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return "${value.toInt()}s"
                    }
                }
            }

            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = Color.rgb(230, 230, 230)
                textColor = Color.rgb(62, 44, 129)
            }

            axisRight.isEnabled = false

            legend.apply {
                textColor = Color.rgb(62, 44, 129)
                textSize = 12f
            }
        }

        val entries = ArrayList<Entry>()
        val dataSet = LineDataSet(entries, label).apply {
            this.color = color
            setCircleColor(color)
            lineWidth = 2f
            circleRadius = 3f
            setDrawCircleHole(false)
            valueTextSize = 0f
            setDrawFilled(true)
            fillAlpha = 50
            fillColor = color
        }

        chart.data = LineData(dataSet)
        chart.invalidate()
    }

    private fun clearCharts() {
        clearChart(heartRateChart)
        clearChart(spo2Chart)
        heartRateCounter = 0
        spo2Counter = 0
    }

    private fun clearChart(chart: LineChart) {
        val data = chart.data
        if (data != null) {
            val dataSet = data.getDataSetByIndex(0) as LineDataSet
            dataSet.clear()
            data.notifyDataChanged()
            chart.notifyDataSetChanged()
            chart.invalidate()
        }
    }

    private fun observeViewModel() {
        homeViewModel.currentHealthData.observe(viewLifecycleOwner) { healthData ->
            if (healthData != null) {
                updateChartData(heartRateChart, heartRateCounter, healthData.heartRate)
                updateChartData(spo2Chart, spo2Counter, healthData.spo2)

                binding.currentHeartRateValue.text = healthData.heartRate.toInt().toString()
                binding.currentSpo2Value.text = healthData.spo2.toInt().toString()

                heartRateCounter++
                spo2Counter++
            } else {
                binding.currentHeartRateValue.text = "--"
                binding.currentSpo2Value.text = "--"
                clearCharts()
            }
        }

        homeViewModel.connectionStatus.observe(viewLifecycleOwner) { status ->
            binding.connectionStatusText.text = status
        }

        homeViewModel.isConnected.observe(viewLifecycleOwner) { isConnected ->
            val indicatorColor = if (isConnected) "#4CAF50" else "#FF9800"
            binding.connectionIndicator.backgroundTintList =
                android.content.res.ColorStateList.valueOf(Color.parseColor(indicatorColor))

            if (!isConnected) {
                clearCharts()
            }
        }

        homeViewModel.healthAlerts.observe(viewLifecycleOwner) { alerts ->
            if (alerts.isNotEmpty()) {
                val criticalAlerts =
                    alerts.filter { it.status == com.example.percobaan6.ui.history.AlertLevel.CRITICAL }
                val warningAlerts =
                    alerts.filter { it.status == com.example.percobaan6.ui.history.AlertLevel.WARNING }

                if (criticalAlerts.isNotEmpty() || warningAlerts.isNotEmpty()) {
                    binding.healthAlertCard.visibility = View.VISIBLE
                    val alertMessage = (criticalAlerts + warningAlerts).joinToString("\n") { it.message }
                    binding.healthAlertText.text = alertMessage
                } else {
                    binding.healthAlertCard.visibility = View.GONE
                }
            } else {
                binding.healthAlertCard.visibility = View.GONE
            }
        }
    }

    private fun updateChartData(chart: LineChart, counter: Int, value: Float) {
        val data = chart.data ?: return
        val dataSet = data.getDataSetByIndex(0) as LineDataSet

        dataSet.addEntry(Entry(counter.toFloat(), value))

        if (dataSet.entryCount > maxDataPoints) {
            dataSet.removeFirst()

            for (i in 0 until dataSet.entryCount) {
                val entry = dataSet.getEntryForIndex(i)
                entry.x = i.toFloat()
            }
        }

        data.notifyDataChanged()
        chart.notifyDataSetChanged()

        chart.setVisibleXRangeMaximum(maxDataPoints.toFloat())
        chart.moveViewToX(dataSet.entryCount.toFloat())

        animateChartUpdate(chart)
    }

    private fun animateChartUpdate(chart: LineChart) {
        val animator = ValueAnimator.ofFloat(0f, 1f)
        animator.duration = 300
        animator.addUpdateListener {
            chart.invalidate()
        }
        animator.start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
//package com.example.percobaan6.ui.home NIERRRRR
//
//import android.animation.ValueAnimator
//import android.graphics.Color
//import android.os.Bundle
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import androidx.fragment.app.Fragment
//import androidx.lifecycle.ViewModelProvider
//import com.example.percobaan6.databinding.FragmentHomeBinding
//import com.github.mikephil.charting.charts.LineChart
//import com.github.mikephil.charting.components.XAxis
//import com.github.mikephil.charting.data.Entry
//import com.github.mikephil.charting.data.LineData
//import com.github.mikephil.charting.data.LineDataSet
//import com.github.mikephil.charting.formatter.ValueFormatter
//
//class HomeFragment : Fragment() {
//
//    private var _binding: FragmentHomeBinding? = null
//    private val binding get() = _binding!!
//
//    private lateinit var heartRateChart: LineChart
//    private lateinit var spo2Chart: LineChart
//    private lateinit var homeViewModel: HomeViewModel
//
//    private var heartRateCounter = 0
//    private var spo2Counter = 0
//    private val maxDataPoints = 20
//
//    override fun onCreateView(
//        inflater: LayoutInflater,
//        container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View {
//        homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)
//        _binding = FragmentHomeBinding.inflate(inflater, container, false)
//        val root: View = binding.root
//
//        initializeCharts()
//        observeViewModel()
//
//        return root
//    }
//
//    private fun initializeCharts() {
//        heartRateChart = binding.heartRateChart
//        spo2Chart = binding.spo2Chart
//
//        binding.breathRateChart.visibility = View.GONE
//        binding.skinTempChart.visibility = View.GONE
//        binding.edaChart.visibility = View.GONE
//
//        setupChart(heartRateChart, "Heart Rate (BPM)", Color.rgb(255, 102, 102))
//        setupChart(spo2Chart, "SPO2 (%)", Color.rgb(102, 178, 255))
//
//        clearCharts()
//    }
//
//    private fun setupChart(chart: LineChart, label: String, color: Int) {
//        chart.apply {
//            description.isEnabled = false
//            setTouchEnabled(false)
//            isDragEnabled = false
//            setScaleEnabled(false)
//            setPinchZoom(false)
//            setDrawGridBackground(false)
//
//            xAxis.apply {
//                position = XAxis.XAxisPosition.BOTTOM
//                setDrawGridLines(false)
//                granularity = 1f
//                labelCount = 5
//                textColor = Color.rgb(62, 44, 129)
//                valueFormatter = object : ValueFormatter() {
//                    override fun getFormattedValue(value: Float): String {
//                        return "${value.toInt()}s"
//                    }
//                }
//            }
//
//            axisLeft.apply {
//                setDrawGridLines(true)
//                gridColor = Color.rgb(230, 230, 230)
//                textColor = Color.rgb(62, 44, 129)
//            }
//
//            axisRight.isEnabled = false
//
//            legend.apply {
//                textColor = Color.rgb(62, 44, 129)
//                textSize = 12f
//            }
//        }
//
//        val entries = ArrayList<Entry>()
//        val dataSet = LineDataSet(entries, label).apply {
//            this.color = color
//            setCircleColor(color)
//            lineWidth = 2f
//            circleRadius = 3f
//            setDrawCircleHole(false)
//            valueTextSize = 0f
//            setDrawFilled(true)
//            fillAlpha = 50
//            fillColor = color
//        }
//
//        chart.data = LineData(dataSet)
//        chart.invalidate()
//    }
//
//    private fun clearCharts() {
//        clearChart(heartRateChart)
//        clearChart(spo2Chart)
//        heartRateCounter = 0
//        spo2Counter = 0
//    }
//
//    private fun clearChart(chart: LineChart) {
//        val data = chart.data
//        if (data != null) {
//            val dataSet = data.getDataSetByIndex(0) as LineDataSet
//            dataSet.clear()
//            data.notifyDataChanged()
//            chart.notifyDataSetChanged()
//            chart.invalidate()
//        }
//    }
//
//    private fun observeViewModel() {
//        homeViewModel.currentHealthData.observe(viewLifecycleOwner) { healthData ->
//            if (healthData != null) {
//                updateChartData(heartRateChart, heartRateCounter, healthData.heartRate)
//                updateChartData(spo2Chart, spo2Counter, healthData.spo2)
//
//                binding.currentHeartRateValue.text = healthData.heartRate.toInt().toString()
//                binding.currentSpo2Value.text = healthData.spo2.toInt().toString()
//
//                heartRateCounter++
//                spo2Counter++
//            } else {
//                binding.currentHeartRateValue.text = "--"
//                binding.currentSpo2Value.text = "--"
//                clearCharts()
//            }
//        }
//
//        homeViewModel.connectionStatus.observe(viewLifecycleOwner) { status ->
//            binding.connectionStatusText.text = status
//        }
//
//        homeViewModel.isConnected.observe(viewLifecycleOwner) { isConnected ->
//            val indicatorColor = if (isConnected) "#4CAF50" else "#FF9800"
//            binding.connectionIndicator.backgroundTintList =
//                android.content.res.ColorStateList.valueOf(Color.parseColor(indicatorColor))
//
//            if (!isConnected) {
//                clearCharts()
//            }
//        }
//
//        homeViewModel.healthAlerts.observe(viewLifecycleOwner) { alerts ->
//            if (alerts.isNotEmpty()) {
//                val criticalAlerts = alerts.filter { it.status == com.example.percobaan6.ui.history.AlertLevel.CRITICAL }
//                val warningAlerts = alerts.filter { it.status == com.example.percobaan6.ui.history.AlertLevel.WARNING }
//
//                if (criticalAlerts.isNotEmpty() || warningAlerts.isNotEmpty()) {
//                    binding.healthAlertCard.visibility = View.VISIBLE
//                    val alertMessage = (criticalAlerts + warningAlerts).joinToString("\n") { it.message }
//                    binding.healthAlertText.text = alertMessage
//                } else {
//                    binding.healthAlertCard.visibility = View.GONE
//                }
//            } else {
//                binding.healthAlertCard.visibility = View.GONE
//            }
//        }
//    }
//
//    private fun updateChartData(chart: LineChart, counter: Int, value: Float) {
//        val data = chart.data ?: return
//        val dataSet = data.getDataSetByIndex(0) as LineDataSet
//
//        dataSet.addEntry(Entry(counter.toFloat(), value))
//
//        if (dataSet.entryCount > maxDataPoints) {
//            dataSet.removeFirst()
//
//            for (i in 0 until dataSet.entryCount) {
//                val entry = dataSet.getEntryForIndex(i)
//                entry.x = i.toFloat()
//            }
//        }
//
//        data.notifyDataChanged()
//        chart.notifyDataSetChanged()
//
//        chart.setVisibleXRangeMaximum(maxDataPoints.toFloat())
//        chart.moveViewToX(dataSet.entryCount.toFloat())
//
//        animateChartUpdate(chart)
//    }
//
//    private fun animateChartUpdate(chart: LineChart) {
//        val animator = ValueAnimator.ofFloat(0f, 1f)
//        animator.duration = 300
//        animator.addUpdateListener {
//            chart.invalidate()
//        }
//        animator.start()
//    }
//
//    override fun onDestroyView() {
//        super.onDestroyView()
//        _binding = null
//    }
//}
//package com.example.percobaan6.ui.home
//
//import android.animation.ValueAnimator
//import android.graphics.Color
//import android.os.Bundle
//import android.os.Handler
//import android.os.Looper
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import androidx.fragment.app.Fragment
//import androidx.lifecycle.ViewModelProvider
//import com.example.percobaan6.databinding.FragmentHomeBinding
//import com.github.mikephil.charting.charts.LineChart
//import com.github.mikephil.charting.components.XAxis
//import com.github.mikephil.charting.data.Entry
//import com.github.mikephil.charting.data.LineData
//import com.github.mikephil.charting.data.LineDataSet
//import com.github.mikephil.charting.formatter.ValueFormatter
//
//class HomeFragment : Fragment() {
//
//    private var _binding: FragmentHomeBinding? = null
//    private val binding get() = _binding!!
//
//    private lateinit var heartRateChart: LineChart
//    private lateinit var spo2Chart: LineChart
//    private lateinit var homeViewModel: HomeViewModel
//
//    private var heartRateCounter = 0
//    private var spo2Counter = 0
//
//    private val maxDataPoints = 20
//
//    override fun onCreateView(
//        inflater: LayoutInflater,
//        container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View {
//        homeViewModel = ViewModelProvider(requireActivity()).get(HomeViewModel::class.java)
//
//        _binding = FragmentHomeBinding.inflate(inflater, container, false)
//        val root: View = binding.root
//
//        initializeCharts()
//        observeViewModel()
//
//        return root
//    }
//
//    private fun initializeCharts() {
//        heartRateChart = binding.heartRateChart
//        spo2Chart = binding.spo2Chart
//
//        binding.breathRateChart.visibility = View.GONE
//        binding.skinTempChart.visibility = View.GONE
//        binding.edaChart.visibility = View.GONE
//
//        setupChart(heartRateChart, "Heart Rate (BPM)", Color.rgb(255, 102, 102))
//        setupChart(spo2Chart, "SPO2 (%)", Color.rgb(102, 178, 255))
//    }
//
//    private fun setupChart(chart: LineChart, label: String, color: Int) {
//        chart.apply {
//            description.isEnabled = false
//            setTouchEnabled(false)
//            isDragEnabled = false
//            setScaleEnabled(false)
//            setPinchZoom(false)
//            setDrawGridBackground(false)
//
//            xAxis.apply {
//                position = XAxis.XAxisPosition.BOTTOM
//                setDrawGridLines(false)
//                granularity = 1f
//                labelCount = 5
//                textColor = Color.rgb(62, 44, 129)
//                valueFormatter = object : ValueFormatter() {
//                    override fun getFormattedValue(value: Float): String {
//                        return "${value.toInt()}s"
//                    }
//                }
//            }
//
//            axisLeft.apply {
//                setDrawGridLines(true)
//                gridColor = Color.rgb(230, 230, 230)
//                textColor = Color.rgb(62, 44, 129)
//            }
//
//            axisRight.isEnabled = false
//
//            legend.apply {
//                textColor = Color.rgb(62, 44, 129)
//                textSize = 12f
//            }
//        }
//
//        val entries = ArrayList<Entry>()
//        val dataSet = LineDataSet(entries, label).apply {
//            this.color = color
//            setCircleColor(color)
//            lineWidth = 2f
//            circleRadius = 3f
//            setDrawCircleHole(false)
//            valueTextSize = 0f
//            setDrawFilled(true)
//            fillAlpha = 50
//            fillColor = color
//        }
//
//        chart.data = LineData(dataSet)
//        chart.invalidate()
//    }
//
//    private fun observeViewModel() {
//        homeViewModel.currentHealthData.observe(viewLifecycleOwner) { healthData ->
//            healthData?.let {
//                updateChartData(heartRateChart, heartRateCounter, it.heartRate)
//                updateChartData(spo2Chart, spo2Counter, it.spo2)
//
//                binding.currentHeartRateValue.text = it.heartRate.toInt().toString()
//                binding.currentSpo2Value.text = it.spo2.toInt().toString()
//
//                heartRateCounter++
//                spo2Counter++
//            }
//        }
//
//        homeViewModel.connectionStatus.observe(viewLifecycleOwner) { status ->
//            binding.connectionStatusText.text = status
//            updateConnectionIndicator(status)
//        }
//
//        homeViewModel.healthAlerts.observe(viewLifecycleOwner) { alerts ->
//            handleHealthAlerts(alerts)
//        }
//    }
//
//    private fun updateConnectionIndicator(status: String) {
//        val color = when {
//            status.contains("Connected") -> "#4CAF50"
//            status.contains("Connecting") -> "#FF9800"
//            else -> "#F44336"
//        }
//        binding.connectionIndicator.setBackgroundTintList(
//            android.content.res.ColorStateList.valueOf(Color.parseColor(color))
//        )
//    }
//
//    private fun handleHealthAlerts(alerts: List<com.example.percobaan6.ui.history.HealthStatus>) {
//        val criticalAlerts = alerts.filter { it.status == com.example.percobaan6.ui.history.AlertLevel.CRITICAL }
//        val warningAlerts = alerts.filter { it.status == com.example.percobaan6.ui.history.AlertLevel.WARNING }
//
//        if (criticalAlerts.isNotEmpty() || warningAlerts.isNotEmpty()) {
//            val alertMessage = when {
//                criticalAlerts.isNotEmpty() -> criticalAlerts.first().message
//                else -> warningAlerts.first().message
//            }
//            binding.healthAlertText.text = alertMessage
//            binding.healthAlertCard.visibility = View.VISIBLE
//        } else {
//            binding.healthAlertCard.visibility = View.GONE
//        }
//    }
//
//    private fun updateChartData(chart: LineChart, counter: Int, value: Float) {
//        val data = chart.data ?: return
//        val dataSet = data.getDataSetByIndex(0) as LineDataSet
//
//        dataSet.addEntry(Entry(counter.toFloat(), value))
//
//        if (dataSet.entryCount > maxDataPoints) {
//            dataSet.removeFirst()
//
//            for (i in 0 until dataSet.entryCount) {
//                val entry = dataSet.getEntryForIndex(i)
//                entry.x = i.toFloat()
//            }
//        }
//
//        data.notifyDataChanged()
//        chart.notifyDataSetChanged()
//
//        chart.setVisibleXRangeMaximum(maxDataPoints.toFloat())
//        chart.moveViewToX(dataSet.entryCount.toFloat())
//
//        animateChartUpdate(chart)
//    }
//
//    private fun animateChartUpdate(chart: LineChart) {
//        val animator = ValueAnimator.ofFloat(0f, 1f)
//        animator.duration = 300
//        animator.addUpdateListener {
//            chart.invalidate()
//        }
//        animator.start()
//    }
//
//    override fun onDestroyView() {
//        super.onDestroyView()
//        _binding = null
//    }
//}
//package com.example.percobaan6.ui.home
//
//import android.animation.ValueAnimator
//import android.graphics.Color
//import android.os.Bundle
//import android.os.Handler
//import android.os.Looper
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import androidx.fragment.app.Fragment
//import androidx.lifecycle.ViewModelProvider
//import com.example.percobaan6.databinding.FragmentHomeBinding
//import com.github.mikephil.charting.charts.LineChart
//import com.github.mikephil.charting.components.XAxis
//import com.github.mikephil.charting.data.Entry
//import com.github.mikephil.charting.data.LineData
//import com.github.mikephil.charting.data.LineDataSet
//import com.github.mikephil.charting.formatter.ValueFormatter
//
//class HomeFragment : Fragment() {
//
//    private var _binding: FragmentHomeBinding? = null
//    private val binding get() = _binding!!
//
//    private lateinit var heartRateChart: LineChart
//    private lateinit var spo2Chart: LineChart
//    private lateinit var homeViewModel: HomeViewModel
//
//    private val handler = Handler(Looper.getMainLooper())
//    private val updateInterval = 1000L
//    private var isUpdating = false
//
//    private var heartRateCounter = 0
//    private var spo2Counter = 0
//
//    private val maxDataPoints = 20
//
//    override fun onCreateView(
//        inflater: LayoutInflater,
//        container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View {
//        homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)
//
//        _binding = FragmentHomeBinding.inflate(inflater, container, false)
//        val root: View = binding.root
//
//        initializeCharts()
//        observeViewModel()
//        startRealTimeUpdates()
//
//        return root
//    }
//
//    private fun initializeCharts() {
//        heartRateChart = binding.heartRateChart
//        spo2Chart = binding.spo2Chart
//
//        // Hide unused charts
//        binding.breathRateChart.visibility = View.GONE
//        binding.skinTempChart.visibility = View.GONE
//        binding.edaChart.visibility = View.GONE
//
//        setupChart(heartRateChart, "Heart Rate (BPM)", Color.rgb(255, 102, 102))
//        setupChart(spo2Chart, "SPO2 (%)", Color.rgb(102, 178, 255))
//    }
//
//    private fun setupChart(chart: LineChart, label: String, color: Int) {
//        chart.apply {
//            description.isEnabled = false
//            setTouchEnabled(false)
//            isDragEnabled = false
//            setScaleEnabled(false)
//            setPinchZoom(false)
//            setDrawGridBackground(false)
//
//            xAxis.apply {
//                position = XAxis.XAxisPosition.BOTTOM
//                setDrawGridLines(false)
//                granularity = 1f
//                labelCount = 5
//                textColor = Color.rgb(62, 44, 129)
//                valueFormatter = object : ValueFormatter() {
//                    override fun getFormattedValue(value: Float): String {
//                        return "${value.toInt()}s"
//                    }
//                }
//            }
//
//            axisLeft.apply {
//                setDrawGridLines(true)
//                gridColor = Color.rgb(230, 230, 230)
//                textColor = Color.rgb(62, 44, 129)
//            }
//
//            axisRight.isEnabled = false
//
//            legend.apply {
//                textColor = Color.rgb(62, 44, 129)
//                textSize = 12f
//            }
//        }
//
//        val entries = ArrayList<Entry>()
//        val dataSet = LineDataSet(entries, label).apply {
//            this.color = color
//            setCircleColor(color)
//            lineWidth = 2f
//            circleRadius = 3f
//            setDrawCircleHole(false)
//            valueTextSize = 0f
//            setDrawFilled(true)
//            fillAlpha = 50
//            fillColor = color
//        }
//
//        chart.data = LineData(dataSet)
//        chart.invalidate()
//    }
//
//    private fun observeViewModel() {
//        // Observe real-time health data from ViewModel
//        homeViewModel.currentHealthData.observe(viewLifecycleOwner) { healthData ->
//            healthData?.let {
//                // Update charts (sudah ada)
//                updateChartData(heartRateChart, heartRateCounter, it.heartRate)
//                updateChartData(spo2Chart, spo2Counter, it.spo2)
//
//                // YANG MISSING: Update current values display
//                binding.currentHeartRateValue.text = it.heartRate.toInt().toString()
//                binding.currentSpo2Value.text = it.spo2.toInt().toString()
//
//                heartRateCounter++
//                spo2Counter++
//            }
//        }
//
//        // Observe connection status
//        homeViewModel.connectionStatus.observe(viewLifecycleOwner) { status ->
//            // You can update UI to show connection status if needed
//            // binding.connectionStatusText.text = status
//        }
//
//        // Observe health alerts
//        homeViewModel.healthAlerts.observe(viewLifecycleOwner) { alerts ->
//            // Handle health alerts if needed
//            val heartRateAlert = alerts.find { it.parameter == "Heart Rate" }
//            val spo2Alert = alerts.find { it.parameter == "SPO2" }
//
//            // You can show alerts in UI if needed
//        }
//    }
//
//    private fun startRealTimeUpdates() {
//        isUpdating = true
//        // No need for manual updates as we're observing ViewModel data
//    }
//
//    private fun updateChartData(chart: LineChart, counter: Int, value: Float) {
//        val data = chart.data ?: return
//        val dataSet = data.getDataSetByIndex(0) as LineDataSet
//
//        dataSet.addEntry(Entry(counter.toFloat(), value))
//
//        if (dataSet.entryCount > maxDataPoints) {
//            dataSet.removeFirst()
//
//            for (i in 0 until dataSet.entryCount) {
//                val entry = dataSet.getEntryForIndex(i)
//                entry.x = i.toFloat()
//            }
//        }
//
//        data.notifyDataChanged()
//        chart.notifyDataSetChanged()
//
//        chart.setVisibleXRangeMaximum(maxDataPoints.toFloat())
//        chart.moveViewToX(dataSet.entryCount.toFloat())
//
//        animateChartUpdate(chart)
//    }
//
//    private fun animateChartUpdate(chart: LineChart) {
//        val animator = ValueAnimator.ofFloat(0f, 1f)
//        animator.duration = 300
//        animator.addUpdateListener {
//            chart.invalidate()
//        }
//        animator.start()
//    }
//
//    override fun onDestroyView() {
//        super.onDestroyView()
//        isUpdating = false
//        handler.removeCallbacksAndMessages(null)
//        _binding = null
//    }
//
//    override fun onPause() {
//        super.onPause()
//        isUpdating = false
//    }
//
//    override fun onResume() {
//        super.onResume()
//        isUpdating = true
//    }
//}
//package com.example.percobaan6.ui.home
//
//import android.animation.ValueAnimator
//import android.graphics.Color
//import android.os.Bundle
//import android.os.Handler
//import android.os.Looper
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import androidx.fragment.app.Fragment
//import androidx.lifecycle.ViewModelProvider
//import com.example.percobaan6.databinding.FragmentHomeBinding
//import com.github.mikephil.charting.charts.LineChart
//import com.github.mikephil.charting.components.XAxis
//import com.github.mikephil.charting.data.Entry
//import com.github.mikephil.charting.data.LineData
//import com.github.mikephil.charting.data.LineDataSet
//import com.github.mikephil.charting.formatter.ValueFormatter
//import kotlin.random.Random
//
//class HomeFragment : Fragment() {
//
//    private var _binding: FragmentHomeBinding? = null
//    private val binding get() = _binding!!
//
//    private lateinit var heartRateChart: LineChart
//    private lateinit var spo2Chart: LineChart
//    private lateinit var breathRateChart: LineChart
//    private lateinit var skinTempChart: LineChart
//    private lateinit var edaChart: LineChart
//
//    private val handler = Handler(Looper.getMainLooper())
//    private val updateInterval = 1000L // Update setiap 1 detik
//    private var isUpdating = false
//
//    // Data counters untuk X-axis
//    private var heartRateCounter = 0
//    private var spo2Counter = 0
//    private var breathRateCounter = 0
//    private var skinTempCounter = 0
//    private var edaCounter = 0
//
//    // Maximum data points to show
//    private val maxDataPoints = 20
//
//    override fun onCreateView(
//        inflater: LayoutInflater,
//        container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View {
//        val homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)
//
//        _binding = FragmentHomeBinding.inflate(inflater, container, false)
//        val root: View = binding.root
//
//        // Initialize charts
//        initializeCharts()
//
//        // Start real-time updates
//        startRealTimeUpdates()
//
//        return root
//    }
//
//    private fun initializeCharts() {
//        heartRateChart = binding.heartRateChart
//        spo2Chart = binding.spo2Chart
//        breathRateChart = binding.breathRateChart
//        skinTempChart = binding.skinTempChart
//        edaChart = binding.edaChart
//
//        // Setup Heart Rate Chart
//        setupChart(heartRateChart, "Heart Rate (BPM)", Color.rgb(255, 102, 102))
//
//        // Setup SPO2 Chart
//        setupChart(spo2Chart, "SPO2 (%)", Color.rgb(102, 178, 255))
//
//        // Setup Breath Rate Chart
//        setupChart(breathRateChart, "Breath Rate (BPM)", Color.rgb(102, 255, 178))
//
//        // Setup Skin Temperature Chart
//        setupChart(skinTempChart, "Skin Temperature (°C)", Color.rgb(255, 178, 102))
//
//        // Setup EDA Chart
//        setupChart(edaChart, "EDA (μS)", Color.rgb(178, 102, 255))
//    }
//
//    private fun setupChart(chart: LineChart, label: String, color: Int) {
//        chart.apply {
//            description.isEnabled = false
//            setTouchEnabled(false)
//            isDragEnabled = false
//            setScaleEnabled(false)
//            setPinchZoom(false)
//            setDrawGridBackground(false)
//
//            // X-axis setup
//            xAxis.apply {
//                position = XAxis.XAxisPosition.BOTTOM
//                setDrawGridLines(false)
//                granularity = 1f
//                labelCount = 5
//                textColor = Color.rgb(62, 44, 129)
//                valueFormatter = object : ValueFormatter() {
//                    override fun getFormattedValue(value: Float): String {
//                        return "${value.toInt()}s"
//                    }
//                }
//            }
//
//            // Y-axis setup
//            axisLeft.apply {
//                setDrawGridLines(true)
//                gridColor = Color.rgb(230, 230, 230)
//                textColor = Color.rgb(62, 44, 129)
//            }
//
//            axisRight.isEnabled = false
//
//            legend.apply {
//                textColor = Color.rgb(62, 44, 129)
//                textSize = 12f
//            }
//        }
//
//        // Initialize with empty data
//        val entries = ArrayList<Entry>()
//        val dataSet = LineDataSet(entries, label).apply {
//            this.color = color
//            setCircleColor(color)
//            lineWidth = 2f
//            circleRadius = 3f
//            setDrawCircleHole(false)
//            valueTextSize = 0f
//            setDrawFilled(true)
//            fillAlpha = 50
//            fillColor = color
//        }
//
//        chart.data = LineData(dataSet)
//        chart.invalidate()
//    }
//
//    private fun startRealTimeUpdates() {
//        isUpdating = true
//        updateCharts()
//    }
//
//    private fun updateCharts() {
//        if (!isUpdating || _binding == null) return
//
//        // Update Heart Rate (60-100 BPM normal range)
//        updateChartData(heartRateChart, heartRateCounter, generateHeartRateValue())
//        heartRateCounter++
//
//        // Update SPO2 (95-100% normal range)
//        updateChartData(spo2Chart, spo2Counter, generateSPO2Value())
//        spo2Counter++
//
//        // Update Breath Rate (12-20 BPM normal range)
//        updateChartData(breathRateChart, breathRateCounter, generateBreathRateValue())
//        breathRateCounter++
//
//        // Update Skin Temperature (36-37°C normal range)
//        updateChartData(skinTempChart, skinTempCounter, generateSkinTempValue())
//        skinTempCounter++
//
//        // Update EDA (1-20 μS typical range)
//        updateChartData(edaChart, edaCounter, generateEDAValue())
//        edaCounter++
//
//        // Schedule next update
//        handler.postDelayed({ updateCharts() }, updateInterval)
//    }
//
//    private fun updateChartData(chart: LineChart, counter: Int, value: Float) {
//        val data = chart.data ?: return
//        val dataSet = data.getDataSetByIndex(0) as LineDataSet
//
//        // Add new data point
//        dataSet.addEntry(Entry(counter.toFloat(), value))
//
//        // Remove old data points if exceeding maximum
//        if (dataSet.entryCount > maxDataPoints) {
//            dataSet.removeFirst()
//
//            // Adjust X values to maintain sliding window effect
//            for (i in 0 until dataSet.entryCount) {
//                val entry = dataSet.getEntryForIndex(i)
//                entry.x = i.toFloat()
//            }
//        }
//
//        // Update chart
//        data.notifyDataChanged()
//        chart.notifyDataSetChanged()
//
//        // Auto-scale and animate
//        chart.setVisibleXRangeMaximum(maxDataPoints.toFloat())
//        chart.moveViewToX(dataSet.entryCount.toFloat())
//
//        // Animate the chart update
//        animateChartUpdate(chart)
//    }
//
//    private fun animateChartUpdate(chart: LineChart) {
//        val animator = ValueAnimator.ofFloat(0f, 1f)
//        animator.duration = 300
//        animator.addUpdateListener {
//            chart.invalidate()
//        }
//        animator.start()
//    }
//
//    // Data generators with realistic variations
//    private fun generateHeartRateValue(): Float {
//        val baseValue = 75f
//        val variation = Random.nextFloat() * 20f - 10f // ±10 BPM variation
//        return (baseValue + variation).coerceIn(50f, 120f)
//    }
//
//    private fun generateSPO2Value(): Float {
//        val baseValue = 98f
//        val variation = Random.nextFloat() * 4f - 2f // ±2% variation
//        return (baseValue + variation).coerceIn(90f, 100f)
//    }
//
//    private fun generateBreathRateValue(): Float {
//        val baseValue = 16f
//        val variation = Random.nextFloat() * 6f - 3f // ±3 BPM variation
//        return (baseValue + variation).coerceIn(10f, 25f)
//    }
//
//    private fun generateSkinTempValue(): Float {
//        val baseValue = 36.5f
//        val variation = Random.nextFloat() * 1f - 0.5f // ±0.5°C variation
//        return (baseValue + variation).coerceIn(35f, 38f)
//    }
//
//    private fun generateEDAValue(): Float {
//        val baseValue = 10f
//        val variation = Random.nextFloat() * 8f - 4f // ±4 μS variation
//        return (baseValue + variation).coerceIn(1f, 25f)
//    }
//
//    override fun onDestroyView() {
//        super.onDestroyView()
//        isUpdating = false
//        handler.removeCallbacksAndMessages(null)
//        _binding = null
//    }
//
//    override fun onPause() {
//        super.onPause()
//        isUpdating = false
//    }
//
//    override fun onResume() {
//        super.onResume()
//        if (_binding != null && !isUpdating) {
//            startRealTimeUpdates()
//        }
//    }
//}
//package com.example.percobaan6.ui.home
//
//import android.os.Bundle
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.TextView
//import androidx.fragment.app.Fragment
//import androidx.lifecycle.ViewModelProvider
//import com.example.percobaan6.databinding.FragmentHomeBinding
//
//class HomeFragment : Fragment() {
//
//    private var _binding: FragmentHomeBinding? = null
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
//        val homeViewModel =
//            ViewModelProvider(this).get(HomeViewModel::class.java)
//
//        _binding = FragmentHomeBinding.inflate(inflater, container, false)
//        val root: View = binding.root
//
//        val textView: TextView = binding.textHome
//        homeViewModel.text.observe(viewLifecycleOwner) {
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