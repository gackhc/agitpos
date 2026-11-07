package ggack.agitpos.service

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Observer
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import ggack.agitpos.R
import ggack.agitpos.data.firebase.FirebaseComm
import ggack.agitpos.data.firebase.DepositInbox
import ggack.agitpos.data.model.networkmodel.AccountModel
import ggack.agitpos.domain.DepositNotification
import ggack.agitpos.ui.login.LoginActivity
import ggack.agitpos.utils.Utils
import java.util.Calendar
import javax.inject.Inject

@AndroidEntryPoint
class NotiListenerService : NotificationListenerService() {
    @Inject lateinit var firebaseComm: FirebaseComm
    private lateinit var inbox: DepositInbox
    private val handler = Handler(Looper.getMainLooper())
    private var saving = false
    private var destroyed = false
    private var connected = false
    private var statusText = "입금 확인 중"
    private val retry = Runnable { drainInbox() }
    private val ownerObserver = Observer<String?> { owner ->
        if (owner != null) {
            if (connected) activeNotifications?.forEach { capture(it) }
            drainInbox()
        }
    }
    private val accountObserver = Observer<AccountModel?> {
        if (connected) activeNotifications?.forEach { notification -> capture(notification) }
    }

    override fun onCreate() {
        super.onCreate()
        inbox = DepositInbox(this)
        firebaseComm.liveOwner.observeForever(ownerObserver)
        firebaseComm.liveAccount.observeForever(accountObserver)
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startNotiService()
        drainInbox()
        return START_STICKY
    }
    override fun onListenerConnected() {
        super.onListenerConnected()
        connected = true
        startNotiService()
        activeNotifications?.forEach { capture(it) }
        drainInbox()
    }
    override fun onListenerDisconnected() {
        connected = false
        super.onListenerDisconnected()
    }
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn?.let { capture(it) }
    }
    private fun capture(sbn: StatusBarNotification) {
        if (sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return
        val owner = FirebaseAuth.getInstance().currentUser?.email?.replace(".", "") ?: return
        val body = sbn.notification.extras?.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            ?: sbn.notification.extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: return
        val bankAccount = if (Utils.getPref(this, "databaseOwner") == owner) Utils.getPref(this, "bankAccount") else ""
        val eventTime = sbn.notification.`when`.takeIf { it > 0 } ?: sbn.postTime
        val request = DepositNotification.parse(sbn.packageName, sbn.key, eventTime, body, bankAccount) ?: return
        if (!inbox.enqueue(owner, request)) {
            statusText = "입금 기록 저장 실패 · 기기 저장 공간을 확인해 주세요"
            startNotiService()
            return
        }
        drainInbox()
    }
    private fun drainInbox() {
        if (destroyed || saving) return
        val owner = firebaseComm.liveOwner.value ?: return
        val authOwner = FirebaseAuth.getInstance().currentUser?.email?.replace(".", "") ?: return
        if (owner != authOwner) return
        val entry = inbox.next(owner) ?: return
        saving = true
        firebaseComm.submit(entry.request) { result ->
            saving = false
            if (result.isSuccess) inbox.remove(entry)
            if (destroyed) return@submit
            handler.removeCallbacks(retry)
            if (result.isSuccess) {
                statusText = "입금 확인 중"
                startNotiService()
                drainInbox()
            } else {
                Log.w("DepositInbox", "Deposit transaction not committed")
                statusText = "입금 저장 대기 중 · 연결 상태를 확인해 주세요"
                startNotiService()
                handler.postDelayed(retry, 30_000)
            }
        }
    }
    override fun onDestroy() {
        destroyed = true
        handler.removeCallbacksAndMessages(null)
        firebaseComm.liveOwner.removeObserver(ownerObserver)
        firebaseComm.liveAccount.removeObserver(accountObserver)
        super.onDestroy()
    }

     fun startNotiService() {
         val content = statusText
         val builder = NotificationCompat.Builder(this, getString(R.string.app_name))
         builder.setSmallIcon(R.drawable.ic_launcher_foreground)
         val style = NotificationCompat.BigTextStyle()
         style.bigText("")
         style.setBigContentTitle(null)
         style.setSummaryText("")
         builder.setContentText(content)
         builder.setContentTitle(null)
         builder.setOngoing(false)
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
         startForeground(2102, notification)
     }

     // 서비스 재시작 코드
     override fun onTaskRemoved(rootIntent: Intent?) {
         super.onTaskRemoved(rootIntent)
         setAlarmTimer()
     }


     fun setAlarmTimer() {
         val c = Calendar.getInstance()
         c.timeInMillis = System.currentTimeMillis()
         c.add(Calendar.SECOND, 1)
         val intent = Intent(this, RebootReceiver::class.java)
         val sender = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
         val mAlarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
         if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || mAlarmManager.canScheduleExactAlarms()) {
              mAlarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, c.timeInMillis, sender)
          } else {
              mAlarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, c.timeInMillis, sender)
          }
         Log.e("NotiListenerService", "setAlarmTimer")
     }

}
