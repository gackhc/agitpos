package ggack.agitpos.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import ggack.agitpos.R
import ggack.agitpos.ui.view.activity.LoginActivity
import ggack.agitpos.utils.Utils

class RestartService : Service() {
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.e("RestartService", "onStartCommand")
        var content = "입금 확인중"
        val builder = NotificationCompat.Builder(this, getString(R.string.app_name))
        builder.setSmallIcon(R.drawable.ic_launcher_foreground)
        //builder.setSmallIcon(R.drawable.ic)
        val style = NotificationCompat.BigTextStyle()
        style.bigText("")
        style.setBigContentTitle(null)
        style.setSummaryText("")
        builder.setContentText(content)
        builder.setContentTitle(null)
        builder.setOngoing(true)
        builder.setStyle(style)
        builder.setWhen(0)
        builder.setShowWhen(false)
        builder.setVibrate(null)
        builder.setDefaults(0)
        builder.setSound(null)
        builder.priority = NotificationCompat.PRIORITY_LOW
        val notificationIntent = Intent(this, LoginActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)
        builder.setContentIntent(pendingIntent)

        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notiChannel = NotificationChannel(getString(R.string.app_name), getString(R.string.app_name), NotificationManager.IMPORTANCE_DEFAULT)
            notiChannel.vibrationPattern = longArrayOf(0L)
            notiChannel.enableVibration(false)
            notiChannel.importance = NotificationManager.IMPORTANCE_LOW
            manager.createNotificationChannel(notiChannel)
        }
        val notification = builder.build()
        startForeground(2101, notification)

        /////////////////////////////////////////////////////////////////////

        /////////////////////////////////////////////////////////////////////
        val intent = Intent(this, NotiListenerService::class.java)
        if (!Utils.isServiceRunningCheck(applicationContext)) startService(intent)

        stopForeground(true)
        stopSelf()
        return START_NOT_STICKY
    }
}