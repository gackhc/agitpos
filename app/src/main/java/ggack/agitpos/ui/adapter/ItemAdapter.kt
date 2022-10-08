package ggack.agitpos.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import ggack.agitpos.data.model.networkmodel.ItemModel
import ggack.agitpos.databinding.ItemItemsBinding
import ggack.agitpos.listener.ListListener

class ItemAdapter(var list : MutableList<ItemModel>, val listener : ListListener) : RecyclerView.Adapter<ItemAdapter.MyViewHolder>(){
    inner class MyViewHolder(var b : ItemItemsBinding): RecyclerView.ViewHolder(b.root) {
        fun bind(item : ItemModel) {
            b.tvName.text = item.name
            b.tvPrice.text = "${item.price} pt"
            b.root.setOnClickListener {
                listener.onClick(item.name, item)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val binding = ItemItemsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            holder.bind(list[position])
    }

    override fun getItemCount(): Int {
        return list.size
    }
}