package ggack.agitpos.data.model

object FromMap {
    fun toDepositModel(map : Map<String, Any>) : DepositModel
            = DepositModel((map["money"] as Long).toInt(), map["name"] as String)
    fun toUserModel(map : Map<String, Any>) : UserModel
            = UserModel(map["pin"] as String, (map["money"] as Long).toInt())
}