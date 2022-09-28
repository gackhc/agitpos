package ggack.agitpos.ui.viewmodel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import ggack.agitpos.data.firebase.FirebaseComm
import ggack.agitpos.data.model.AccountModel
import ggack.agitpos.data.model.HistoryModel
import ggack.agitpos.ui.base.BaseViewModel
import ggack.agitpos.utils.CharSearcher

class LoginViewModel : BaseViewModel() {
    lateinit var liveUserData : LiveData<Map<String, Map<String, Any>>?>
    lateinit var liveHistory : LiveData<MutableMap<String, HistoryModel>?>
    lateinit var liveValidate : LiveData<Boolean>
    lateinit var liveAccount : LiveData<AccountModel?>
    fun init() {
        liveUserData = Transformations.map(FirebaseComm.liveUserList) {
            it
        }
        liveHistory = Transformations.map(FirebaseComm.liveHistory) {
            it
        }
        liveValidate = Transformations.map(FirebaseComm.liveValidate) {
            it
        }
        liveAccount = Transformations.map(FirebaseComm.liveAccount) {
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
        FirebaseComm.getUsers()
    }

    fun firebaseInit(context : Context, email : String) {
        FirebaseComm.init()
        FirebaseComm.setID(context, email)
        FirebaseComm.getAccount(context)
    }

    fun firebaseValidate() {
        FirebaseComm.checkValidate()
    }
}