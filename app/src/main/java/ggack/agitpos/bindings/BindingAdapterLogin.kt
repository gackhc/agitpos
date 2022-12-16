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


@BindingAdapter("app:setAccountTextVisibility")
fun setAccountTextVisibility(view : TextView, account : AccountModel?) {
    if(account?.account?.isNotEmpty() == true)
        view.visibility = View.VISIBLE
    else
        view.visibility = View.GONE
}
@BindingAdapter("app:setAccountText")
fun setAccountText(view : TextView, account : AccountModel?) {
    view.text = "카카오뱅크 ${account?.account} ${account?.name}"
}

@BindingAdapter("app:setKakaoTextVisibility")
fun setKakaoTextVisibility(view : TextView, account : AccountModel?) {
    if(account?.kakaopay?.isNotEmpty() == true)
        view.visibility = View.VISIBLE
    else
        view.visibility = View.GONE
}
@BindingAdapter("app:setKakaoQR")
fun setKakaoQR(view : ImageView, account : AccountModel?) {
    account?.kakaopay?.let {
        view.visibility = View.VISIBLE
        val hints: Hashtable<EncodeHintType, String> = Hashtable()
        hints[EncodeHintType.CHARACTER_SET] = "UTF-8"
        view.setImageBitmap(BarcodeEncoder().createBitmap(MultiFormatWriter().encode(it, BarcodeFormat.QR_CODE, 1000, 1000, hints)))
    } ?: run {
        view.visibility = View.GONE
    }
}