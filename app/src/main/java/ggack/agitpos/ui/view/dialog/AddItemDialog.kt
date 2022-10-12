package ggack.agitpos.ui.view.dialog

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.view.inputmethod.InputMethodManager
import ggack.agitpos.databinding.DialogAddItemBinding

class AddItemDialog(val context: Context, val resultCallback : ResultCallback?) {
    var binding : DialogAddItemBinding
    private var dlg : Dialog = Dialog(context)
    init{
        dlg.requestWindowFeature(Window.FEATURE_NO_TITLE)
        binding = DialogAddItemBinding.inflate(LayoutInflater.from(context))
        dlg.setContentView(binding.root)
    }

    fun setText(str:String){
        binding.tvSimple.text = str
    }

    fun show(){
        binding.btnConfirm.setOnClickListener(onClickListener)
        dlg.setCancelable(false)
        dlg.show()
    }

    fun dismiss(){
        val imm: InputMethodManager? = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
        imm?.hideSoftInputFromWindow(binding.editTextPrice.windowToken, 0)
        imm?.hideSoftInputFromWindow(binding.editTextName.windowToken, 0)
        dlg.dismiss()
    }

    val onClickListener = View.OnClickListener{
        when(it){
            binding.btnConfirm->{
                resultCallback?.onResult(binding.editTextName.text.toString(),
                    binding.editTextPrice.text.toString().toInt())
                dismiss()
            }
        }
    }

    fun interface ResultCallback{
        fun onResult(name : String, price : Int)
    }
}