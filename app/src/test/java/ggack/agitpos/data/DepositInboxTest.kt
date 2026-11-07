package ggack.agitpos.data

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import ggack.agitpos.data.firebase.DepositInbox
import ggack.agitpos.domain.LedgerAction
import ggack.agitpos.domain.LedgerRequest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28])
class DepositInboxTest {
    @Test fun pendingDepositSurvivesRecreationAndStaysWithItsAccount() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val request = LedgerRequest("deposit_test", 1000, LedgerAction.Deposit("alice", 5000, "test"))
        assertTrue(DepositInbox(context).enqueue("owner-a", request))
        val restarted = DepositInbox(context)
        assertNull(restarted.next("owner-b"))
        val entry = restarted.next("owner-a")!!
        assertEquals(request, entry.request)
        assertEquals(request, DepositInbox(context).next("owner-a")!!.request)
        restarted.remove(entry)
        assertNull(DepositInbox(context).next("owner-a"))
    }
    @Test fun replayDoesNotDuplicatePendingEntryAndOwnersDoNotOverwriteEachOther() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val inbox = DepositInbox(context)
        val request = LedgerRequest("same_id", 1000, LedgerAction.Deposit("alice", 5000, "test"))
        inbox.enqueue("a", request)
        inbox.enqueue("a", request)
        inbox.enqueue("b", request)
        inbox.remove(inbox.next("a")!!)
        assertNull(inbox.next("a"))
        assertNotNull(inbox.next("b"))
    }
}
