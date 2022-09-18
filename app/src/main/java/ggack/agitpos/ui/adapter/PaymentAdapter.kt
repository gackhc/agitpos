package ggack.agitpos.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import ggack.agitpos.data.model.ItemModel
import ggack.agitpos.databinding.ItemItemPaymentBinding
import ggack.agitpos.databinding.ItemItemsBinding
import ggack.agitpos.listener.ListListener

class PaymentAdapter(var list : MutableList<ItemModel>, val listener : ListListener) : RecyclerView.Adapter<PaymentAdapter.MyViewHolder>(){
    inner class MyViewHolder(var b : ItemItemPaymentBinding): RecyclerView.ViewHolder(b.root) {
        fun bind(item : ItemModel, position : Int) {
            b.tvName.text = item.name
            b.tvCount.text = item.cnt.toString()
            b.tvPrice.text = (item.cnt * item.price).toString()
            b.cvUp.setOnClickListener {
                item.cnt++
                notifyItemChanged(position)
                listener.onChange(position)
            }
            b.cvDown.setOnClickListener {
                item.cnt--
                if(item.cnt > 0)
                    notifyItemChanged(position)
                else {
                    list.removeAt(position)
                    notifyItemRemoved(position)
                }
                listener.onChange(position)
            }

            b.root.setOnClickListener {
                listener.onClick(item.name, item.price)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val binding = ItemItemPaymentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            holder.bind(list[position], position)
    }

    override fun getItemCount(): Int {
        return list.size
    }
}