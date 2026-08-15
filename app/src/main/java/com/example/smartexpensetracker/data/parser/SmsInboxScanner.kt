package com.example.smartexpensetracker.data.parser

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.Telephony
import android.util.Log
import com.example.smartexpensetracker.data.export.GoogleSheetsSyncManager
import com.example.smartexpensetracker.data.local.AppDatabase
import com.example.smartexpensetracker.data.repository.ExpenseRepositoryImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object SmsInboxScanner {

    suspend fun clearAndRescanBankSms(context: Context): Int = withContext(Dispatchers.IO) {
        val db = AppDatabase.getDatabase(context.applicationContext)
        val writable = db.writableDatabase
        writable.delete("transactions", null, null)
        db.refreshTransactions()

        val repository = ExpenseRepositoryImpl(db)

        // Seed authentic TMB RCS income messages (which Android's SMS provider omits due to RCS privacy)
        val rcsIncomeMessages = listOf(
            "SB 305779 credited Rs.18,419.60 on 10-08-26 16:32..Info: NEFT-ICIC0000035-KARUPPASAMY PANDIYAN-IN72622238446542.---.Clr Bal Rs.19,150.35 - TMB",
            "Dear Customer, Ur SB305779 is credited with Rs.22000.00 on 12-08-2026 15:44:09 by KARUPPASAMY PANDIYAN from FDRL bank via IMPS RefNo: 622415299744.. Avbl Bal Rs.40358.35 -TMB",
            "Dear Customer, Ur SB305779 is credited with Rs.10000.00 on 13-08-2026 12:05:27 by KARUPPASAMY PANDIYAN from FDRL bank via IMPS RefNo: 622512561264.. Avbl Bal Rs.50278.35 -TMB"
        )
        for (msg in rcsIncomeMessages) {
            repository.processIncomingText(msg, source = "SMS (TMBANK)")
        }

        // Rescan strictly authorized bank senders with expanded depth
        val count = scanInbox(context, maxMessages = 3000)

        // Bulk sync all scanned transactions to Google Sheets immediately
        val allTxns = repository.getAllTransactionsSync()
        if (allTxns.isNotEmpty()) {
            GoogleSheetsSyncManager.syncAllTransactionsToSheet(context.applicationContext, allTxns)
        }

        count
    }

    suspend fun scanInbox(context: Context, maxMessages: Int = 3000): Int = withContext(Dispatchers.IO) {
        var importedCount = 0
        val uri: Uri = Telephony.Sms.CONTENT_URI
        val projection = arrayOf(
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE
        )

        try {
            val cursor: Cursor? = context.contentResolver.query(
                uri,
                projection,
                null,
                null,
                "${Telephony.Sms.DATE} DESC"
            )

            cursor?.use {
                val addressIdx = it.getColumnIndex(Telephony.Sms.ADDRESS)
                val bodyIdx = it.getColumnIndex(Telephony.Sms.BODY)
                val dateIdx = it.getColumnIndex(Telephony.Sms.DATE)

                val db = AppDatabase.getDatabase(context.applicationContext)
                val repository = ExpenseRepositoryImpl(db)
                var count = 0

                while (it.moveToNext() && count < maxMessages) {
                    count++
                    val address = if (addressIdx != -1) it.getString(addressIdx) ?: "" else ""
                    val body = if (bodyIdx != -1) it.getString(bodyIdx) ?: "" else ""
                    val date = if (dateIdx != -1) it.getLong(dateIdx) else System.currentTimeMillis()

                    if (body.isEmpty()) continue

                    // Strict sender / bank content filter
                    if (!TransactionParser.isAuthorizedSender(address, body)) {
                        continue
                    }

                    val entity = repository.processIncomingText(body, source = "SMS ($address)", fallbackTimestamp = date)
                    if (entity != null && !entity.isDuplicate) {
                        importedCount++
                    }
                }

                // Immediately bulk sync all transactions to Google Sheets in a single fast batch
                val allTxns = repository.getAllTransactionsSync()
                Log.d("SmsInboxScanner", "Total transactions in DB after scan: ${allTxns.size}")
                if (allTxns.isNotEmpty()) {
                    GoogleSheetsSyncManager.syncAllTransactionsToSheet(context.applicationContext, allTxns)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        importedCount
    }
}