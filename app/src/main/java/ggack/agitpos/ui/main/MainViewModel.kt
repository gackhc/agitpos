package ggack.agitpos.ui.main

import android.Manifest
import android.app.Application
import android.content.Context
import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import dagger.hilt.android.lifecycle.HiltViewModel
import ggack.agitpos.data.firebase.FirebaseComm
import ggack.agitpos.data.model.EventModel
import ggack.agitpos.data.model.networkmodel.*
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
            val list = FromMap.toItemModels(it)
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
        liveCurrentUserName.postValue(name)
        liveCurrentUserExtraName.postValue(user.extraName)
        liveCurrentUserMoney.postValue(user.money)
        if(user.member?.length == 6) {
            val date = user.member + "31"
            val format = DateTimeFormatter.ofPattern("yyyyMMdd")
            liveCurrentUserMemberDate.postValue(LocalDate.parse(date, format).format(DateTimeFormatter.ofPattern("MM월 dd일 만료")))
            liveCurrentUserMember.postValue(LocalDate.now().isBefore(LocalDate.parse(date, format)))
        } else if (user.member?.length == 14) {
            val format = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
            liveCurrentUserMemberDate.postValue(LocalDateTime.parse(user.member, format).format(DateTimeFormatter.ofPattern("MM월 dd일 HH시 만료")))
            liveCurrentUserMember.postValue(LocalDateTime.now().isBefore(LocalDateTime.parse(user.member, format)))
        }

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

    fun onPayment(list : List<ItemModel>) {
        val map = mutableMapOf<String, Map<String, Any>>()
        var total = 0
        var money = liveCurrentUserMoney.value?:0
        list.forEach {model->
            if(model.name.contains("운영진 출첵")) {
                var bonus = liveCurrentUserBonus.value?:2022120100L
                var cnt = bonus % 100L
                val dtf = DateTimeFormatter.ofPattern("yyyyMMdd")
                val date = LocalDate.parse(Math.floorDiv(bonus, 100).toString(), dtf)
                val today = LocalDate.now()
                //같은날이면 x
                if(date.isBefore(today)) {
                    //새로운달이면 처음부터
                    //8번 이상이면 x
                    if(date.monthValue == today.monthValue && cnt < 8) {
                        money += 6500
                    } else if(date.monthValue != today.monthValue){
                        cnt = 0L
                        money += 6500
                    }
                    cnt++
                    map[String.format("운영진 출첵 %d|8", cnt)] = model.getMap()
                    firebaseComm.setSvBonus(liveCurrentUserName.value?:"", today.format(dtf).toLong() * 100L + cnt)
                }
            } else {
                map[model.name] = model.getMap()
                total += model.price * model.cnt
            }
        }
        val timeLimitMonth = LocalDateTime.now().plusMonths(1).withDayOfMonth(1).plusMinutes(1).withHour(8).withMinute(0)
        var timeLimitWeek = LocalDateTime.now().plusWeeks(1).withHour(8).withMinute(0)
        timeLimitWeek = timeLimitWeek.minusDays(timeLimitWeek.dayOfWeek.value - 1L)

        if(map.keys.contains("월간 회원권"))
            firebaseComm.setMembership(liveCurrentUserName.value?:"",
                timeLimitMonth.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")))
        if(map.keys.contains("주간 회원권"))
            firebaseComm.setMembership(liveCurrentUserName.value?:"",
                timeLimitWeek.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")))

        firebaseComm.setMoney(liveCurrentUserName.value?:"", money - total)
        if(map.isNotEmpty()) {
            firebaseComm.addHistory(
                HistoryModel(
                    Instant.now().toEpochMilli(),
                    liveCurrentUserName.value ?: "",
                    Constants.HISTORY_PAYMENT,
                    total,
                    map,
                    ""
                )
            )
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