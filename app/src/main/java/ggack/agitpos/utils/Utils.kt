package ggack.agitpos.utils

import android.R
import android.app.ProgressDialog
import android.content.Context
import android.os.Handler
import android.util.Log
import android.widget.Toast


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

    private val mHandler = Handler() {
        when(it.what) {
            MSG_DISMISS_WAIT_DIALOG -> {
                dismissWaitDialog()
            }
        }
        true
    }
}