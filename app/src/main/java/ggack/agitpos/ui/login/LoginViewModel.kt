package ggack.agitpos.ui.login

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import dagger.hilt.android.lifecycle.HiltViewModel
import ggack.agitpos.data.login.UserSession
import ggack.agitpos.data.firebase.FirebaseComm
import ggack.agitpos.data.model.EventModel
import ggack.agitpos.data.model.networkmodel.AccountModel
import ggack.agitpos.data.model.networkmodel.FromMap
import ggack.agitpos.data.model.networkmodel.HistoryModel
import ggack.agitpos.ui.base.BaseViewModel
import ggack.agitpos.utils.CharSearcher
import ggack.agitpos.utils.Constants
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(private val firebaseComm: FirebaseComm, private val session: UserSession) : BaseViewModel() {
    lateinit var liveUserData : LiveData<Map<String, Map<String, Any>>?>
    lateinit var liveHistory : LiveData<MutableMap<String, HistoryModel>?>
    lateinit var liveValidate : LiveData<Boolean>
    lateinit var liveAccount : LiveData<AccountModel?>

    private val _liveShowBankAccount = MutableLiveData(false)
    val liveShowBankAccount : LiveData<Boolean> = _liveShowBankAccount

    private val _liveIdsVisible = MutableLiveData(false)
    val liveIdsVisible : LiveData<Boolean> = _liveIdsVisible


    val liveEtId= MutableLiveData("")
    val liveEtPin= MutableLiveData("")

    fun init() {
        liveUserData = Transformations.map(firebaseComm.liveUserList) {
            it
        }
        liveHistory = Transformations.map(firebaseComm.liveHistory) {
            it
        }
        liveValidate = Transformations.map(firebaseComm.liveValidate) {
            it
        }
        liveAccount = Transformations.map(firebaseComm.liveAccount) {
            it
        }
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

    fun refreshUser() {
        firebaseComm.getUsers()
    }

    fun firebaseInit(context : Context, email : String) {
        firebaseComm.init()
        firebaseComm.setID(context, email)
        firebaseComm.getAccount(context)
    }

    fun firebaseValidate() {
        firebaseComm.checkValidate()
    }


    fun onShowBankAccountBtnClick() {
        _liveShowBankAccount.postValue(!(_liveShowBankAccount.value?:true))
    }

    fun addUser(userName : String) {
        firebaseComm.addUser(userName)
    }
    fun onAddAccount() {
        liveEvent.postValue(EventModel(Constants.ACTION_ADD_USER_ACCOUNT))
    }
    fun setIdsVisibility(visible : Boolean) {
        Log.e("debug", "setIdsVisibility $visible")
        _liveIdsVisible.postValue(visible)
    }
    fun setIdsVisibility(visible : Boolean, view : View) {
        Log.e("debug", "setIdsVisibility $visible")
        setIdsVisibility(visible)
        liveEtId.postValue("")
    }
    fun onPinChanged(s: CharSequence, start :Int, before : Int, count: Int){
        if(s.length == 4) {
            liveUserData.value?.get(liveEtId.value?.lowercase())?.let {
                val model = FromMap.toUserModel(it)
                if (s.toString() == model.pin) {
                    session.signIn(liveEtId.value.orEmpty().lowercase())
                    liveEvent.postValue(EventModel(Constants.ACTION_TOAST, "로그인 성공"))
                    firebaseComm.getItems()

                    val intent = Intent()
                    intent.putExtra("name", liveEtId.value?.lowercase())
                    intent.putExtra("isMaster", session.name == "*master")
                    liveEvent.postValue(EventModel(Constants.ACTION_START_ACTIVITY_MAIN, intent))
                } else
                    liveEtPin.postValue("")

            } ?: run {
                liveEtPin.postValue("")
                liveEvent.postValue(EventModel(Constants.ACTION_TOAST, "닉네임을 선택해주세요"))
            }
        }
    }
}