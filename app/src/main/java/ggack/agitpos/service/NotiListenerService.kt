 package ggack.agitpos.service

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Observer
import ggack.agitpos.R
import ggack.agitpos.data.firebase.FirebaseComm
import ggack.agitpos.data.model.networkmodel.DepositModel
import ggack.agitpos.data.model.networkmodel.FromMap
import ggack.agitpos.data.model.networkmodel.HistoryModel
import ggack.agitpos.ui.view.activity.LoginActivity
import ggack.agitpos.utils.Constants
import ggack.agitpos.utils.Utils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.*


 class NotiListenerService: NotificationListenerService() {
     private var observerUser : Observer<Any?>? = null
     private var mUserPrepared = false
     private var mUserList : Map<String, Map<String, Any>>? = null

     override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
         startNotiService()
         return super.onStartCommand(intent, flags, startId)
     }

     override fun onListenerConnected() {
         super.onListenerConnected()
         Log.e("onListenerConnected", "connected")

         observerUser = Observer {
             mUserList = it as Map<String, Map<String, Any>>?
             mUserPrepared = true
         }
         observerUser?.let {
             FirebaseComm.liveUserList.observeForever(it)
         }
     }

     override fun onListenerDisconnected() {
         super.onListenerDisconnected()
         Log.e("onListenerDisconnected", "disconnected")
         observerUser?.let { FirebaseComm.liveUserList.removeObserver(it) }
     }
     override fun onNotificationPosted(sbn: StatusBarNotification?) {
         super.onNotificationPosted(sbn)
         Log.e("onNotificationPosted", "${sbn?.packageName} : ${sbn?.notification?.extras}")
         if(sbn?.packageName == "com.kakao.talk") {
             val body = sbn?.notification?.extras?.getString("android.text")
             if (body?.contains("코드송금 받기가 완료되었습니다.") == true) {
                 val strArray = body.split("\n")

                 var nickName : String? = null
                 var money : Int? = null
                 for(str in strArray) {
                     if(str.contains("받은금액 :")) {
                         money =
                         str.replace("- 받은금액 : ", "")
                             .replace(",", "")
                             .replace("원", "").toInt()
                     } else if(str.contains("메시지 :")) {
                         nickName =
                             str.trim().replace("- 메시지 : ", "").lowercase()
                     }
                 }
                if(nickName != null && money != null) {
                    val model = DepositModel(Instant.now().toEpochMilli(), money, nickName)
                    mUserPrepared = false
                    FirebaseComm.getUsers()
                    CoroutineScope(Dispatchers.Main).launch {
                        while (!mUserPrepared) {
                            delay(10)
                        }
                        if (mUserList?.contains(model.name) == true) {
                            val user = FromMap.toUserModel(mUserList!![model.name]!!)
                            val money = user.money + model.money
                            FirebaseComm.setMoney(model.name, money)

                            val historyModel = HistoryModel(
                                System.currentTimeMillis(),
                                model.name,
                                Constants.HISTORY_DEPOSIT,
                                model.money,
                                mapOf(),
                                "카카오페이"
                            )
                            FirebaseComm.addHistory(historyModel)
                        } else {
                            FirebaseComm.addDepositItem(model)
                        }
                    }
                }
             }
         } else if(sbn?.packageName == "com.samsung.android.messaging") {
             val body = sbn?.notification?.extras?.getString("android.text")?:""
             val bankAccount = Utils.getPref(this, "bankAccount")
             if (body.contains("[카카오뱅크]") && body.contains("입금") && body.contains("($bankAccount)")) {
                 val strArray = body.split("\n")

                 var nickName : String? = null
                 var money : Int? = null

                 nickName = strArray[strArray.lastIndex - 1].trim().lowercase()
                 if(strArray[strArray.lastIndex - 2].contains("입금")) {
                     money = strArray[strArray.lastIndex - 2].trim().replace("입금 ", "")
                         .replace("원", "")
                         .replace(",", "").toInt()
                 }
                 if(nickName != null && money != null) {
                     val model = DepositModel(Instant.now().toEpochMilli(), money, nickName)
                     mUserPrepared = false
                     FirebaseComm.getUsers()
                     CoroutineScope(Dispatchers.Main).launch {
                         while (!mUserPrepared) {
                             delay(10)
                         }
                         if (mUserList?.contains(model.name) == true) {
                             val user = FromMap.toUserModel(mUserList!![model.name]!!)
                             val money = user.money + model.money
                             FirebaseComm.setMoney(model.name, money)

                             val historyModel = HistoryModel(
                                 System.currentTimeMillis(),
                                 model.name,
                                 Constants.HISTORY_DEPOSIT,
                                 model.money,
                                 mapOf(),
                                 "카뱅"
                             )
                             FirebaseComm.addHistory(historyModel)
                         } else {
                             FirebaseComm.addDepositItem(model)
                         }
                     }
                 }
             }
         }
     }
     override fun onNotificationRemoved(sbn: StatusBarNotification?) {
         super.onNotificationRemoved(sbn)
     }



     fun startNotiService() {
         var content = "입금 확인중"
         val builder = NotificationCompat.Builder(this, getString(R.string.app_name))
         builder.setSmallIcon(R.drawable.ic_launcher_foreground)
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
         val notiChannel = NotificationChannel(getString(R.string.app_name), getString(R.string.app_name), NotificationManager.IMPORTANCE_DEFAULT)
         notiChannel.vibrationPattern = longArrayOf(0L)
         notiChannel.enableVibration(false)
         notiChannel.importance = NotificationManager.IMPORTANCE_LOW
         manager.createNotificationChannel(notiChannel)
         val notification = builder.build()
         startForeground(2102, notification)
     }

     // 서비스 재시작 코드
     override fun onDestroy() {
         setAlarmTimer()
         super.onDestroy()
     }
     fun setAlarmTimer() {
         val c = Calendar.getInstance()
         c.timeInMillis = System.currentTimeMillis()
         c.add(Calendar.SECOND, 1)
         val intent = Intent(this, AlarmReceiver::class.java)
         val sender = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
         val mAlarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
         mAlarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, c.timeInMillis, sender)
         Log.e("DepositService", "setAlarmTimer")
     }
}