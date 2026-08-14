package com.example.smartexpensetracker.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartexpensetracker.data.export.BackupManager
import com.example.smartexpensetracker.data.export.DataExporter
import com.example.smartexpensetracker.data.export.GoogleSheetsSyncManager
import com.example.smartexpensetracker.data.local.AppDatabase
import com.example.smartexpensetracker.data.local.entity.*
import com.example.smartexpensetracker.data.repository.ExpenseRepository
import com.example.smartexpensetracker.data.repository.ExpenseRepositoryImpl
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository: ExpenseRepository = ExpenseRepositoryImpl(db)

    val currentMonthKey: String
        get() {
            val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
            return sdf.format(Date())
        }

    private val monthSdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())

    // All transactions from DB
    val allTransactions: StateFlow<List<TransactionEntity>> = repository.getAllTransactionsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val duplicateTransactions: StateFlow<List<TransactionEntity>> = repository.getDuplicateTransactionsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<CategoryEntity>> = repository.getAllCategoriesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val monthlyBudget: StateFlow<BudgetEntity?> = repository.getMonthlyBudgetFlow(currentMonthKey)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val categoryBudgets: StateFlow<List<CategoryBudgetEntity>> = repository.getCategoryBudgetsFlow(currentMonthKey)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val monthlySummary: StateFlow<MonthlySummaryEntity?> = repository.getMonthlySummaryFlow(currentMonthKey)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val recurringExpenses: StateFlow<List<RecurringExpenseEntity>> = repository.getActiveRecurringFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filter states
    val searchQuery = MutableStateFlow("")
    val selectedCategoryFilter = MutableStateFlow<String?>(null)
    val selectedTypeFilter = MutableStateFlow<String?>(null) // "Income", "Expense", or null
    val selectedPeriodFilter = MutableStateFlow("Month") // "All", "Today", "Week", "Month", "CustomDate", "DateRange"
    val selectedCustomDateMillis = MutableStateFlow<Long?>(null)
    val selectedStartDateMillis = MutableStateFlow<Long?>(null)
    val selectedEndDateMillis = MutableStateFlow<Long?>(null)

    // Google Sheets state
    val webhookUrl = MutableStateFlow(GoogleSheetsSyncManager.getWebhookUrl(application))
    val autoSyncEnabled = MutableStateFlow(GoogleSheetsSyncManager.isAutoSyncEnabled(application))

    // User Profile & Authentication state
    val userProfile = MutableStateFlow(com.example.smartexpensetracker.data.local.UserProfileManager.getUserProfile(application))
    val isLoggedIn = MutableStateFlow(userProfile.value.isLoggedIn)
    val currentGeneratedOtp = MutableStateFlow<String?>(null)

    fun sendOtp(phoneNumber: String): String {
        val otp = com.example.smartexpensetracker.data.local.UserProfileManager.generateOtp(getApplication(), phoneNumber)
        currentGeneratedOtp.value = otp
        userProfile.value = com.example.smartexpensetracker.data.local.UserProfileManager.getUserProfile(getApplication())
        // Dispatch real SMS locally on device (works 100% offline)
        com.example.smartexpensetracker.data.receiver.SpendWiseSmsOtpReceiver.sendVerificationSms(getApplication(), phoneNumber, otp)
        return otp
    }

    fun verifyOtp(enteredOtp: String): Boolean {
        val success = com.example.smartexpensetracker.data.local.UserProfileManager.verifyOtp(getApplication(), enteredOtp)
        if (success) {
            userProfile.value = com.example.smartexpensetracker.data.local.UserProfileManager.getUserProfile(getApplication())
            isLoggedIn.value = true
        }
        return success
    }

    fun completeOnboarding(userName: String, mobileNumber: String, accountNumber: String, bankName: String) {
        val profile = com.example.smartexpensetracker.data.local.UserProfile(
            userName = userName,
            mobileNumber = mobileNumber,
            accountNumber = accountNumber,
            bankName = bankName,
            isLoggedIn = true,
            isPhoneVerified = true
        )
        com.example.smartexpensetracker.data.local.UserProfileManager.saveUserProfile(getApplication(), profile)
        userProfile.value = profile
        isLoggedIn.value = true
    }

    fun logout() {
        com.example.smartexpensetracker.data.local.UserProfileManager.logout(getApplication())
        userProfile.value = com.example.smartexpensetracker.data.local.UserProfileManager.getUserProfile(getApplication())
        isLoggedIn.value = false
        currentGeneratedOtp.value = null
    }

    fun updateUserProfile(profile: com.example.smartexpensetracker.data.local.UserProfile) {
        com.example.smartexpensetracker.data.local.UserProfileManager.saveUserProfile(getApplication(), profile)
        userProfile.value = profile
    }

    fun autoDetectUserProfile(): com.example.smartexpensetracker.data.local.UserProfile {
        val detected = com.example.smartexpensetracker.data.local.UserProfileManager.autoDetectProfileFromTransactions(
            getApplication(),
            allTransactions.value
        )
        userProfile.value = detected
        return detected
    }

    private data class FilterParams(
        val query: String,
        val catFilter: String?,
        val typeFilter: String?,
        val periodFilter: String,
        val customDateMillis: Long?,
        val startDateMillis: Long?,
        val endDateMillis: Long?
    )

    private val filterParamsFlow = combine(
        searchQuery,
        selectedCategoryFilter,
        selectedTypeFilter,
        selectedPeriodFilter,
        selectedCustomDateMillis,
        selectedStartDateMillis,
        selectedEndDateMillis
    ) { params: Array<Any?> ->
        FilterParams(
            query = params[0] as String,
            catFilter = params[1] as String?,
            typeFilter = params[2] as String?,
            periodFilter = params[3] as String,
            customDateMillis = params[4] as Long?,
            startDateMillis = params[5] as Long?,
            endDateMillis = params[6] as Long?
        )
    }

    // Filtered transaction list (strictly excludes duplicates to keep user list accurate)
    val filteredTransactions: StateFlow<List<TransactionEntity>> = combine(
        allTransactions,
        filterParamsFlow
    ) { list, filter ->
        list.filter { t ->
            if (t.isDuplicate) return@filter false
            val matchesQuery = filter.query.isEmpty() || t.merchant.contains(filter.query, ignoreCase = true) || t.note.contains(filter.query, ignoreCase = true)
            val matchesCat = filter.catFilter == null || t.category.equals(filter.catFilter, ignoreCase = true)
            val matchesType = when (filter.typeFilter) {
                "Income" -> t.isIncome
                "Expense" -> !t.isIncome
                else -> true
            }
            val matchesPeriod = when (filter.periodFilter) {
                "Today" -> isToday(t.timestamp)
                "Week" -> isThisWeek(t.timestamp)
                "Month" -> isCurrentMonth(t.timestamp)
                "CustomDate" -> filter.customDateMillis != null && isSameDay(t.timestamp, filter.customDateMillis)
                "DateRange" -> {
                    val start = filter.startDateMillis?.let { getStartOfDay(it) } ?: Long.MIN_VALUE
                    val end = filter.endDateMillis?.let { getEndOfDay(it) } ?: (filter.startDateMillis?.let { getEndOfDay(it) } ?: Long.MAX_VALUE)
                    t.timestamp in start..end
                }
                else -> true // "All"
            }
            matchesQuery && matchesCat && matchesType && matchesPeriod
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Calculations (STRICTLY excludes duplicates: !it.isDuplicate)
    val totalMonthlyExpenses: StateFlow<Double> = allTransactions.map { list ->
        list.filter { !it.isIncome && !it.isDuplicate && isCurrentMonth(it.timestamp) }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalMonthlyIncome: StateFlow<Double> = allTransactions.map { list ->
        list.filter { it.isIncome && !it.isDuplicate && isCurrentMonth(it.timestamp) }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val todaySpending: StateFlow<Double> = allTransactions.map { list ->
        list.filter { !it.isIncome && !it.isDuplicate && isToday(it.timestamp) }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val weekSpending: StateFlow<Double> = allTransactions.map { list ->
        list.filter { !it.isIncome && !it.isDuplicate && isThisWeek(it.timestamp) }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val categoryExpenseMap: StateFlow<Map<String, Double>> = allTransactions.map { list ->
        list.filter { !it.isIncome && !it.isDuplicate && isCurrentMonth(it.timestamp) }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val dailyTrendData: StateFlow<List<Pair<String, Double>>> = allTransactions.map { list ->
        val sdf = SimpleDateFormat("dd MMM", Locale.getDefault())
        list.filter { !it.isIncome && !it.isDuplicate && isCurrentMonth(it.timestamp) }
            .groupBy { sdf.format(Date(it.timestamp)) }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
            .toList()
            .takeLast(7)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val latestBankBalance: StateFlow<Double?> = allTransactions.map { list ->
        list.filter { !it.isDuplicate && it.accountBalance != null && !it.source.contains("Test", ignoreCase = true) }
            .maxByOrNull { it.timestamp }
            ?.accountBalance
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val latestAccountNumber: StateFlow<String?> = allTransactions.map { list ->
        list.filter { !it.isDuplicate && !it.accountNumber.isNullOrEmpty() && !it.source.contains("Test", ignoreCase = true) }
            .maxByOrNull { it.timestamp }
            ?.accountNumber
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        // Auto-purge duplicate rows on launch so accurate balances are displayed
        viewModelScope.launch {
            repository.deleteDuplicates()
        }
    }

    // Actions
    fun addTransaction(amount: Double, isIncome: Boolean, merchant: String, category: String, paymentMethod: String, note: String) {
        viewModelScope.launch {
            val entity = TransactionEntity(
                amount = amount,
                isIncome = isIncome,
                merchant = merchant.uppercase().trim(),
                category = category,
                paymentMethod = paymentMethod,
                source = "Manual",
                note = note
            )
            val id = repository.insertTransaction(entity)
            if (autoSyncEnabled.value) {
                GoogleSheetsSyncManager.syncTransactionToSheet(getApplication(), entity.copy(id = id))
            }
        }
    }

    fun updateTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.updateTransaction(transaction)
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }

    fun deleteDuplicates(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteDuplicates()
            onComplete()
        }
    }

    fun simulateIncomingText(text: String) {
        viewModelScope.launch {
            val entity = repository.processIncomingText(text, source = "Simulated Test")
            if (entity != null && !entity.isDuplicate && autoSyncEnabled.value) {
                GoogleSheetsSyncManager.syncTransactionToSheet(getApplication(), entity)
            }
        }
    }

    fun setMonthlyBudget(amount: Double) {
        viewModelScope.launch {
            repository.setMonthlyBudget(currentMonthKey, amount)
        }
    }

    fun setCategoryBudget(categoryName: String, amount: Double) {
        viewModelScope.launch {
            repository.setCategoryBudget(currentMonthKey, categoryName, amount)
        }
    }

    fun generateMonthlySummary(monthKey: String, onComplete: (MonthlySummaryEntity) -> Unit = {}) {
        viewModelScope.launch {
            val summary = repository.generateMonthlySummary(monthKey)
            onComplete(summary)
        }
    }

    fun addCategory(name: String, emoji: String) {
        viewModelScope.launch {
            repository.addCategory(name.trim(), emoji.trim())
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch {
            repository.deleteCategory(category)
        }
    }

    fun addMerchantRule(merchant: String, category: String) {
        viewModelScope.launch {
            repository.addMerchantRule(merchant.uppercase().trim(), category)
        }
    }

    fun addRecurringExpense(title: String, amount: Double, category: String, dayOfMonth: Int) {
        viewModelScope.launch {
            repository.addRecurringExpense(title, amount, category, dayOfMonth)
        }
    }

    fun deleteRecurringExpense(entity: RecurringExpenseEntity) {
        viewModelScope.launch {
            repository.deleteRecurringExpense(entity)
        }
    }

    fun updateWebhookUrl(url: String) {
        GoogleSheetsSyncManager.saveWebhookUrl(getApplication(), url)
        webhookUrl.value = url
    }

    fun updateAutoSync(enabled: Boolean) {
        GoogleSheetsSyncManager.setAutoSyncEnabled(getApplication(), enabled)
        autoSyncEnabled.value = enabled
    }

    fun testGoogleSheetsSync(): Boolean {
        val testEntity = TransactionEntity(
            amount = 250.0,
            isIncome = false,
            merchant = "SWIGGY",
            category = "Food",
            paymentMethod = "UPI",
            source = "Manual Test",
            note = "Smart Expense Tracker Initial Test"
        )
        return kotlinx.coroutines.runBlocking {
            GoogleSheetsSyncManager.syncTransactionToSheet(getApplication(), testEntity)
        }
    }

    fun syncAllToGoogleSheets(onComplete: (Int) -> Unit = {}) {
        viewModelScope.launch {
            val count = GoogleSheetsSyncManager.syncAllTransactionsToSheet(getApplication(), allTransactions.value)
            onComplete(count)
        }
    }

    fun scanSmsInbox(onComplete: (Int) -> Unit = {}) {
        viewModelScope.launch {
            val count = com.example.smartexpensetracker.data.parser.SmsInboxScanner.scanInbox(getApplication())
            onComplete(count)
        }
    }

    fun clearAndRescan(onComplete: (Int) -> Unit = {}) {
        viewModelScope.launch {
            val count = com.example.smartexpensetracker.data.parser.SmsInboxScanner.clearAndRescanBankSms(getApplication())
            onComplete(count)
        }
    }

    fun exportCsv(): String = DataExporter.exportToCsv(allTransactions.value)

    fun exportJson(): String = DataExporter.exportToJson(allTransactions.value)

    fun exportBackupJson(): String = BackupManager.createBackupJson(allTransactions.value)

    fun generatePdfStatement(
        customerName: String = userProfile.value.userName,
        mobileNumber: String = userProfile.value.mobileNumber,
        accountNumber: String = userProfile.value.accountNumber,
        bankName: String = userProfile.value.bankName,
        periodText: String = "August 2026",
        password: String = "",
        onComplete: (java.io.File) -> Unit
    ) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val balance = latestBankBalance.value ?: 353.35
            val pass = password.ifBlank {
                com.example.smartexpensetracker.data.local.UserProfileManager.computePassword(
                    userName = customerName,
                    mobileNumber = mobileNumber,
                    accountNumber = accountNumber,
                    pattern = userProfile.value.passwordPattern,
                    customPass = userProfile.value.customPassword
                )
            }
            val info = com.example.smartexpensetracker.data.export.BankStatementGenerator.StatementCustomerInfo(
                customerName = customerName,
                accountNumber = accountNumber,
                bankName = bankName,
                mobileNumber = mobileNumber,
                periodText = periodText,
                closingBalance = balance,
                password = pass
            )
            val file = com.example.smartexpensetracker.data.export.BankStatementGenerator.generatePdfStatement(
                context = getApplication(),
                transactions = allTransactions.value,
                info = info
            )
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                onComplete(file)
            }
        }
    }

    fun generateExcelStatement(
        customerName: String = userProfile.value.userName,
        mobileNumber: String = userProfile.value.mobileNumber,
        accountNumber: String = userProfile.value.accountNumber,
        bankName: String = userProfile.value.bankName,
        periodText: String = "August 2026",
        password: String = "",
        onComplete: (java.io.File) -> Unit
    ) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val balance = latestBankBalance.value ?: 353.35
            val pass = password.ifBlank {
                com.example.smartexpensetracker.data.local.UserProfileManager.computePassword(
                    userName = customerName,
                    mobileNumber = mobileNumber,
                    accountNumber = accountNumber,
                    pattern = userProfile.value.passwordPattern,
                    customPass = userProfile.value.customPassword
                )
            }
            val info = com.example.smartexpensetracker.data.export.BankStatementGenerator.StatementCustomerInfo(
                customerName = customerName,
                accountNumber = accountNumber,
                bankName = bankName,
                mobileNumber = mobileNumber,
                periodText = periodText,
                closingBalance = balance,
                password = pass
            )
            val file = com.example.smartexpensetracker.data.export.BankStatementGenerator.generateExcelStatement(
                context = getApplication(),
                transactions = allTransactions.value,
                info = info
            )
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                onComplete(file)
            }
        }
    }

    fun restoreBackup(json: String) {
        val restored = BackupManager.restoreBackupJson(json)
        if (restored != null) {
            viewModelScope.launch {
                for (t in restored) {
                    repository.insertTransaction(t)
                }
            }
        }
    }

    fun getStartOfDay(ms: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = ms
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun getEndOfDay(ms: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = ms
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return cal.timeInMillis
    }

    fun isSameDay(ts1: Long, ts2: Long): Boolean {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date(ts1)) == sdf.format(Date(ts2))
    }

    private fun isCurrentMonth(ts: Long): Boolean {
        return monthSdf.format(Date(ts)) == currentMonthKey
    }

    private fun isToday(ts: Long): Boolean {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date(ts)) == sdf.format(Date())
    }

    private fun isThisWeek(ts: Long): Boolean {
        val cal = Calendar.getInstance()
        val currentWeek = cal.get(Calendar.WEEK_OF_YEAR)
        cal.timeInMillis = ts
        return cal.get(Calendar.WEEK_OF_YEAR) == currentWeek
    }
}
