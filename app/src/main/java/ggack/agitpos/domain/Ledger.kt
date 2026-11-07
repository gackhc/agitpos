package ggack.agitpos.domain

import ggack.agitpos.data.model.networkmodel.ItemModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

sealed class LedgerAction {
    data class Payment(val name: String, val items: List<ItemModel>, val operator: String = "") : LedgerAction()
    data class Deposit(val name: String, val money: Int, val source: String) : LedgerAction()
    data class AssignDeposits(val name: String, val keys: List<String>) : LedgerAction()
    data class SetBalance(val name: String, val expected: Int, val money: Int) : LedgerAction()
}

data class LedgerRequest(val id: String, val date: Long, val action: LedgerAction)

/** Pure transaction reducer. It never mutates the supplied snapshot or performs side effects. */
class Ledger(private val zone: ZoneId = ZoneId.systemDefault()) {
    fun apply(snapshot: Map<String, Any?>, request: LedgerRequest): Map<String, Any?> {
        require(request.id.matches(Regex("[A-Za-z0-9_-]+"))) { "거래 ID가 올바르지 않습니다." }
        if (record(snapshot["processedOperations"]).containsKey(request.id)) return snapshot
        val root = snapshot.toMutableMap()
        val users = record(root["users"]).toMutableMap()
        val deposits = record(root["deposits"]).toMutableMap()
        val history = record(root["history"]).toMutableMap()
        val now = Instant.ofEpochMilli(request.date).atZone(zone).toLocalDateTime()

        fun user(name: String): MutableMap<String, Any?> {
            require(users.containsKey(name)) { "사용자를 찾을 수 없습니다. 새로고침해 주세요." }
            return record(users[name]).toMutableMap()
        }
        fun addHistory(id: String, date: Long, name: String, kind: String, money: Int,
                       items: Map<String, Any?> = emptyMap(), etc: String = "") {
            val month = Instant.ofEpochMilli(date).atZone(zone).format(DateTimeFormatter.ofPattern("yyyyMM"))
            val entries = record(history[month]).toMutableMap()
            entries[id] = mapOf("date" to date, "nickname" to name, "kind" to kind,
                "money" to money, "item" to items, "etc" to etc)
            history[month] = entries
        }
        when (val action = request.action) {
            is LedgerAction.Payment -> {
                require(action.items.isNotEmpty()) { "결제할 항목을 선택해 주세요." }
                require(action.items.map { it.name }.distinct().size == action.items.size) { "중복된 결제 항목입니다." }
                val customer = user(action.name)
                var balance = amount(customer["money"])
                var total = 0
                val items = linkedMapOf<String, Any?>()
                for (item in action.items) {
                    require(item.cnt > 0 && (item.name.contains("운영진 출첵") || validKey(item.name))) { "항목 이름과 수량을 확인해 주세요." }
                    if (item.name.contains("운영진 출첵")) {
                        require(customer["isSupervisor"] == true) { "운영진만 출석 적립을 할 수 있습니다." }
                        val previous = (customer["svBonus"] as? Number)?.toLong() ?: 2022120100L
                        val today = now.toLocalDate()
                        val count = requireNotNull(Attendance.nextCount(previous, today)) { "출석 적립은 하루 1회, 한 달 8회까지 가능합니다." }
                        balance = Math.addExact(balance, 6500)
                        customer["svBonus"] = today.format(DateTimeFormatter.BASIC_ISO_DATE).toLong() * 100 + count
                        items["운영진 출첵 $count|8"] = item.getMap()
                    } else {
                        total = Math.addExact(total, Math.multiplyExact(item.price, item.cnt))
                        items[item.name] = item.getMap()
                    }
                }
                customer["money"] = Math.subtractExact(balance, total)
                val expiry = listOfNotNull(
                    if (items.containsKey("월간 회원권")) Membership.monthly(now) else null,
                    if (items.containsKey("주간 회원권")) Membership.weekly(now) else null,
                    Membership.expiry(customer["member"] as? String)
                ).maxOrNull()
                if (items.containsKey("월간 회원권") || items.containsKey("주간 회원권")) {
                    customer["member"] = Membership.encode(requireNotNull(expiry))
                }
                users[action.name] = customer
                addHistory(request.id, request.date, action.name, "결제", total, items, action.operator)
            }
            is LedgerAction.Deposit -> {
                require(action.money > 0 && action.name.isNotBlank()) { "입금 정보가 올바르지 않습니다." }
                val normalized = action.name.trim().lowercase()
                val matches = users.keys.filter { name ->
                    name == normalized || (record(users[name])["extraName"] as? String)?.trim()?.lowercase() == normalized
                }
                if (matches.size == 1) {
                    val name = matches.single()
                    val customer = user(name)
                    customer["money"] = Math.addExact(amount(customer["money"]), action.money)
                    users[name] = customer
                    addHistory(request.id, request.date, name, "입금", action.money, etc = action.source)
                } else {
                    // Ambiguous aliases are left for the administrator to assign explicitly.
                    deposits[request.id] = mapOf("date" to request.date, "name" to normalized,
                        "money" to action.money, "source" to action.source)
                }
            }
            is LedgerAction.AssignDeposits -> {
                require(action.keys.isNotEmpty() && action.keys.distinct().size == action.keys.size) { "입금 내역을 선택해 주세요." }
                val customer = user(action.name)
                var balance = amount(customer["money"])
                action.keys.forEachIndexed { index, key ->
                    require(deposits.containsKey(key)) { "이미 처리되거나 삭제된 입금입니다. 새로고침해 주세요." }
                    val deposit = record(deposits[key])
                    val money = amount(deposit["money"])
                    require(money > 0) { "입금 금액을 확인해 주세요." }
                    balance = Math.addExact(balance, money)
                    val date = (deposit["date"] as? Number)?.toLong()
                        ?: (deposit["date"] as? String)?.toLongOrNull()
                        ?: error("입금 날짜를 확인해 주세요.")
                    addHistory("${request.id}_$index", date, action.name, "입금", money,
                        etc = deposit["name"] as? String ?: "")
                    deposits.remove(key)
                }
                customer["money"] = balance
                users[action.name] = customer
            }
            is LedgerAction.SetBalance -> {
                val customer = user(action.name)
                val before = amount(customer["money"])
                require(before == action.expected) { "잔액이 변경되었습니다. 새로고침 후 다시 수정해 주세요." }
                customer["money"] = action.money
                users[action.name] = customer
                addHistory(request.id, request.date, action.name, "조정", Math.subtractExact(action.money, before),
                    etc = "관리자 잔액 수정: $before → ${action.money}")
            }
        }
        root["users"] = users
        root["deposits"] = deposits
        root["history"] = history
        root["processedOperations"] = record(root["processedOperations"]) + (request.id to request.date)
        return root
    }

    companion object {
        fun validKey(value: String): Boolean = value.isNotBlank() && value.none { it in ".#$[]/" || it.code < 32 || it.code == 127 }
        fun amount(value: Any?): Int {
            require(value is Number) { "금액 데이터가 올바르지 않습니다." }
            val long = value.toLong()
            require(long in Int.MIN_VALUE..Int.MAX_VALUE && value.toDouble() == long.toDouble()) { "금액 범위를 초과했습니다." }
            return long.toInt()
        }
        @Suppress("UNCHECKED_CAST")
        fun record(value: Any?): Map<String, Any?> {
            if (value == null) return emptyMap()
            require(value is Map<*, *> && value.keys.all { it is String }) { "데이터 형식을 확인해 주세요." }
            return value as Map<String, Any?>
        }
    }
}
