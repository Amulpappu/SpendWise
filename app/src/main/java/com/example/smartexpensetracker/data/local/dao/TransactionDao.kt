package com.example.smartexpensetracker.data.local.dao

import com.example.smartexpensetracker.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

interface TransactionDao {
    fun getAllTransactionsFlow(): Flow<List<TransactionEntity>>
    fun getDuplicateTransactionsFlow(): Flow<List<TransactionEntity>>
    suspend fun getAllTransactionsSync(): List<TransactionEntity>
    suspend fun findRecentTransactions(startTime: Long, endTime: Long): List<TransactionEntity>
    suspend fun findByRefId(refId: String): TransactionEntity?
    suspend fun insertTransaction(transaction: TransactionEntity): Long
    suspend fun updateTransaction(transaction: TransactionEntity)
    suspend fun deleteTransaction(transaction: TransactionEntity)
    suspend fun deleteDuplicates()
}
