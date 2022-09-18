package ggack.agitpos.ui.view.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import ggack.agitpos.R
import ggack.agitpos.databinding.FragmentMainMasterBinding
import ggack.agitpos.ui.view.activity.MainActivity
import ggack.agitpos.ui.viewmodel.MainViewModel

class MainMasterFragment : Fragment() {
    private var binding : FragmentMainMasterBinding? = null
    private lateinit var viewModel : MainViewModel

    override fun onCreateView(inflater : LayoutInflater, container : ViewGroup?, savedInstanceState : Bundle?) : View? {
        binding = FragmentMainMasterBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]
        binding?.tvName?.setOnClickListener {
            (activity as MainActivity).startFragment(UserListFragment())
        }
        binding?.btnMenu?.setOnClickListener {
            (activity as MainActivity).startFragment(DepositFragment())
        }
        binding?.tvMoney?.setOnClickListener {
            viewModel.checkDeposits(requireContext())
        }
    }
}