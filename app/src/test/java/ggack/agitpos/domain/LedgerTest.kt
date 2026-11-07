package ggack.agitpos.domain

import ggack.agitpos.data.model.networkmodel.ItemModel
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class LedgerTest {
    private val zone = ZoneId.of("Asia/Seoul")
    private val ledger = Ledger(zone)
    private val date = LocalDateTime.of(2026, 11, 7, 12, 34, 56).atZone(zone).toInstant().toEpochMilli()
    private fun initial(money: Int = 10000, member: String = "", supervisor: Boolean = false,
                        bonus: Long = 2022120100L): Map<String, Any?> = mapOf(
        "users" to mapOf("alice" to mapOf("money" to money, "pin" to "1234", "member" to member,
            "isSupervisor" to supervisor, "svBonus" to bonus)), "version" to "preserve-me")
    private fun payment(id: String = "pay", items: List<ItemModel> = listOf(ItemModel("음료", 1500, 2))) =
        LedgerRequest(id, date, LedgerAction.Payment("alice", items))
    private fun deposit(id: String = "deposit", name: String = "alice", money: Int = 5000) =
        LedgerRequest(id, date, LedgerAction.Deposit(name, money, "카카오페이"))
    private fun user(root: Map<String, Any?>) = Ledger.record(Ledger.record(root["users"])["alice"])
    private fun history(root: Map<String, Any?>) = Ledger.record(Ledger.record(root["history"])["202611"])
    private fun reject(block: () -> Unit) {
        try { block(); fail("Expected rejection") } catch (_: IllegalArgumentException) { }
    }

    @Test fun concurrentPaymentAndDepositUseLatestSnapshotInEitherOrder() {
        val start = initial()
        val paymentFirst = ledger.apply(ledger.apply(start, payment()), deposit())
        val depositFirst = ledger.apply(ledger.apply(start, deposit()), payment())
        assertEquals(12000, user(paymentFirst)["money"])
        assertEquals(12000, user(depositFirst)["money"])
        assertEquals(2, history(paymentFirst).size)
        assertEquals(10000, user(start)["money"])
        assertEquals("preserve-me", paymentFirst["version"])
    }
    @Test fun replayedDepositAndPaymentAreIdempotent() {
        var result = ledger.apply(initial(), deposit())
        result = ledger.apply(result, deposit())
        result = ledger.apply(result, payment())
        result = ledger.apply(result, payment())
        assertEquals(12000, user(result)["money"])
        assertEquals(2, history(result).size)
    }
    @Test fun distinctSameMillisecondDepositsKeepBothHistories() {
        val result = ledger.apply(ledger.apply(initial(), deposit("a")), deposit("b"))
        assertEquals(20000, user(result)["money"])
        assertEquals(2, history(result).size)
    }
    @Test fun unknownDepositCanBeAssignedExactlyOnce() {
        val pending = ledger.apply(initial(), deposit(name = "unknown"))
        assertEquals(10000, user(pending)["money"])
        assertEquals(1, Ledger.record(pending["deposits"]).size)
        val assign = LedgerRequest("assign", date, LedgerAction.AssignDeposits("alice", listOf("deposit")))
        val assigned = ledger.apply(pending, assign)
        assertEquals(15000, user(assigned)["money"])
        assertTrue(Ledger.record(assigned["deposits"]).isEmpty())
        assertEquals(assigned, ledger.apply(assigned, assign))
        reject { ledger.apply(assigned, assign.copy(id = "another_assignment")) }
    }
    @Test fun pendingBatchRejectsPartialConsumptionWithoutChangingInput() {
        val pending = ledger.apply(initial(), deposit(name = "unknown"))
        reject { ledger.apply(pending, LedgerRequest("assign", date,
            LedgerAction.AssignDeposits("alice", listOf("deposit", "missing")))) }
        assertEquals(10000, user(pending)["money"])
        assertEquals(1, Ledger.record(pending["deposits"]).size)
        assertTrue(history(pending).isEmpty())
    }
    @Test fun ambiguousAliasesStayPending() {
        val start = initial().toMutableMap()
        start["users"] = mapOf("alice" to mapOf("money" to 10000, "extraName" to "sender"),
            "bob" to mapOf("money" to 0, "extraName" to "sender"))
        val result = ledger.apply(start, deposit(name = "sender"))
        assertEquals(10000, user(result)["money"])
        assertEquals(1, Ledger.record(result["deposits"]).size)
    }
    @Test fun masterAndClientMembershipUseSameExpiryAndCharge() {
        val items = listOf(ItemModel("월간 회원권", 40000, 1))
        val client = ledger.apply(initial(50000), payment(items = items))
        val master = ledger.apply(initial(50000), LedgerRequest("master", date, LedgerAction.Payment("alice", items, "마스터")))
        assertEquals("20261201080000", user(client)["member"])
        assertEquals(user(client), user(master))
        assertEquals(10000, user(master)["money"])
    }
    @Test fun weeklyPurchaseDoesNotShortenExistingMembership() {
        val result = ledger.apply(initial(member = "20261201080000"), payment(items = listOf(ItemModel("주간 회원권", 10000, 1))))
        assertEquals("20261201080000", user(result)["member"])
    }
    @Test fun attendanceGrantsOncePerDayAndCapsAtEightPerMonth() {
        val items = listOf(ItemModel("운영진 출첵 8/8", 0, 1))
        val result = ledger.apply(initial(supervisor = true, bonus = 2026110607), payment(items = items))
        assertEquals(16500, user(result)["money"])
        assertEquals(2026110708L, user(result)["svBonus"])
        reject { ledger.apply(result, payment("second", items)) }
        reject { ledger.apply(initial(supervisor = true, bonus = 2026110608), payment(items = items)) }
    }
    @Test fun attendanceResetsAcrossYearsAndRejectsNonSupervisor() {
        val items = listOf(ItemModel("운영진 출첵 1/8", 0, 1))
        val result = ledger.apply(initial(supervisor = true, bonus = 2025110708), payment(items = items))
        assertEquals(2026110701L, user(result)["svBonus"])
        reject { ledger.apply(initial(), payment(items = items)) }
    }
    @Test fun staleAdminBalanceEditIsRejected() {
        val changed = ledger.apply(initial(), deposit())
        reject { ledger.apply(changed, LedgerRequest("edit", date, LedgerAction.SetBalance("alice", 10000, 12000))) }
        val result = ledger.apply(changed, LedgerRequest("edit", date, LedgerAction.SetBalance("alice", 15000, 12000)))
        assertEquals(12000, user(result)["money"])
        assertEquals("조정", Ledger.record(history(result)["edit"])["kind"])
    }
    @Test fun negativeSettlementPricesRemainSupported() {
        val result = ledger.apply(initial(), payment(items = listOf(ItemModel("정산", -1000, 1))))
        assertEquals(11000, user(result)["money"])
    }
    @Test fun invalidCartDoesNotModifySnapshot() {
        val start = initial()
        reject { ledger.apply(start, payment(items = emptyList())) }
        reject { ledger.apply(start, payment(items = listOf(ItemModel("x", 1, 0)))) }
        reject { ledger.apply(start, payment(items = listOf(ItemModel("x", 1, 1), ItemModel("x", 2, 1)))) }
        reject { ledger.apply(start, deposit(money = -1)) }
        assertEquals(10000, user(start)["money"])
        assertFalse(start.containsKey("history"))
    }
    @Test(expected = ArithmeticException::class) fun overflowingBalanceIsRejected() {
        ledger.apply(initial(Int.MAX_VALUE), deposit())
    }
    @Test fun missingUserDoesNotCreatePaymentHistory() {
        val start = emptyMap<String, Any?>()
        reject { ledger.apply(start, payment()) }
        assertTrue(start.isEmpty())
    }
}
