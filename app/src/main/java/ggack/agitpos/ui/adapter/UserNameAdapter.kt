package ggack.agitpos.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import ggack.agitpos.data.firebase.FirebaseComm
import ggack.agitpos.data.model.FromMap
import ggack.agitpos.data.model.UserModel
import ggack.agitpos.databinding.ItemUserNameBinding
import ggack.agitpos.listener.ListListener

class UserNameAdapter(var map : Map<String, Map<String, Any>>, val listener : ListListener) : RecyclerView.Adapter<UserNameAdapter.MyViewHolder>(){
    var keys = map.keys.toList()
    inner class MyViewHolder(var b : ItemUserNameBinding): RecyclerView.ViewHolder(b.root) {
        fun bind(name : String, item : UserModel?) {
            b.tvName.text = name
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val binding = ItemUserNameBinding.inflate(LayoutInflater.from(parent.context), parent, false)
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