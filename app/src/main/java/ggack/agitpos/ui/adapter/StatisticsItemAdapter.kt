package ggack.agitpos.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import ggack.agitpos.data.model.StatisticsItemModel
import ggack.agitpos.data.model.StatisticsModel
import ggack.agitpos.databinding.ItemHistoryBinding
import java.time.format.DateTimeFormatter

class StatisticsItemAdapter(var list : MutableList<StatisticsItemModel>) : RecyclerView.Adapter<StatisticsItemAdapter.MyViewHolder>(){
    inner class MyViewHolder(var b : ItemHistoryBinding): RecyclerView.ViewHolder(b.root) {
        fun bind(item : StatisticsItemModel) {
            //item.nickname
            //item.item?.keys
            b.tv.setLines(item.items.size + 1)
            var str = "${item.date.format(DateTimeFormatter.ofPattern("yyyy년 MM월"))}"
            item.items.forEach {model ->
                str += "\n - ${model.name} : ${model.cnt} 개, ${model.price} 원"
            }
            b.tv.text = str
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