package ggack.agitpos.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import ggack.agitpos.R
import ggack.agitpos.data.firebase.FirebaseComm
import ggack.agitpos.data.model.StatisticsItemModel
import ggack.agitpos.data.model.StatisticsModel
import ggack.agitpos.data.model.StatisticsMonthModel
import ggack.agitpos.data.model.networkmodel.FromMap
import ggack.agitpos.data.model.networkmodel.HistoryModel
import ggack.agitpos.data.model.networkmodel.ItemModel
import ggack.agitpos.databinding.FragmentMainClientBinding
import ggack.agitpos.databinding.FragmentMasterStatisticsBinding
import ggack.agitpos.ui.adapter.StatisticsDailyAdapter
import ggack.agitpos.ui.adapter.StatisticsItemAdapter
import ggack.agitpos.ui.adapter.StatisticsMonthAdapter
import ggack.agitpos.utils.Constants
import ggack.agitpos.utils.Utils
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@AndroidEntryPoint
class StatisticsFragment : Fragment() {
    @Inject lateinit var firebaseComm: FirebaseComm
    private var binding : FragmentMasterStatisticsBinding? = null
    private val viewModel by activityViewModels<MainViewModel>()
    private val listMonthData : MutableList<StatisticsMonthModel> = mutableListOf()
    private val listDateData : MutableList<StatisticsModel> = mutableListOf()
    private val listItemData : MutableList<StatisticsItemModel> = mutableListOf()
    var currentSel = 0
    override fun onCreateView(inflater : LayoutInflater, container : ViewGroup?, savedInstanceState : Bundle?) : View? {
        binding = FragmentMasterStatisticsBinding.inflate(inflater, container, false)
        binding?.vm = viewModel
        binding?.lifecycleOwner = viewLifecycleOwner
        return binding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initObserver()
        initViews()

        currentSel = 1
        binding?.recyclerView?.layoutManager = LinearLayoutManager(requireContext())
        binding?.recyclerView?.adapter = StatisticsMonthAdapter(listMonthData)
    }
    private fun initViews() {
        binding?.swipeRefreshLayout?.setOnRefreshListener {
            viewModel.refreshStatistics()
            binding?.swipeRefreshLayout?.isRefreshing = false
        }
    }

    private fun initObserver() {
        firebaseComm.liveHistory.observe(viewLifecycleOwner) {
            it?.let {
                setDateData(it)
                setMonthData(it)
                setItemData(it)
                binding?.recyclerView?.adapter?.notifyDataSetChanged()
            }
        }

        viewModel.liveEvent.observe(viewLifecycleOwner) {
            when(it?.action) {
                Constants.ACTION_TOAST -> {
                    Utils.toast(requireContext(), it.param as String)
                }
                Constants.ACTION_SELECT_STATISTICS_DAY -> {
                    currentSel = 0
                    binding?.recyclerView?.layoutManager = LinearLayoutManager(requireContext())
                    binding?.recyclerView?.adapter = StatisticsDailyAdapter(listDateData)
                }
                Constants.ACTION_SELECT_STATISTICS_ITEM -> {
                    currentSel = 2
                    binding?.recyclerView?.layoutManager = LinearLayoutManager(requireContext())
                    binding?.recyclerView?.adapter = StatisticsItemAdapter(listItemData)
                }
                Constants.ACTION_SELECT_STATISTICS_MONTH -> {
                    currentSel = 1
                    binding?.recyclerView?.layoutManager = LinearLayoutManager(requireContext())
                    binding?.recyclerView?.adapter = StatisticsMonthAdapter(listMonthData)
                }
            }
        }
    }
    private fun setMonthData(data : Map<String, HistoryModel>) {
        val map : MutableMap<String, StatisticsMonthModel> = hashMapOf()
        data.forEach {
            val dateTime = Utils.getDate(it.value.date)
            val kind = it.value.kind
            var money = it.value.money
            val strMonth = dateTime.format(DateTimeFormatter.ofPattern("yyyyMM"))
            val month = dateTime.toLocalDate()
            if(!map.containsKey(strMonth))
                map[strMonth] = StatisticsMonthModel(month, 0, 0)
            val items = FromMap.toItemModels(it.value.item?: hashMapOf())
            items.forEach {model->
                if(model.name.contains("정산")) {
                    money -= model.price
                }
            }
            when(kind) {
                "입금" -> map[strMonth]!!.deposit += money
                "결제" -> map[strMonth]!!.payment += money
            }
        }
        listMonthData.clear()
        listMonthData.addAll(map.values.toList().sortedBy { it.date })
    }
    private fun setDateData(data : Map<String, HistoryModel>) {
        val map : MutableMap<String, StatisticsModel> = hashMapOf()
        data.forEach {
            val dateTime = Utils.getDate(it.value.date)
            val kind = it.value.kind
            var money = it.value.money
            val strDate = dateTime.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
            val date = dateTime.toLocalDate()
            if(!map.containsKey(strDate))
                map[strDate] = StatisticsModel(date, 0, 0)
            val items = FromMap.toItemModels(it.value.item?: hashMapOf())
            items.forEach {model->
                if(model.name.contains("정산")) {
                    money -= model.price
                }
            }

            when(kind) {
                "입금" -> map[strDate]!!.deposit += money
                "결제" -> map[strDate]!!.payment += money
            }
        }
        listDateData.clear()
        listDateData.addAll(map.values.toList().sortedBy { it.date })
    }
    private fun setItemData(data : Map<String, HistoryModel>) {
        val map : MutableMap<String, StatisticsItemModel> = hashMapOf()
        data.forEach {
            val dateTime = Utils.getDate(it.value.date)
            val items = FromMap.toItemModels(it.value.item?: hashMapOf())
            val strMonth = dateTime.format(DateTimeFormatter.ofPattern("yyyyMM"))
            val month = dateTime.toLocalDate()
            if(!map.containsKey(strMonth))
                map[strMonth] = StatisticsItemModel(month, mutableListOf(), 0)
            val list = map[strMonth]?.items?.toMutableList()?: mutableListOf()
            items.forEach {model ->
                if(!model.name.contains("정산") && !model.name.contains("출첵")) {
                    val i = list.contains(model.name)
                    if (i == -1) {
                        map[strMonth]?.items?.add(model)
                    } else {
                        map[strMonth]?.items?.get(i)!!.cnt += model.cnt
                        map[strMonth]?.items?.get(i)!!.price += model.price
                    }
                }
            }
        }
        listItemData.clear()
        listItemData.addAll(map.values.toList().sortedBy { it.date })
    }

    fun MutableList<ItemModel>.contains(key : String) : Int {
        for((i, model) in this.withIndex()) {
            if(model.name == key)
                return i
        }
        return -1
    }
}