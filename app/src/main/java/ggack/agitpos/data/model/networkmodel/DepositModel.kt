package ggack.agitpos.data.model.networkmodel

data class DepositModel(var date : Long, var money : Int, var name : String){
    fun getMap() : Map<String, Any> = mapOf("date" to date,"money" to money, "name" to name)
}
