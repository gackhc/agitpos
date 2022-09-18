package ggack.agitpos.data.model

import com.google.firebase.database.Exclude

data class ItemModel(var name : String, var price : Int, var cnt : Int, var child : ItemModel?)

