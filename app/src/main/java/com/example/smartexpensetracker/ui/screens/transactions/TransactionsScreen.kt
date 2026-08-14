package com.example.smartexpensetracker.ui.screens.transactions

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartexpensetracker.data.local.entity.TransactionEntity
import com.example.smartexpensetracker.ui.components.AddEditTransactionDialog
import com.example.smartexpensetracker.ui.components.CategoryChip
import com.example.smartexpensetracker.ui.components.TransactionItem
import com.example.smartexpensetracker.ui.theme.PrimaryEmerald
import com.example.smartexpensetracker.ui.theme.SuccessGreen
import com.example.smartexpensetracker.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    viewModel: MainViewModel,
    onAddTransactionClick: () -> Unit
) {
    val filteredTxns by viewModel.filteredTransactions.collectAsState()
    val duplicates by viewModel.duplicateTransactions.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCat by viewModel.selectedCategoryFilter.collectAsState()
    val selectedType by viewModel.selectedTypeFilter.collectAsState()
    val selectedPeriod by viewModel.selectedPeriodFilter.collectAsState()
    val selectedCustomDate by viewModel.selectedCustomDateMillis.collectAsState()
    val selectedStartDate by viewModel.selectedStartDateMillis.collectAsState()
    val selectedEndDate by viewModel.selectedEndDateMillis.collectAsState()

    var transactionToEdit by remember { mutableStateOf<TransactionEntity?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDateRangePicker by remember { mutableStateOf(false) }

    val dateRangePickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = selectedStartDate,
        initialSelectedEndDateMillis = selectedEndDate
    )

    val dateFormat = remember { SimpleDateFormat("dd MMM", Locale.getDefault()) }
    val fullDateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    val rangeLabel = remember(selectedPeriod, selectedStartDate, selectedEndDate) {
        if (selectedPeriod == "DateRange" && selectedStartDate != null) {
            if (selectedEndDate != null && selectedEndDate != selectedStartDate) {
                "${dateFormat.format(Date(selectedStartDate!!))} - ${dateFormat.format(Date(selectedEndDate!!))}"
            } else {
                fullDateFormat.format(Date(selectedStartDate!!))
            }
        } else {
            "Between Dates 📅"
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddTransactionClick,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Transactions",
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.searchQuery.value = it },
                placeholder = { Text("Search merchant, payee, note...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 1. Time / Period Filter Chips with Range Picker
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = selectedPeriod == "Month",
                    onClick = {
                        viewModel.selectedPeriodFilter.value = "Month"
                        viewModel.selectedCustomDateMillis.value = null
                        viewModel.selectedStartDateMillis.value = null
                        viewModel.selectedEndDateMillis.value = null
                    },
                    label = { Text("This Month") },
                    modifier = Modifier.padding(end = 6.dp)
                )

                FilterChip(
                    selected = selectedPeriod == "Week",
                    onClick = {
                        viewModel.selectedPeriodFilter.value = "Week"
                        viewModel.selectedCustomDateMillis.value = null
                        viewModel.selectedStartDateMillis.value = null
                        viewModel.selectedEndDateMillis.value = null
                    },
                    label = { Text("This Week") },
                    modifier = Modifier.padding(end = 6.dp)
                )

                FilterChip(
                    selected = selectedPeriod == "Today",
                    onClick = {
                        viewModel.selectedPeriodFilter.value = "Today"
                        viewModel.selectedCustomDateMillis.value = null
                        viewModel.selectedStartDateMillis.value = null
                        viewModel.selectedEndDateMillis.value = null
                    },
                    label = { Text("Today") },
                    modifier = Modifier.padding(end = 6.dp)
                )

                // Date Range (Between Dates) Chip
                FilterChip(
                    selected = selectedPeriod == "DateRange",
                    onClick = {
                        showDateRangePicker = true
                    },
                    leadingIcon = {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    trailingIcon = {
                        if (selectedPeriod == "DateRange") {
                            IconButton(
                                onClick = {
                                    viewModel.selectedPeriodFilter.value = "Month"
                                    viewModel.selectedStartDateMillis.value = null
                                    viewModel.selectedEndDateMillis.value = null
                                },
                                modifier = Modifier.size(16.dp)
                            ) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear Range", modifier = Modifier.size(12.dp))
                            }
                        }
                    },
                    label = { Text(rangeLabel, fontWeight = if (selectedPeriod == "DateRange") FontWeight.Bold else FontWeight.Normal) },
                    modifier = Modifier.padding(end = 6.dp)
                )

                FilterChip(
                    selected = selectedPeriod == "All",
                    onClick = {
                        viewModel.selectedPeriodFilter.value = "All"
                        viewModel.selectedCustomDateMillis.value = null
                        viewModel.selectedStartDateMillis.value = null
                        viewModel.selectedEndDateMillis.value = null
                    },
                    label = { Text("All Time") },
                    modifier = Modifier.padding(end = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 2. Type & Category Filters Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = selectedType == null,
                    onClick = { viewModel.selectedTypeFilter.value = null },
                    label = { Text("All Types") },
                    modifier = Modifier.padding(end = 6.dp)
                )

                FilterChip(
                    selected = selectedType == "Expense",
                    onClick = { viewModel.selectedTypeFilter.value = if (selectedType == "Expense") null else "Expense" },
                    label = { Text("Expenses (Debited 🔴)") },
                    modifier = Modifier.padding(end = 6.dp)
                )

                FilterChip(
                    selected = selectedType == "Income",
                    onClick = { viewModel.selectedTypeFilter.value = if (selectedType == "Income") null else "Income" },
                    label = { Text("Income (Credited 🟢)") },
                    modifier = Modifier.padding(end = 6.dp)
                )

                categories.forEach { cat ->
                    CategoryChip(
                        name = cat.name,
                        emoji = cat.emoji,
                        isSelected = selectedCat == cat.name,
                        onSelect = { viewModel.selectedCategoryFilter.value = if (selectedCat == cat.name) null else cat.name }
                    )
                }
            }

            // Duplicates Warning Banner if present
            if (duplicates.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${duplicates.size} duplicate notification(s) suppressed",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        TextButton(
                            onClick = { viewModel.deleteDuplicates() },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("Clear", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Total Header Count & Net Sum
            val totalSum = filteredTxns.sumOf { if (it.isIncome) it.amount else -it.amount }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${filteredTxns.size} Transactions",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Text(
                    text = "Net: ${if (totalSum >= 0) "+" else "-"}₹${String.format(Locale.getDefault(), "%,.2f", Math.abs(totalSum))}",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (totalSum >= 0) SuccessGreen else MaterialTheme.colorScheme.error
                    )
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Transactions List
            if (filteredTxns.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No transactions found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Try clearing filters or scanning your SMS inbox",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filteredTxns, key = { it.id }) { txn ->
                        TransactionItem(
                            transaction = txn,
                            onEdit = {
                                transactionToEdit = it
                                showEditDialog = true
                            },
                            onDelete = { viewModel.deleteTransaction(it) }
                        )
                    }
                }
            }
        }
    }

    // Material 3 Date Range Picker Dialog (Select Between Dates on Calendar)
    if (showDateRangePicker) {
        DatePickerDialog(
            onDismissRequest = { showDateRangePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val start = dateRangePickerState.selectedStartDateMillis
                        val end = dateRangePickerState.selectedEndDateMillis
                        if (start != null) {
                            viewModel.selectedStartDateMillis.value = start
                            viewModel.selectedEndDateMillis.value = end
                            viewModel.selectedPeriodFilter.value = "DateRange"
                        }
                        showDateRangePicker = false
                    }
                ) {
                    Text("Apply Range")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDateRangePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DateRangePicker(
                state = dateRangePickerState,
                title = {
                    Text(
                        "Select Date Range",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 24.dp, top = 16.dp)
                    )
                },
                headline = {
                    Text(
                        "Tap start date and end date",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.padding(start = 24.dp, bottom = 12.dp)
                    )
                }
            )
        }
    }

    if (showEditDialog) {
        AddEditTransactionDialog(
            transactionToEdit = transactionToEdit,
            categories = categories,
            onDismiss = { showEditDialog = false },
            onSave = { amt, isInc, merch, cat, method, note ->
                val updated = transactionToEdit?.copy(
                    amount = amt,
                    isIncome = isInc,
                    merchant = merch,
                    category = cat,
                    paymentMethod = method,
                    note = note
                ) ?: TransactionEntity(
                    amount = amt,
                    isIncome = isInc,
                    merchant = merch,
                    category = cat,
                    paymentMethod = method,
                    source = "Manual",
                    note = note
                )
                if (transactionToEdit != null) {
                    viewModel.updateTransaction(updated)
                } else {
                    viewModel.addTransaction(amt, isInc, merch, cat, method, note)
                }
                showEditDialog = false
            }
        )
    }
}
