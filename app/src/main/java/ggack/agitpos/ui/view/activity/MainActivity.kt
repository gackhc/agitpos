package ggack.agitpos.ui.view.activity

import android.Manifest
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import ggack.agitpos.R
import ggack.agitpos.data.firebase.FirebaseComm
import ggack.agitpos.data.sms.SmsManager
import ggack.agitpos.databinding.ActivityMainBinding
import ggack.agitpos.ui.base.BaseActivity
import ggack.agitpos.ui.view.fragment.MainMasterFragment
import ggack.agitpos.ui.view.fragment.UserListFragment
import ggack.agitpos.ui.viewmodel.MainViewModel
import ggack.agitpos.utils.Permissions

class MainActivity : BaseActivity() {
    private val viewModel : MainViewModel by viewModels()
    private lateinit var binding : ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewModel.init()
        FirebaseComm.init()
        FirebaseComm.setAccount("ex1")
        val ft = supportFragmentManager.beginTransaction()
        ft.replace(binding.container.id, MainMasterFragment())
        ft.commit()
    }

    fun startFragment(f : Fragment) {
        val ft = supportFragmentManager.beginTransaction()
        ft.add(binding.container.id, f)
        ft.addToBackStack(null)
        ft.commit()
    }

    override fun onResume() {
        super.onResume()
    }
}