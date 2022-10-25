package ggack.agitpos.data.model.networkmodel

object FromMap {
    fun toDepositModel(map : Map<String, Any>) : DepositModel
            = DepositModel(if(map["date"] is Long) map["date"] as Long else (map["date"] as String).toLong(),
        (map["money"] as Long).toInt(), map["name"] as String)

    fun toUserModel(map : Map<String, Any>) : UserModel
            = UserModel(map["pin"] as String,
        (map["money"] as Long).toInt(),
        map["member"] as String?,
        map["extraName"] as String?,)

    fun toHistoryModel(map : Map<String, Any?>) : HistoryModel
            = HistoryModel(map["date"] as Long,
        map["nickname"] as String,
        map["kind"] as String,
        (map["money"] as Long).toInt(),
        map["item"] as Map<String, Map<String, Any>>?,
        map["etc"] as String)

    fun toAccountModel(map : Map<String, Any?>) : AccountModel
            = AccountModel((map["account"] as String?)?:"",
        (map["name"] as String?)?:"",
        (map["kakaopay"] as String?)?:"",)

    fun toItemModels(array : List<Map<String, Any>?>) : List<ItemModel> {
        val list = mutableListOf<ItemModel>()
        for((i, m) in array.withIndex()) {
            if(m != null) {
                list.add(
                    ItemModel(
                        m["name"] as String,
                    (m["price"] as Long).toInt() * (m["cnt"] as Long).toInt(),
                        (m["cnt"] as Long).toInt(),
                        i
                    ))
            }
        }
        return list
    }
    fun toItemModels(map : Map<String, Map<String, Any>>) : List<ItemModel> {
        val list = mutableListOf<ItemModel>()
        map.toSortedMap().forEach { (t, u) ->
            list.add(ItemModel(
                u["name"] as String,
                (u["price"] as Long).toInt() * (u["cnt"] as Long).toInt(),
                (u["cnt"] as Long).toInt()
            ))
        }
        return list
    }
}