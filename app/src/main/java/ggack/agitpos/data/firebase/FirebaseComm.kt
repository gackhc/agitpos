package ggack.agitpos.data.firebase

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import ggack.agitpos.data.model.DepositModel
import ggack.agitpos.data.model.UserModel

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
            mDatabase.child(it).child("users").child(name).setValue(map).addOnSuccessListener {
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
            mDatabase.child(it).child("users").child(name).removeValue().addOnSuccessListener {
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
                val value = data.value as Map<String, Map<String, Any>>
                liveUserList.postValue(value)
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
                val value = data.value as Map<String, Map<String, Any>>
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
    fun addDepositItem(map : Map<String, DepositModel>) {
        mAccount?.let { it ->
            mDatabase.child(it).child("deposits").setValue(map).addOnSuccessListener {
                getDeposits()
            }.addOnFailureListener {
                Log.e(TAG, "Error removeDepositItem", it)
            }
        }
    }
    fun changePin(name : String, pin : String) {
        mAccount?.let { it ->
            mDatabase.child(it).child("users").child(name).child("pin").setValue(pin)
            .addOnSuccessListener {
                getUsers()
            }.addOnFailureListener {
                Log.e(TAG, "Error changePin", it)
            }
        }
    }
    fun setMoney(name : String, money : Int) {
        mAccount?.let { it ->
            mDatabase.child(it).child("users").child(name).child("money").setValue(money)
                .addOnSuccessListener {
                    getUsers()
                }.addOnFailureListener {
                    Log.e(TAG, "Error changePin", it)
                }
        }
    }
}