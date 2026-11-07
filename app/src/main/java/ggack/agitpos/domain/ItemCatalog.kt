package ggack.agitpos.domain

import ggack.agitpos.data.model.networkmodel.ItemModel

object ItemCatalog {
    fun decode(value: Any?): List<ItemModel> {
        val entries = when (value) {
            null -> emptyList()
            is Map<*, *> -> value.entries.map { (key, item) -> key.toString().toInt() to item }
            is List<*> -> value.mapIndexed { index, item -> index to item }
            else -> error("상품 데이터 형식이 올바르지 않습니다.")
        }
        return entries.sortedBy { it.first }.mapNotNull { (index, value) ->
            if (value == null) null else {
                val item = Ledger.record(value)
                ItemModel(item["name"] as String, Ledger.amount(item["price"]),
                    Ledger.amount(item["cnt"]), index)
            }
        }
    }

    fun move(items: MutableList<ItemModel>, from: Int, to: Int): Boolean {
        if (from !in items.indices || to !in items.indices || from == to ||
            items[from].viewType == -1 || items[to].viewType == -1) return false
        items.add(to, items.removeAt(from))
        items.filter { it.viewType != -1 }.forEachIndexed { index, item -> item.idx = index + 1 }
        return true
    }
}
