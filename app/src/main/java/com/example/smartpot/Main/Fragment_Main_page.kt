package com.example.smartpot.Main

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.smartpot.Fragment_Blank2
import com.example.smartpot.Main.Dictionary.Fragment_CustomDialog_hearhoya
import com.example.smartpot.Main.Dictionary.Fragment_main_Cactus
import com.example.smartpot.Main.Dictionary.Fragment_main_Fishbone
import com.example.smartpot.Main.Dictionary.Fragment_main_Haunted_house
import com.example.smartpot.Main.Dictionary.Fragment_main_Hearthoya
import com.example.smartpot.Main.Dictionary.Fragment_main_Stucky
import com.example.smartpot.R
import com.example.smartpot.hardtest
import com.example.smartpot.ai.AIActivity
import com.example.smartpot.iot.FirebaseManager
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class Fragment_Main_page: Fragment() {

    private lateinit var viewPager: ViewPager2
    private lateinit var addButton: Button
    private lateinit var addText: TextView
    private lateinit var addImage: ImageView
    private lateinit var indicatorLayout: LinearLayout
    private lateinit var chart: LineChart
    
    private val firebaseManager = FirebaseManager()
    private val fragments = ArrayList<Fragment>()

    class CardsPagerTransformerShift(
        private val baseElevation: Int,
        private val raisingElevation: Int,
        private val smallerScale: Float,
        private val startOffset: Float
    ) : ViewPager2.PageTransformer {
        override fun transformPage(page: View, position: Float) {
            val absPosition = Math.abs(position - startOffset)
            if (absPosition >= 1) {
                page.elevation = baseElevation.toFloat()
                page.scaleY = smallerScale
            } else {
                page.elevation = ((1 - absPosition) * raisingElevation + baseElevation).toFloat()
                page.scaleY = (smallerScale - 1) * absPosition + 1
            }
        }
    }

    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.page_main, container, false)
        viewPager = view.findViewById(R.id.viewPager)
        addButton = view.findViewById(R.id.addButton)
        addText = view.findViewById(R.id.addText)
        addImage = view.findViewById(R.id.addImage)
        indicatorLayout = view.findViewById(R.id.indicatorLayout)
        chart = view.findViewById(R.id.plant_water_chart)
        
        val pagerAdapter = ScreenSlidePagerAdapter(requireActivity())
        viewPager.adapter = pagerAdapter

        viewPager.adapter?.notifyItemInserted(0)
        viewPager.setCurrentItem(1, true)

        addButton.setOnClickListener {
            showAddButtonDialog()
        }

        sharedPreferences = requireContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val lastPageIndex = sharedPreferences.getInt("lastPageIndex", 0)
        viewPager.setCurrentItem(lastPageIndex, false)

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateIndicators(position)
                if (position == fragments.size - 1) {
                    addButton.visibility = View.VISIBLE
                    addText.visibility = View.VISIBLE
                    addImage.visibility = View.VISIBLE
                } else {
                    addButton.visibility = View.GONE
                    addText.visibility = View.GONE
                    addImage.visibility = View.GONE
                }
                sharedPreferences.edit().putInt("lastPageIndex", position).apply()
                updateButtonInCurrentFragment()
            }
        })

        initChart()
        observeIoTData(view)

        val viewPagerPadding = resources.getDimensionPixelSize(R.dimen.view_pager_padding)
        val screen = requireActivity().windowManager.defaultDisplay.width
        val startOffset = viewPagerPadding.toFloat() / (screen - 2 * viewPagerPadding)
        viewPager.setPageTransformer(CardsPagerTransformerShift(0, 50, 0.75f, startOffset))
        return view
    }

    private fun observeIoTData(rootView: View) {
        firebaseManager.observeSensorData { data ->
            data?.let {
                rootView.findViewById<TextView>(R.id.tv_main_today_temp)?.text = "${it.temperature} ℃"
                rootView.findViewById<TextView>(R.id.tv_main_today_water)?.text = "${it.soilMoisture.toInt()} %"
            }
        }
    }

    private fun showAddButtonDialog() {
        val items = arrayOf("하트호야", "스투키", "선인장", "피쉬본", "괴마옥", "기기 추가 (BT)", "AI 감정 인식 / 식물 진단")

        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("메뉴를 선택해주세요")
        builder.setItems(items) { _, which ->
            when (which) {
                0 -> addNewPage_Hearthoya()
                1 -> addNewPage_Stucky()
                2 -> addNewPage_Cactus()
                3 -> addNewPage_Fishbone()
                4 -> addNewPage_Haunted_house()
                5 -> startActivity(Intent(requireContext(), hardtest::class.java))
                6 -> startActivity(Intent(requireContext(), AIActivity::class.java))
            }
        }
        builder.create().show()
    }

    private fun addLimitLine() {
        val limitLine = LimitLine(50f)
        limitLine.lineColor = Color.parseColor("#B0E19C")
        limitLine.lineWidth = 1f
        limitLine.enableDashedLine(20f, 20f, 0f)
        limitLine.labelPosition = LimitLine.LimitLabelPosition.LEFT_TOP
        val yAxisLeft: YAxis = chart.axisLeft
        yAxisLeft.addLimitLine(limitLine)
        yAxisLeft.axisMinimum = 0f
        yAxisLeft.axisMaximum = 100f
    }

    private fun updateIndicators(currentPosition: Int) {
        val existingIndicatorCount = indicatorLayout.childCount
        for (i in fragments.indices) {
            val indicator: View
            if (i < existingIndicatorCount) {
                indicator = indicatorLayout.getChildAt(i)
            } else {
                indicator = View(context)
                val indicatorSize = resources.getDimensionPixelSize(R.dimen.indicator_size)
                val indicatorMargin = resources.getDimensionPixelSize(R.dimen.indicator_margin)
                val params = LinearLayout.LayoutParams(indicatorSize, indicatorSize)
                params.setMargins(indicatorMargin, 0, indicatorMargin, 0)
                indicator.layoutParams = params
                indicatorLayout.addView(indicator)
            }
            indicator.setBackgroundResource(
                if (i == currentPosition) R.drawable.icon_main_selected_indicator
                else R.drawable.icon_main_unselected_indiactor
            )
        }
    }

    fun addNewPage_Hearthoya() {
        if (fragments.lastOrNull() is Fragment_Blank2) {
            fragments.removeAt(fragments.size - 1)
            viewPager.adapter?.notifyItemRemoved(fragments.size - 1)
        }
        fragments.add(Fragment_main_Hearthoya.newInstance(fragments.size + 1))
        addNewPage2()
    }
    
    fun addNewPage_Stucky() {
        if (fragments.lastOrNull() is Fragment_Blank2) {
            fragments.removeAt(fragments.size - 1)
            viewPager.adapter?.notifyItemRemoved(fragments.size - 1)
        }
        fragments.add(Fragment_main_Stucky.newInstance(fragments.size + 1))
        addNewPage2()
    }

    fun addNewPage_Cactus() {
        if (fragments.lastOrNull() is Fragment_Blank2) {
            fragments.removeAt(fragments.size - 1)
            viewPager.adapter?.notifyItemRemoved(fragments.size - 1)
        }
        fragments.add(Fragment_main_Cactus.newInstance(fragments.size + 1))
        addNewPage2()
    }

    fun addNewPage_Fishbone() {
        if (fragments.lastOrNull() is Fragment_Blank2) {
            fragments.removeAt(fragments.size - 1)
            viewPager.adapter?.notifyItemRemoved(fragments.size - 1)
        }
        fragments.add(Fragment_main_Fishbone.newInstance(fragments.size + 1))
        addNewPage2()
    }

    fun addNewPage_Haunted_house() {
        if (fragments.lastOrNull() is Fragment_Blank2) {
            fragments.removeAt(fragments.size - 1)
            viewPager.adapter?.notifyItemRemoved(fragments.size - 1)
        }
        fragments.add(Fragment_main_Haunted_house.newInstance(fragments.size + 1))
        addNewPage2()
    }

    fun addNewPage2() {
        val currentPosition = viewPager.currentItem
        fragments.add(Fragment_Blank2.newInstance(fragments.size + 1))
        viewPager.adapter?.notifyItemInserted(currentPosition + 1)
        viewPager.setCurrentItem(0, true)
        updateButtonInCurrentFragment()
    }

    private fun initChart() {
        chart.description.isEnabled = false
        chart.setTouchEnabled(false)
        chart.legend.isEnabled = false
        val xAxis = chart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val calendar = Calendar.getInstance()
                calendar.add(Calendar.DAY_OF_MONTH, value.toInt() - 7)
                return SimpleDateFormat("MM/dd", Locale.getDefault()).format(calendar.time)
            }
        }
        val yAxisLeft: YAxis = chart.axisLeft
        yAxisLeft.axisMinimum = 0f
        yAxisLeft.axisMaximum = 100f
        yAxisLeft.setDrawLabels(false)
        yAxisLeft.setDrawGridLines(false)
        chart.axisRight.isEnabled = false
        addLimitLine()
        setChartData()
    }

    private fun initializeButtonInCurrentFragment() {
        val currentIndex = viewPager.currentItem
        if (currentIndex < 0 || currentIndex >= fragments.size) return
        val currentFragment = fragments[currentIndex]
        currentFragment.view?.findViewById<Button>(R.id.harthoya_grow)?.setOnClickListener {
            Fragment_CustomDialog_hearhoya().show(parentFragmentManager, "custom_dialog")
        }
    }

    private fun updateButtonInCurrentFragment() {
        initializeButtonInCurrentFragment()
    }

    private fun setChartData() {
        val entries = mutableListOf<Entry>()
        entries.add(Entry(1f, 30f))
        entries.add(Entry(2f, 80f))
        entries.add(Entry(3f, 66f))
        entries.add(Entry(4f, 59f))
        entries.add(Entry(5f, 49f))
        entries.add(Entry(6f, 47f))
        entries.add(Entry(7f, 40f))

        val dataSet = LineDataSet(entries, null)
        dataSet.color = Color.parseColor("#54B22D")
        dataSet.setDrawIcons(true)
        dataSet.lineWidth = 1f
        dataSet.setDrawValues(false)

        chart.data = LineData(dataSet)
        chart.invalidate()
    }

    private inner class ScreenSlidePagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
        override fun getItemCount(): Int = fragments.size
        override fun createFragment(position: Int): Fragment = fragments[position]
    }
}
