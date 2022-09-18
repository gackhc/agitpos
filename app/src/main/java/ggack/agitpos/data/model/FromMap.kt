package ggack.agitpos.data.model

object FromMap {
    fun toDepositModel(map : Map<String, Any>) : DepositModel
            = DepositModel(map["date"] as String, (map["money"] as Long).toInt(), map["name"] as String)
    fun toUserModel(map : Map<String, Any>) : UserModel
            = UserModel(map["pin"] as String, (map["money"] as Long).toInt())
    fun toRecordModel(map : Map<String, Any>) : RecordModel
            = RecordModel(map["date"] as Long,
        map["nickname"] as String,
        map["kind"] as String,
        (map["money"] as Long).toInt(),
        map["item"] as String)
}