package com.example.smartexpensetracker.data.local.entity

data class TransactionEntity(
    val id: Long = 0,
    val amount: Double,
    val isIncome: Boolean = false,
    val merchant: String,
    val category: String,
    val timestamp: Long = System.currentTimeMillis(),
    val paymentMethod: String = "UPI", // UPI, Card, NetBanking, Cash, Other
    val refId: String? = null,
    val bankName: String? = null,
    val accountNumber: String? = null,
    val accountBalance: Double? = null,
    val source: String = "Notification", // Notification, SMS, Manual
    val note: String = "",
    val isDuplicate: Boolean = false,
    val duplicateOfId: Long? = null,
    val rawText: String = ""
)
