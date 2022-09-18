package ggack.agitpos.data.model

data class DepositModel(var date : String, var money : Int, var name : String){
    fun getMap() : Map<String, Any> = mapOf("date" to date,"money" to money, "name" to name)
}
