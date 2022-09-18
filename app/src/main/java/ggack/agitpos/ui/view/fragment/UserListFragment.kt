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
import ggack.agitpos.databinding.FragmentUserListBinding
import ggack.agitpos.listener.ListListener
import ggack.agitpos.ui.adapter.UserAdapter
import ggack.agitpos.ui.viewmodel.MainViewModel
import ggack.agitpos.utils.SimpleEditDialog

class UserListFragment : Fragment() {
    private var binding : FragmentUserListBinding? = null
    private var adapter : UserAdapter? = null
    private lateinit var viewModel : MainViewModel
    var mapUserList = mapOf<String, Map<String, Any>>()
    override fun onCreateView(inflater : LayoutInflater, container : ViewGroup?, savedInstanceState : Bundle?) : View? {
        binding = FragmentUserListBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]
        binding?.btnAddUser?.setOnClickListener {
            viewModel.popupAddUser(requireContext())
        }
        initObserver()
        initAdapter()
        viewModel.refreshUser()
    }

    fun initObserver() {
        viewModel.liveUserData.observe(viewLifecycleOwner) {
            it?.let {
                mapUserList = it
                adapter?.map = it
                adapter?.keys = it.keys.toList()
                adapter?.notifyDataSetChanged()
            }
        }
    }

    fun initAdapter() {
        binding?.recyclerView?.layoutManager = LinearLayoutManager(requireContext())
        adapter = UserAdapter(mapUserList, object : ListListener {
            override fun onRemove(key: String, item: Any?) {
                super.onRemove(key, item)
                val dlg = AlertDialog.Builder(requireContext())
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
            }
        })
        binding?.recyclerView?.adapter = adapter
    }
}