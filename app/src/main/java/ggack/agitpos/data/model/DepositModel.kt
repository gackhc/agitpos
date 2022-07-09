package ggack.agitpos.data.model

data class DepositModel(var money : Int, var name : String){
    fun getMap() : Map<String, Any> = mapOf("money" to money, "name" to name)
}
