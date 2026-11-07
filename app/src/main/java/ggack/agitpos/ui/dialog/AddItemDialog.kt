package ggack.agitpos.ui.dialog

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
    fun setText(name : String, price : Int) {
        binding.editTextName.setText(name)
        binding.editTextPrice.setText(price.toString())
    }

    fun show(){
        binding.btnDelete.setOnClickListener(onClickListener)
        binding.btnConfirm.setOnClickListener(onClickListener)
        binding.btnCancel.setOnClickListener(onClickListener)
        dlg.setCancelable(true)
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
                val name = binding.editTextName.text.toString().trim()
                val price = binding.editTextPrice.text.toString().toIntOrNull()
                if (!ggack.agitpos.domain.Ledger.validKey(name)) {
                    binding.editTextName.error = "이름을 확인해 주세요 (. # $ [ ] / 제외)"
                    return@OnClickListener
                }
                if (price == null) {
                    binding.editTextPrice.error = "올바른 금액을 입력해 주세요"
                    return@OnClickListener
                }
                resultCallback?.onResult(name, price)
                dismiss()
            }
            binding.btnDelete->{
                resultCallback?.onDelete()
                dismiss()
            }
            binding.btnCancel->{
                dismiss()
            }
        }
    }

    interface ResultCallback{
        fun onResult(name : String, price : Int)
        fun onDelete()
    }
}