package ggack.agitpos.utils

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ggack.agitpos.data.model.FromMap
import ggack.agitpos.data.model.UserModel
import ggack.agitpos.databinding.DialogSimpleListBinding
import ggack.agitpos.databinding.ItemSimpleListBinding
import ggack.agitpos.databinding.ItemUserListBinding
import ggack.agitpos.listener.ListListener

class SimpleListDialog(val context: Context, val list : List<String>, val resultCallback : ResultCallback?) {
    var binding : DialogSimpleListBinding
    companion object{
        const val TYPE_ONE_BUTTON = 1
        const val TYPE_TWO_BUTTON = 2
    }
    private var dlg : Dialog = Dialog(context)
    private var margin : Int = 10
    private var textSize : Int = 20
    init{
        dlg.requestWindowFeature(Window.FEATURE_NO_TITLE)
        binding = DialogSimpleListBinding.inflate(LayoutInflater.from(context))
        dlg.setContentView(binding.root)

        binding.rv.layoutManager = LinearLayoutManager(context)
        binding.rv.adapter = SimpleAdapter()
        binding.rv.adapter?.notifyDataSetChanged()

        binding.tvText.textAlignment = View.TEXT_ALIGNMENT_TEXT_START
    }
    fun setTitle(rid : Int) {
        binding.tvText.setText(rid)
        binding.tvText.visibility = View.VISIBLE
    }
    fun setTitle(str : String) {
        binding.tvText.setText(str)
        binding.tvText.visibility = View.VISIBLE
    }
    fun setCancelable(b : Boolean) {
        dlg.setCancelable(b)
    }
    fun isShowing() = dlg.isShowing
    fun show(){
        binding.btnOK.setOnClickListener(onClickListener)
        dlg.show()
    }

    fun dismiss(){
        dlg.dismiss()
    }

    fun setListMargin(i : Int) {
        margin = i
    }

    fun setListTextSize(i : Int) {
        textSize = i
    }
    val onClickListener = View.OnClickListener{
        when(it){
            binding.btnOK ->{
                dismiss()
            }
        }
    }

    fun setBtnTxt(s : String) {
        binding.btnOK.text = s
    }

    fun interface ResultCallback {
        fun onResult(result : String)
    }

    private inner class SimpleAdapter : RecyclerView.Adapter<SimpleAdapter.MyViewHolder>(){
        inner class MyViewHolder(var b : ItemSimpleListBinding): RecyclerView.ViewHolder(b.root) {
            fun bind(name : String) {
                b.tv.text = name
                b.root.setOnClickListener {
                    resultCallback?.onResult(name)
                    dismiss()
                }
                val lp = b.root.layoutParams as RecyclerView.LayoutParams
                lp.setMargins(10,margin,10,margin)
                b.root.layoutParams = lp
                b.tv.textSize = textSize.toFloat()
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
            val binding = ItemSimpleListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return MyViewHolder(binding)
        }

        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            holder.bind(list[position])
        }

        override fun getItemCount(): Int {
            return list.size
        }
    }
}