package ggack.agitpos.utils

import android.R
import android.app.Activity
import android.app.ActivityManager
import android.app.ProgressDialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.util.Log
import android.widget.Toast
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset


val isDebug = true

object Utils {
    private var mDlg : ProgressDialog? = null

    private val MSG_DISMISS_WAIT_DIALOG = 1000

    fun DebugLog(str : String) {
        Log.e("debug", str)
    }

    fun toast(context : Context?, str : String) {
        Toast.makeText(context, str, Toast.LENGTH_SHORT).show()
    }

    fun showWaitDialog(context: Context, str : String = "Loading...") {
        if(mDlg == null)
            mDlg = ProgressDialog(context)
        mDlg?.setCancelable(false)
        mDlg?.setMessage(str)
        mDlg?.setProgressStyle(R.style.Widget_ProgressBar_Horizontal)
        mHandler.sendEmptyMessageDelayed(MSG_DISMISS_WAIT_DIALOG, 5000)
    }

    fun dismissWaitDialog() {
        mHandler.removeMessages(MSG_DISMISS_WAIT_DIALOG)
        mDlg?.dismiss()
        mDlg = null
    }

    fun getDate(date : Long) : LocalDateTime {
        return LocalDateTime.ofInstant(
            Instant.ofEpochMilli(date),
            ZoneOffset.systemDefault()
        )
    }
    fun getDateLong(date : LocalDateTime) : Long {
        return date.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    fun getPref(context: Context, key : String) : String {
        val pref = context.getSharedPreferences("agitpos", Context.MODE_PRIVATE)
        return pref.getString(key, "")?:""
    }
    fun setPref(context: Context, key : String, v : String) {
        val pref = context.getSharedPreferences("agitpos", Context.MODE_PRIVATE)
        val edit= pref.edit()
        edit.putString(key, v)
        edit.apply()
    }
    fun getPrefLong(context: Context, key : String) : Long {
        val pref = context.getSharedPreferences("agitpos", Context.MODE_PRIVATE)
        return pref.getLong(key, 0)
    }
    fun setPref(context: Context, key : String, v : Long) {
        val pref = context.getSharedPreferences("agitpos", Context.MODE_PRIVATE)
        val edit= pref.edit()
        edit.putLong(key, v)
        edit.apply()
    }
    fun getPrefInt(context: Context, key : String) : Int {
        val pref = context.getSharedPreferences("agitpos", Context.MODE_PRIVATE)
        return pref.getInt(key, 0)
    }
    fun setPref(context: Context, key : String, v : Int) {
        val pref = context.getSharedPreferences("agitpos", Context.MODE_PRIVATE)
        pref.edit().putInt("key", v)
        pref.edit().apply()
    }

    fun isServiceRunningCheck(context: Context): Boolean {
        val manager = context.getSystemService(Activity.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if ("ggack.agitpos.service.DepositService" == service.service.className) {
                return true
            }
        }
        return false
    }

    private val mHandler = Handler() {
        when(it.what) {
            MSG_DISMISS_WAIT_DIALOG -> {
                dismissWaitDialog()
            }
        }
        true
    }
}