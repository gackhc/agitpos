package ggack.agitpos.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import ggack.agitpos.data.model.StatisticsItemModel
import ggack.agitpos.data.model.StatisticsModel
import ggack.agitpos.databinding.ItemHistoryBinding
import ggack.agitpos.databinding.ItemStatisticsBinding
import java.time.format.DateTimeFormatter

class StatisticsItemAdapter(var list : MutableList<StatisticsItemModel>) : RecyclerView.Adapter<StatisticsItemAdapter.MyViewHolder>(){
    inner class MyViewHolder(var b : ItemStatisticsBinding): RecyclerView.ViewHolder(b.root) {
        fun bind(item : StatisticsItemModel) {
            b.item = item
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val binding = ItemStatisticsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            holder.bind(list[position])
    }

    override fun getItemCount(): Int {
        return list.size
    }
}