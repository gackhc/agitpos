package ggack.agitpos.data.model.networkmodel

data class ItemModel(var name : String, var price : Int, var cnt : Int, var idx : Int = 0,var viewType : Int = 0) {
    fun getMap() : Map<String, Any> = mapOf(
        "name" to name,
        "price" to price,
        "cnt" to cnt
    )
}

