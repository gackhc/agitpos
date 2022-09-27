package ggack.agitpos.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import ggack.agitpos.R
import ggack.agitpos.data.model.FromMap
import ggack.agitpos.data.model.UserModel
import ggack.agitpos.databinding.ItemUserListBinding
import ggack.agitpos.listener.ListListener
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class UserAdapter(var context : Context, var map : Map<String, Map<String, Any>>, val listener : ListListener) : RecyclerView.Adapter<UserAdapter.MyViewHolder>(){
    var keys = map.keys.toList()
    inner class MyViewHolder(var b : ItemUserListBinding): RecyclerView.ViewHolder(b.root) {
        fun bind(name : String, item : UserModel?) {

            if(item?.member == LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"))) {
                b.tvName.text = "$name *"
                b.tvName?.setTextColor(context.getColor(R.color.red))
            } else {
                b.tvName.text = name
                b.tvName?.setTextColor(context.getColor(R.color.black))
            }
            b.tvMoney.text = (item?.money?:0).toString()
            b.btnMember.setOnClickListener {
                listener.onMember(name, item, item?.member != LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM")))
            }
            b.btnDelete.setOnClickListener {
                listener.onRemove(name, item)
            }
            b.btnMoney.setOnClickListener {
                listener.onChange(name, item)
            }
            b.btnRecord.setOnClickListener {
                listener.onRecord(name, item)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val binding = ItemUserListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val name = keys[position]
        val item = map[name]
        item?.let {
            holder.bind(name, FromMap.toUserModel(item))
        }
    }

    override fun getItemCount(): Int {
        return keys.size
    }
}