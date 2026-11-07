package ggack.agitpos.data.firebase

import android.content.Context
import ggack.agitpos.domain.LedgerAction
import ggack.agitpos.domain.LedgerRequest
import org.json.JSONObject

/** Pending parsed notifications survive service/process restarts and remain account-scoped. */
class DepositInbox(context: Context) {
    private val preferences = context.getSharedPreferences("pending_deposits", Context.MODE_PRIVATE)
    data class Entry(val storageKey: String, val request: LedgerRequest)

    fun enqueue(owner: String, request: LedgerRequest): Boolean {
        val action = request.action as LedgerAction.Deposit
        val value = JSONObject().put("owner", owner).put("id", request.id).put("date", request.date)
            .put("name", action.name).put("money", action.money).put("source", action.source)
        return preferences.edit().putString("$owner/${request.id}", value.toString()).commit()
    }

    fun next(owner: String): Entry? = preferences.all.mapNotNull { (key, raw) ->
        runCatching {
            val value = JSONObject(raw as String)
            if (value.getString("owner") != owner) null else Entry(key,
                LedgerRequest(value.getString("id"), value.getLong("date"), LedgerAction.Deposit(
                    value.getString("name"), value.getInt("money"), value.getString("source"))))
        }.getOrNull()
    }.minByOrNull { it.request.date }

    fun remove(entry: Entry) { preferences.edit().remove(entry.storageKey).commit() }
}
