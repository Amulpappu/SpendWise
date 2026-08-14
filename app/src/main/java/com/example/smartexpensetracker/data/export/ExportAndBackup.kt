package com.example.smartexpensetracker.data.export

import android.content.Context
import com.example.smartexpensetracker.data.local.entity.TransactionEntity
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object DataExporter {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    fun exportToCsv(transactions: List<TransactionEntity>): String {
        val sb = java.lang.StringBuilder()
        sb.append("ID,Date,Merchant,Amount,Type,Category,PaymentMethod,RefID,Source,Note\n")
        for (t in transactions) {
            val dateStr = dateFormat.format(Date(t.timestamp))
            val typeStr = if (t.isIncome) "Income" else "Expense"
            val merchantEsc = escapeCsv(t.merchant)
            val noteEsc = escapeCsv(t.note)
            val refEsc = escapeCsv(t.refId ?: "")
            sb.append("${t.id},\"$dateStr\",\"$merchantEsc\",${t.amount},\"$typeStr\",\"${t.category}\",\"${t.paymentMethod}\",\"$refEsc\",\"${t.source}\",\"$noteEsc\"\n")
        }
        return sb.toString()
    }

    fun exportToJson(transactions: List<TransactionEntity>): String {
        val gson: Gson = GsonBuilder().setPrettyPrinting().create()
        return gson.toJson(transactions)
    }

    private fun escapeCsv(str: String): String {
        return str.replace("\"", "\"\"")
    }
}

object BackupManager {

    data class BackupData(
        val version: Int = 1,
        val timestamp: Long = System.currentTimeMillis(),
        val transactions: List<TransactionEntity>
    )

    fun createBackupJson(transactions: List<TransactionEntity>): String {
        val backup = BackupData(transactions = transactions)
        return GsonBuilder().setPrettyPrinting().create().toJson(backup)
    }

    fun restoreBackupJson(jsonString: String): List<TransactionEntity>? {
        return try {
            val backup = Gson().fromJson(jsonString, BackupData::class.java)
            backup?.transactions
        } catch (e: Exception) {
            null
        }
    }
}
