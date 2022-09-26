package ggack.agitpos.data.model

object FromMap {
    fun toDepositModel(map : Map<String, Any>) : DepositModel
            = DepositModel(if(map["date"] is Long) map["date"] as Long else (map["date"] as String).toLong(),
        (map["money"] as Long).toInt(), map["name"] as String)

    fun toUserModel(map : Map<String, Any>) : UserModel
            = UserModel(map["pin"] as String,
        (map["money"] as Long).toInt(),
        map["member"] as String?)

    fun toHistoryModel(map : Map<String, Any?>) : HistoryModel
            = HistoryModel(map["date"] as Long,
        map["nickname"] as String,
        map["kind"] as String,
        (map["money"] as Long).toInt(),
        map["item"] as Map<String, Map<String, Any>>?,
        map["etc"] as String)
}