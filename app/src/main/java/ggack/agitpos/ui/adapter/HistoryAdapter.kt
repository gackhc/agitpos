package ggack.agitpos.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import ggack.agitpos.data.model.networkmodel.HistoryModel
import ggack.agitpos.databinding.ItemHistoryBinding
import ggack.agitpos.utils.Utils
import java.time.format.DateTimeFormatter

class HistoryAdapter(var list : MutableList<HistoryModel>, var onClickListener: View.OnClickListener) : RecyclerView.Adapter<HistoryAdapter.MyViewHolder>(){
    inner class MyViewHolder(var b : ItemHistoryBinding): RecyclerView.ViewHolder(b.root) {
        fun bind(item : HistoryModel) {
            b.item = item
            b.onClickListener = onClickListener
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val binding = ItemHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            holder.bind(list[position])
    }

    override fun getItemCount(): Int {
        return list.size
    }
}