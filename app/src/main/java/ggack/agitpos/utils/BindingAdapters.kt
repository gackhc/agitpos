package ggack.agitpos.utils

import android.widget.EditText
import androidx.databinding.BindingAdapter
import java.util.*

interface OnFocusLostListener {
    fun onFocusLost(view : EditText)
}

@BindingAdapter("app:onFocusLost")
fun EditText.onFocusLost(callback : OnFocusLostListener) {
    setOnFocusChangeListener { v, hasFocus ->
        if(!hasFocus) callback.onFocusLost(this)
    }
}