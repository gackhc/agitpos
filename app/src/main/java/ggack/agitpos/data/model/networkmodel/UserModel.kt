package ggack.agitpos.data.model.networkmodel

data class UserModel(var pin : String, var money : Int, var member : String? = "") {
    fun getMap() : Map<String, Any?> = mapOf("pin" to pin, "money" to money, "member" to member)
}

