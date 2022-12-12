package ggack.agitpos.ui.login

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import ggack.agitpos.data.firebase.FirebaseComm
import ggack.agitpos.data.model.networkmodel.AccountModel
import ggack.agitpos.data.model.networkmodel.HistoryModel
import ggack.agitpos.utils.CharSearcher
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(private val firebaseComm: FirebaseComm) : ViewModel() {
    lateinit var liveUserData : LiveData<Map<String, Map<String, Any>>?>
    lateinit var liveHistory : LiveData<MutableMap<String, HistoryModel>?>
    lateinit var liveValidate : LiveData<Boolean>
    lateinit var liveAccount : LiveData<AccountModel?>

    private val _liveShowBankAccount = MutableLiveData(false)
    val liveShowBankAccount : LiveData<Boolean> = _liveShowBankAccount

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

    var mapUserList = mapOf<String, Map<String, Any>>()
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
}