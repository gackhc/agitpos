package ggack.agitpos.ui.main

import Singleton.isDuplicated
import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import ggack.agitpos.data.firebase.FirebaseComm
import ggack.agitpos.data.model.networkmodel.DepositModel
import ggack.agitpos.data.model.networkmodel.FromMap
import ggack.agitpos.data.model.networkmodel.HistoryModel
import ggack.agitpos.databinding.FragmentListBinding
import ggack.agitpos.listener.ListListener
import ggack.agitpos.ui.adapter.DepositAdapter
import ggack.agitpos.domain.LedgerAction
import ggack.agitpos.domain.LedgerRequest
import java.util.UUID
import ggack.agitpos.utils.Constants
import ggack.agitpos.ui.dialog.SimpleDialog
import ggack.agitpos.ui.dialog.SimpleListDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class DepositFragment : Fragment() {
    @Inject lateinit var firebaseComm: FirebaseComm
    private var binding : FragmentListBinding? = null
    private var adapter : DepositAdapter? = null
    private val viewModel by activityViewModels<MainViewModel>()
    private var mapDeposit = mapOf<String, Map<String, Any>>()
    private var mMapName : Map<String, Map<String, Any>> = HashMap()
    private var saving = false
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
            }
        }
    }

    fun initAdapter() {
        binding?.recyclerView?.layoutManager = LinearLayoutManager(requireContext())
        adapter = DepositAdapter(mapDeposit, object : ListListener {
            override fun onClick(key: String, item: Any?) {
                super.onClick(key, item)
                val model = item as DepositModel
                val dlg = SimpleListDialog(requireContext(), mMapName.keys.toList().sorted()) { name ->
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
                            if(model.name.isNotEmpty()) {
                                if (mMapName.isDuplicated(key, model.name))
                                    Toast.makeText(
                                        requireContext(),
                                        "이름이 중복됩니다",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                else
                                    firebaseComm.addUser(model.name)
                            }
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
                            firebaseComm.removeDepositItem(key)
                        }
                    }
                }
                dlg.setText("삭제 하시겠습니까?")
                dlg.show()
            }
        })
        binding?.recyclerView?.adapter = adapter
    }
    fun addAll(originName: String, name: String) {
        if (saving) return
        val keys = mapDeposit.filterValues { it["name"] == originName }.keys.toList()
        if (keys.isEmpty()) return
        saving = true
        firebaseComm.submit(LedgerRequest(UUID.randomUUID().toString(), System.currentTimeMillis(),
            LedgerAction.AssignDeposits(name, keys))) { result ->
            saving = false
            if (binding == null) return@submit
            if (result.isSuccess) setExtraName(name, originName)
            else context?.let { Toast.makeText(it, result.exceptionOrNull()?.message ?: "입금 반영에 실패했습니다.", Toast.LENGTH_LONG).show() }
        }
    }
    fun setExtraName(key : String, name : String) {
        val dlg = SimpleDialog(requireContext(), SimpleDialog.TYPE_TWO_BUTTON) {
            when(it) {
                Activity.RESULT_OK -> {
                    if(name.isNotEmpty()) {
                        if (mMapName.isDuplicated(key, name))
                            Toast.makeText(requireContext(), "이름이 중복됩니다", Toast.LENGTH_SHORT).show()
                        else
                            firebaseComm.setExtraName(key, name)
                    }
                }
            }
        }
        dlg.setText("추가이름으로 등록하시겠습니까?")
        dlg.show()
    }

}
