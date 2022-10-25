package ggack.agitpos.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import ggack.agitpos.data.model.StatisticsModel
import ggack.agitpos.databinding.ItemHistoryBinding
import java.time.format.DateTimeFormatter

class StatisticsDailyAdapter(var list : MutableList<StatisticsModel>) : RecyclerView.Adapter<StatisticsDailyAdapter.MyViewHolder>(){
    inner class MyViewHolder(var b : ItemHistoryBinding): RecyclerView.ViewHolder(b.root) {
        fun bind(item : StatisticsModel) {
            //item.nickname
            //item.item?.keys
            b.tv.text = "${item.date.format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일"))} 입금액 : ${item.deposit} 결제액 : ${item.payment}"
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