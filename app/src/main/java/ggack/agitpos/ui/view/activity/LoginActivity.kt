package ggack.agitpos.ui.view.activity

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.ContactsContract
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import ggack.agitpos.R
import ggack.agitpos.data.firebase.FirebaseComm
import ggack.agitpos.data.model.FromMap
import ggack.agitpos.databinding.ActivityLoginBinding
import ggack.agitpos.databinding.ActivityMainBinding
import ggack.agitpos.listener.ListListener
import ggack.agitpos.service.DepositService
import ggack.agitpos.service.RestartService
import ggack.agitpos.ui.adapter.UserNameAdapter
import ggack.agitpos.ui.viewmodel.LoginViewModel
import ggack.agitpos.ui.viewmodel.MainViewModel
import ggack.agitpos.utils.Utils
import java.util.*
import kotlin.concurrent.timer
import androidx.core.app.ActivityCompat

import android.content.pm.PackageManager
import android.net.Uri

import androidx.core.content.ContextCompat
import android.os.PowerManager
import android.provider.Settings


class LoginActivity : AppCompatActivity() {
    private val viewModel : LoginViewModel by viewModels()
    private lateinit var binding : ActivityLoginBinding

    private lateinit var mGoogleClient : GoogleSignInClient
    private lateinit var mAuth: FirebaseAuth
    private var mTimerRefresh : Timer? = null
    private var mAdapter : UserNameAdapter? = null
    private var mMapName : Map<String, Map<String, Any>> = HashMap()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewModel.init()
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            //.requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        mAuth = Firebase.auth
        mGoogleClient = GoogleSignIn.getClient(this, gso)
        FirebaseComm.init()
        FirebaseComm.setAccount("ex1")


        initPowerMode()
        initAdapter()
        initObserver()
        initView()
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
    override fun onResume() {
        super.onResume()

        val permissions = arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS)
        val permissionCheck =
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) + ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, permissions, 1)
        }

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
                mMapName = it
                mAdapter?.map = it
                mAdapter?.keys = it.keys.toList()
                mAdapter?.notifyDataSetChanged()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == 1000) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                // Google Sign In was successful, authenticate with Firebase
                val account = task.getResult(ApiException::class.java)!!
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                // Google Sign In failed, update UI appropriately
                Log.w("login", "Google sign in failed", e)
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        mAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d("login", "signInWithCredential:success")
                    val user = mAuth.currentUser
                    Singleton.account = user?.email
                    setResult(RESULT_OK)
                    finish()
                } else {
                    Log.w("login", "signInWithCredential:failure", task.exception)
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
                mAdapter?.keys = list
                mAdapter?.notifyDataSetChanged()
            }
        })

        binding.root.setOnClickListener {
            binding.rvNickName.visibility = View.GONE
        }
        binding.etNickname.setOnClickListener {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(binding.etNickname,0)
            binding.rvNickName.visibility = View.VISIBLE
        }

        binding.etPin.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }
            override fun afterTextChanged(s: Editable?) {
                if(s?.length == 4) {
                    mMapName[binding.etNickname.text.toString()]?.let {
                        val model = FromMap.toUserModel(it)
                        if (s.toString() == model.pin) {
                            Toast.makeText(this@LoginActivity, "로그인 성공", Toast.LENGTH_SHORT).show()

                            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                            imm.hideSoftInputFromWindow(currentFocus?.windowToken,0)
                            val intentMain = Intent(this@LoginActivity, MainActivity::class.java)
                            intentMain.putExtra("name", binding.etNickname.text.toString())
                            intentMain.putExtra("isMaster", binding.etNickname.text.toString() == "마스터")

                            startActivity(intentMain)
                        } else
                            binding.etPin.setText("")

                    } ?: run {
                        Toast.makeText(this@LoginActivity, "닉네임을 선택해주세요", Toast.LENGTH_SHORT).show()
                        binding.etNickname.requestFocus()
                    }
                }
            }
        })

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
    }
}