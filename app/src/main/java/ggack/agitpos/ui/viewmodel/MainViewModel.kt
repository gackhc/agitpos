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
import ggack.agitpos.utils.Permissions

class MainViewModel : BaseViewModel() {
    lateinit var liveUserData : LiveData<Map<String, Map<String, Any>>>
    lateinit var liveDeposits: LiveData<Map<String, Map<String, Any>>>
    fun init() {
        liveUserData = Transformations.map(FirebaseComm.liveUserList){it}
        liveDeposits = Transformations.map(FirebaseComm.liveDeposits){it}
    }
    fun popupAddUser(context : Context) {
        val editText = EditText(context)
        val dlg = AlertDialog.Builder(context).setView(editText).setTitle("유저 추가").setPositiveButton("OK"
        ) { _, _ ->
            addUser(editText.text.toString())
        }.setNegativeButton("Cancel", null)
        dlg.show()
    }
    fun popUpChangeMoney(context : Context, name : String, money : Int) {
        val editText = EditText(context)
        editText.inputType = InputType.TYPE_CLASS_NUMBER
        editText.setText(money.toString())
        editText.textAlignment = View.TEXT_ALIGNMENT_CENTER
        val dlg = AlertDialog.Builder(context).setView(editText).setTitle("금액 수정").setPositiveButton("OK"
        ) { _, _ ->
            try {
                val v = editText.text.toString().toInt()
                FirebaseComm.setMoney(name, v)
                Toast.makeText(context, "입력 성공", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "입력 실패", Toast.LENGTH_SHORT).show()
            }
        }
        dlg.show()
    }
    fun addUser(userName : String) {
        FirebaseComm.addUser(userName)
    }
    fun removeUser(userName : String) {
        FirebaseComm.removeUser(userName)
    }
    fun refreshUser() {
        FirebaseComm.getUsers()
    }
    fun getDeposits() {
        FirebaseComm.getDeposits()
    }
    fun setMoney(name : String, money : Int) {
        FirebaseComm.setMoney(name, money)
    }

    fun checkDeposits(context : Context) {

        if(Permissions.requestPermission(context, Manifest.permission.READ_SMS, 0, "")) {
            SmsManager.startCheck(context)
        }
    }
}