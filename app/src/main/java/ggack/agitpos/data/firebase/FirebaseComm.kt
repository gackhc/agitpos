package ggack.agitpos.data.firebase

import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.google.firebase.database.*
import ggack.agitpos.data.model.*
import ggack.agitpos.utils.Utils
import java.lang.Exception
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object FirebaseComm {
    private val TAG = "Firebase"
    private lateinit var mDatabase : DatabaseReference
    private var mID : String? = null
    private lateinit var mValidate : LocalDate
    val liveUserList = MutableLiveData<Map<String, Map<String, Any>>?>(null)
    val liveDeposits = MutableLiveData<Map<String, Map<String, Any>>?>(null)
    val liveHistory = MutableLiveData<MutableMap<String, HistoryModel>?>(null)
    val liveValidate = MutableLiveData<Boolean>(true)
    val liveAccount = MutableLiveData<AccountModel?>(null)
    val liveHistoryByName = MutableLiveData<MutableMap<String, MutableList<HistoryModel>>?>(null)
    fun init() {
        mDatabase = FirebaseDatabase.getInstance().reference
    }

    fun setID(context: Context, id : String) {
        mID = id
        setHistoryListener()
        checkValidate(id)
        setVersion(context)
        getUsers()
    }

    fun checkValidate(id : String? = mID) {
        id?.let {
            mDatabase.child(id).get().addOnSuccessListener { data ->
                val map = data.value as MutableMap<String, Any>
                val validate = map["validate"] as String?
                val dtf = DateTimeFormatter.ofPattern("yyyyMMdd")
                if (validate.isNullOrBlank()) {
                    mValidate = LocalDate.now()
                    mDatabase.child(it).child("validate").setValue(mValidate.format(dtf))
                } else {
                    mValidate = LocalDate.parse(validate, dtf)
                    if (LocalDate.now().isAfter(mValidate)) {
                        liveValidate.postValue(false)
                        return@addOnSuccessListener
                    }
                }
                liveValidate.postValue(true)
            }.addOnFailureListener {
                liveValidate.postValue(false)
            }
        }
    }
    fun setVersion(context: Context) {
        mID?.let {
            try {
                val pinfo = context.packageManager.getPackageInfo(context.packageName, 0);
                mDatabase.child(it).child("version").setValue(pinfo.versionName)
            } catch (e : Exception) {

            }
        }
    }
    fun addUser(name : String) {
        mID?.let { it ->
            val map : Map<String, Any?> = UserModel("0000", 0).getMap()
            mDatabase.child(it).child("users").child(name.lowercase()).setValue(map).addOnSuccessListener {
                Log.e(TAG, "add success $name")
                getUsers()
            }.addOnFailureListener {
                Log.e(TAG, "Error getting data", it)
            }
        }
    }
    fun removeUser(name : String) {
        mID?.let { it ->
            val map : Map<String, Any?> = UserModel("0000", 0).getMap()
            mDatabase.child(it).child("users").child(name.lowercase()).removeValue().addOnSuccessListener {
                Log.e(TAG, "add success $name")
                getUsers()
            }.addOnFailureListener {
                Log.e(TAG, "Error getting data", it)
            }
        }
    }
    fun getUsers() {
        mID?.let { it ->
            mDatabase.child(it).child("users").get().addOnSuccessListener { data ->
                val value = data.value as Map<String, Map<String, Any>>?
                if(value == null) {
                    addUser("*MASTER")
                } else {
                    liveUserList.postValue(value)
                }
                Log.e(TAG, value.toString())
            }.addOnSuccessListener {

            }.addOnFailureListener {
                Log.e(TAG, "Error getUsers", it)
            }
        }
    }
    fun getDeposits() {
        mID?.let { it ->
            mDatabase.child(it).child("deposits").get().addOnSuccessListener { data ->
                val value = data.value as Map<String, Map<String, Any>>? ?: run {
                    liveDeposits.postValue(mapOf())
                    return@addOnSuccessListener
                }
                liveDeposits.postValue(value)
                Log.e(TAG, value.toString())
            }.addOnFailureListener {
                Log.e(TAG, "Error getDeposits", it)
            }
        }
    }
    fun removeDepositItem(date:String) {
        mID?.let { it ->
            mDatabase.child(it).child("deposits").child(date).removeValue().addOnSuccessListener {
                getDeposits()
            }.addOnFailureListener {
                Log.e(TAG, "Error removeDepositItem", it)
            }
        }
    }
    fun addDepositItem(model : DepositModel) {
        mID?.let { it ->
            mDatabase.child(it).child("deposits").child(model.date.toString()).setValue(model.getMap()).addOnSuccessListener {
                getDeposits()
            }.addOnFailureListener {
                Log.e(TAG, "Error removeDepositItem", it)
            }
        }
    }
    fun changePin(name : String, pin : String) {
        mID?.let { it ->
            mDatabase.child(it).child("users").child(name.lowercase()).child("pin").setValue(pin)
            .addOnSuccessListener {
                getUsers()
            }.addOnFailureListener {
                Log.e(TAG, "Error changePin", it)
            }
        }
    }
    fun setMoney(name : String, money : Int) {
        mID?.let { it ->
            mDatabase.child(it).child("users").child(name.lowercase()).child("money").setValue(money)
                .addOnSuccessListener {
                    getUsers()
                }.addOnFailureListener {
                    Log.e(TAG, "Error changePin", it)
                }
        }
    }
    fun setMembership(name : String, month : String) {
        mID?.let { it ->
            mDatabase.child(it).child("users").child(name.lowercase()).child("member").setValue(month)
                .addOnSuccessListener {
                    getUsers()
                }.addOnFailureListener {
                    Log.e(TAG, "Error changePin", it)
                }
        }
    }
    fun addHistory(historyModel : HistoryModel) {
        val month = Utils.getDate(historyModel.date).format(DateTimeFormatter.ofPattern("yyyyMM"))
        val datetime = Utils.getDate(historyModel.date).format(DateTimeFormatter.ofPattern("ddHHmmssSSS"))
        mID?.let { it ->
            mDatabase.child(it).child("history").child(month).child(datetime).setValue(historyModel.getMap())
        }
    }
    fun setAccount(context : Context, account : String, name : String, kakaoPay : String) {
        mID?.let {
            try {
                mDatabase.child(it).child("account").setValue(AccountModel(account, name, kakaoPay).getMap())
                liveAccount.postValue(AccountModel(account, name))
                getAccount(context)
            } catch (e : Exception) {

            }
        }
    }
    fun getAccount(context : Context) {
        mID?.let { it ->
            try {
                mDatabase.child(it).child("account").get().addOnSuccessListener {data ->
                    data.value?.let {v ->
                        val model = FromMap.toAccountModel(v as Map<String, Any>)
                        if (!model.account.isNullOrEmpty())
                            Utils.setPref(
                                context,
                                "bankAccount",
                                model.account!!.substring(model.account!!.lastIndex - 3)
                            )
                        liveAccount.postValue(model)
                    }
                }
            } catch (e : Exception) {

            }
        }
    }
    private val historyListener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            val children = snapshot.children.sortedBy { it.key }
            val map : MutableMap<String, HistoryModel> = hashMapOf()
            val mapName : MutableMap<String, MutableList<HistoryModel>> = hashMapOf()
            children.forEach {
                it.key?.let {key ->
                    FromMap.toHistoryModel(it.value as Map<String, Any?>).let { model ->
                        map[key] = model
                        if (mapName[model.nickname] == null)
                            mapName[model.nickname] = mutableListOf()
                        mapName[model.nickname]?.add(model)
                    }
                }
            }
            liveHistoryByName.postValue(mapName)
            liveHistory.postValue(map)
            Log.e("debug", map.toString())
        }
        override fun onCancelled(error: DatabaseError) {
        }
    }
    private var prevListenerMonth = ""
    private fun setHistoryListener() {
        mID?.let { it ->
            val now = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"))
            if(now != prevListenerMonth) {
                prevListenerMonth = now
                mDatabase.child(it).child("history")
                    .child(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM")))
                    .addValueEventListener(historyListener)
            }
        }
    }
}