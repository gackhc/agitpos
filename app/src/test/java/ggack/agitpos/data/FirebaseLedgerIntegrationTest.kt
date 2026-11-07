package ggack.agitpos.data

import android.app.Application
import android.content.pm.ServiceInfo
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.database.FirebaseDatabase
import ggack.agitpos.data.firebase.FirebaseComm
import ggack.agitpos.data.model.networkmodel.ItemModel
import ggack.agitpos.domain.LedgerAction
import ggack.agitpos.domain.LedgerRequest
import org.junit.After
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import android.os.Looper
import java.util.UUID

/** Opt-in tests. Every database client is pinned to a loopback demo emulator. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28])
@LooperMode(LooperMode.Mode.PAUSED)
class FirebaseLedgerIntegrationTest {
    private val apps = mutableListOf<FirebaseApp>()
    private val databases = mutableListOf<FirebaseDatabase>()
    private val context get() = ApplicationProvider.getApplicationContext<Application>()
    @Before fun requireEmulator() {
        assumeTrue(java.lang.Boolean.getBoolean("agitpos.emulator"))
        // Register only Database. Do not load the production manifest's analytics,
        // auth or crash-reporting providers into the test application.
        val service = ServiceInfo().apply {
            name = "com.google.firebase.components.ComponentDiscoveryService"
            packageName = context.packageName
            applicationInfo = context.applicationInfo
            metaData = Bundle().apply {
                putString("com.google.firebase.components:com.google.firebase.database.DatabaseRegistrar",
                    "com.google.firebase.components.ComponentRegistrar")
            }
        }
        shadowOf(context.packageManager).addOrUpdateService(service)
    }
    @After fun cleanup() {
        databases.forEach { it.goOffline() }
        apps.forEach { it.delete() }
    }
    private fun database(): FirebaseDatabase {
        val options = FirebaseOptions.Builder().setProjectId("demo-agitpos-review")
            .setApplicationId("1:123:android:test").setApiKey("fake-emulator-key")
            .setDatabaseUrl("https://demo-agitpos-review-default-rtdb.firebaseio.com").build()
        val app = FirebaseApp.initializeApp(context, options, UUID.randomUUID().toString())
        apps.add(app)
        return FirebaseDatabase.getInstance(app).also {
            it.useEmulator("127.0.0.1", 9009)
            databases.add(it)
        }
    }
    private fun waitUntil(condition: () -> Boolean) {
        val limit = System.nanoTime() + 20_000_000_000
        while (!condition() && System.nanoTime() < limit) {
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(10)
        }
        shadowOf(Looper.getMainLooper()).idle()
        assertTrue("Timed out waiting for local emulator", condition())
    }
    private fun <T> await(task: Task<T>): T {
        waitUntil { task.isComplete }
        if (!task.isSuccessful) throw AssertionError(task.exception)
        return task.result
    }
    private fun repository(db: FirebaseDatabase, owner: String): FirebaseComm = FirebaseComm().also {
        it.init(db)
        it.setID(context, owner)
        if (owner != "denied") waitUntil { it.liveUserList.value?.containsKey("alice") == true }
    }
    private fun seed(db: FirebaseDatabase, owner: String) {
        await(db.reference.child(owner).setValue(mapOf("validate" to "20991231",
            "users" to mapOf("alice" to mapOf("pin" to "1234", "money" to 10000)))))
    }
    @Test fun twoClientsCommitConcurrentDepositAndPaymentAndReplayOnlyOnce() {
        val owner = "case_${UUID.randomUUID()}"
        val dbA = database()
        val dbB = database()
        seed(dbA, owner)
        val a = repository(dbA, owner)
        val b = repository(dbB, owner)
        val results = mutableListOf<Result<Unit>>()
        val deposit = LedgerRequest("deposit", 1794020400000L, LedgerAction.Deposit("alice", 5000, "test"))
        val payment = LedgerRequest("payment", 1794020400000L, LedgerAction.Payment("alice", listOf(ItemModel("drink", 3000, 1))))
        a.submit(deposit) { results.add(it) }
        b.submit(payment) { results.add(it) }
        waitUntil { results.size == 2 }
        assertTrue(results.toString(), results.all { it.isSuccess })
        b.submit(deposit) { results.add(it) }
        waitUntil { results.size == 3 }
        assertTrue(results.last().isSuccess)
        val root = await(dbA.reference.child(owner).get())
        assertEquals(12000L, root.child("users/alice/money").value)
        assertEquals(2L, root.child("processedOperations").childrenCount)
        assertEquals(2, root.child("history").children.sumOf { it.childrenCount.toInt() })
    }
    @Test fun serverPermissionFailureIsNotReportedAsSuccess() {
        val db = database()
        val repo = repository(db, "denied")
        var result: Result<Unit>? = null
        repo.submit(LedgerRequest("denied_payment", 1000, LedgerAction.Payment("alice", listOf(ItemModel("drink", 3000, 1))))) { result = it }
        waitUntil { result != null }
        assertTrue(result!!.isFailure)
        assertFalse(await(db.reference.child("denied").get()).exists())
    }
    @Test fun invalidPaymentLeavesServerBalanceAndHistoryUntouched() {
        val owner = "case_${UUID.randomUUID()}"
        val db = database()
        seed(db, owner)
        val repo = repository(db, owner)
        var result: Result<Unit>? = null
        repo.submit(LedgerRequest("invalid_payment", 1000, LedgerAction.Payment("missing", listOf(ItemModel("drink", 3000, 1))))) { result = it }
        waitUntil { result != null }
        assertTrue(result!!.isFailure)
        val root = await(db.reference.child(owner).get())
        assertEquals(10000L, root.child("users/alice/money").value)
        assertFalse(root.child("history").exists())
        assertFalse(root.child("processedOperations").exists())
    }
}
