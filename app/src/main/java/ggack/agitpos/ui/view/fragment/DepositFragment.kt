package ggack.agitpos.ui.view.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import ggack.agitpos.data.model.DepositModel
import ggack.agitpos.data.model.UserModel
import ggack.agitpos.databinding.FragmentListBinding
import ggack.agitpos.listener.ListListener
import ggack.agitpos.ui.adapter.DepositAdapter
import ggack.agitpos.ui.adapter.UserAdapter
import ggack.agitpos.ui.viewmodel.MainViewModel

class DepositFragment : Fragment() {
    private var binding : FragmentListBinding? = null
    private var adapter : DepositAdapter? = null
    private lateinit var viewModel : MainViewModel
    var mapDeposit = mapOf<String, Map<String, Any>>()
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
        viewModel.refreshUser()
    }

    fun initObserver() {
        viewModel.liveDeposits.observe(viewLifecycleOwner) {
            it?.let {
                mapDeposit = it
                adapter?.keys = it.keys.toList()
                adapter?.notifyDataSetChanged()
            }
        }
    }

    fun initAdapter() {
        binding?.recyclerView?.layoutManager = LinearLayoutManager(requireContext())
        adapter = DepositAdapter(mapDeposit, object : ListListener {

        })
        binding?.recyclerView?.adapter = adapter
    }
}