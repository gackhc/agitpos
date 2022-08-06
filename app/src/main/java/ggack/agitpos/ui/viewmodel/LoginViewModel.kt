package ggack.agitpos.ui.viewmodel

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.view.marginEnd
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import ggack.agitpos.data.firebase.FirebaseComm
import ggack.agitpos.data.model.DepositModel
import ggack.agitpos.data.model.UserModel
import ggack.agitpos.data.sms.SmsManager
import ggack.agitpos.ui.base.BaseViewModel
import ggack.agitpos.utils.CharSearcher
import ggack.agitpos.utils.Permissions

class LoginViewModel : BaseViewModel() {
    lateinit var liveUserData : LiveData<Map<String, Map<String, Any>>>
    fun init() {
        liveUserData = Transformations.map(FirebaseComm.liveUserList){it}
    }

    var mapUserList = mapOf<String, Map<String, Any>>()
    fun GetUserByConsonant(key : String) : ArrayList<String>{
        val searcher = CharSearcher()
        val arrRetData = ArrayList<String>()
        mapUserList.keys.let {
            for (name in it) {
                if (searcher.matchString(name, key))
                    arrRetData.add(name)
            }
            if (arrRetData.size == 0) {
                for (name in it) {
                    if (name >= key)
                        arrRetData.add(name)
                }
            }
        }
        return arrRetData
    }

    fun refreshUser() {
        FirebaseComm.getUsers()
    }
}