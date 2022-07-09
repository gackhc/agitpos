package ggack.agitpos.data.sms

import android.app.Activity
import com.google.android.gms.auth.api.phone.SmsRetriever
import java.lang.ref.WeakReference
import android.R

import android.widget.TextView

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import ggack.agitpos.data.model.DepositModel
import java.lang.StringBuilder
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.*


object SmsManager {
    val liveDepositArray = MutableLiveData<List<DepositModel>>()
    fun startCheck(act : Context){
        val cr: ContentResolver = act.contentResolver
        val cursor: Cursor? = cr.query(Uri.parse("content://sms/inbox"), null, "address=?", arrayOf("15993333"), null)
        cursor?.let {
            val dateidx: Int = cursor.getColumnIndex("date")
            val bodyidx: Int = cursor.getColumnIndex("body")
            val formatter = SimpleDateFormat("MM/dd HH:mm")
            var count = 0
            val list = mutableListOf<DepositModel>()
            if (cursor.count > 0) {
                cursor.moveToNext()
                while (!cursor.isClosed) {
                    val date: Long = cursor.getLong(dateidx)
                    val datetime = LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(date),
                        ZoneOffset.systemDefault()
                    )
                    val sDate: String = formatter.format(Date(date))
                    val body: String = cursor.getString(bodyidx)
                    val strArray = body.split("\n")
                    val nickName = strArray[strArray.lastIndex - 1].trim()
                    if(strArray[strArray.lastIndex - 2].contains("입금")) {
                        val money = strArray[strArray.lastIndex - 2].trim().replace("입금 ", "")
                           .replace("원", "")
                           .replace(",", "").toInt()
                       Log.e("debug", "$nickName : $money")
                        list.add(DepositModel(money, nickName))
                    }
                    if (count++ == 10)
                        break
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