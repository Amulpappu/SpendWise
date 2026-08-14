package com.example.smartexpensetracker.data.local.dao

import com.example.smartexpensetracker.data.local.entity.*
import kotlinx.coroutines.flow.Flow

interface CategoryDao {
    fun getAllCategoriesFlow(): Flow<List<CategoryEntity>>
    suspend fun getAllCategoriesSync(): List<CategoryEntity>
    suspend fun insertCategory(category: CategoryEntity)
    suspend fun insertCategories(categories: List<CategoryEntity>)
    suspend fun deleteCategory(category: CategoryEntity)
}

interface BudgetDao {
    fun getMonthlyBudgetFlow(monthKey: String): Flow<BudgetEntity?>
    fun getMonthlyBudgetSync(monthKey: String): BudgetEntity?
    suspend fun setMonthlyBudget(budget: BudgetEntity)

    fun getCategoryBudgetsFlow(monthKey: String): Flow<List<CategoryBudgetEntity>>
    fun getCategoryBudgetsSync(monthKey: String): List<CategoryBudgetEntity>
    suspend fun setCategoryBudget(budget: CategoryBudgetEntity)
}

interface MerchantRuleDao {
    fun getAllRulesFlow(): Flow<List<MerchantRuleEntity>>
    suspend fun getAllRulesSync(): List<MerchantRuleEntity>
    suspend fun findRuleForMerchant(merchant: String): MerchantRuleEntity?
    suspend fun insertRule(rule: MerchantRuleEntity)
    suspend fun deleteRule(rule: MerchantRuleEntity)
}

interface RecurringExpenseDao {
    fun getActiveRecurringFlow(): Flow<List<RecurringExpenseEntity>>
    suspend fun insertRecurring(expense: RecurringExpenseEntity): Long
    suspend fun updateRecurring(expense: RecurringExpenseEntity)
    suspend fun deleteRecurring(expense: RecurringExpenseEntity)
}

interface MonthlySummaryDao {
    fun getSummaryFlow(monthKey: String): Flow<MonthlySummaryEntity?>
    suspend fun insertSummary(summary: MonthlySummaryEntity)
}
