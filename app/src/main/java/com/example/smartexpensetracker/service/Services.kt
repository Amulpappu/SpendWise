package com.example.smartexpensetracker.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.telephony.SmsMessage
import com.example.smartexpensetracker.data.export.GoogleSheetsSyncManager
import com.example.smartexpensetracker.data.local.AppDatabase
import com.example.smartexpensetracker.data.repository.ExpenseRepositoryImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TransactionNotificationListener : NotificationListenerService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private lateinit var repository: ExpenseRepositoryImpl

    override fun onCreate() {
        super.onCreate()
        val db = AppDatabase.getDatabase(applicationContext)
        repository = ExpenseRepositoryImpl(db)
        BudgetAlertManager.createNotificationChannel(applicationContext)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        val sbnNonNull = sbn ?: return

        val pkgName = sbnNonNull.packageName ?: ""
        // Do not process our own SpendWise notifications
        if (pkgName == applicationContext.packageName) return

        val extras = sbnNonNull.notification.extras
        val title = extras.getString("android.title") ?: ""
        val text = extras.getCharSequence("android.text")?.toString() ?: ""
        val bigText = extras.getCharSequence("android.bigText")?.toString() ?: ""

        val combinedText = "$title $text $bigText".trim()
        if (combinedText.isEmpty()) return

        serviceScope.launch {
            val entity = repository.processIncomingText(combinedText, source = "Notification ($pkgName)")
            if (entity != null && !entity.isDuplicate) {
                // Trigger local alert notification
                BudgetAlertManager.sendTransactionNotification(
                    context = applicationContext,
                    amount = entity.amount,
                    category = entity.category,
                    merchant = entity.merchant,
                    isIncome = entity.isIncome
                )
                // Auto-sync to Google Sheet if enabled
                if (GoogleSheetsSyncManager.isAutoSyncEnabled(applicationContext)) {
                    GoogleSheetsSyncManager.syncTransactionToSheet(applicationContext, entity)
                }
            }
        }
    }
}

class SmsReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        if (intent.action != "android.provider.Telephony.SMS_RECEIVED") return

        val bundle: Bundle? = intent.extras
        if (bundle != null) {
            try {
                val pdus = bundle.get("pdus") as? Array<*> ?: return
                val db = AppDatabase.getDatabase(context.applicationContext)
                val repository = ExpenseRepositoryImpl(db)

                for (pdu in pdus) {
                    val format = bundle.getString("format")
                    val sms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        SmsMessage.createFromPdu(pdu as ByteArray, format)
                    } else {
                        @Suppress("DEPRECATION")
                        SmsMessage.createFromPdu(pdu as ByteArray)
                    }
                    val body = sms.messageBody ?: continue
                    val sender = sms.originatingAddress ?: "SMS"

                    scope.launch {
                        val entity = repository.processIncomingText(body, source = "SMS ($sender)")
                        if (entity != null && !entity.isDuplicate) {
                            // Trigger local alert notification
                            BudgetAlertManager.sendTransactionNotification(
                                context = context.applicationContext,
                                amount = entity.amount,
                                category = entity.category,
                                merchant = entity.merchant,
                                isIncome = entity.isIncome
                            )
                            // Auto-sync to Google Sheet if enabled
                            if (GoogleSheetsSyncManager.isAutoSyncEnabled(context.applicationContext)) {
                                GoogleSheetsSyncManager.syncTransactionToSheet(context.applicationContext, entity)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
