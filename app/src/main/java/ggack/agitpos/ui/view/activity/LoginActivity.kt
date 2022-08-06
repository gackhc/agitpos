package ggack.agitpos.ui.view.activity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
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
import ggack.agitpos.databinding.ActivityLoginBinding
import ggack.agitpos.databinding.ActivityMainBinding
import ggack.agitpos.ui.viewmodel.LoginViewModel
import ggack.agitpos.ui.viewmodel.MainViewModel
import ggack.agitpos.utils.Utils

class LoginActivity : AppCompatActivity() {
    private val viewModel : LoginViewModel by viewModels()
    private lateinit var binding : ActivityLoginBinding

    private lateinit var mGoogleClient : GoogleSignInClient
    private lateinit var mAuth: FirebaseAuth


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            //.requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        mAuth = Firebase.auth
        mGoogleClient = GoogleSignIn.getClient(this, gso)
        FirebaseComm.init()
        FirebaseComm.setAccount("ex1")

        initObserver()
    }
    private fun signIn() {
        val signInIntent = mGoogleClient.signInIntent
        startActivityForResult(signInIntent, 1000)
    }


    private fun initObserver() {
        viewModel.liveUserData.observe(this) {
        }
        viewModel.
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
}