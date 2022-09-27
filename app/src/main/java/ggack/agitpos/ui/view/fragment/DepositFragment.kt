package ggack.agitpos.ui.view.fragment

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import ggack.agitpos.data.firebase.FirebaseComm
import ggack.agitpos.data.model.DepositModel
import ggack.agitpos.data.model.FromMap
import ggack.agitpos.data.model.HistoryModel
import ggack.agitpos.databinding.FragmentListBinding
import ggack.agitpos.listener.ListListener
import ggack.agitpos.ui.adapter.DepositAdapter
import ggack.agitpos.ui.viewmodel.MainViewModel
import ggack.agitpos.utils.Constants
import ggack.agitpos.utils.SimpleDialog
import ggack.agitpos.utils.SimpleListDialog
import ggack.agitpos.utils.Utils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class DepositFragment : Fragment() {
    private var binding : FragmentListBinding? = null
    private var adapter : DepositAdapter? = null
    private lateinit var viewModel : MainViewModel
    private var mapDeposit = mapOf<String, Map<String, Any>>()
    private var mMapName : Map<String, Map<String, Any>> = HashMap()
    private var mUserPrepared = false
    override fun onCreateView(inflater : LayoutInflater, container : ViewGroup?, savedInstanceState : Bundle?) : View? {
        binding = FragmentListBinding.inflate(inflater, container, false)
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
        initAdapter()
        viewModel.getDeposits()
    }

    fun initObserver() {
        viewModel.liveDeposits.observe(viewLifecycleOwner) {
            it?.let {
                mapDeposit = it
                adapter?.map = it
                adapter?.keys = it.keys.toMutableList().sorted().toMutableList()
                adapter?.notifyDataSetChanged()
            }
        }
        viewModel.liveUserData.observe(viewLifecycleOwner) {
            it?.let {
                mMapName = it
                mUserPrepared = true
            }
        }
    }

    fun initAdapter() {
        binding?.recyclerView?.layoutManager = LinearLayoutManager(requireContext())
        adapter = DepositAdapter(mapDeposit, object : ListListener {
            override fun onClick(key: String, item: Any?) {
                super.onClick(key, item)
                val model = item as DepositModel
                val dlg = SimpleListDialog(requireContext(), mMapName.keys.toList()) { name ->
                    addAll(model.name, name)
                }
                dlg.setBtnTxt("취소")
                dlg.show()
            }

            override fun onADD(key: String, item: Any?) {
                super.onADD(key, item)
                val model = item as DepositModel
                val dlg = SimpleDialog(requireContext(), SimpleDialog.TYPE_TWO_BUTTON) {
                    when (it) {
                        Activity.RESULT_OK -> {
                            mUserPrepared = false
                            FirebaseComm.addUser(model.name)
                        }
                    }
                }
                dlg.setText("사용자를 추가하겠습니까?")
                dlg.show()
            }

            override fun onRemove(key: String, item: Any?) {
                super.onRemove(key, item)
                val model = item as DepositModel
                val dlg = SimpleDialog(requireContext(), SimpleDialog.TYPE_TWO_BUTTON) {
                    when (it) {
                        Activity.RESULT_OK -> {
                            mUserPrepared = false
                            FirebaseComm.removeDepositItem(model.date.toString())
                        }
                    }
                }
                dlg.setText("삭제 하시겠습니까?")
                dlg.show()
            }
        })
        binding?.recyclerView?.adapter = adapter
    }
    fun addAll(originName : String, name : String) {
        CoroutineScope(Dispatchers.Main).launch {
            val keys = mutableListOf<String>()
            keys.addAll(adapter?.keys?: listOf())
            keys.forEach {date ->
                while(!mUserPrepared) {
                    delay(10)
                }
                mUserPrepared = false
                mapDeposit[date]?.let{map->
                    val m = FromMap.toDepositModel(map)
                    if(m.name == originName){
                        activity?.runOnUiThread {
                            setData(date, name, m)
                        }
                    }
                }
            }
        }
    }
    fun setData(key : String, name : String, model : DepositModel) {
        val user = FromMap.toUserModel(mMapName[name]!!)
        FirebaseComm.setMoney(name, model.money + user.money)
        FirebaseComm.addHistory(HistoryModel(
            model.date,
            name,
            Constants.HISTORY_DEPOSIT,
            model.money,
            mapOf(),
            model.name
        ))
        FirebaseComm.removeDepositItem(key)
        val pos = adapter?.keys?.indexOf(key)
        pos?.let{
            adapter?.keys?.removeAt(pos)
            adapter?.notifyItemRemoved(pos)
        }
    }
}