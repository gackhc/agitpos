package ggack.agitpos.data.model.networkmodel

data class UserModel(var pin : String, var money : Int, var member : String? = "", var extraName : String? = "", var isSupervisor : Boolean? = false, var svBonus : Int? = 202212010) {
    fun getMap() : Map<String, Any?> = mapOf("pin" to pin, "money" to money, "member" to member, "extraName" to extraName, "isSupervisor" to isSupervisor, "svBonus" to svBonus)
}

