package ggack.agitpos.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context : Context, intent : Intent) {
        Log.e("AlarmReceiver", "onReceive")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(context, RestartService::class.java)
                context.startForegroundService(intent)
        } else {
            val intent = Intent(context, DepositService::class.java)
            context.startService(intent)
        }
    }
}