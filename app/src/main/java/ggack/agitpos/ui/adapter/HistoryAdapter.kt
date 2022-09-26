package ggack.agitpos.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import ggack.agitpos.data.model.FromMap
import ggack.agitpos.data.model.HistoryModel
import ggack.agitpos.data.model.ItemModel
import ggack.agitpos.data.model.UserModel
import ggack.agitpos.databinding.ItemHistoryBinding
import ggack.agitpos.databinding.ItemItemsBinding
import ggack.agitpos.databinding.ItemUserListBinding
import ggack.agitpos.listener.ListListener
import ggack.agitpos.utils.Utils
import java.time.format.DateTimeFormatter

class HistoryAdapter(var list : MutableList<HistoryModel>) : RecyclerView.Adapter<HistoryAdapter.MyViewHolder>(){
    inner class MyViewHolder(var b : ItemHistoryBinding): RecyclerView.ViewHolder(b.root) {
        fun bind(item : HistoryModel) {
            item.nickname
            item.item?.keys

            val date = Utils.getDate(item.date).format(DateTimeFormatter.ofPattern("dd일 hh:mm:ss"))
            var text = "$date : [${item.nickname}] [${item.kind}]"
            if(item.money != 0)
                text += " [금액 : ${item.money}]"
            if(item.item?.keys != null && item.item?.keys?.isNotEmpty() == true)
                text += " ${item.item?.keys}"
            if(item.etc.isNotEmpty())
                text += " [기타 : ${item.etc}]"

            b.tv.text = text
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