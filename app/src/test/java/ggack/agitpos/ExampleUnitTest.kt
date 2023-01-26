package ggack.agitpos

import android.app.Application
import android.app.Notification
import android.content.Context
import android.os.Bundle
import android.os.Parcel
import android.os.UserHandle
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import ggack.agitpos.data.model.networkmodel.DepositModel
import ggack.agitpos.data.model.networkmodel.FromMap
import ggack.agitpos.data.model.networkmodel.HistoryModel
import ggack.agitpos.service.NotiListenerService
import ggack.agitpos.utils.Constants
import ggack.agitpos.utils.Utils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.junit.Test

import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleUnitTest {

    @Test
    fun notiTest() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val notification = NotiListenerService()
        val bundle = Bundle()
        bundle.putString("android.text", "김*혜님이  100원을 보냈어요.\n김동혜")
        onNotificationPosted("com.kakaopay.app", bundle, context)
    }/*
E/onNotificationPosted: com.kakaopay.app : Bundle[mParcelledData.dataSize=3332]
E/noti: packageName = com.kakaopay.app
E/noti: body = 김*혜님이  100원을 보냈어요.
    김동혜
2023-01-16 21:35:14.359 17336-17336/ggack.agitpos E/noti: strArr = 김*혜님이  100원을 보냈어요.
2023-01-16 21:35:14.359 17336-17336/ggack.agitpos E/noti: strArr = 김동혜

    public StatusBarNotification(String pkg, String opPkg, int id,
            String tag, int uid, int initialPid, Notification notification, UserHandle user,
            String overrideGroupKey, long postTime) {
    */

    fun onNotificationPosted(packageName : String, extras : Bundle?, context : Context) {
        Log.e("onNotificationPosted", "${packageName} : ${extras}")
        try {
            val body = extras?.getString("android.text")
            val strArray = body?.split("\n")
            //Log.e("noti", "packageName = ${packageName}")
            //Log.e("noti", "body = $body")
            strArray?.forEach {
                //Log.e("noti", "strArr = $it")
            }
        } catch (e : Exception) {
            //Log.e("err", e.toString())
        }
        if(packageName?.contains("kakaopay") == true) {
            val body = extras?.getString("android.text")
            if (body?.contains("보냈어요.") == true) {
                try {
                    val strArray = body.split("\n")
                    val nickName = strArray[1]
                    val money = strArray[0].replace("  ", " ").split(" ").get(1).replace("원을", "").toInt()
                    val model = DepositModel(Instant.now().toEpochMilli(), money, nickName)
                } catch (e : Exception) {
                    Log.e("error", body)
                }
            }
        }
        else if(packageName == "com.samsung.android.messaging") {
            val body = extras?.getString("android.text")?:""
            val bankAccount = Utils.getPref(context, "bankAccount")
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
                }
            }
        }
    }
}