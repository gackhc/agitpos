package ggack.agitpos.domain

import ggack.agitpos.data.login.UserSession
import ggack.agitpos.data.model.networkmodel.ItemModel
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDateTime

class CompatibilityTest {
    private val row = mapOf("name" to "음료", "price" to 1500L, "cnt" to 1L)
    @Test fun sparseFirebaseMapPreservesNumericKeysAndUnitPrice() {
        val result = ItemCatalog.decode(hashMapOf("12" to row, "3" to row))
        assertEquals(listOf(3, 12), result.map { it.idx })
        assertEquals(listOf(1500, 1500), result.map { it.price })
    }
    @Test fun firebaseArraySkipsHolesWithoutRenumbering() {
        assertEquals(listOf(1, 3), ItemCatalog.decode(listOf(null, row, null, row)).map { it.idx })
        assertTrue(ItemCatalog.decode(null).isEmpty())
    }
    @Test fun firstItemCanMoveDownAndAddButtonCannotMove() {
        val list = mutableListOf(ItemModel("first", 1, 1, 3), ItemModel("second", 2, 1, 12), ItemModel("plusitem", 0, 0, 99999, -1))
        assertTrue(ItemCatalog.move(list, 0, 1))
        assertEquals(listOf("second", "first", "plusitem"), list.map { it.name })
        assertEquals(listOf(1, 2, 99999), list.map { it.idx })
        assertFalse(ItemCatalog.move(list, 0, 2))
        assertFalse(ItemCatalog.move(list, 2, 0))
        assertFalse(ItemCatalog.move(list, -1, 0))
    }
    @Test fun legacyMembershipAndInvalidValuesAreHandled() {
        assertEquals(LocalDateTime.of(2024, 3, 1, 0, 0), Membership.expiry("202402"))
        assertEquals(LocalDateTime.of(2026, 11, 8, 0, 0), Membership.expiry("20261107"))
        assertEquals(LocalDateTime.of(2026, 12, 1, 8, 0), Membership.expiry("20261201080000"))
        assertNull(Membership.expiry("20260230"))
        assertNull(Membership.expiry(""))
        assertNull(Membership.expiry(null))
    }
    @Test fun expiryBoundariesHaveNoStraySeconds() {
        val sunday = LocalDateTime.of(2026, 11, 8, 23, 59, 59)
        assertEquals(LocalDateTime.of(2026, 11, 9, 8, 0), Membership.weekly(sunday))
        assertEquals(LocalDateTime.of(2026, 12, 1, 8, 0), Membership.monthly(sunday))
    }
    @Test fun sessionRequiresMatchingPinLoginAndDoesNotRestoreMaster() {
        val session = UserSession()
        assertFalse(session.permits("*master", true))
        session.signIn("alice")
        assertTrue(session.permits("alice"))
        assertFalse(session.permits("*master", true))
        session.signIn("*master")
        assertTrue(session.permits("*master", true))
        session.clear()
        assertFalse(session.permits("*master", true))
    }
    @Test fun notificationReplayKeepsIdButSeparateTransfersHaveDistinctIds() {
        val body = "김*혜님이  5,000원을 보냈어요.\nalice"
        val first = DepositNotification.parse("com.kakaopay.app", "key", 1000, body, "")!!
        val replay = DepositNotification.parse("com.kakaopay.app", "key", 1000, body.replace("  ", " "), "")!!
        val another = DepositNotification.parse("com.kakaopay.app", "key", 2000, body, "")!!
        assertEquals(first.id, replay.id)
        assertNotEquals(first.id, another.id)
        assertEquals(5000, (first.action as LedgerAction.Deposit).money)
    }
    @Test fun malformedOrUntrustedNotificationsAreIgnored() {
        assertNull(DepositNotification.parse("fake.kakaopay", "k", 1, "김님이 100원을 보냈어요.\nalice", ""))
        assertNull(DepositNotification.parse("com.kakaopay.app", "k", 1, "김님이 100원을 보냈어요.", ""))
        assertNull(DepositNotification.parse("com.kakaopay.app", "k", 1, "김님이 999999999999원을 보냈어요.\nalice", ""))
        assertNull(DepositNotification.parse("com.samsung.android.messaging", "k", 1, "[카카오뱅크] 입금", "1234"))
    }
    @Test fun bankNotificationRequiresConfiguredAccount() {
        val body = "[카카오뱅크]\n(1234)\n입금 5,000원\nalice\n잔액 10,000원"
        assertNotNull(DepositNotification.parse("com.samsung.android.messaging", "k", 1, body, "1234"))
        assertNull(DepositNotification.parse("com.samsung.android.messaging", "k", 1, body, "5678"))
        assertNull(DepositNotification.parse("com.samsung.android.messaging", "k", 1, body, ""))
    }
}
