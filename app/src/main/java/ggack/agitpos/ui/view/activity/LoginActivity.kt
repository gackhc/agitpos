package ggack.agitpos.ui.view.activity

import Singleton
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.PowerManager
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
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
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.journeyapps.barcodescanner.BarcodeEncoder
import ggack.agitpos.R
import ggack.agitpos.data.model.networkmodel.FromMap
import ggack.agitpos.data.model.networkmodel.HistoryModel
import ggack.agitpos.databinding.ActivityLoginBinding
import ggack.agitpos.listener.ListListener
import ggack.agitpos.service.RestartService
import ggack.agitpos.ui.adapter.HistoryAdapter
import ggack.agitpos.ui.adapter.UserNameAdapter
import ggack.agitpos.ui.base.BaseActivity
import ggack.agitpos.ui.viewmodel.LoginViewModel
import ggack.agitpos.utils.SimpleDialog
import ggack.agitpos.utils.Utils
import java.util.*
import kotlin.collections.HashMap
import kotlin.collections.Map
import kotlin.collections.forEach
import kotlin.collections.hashMapOf
import kotlin.collections.lastIndex
import kotlin.collections.mutableListOf
import kotlin.collections.set
import kotlin.collections.sorted
import kotlin.collections.toList
import kotlin.concurrent.timer


class LoginActivity : BaseActivity() {
    private val viewModel : LoginViewModel by viewModels()
    private lateinit var binding : ActivityLoginBinding

    private lateinit var mGoogleClient : GoogleSignInClient
    private lateinit var mAuth: FirebaseAuth
    private var mTimerRefresh : Timer? = null
    private var mAdapter : UserNameAdapter? = null
    private var mHistoryAdapter : HistoryAdapter? = null
    private var mMapName : Map<String, Map<String, Any>> = HashMap()
    private var mListHistory = mutableListOf<HistoryModel>()
    private lateinit var mHandler : Handler
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewModel.init()
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        mAuth = Firebase.auth
        mGoogleClient = GoogleSignIn.getClient(this, gso)
        initHandler()
        initAdapter()
        initObserver()
        initView()
        signIn()
    }
    fun initHandler() {
        mHandler = Handler(mainLooper) {
            when(it.what) {
                0 -> { //기록 보기
                    binding.rvHistory.visibility = View.VISIBLE
                    binding.btnChange.text = "입금 방법 보기"
                }
            }
            true
        }

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
        val permissions = arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS)
        val permissionCheck =
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) + ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, permissions, 1)
            return
        } else if(!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
            Toast.makeText(this, "다른 앱 위에 표시 권한을 설정해주세요", Toast.LENGTH_LONG).show()
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

        mHandler.removeMessages(0)
        mHandler.sendEmptyMessage(0)
    }
    override fun onPause() {
        super.onPause()
        mTimerRefresh?.cancel()
    }

    private fun signIn() {
        val signInIntent = mGoogleClient.signInIntent
        startActivityForResult(signInIntent, 1000)
    }

    val historyKeys = mutableListOf<String>()
    private fun initObserver() {
        viewModel.liveUserData.observe(this) {
            it?.let {
                mMapName = it
                mAdapter?.map = it
                mAdapter?.keys = it.keys.toList().sorted()
                mAdapter?.notifyDataSetChanged()
            }
        }
        viewModel.liveHistory.observe(this) {
            val keys = it?.keys?.sorted()
            keys?.forEach { key ->
                if(!historyKeys.contains(key)) {
                    it[key]?.let { model ->
                        if(mListHistory.size < 10) {
                            mListHistory.add(model)
                            mHistoryAdapter?.notifyItemInserted(mListHistory.lastIndex)
                            binding.rvHistory.scrollToPosition(mListHistory.lastIndex)
                        } else {
                            mListHistory.removeAt(0)
                            mListHistory.add(model)
                            mHistoryAdapter?.notifyDataSetChanged()
                            binding.rvHistory.scrollToPosition(mListHistory.lastIndex)
                        }
                    }
                }
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

        viewModel.liveAccount.observe(this) {
            it?.let {
                if(it.account.isNotBlank()) {
                    binding.tv1.visibility = View.VISIBLE
                    binding.tv2.visibility = View.VISIBLE
                    binding.tv3.visibility = View.VISIBLE
                    binding.tvBankAccount.visibility = View.VISIBLE
                    binding.tvBankAccount.text = "카카오뱅크 ${it.account} ${it.name}"
                }
                if(it.kakaopay.isNotBlank()) {
                    binding.tv4.visibility = View.VISIBLE
                    binding.ivQr.visibility = View.VISIBLE
                    val hints: Hashtable<EncodeHintType, String> = Hashtable()
                    hints[EncodeHintType.CHARACTER_SET] = "UTF-8"
                    binding.ivQr.setImageBitmap(BarcodeEncoder().createBitmap(MultiFormatWriter().encode(it.kakaopay, BarcodeFormat.QR_CODE, 1000, 1000, hints)))
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
                Toast.makeText(this, "구글 로그인 실패", Toast.LENGTH_SHORT).show()
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
                        Toast.makeText(this, "구글 로그인 실패(이메일 정보 이상)", Toast.LENGTH_SHORT).show()
                        finishAffinity()
                        Utils.setPref(this, "restart", false)
                    } else {
                        viewModel.firebaseInit(this, user?.email?.replace(".","")?:"")
                    }
                } else {
                    Log.w("login", "signInWithCredential:failure", task.exception)
                    Toast.makeText(this, "구글 로그인 실패", Toast.LENGTH_SHORT).show()
                    finishAffinity()
                    Utils.setPref(this, "restart", false)
                }
            }
    }

    private fun initView() {
        binding.etNickname.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus)
                binding.rvNickName.visibility = View.VISIBLE
            else
                binding.rvNickName.visibility = View.GONE
        }
        binding.etNickname.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }
            override fun afterTextChanged(s: Editable?) {
                val list = viewModel.GetUserByConsonant(s.toString())
                mAdapter?.keys = list.sorted()
                mAdapter?.notifyDataSetChanged()
            }
        })

        binding.root.setOnClickListener {
            binding.rvNickName.visibility = View.GONE
        }
        binding.etNickname.setOnClickListener {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(binding.etNickname,0)
            binding.etNickname.setText("")
            binding.rvNickName.visibility = View.VISIBLE
        }
        binding.etNickname.setOnFocusChangeListener { v, hasFocus ->
            if(hasFocus) {
                binding.etNickname.setText("")
                binding.rvNickName.visibility = View.VISIBLE
            } else {
                binding.rvNickName.visibility = View.GONE
            }
        }
        binding.rvHistory.setOnTouchListener { v, event ->
            binding.rvNickName.visibility = View.GONE
            false
        }
        binding.etPin.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }
            override fun afterTextChanged(s: Editable?) {
                if(s?.length == 4) {
                    mMapName[binding.etNickname.text.toString().lowercase()]?.let {
                        val model = FromMap.toUserModel(it)
                        if (s.toString() == model.pin) {
                            Toast.makeText(this@LoginActivity, "로그인 성공", Toast.LENGTH_SHORT).show()

                            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                            imm.hideSoftInputFromWindow(currentFocus?.windowToken,0)
                            val intentMain = Intent(this@LoginActivity, MainActivity::class.java)
                            intentMain.putExtra("name", binding.etNickname.text.toString().lowercase())
                            intentMain.putExtra("isMaster", binding.etNickname.text.toString() == "*master")

                            startActivity(intentMain)
                        } else
                            binding.etPin.setText("")

                    } ?: run {
                        binding.etPin.setText("")
                        Toast.makeText(this@LoginActivity, "닉네임을 선택해주세요", Toast.LENGTH_SHORT).show()
                        binding.etNickname.requestFocus()
                    }
                }
            }
        })
        binding.btnChange.setOnClickListener {
            mHandler.removeMessages(0)
            if(binding.rvHistory.isVisible) {
                binding.rvHistory.visibility = View.GONE
                binding.btnChange.text = "기록 보기"
            } else {
                binding.rvHistory.visibility = View.VISIBLE
                binding.btnChange.text = "입금 방법 보기"
                mHandler.sendEmptyMessageDelayed(0, 60000)
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