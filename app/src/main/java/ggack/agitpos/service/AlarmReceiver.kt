package ggack.agitpos.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context : Context, intent : Intent) {
        Log.e("AlarmReceiver", "onReceive")
        val intent = Intent(context, RestartService::class.java)
        context.startForegroundService(intent)
    }
}