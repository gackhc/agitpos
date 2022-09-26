package ggack.agitpos.service

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import ggack.agitpos.ui.view.activity.LoginActivity
import ggack.agitpos.utils.Utils

class RestartActivityService : Service() {
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Handler(mainLooper).postDelayed({
            if(Utils.isAppIsInBackground(this)) {
                val i = Intent(applicationContext, LoginActivity::class.java)
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                applicationContext.startActivity(i)
            }
        }, 5000)
        return START_NOT_STICKY
    }
}