package ggack.agitpos.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.activityViewModels
import androidx.navigation.Navigation
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import ggack.agitpos.R
import ggack.agitpos.data.model.networkmodel.AccountModel
import ggack.agitpos.databinding.FragmentMainMasterBinding
import ggack.agitpos.ui.dialog.AccountDialog
import ggack.agitpos.utils.Utils

@AndroidEntryPoint
class MainMasterFragment : Fragment() {
    private var binding : FragmentMainMasterBinding? = null
    private val viewModel by activityViewModels<MainViewModel>()
    var restart = false

    override fun onCreateView(inflater : LayoutInflater, container : ViewGroup?, savedInstanceState : Bundle?) : View? {
        binding = FragmentMainMasterBinding.inflate(inflater, container, false)
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
        viewModel.liveAccount.observe(viewLifecycleOwner) {

        }
    }
}