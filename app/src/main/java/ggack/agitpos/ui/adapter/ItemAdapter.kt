package ggack.agitpos.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import ggack.agitpos.data.model.networkmodel.ItemModel
import ggack.agitpos.databinding.ItemItemAddBinding
import ggack.agitpos.databinding.ItemItemsBinding
import ggack.agitpos.listener.ListListener

class ItemAdapter(var list : MutableList<ItemModel>, val listener : ListListener) : RecyclerView.Adapter<RecyclerView.ViewHolder>(){
    inner class MyViewHolder(var b : ItemItemsBinding): RecyclerView.ViewHolder(b.root) {
        fun bind(item : ItemModel) {
            b.tvName.text = item.name
            b.tvPrice.text = "${item.price} pt"
            b.root.setOnClickListener {
                listener.onClick(item.name, item)
            }
//            b.root.setOnLongClickListener {
//                listener.onRemove(item.name, item)
//                true
//            }
        }
    }
    inner class AddHolder(var b : ItemItemAddBinding): RecyclerView.ViewHolder(b.root) {
        fun bind() {
            b.root.setOnClickListener {
                listener.onADD("", null)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        when (viewType) {
            0 ->
                return MyViewHolder(ItemItemsBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            else ->
                return AddHolder(ItemItemAddBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }

    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is MyViewHolder)
            holder.bind(list[position])
        else if (holder is AddHolder)
            holder.bind()
    }

    override fun getItemViewType(position: Int): Int {
        return list[position].viewType
    }

    override fun getItemCount(): Int {
        return list.size
    }
}