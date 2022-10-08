package ggack.agitpos.data.model.networkmodel

data class AccountModel(var account : String = "", var name : String = "", var kakaopay  :String = "") {
    fun getMap() : Map<String, Any?> = mapOf("account" to account, "name" to name, "kakaopay" to kakaopay)
}

