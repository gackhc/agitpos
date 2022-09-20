package ggack.agitpos.data.firebase

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import ggack.agitpos.data.model.DepositModel
import ggack.agitpos.data.model.HistoryModel
import ggack.agitpos.data.model.UserModel
import ggack.agitpos.utils.Utils
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object FirebaseComm {
    private val TAG = "Firebase"
    private lateinit var mDatabase : DatabaseReference
    private var mAccount : String? = null

    val liveUserList = MutableLiveData<Map<String, Map<String, Any>>?>(null)
    val liveDeposits = MutableLiveData<Map<String, Map<String, Any>>?>(null)
    fun init() {
        mDatabase = FirebaseDatabase.getInstance().reference
    }

    fun setAccount(id : String) {
        mAccount = id
    }
    fun addUser(name : String) {
        mAccount?.let { it ->
            val map : Map<String, Any> = UserModel("0000", 0).getMap()
            mDatabase.child(it).child("users").child(name.lowercase()).setValue(map).addOnSuccessListener {
                Log.e(TAG, "add success $name")
                getUsers()
            }.addOnFailureListener {
                Log.e(TAG, "Error getting data", it)
            }
        }
    }
    fun removeUser(name : String) {
        mAccount?.let { it ->
            val map : Map<String, Any> = UserModel("0000", 0).getMap()
            mDatabase.child(it).child("users").child(name.lowercase()).removeValue().addOnSuccessListener {
                Log.e(TAG, "add success $name")
                getUsers()
            }.addOnFailureListener {
                Log.e(TAG, "Error getting data", it)
            }
        }
    }
    fun getUsers() {
        mAccount?.let { it ->
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
        mAccount?.let { it ->
            mDatabase.child(it).child("deposits").get().addOnSuccessListener { data ->
                val value = data.value as Map<String, Map<String, Any>>? ?: return@addOnSuccessListener
                liveDeposits.postValue(value)
                Log.e(TAG, value.toString())
            }.addOnSuccessListener {

            }.addOnFailureListener {
                Log.e(TAG, "Error getDeposits", it)
            }
        }
    }
    fun removeDepositItem(date:String) {
        mAccount?.let { it ->
            mDatabase.child(it).child("deposits").child(date).removeValue().addOnSuccessListener {
                getDeposits()
            }.addOnFailureListener {
                Log.e(TAG, "Error removeDepositItem", it)
            }
        }
    }
    fun addDepositItem(model : DepositModel) {
        mAccount?.let { it ->
            mDatabase.child(it).child("deposits").child(model.date).setValue(model.getMap()).addOnSuccessListener {
                getDeposits()
            }.addOnFailureListener {
                Log.e(TAG, "Error removeDepositItem", it)
            }
        }
    }
    fun changePin(name : String, pin : String) {
        mAccount?.let { it ->
            mDatabase.child(it).child("users").child(name.lowercase()).child("pin").setValue(pin)
            .addOnSuccessListener {
                getUsers()
            }.addOnFailureListener {
                Log.e(TAG, "Error changePin", it)
            }
        }
    }
    fun setMoney(name : String, money : Int) {
        mAccount?.let { it ->
            mDatabase.child(it).child("users").child(name.lowercase()).child("money").setValue(money)
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
        mAccount?.let { it ->
            mDatabase.child(it).child("history").child(month).child(datetime).setValue(historyModel.getMap())
        }
    }
}