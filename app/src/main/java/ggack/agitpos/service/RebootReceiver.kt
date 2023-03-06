package ggack.agitpos.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class RebootReceiver : BroadcastReceiver() {
    override fun onReceive(context : Context, intent : Intent) {
        val i = Intent(context, NotiListenerService::class.java)
        context.startForegroundService(i)
    }
}