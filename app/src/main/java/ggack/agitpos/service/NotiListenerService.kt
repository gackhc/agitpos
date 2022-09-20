 package ggack.agitpos.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.lifecycle.Observer
import ggack.agitpos.data.firebase.FirebaseComm
import ggack.agitpos.data.model.DepositModel
import ggack.agitpos.data.model.FromMap
import ggack.agitpos.data.model.HistoryModel
import ggack.agitpos.utils.Constants
import ggack.agitpos.utils.Utils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.Exception
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


 class NotiListenerService: NotificationListenerService() {
     private var observerUser : Observer<Any?>? = null
     private var mUserPrepared = false
     private var mUserList : Map<String, Map<String, Any>>? = null

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
         if(sbn?.packageName == "com.kakao.talk") {
             val body = sbn?.notification?.extras?.getString("android.text")
             if (body?.contains("코드송금 받기가 완료되었습니다.") == true) {
                 val datetime = LocalDateTime.now()
                 val sdf = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS")
                 val strArray = body.split("\n")

                 var nickName : String? = null
                 var money : Int? = null
                 for(str in strArray) {
                     if(str.contains("받은금액 :")) {
                         money =
                         str.trim().replace("-받은금액:", "")
                             .replace(",", "")
                             .replace("원", "").toInt()
                     } else if(str.contains("메시지 :")) {
                         nickName =
                             str.trim().replace("-메시지:", "").lowercase()
                     }
                 }
                if(nickName != null && money != null) {
                    val model = DepositModel(datetime.format(sdf), money, nickName)
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
                                ""
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
}