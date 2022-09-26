package ggack.agitpos.ui.view.activity

import android.Manifest
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import ggack.agitpos.R
import ggack.agitpos.data.firebase.FirebaseComm
import ggack.agitpos.data.model.FromMap
import ggack.agitpos.data.model.UserModel
import ggack.agitpos.data.sms.SmsManager
import ggack.agitpos.databinding.ActivityMainBinding
import ggack.agitpos.ui.base.BaseActivity
import ggack.agitpos.ui.view.fragment.MainClientFragment
import ggack.agitpos.ui.view.fragment.MainMasterFragment
import ggack.agitpos.ui.view.fragment.UserListFragment
import ggack.agitpos.ui.viewmodel.MainViewModel
import ggack.agitpos.utils.Permissions
import ggack.agitpos.utils.SimpleDialog
import ggack.agitpos.utils.Utils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : BaseActivity() {
    private val viewModel : MainViewModel by viewModels()
    private lateinit var binding : ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewModel.init()
        initObserver()
        if(intent.getBooleanExtra("isMaster", false)) {
            startFragment(MainMasterFragment())
        } else {
            startFragment(MainClientFragment())
        }
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
    }
    fun startFragment(f : Fragment) {
        val ft = supportFragmentManager.beginTransaction()
        ft.replace(binding.container.id, f)
        ft.commit()
    }
    fun addFragment(f : Fragment) {
        val ft = supportFragmentManager.beginTransaction()
        ft.add(binding.container.id, f)
        ft.addToBackStack(null)
        ft.commit()
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