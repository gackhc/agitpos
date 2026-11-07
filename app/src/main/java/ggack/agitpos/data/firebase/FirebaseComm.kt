package ggack.agitpos.data.firebase

import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.google.firebase.database.*
import ggack.agitpos.data.model.networkmodel.*
import ggack.agitpos.domain.ItemCatalog
import ggack.agitpos.domain.Ledger
import ggack.agitpos.domain.LedgerRequest
import ggack.agitpos.utils.Utils
import java.lang.Exception
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseComm @Inject constructor(){
    private val TAG = "Firebase"
    private lateinit var mDatabase : DatabaseReference
    private var mID : String? = null
    private lateinit var mValidate : LocalDate
    val liveItemList = MutableLiveData<List<ItemModel>>(listOf())
    val liveOwner = MutableLiveData<String?>(null)
    val liveError = MutableLiveData<String>()
    private val ledger = Ledger()
    val liveUserList = MutableLiveData<Map<String, Map<String, Any>>>(mapOf())
    val liveDeposits = MutableLiveData<Map<String, Map<String, Any>>>(mapOf())
    val liveHistory = MutableLiveData<MutableMap<String, HistoryModel>>(mutableMapOf())
    val liveHistoryStatistics = MutableLiveData<MutableMap<String, HistoryModel>>(mutableMapOf())
    val liveValidate = MutableLiveData(true)
    val liveAccount = MutableLiveData<AccountModel?>(null)
    val liveHistoryByName = MutableLiveData<MutableMap<String, MutableList<HistoryModel>>>(
        mutableMapOf()
    )
    fun init(database: FirebaseDatabase = FirebaseDatabase.getInstance()) {
        mDatabase = database.reference
    }

    fun setID(context: Context, id : String) {
        if (mID != id) {
            historyReference?.removeEventListener(historyListener)
            historyReference = null
            historyMonths.clear()
            liveHistory.value = mutableMapOf()
            liveHistoryByName.value = mutableMapOf()
            liveUserList.value = emptyMap()
            liveItemList.value = emptyList()
            liveDeposits.value = emptyMap()
            liveAccount.value = null
            searchHistoryMonth = LocalDate.now()
            if (Utils.getPref(context, "databaseOwner") != id) Utils.setPref(context, "bankAccount", "")
            Utils.setPref(context, "databaseOwner", id)
        }
        mID = id
        liveOwner.value = id
        setHistoryListener()
        checkValidate(id)
        setVersion(context)
        getUsers()
    }

    fun checkValidate(id : String? = mID) {
        id?.let {
            mDatabase.child(id).get().addOnSuccessListener { data ->
                val validate = data.child("validate").value as? String
                val dtf = DateTimeFormatter.ofPattern("yyyyMMdd")
                if (validate.isNullOrBlank()) {
                    mValidate = LocalDate.now()
                    mDatabase.child(it).child("validate").setValue(mValidate.format(dtf))
                } else {
                    mValidate = runCatching { LocalDate.parse(validate, dtf) }.getOrElse {
                        liveValidate.postValue(false)
                        return@addOnSuccessListener
                    }
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
        val normalized = name.trim().lowercase()
        if (!Ledger.validKey(normalized)) {
            liveError.value = "이름에는 . # $ [ ] / 문자를 사용할 수 없습니다."
            return
        }
        mID?.let { it ->
            mDatabase.child(it).child("users").child(normalized).runTransaction(object : Transaction.Handler {
                override fun doTransaction(data: MutableData): Transaction.Result {
                    if (data.value != null) return Transaction.abort()
                    data.value = UserModel("0000", 0).getMap()
                    return Transaction.success(data)
                }
                override fun onComplete(error: DatabaseError?, committed: Boolean, data: DataSnapshot?) {
                    if (error != null) liveError.value = "사용자 저장에 실패했습니다."
                    else if (!committed) liveError.value = "이미 등록된 이름입니다."
                    if (error == null) getUsers()
                }
            }, false)
        }
    }
    fun removeUser(name : String) {
        mID?.let { it ->
            mDatabase.child(it).child("users").child(name.lowercase()).removeValue().addOnSuccessListener {
                Log.e(TAG, "add success $name")
                getUsers()
            }.addOnFailureListener {
                Log.e(TAG, "Error getting data", it)
            }
        }
    }
    fun getUsers() {
        setHistoryListener()
        mID?.let { it ->
            mDatabase.child(it).child("users").get().addOnSuccessListener { data ->
                if (mID != it) return@addOnSuccessListener
                val value = data.value as? Map<String, Map<String, Any>> ?: run {
                    addUser("*MASTER")
                    return@addOnSuccessListener
                }
                liveUserList.postValue(value)
            }.addOnFailureListener {
                Log.e(TAG, "Error getUsers", it)
            }
        }
    }
    fun getDeposits() {
        mID?.let { it ->
            mDatabase.child(it).child("deposits").get().addOnSuccessListener { data ->
                if (mID != it) return@addOnSuccessListener
                val value = data.value as Map<String, Map<String, Any>>? ?: run {
                    liveDeposits.postValue(mapOf())
                    return@addOnSuccessListener
                }
                liveDeposits.postValue(value)
            }.addOnFailureListener {
                Log.e(TAG, "Error getDeposits", it)
            }
        }
    }
    fun getItems() {
        mID?.let { it ->
            mDatabase.child(it).child("items").get().addOnSuccessListener { data ->
                if (mID != it) return@addOnSuccessListener
                runCatching { ItemCatalog.decode(data.value) }
                    .onSuccess { liveItemList.postValue(it) }
                    .onFailure { liveError.postValue("상품 데이터를 읽지 못했습니다.") }
            }.addOnFailureListener {
                Log.e(TAG, "Error getDeposits", it)
            }
        }
    }
    fun setItems(map : Map<String, Map<String, Any>>) {
        mID?.let { it ->
            mDatabase.child(it).child("items").setValue(map).addOnSuccessListener {
                getItems()
            }.addOnFailureListener {
                Log.e(TAG, "Error getting data", it)
            }
        }
    }
    fun addItem(model : ItemModel) {
        mID?.let { it ->
            mDatabase.child(it).child("items").child(model.idx.toString()).setValue(model.getMap()).addOnSuccessListener {
                getItems()
            }.addOnFailureListener {
                Log.e(TAG, "Error getting data", it)
            }
        }
    }
    fun removeItem(model : ItemModel) {
        mID?.let { it ->
            mDatabase.child(it).child("items").child((model.idx).toString()).removeValue().addOnSuccessListener {
                Log.e(TAG, "add success ${model.name}")
                getItems()
            }.addOnFailureListener {
                Log.e(TAG, "Error getting data", it)
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
    fun submit(request: LedgerRequest, callback: (Result<Unit>) -> Unit) {
        val owner = mID
        if (owner == null || !::mDatabase.isInitialized) {
            callback(Result.failure(IllegalStateException("로그인 후 다시 시도해 주세요.")))
            return
        }
        var rejection: Exception? = null
        mDatabase.child(owner).runTransaction(object : Transaction.Handler {
            override fun doTransaction(data: MutableData): Transaction.Result {
                rejection = null
                try {
                    // A cold cache can be empty even when the server has data. A no-op
                    // success lets Firebase retry against the authoritative snapshot.
                    if (data.value != null) data.value = ledger.apply(Ledger.record(data.value), request)
                } catch (error: Exception) {
                    rejection = error
                }
                return Transaction.success(data)
            }
            override fun onComplete(error: DatabaseError?, committed: Boolean, data: DataSnapshot?) {
                val applied = data?.child("processedOperations")?.hasChild(request.id) == true
                if (error == null && committed && applied) {
                    if (mID == owner) {
                        getUsers()
                        getDeposits()
                        // A manually assigned deposit can belong to an older month.
                        data?.child("history")?.children?.forEach { mergeHistory(it) }
                    }
                    callback(Result.success(Unit))
                } else {
                    callback(Result.failure(error?.toException() ?: rejection
                        ?: IllegalStateException("거래를 저장하지 못했습니다. 로그인과 연결 상태를 확인해 주세요.")))
                }
            }
        }, false)
    }
    fun setExtraName(name : String, extraName : String) {
        mID?.let { it ->
            mDatabase.child(it).child("users").child(name.lowercase()).child("extraName").setValue(extraName)
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
    fun setSupervisor(name : String, enable : Boolean) {
        mID?.let { it ->
            mDatabase.child(it).child("users").child(name.lowercase()).child("isSupervisor").setValue(enable)
                .addOnSuccessListener {
                    getUsers()
                }.addOnFailureListener {
                    Log.e(TAG, "Error changePin", it)
                }
        }
    }
    private var searchHistoryMonth: LocalDate = LocalDate.now()
    fun getPrevHistory() {
        val owner = mID ?: return
        val month = searchHistoryMonth.minusMonths(1)
        mDatabase.child(owner).child("history").child(month.format(DateTimeFormatter.ofPattern("yyyyMM")))
            .get().addOnSuccessListener {
                if (mID == owner) {
                    searchHistoryMonth = month
                    mergeHistory(it)
                }
            }.addOnFailureListener { liveError.postValue("이전 거래 내역을 읽지 못했습니다.") }
    }
    fun setAccount(context: Context, account: String, name: String, kakaoPay: String) {
        val owner = mID ?: return
        mDatabase.child(owner).child("account").setValue(AccountModel(account, name, kakaoPay).getMap())
            .addOnSuccessListener { if (mID == owner) getAccount(context) }
            .addOnFailureListener { liveError.postValue("계좌 정보를 저장하지 못했습니다.") }
    }
    fun getAccount(context: Context) {
        val owner = mID ?: return
        mDatabase.child(owner).child("account").get().addOnSuccessListener { data ->
            if (mID != owner) return@addOnSuccessListener
            runCatching { FromMap.toAccountModel(Ledger.record(data.value)) }
                .onSuccess { model ->
                    Utils.setPref(context, "bankAccount", model.account.takeIf { it.length >= 4 }?.takeLast(4) ?: "")
                    liveAccount.postValue(model)
                }.onFailure { liveError.postValue("계좌 정보의 형식을 확인해 주세요.") }
        }.addOnFailureListener { liveError.postValue("계좌 정보를 읽지 못했습니다.") }
    }

    private var historyReference: DatabaseReference? = null
    private val historyMonths = mutableMapOf<String, Map<String, HistoryModel>>()
    private fun mergeHistory(snapshot: DataSnapshot) {
        val month = snapshot.key ?: return
        val entries = linkedMapOf<String, HistoryModel>()
        snapshot.children.forEach { child ->
            runCatching { FromMap.toHistoryModel(Ledger.record(child.value)) }
                .onSuccess { entries["$month/${child.key}"] = it }
                .onFailure { liveError.postValue("일부 거래 내역의 형식을 확인해 주세요.") }
        }
        historyMonths[month] = entries
        val all = historyMonths.values.flatMap { it.entries }.associate { it.key to it.value }
        liveHistory.postValue(all.toMutableMap())
        liveHistoryByName.postValue(all.values.groupBy { it.nickname }
            .mapValues { it.value.sortedBy { model -> model.date }.toMutableList() }.toMutableMap())
    }
    private val historyListener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) = mergeHistory(snapshot)
        override fun onCancelled(error: DatabaseError) {
            liveError.postValue("거래 내역을 읽지 못했습니다.")
        }
    }
    private fun setHistoryListener() {
        val owner = mID ?: return
        val ref = mDatabase.child(owner).child("history")
            .child(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM")))
        if (historyReference?.toString() != ref.toString()) {
            historyReference?.removeEventListener(historyListener)
            historyReference = ref
            ref.addValueEventListener(historyListener)
        }
    }
}
