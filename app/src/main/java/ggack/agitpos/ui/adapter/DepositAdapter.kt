package ggack.agitpos.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import ggack.agitpos.data.model.networkmodel.DepositModel
import ggack.agitpos.data.model.networkmodel.FromMap
import ggack.agitpos.databinding.ItemDepositBinding
import ggack.agitpos.listener.ListListener
import ggack.agitpos.utils.Utils
import java.time.format.DateTimeFormatter

class DepositAdapter(var map : Map<String, Map<String, Any>>, val listener : ListListener) : RecyclerView.Adapter<DepositAdapter.MyViewHolder>(){
    var keys = map.keys.toMutableList()
    inner class MyViewHolder(var b: ItemDepositBinding): RecyclerView.ViewHolder(b.root) {
        fun bind(date : String, item : DepositModel?) {
            val localdate = Utils.getDate(item?.date?:0)
            val strdate = localdate.format(DateTimeFormatter.ofPattern("MM-dd\nHH:mm:ss"))
            b.tvName.text = item?.name
            b.tvMoney.text = item?.money.toString()
            b.tvDate.text = strdate
            b.btnSetValue.setOnClickListener {
                listener.onClick(date, item)
            }
            b.btnAdd.setOnClickListener {
                listener.onADD(date, item)
            }
            b.btnDelete.setOnClickListener {
                listener.onRemove(date, item)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val binding = ItemDepositBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val date = keys[position]
        val item = map[date]
        item?.let {
            holder.bind(date, FromMap.toDepositModel(item))
        }
    }

    override fun getItemCount(): Int {
        return keys.size
    }
}