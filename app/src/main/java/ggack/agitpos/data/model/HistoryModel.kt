package ggack.agitpos.data.model

data class HistoryModel(
    var date : Long,
    var nickname : String,
    var kind : String,
    var money : Int,
    var item : Map<String, Map<String, Any>>? = mapOf(),
    var etc : String = ""
    ) {
    fun getMap() : Map<String, Any?> = mapOf(
        "date" to date,
        "nickname" to nickname,
        "kind" to kind,
        "money" to money,
        "item" to item,
        "etc" to etc
    )
}

