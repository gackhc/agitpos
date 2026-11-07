package ggack.agitpos.ui.main

import android.Manifest
import android.app.AlertDialog
import android.app.Application
import android.content.Context
import android.text.InputFilter
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import dagger.hilt.android.lifecycle.HiltViewModel
import ggack.agitpos.data.firebase.FirebaseComm
import ggack.agitpos.data.model.EventModel
import ggack.agitpos.data.model.networkmodel.*
import ggack.agitpos.domain.Membership
import ggack.agitpos.domain.LedgerAction
import ggack.agitpos.domain.LedgerRequest
import java.util.UUID
import ggack.agitpos.data.sms.SmsManager
import ggack.agitpos.ui.base.BaseViewModel
import ggack.agitpos.utils.CharSearcher
import ggack.agitpos.utils.Constants
import ggack.agitpos.utils.Permissions
import ggack.agitpos.utils.Utils
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(private val firebaseComm: FirebaseComm, application : Application) : BaseViewModel() {
    private val context = application.baseContext
    val liveError: LiveData<String> = firebaseComm.liveError
    fun clearError() { firebaseComm.liveError.value = null }
    lateinit var liveUserData : LiveData<Map<String, Map<String, Any>>?>
    lateinit var liveDeposits: LiveData<Map<String, Map<String, Any>>?>
    lateinit var liveItems: LiveData<List<ItemModel>>
    lateinit var liveHistoryByName : LiveData<MutableMap<String, MutableList<HistoryModel>>?>
    lateinit var liveAccount : LiveData<AccountModel?>

    private val _liveHasMonthlyMemberItem = MutableLiveData(false)
    val liveHasMonthlyMemberItem : LiveData<Boolean> = _liveHasMonthlyMemberItem

    private val _liveAutoStart = MutableLiveData(false)
    val liveAutoStart : LiveData<Boolean> = _liveAutoStart

    val liveCurrentUserMoney = MutableLiveData(0)
    val liveCurrentUserName = MutableLiveData("")
    val liveCurrentUserExtraName = MutableLiveData("")
    val liveCurrentUserMemberDate = MutableLiveData("")
    val liveCurrentUserMember = MutableLiveData(false)
    val liveCurrentUserSupervisor = MutableLiveData(false)
    val liveCurrentUserBonus = MutableLiveData(2022120100L)
    fun init(isAutoStartOn : Boolean) {
        liveUserData = Transformations.map(firebaseComm.liveUserList){it}
        liveDeposits = Transformations.map(firebaseComm.liveDeposits){it}
        liveItems = Transformations.map(firebaseComm.liveItemList){
            val list = it
            _liveHasMonthlyMemberItem.postValue(false)
            for(item in list) {
                if(item.name == "월간 회원권"){
                    _liveHasMonthlyMemberItem.postValue(true)
                    break
                }
            }
            list
        }
        liveHistoryByName = Transformations.map(firebaseComm.liveHistoryByName){it}
        liveAccount = Transformations.map(firebaseComm.liveAccount){it}

        _liveAutoStart.postValue(isAutoStartOn)
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
    fun removeItem(model : ItemModel) {
        firebaseComm.removeItem(model)
    }
    fun addItem(model : ItemModel) {
        firebaseComm.addItem(model)
    }
    fun setItems(list : List<ItemModel>) {
        val map = mutableMapOf<String, Map<String, Any>>()
        list.filter { it.viewType != -1 }.forEach { map[it.idx.toString()] = it.getMap() }
        firebaseComm.setItems(map)
    }
    fun changePin(name : String, pin : String) {
        firebaseComm.changePin(name, pin)
    }

    fun checkDeposits(context : Context) {
        if(Permissions.requestPermission(context, Manifest.permission.READ_SMS, 0, "")) {
            SmsManager.startCheck(context)
        }
    }

    fun setUser(name : String, user : UserModel) {
        liveCurrentUserName.postValue(name)
        liveCurrentUserExtraName.postValue(user.extraName)
        liveCurrentUserMoney.postValue(user.money)
        val expiry = Membership.expiry(user.member)
        liveCurrentUserMemberDate.postValue(expiry?.format(DateTimeFormatter.ofPattern("MM월 dd일 HH시 만료")) ?: "")
        liveCurrentUserMember.postValue(expiry != null && LocalDateTime.now().isBefore(expiry))

        liveCurrentUserSupervisor.postValue(user.isSupervisor == true)
        liveCurrentUserBonus.postValue(user.svBonus?:2022120100)
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
        addItem(ItemModel("월간 회원권", 40000, 1,
            if(list.size <= 1) 1 else list.last().idx + 1))
    }

    val liveSaving = MutableLiveData(false)
    fun onPayment(list: List<ItemModel>, callback: (Result<Unit>) -> Unit) =
        payFor(liveCurrentUserName.value ?: "", list, "", callback)

    fun payFor(name: String, list: List<ItemModel>, operator: String, callback: (Result<Unit>) -> Unit) {
        if (liveSaving.value == true) return
        liveSaving.value = true
        val request = LedgerRequest(UUID.randomUUID().toString(), System.currentTimeMillis(),
            LedgerAction.Payment(name, list.map { it.copy() }, operator))
        firebaseComm.submit(request) { result ->
            liveSaving.value = false
            callback(result)
        }
    }

    fun onStartFragment(action : Int) {
        liveEvent.postValue(EventModel(action))
    }
    fun onSetUserAccount(v : View) {
        liveEvent.postValue(EventModel(Constants.ACTION_SET_USER_ACCOUNT))
    }
    fun onSetQR(v : View) {

    }
    fun onSetPin() {
        liveEvent.postValue(EventModel(Constants.ACTION_SET_MASTER_PIN))
    }

    fun onSetAutoStart(v : View) {
        _liveAutoStart.postValue(!(liveAutoStart.value?:true))
        Utils.setPref(context, "settingRestart", !(_liveAutoStart.value?:true))
    }
    fun onSelStatistics(sel : Int) {
        liveEvent.postValue(EventModel(sel))
    }

    fun refreshStatistics() {
        firebaseComm.getPrevHistory()
    }
}