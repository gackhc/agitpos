package ggack.agitpos.data.model.networkmodel

data class ItemModel(var name : String, var price : Int, var cnt : Int) {
    fun getMap() : Map<String, Any> = mapOf(
        "name" to name,
        "price" to price,
        "cnt" to cnt
    )
}

