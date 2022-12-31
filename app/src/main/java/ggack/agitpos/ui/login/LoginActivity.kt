package ggack.agitpos.ui.login

import Singleton
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.core.app.NotificationManagerCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.AndroidEntryPoint
import ggack.agitpos.R
import ggack.agitpos.data.firebase.FirebaseComm
import ggack.agitpos.data.model.networkmodel.HistoryModel
import ggack.agitpos.databinding.ActivityLoginBinding
import ggack.agitpos.listener.ListListener
import ggack.agitpos.service.RestartService
import ggack.agitpos.ui.adapter.HistoryAdapter
import ggack.agitpos.ui.adapter.UserNameAdapter
import ggack.agitpos.ui.base.BaseActivity
import ggack.agitpos.ui.dialog.SimpleDialog
import ggack.agitpos.ui.main.ClientActivity
import ggack.agitpos.ui.main.MainActivity
import ggack.agitpos.utils.Constants
import ggack.agitpos.utils.Utils
import java.util.*
import javax.inject.Inject
import kotlin.concurrent.timer

@AndroidEntryPoint
class LoginActivity : BaseActivity() {
    @Inject
    lateinit var firebaseComm: FirebaseComm
    private val viewModel by viewModels<LoginViewModel>()
    private lateinit var binding : ActivityLoginBinding

    private lateinit var mGoogleClient : GoogleSignInClient
    private lateinit var mAuth: FirebaseAuth
    private var mTimerRefresh : Timer? = null
    private var mAdapter : UserNameAdapter? = null
    private var mHistoryAdapter : HistoryAdapter? = null
    private var mListHistory = mutableListOf<HistoryModel>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_login)
        binding.lifecycleOwner = this
        binding.vm = viewModel
        viewModel.init()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        mAuth = Firebase.auth
        mGoogleClient = GoogleSignIn.getClient(this, gso)
        initAdapter()
        initObserver()
        signIn()
    }
    fun initPowerMode() {
        val pm = applicationContext.getSystemService(POWER_SERVICE) as PowerManager
        var isWhiteListing = pm.isIgnoringBatteryOptimizations(applicationContext.packageName)
        if (!isWhiteListing) {
            val intent = Intent()
            intent.action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
            intent.data = Uri.parse("package:" + applicationContext.packageName)
            startActivity(intent)
        }
        startForegroundService(Intent(this, RestartService::class.java))
    }

    override fun onBackPressed() {
        val dlg = SimpleDialog(this, SimpleDialog.TYPE_TWO_BUTTON) {
            when(it) {
                RESULT_OK -> {
                    finishAffinity()
                    Utils.setPref(this, "restart", false)
                }
            }
        }
        dlg.setText("앱을 종료하시겠습니까?")
        dlg.show()
    }

    override fun onResume() {
        super.onResume()
        Utils.setPref(this, "restart", false)
        if(!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
            Utils.toast(this, "다른 앱 위에 표시 권한을 설정해주세요")
            return
        } else {
            val sets = NotificationManagerCompat.getEnabledListenerPackages(this)
            if (!sets.contains(packageName)) {
                val dlg = SimpleDialog(this, SimpleDialog.TYPE_ONE_BUTTON) {
                    startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
                }
                dlg.setText("알림 설정 해주세요")
                dlg.show()
                return
            }
        }
        viewModel.firebaseValidate()
        Utils.setPref(this, "restart", true)
        initPowerMode()
        mTimerRefresh = timer(period = 10000, initialDelay = 0) {
            viewModel.refreshUser()
        }
        binding.etNickname.setText("")
        binding.etPin.setText("")
    }
    override fun onPause() {
        super.onPause()
        mTimerRefresh?.cancel()
    }

    private fun signIn() {
        val signInIntent = mGoogleClient.signInIntent
        startActivityForResult(signInIntent, 1000)
    }

    private fun initObserver() {
        viewModel.liveUserData.observe(this) {
            it?.let {
                mAdapter?.map = it
                mAdapter?.keys = it.keys.toList().sorted()
                mAdapter?.notifyDataSetChanged()
            }
        }
        viewModel.liveHistory.observe(this) {
            val list = it?.values?.toList()?.sortedBy { it.date }?.reversed()
            list?.let {
                mListHistory.clear()
                if(list.size < 10) {
                    mListHistory.addAll(list)
                } else {
                    for(i in 0..9) {
                        mListHistory.add(list[i])
                    }
                }
                mHistoryAdapter?.notifyDataSetChanged()
                binding.rvHistory.scrollToPosition(mListHistory.lastIndex)
            }
        }
        viewModel.liveValidate.observe(this) {
            if (!it) {
                if(dialogValidate?.isShowing() == true)
                    return@observe
                dialogValidate = SimpleDialog(this, SimpleDialog.TYPE_ONE_BUTTON) {
                    finishAffinity()
                    Utils.setPref(this, "restart", false)
                }
                dialogValidate?.setText("사용기간이 만료되었습니다. 관리자에게 문의 바랍니다. \nhongc90@naver.com")
                dialogValidate?.setCancelable(false)
                dialogValidate?.show()
            }
        }

        viewModel.liveEtId.observe(this) {
            val list = viewModel.GetUserByConsonant(it)
            mAdapter?.keys = list.sorted()
            mAdapter?.notifyDataSetChanged()
        }

        viewModel.liveEvent.observe(this) {
            when(it?.action) {
                Constants.ACTION_TOAST -> {
                    Utils.toast(this, it.param as String)
                }
                Constants.ACTION_START_ACTIVITY_MAIN -> {
                    val intent = it.param as Intent
                    if(intent.getBooleanExtra("isMaster", false)) {
                        intent.setClass(this, MainActivity::class.java)
                        startActivity(intent)
                    } else {
                        intent.setClass(this, ClientActivity::class.java)
                        startActivity(intent)
                    }
                }
            }
        }
    }
    var dialogValidate : SimpleDialog? = null

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == 1000) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                // Google Sign In was successful, authenticate with Firebase
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account)
            } catch (e: ApiException) {
                // Google Sign In failed, update UI appropriately
                Log.w("login", "Google sign in failed", e)
                Utils.toast(this, "구글 로그인 실패")
                finishAffinity()
                Utils.setPref(this, "restart", false)
            }
        }
    }

    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        mAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d("login", "signInWithCredential:success")
                    val user = mAuth.currentUser
                    Singleton.account = user?.email
                    if(user?.email.isNullOrEmpty()) {
                        Utils.toast(this, "구글 로그인 실패(이메일 정보 이상)")
                        finishAffinity()
                        Utils.setPref(this, "restart", false)
                    } else {
                        viewModel.firebaseInit(this, user?.email?.replace(".","")?:"")
                    }
                } else {
                    Log.w("login", "signInWithCredential:failure", task.exception)
                    Utils.toast(this, "구글 로그인 실패")
                    finishAffinity()
                    Utils.setPref(this, "restart", false)
                }
            }
    }

    private fun initAdapter() {
        binding.rvNickName.layoutManager = LinearLayoutManager(this)
        mAdapter = UserNameAdapter(hashMapOf(), object : ListListener{
            override fun onClick(key: String, item: Any?) {
                super.onClick(key, item)
                binding.etNickname.setText(key)
                binding.rvNickName.visibility = View.GONE
            }
        })

        binding.rvNickName.adapter = mAdapter


        binding.rvHistory.layoutManager = LinearLayoutManager(this)
        mHistoryAdapter = HistoryAdapter(mListHistory) {
            binding.rvNickName.visibility = View.GONE
        }
        binding.rvHistory.adapter = mHistoryAdapter
    }
}