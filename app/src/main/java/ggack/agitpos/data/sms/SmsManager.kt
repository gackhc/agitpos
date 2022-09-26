package ggack.agitpos.data.sms

import android.Manifest
import android.app.Activity
import com.google.android.gms.auth.api.phone.SmsRetriever
import java.lang.ref.WeakReference
import android.R

import android.widget.TextView

import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import ggack.agitpos.data.model.DepositModel
import ggack.agitpos.utils.Utils
import java.lang.Exception
import java.lang.StringBuilder
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*


object SmsManager {
    val liveDepositArray = MutableLiveData<List<DepositModel>>()
    fun startCheck(con : Context){
        val permissionCheck =
            ContextCompat.checkSelfPermission(con, Manifest.permission.RECEIVE_SMS) + ContextCompat.checkSelfPermission(con, Manifest.permission.READ_SMS)
        if (permissionCheck != PackageManager.PERMISSION_GRANTED)
            return

        val cr: ContentResolver = con.contentResolver
        val cursor: Cursor? = cr.query(Uri.parse("content://sms/inbox"), null, "address=?", arrayOf("15993333"), null)
        var lastCheck = Utils.getPrefLong(con, "smsCheck")
        if(lastCheck == 0L)
            lastCheck = Utils.getDateLong(LocalDateTime.of(2022,9,19,0,0))
        Utils.setPref(con, "smsCheck", Utils.getDateLong(LocalDateTime.now()))
        cursor?.let {
            val dateidx: Int = cursor.getColumnIndex("date")
            val bodyidx: Int = cursor.getColumnIndex("body")
            val list = mutableListOf<DepositModel>()
            if (cursor.count > 0) {
                cursor.moveToNext()
                while (!cursor.isClosed) {
                    val date: Long = cursor.getLong(dateidx)
                    if(date < lastCheck)
                        break
                    val body: String = cursor.getString(bodyidx)
                    val strArray = body.split("\n")
                    try{
                        val nickName = strArray[strArray.lastIndex - 1].trim().lowercase()
                        if(strArray[strArray.lastIndex - 2].contains("입금")) {
                            val money = strArray[strArray.lastIndex - 2].trim().replace("입금 ", "")
                               .replace("원", "")
                               .replace(",", "").toInt()
                           Log.e("debug", "$nickName : $money")
                           list.add(DepositModel(date, money, nickName))
                        }
                    } catch (e : Exception) {
                        e.printStackTrace()
                    }
                    if (cursor.isLast)
                        break
                    cursor.moveToNext()
                }
            }
            liveDepositArray.postValue(list)
            cursor.close()
        }
    }
}