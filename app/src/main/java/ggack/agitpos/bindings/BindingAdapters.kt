package ggack.agitpos.bindings

import android.widget.EditText
import androidx.databinding.BindingAdapter
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