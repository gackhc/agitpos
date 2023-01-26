package ggack.agitpos.utils

import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.journeyapps.barcodescanner.BarcodeEncoder
import ggack.agitpos.data.model.networkmodel.AccountModel
import java.util.*


@BindingAdapter("app:bankAccount", "app:viewMode")
fun setAccountTextVisibility(view : View, account : AccountModel?, viewMode : Boolean) {
    if(account?.account.isNullOrEmpty() || !viewMode)
        view.visibility = View.GONE
    else
        view.visibility = View.VISIBLE
}
@BindingAdapter("app:setAccountText")
fun setAccountText(view : TextView, account : AccountModel?) {
    view.text = "카카오뱅크 ${account?.account} ${account?.name}"
}

@BindingAdapter("app:kakaoPayAccount", "app:viewMode")
fun setKakaoTextVisibility(view : View, account : AccountModel?, viewMode : Boolean) {
    if(account?.kakaopay.isNullOrEmpty() || !viewMode)
        view.visibility = View.GONE
    else
        view.visibility = View.VISIBLE
}
@BindingAdapter("app:setKakaoQR")
fun setKakaoQR(view : ImageView, account : AccountModel?) {
    if(account?.kakaopay.isNullOrEmpty()) {
        view.visibility = View.GONE
    } else {
        view.visibility = View.VISIBLE
        val hints: Hashtable<EncodeHintType, String> = Hashtable()
        hints[EncodeHintType.CHARACTER_SET] = "UTF-8"
        view.setImageBitmap(BarcodeEncoder().createBitmap(MultiFormatWriter().encode(account?.kakaopay, BarcodeFormat.QR_CODE, 1000, 1000, hints)))
    }
}