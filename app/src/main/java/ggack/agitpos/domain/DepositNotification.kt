package ggack.agitpos.domain

import java.security.MessageDigest

object DepositNotification {
    fun parse(packageName: String, key: String, eventTime: Long, body: String,
              bankAccount: String): LedgerRequest? {
        val lines = body.trim().lines().map { it.trim() }
        val parsed = when (packageName) {
            "com.kakaopay.app" -> {
                if (lines.size != 2 || lines[1].isBlank()) return null
                val match = Regex("^.+님이\\s+([0-9][0-9,]*)원을\\s+보냈어요\\.?$").matchEntire(lines[0]) ?: return null
                Triple(lines[1], match.groupValues[1], "카카오페이")
            }
            "com.samsung.android.messaging" -> {
                if (bankAccount.length != 4 || !body.contains("[카카오뱅크]") ||
                    !body.contains("($bankAccount)") || lines.size < 3) return null
                val match = Regex("^입금\\s+([0-9][0-9,]*)원$").matchEntire(lines[lines.lastIndex - 2]) ?: return null
                Triple(lines[lines.lastIndex - 1], match.groupValues[1], "카뱅")
            }
            else -> return null
        }
        val money = parsed.second.replace(",", "").toIntOrNull()?.takeIf { it > 0 } ?: return null
        if (parsed.first.isBlank() || eventTime <= 0) return null
        val canonicalBody = lines.joinToString("\n").replace(Regex("[ \\t]+"), " ")
        val identity = "$packageName\u0000$key\u0000$eventTime\u0000$canonicalBody"
        val id = MessageDigest.getInstance("SHA-256").digest(identity.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        return LedgerRequest("deposit_$id", eventTime,
            LedgerAction.Deposit(parsed.first.trim().lowercase(), money, parsed.third))
    }
}
