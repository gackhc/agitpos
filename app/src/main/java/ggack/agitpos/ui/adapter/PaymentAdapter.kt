package ggack.agitpos.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import ggack.agitpos.data.model.networkmodel.ItemModel
import ggack.agitpos.databinding.ItemItemPaymentBinding
import ggack.agitpos.listener.ItemCountChangeListener
import ggack.agitpos.listener.ListListener

class PaymentAdapter(var list : MutableList<ItemModel>, val listener : ListListener) : RecyclerView.Adapter<PaymentAdapter.MyViewHolder>(){
    inner class MyViewHolder(var b : ItemItemPaymentBinding): RecyclerView.ViewHolder(b.root) {
        fun bind(item : ItemModel) {
            b.item = item
            b.listener = object : ItemCountChangeListener {
                override fun up() {
                    val p = list.position(item.name)
                    if(p >= 0) {
                        item.cnt++
                        if(item.name == "운영진 출첵")
                            item.cnt = 1
                        notifyItemChanged(p)
                        listener.onChange(p)
                    }
                }

                override fun down() {
                    val p = list.position(item.name)
                    if(p >= 0) {
                        item.cnt--
                        if(item.cnt > 0) {
                            notifyItemChanged(p)
                        } else {
                            list.removeAt(p)
                            notifyItemRemoved(p)
                        }
                        listener.onChange(p)
                    }
                }
            }
        }
    }
    fun MutableList<ItemModel>.position(s : String): Int = run {
        for ((i, model) in this.withIndex()) {
            if(model.name == s)
                return i
        }
        return -1
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val binding = ItemItemPaymentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.bind(list[position])
    }

    override fun getItemCount(): Int {
        return list.size
    }
}