package ggack.agitpos.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.lifecycle.Observer
import ggack.agitpos.data.model.DepositModel
import ggack.agitpos.data.sms.SmsManager
import java.util.*
import kotlin.concurrent.timer

class DepositService : Service() {
    private var mTimer : Timer? = null
    private var observer : Observer<Any?>? = null
    override fun onBind(intent: Intent?): IBinder? {
        throw UnsupportedOperationException("Not yet")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        mTimer?.cancel()
        //TODO  여기서 부터 해야함.
        // 닉네임 확인해서 있으면 데이터 반영하고
        // 닉네임 없으면 deposit 리스트에 넣기
        // sms 도착시, 카페 도착시 체크하도록 하는 것 넣기
        observer = Observer {
            val list = it as List<DepositModel>
            list.forEach {

            }
        }
        observer?.let {
            SmsManager.liveDepositArray.observeForever(it)
        }
        mTimer = timer(initialDelay = 0, period = 10000) {
            //SmsManager.startCheck(this@DepositService)
        }
        return START_STICKY
    }

    override fun onDestroy() {
        observer?.let {
            SmsManager.liveDepositArray.removeObserver(it)
        }
        mTimer?.cancel()
        super.onDestroy()
    }
}