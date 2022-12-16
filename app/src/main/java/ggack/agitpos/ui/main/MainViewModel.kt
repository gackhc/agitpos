package ggack.agitpos.ui.main

import Singleton.isDuplicated
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
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import ggack.agitpos.data.firebase.FirebaseComm
import ggack.agitpos.data.model.networkmodel.*
import ggack.agitpos.data.sms.SmsManager
import ggack.agitpos.ui.dialog.AddItemDialog
import ggack.agitpos.utils.CharSearcher
import ggack.agitpos.utils.Permissions
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(private val firebaseComm: FirebaseComm) : ViewModel() {
    lateinit var liveUserData : LiveData<Map<String, Map<String, Any>>?>
    lateinit var liveDeposits: LiveData<Map<String, Map<String, Any>>?>
    lateinit var liveItems: LiveData<List<ItemModel>>
    lateinit var liveHistoryByName : LiveData<MutableMap<String, MutableList<HistoryModel>>?>
    lateinit var liveAccount : LiveData<AccountModel?>

    val liveCurrentUser = MutableLiveData<UserModel?>(null)
    private val _liveHasMonthlyMemberItem = MutableLiveData(false)
    val liveHasMonthlyMemberItem : LiveData<Boolean> = _liveHasMonthlyMemberItem
    var userName = ""
    fun init() {
        liveUserData = Transformations.map(firebaseComm.liveUserList){it}
        liveDeposits = Transformations.map(firebaseComm.liveDeposits){it}
        liveItems = Transformations.map(firebaseComm.liveItemList){
            val list = FromMap.toItemModels(it)
            _liveHasMonthlyMemberItem.postValue(false)
            for(item in list) {
                if(item.name == "월간 이용권"){
                    _liveHasMonthlyMemberItem.postValue(true)
                    break
                }
            }
            list
        }
        liveHistoryByName = Transformations.map(firebaseComm.liveHistoryByName){it}
        liveAccount = Transformations.map(firebaseComm.liveAccount){it}
    }
    fun popupAddUser(context : Context) {
        val editText = EditText(context)
        val dlg = AlertDialog.Builder(context).setView(editText).setTitle("유저 추가").setPositiveButton("OK"
        ) { _, _ ->
            if(editText.text.toString().isNotEmpty()) {
                if (liveUserData.value?.isDuplicated(
                        editText.text.toString(),
                        editText.text.toString()
                    ) == true
                )
                    Toast.makeText(context, "이름이 중복됩니다", Toast.LENGTH_SHORT).show()
                else
                    addUser(editText.text.toString())
            }
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
                firebaseComm.setMoney(name, v)
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
                    firebaseComm.changePin(name, editText.text.toString())
                    Toast.makeText(context, "수정 성공", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "수정 실패", Toast.LENGTH_SHORT).show()
                }
            }
        }
        dlg.show()
    }
    fun popUpItemChange(context : Context, item : ItemModel? = null, idx : Int) {
        val dlg = AddItemDialog(context, object : AddItemDialog.ResultCallback {
            override fun onResult(name: String, price: Int) {
                addItem(ItemModel(name, price, 1, idx))
            }

            override fun onDelete() {
                if (item != null) {
                    removeItem(item)
                }
            }

        })
        item?.let {
            dlg.setText(it.name, it.price)
            dlg.binding.btnDelete.visibility = View.VISIBLE
        }
        dlg.show()
    }
    fun addUser(userName : String) {
        firebaseComm.addUser(userName)
    }
    fun removeUser(userName : String) {
        firebaseComm.removeUser(userName)
    }
    fun refreshUser() {
        firebaseComm.getUsers()
    }
    fun getDeposits() {
        firebaseComm.getDeposits()
    }
    fun refreshItems() {
        firebaseComm.getItems()
    }
    fun setMoney(name : String, money : Int) {
        firebaseComm.setMoney(name, money)
    }
    fun removeItem(model : ItemModel) {
        firebaseComm.removeItem(model)
    }
    fun addItem(model : ItemModel) {
        firebaseComm.addItem(model)
    }
    fun setItems(list : List<ItemModel>) {
        val map = mutableMapOf<String, Map<String, Any>>()
        for(i in 0 until list.lastIndex) {
            map[list[i].idx.toString()] = list[i].getMap()
        }
        firebaseComm.setItems(map)
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
        firebaseComm.setAccount(context, account.replace(" ", "").replace("-", ""), name, kakaoPay)
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

    fun addMonthlyMemberItem() {
        val list = liveItems.value?: listOf()
        addItem(ItemModel("월간 이용권", 40000, 1,
            if(list.size <= 1) 1 else list.last().idx + 1))
    }
 }