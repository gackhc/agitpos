package ggack.agitpos.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import ggack.agitpos.BuildConfig
import ggack.agitpos.data.model.networkmodel.FromMap
import ggack.agitpos.data.model.networkmodel.UserModel
import ggack.agitpos.databinding.ItemUserListBinding
import ggack.agitpos.listener.ListListener

class UserAdapter(var map : Map<String, Map<String, Any>>, val listener : ListListener) : RecyclerView.Adapter<UserAdapter.MyViewHolder>(){
    var keys = map.keys.toList()
    inner class MyViewHolder(var b : ItemUserListBinding): RecyclerView.ViewHolder(b.root) {
        fun bind(name : String, item : UserModel?) {
            b.item = item
            b.name = name
            b.listener = listener
            b.isDebug = BuildConfig.DEBUG
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