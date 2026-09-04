package com.example.smartexpensetracker.data.local.entity

data class CategoryEntity(
    val name: String,
    val emoji: String,
    val isDefault: Boolean = true
) {
    val cleanEmoji: String
        get() = if (emoji.contains("?") || emoji.contains("?") || emoji.contains("?") || emoji.isBlank() || emoji.length > 4) {
            getCategoryEmoji(name)
        } else emoji
}

fun getCategoryEmoji(category: String): String {
    return when (category.lowercase().trim()) {
        "friends" -> "\uD83D\uDC65"
        "groceries" -> "\uD83D\uDED2"
        "food", "dining" -> "\uD83C\uDF54"
        "recharge & bills", "recharge", "bills" -> "\uD83D\uDCF1"
        "shopping" -> "\uD83D\uDECD\uFE0F"
        "transport" -> "\uD83D\uDE97"
        "entertainment" -> "\uD83C\uDFAC"
        "gaming" -> "\uD83C\uDFAE"
        "salary" -> "\uD83D\uDCB0"
        "income", "cashback", "refund", "investment" -> "\uD83D\uDCB5"
        "education" -> "\uD83C\uDF93"
        "subscriptions" -> "\uD83D\uDD01"
        "travel" -> "\u2708\uFE0F"
        "medical" -> "\uD83D\uDC8A"
        "technology" -> "\uD83D\uDCBB"
        "home" -> "\uD83C\uDFE0"
        else -> "\uD83C\uDFF7\uFE0F"
    }
}

data class BudgetEntity(
    val monthKey: String,
    val totalBudget: Double,
    val currencySymbol: String = "\u20B9",
    val warn75Sent: Boolean = false,
    val warn90Sent: Boolean = false,
    val warn100Sent: Boolean = false
)

data class CategoryBudgetEntity(
    val id: Long = 0,
    val monthKey: String,
    val categoryName: String,
    val allocatedAmount: Double,
    val warn75Sent: Boolean = false,
    val warn90Sent: Boolean = false,
    val warn100Sent: Boolean = false
)

data class MerchantRuleEntity(
    val merchantPattern: String,
    val categoryName: String,
    val userCreated: Boolean = true
)

data class RecurringExpenseEntity(
    val id: Long = 0,
    val title: String,
    val amount: Double,
    val categoryName: String,
    val frequency: String = "Monthly",
    val dayOfMonth: Int = 1,
    val paymentMethod: String = "UPI",
    val isAutoDeducted: Boolean = true,
    val isActive: Boolean = true
)

data class MonthlySummaryEntity(
    val monthKey: String,
    val totalIncome: Double,
    val totalExpense: Double,
    val totalSavings: Double,
    val highestCategory: String,
    val highestCategoryAmount: Double,
    val highestTransactionMerchant: String,
    val highestTransactionAmount: Double,
    val averageDailySpending: Double,
    val budgetUsagePercentage: Double,
    val generatedAt: Long = System.currentTimeMillis()
)
