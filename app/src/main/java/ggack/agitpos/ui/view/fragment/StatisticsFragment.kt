package ggack.agitpos.ui.view.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import ggack.agitpos.data.firebase.FirebaseComm
import ggack.agitpos.data.model.StatisticsItemModel
import ggack.agitpos.data.model.StatisticsModel
import ggack.agitpos.data.model.networkmodel.HistoryModel
import ggack.agitpos.databinding.FragmentMasterStatisticsBinding
import ggack.agitpos.ui.viewmodel.MainViewModel
import ggack.agitpos.utils.Utils
import java.time.format.DateTimeFormatter

class StatisticsFragment : Fragment() {
    private var binding : FragmentMasterStatisticsBinding? = null
    private lateinit var viewModel : MainViewModel
    private val mapMonthData : MutableMap<String, StatisticsModel> = hashMapOf() // key : month
    private val mapDateData : MutableMap<String, MutableMap<String, StatisticsModel>> = hashMapOf() // month , date
    private val mapItemData : MutableMap<String, MutableMap<String, Int>> = hashMapOf()  // key : Date

    override fun onCreateView(inflater : LayoutInflater, container : ViewGroup?, savedInstanceState : Bundle?) : View? {
        binding = FragmentMasterStatisticsBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]
        initObserver()
        initButtons()
        initViews()
    }
    private fun initButtons() {

    }
    private fun initViews() {

    }

    private fun initObserver() {
        FirebaseComm.liveHistoryStatistics.observe(viewLifecycleOwner) {

        }
    }
    fun setMonthData(data : Map<String, HistoryModel>) {
        var moneyDeposit = 0
        var moneyPayment = 0
        var month = ""
        data.forEach {
            val dateTime = Utils.getDate(it.value.date)
            val kind = it.value.kind
            val money = it.value.money
            if(month == "")
                month = dateTime.format(DateTimeFormatter.ofPattern("yyyyMM"))
            when(kind) {
                "결제" -> moneyPayment += money
                "입금" -> moneyDeposit += money
            }
        }
        mapMonthData[month] = StatisticsModel(moneyDeposit, moneyPayment)
    }
    fun setDateData(data : Map<String, HistoryModel>) {
        val map : MutableMap<String, MutableMap<String, StatisticsModel>> = hashMapOf()
        data.forEach {
            val dateTime = Utils.getDate(it.value.date)
            val kind = it.value.kind
            val money = it.value.money
            val month = dateTime.format(DateTimeFormatter.ofPattern("yyyyMM"))
            val date = dateTime.format(DateTimeFormatter.ofPattern("dd"))
            if(map[month] == null) map[month] = hashMapOf()
            if(map[month]!![date] == null) map[month]!![date] = StatisticsModel(0,0)
            when(kind) {
                "결제" -> map[month]!![date]!!.payment += money
                "입금" -> map[month]!![date]!!.deposit += money
            }
        }
        map.forEach {
            mapDateData[it.key] = map[it.key]!!
        }
    }
    fun setItemData(data : Map<String, HistoryModel>) {
        val map : MutableMap<String, MutableMap<String, Int>> = hashMapOf()
        data.forEach {
            val dateTime = Utils.getDate(it.value.date)
            val kind = it.value.kind
            val money = it.value.money
            val month = dateTime.format(DateTimeFormatter.ofPattern("yyyyMM"))
            val date = dateTime.format(DateTimeFormatter.ofPattern("dd"))
            if(map[month] == null) map[month] = hashMapOf()
            if(kind == "결제") {
                it.value.item?.forEach {
                    if(map[month]!![it.key] == null) map[month]!![it.key] = 0
                    map[month]!![it.key]?.plus(1)
                }
            }
        }
        map.forEach {
            mapItemData[it.key] = map[it.key]!!
        }
    }
}