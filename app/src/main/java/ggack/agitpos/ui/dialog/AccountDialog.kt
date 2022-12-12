package ggack.agitpos.ui.dialog

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.view.inputmethod.InputMethodManager
import ggack.agitpos.databinding.DialogAccountBinding

class AccountDialog(val context: Context, val resultCallback : ResultCallback?) {
    var binding : DialogAccountBinding
    private var dlg : Dialog = Dialog(context)
    init{
        dlg.requestWindowFeature(Window.FEATURE_NO_TITLE)
        binding = DialogAccountBinding.inflate(LayoutInflater.from(context))
        dlg.setContentView(binding.root)
    }

    fun setText(str:String){
        binding.tvSimple.text = str
    }

    fun setEditText(account : String, name : String, kakaopay : String){
        binding.editTextAccount.setText(account)
        binding.editTextName.setText(name)
        binding.editTextKakaoPay.setText(kakaopay)
    }

    fun show(){
        binding.btnConfirm.setOnClickListener(onClickListener)
        dlg.setCancelable(false)
        dlg.show()
    }

    fun dismiss(){
        val imm: InputMethodManager? = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
        imm?.hideSoftInputFromWindow(binding.editTextAccount.windowToken, 0)
        imm?.hideSoftInputFromWindow(binding.editTextName.windowToken, 0)
        dlg.dismiss()
    }

    val onClickListener = View.OnClickListener{
        when(it){
            binding.btnConfirm->{
                resultCallback?.onResult(binding.editTextAccount.text.toString(),
                    binding.editTextName.text.toString(),
                    binding.editTextKakaoPay.text.toString())
                dismiss()
            }
        }
    }

    fun interface ResultCallback{
        fun onResult(account : String, name : String, kakaopay : String)
    }
}