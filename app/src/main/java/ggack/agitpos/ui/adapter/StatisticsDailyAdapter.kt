package ggack.agitpos.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import ggack.agitpos.data.model.StatisticsModel
import ggack.agitpos.databinding.ItemStatisticsBinding

class StatisticsDailyAdapter(var list : MutableList<StatisticsModel>) : RecyclerView.Adapter<StatisticsDailyAdapter.MyViewHolder>(){
    inner class MyViewHolder(var b : ItemStatisticsBinding): RecyclerView.ViewHolder(b.root) {
        fun bind(item : StatisticsModel) {
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