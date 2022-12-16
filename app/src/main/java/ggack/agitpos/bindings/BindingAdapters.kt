package ggack.agitpos.bindings

import android.annotation.SuppressLint
import android.app.Application
import android.graphics.Color
import android.widget.EditText
import android.widget.TextView
import androidx.databinding.BindingAdapter
import ggack.agitpos.R
import ggack.agitpos.data.model.StatisticsItemModel
import ggack.agitpos.data.model.StatisticsModel
import ggack.agitpos.data.model.StatisticsMonthModel
import ggack.agitpos.data.model.networkmodel.DepositModel
import ggack.agitpos.data.model.networkmodel.HistoryModel
import ggack.agitpos.data.model.networkmodel.ItemModel
import ggack.agitpos.data.model.networkmodel.UserModel
import ggack.agitpos.utils.Utils
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

interface FocusListener {
    fun onFocusChange(view : EditText, focus : Boolean)
}

@BindingAdapter("app:onFocusChange")
fun EditText.onFocusChange(callback : FocusListener) {
    setOnFocusChangeListener { v, hasFocus ->
        callback.onFocusChange(this, hasFocus)
    }
}
@BindingAdapter("app:setHistroyText")
fun setHistroyText(v : TextView, item : HistoryModel) {
    val date = Utils.getDate(item.date).format(DateTimeFormatter.ofPattern("dd일 HH:mm:ss"))
    var text = "$date : [${item.nickname}] [${item.kind}]"
    if(item.money != 0)
        text += " [금액 : ${item.money}]"
    if(item.item?.keys != null && item.item?.keys?.isNotEmpty() == true)
        text += " ${item.item?.keys}"
    if(item.etc.isNotEmpty())
        text += " [기타 : ${item.etc}]"

    v.text = text
}
@BindingAdapter("app:setStatisticsText")
fun setStatisticsText(v : TextView, item : Any) {
    when {
        item is StatisticsModel -> {
            v.text = "${item.date.format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일"))} 입금액 : ${item.deposit} 결제액 : ${item.payment}"
        }
        item is StatisticsItemModel -> {
            v.setLines(item.items.size + 1)
            var str = "${item.date.format(DateTimeFormatter.ofPattern("yyyy년 MM월"))}"
            item.items.forEach {model ->
                str += "\n - ${model.name} : ${model.cnt} 개, ${model.price} 원"
            }
            v.text = str
        }
        item is StatisticsMonthModel -> {
            v.text = "${item.date.format(DateTimeFormatter.ofPattern("yyyy년 MM월"))} 총 입금액 : ${item.deposit} 총 결제액 : ${item.payment}"
        }
    }
}
@BindingAdapter("app:setDepositDateText")
fun setDepositDateText(v : TextView, item : DepositModel) {
    val localdate = Utils.getDate(item.date)
    val strdate = localdate.format(DateTimeFormatter.ofPattern("MM-dd\nHH:mm:ss"))
    v.text = strdate
}
@BindingAdapter("app:setItemsPriceText")
fun setDepositDateText(v : TextView, item : ItemModel) {
    v.text = "￦ ${item.price}"
}
@BindingAdapter("app:name", "app:member")
fun setMemberText(v : TextView, name : String, member : String) {
    if(member == LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"))) {
        v.text = "$name *"
        v.setTextColor(v.context.getColor(R.color.red))
    } else {
        v.text = name
        v.setTextColor(v.context.getColor(R.color.black))
    }
}