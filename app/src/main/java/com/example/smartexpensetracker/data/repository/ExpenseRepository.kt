package com.example.smartexpensetracker.data.repository

import com.example.smartexpensetracker.data.local.AppDatabase
import com.example.smartexpensetracker.data.local.entity.*
import com.example.smartexpensetracker.data.parser.Categorizer
import com.example.smartexpensetracker.data.parser.DuplicateDetector
import com.example.smartexpensetracker.data.parser.TransactionParser
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.*

interface ExpenseRepository {
    fun getAllTransactionsFlow(): Flow<List<TransactionEntity>>
    fun getDuplicateTransactionsFlow(): Flow<List<TransactionEntity>>
    suspend fun insertTransaction(transaction: TransactionEntity): Long
    suspend fun updateTransaction(transaction: TransactionEntity)
    suspend fun deleteTransaction(transaction: TransactionEntity)
    suspend fun deleteDuplicates()
    suspend fun processIncomingText(rawText: String, source: String, fallbackTimestamp: Long = System.currentTimeMillis()): TransactionEntity?

    fun getAllCategoriesFlow(): Flow<List<CategoryEntity>>
    suspend fun addCategory(name: String, emoji: String)
    suspend fun deleteCategory(category: CategoryEntity)

    fun getMonthlyBudgetFlow(monthKey: String): Flow<BudgetEntity?>
    suspend fun setMonthlyBudget(monthKey: String, amount: Double)
    fun getCategoryBudgetsFlow(monthKey: String): Flow<List<CategoryBudgetEntity>>
    suspend fun setCategoryBudget(monthKey: String, categoryName: String, amount: Double)

    fun getAllMerchantRulesFlow(): Flow<List<MerchantRuleEntity>>
    suspend fun addMerchantRule(merchant: String, category: String)
    suspend fun deleteMerchantRule(rule: MerchantRuleEntity)

    fun getActiveRecurringFlow(): Flow<List<RecurringExpenseEntity>>
    suspend fun addRecurringExpense(title: String, amount: Double, category: String, dayOfMonth: Int)
    suspend fun updateRecurringExpense(recurring: RecurringExpenseEntity)
    suspend fun deleteRecurringExpense(recurring: RecurringExpenseEntity)

    fun getMonthlySummaryFlow(monthKey: String): Flow<MonthlySummaryEntity?>
    suspend fun generateMonthlySummary(monthKey: String): MonthlySummaryEntity
    suspend fun getAllTransactionsSync(): List<TransactionEntity>
}

class ExpenseRepositoryImpl(
    private val db: AppDatabase
) : ExpenseRepository {

    private val transactionDao = db.transactionDao()
    private val categoryDao = db.categoryDao()
    private val budgetDao = db.budgetDao()
    private val ruleDao = db.merchantRuleDao()
    private val recurringDao = db.recurringExpenseDao()
    private val summaryDao = db.monthlySummaryDao()

    private val duplicateDetector = DuplicateDetector(transactionDao)
    private val categorizer = Categorizer(ruleDao)

    override fun getAllTransactionsFlow(): Flow<List<TransactionEntity>> =
        transactionDao.getAllTransactionsFlow()

    override fun getDuplicateTransactionsFlow(): Flow<List<TransactionEntity>> =
        transactionDao.getDuplicateTransactionsFlow()

    override suspend fun insertTransaction(transaction: TransactionEntity): Long =
        transactionDao.insertTransaction(transaction)

    override suspend fun updateTransaction(transaction: TransactionEntity) =
        transactionDao.updateTransaction(transaction)

    override suspend fun deleteTransaction(transaction: TransactionEntity) =
        transactionDao.deleteTransaction(transaction)

    override suspend fun deleteDuplicates() =
        transactionDao.deleteDuplicates()

    override suspend fun processIncomingText(rawText: String, source: String, fallbackTimestamp: Long): TransactionEntity? {
        val sender = if (source.contains("(") && source.contains(")")) {
            source.substringAfter("(").substringBefore(")")
        } else null
        val parsed = TransactionParser.parse(rawText, sender, fallbackTimestamp) ?: return null

        val (isDup, dupOfId) = duplicateDetector.checkDuplicate(parsed)
        if (isDup) {
            // Skip inserting duplicates to keep calculations accurate
            return null
        }
        val category = categorizer.categorize(parsed.merchant, rawText, parsed.isIncome)

        val entity = TransactionEntity(
            amount = parsed.amount,
            isIncome = parsed.isIncome,
            merchant = parsed.merchant,
            category = category,
            timestamp = parsed.timestamp,
            paymentMethod = parsed.paymentMethod,
            refId = parsed.refId,
            bankName = parsed.bankName,
            accountNumber = parsed.accountNumber,
            accountBalance = parsed.availableBalance,
            source = source,
            isDuplicate = false,
            duplicateOfId = null,
            rawText = parsed.rawSanitizedText
        )

        val id = transactionDao.insertTransaction(entity)
        return entity.copy(id = id)
    }

    override fun getAllCategoriesFlow(): Flow<List<CategoryEntity>> =
        categoryDao.getAllCategoriesFlow()

    override suspend fun addCategory(name: String, emoji: String) {
        categoryDao.insertCategory(CategoryEntity(name = name, emoji = emoji, isDefault = false))
    }

    override suspend fun deleteCategory(category: CategoryEntity) {
        categoryDao.deleteCategory(category)
    }

    override fun getMonthlyBudgetFlow(monthKey: String): Flow<BudgetEntity?> =
        budgetDao.getMonthlyBudgetFlow(monthKey)

    override suspend fun setMonthlyBudget(monthKey: String, amount: Double) {
        budgetDao.setMonthlyBudget(BudgetEntity(monthKey = monthKey, totalBudget = amount, currencySymbol = "\u20B9"))
    }

    override fun getCategoryBudgetsFlow(monthKey: String): Flow<List<CategoryBudgetEntity>> =
        budgetDao.getCategoryBudgetsFlow(monthKey)

    override suspend fun setCategoryBudget(monthKey: String, categoryName: String, amount: Double) {
        budgetDao.setCategoryBudget(
            CategoryBudgetEntity(
                monthKey = monthKey,
                categoryName = categoryName,
                allocatedAmount = amount
            )
        )
    }

    override fun getAllMerchantRulesFlow(): Flow<List<MerchantRuleEntity>> =
        ruleDao.getAllRulesFlow()

    override suspend fun addMerchantRule(merchant: String, category: String) {
        ruleDao.insertRule(
            MerchantRuleEntity(
                merchantPattern = merchant.uppercase(),
                categoryName = category,
                userCreated = true
            )
        )
    }

    override suspend fun deleteMerchantRule(rule: MerchantRuleEntity) {
        ruleDao.deleteRule(rule)
    }

    override fun getActiveRecurringFlow(): Flow<List<RecurringExpenseEntity>> =
        recurringDao.getActiveRecurringFlow()

    override suspend fun addRecurringExpense(title: String, amount: Double, category: String, dayOfMonth: Int) {
        recurringDao.insertRecurring(
            RecurringExpenseEntity(
                title = title,
                amount = amount,
                categoryName = category,
                dayOfMonth = dayOfMonth,
                frequency = "MONTHLY",
                paymentMethod = "UPI",
                isAutoDeducted = true,
                isActive = true
            )
        )
    }

    override suspend fun updateRecurringExpense(recurring: RecurringExpenseEntity) {
        recurringDao.updateRecurring(recurring)
    }

    override suspend fun deleteRecurringExpense(recurring: RecurringExpenseEntity) {
        recurringDao.deleteRecurring(recurring)
    }

    override fun getMonthlySummaryFlow(monthKey: String): Flow<MonthlySummaryEntity?> =
        summaryDao.getSummaryFlow(monthKey)

    override suspend fun generateMonthlySummary(monthKey: String): MonthlySummaryEntity {
        val allTxns = transactionDao.getAllTransactionsSync()
        val monthSdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val monthlyTxns = allTxns.filter { !it.isDuplicate && monthSdf.format(Date(it.timestamp)) == monthKey }

        val totalIncome = monthlyTxns.filter { it.isIncome }.sumOf { it.amount }
        val totalExpense = monthlyTxns.filter { !it.isIncome }.sumOf { it.amount }
        val totalSavings = totalIncome - totalExpense

        val categoryGroups = monthlyTxns.filter { !it.isIncome }.groupBy { it.category }
        val highestCategoryEntry = categoryGroups.maxByOrNull { it.value.sumOf { t -> t.amount } }
        val highestCategory = highestCategoryEntry?.key ?: "None"
        val highestCategoryAmount = highestCategoryEntry?.value?.sumOf { it.amount } ?: 0.0

        val highestTxn = monthlyTxns.filter { !it.isIncome }.maxByOrNull { it.amount }
        val highestTxnMerchant = highestTxn?.merchant ?: "None"
        val highestTxnAmount = highestTxn?.amount ?: 0.0

        val daysInMonth = Calendar.getInstance().getActualMaximum(Calendar.DAY_OF_MONTH)
        val avgDaily = if (daysInMonth > 0) totalExpense / daysInMonth else 0.0

        val budget = budgetDao.getMonthlyBudgetSync(monthKey)
        val budgetUsagePct = if (budget != null && budget.totalBudget > 0) {
            (totalExpense / budget.totalBudget) * 100
        } else 0.0

        val summary = MonthlySummaryEntity(
            monthKey = monthKey,
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            totalSavings = totalSavings,
            highestCategory = highestCategory,
            highestCategoryAmount = highestCategoryAmount,
            highestTransactionMerchant = highestTxnMerchant,
            highestTransactionAmount = highestTxnAmount,
            averageDailySpending = avgDaily,
            budgetUsagePercentage = budgetUsagePct,
            generatedAt = System.currentTimeMillis()
        )
        summaryDao.insertSummary(summary)
        return summary
    }

    override suspend fun getAllTransactionsSync(): List<TransactionEntity> =
        transactionDao.getAllTransactionsSync()
}
