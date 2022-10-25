package ggack.agitpos.ui.view.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import ggack.agitpos.data.model.networkmodel.AccountModel
import ggack.agitpos.databinding.FragmentMainMasterBinding
import ggack.agitpos.ui.view.activity.MainActivity
import ggack.agitpos.ui.viewmodel.MainViewModel
import ggack.agitpos.ui.view.dialog.AccountDialog
import ggack.agitpos.utils.Utils

class MainMasterFragment : Fragment() {
    private var binding : FragmentMainMasterBinding? = null
    private lateinit var viewModel : MainViewModel
    private var account : AccountModel? = null
    var restart = false

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
        binding?.btnUser?.setOnClickListener {
            (activity as MainActivity).addFragment(UserListFragment())
        }
        binding?.btnDeposit?.setOnClickListener {
            (activity as MainActivity).addFragment(DepositFragment())
        }
        binding?.btnAccount?.setOnClickListener {
            val dlg = AccountDialog(requireContext()) { account, name, kakaoPay ->
                if(account.length < 8) {
                    Toast.makeText(requireContext(), "계좌번호를 확인 해 주세요", Toast.LENGTH_SHORT).show()
                    viewModel.setAccount(requireContext(), "", name, kakaoPay)
                } else {
                    viewModel.setAccount(requireContext(), account, name, kakaoPay)
                }
            }
            dlg.setText("입금계좌(카카오뱅크)를 숫자만 입력해주세요")
            account?.let{
                dlg.setEditText(it.account, it.name, it.kakaopay)
            }
            dlg.show()
        }
        viewModel.liveAccount.observe(viewLifecycleOwner) {
            account = it
        }
        binding?.btnCharge?.setOnClickListener {
            (activity as MainActivity).addFragment(MasterChargeFragment())
        }
        binding?.btnStatistics?.setOnClickListener {
            (activity as MainActivity).addFragment(StatisticsFragment())
        }
        binding?.btnItemSetting?.setOnClickListener {
            (activity as MainActivity).addFragment(ItemListFragment())
        }

        restart = Utils.getPrefBoolean(requireContext(), "settingRestart", true)
        if(restart)
            binding?.btnAutoStart?.setText("자동 재시작 해제")
        else
            binding?.btnAutoStart?.setText("자동 재시작 설정")
        binding?.btnAutoStart?.setOnClickListener {
            restart = !restart
            Utils.setPref(requireContext(), "settingRestart", restart)
            if(restart)
                binding?.btnAutoStart?.setText("자동 재시작 해제")
            else
                binding?.btnAutoStart?.setText("자동 재시작 설정")
        }
    }
}