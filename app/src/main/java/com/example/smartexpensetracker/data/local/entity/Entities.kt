package com.example.smartexpensetracker.data.local.entity

data class CategoryEntity(
    val name: String,
    val emoji: String,
    val isDefault: Boolean = true
)

data class BudgetEntity(
    val monthKey: String, // Format: YYYY-MM e.g. "2026-08"
    val totalBudget: Double,
    val currencySymbol: String = "\u20B9",
    val warn75Sent: Boolean = false,
    val warn90Sent: Boolean = false,
    val warn100Sent: Boolean = false
)

data class CategoryBudgetEntity(
    val id: Long = 0,
    val monthKey: String, // YYYY-MM
    val categoryName: String,
    val allocatedAmount: Double,
    val warn75Sent: Boolean = false,
    val warn90Sent: Boolean = false,
    val warn100Sent: Boolean = false
)

data class MerchantRuleEntity(
    val merchantPattern: String, // Uppercase merchant key e.g. SWIGGY, UBER
    val categoryName: String,
    val userCreated: Boolean = true
)

data class RecurringExpenseEntity(
    val id: Long = 0,
    val title: String, // e.g. Netflix, Hostel Rent
    val amount: Double,
    val categoryName: String,
    val frequency: String = "Monthly", // Monthly, Weekly, Yearly
    val dayOfMonth: Int = 1, // Day due
    val paymentMethod: String = "UPI",
    val isAutoDeducted: Boolean = true,
    val isActive: Boolean = true
)

data class MonthlySummaryEntity(
    val monthKey: String, // YYYY-MM
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
