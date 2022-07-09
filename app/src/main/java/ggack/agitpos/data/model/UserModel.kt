package ggack.agitpos.data.model

import com.google.firebase.database.Exclude

data class UserModel(var pin : String, var money : Int) {
    fun getMap() : Map<String, Any> = mapOf("pin" to pin, "money" to money)
}

