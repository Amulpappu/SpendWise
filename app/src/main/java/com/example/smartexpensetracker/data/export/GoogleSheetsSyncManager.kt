package com.example.smartexpensetracker.data.export

import android.content.Context
import com.example.smartexpensetracker.data.local.entity.TransactionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

object GoogleSheetsSyncManager {

    const val DEFAULT_WEBHOOK_URL = "https://script.google.com/macros/s/AKfycbx27gXUzTE6lKEhP-a1UGhLJ5MwctducYTHKmExClNzf57ZM69sbVouwbBC7qwAD704Fw/exec"
    private const val PREFS_NAME = "google_sheets_prefs"
    private const val KEY_WEBHOOK_URL = "webhook_url"
    private const val KEY_AUTO_SYNC = "auto_sync_enabled"

    private val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    fun getWebhookUrl(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_WEBHOOK_URL, null)
        val oldUrls = listOf(
            "AKfycbyUbZ61_7UnASiyLV",
            "AKfycbxec14GhgurG284NGRX2k0yBK6dU9TdkhL0csLbkNlomClA9_2AGGVmOLBFE4HH0qHt",
            "AKfycbyvvndQsIPGPpsjSm3ilauC2aXpa0eW3lkyFHwBPmUibYeHYrkskJcJNE47NcWcELehog",
            "AKfycbxVXjX6oeYpWJoFh-wT6ENPbnITMvy0n00ckSxEV2stv68EfskZatjJNXHnWjMrqqogow"
        )
        return if (saved.isNullOrBlank() || oldUrls.any { saved.contains(it) }) DEFAULT_WEBHOOK_URL else saved
    }

    fun saveWebhookUrl(context: Context, url: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_WEBHOOK_URL, url.trim()).apply()
    }

    fun isAutoSyncEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_AUTO_SYNC, true) // Enabled by default
    }

    fun setAutoSyncEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_AUTO_SYNC, enabled).apply()
    }

    /**
     * Fast bulk batch synchronization: transmits all transactions with their exact original historical SMS dates
     */
    suspend fun syncAllTransactionsToSheet(context: Context, transactions: List<TransactionEntity>): Int = withContext(Dispatchers.IO) {
        val webhookUrl = getWebhookUrl(context)
        if (webhookUrl.isBlank() || transactions.isEmpty()) return@withContext 0

        val nonDuplicates = transactions.filter { !it.isDuplicate }.sortedBy { it.timestamp }
        if (nonDuplicates.isEmpty()) return@withContext 0

        val profile = com.example.smartexpensetracker.data.local.UserProfileManager.getUserProfile(context)

        try {
            val batchItemsJson = nonDuplicates.joinToString(",") { txn ->
                """
                {
                    "id": ${txn.id},
                    "timestamp": "${sdf.format(Date(txn.timestamp))}",
                    "merchant": "${txn.merchant.replace("\"", "\\\"").replace("\n", " ")}",
                    "amount": ${txn.amount},
                    "category": "${txn.category}",
                    "type": "${if (txn.isIncome) "Income" else "Expense"}",
                    "paymentMethod": "${txn.paymentMethod}",
                    "note": "${txn.note.replace("\"", "\\\"").replace("\n", " ")}"
                }
                """.trimIndent()
            }

            val payload = """
            {
                "user": {
                    "name": "${profile.userName.replace("\"", "\\\"")}",
                    "phone": "${profile.mobileNumber}",
                    "account": "${profile.accountNumber}",
                    "bank": "${profile.bankName.replace("\"", "\\\"")}"
                },
                "batch": [
                    $batchItemsJson
                ]
            }
            """.trimIndent()

            val success = sendPostRequest(webhookUrl, payload)
            if (success) nonDuplicates.size else 0
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback: sync individually if bulk fails
            var syncedCount = 0
            for (txn in nonDuplicates) {
                if (syncTransactionToSheet(context, txn)) syncedCount++
            }
            syncedCount
        }
    }

    suspend fun syncTransactionToSheet(context: Context, transaction: TransactionEntity): Boolean {
        val webhookUrl = getWebhookUrl(context)
        if (webhookUrl.isBlank()) return false

        return withContext(Dispatchers.IO) {
            val profile = com.example.smartexpensetracker.data.local.UserProfileManager.getUserProfile(context)
            val jsonPayload = """
                {
                    "user": {
                        "name": "${profile.userName.replace("\"", "\\\"")}",
                        "phone": "${profile.mobileNumber}",
                        "account": "${profile.accountNumber}",
                        "bank": "${profile.bankName.replace("\"", "\\\"")}"
                    },
                    "transaction": {
                        "id": ${transaction.id},
                        "timestamp": "${sdf.format(Date(transaction.timestamp))}",
                        "merchant": "${transaction.merchant.replace("\"", "\\\"").replace("\n", " ")}",
                        "amount": ${transaction.amount},
                        "category": "${transaction.category}",
                        "type": "${if (transaction.isIncome) "Income" else "Expense"}",
                        "paymentMethod": "${transaction.paymentMethod}",
                        "note": "${transaction.note.replace("\"", "\\\"").replace("\n", " ")}"
                    },
                    "id": ${transaction.id},
                    "timestamp": "${sdf.format(Date(transaction.timestamp))}",
                    "merchant": "${transaction.merchant.replace("\"", "\\\"").replace("\n", " ")}",
                    "amount": ${transaction.amount},
                    "category": "${transaction.category}",
                    "type": "${if (transaction.isIncome) "Income" else "Expense"}",
                    "paymentMethod": "${transaction.paymentMethod}",
                    "note": "${transaction.note.replace("\"", "\\\"").replace("\n", " ")}"
                }
            """.trimIndent()

            sendPostRequest(webhookUrl, jsonPayload)
        }
    }

    private fun sendPostRequest(webhookUrl: String, jsonPayload: String): Boolean {
        return try {
            val url = URL(webhookUrl.trim())
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json; utf-8")
            conn.setRequestProperty("User-Agent", "Mozilla/5.0")
            conn.doOutput = true
            conn.instanceFollowRedirects = false
            conn.connectTimeout = 15000
            conn.readTimeout = 15000

            OutputStreamWriter(conn.outputStream).use { writer ->
                writer.write(jsonPayload)
                writer.flush()
            }

            val code = conn.responseCode
            if (code in 200..299 || code == HttpURLConnection.HTTP_MOVED_TEMP || code == HttpURLConnection.HTTP_SEE_OTHER) {
                val location = conn.getHeaderField("Location")
                conn.disconnect()

                if (!location.isNullOrEmpty()) {
                    val getConn = URL(location).openConnection() as HttpURLConnection
                    getConn.requestMethod = "GET"
                    getConn.instanceFollowRedirects = true
                    getConn.connectTimeout = 10000
                    getConn.readTimeout = 10000
                    val getCode = getConn.responseCode
                    getConn.disconnect()
                    getCode in 200..299 || getCode == HttpURLConnection.HTTP_MOVED_TEMP
                } else {
                    true
                }
            } else {
                conn.disconnect()
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
