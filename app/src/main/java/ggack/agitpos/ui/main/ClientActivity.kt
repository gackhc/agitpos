package ggack.agitpos.ui.main

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.NavHostFragment
import dagger.hilt.android.AndroidEntryPoint
import ggack.agitpos.R
import ggack.agitpos.data.model.networkmodel.FromMap
import ggack.agitpos.databinding.ActivityClientBinding
import ggack.agitpos.databinding.ActivityMainBinding
import ggack.agitpos.ui.base.BaseActivity
import ggack.agitpos.ui.dialog.SimpleDialog
import ggack.agitpos.ui.login.LoginActivity
import ggack.agitpos.utils.Constants
import ggack.agitpos.utils.Utils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ClientActivity : BaseActivity() {
    private val viewModel by viewModels<MainViewModel>()
    private lateinit var binding : ActivityClientBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_client)
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
        viewModel.liveEvent.observe(this) {
            when(it?.action) {
                Constants.ACTION_TOAST -> {
                    Utils.toast(this, it.param as String)
                }
            }
        }
     }

    override fun onPause() {
        super.onPause()
        if(Utils.isAppIsInBackground(this)) {
            Log.e("debug", "onMoveToBackground")
            val coroutine = CoroutineScope(Dispatchers.Main).launch {
                delay(5000)
                baseContext.startActivity(Intent(baseContext, LoginActivity::class.java))
            }
            val dlg = SimpleDialog(baseContext, SimpleDialog.TYPE_ONE_BUTTON) {
                when (it) {
                    RESULT_OK -> {
                        coroutine.cancel()
                    }
                }
            }
            dlg.setText("앱 창을 다시 실행합니다. 재실행을 원치않는다면 5초안에 확인을 눌러주세요")
            dlg.show()
        }
    }
}