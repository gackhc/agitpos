package ggack.agitpos.utils

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.view.inputmethod.InputMethodManager
import ggack.agitpos.databinding.DialogSimpleEditBinding

class SimpleEditDialog(val context: Context, val type : Int, val resultCallback : ResultCallback?) {
    var binding : DialogSimpleEditBinding
    companion object{
        const val TYPE_ONE_BUTTON = 1
        const val TYPE_TWO_BUTTON = 2

        const val RESULT_OK = 1
        const val RESULT_CANCEL = 2
    }
    private var dlg : Dialog = Dialog(context)
    init{
        dlg.requestWindowFeature(Window.FEATURE_NO_TITLE)
        binding = DialogSimpleEditBinding.inflate(LayoutInflater.from(context))
        dlg.setContentView(binding.root)
    }

    fun setText(str:String){
        binding.tvSimple.text = str
    }

    fun setEditText(str:String){
        binding.editText.setText(str)
    }

    fun show(){
        when(type){
            TYPE_ONE_BUTTON ->{
                binding.btnOk.visibility = View.GONE
                binding.btnCancel.visibility = View.GONE
                binding.btnConfirm.visibility = View.VISIBLE
            }
            TYPE_TWO_BUTTON ->{
                binding.btnOk.visibility = View.VISIBLE
                binding.btnCancel.visibility = View.VISIBLE
                binding.btnConfirm.visibility = View.GONE
            }
        }
        binding.btnOk.setOnClickListener(onClickListener)
        binding.btnCancel.setOnClickListener(onClickListener)
        binding.btnConfirm.setOnClickListener(onClickListener)
        dlg.setCancelable(false)
        dlg.show()
    }

    fun dismiss(){
        val imm: InputMethodManager? = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
        imm?.hideSoftInputFromWindow(binding.editText.windowToken, 0)
        dlg.dismiss()
    }

    val onClickListener = View.OnClickListener{
        when(it){
            binding.btnOk, binding.btnConfirm->{
                resultCallback?.onResult(RESULT_OK, binding.editText.text.toString())
                dismiss()
            }
            binding.btnCancel->{
                resultCallback?.onResult(RESULT_CANCEL, "")
                dismiss()
            }
        }
    }

    fun interface ResultCallback{
        fun onResult(result : Int, string : String)
    }
}