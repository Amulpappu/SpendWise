package com.example.smartexpensetracker.data.local

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.smartexpensetracker.data.local.dao.*
import com.example.smartexpensetracker.data.local.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AppDatabase private constructor(context: Context) : SQLiteOpenHelper(
    context.applicationContext,
    "smart_expense_tracker_native.db",
    null,
    1
) {

    private val dbScope = CoroutineScope(Dispatchers.IO)

    // Reactive StateFlows for real-time UI updates
    private val _transactionsFlow = MutableStateFlow<List<TransactionEntity>>(emptyList())
    private val _duplicateTransactionsFlow = MutableStateFlow<List<TransactionEntity>>(emptyList())
    private val _categoriesFlow = MutableStateFlow<List<CategoryEntity>>(DEFAULT_CATEGORIES)
    private val _monthlyBudgetsMap = mutableMapOf<String, MutableStateFlow<BudgetEntity?>>()
    private val _categoryBudgetsMap = mutableMapOf<String, MutableStateFlow<List<CategoryBudgetEntity>>>()
    private val _merchantRulesFlow = MutableStateFlow<List<MerchantRuleEntity>>(emptyList())
    private val _recurringFlow = MutableStateFlow<List<RecurringExpenseEntity>>(emptyList())
    private val _monthlySummariesMap = mutableMapOf<String, MutableStateFlow<MonthlySummaryEntity?>>()

    init {
        // Initial load in background
        dbScope.launch {
            refreshTransactions()
            refreshCategories()
            refreshMerchantRules()
            refreshRecurring()
        }
    }

    override fun onOpen(db: SQLiteDatabase) {
        super.onOpen(db)
        try {
            db.execSQL("ALTER TABLE transactions ADD COLUMN accountNumber TEXT")
        } catch (e: Exception) {}
        try {
            db.execSQL("ALTER TABLE transactions ADD COLUMN accountBalance REAL")
        } catch (e: Exception) {}
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS transactions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                amount REAL NOT NULL,
                isIncome INTEGER NOT NULL,
                merchant TEXT NOT NULL,
                category TEXT NOT NULL,
                timestamp INTEGER NOT NULL,
                paymentMethod TEXT NOT NULL,
                refId TEXT,
                bankName TEXT,
                accountNumber TEXT,
                accountBalance REAL,
                source TEXT NOT NULL,
                note TEXT NOT NULL,
                isDuplicate INTEGER NOT NULL,
                duplicateOfId INTEGER,
                rawText TEXT NOT NULL
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS categories (
                name TEXT PRIMARY KEY,
                emoji TEXT NOT NULL,
                isDefault INTEGER NOT NULL
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS monthly_budget (
                monthKey TEXT PRIMARY KEY,
                totalBudget REAL NOT NULL,
                currencySymbol TEXT NOT NULL,
                warn75Sent INTEGER NOT NULL,
                warn90Sent INTEGER NOT NULL,
                warn100Sent INTEGER NOT NULL
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS category_budgets (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                monthKey TEXT NOT NULL,
                categoryName TEXT NOT NULL,
                allocatedAmount REAL NOT NULL,
                warn75Sent INTEGER NOT NULL,
                warn90Sent INTEGER NOT NULL,
                warn100Sent INTEGER NOT NULL
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS merchant_rules (
                merchantPattern TEXT PRIMARY KEY,
                categoryName TEXT NOT NULL,
                userCreated INTEGER NOT NULL
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS recurring_expenses (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                amount REAL NOT NULL,
                categoryName TEXT NOT NULL,
                frequency TEXT NOT NULL,
                dayOfMonth INTEGER NOT NULL,
                paymentMethod TEXT NOT NULL,
                isAutoDeducted INTEGER NOT NULL,
                isActive INTEGER NOT NULL
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS monthly_summaries (
                monthKey TEXT PRIMARY KEY,
                totalIncome REAL NOT NULL,
                totalExpense REAL NOT NULL,
                totalSavings REAL NOT NULL,
                highestCategory TEXT NOT NULL,
                highestCategoryAmount REAL NOT NULL,
                highestTransactionMerchant TEXT NOT NULL,
                highestTransactionAmount REAL NOT NULL,
                averageDailySpending REAL NOT NULL,
                budgetUsagePercentage REAL NOT NULL,
                generatedAt INTEGER NOT NULL
            )
        """.trimIndent())

        // Prepopulate default categories
        for (cat in DEFAULT_CATEGORIES) {
            val cv = ContentValues().apply {
                put("name", cat.name)
                put("emoji", cat.emoji)
                put("isDefault", if (cat.isDefault) 1 else 0)
            }
            db.insertWithOnConflict("categories", null, cv, SQLiteDatabase.CONFLICT_IGNORE)
        }

        // Prepopulate default merchant rules
        val defaultRules = listOf(
            "SWIGGY" to "Food",
            "ZOMATO" to "Food",
            "UBER" to "Transport",
            "OLA" to "Transport",
            "AMAZON" to "Shopping",
            "FLIPKART" to "Shopping",
            "NETFLIX" to "Subscriptions",
            "SPOTIFY" to "Subscriptions"
        )
        for ((m, c) in defaultRules) {
            val cv = ContentValues().apply {
                put("merchantPattern", m)
                put("categoryName", c)
                put("userCreated", 0)
            }
            db.insertWithOnConflict("merchant_rules", null, cv, SQLiteDatabase.CONFLICT_IGNORE)
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}

    // DAOs
    fun transactionDao(): TransactionDao = object : TransactionDao {
        override fun getAllTransactionsFlow(): Flow<List<TransactionEntity>> = _transactionsFlow.asStateFlow()
        override fun getDuplicateTransactionsFlow(): Flow<List<TransactionEntity>> = _duplicateTransactionsFlow.asStateFlow()

        override suspend fun getAllTransactionsSync(): List<TransactionEntity> {
            val list = mutableListOf<TransactionEntity>()
            val db = readableDatabase
            db.rawQuery("SELECT * FROM transactions ORDER BY timestamp DESC", null).use { cursor ->
                while (cursor.moveToNext()) {
                    list.add(cursorToTransaction(cursor))
                }
            }
            return list
        }

        override suspend fun findRecentTransactions(startTime: Long, endTime: Long): List<TransactionEntity> {
            val list = mutableListOf<TransactionEntity>()
            val db = readableDatabase
            db.rawQuery("SELECT * FROM transactions WHERE timestamp BETWEEN ? AND ?", arrayOf(startTime.toString(), endTime.toString())).use { cursor ->
                while (cursor.moveToNext()) {
                    list.add(cursorToTransaction(cursor))
                }
            }
            return list
        }

        override suspend fun findByRefId(refId: String): TransactionEntity? {
            val db = readableDatabase
            db.rawQuery("SELECT * FROM transactions WHERE refId = ? LIMIT 1", arrayOf(refId)).use { cursor ->
                if (cursor.moveToNext()) {
                    return cursorToTransaction(cursor)
                }
            }
            return null
        }

        override suspend fun insertTransaction(transaction: TransactionEntity): Long {
            val db = writableDatabase
            val cv = ContentValues().apply {
                put("amount", transaction.amount)
                put("isIncome", if (transaction.isIncome) 1 else 0)
                put("merchant", transaction.merchant)
                put("category", transaction.category)
                put("timestamp", transaction.timestamp)
                put("paymentMethod", transaction.paymentMethod)
                put("refId", transaction.refId)
                put("bankName", transaction.bankName)
                put("accountNumber", transaction.accountNumber)
                put("accountBalance", transaction.accountBalance)
                put("source", transaction.source)
                put("note", transaction.note)
                put("isDuplicate", if (transaction.isDuplicate) 1 else 0)
                put("duplicateOfId", transaction.duplicateOfId)
                put("rawText", transaction.rawText)
            }
            val id = db.insert("transactions", null, cv)
            refreshTransactions()
            return id
        }

        override suspend fun updateTransaction(transaction: TransactionEntity) {
            val db = writableDatabase
            val cv = ContentValues().apply {
                put("amount", transaction.amount)
                put("isIncome", if (transaction.isIncome) 1 else 0)
                put("merchant", transaction.merchant)
                put("category", transaction.category)
                put("timestamp", transaction.timestamp)
                put("paymentMethod", transaction.paymentMethod)
                put("refId", transaction.refId)
                put("bankName", transaction.bankName)
                put("accountNumber", transaction.accountNumber)
                put("accountBalance", transaction.accountBalance)
                put("source", transaction.source)
                put("note", transaction.note)
                put("isDuplicate", if (transaction.isDuplicate) 1 else 0)
                put("duplicateOfId", transaction.duplicateOfId)
                put("rawText", transaction.rawText)
            }
            db.update("transactions", cv, "id = ?", arrayOf(transaction.id.toString()))
            refreshTransactions()
        }

        override suspend fun deleteTransaction(transaction: TransactionEntity) {
            val db = writableDatabase
            db.delete("transactions", "id = ?", arrayOf(transaction.id.toString()))
            refreshTransactions()
        }

        override suspend fun deleteDuplicates() {
            val db = writableDatabase
            db.delete("transactions", "isDuplicate = 1", null)
            refreshTransactions()
        }
    }

    fun categoryDao(): CategoryDao = object : CategoryDao {
        override fun getAllCategoriesFlow(): Flow<List<CategoryEntity>> = _categoriesFlow.asStateFlow()

        override suspend fun getAllCategoriesSync(): List<CategoryEntity> {
            val list = mutableListOf<CategoryEntity>()
            val db = readableDatabase
            db.rawQuery("SELECT * FROM categories", null).use { cursor ->
                while (cursor.moveToNext()) {
                    list.add(CategoryEntity(
                        name = cursor.getString(cursor.getColumnIndexOrThrow("name")),
                        emoji = cursor.getString(cursor.getColumnIndexOrThrow("emoji")),
                        isDefault = cursor.getInt(cursor.getColumnIndexOrThrow("isDefault")) == 1
                    ))
                }
            }
            return if (list.isEmpty()) DEFAULT_CATEGORIES else list
        }

        override suspend fun insertCategory(category: CategoryEntity) {
            val db = writableDatabase
            val cv = ContentValues().apply {
                put("name", category.name)
                put("emoji", category.emoji)
                put("isDefault", if (category.isDefault) 1 else 0)
            }
            db.insertWithOnConflict("categories", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
            refreshCategories()
        }

        override suspend fun insertCategories(categories: List<CategoryEntity>) {
            val db = writableDatabase
            for (cat in categories) {
                val cv = ContentValues().apply {
                    put("name", cat.name)
                    put("emoji", cat.emoji)
                    put("isDefault", if (cat.isDefault) 1 else 0)
                }
                db.insertWithOnConflict("categories", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
            }
            refreshCategories()
        }

        override suspend fun deleteCategory(category: CategoryEntity) {
            val db = writableDatabase
            db.delete("categories", "name = ?", arrayOf(category.name))
            refreshCategories()
        }
    }

    fun budgetDao(): BudgetDao = object : BudgetDao {
        override fun getMonthlyBudgetFlow(monthKey: String): Flow<BudgetEntity?> {
            return _monthlyBudgetsMap.getOrPut(monthKey) {
                MutableStateFlow(getMonthlyBudgetSync(monthKey))
            }.asStateFlow()
        }

        override fun getMonthlyBudgetSync(monthKey: String): BudgetEntity? {
            val db = readableDatabase
            db.rawQuery("SELECT * FROM monthly_budget WHERE monthKey = ? LIMIT 1", arrayOf(monthKey)).use { cursor ->
                if (cursor.moveToNext()) {
                    return BudgetEntity(
                        monthKey = cursor.getString(cursor.getColumnIndexOrThrow("monthKey")),
                        totalBudget = cursor.getDouble(cursor.getColumnIndexOrThrow("totalBudget")),
                        currencySymbol = cursor.getString(cursor.getColumnIndexOrThrow("currencySymbol")),
                        warn75Sent = cursor.getInt(cursor.getColumnIndexOrThrow("warn75Sent")) == 1,
                        warn90Sent = cursor.getInt(cursor.getColumnIndexOrThrow("warn90Sent")) == 1,
                        warn100Sent = cursor.getInt(cursor.getColumnIndexOrThrow("warn100Sent")) == 1
                    )
                }
            }
            return null
        }

        override suspend fun setMonthlyBudget(budget: BudgetEntity) {
            val db = writableDatabase
            val cv = ContentValues().apply {
                put("monthKey", budget.monthKey)
                put("totalBudget", budget.totalBudget)
                put("currencySymbol", budget.currencySymbol)
                put("warn75Sent", if (budget.warn75Sent) 1 else 0)
                put("warn90Sent", if (budget.warn90Sent) 1 else 0)
                put("warn100Sent", if (budget.warn100Sent) 1 else 0)
            }
            db.insertWithOnConflict("monthly_budget", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
            _monthlyBudgetsMap[budget.monthKey]?.value = budget
        }

        override fun getCategoryBudgetsFlow(monthKey: String): Flow<List<CategoryBudgetEntity>> {
            return _categoryBudgetsMap.getOrPut(monthKey) {
                MutableStateFlow(getCategoryBudgetsSync(monthKey))
            }.asStateFlow()
        }

        override fun getCategoryBudgetsSync(monthKey: String): List<CategoryBudgetEntity> {
            val list = mutableListOf<CategoryBudgetEntity>()
            val db = readableDatabase
            db.rawQuery("SELECT * FROM category_budgets WHERE monthKey = ?", arrayOf(monthKey)).use { cursor ->
                while (cursor.moveToNext()) {
                    list.add(CategoryBudgetEntity(
                        id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                        monthKey = cursor.getString(cursor.getColumnIndexOrThrow("monthKey")),
                        categoryName = cursor.getString(cursor.getColumnIndexOrThrow("categoryName")),
                        allocatedAmount = cursor.getDouble(cursor.getColumnIndexOrThrow("allocatedAmount")),
                        warn75Sent = cursor.getInt(cursor.getColumnIndexOrThrow("warn75Sent")) == 1,
                        warn90Sent = cursor.getInt(cursor.getColumnIndexOrThrow("warn90Sent")) == 1,
                        warn100Sent = cursor.getInt(cursor.getColumnIndexOrThrow("warn100Sent")) == 1
                    ))
                }
            }
            return list
        }

        override suspend fun setCategoryBudget(budget: CategoryBudgetEntity) {
            val db = writableDatabase
            val cv = ContentValues().apply {
                put("monthKey", budget.monthKey)
                put("categoryName", budget.categoryName)
                put("allocatedAmount", budget.allocatedAmount)
                put("warn75Sent", if (budget.warn75Sent) 1 else 0)
                put("warn90Sent", if (budget.warn90Sent) 1 else 0)
                put("warn100Sent", if (budget.warn100Sent) 1 else 0)
            }
            db.insertWithOnConflict("category_budgets", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
            _categoryBudgetsMap[budget.monthKey]?.value = getCategoryBudgetsSync(budget.monthKey)
        }
    }

    fun merchantRuleDao(): MerchantRuleDao = object : MerchantRuleDao {
        override fun getAllRulesFlow(): Flow<List<MerchantRuleEntity>> = _merchantRulesFlow.asStateFlow()

        override suspend fun getAllRulesSync(): List<MerchantRuleEntity> {
            val list = mutableListOf<MerchantRuleEntity>()
            val db = readableDatabase
            db.rawQuery("SELECT * FROM merchant_rules", null).use { cursor ->
                while (cursor.moveToNext()) {
                    list.add(MerchantRuleEntity(
                        merchantPattern = cursor.getString(cursor.getColumnIndexOrThrow("merchantPattern")),
                        categoryName = cursor.getString(cursor.getColumnIndexOrThrow("categoryName")),
                        userCreated = cursor.getInt(cursor.getColumnIndexOrThrow("userCreated")) == 1
                    ))
                }
            }
            return list
        }

        override suspend fun findRuleForMerchant(merchant: String): MerchantRuleEntity? {
            val db = readableDatabase
            db.rawQuery("SELECT * FROM merchant_rules WHERE merchantPattern = ? LIMIT 1", arrayOf(merchant.uppercase().trim())).use { cursor ->
                if (cursor.moveToNext()) {
                    return MerchantRuleEntity(
                        merchantPattern = cursor.getString(cursor.getColumnIndexOrThrow("merchantPattern")),
                        categoryName = cursor.getString(cursor.getColumnIndexOrThrow("categoryName")),
                        userCreated = cursor.getInt(cursor.getColumnIndexOrThrow("userCreated")) == 1
                    )
                }
            }
            return null
        }

        override suspend fun insertRule(rule: MerchantRuleEntity) {
            val db = writableDatabase
            val cv = ContentValues().apply {
                put("merchantPattern", rule.merchantPattern.uppercase().trim())
                put("categoryName", rule.categoryName)
                put("userCreated", if (rule.userCreated) 1 else 0)
            }
            db.insertWithOnConflict("merchant_rules", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
            refreshMerchantRules()
        }

        override suspend fun deleteRule(rule: MerchantRuleEntity) {
            val db = writableDatabase
            db.delete("merchant_rules", "merchantPattern = ?", arrayOf(rule.merchantPattern))
            refreshMerchantRules()
        }
    }

    fun recurringExpenseDao(): RecurringExpenseDao = object : RecurringExpenseDao {
        override fun getActiveRecurringFlow(): Flow<List<RecurringExpenseEntity>> = _recurringFlow.asStateFlow()

        override suspend fun insertRecurring(expense: RecurringExpenseEntity): Long {
            val db = writableDatabase
            val cv = ContentValues().apply {
                put("title", expense.title)
                put("amount", expense.amount)
                put("categoryName", expense.categoryName)
                put("frequency", expense.frequency)
                put("dayOfMonth", expense.dayOfMonth)
                put("paymentMethod", expense.paymentMethod)
                put("isAutoDeducted", if (expense.isAutoDeducted) 1 else 0)
                put("isActive", if (expense.isActive) 1 else 0)
            }
            val id = db.insert("recurring_expenses", null, cv)
            refreshRecurring()
            return id
        }

        override suspend fun updateRecurring(expense: RecurringExpenseEntity) {
            val db = writableDatabase
            val cv = ContentValues().apply {
                put("title", expense.title)
                put("amount", expense.amount)
                put("categoryName", expense.categoryName)
                put("frequency", expense.frequency)
                put("dayOfMonth", expense.dayOfMonth)
                put("paymentMethod", expense.paymentMethod)
                put("isAutoDeducted", if (expense.isAutoDeducted) 1 else 0)
                put("isActive", if (expense.isActive) 1 else 0)
            }
            db.update("recurring_expenses", cv, "id = ?", arrayOf(expense.id.toString()))
            refreshRecurring()
        }

        override suspend fun deleteRecurring(expense: RecurringExpenseEntity) {
            val db = writableDatabase
            db.delete("recurring_expenses", "id = ?", arrayOf(expense.id.toString()))
            refreshRecurring()
        }
    }

    fun monthlySummaryDao(): MonthlySummaryDao = object : MonthlySummaryDao {
        override fun getSummaryFlow(monthKey: String): Flow<MonthlySummaryEntity?> {
            return _monthlySummariesMap.getOrPut(monthKey) {
                MutableStateFlow(getSummarySync(monthKey))
            }.asStateFlow()
        }

        private fun getSummarySync(monthKey: String): MonthlySummaryEntity? {
            val db = readableDatabase
            db.rawQuery("SELECT * FROM monthly_summaries WHERE monthKey = ? LIMIT 1", arrayOf(monthKey)).use { cursor ->
                if (cursor.moveToNext()) {
                    return MonthlySummaryEntity(
                        monthKey = cursor.getString(cursor.getColumnIndexOrThrow("monthKey")),
                        totalIncome = cursor.getDouble(cursor.getColumnIndexOrThrow("totalIncome")),
                        totalExpense = cursor.getDouble(cursor.getColumnIndexOrThrow("totalExpense")),
                        totalSavings = cursor.getDouble(cursor.getColumnIndexOrThrow("totalSavings")),
                        highestCategory = cursor.getString(cursor.getColumnIndexOrThrow("highestCategory")),
                        highestCategoryAmount = cursor.getDouble(cursor.getColumnIndexOrThrow("highestCategoryAmount")),
                        highestTransactionMerchant = cursor.getString(cursor.getColumnIndexOrThrow("highestTransactionMerchant")),
                        highestTransactionAmount = cursor.getDouble(cursor.getColumnIndexOrThrow("highestTransactionAmount")),
                        averageDailySpending = cursor.getDouble(cursor.getColumnIndexOrThrow("averageDailySpending")),
                        budgetUsagePercentage = cursor.getDouble(cursor.getColumnIndexOrThrow("budgetUsagePercentage")),
                        generatedAt = cursor.getLong(cursor.getColumnIndexOrThrow("generatedAt"))
                    )
                }
            }
            return null
        }

        override suspend fun insertSummary(summary: MonthlySummaryEntity) {
            val db = writableDatabase
            val cv = ContentValues().apply {
                put("monthKey", summary.monthKey)
                put("totalIncome", summary.totalIncome)
                put("totalExpense", summary.totalExpense)
                put("totalSavings", summary.totalSavings)
                put("highestCategory", summary.highestCategory)
                put("highestCategoryAmount", summary.highestCategoryAmount)
                put("highestTransactionMerchant", summary.highestTransactionMerchant)
                put("highestTransactionAmount", summary.highestTransactionAmount)
                put("averageDailySpending", summary.averageDailySpending)
                put("budgetUsagePercentage", summary.budgetUsagePercentage)
                put("generatedAt", summary.generatedAt)
            }
            db.insertWithOnConflict("monthly_summaries", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
            _monthlySummariesMap[summary.monthKey]?.value = summary
        }
    }

    fun refreshTransactions() {
        val list = mutableListOf<TransactionEntity>()
        val dupList = mutableListOf<TransactionEntity>()
        val db = readableDatabase
        db.rawQuery("SELECT * FROM transactions ORDER BY timestamp DESC", null).use { cursor ->
            while (cursor.moveToNext()) {
                val t = cursorToTransaction(cursor)
                list.add(t)
                if (t.isDuplicate) dupList.add(t)
            }
        }
        _transactionsFlow.value = list
        _duplicateTransactionsFlow.value = dupList
    }

    private fun refreshCategories() {
        val list = mutableListOf<CategoryEntity>()
        val db = readableDatabase
        db.rawQuery("SELECT * FROM categories", null).use { cursor ->
            while (cursor.moveToNext()) {
                list.add(CategoryEntity(
                    name = cursor.getString(cursor.getColumnIndexOrThrow("name")),
                    emoji = cursor.getString(cursor.getColumnIndexOrThrow("emoji")),
                    isDefault = cursor.getInt(cursor.getColumnIndexOrThrow("isDefault")) == 1
                ))
            }
        }
        _categoriesFlow.value = if (list.isEmpty()) DEFAULT_CATEGORIES else list
    }

    private fun refreshMerchantRules() {
        val list = mutableListOf<MerchantRuleEntity>()
        val db = readableDatabase
        db.rawQuery("SELECT * FROM merchant_rules", null).use { cursor ->
            while (cursor.moveToNext()) {
                list.add(MerchantRuleEntity(
                    merchantPattern = cursor.getString(cursor.getColumnIndexOrThrow("merchantPattern")),
                    categoryName = cursor.getString(cursor.getColumnIndexOrThrow("categoryName")),
                    userCreated = cursor.getInt(cursor.getColumnIndexOrThrow("userCreated")) == 1
                ))
            }
        }
        _merchantRulesFlow.value = list
    }

    private fun refreshRecurring() {
        val list = mutableListOf<RecurringExpenseEntity>()
        val db = readableDatabase
        db.rawQuery("SELECT * FROM recurring_expenses", null).use { cursor ->
            while (cursor.moveToNext()) {
                list.add(RecurringExpenseEntity(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                    title = cursor.getString(cursor.getColumnIndexOrThrow("title")),
                    amount = cursor.getDouble(cursor.getColumnIndexOrThrow("amount")),
                    categoryName = cursor.getString(cursor.getColumnIndexOrThrow("categoryName")),
                    frequency = cursor.getString(cursor.getColumnIndexOrThrow("frequency")),
                    dayOfMonth = cursor.getInt(cursor.getColumnIndexOrThrow("dayOfMonth")),
                    paymentMethod = cursor.getString(cursor.getColumnIndexOrThrow("paymentMethod")),
                    isAutoDeducted = cursor.getInt(cursor.getColumnIndexOrThrow("isAutoDeducted")) == 1,
                    isActive = cursor.getInt(cursor.getColumnIndexOrThrow("isActive")) == 1
                ))
            }
        }
        _recurringFlow.value = list
    }

    private fun cursorToTransaction(cursor: Cursor): TransactionEntity {
        return TransactionEntity(
            id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
            amount = cursor.getDouble(cursor.getColumnIndexOrThrow("amount")),
            isIncome = cursor.getInt(cursor.getColumnIndexOrThrow("isIncome")) == 1,
            merchant = cursor.getString(cursor.getColumnIndexOrThrow("merchant")),
            category = cursor.getString(cursor.getColumnIndexOrThrow("category")),
            timestamp = cursor.getLong(cursor.getColumnIndexOrThrow("timestamp")),
            paymentMethod = cursor.getString(cursor.getColumnIndexOrThrow("paymentMethod")),
            refId = cursor.getString(cursor.getColumnIndexOrThrow("refId")),
            bankName = cursor.getString(cursor.getColumnIndexOrThrow("bankName")),
            accountNumber = if (cursor.getColumnIndex("accountNumber") != -1 && !cursor.isNull(cursor.getColumnIndex("accountNumber"))) cursor.getString(cursor.getColumnIndex("accountNumber")) else null,
            accountBalance = if (cursor.getColumnIndex("accountBalance") != -1 && !cursor.isNull(cursor.getColumnIndex("accountBalance"))) cursor.getDouble(cursor.getColumnIndex("accountBalance")) else null,
            source = cursor.getString(cursor.getColumnIndexOrThrow("source")),
            note = cursor.getString(cursor.getColumnIndexOrThrow("note")),
            isDuplicate = cursor.getInt(cursor.getColumnIndexOrThrow("isDuplicate")) == 1,
            duplicateOfId = if (cursor.isNull(cursor.getColumnIndexOrThrow("duplicateOfId"))) null else cursor.getLong(cursor.getColumnIndexOrThrow("duplicateOfId")),
            rawText = cursor.getString(cursor.getColumnIndexOrThrow("rawText"))
        )
    }

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = AppDatabase(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }

        val DEFAULT_CATEGORIES = listOf(
            CategoryEntity("Friends", "👥", true),
            CategoryEntity("Groceries", "ðŸ¥¦", true),
            CategoryEntity("Food", "ðŸ”", true),
            CategoryEntity("Recharge & Bills", "ðŸ“±", true),
            CategoryEntity("Shopping", "ðŸ›’", true),
            CategoryEntity("Transport", "ðŸšŒ", true),
            CategoryEntity("Entertainment", "ðŸŽ¬", true),
            CategoryEntity("Gaming", "ðŸŽ®", true),
            CategoryEntity("Salary", "ðŸ’¼", true),
            CategoryEntity("Income", "ðŸ’µ", true),
            CategoryEntity("Education", "ðŸŽ“", true),
            CategoryEntity("Bills", "ðŸ’¡", true),
            CategoryEntity("Subscriptions", "ðŸ”„", true),
            CategoryEntity("Travel", "âœˆï¸", true),
            CategoryEntity("Medical", "ðŸ¥", true),
            CategoryEntity("Technology", "ðŸ’»", true),
            CategoryEntity("Home", "ðŸ ", true),
            CategoryEntity("Other", "ðŸ’°", true)
        )
    }
}
