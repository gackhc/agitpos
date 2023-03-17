package ggack.agitpos.ui.main

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.NavHostFragment
import dagger.hilt.android.AndroidEntryPoint
import ggack.agitpos.R
import ggack.agitpos.data.model.networkmodel.FromMap
import ggack.agitpos.databinding.ActivityMainBinding
import ggack.agitpos.ui.base.BaseActivity
import ggack.agitpos.ui.dialog.AccountDialog
import ggack.agitpos.ui.dialog.SimpleDialog
import ggack.agitpos.ui.login.LoginActivity
import ggack.agitpos.utils.Constants
import ggack.agitpos.utils.Utils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : BaseActivity() {
    private val viewModel by viewModels<MainViewModel>()
    private lateinit var binding : ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.lifecycleOwner = this

        viewModel.init(Utils.getPrefBoolean(this, "settingRestart", true))
        initObserver()
        viewModel.refreshUser()
    }
    fun initObserver() {
        viewModel.liveUserData.observe(this) {
            it?.let {
                val name = intent.getStringExtra("name")?:""
                if (name == "" || !it.contains(name)) {
                    Toast.makeText(this, "유저데이터를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                    onBackPressed()
                    return@observe
                }
                it[name]?.let {map ->
                    viewModel.setUser(name, FromMap.toUserModel(map))
                }
            }
        }
        viewModel.liveEvent.observe(this) { it ->
            val nav = supportFragmentManager?.findFragmentById(R.id.container) as NavHostFragment?
            when(it?.action) {
                Constants.ACTION_TOAST -> {
                    Utils.toast(this, it.param as String)
                }
                Constants.ACTION_START_FRAGMENT_DEPOSIT -> {
                    nav?.navController?.navigate(R.id.action_mainMasterFragment_to_depositFragment, null)
                }
                Constants.ACTION_START_FRAGMENT_ITEMS -> {
                    nav?.navController?.navigate(R.id.action_mainMasterFragment_to_itemListFragment, null)
                }
                Constants.ACTION_START_FRAGMENT_MASTER_CHARGE -> {
                    nav?.navController?.navigate(R.id.action_mainMasterFragment_to_masterChargeFragment, null)
                }
                Constants.ACTION_START_FRAGMENT_STATISTICS -> {
                    nav?.navController?.navigate(R.id.action_mainMasterFragment_to_statisticsFragment, null)
                }
                Constants.ACTION_START_FRAGMENT_USER -> {
                    nav?.navController?.navigate(R.id.action_mainMasterFragment_to_userListFragment, null)
                }
                Constants.ACTION_SET_MASTER_PIN -> {
                    popUpChangePin()
                }
                Constants.ACTION_SET_USER_ACCOUNT -> {
                    val dlg = AccountDialog(this) { account, name, kakaoPay ->
                        if(account.length < 8) {
                            Toast.makeText(this, "계좌번호를 확인 해 주세요", Toast.LENGTH_SHORT).show()
                            viewModel.setAccount(this, "", name, kakaoPay)
                        } else {
                            viewModel.setAccount(this, account, name, kakaoPay)
                        }
                    }
                    dlg.setText("입금계좌(카카오뱅크)를 숫자만 입력해주세요")
                    viewModel.liveAccount.value?.let{model ->
                        dlg.setEditText(model.account, model.name, model.kakaopay)
                    }
                    dlg.show()
                }
            }
        }
     }


    private fun popUpChangePin() {
        val editText = EditText(this)
        editText.inputType = InputType.TYPE_CLASS_NUMBER
        editText.textAlignment = View.TEXT_ALIGNMENT_CENTER
        editText.filters = arrayOf<InputFilter>(InputFilter.LengthFilter(4))
        val dlg = AlertDialog.Builder(this).setView(editText).setTitle("Pin 수정").setPositiveButton("OK"
        ) { _, _ ->
            if(editText.text.length != 4) {
                Toast.makeText(this, "짧습니다", Toast.LENGTH_SHORT).show()
            } else {
                try {
                    viewModel.changePin("*master", editText.text.toString())
                    Toast.makeText(this, "수정 성공", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this, "수정 실패", Toast.LENGTH_SHORT).show()
                }
            }
        }
        dlg.show()
    }
}