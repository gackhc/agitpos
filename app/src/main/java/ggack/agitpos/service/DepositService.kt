package ggack.agitpos.service

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.*
import androidx.lifecycle.Observer
import ggack.agitpos.data.sms.SmsManager
import java.util.*
import android.telephony.SmsMessage
import android.util.Log
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import ggack.agitpos.R
import ggack.agitpos.data.firebase.FirebaseComm
import ggack.agitpos.ui.view.activity.LoginActivity
import javax.inject.Inject


@AndroidEntryPoint
open class DepositService : Service() {
    @Inject lateinit var firebaseComm: FirebaseComm
    private var mTimer : Timer? = null
    //private var observerDeposit : Observer<Any?>? = null
    private var observerUser : Observer<Any?>? = null
    private var mUserPrepared = false
    private var mUserList : Map<String, Map<String, Any>>? = null
    override fun onBind(intent: Intent?): IBinder? {
        throw UnsupportedOperationException("Not yet")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        mTimer?.cancel()
//        observerDeposit = Observer {
//            CoroutineScope(Dispatchers.Main).launch {
//                val list = it as List<DepositModel>
//                list.forEach {model ->
//                    if(mUserList?.contains(model.name) == true) {
//                        while(!mUserPrepared) {
//                            delay(10)
//                        }
//                        val user = FromMap.toUserModel(mUserList!![model.name]!!)
//                        val money = user.money + model.money
//                        mUserPrepared = false
//                        FirebaseComm.setMoney(model.name, money)
//                        val historyModel = HistoryModel(
//                            System.currentTimeMillis(),
//                            model.name,
//                            Constants.HISTORY_DEPOSIT,
//                            model.money,
//                            mapOf(),
//                            ""
//                        )
//                        FirebaseComm.addHistory(historyModel)
//                    } else {
//                        FirebaseComm.addDepositItem(model)
//                    }
//                }
//
//            }
//        }
//        observerUser = Observer {
//            mUserList = it as Map<String, Map<String, Any>>?
//            mUserPrepared = true
//        }
//        observerDeposit?.let {
//            SmsManager.liveDepositArray.observeForever(it)
//        }
//        observerUser?.let {
//            FirebaseComm.liveUserList.observeForever(it)
//        }
//
//        mTimer = timer(initialDelay = 0, period = 60000)  {
//            Handler(Looper.getMainLooper()).post {
//                getUserList()
//                SmsManager.startCheck(this@DepositService)
//            }
//        }
//
//        val filter = IntentFilter()
//        filter.addAction("android.provider.Telephony.SMS_RECEIVED")
//        registerReceiver(smsReceiver, filter)

        startNotiService()
        return START_STICKY
    }
    fun getUserList() {
        mUserPrepared = false
        firebaseComm.getUsers()
    }
    val smsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.e("smsReceiver", "onReceive() called")

            val bundle = intent!!.extras
            val messages = parseSmsMessage(bundle!!)

            if (messages!!.isNotEmpty()) {
                val sender = messages[0]!!.originatingAddress
                val content = messages[0]!!.messageBody.toString()
                val date = Date(messages[0]!!.timestampMillis)
                Log.e("smsReceiver", "$sender [$content]")
                if(sender == "15993333" && content.contains("입금")) {
                    Handler(Looper.getMainLooper()).post {
                        getUserList()
                        SmsManager.startCheck(this@DepositService)
                    }
                }
            }
        }

        private fun parseSmsMessage(bundle: Bundle): Array<SmsMessage?>? {
            // PDU: Protocol Data Units
            val objs = bundle["pdus"] as Array<Any>?
            val messages: Array<SmsMessage?> = arrayOfNulls<SmsMessage>(objs!!.size)
            for (i in objs.indices) {
                messages[i] = SmsMessage.createFromPdu(objs[i] as ByteArray)
            }
            return messages
        }
    }

    override fun onDestroy() {
//        observerDeposit?.let {
//            SmsManager.liveDepositArray.removeObserver(it)
//        }
//        observerUser?.let {
//            FirebaseComm.liveUserList.removeObserver(it)
//        }
//        mTimer?.cancel()
//        try {
//            unregisterReceiver(smsReceiver)
//        } catch (e : Exception) {
//            e.printStackTrace()
//        }
        setAlarmTimer()
        super.onDestroy()
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

    open fun setAlarmTimer() {
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