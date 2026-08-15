package com.example.smartexpensetracker.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat

object BudgetAlertManager {

    private const val CHANNEL_ID = "expense_alerts_channel"
    private const val CHANNEL_NAME = "SpendWise Alerts"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Alerts for transaction detection and budget thresholds"
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun sendTransactionNotification(
        context: Context,
        amount: Double,
        category: String,
        merchant: String,
        isIncome: Boolean = false
    ) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val title = if (isIncome) "💰 Money Received" else "💳 Expense Recorded"
        val text = if (isIncome) {
            "\u20B9${amount.toInt()} received from $merchant ($category)"
        } else {
            "\u20B9${amount.toInt()} spent on $category at $merchant"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_agenda)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        manager.notify((System.currentTimeMillis() % 10000).toInt(), notification)
    }

    fun sendBudgetWarningNotification(context: Context, categoryOrTotal: String, percentage: Int) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("⚠️ Budget Alert")
            .setContentText("You have used $percentage% of your $categoryOrTotal budget.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        manager.notify((System.currentTimeMillis() % 10000 + 10000).toInt(), notification)
    }
}
