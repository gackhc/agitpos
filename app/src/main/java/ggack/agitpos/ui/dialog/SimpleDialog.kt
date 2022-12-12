package ggack.agitpos.ui.dialog

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import ggack.agitpos.databinding.DialogSimpleBinding

class SimpleDialog(val context: Context, val type : Int, val resultCallback : ResultCallback?) {
    var binding : DialogSimpleBinding
    companion object{
        const val TYPE_ONE_BUTTON = 1
        const val TYPE_TWO_BUTTON = 2
    }
    private var dlg : Dialog = Dialog(context)

    init{
        dlg.requestWindowFeature(Window.FEATURE_NO_TITLE)
        binding = DialogSimpleBinding.inflate(LayoutInflater.from(context))
        dlg.setContentView(binding.root)

        binding.tvSimple.textAlignment = View.TEXT_ALIGNMENT_TEXT_START
    }
    fun setTitle(rid : Int) {
        binding.tvTitle.setText(rid)
        binding.tvTitle.visibility = View.VISIBLE
    }
    fun setText(str:String) {
        binding.tvSimple.text = str
    }
    fun setText(rid:Int){
        binding.tvSimple.setText(rid)
    }
    fun setCancelable(b : Boolean) {
        dlg.setCancelable(b)
    }
    fun isShowing() = dlg.isShowing
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
        dlg.show()
    }

    fun dismiss(){
        dlg.dismiss()
    }

    val onClickListener = View.OnClickListener{
        when(it){
            binding.btnOk, binding.btnConfirm->{
                dismiss()
                resultCallback?.onResult(Activity.RESULT_OK)
            }
            binding.btnCancel->{
                dismiss()
                resultCallback?.onResult(Activity.RESULT_CANCELED)
            }
        }
    }

    fun setAlignment(alignment : Int){
        binding.tvSimple.textAlignment = alignment
    }

    fun setCancelTxt(s : String) {
        binding.btnCancel.text = s
    }
    fun setOKTxt(s : String) {
        binding.btnOk.text = s
    }
    fun setConfirmTxt(s : String) {
        binding.btnConfirm.text = s
    }

    fun interface ResultCallback {
        fun onResult(result : Int)
    }
}