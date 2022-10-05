package ggack.agitpos.ui.view.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.crashlytics.internal.model.CrashlyticsReport
import ggack.agitpos.data.firebase.FirebaseComm
import ggack.agitpos.data.model.UserModel
import ggack.agitpos.databinding.FragmentItemsBinding
import ggack.agitpos.databinding.FragmentUserListBinding
import ggack.agitpos.listener.ListListener
import ggack.agitpos.ui.adapter.UserAdapter
import ggack.agitpos.ui.viewmodel.MainViewModel
import ggack.agitpos.utils.SimpleEditDialog
import ggack.agitpos.utils.SimpleListDialog
import ggack.agitpos.utils.Utils
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class ItemListFragment : Fragment() {
    private var binding : FragmentItemsBinding? = null
    private var adapter : UserAdapter? = null
    private lateinit var viewModel : MainViewModel
    var mapUserList = mapOf<String, Map<String, Any>>()
    var mapHistoryByName = mutableMapOf<String, MutableList<String>>()
    override fun onCreateView(inflater : LayoutInflater, container : ViewGroup?, savedInstanceState : Bundle?) : View? {
        binding = FragmentItemsBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]
//        binding?.btnAddUser?.setOnClickListener {
//            viewModel.popupAddUser(requireContext())
//        }
        initObserver()
        initAdapter()
        viewModel.refreshUser()
    }

    fun initObserver() {
        viewModel.liveUserData.observe(viewLifecycleOwner) {
            it?.let {
                mapUserList = it
                adapter?.map = it
                adapter?.keys = it.keys.toList().sorted()
                adapter?.notifyDataSetChanged()
            }
        }
        viewModel.liveHistoryByName.observe(viewLifecycleOwner) {
            it?.keys?.forEach { key ->
                it[key]?.let {list ->
                    val newList = mutableListOf<String>()
                    list.forEach { item ->
                        val date = Utils.getDate(item.date).format(DateTimeFormatter.ofPattern("dd일 hh:mm:ss"))
                        var text = "$date : [${item.kind}]"
                        if(item.money != 0)
                            text += " [금액 : ${item.money}]"
                        if(item.item?.keys != null && item.item?.keys?.isNotEmpty() == true)
                            text += " ${item.item?.keys}"
                        if(item.etc.isNotEmpty())
                            text += " [기타 : ${item.etc}]"
                        newList.add(text)
                    }
                    mapHistoryByName[key] = newList
                }
            }
        }
    }

    fun initAdapter() {
        //binding?.recyclerView?.layoutManager = LinearLayoutManager(requireContext())
        adapter = UserAdapter(requireContext(), mapUserList, object : ListListener {
            override fun onRemove(key: String, item: Any?) {
                super.onRemove(key, item)
                AlertDialog.Builder(requireContext())
                    .setMessage("[$key] 를 삭제합니까?")
                    .setPositiveButton("네") { _, _ ->
                        viewModel.removeUser(key)
                    }
                    .setNegativeButton("아니오") { _, _ ->
                    }.create().show()
            }

            override fun onChange(key: String, item: Any?) {
                super.onChange(key, item)
                viewModel.popUpChangeMoney(requireContext(),key, (item as UserModel?)?.money?:0)
            }

            override fun onRecord(key: String, item: Any?) {
                super.onRecord(key, item)
                val dlg = mapHistoryByName[key]?.let { SimpleListDialog(requireContext(), it) {} }
                dlg?.setListMargin(0)
                dlg?.setListTextSize(10)
                dlg?.show()
            }

            override fun onMember(key: String, item: Any?, b: Boolean) {
                super.onMember(key, item, b)
                FirebaseComm.setMembership(key, if(b) LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM")) else "")
            }
        })
        //binding?.recyclerView?.adapter = adapter
    }
}