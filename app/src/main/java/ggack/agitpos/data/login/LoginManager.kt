package ggack.agitpos.data.login

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth

class LoginManager {
    private lateinit var mAuth : FirebaseAuth
    private lateinit var mGoogleSignInClient : GoogleSignInClient
    fun login(context : Context) {
        mAuth = FirebaseAuth.getInstance()
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()
        mGoogleSignInClient = GoogleSignIn.getClient(context, gso)
        val signInIntent = mGoogleSignInClient.signInIntent
        context.startActivity(signInIntent)
    }
}