package ggack.agitpos.ui.viewmodel

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.text.InputFilter
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import ggack.agitpos.data.firebase.FirebaseComm
import ggack.agitpos.data.model.AccountModel
import ggack.agitpos.data.model.HistoryModel
import ggack.agitpos.data.model.UserModel
import ggack.agitpos.data.sms.SmsManager
import ggack.agitpos.ui.base.BaseViewModel
import ggack.agitpos.utils.CharSearcher
import ggack.agitpos.utils.Permissions

class MainViewModel : BaseViewModel() {
    lateinit var liveUserData : LiveData<Map<String, Map<String, Any>>?>
    lateinit var liveDeposits: LiveData<Map<String, Map<String, Any>>?>
    lateinit var liveHistoryByName : LiveData<MutableMap<String, MutableList<HistoryModel>>?>
    lateinit var liveAccount : LiveData<AccountModel?>

    val liveCurrentUser = MutableLiveData<UserModel?>(null)
    var userName = ""
    fun init() {
        liveUserData = Transformations.map(FirebaseComm.liveUserList){it}
        liveDeposits = Transformations.map(FirebaseComm.liveDeposits){it}
        liveHistoryByName = Transformations.map(FirebaseComm.liveHistoryByName){it}
        liveAccount = Transformations.map(FirebaseComm.liveAccount){it}
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
    fun popUpChangePin(context : Context, name : String = userName) {
        val editText = EditText(context)
        editText.inputType = InputType.TYPE_CLASS_NUMBER
        editText.textAlignment = View.TEXT_ALIGNMENT_CENTER
        editText.filters = arrayOf<InputFilter>(InputFilter.LengthFilter(4))
        val dlg = AlertDialog.Builder(context).setView(editText).setTitle("Pin 수정").setPositiveButton("OK"
        ) { _, _ ->
            if(editText.text.length != 4) {
                Toast.makeText(context, "짧습니다", Toast.LENGTH_SHORT).show()
            } else {
                try {
                    FirebaseComm.changePin(name, editText.text.toString())
                    Toast.makeText(context, "수정 성공", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "수정 실패", Toast.LENGTH_SHORT).show()
                }
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

    fun setUser(name : String, user : UserModel) {
        userName = name
        liveCurrentUser.postValue(user)
    }

    fun setAccount(context: Context, account : String, name : String, kakaoPay : String) {
        FirebaseComm.setAccount(context, account.replace(" ", "").replace("-", ""), name, kakaoPay)
    }
    fun GetUserByConsonant(key : String) : List<String>{
        val searcher = CharSearcher()
        val listData = mutableListOf<String>()
        liveUserData.value?.keys?.let {
            for (name in it) {
                if (searcher.matchString(name, key))
                    listData.add(name)
            }
            if (listData.size == 0) {
                for (name in it) {
                    if (name >= key)
                        listData.add(name)
                }
            }
        }
        return listData.sorted()
    }
}