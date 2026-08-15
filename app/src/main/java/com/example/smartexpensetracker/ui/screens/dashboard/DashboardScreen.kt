package com.example.smartexpensetracker.ui.screens.dashboard

import java.util.Locale
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartexpensetracker.R
import com.example.smartexpensetracker.data.model.BankDirectory
import com.example.smartexpensetracker.data.local.entity.TransactionEntity
import com.example.smartexpensetracker.ui.components.*
import com.example.smartexpensetracker.ui.theme.*
import com.example.smartexpensetracker.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onNavigateToTransactions: () -> Unit,
    onNavigateToReports: () -> Unit,
    onAddTransactionClick: () -> Unit,
    onNavigateToProfile: () -> Unit = {},
    onNavigateToHelp: () -> Unit = {},
    onScanSmsClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val allTransactions by viewModel.allTransactions.collectAsState()
    val totalExpense by viewModel.totalMonthlyExpenses.collectAsState()
    val totalIncome by viewModel.totalMonthlyIncome.collectAsState()
    val todaySpent by viewModel.todaySpending.collectAsState()
    val weekSpent by viewModel.weekSpending.collectAsState()
    val latestBankBal by viewModel.latestBankBalance.collectAsState()
    val latestAccNo by viewModel.latestAccountNumber.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") } // "All", "Expense", "Income"
    var showBalance by remember { mutableStateOf(true) }
    var isSyncingSheet by remember { mutableStateOf(false) }
    var selectedTxnForDetails by remember { mutableStateOf<TransactionEntity?>(null) }
    var txnToEdit by remember { mutableStateOf<TransactionEntity?>(null) }

    val budgetEntity by viewModel.monthlyBudget.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val categoryMap by viewModel.categoryExpenseMap.collectAsState()
    val dailyTrend by viewModel.dailyTrendData.collectAsState()

    val budget = budgetEntity?.totalBudget ?: 0.0
    val currency = "\u20B9"
    val netBalance = totalIncome - totalExpense
    val usedPct = if (budget > 0) ((totalExpense / budget) * 100).toInt().coerceIn(0, 100) else 0

    val matchedBank = remember(userProfile.bankName) {
        BankDirectory.getBankByCodeOrName(userProfile.bankName)
    }

    val filteredTransactions = remember(allTransactions, searchQuery, selectedFilter) {
        allTransactions.filter { txn ->
            val matchesSearch = if (searchQuery.isBlank()) true else {
                txn.merchant.contains(searchQuery, ignoreCase = true) ||
                txn.category.contains(searchQuery, ignoreCase = true) ||
                (txn.bankName?.contains(searchQuery, ignoreCase = true) == true) ||
                (txn.accountNumber?.contains(searchQuery, ignoreCase = true) == true)
            }
            val matchesType = when (selectedFilter) {
                "Expense" -> !txn.isIncome
                "Income" -> txn.isIncome
                else -> true
            }
            matchesSearch && matchesType
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddTransactionClick,
                containerColor = PrimaryEmerald,
                contentColor = Color.Black,
                shape = RoundedCornerShape(18.dp),
                elevation = FloatingActionButtonDefaults.elevation(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Expense", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // ==========================================
            // 1. TOP HEADER & SEARCH BAR
            // ==========================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Profile Initials / Avatar
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(PrimaryEmerald),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (userProfile.userName.isNotBlank()) userProfile.userName.take(2).uppercase() else "LO",
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Search Bar Field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search Payees, Banks...", fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search", modifier = Modifier.size(18.dp))
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryEmerald,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Help/Info Icon
                IconButton(
                    onClick = onScanSmsClick,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                        contentDescription = "Help",
                        tint = PrimaryEmerald
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ==========================================
            // 2. QUICK ACTIONS ROW
            // ==========================================
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Service 1: Passbook / History
                    QuickServiceTile(
                        icon = Icons.AutoMirrored.Filled.ReceiptLong,
                        label = "Passbook",
                        color = Color(0xFF38BDF8),
                        onClick = onNavigateToTransactions
                    )

                    // Service 2: Google Sheets Live Sync
                    QuickServiceTile(
                        icon = if (isSyncingSheet) Icons.Default.HourglassTop else Icons.Default.CloudSync,
                        label = if (isSyncingSheet) "Syncing..." else "Sheet Sync",
                        color = PrimaryEmerald,
                        onClick = {
                            coroutineScope.launch {
                                isSyncingSheet = true
                                val synced = com.example.smartexpensetracker.data.export.GoogleSheetsSyncManager.syncAllTransactionsToSheet(context, allTransactions)
                                isSyncingSheet = false
                                Toast.makeText(context, "Synced $synced transactions to Google Sheet!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )

                    // Service 3: Bank Statements
                    QuickServiceTile(
                        icon = Icons.Default.PictureAsPdf,
                        label = "Statements",
                        color = Color(0xFFFFB703),
                        onClick = onNavigateToReports
                    )

                    // Service 4: Scan SMS
                    QuickServiceTile(
                        icon = Icons.Default.Sms,
                        label = "Scan SMS",
                        color = Color(0xFFFB5607),
                        onClick = {
                            coroutineScope.launch {
                                viewModel.scanSmsInbox()
                                Toast.makeText(context, "Scanned SMS inbox for bank transactions", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ==========================================
            // 3. BANK ACCOUNT & LIVE BALANCE HERO CARD
            // ==========================================
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF0F2027),
                                    Color(0xFF203A43),
                                    Color(0xFF2C5364)
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        // Bank Header Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(PrimaryEmerald),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("🏛️", fontSize = 18.sp)
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column {
                                    Text(
                                        text = if (userProfile.bankName.isNotBlank()) userProfile.bankName else "Tamilnad Mercantile Bank (TMB)",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White
                                    )
                                    Text(
                                        text = "A/c No: ${latestAccNo ?: (if (userProfile.accountNumber.isNotBlank()) userProfile.accountNumber else "XXXX5779")}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.7f)
                                    )
                                }
                            }

                            IconButton(onClick = { showBalance = !showBalance }) {
                                Icon(
                                    imageVector = if (showBalance) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle Balance",
                                    tint = PrimaryEmeraldLight
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "AVAILABLE BALANCE",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                            color = Color.White.copy(alpha = 0.6f)
                        )

                        Text(
                            text = if (showBalance) {
                                if (latestBankBal != null) "$currency${String.format(Locale.US, "%,.2f", latestBankBal)}" else "$currency${String.format(Locale.US, "%,.2f", (353.35).coerceAtLeast(netBalance))}"
                            } else "\u2022\u2022\u2022\u2022\u2022\u2022\u2022\u2022",
                            style = MaterialTheme.typography.headlineLarge.copy(fontSize = 32.sp, fontWeight = FontWeight.Black),
                            color = PrimaryEmeraldLight
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Dual Row for Income vs Expense
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.Black.copy(alpha = 0.25f))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Income Pill
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(PrimaryEmerald.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = PrimaryEmeraldLight, modifier = Modifier.size(16.dp))
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Income", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                                    Text(
                                        text = "+$currency${String.format(Locale.US, "%,.2f", totalIncome)}",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = PrimaryEmeraldLight
                                    )
                                }
                            }

                            // Expense Pill
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(DangerRed.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = DangerRed, modifier = Modifier.size(16.dp))
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Spent", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                                    Text(
                                        text = "-$currency${String.format(Locale.US, "%,.2f", totalExpense)}",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = DangerRed
                                    )
                                }
                            }
                        }

                        if (budget > 0) {
                            Spacer(modifier = Modifier.height(12.dp))
                            LinearProgressIndicator(
                                progress = { (usedPct / 100f).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = if (usedPct > 90) DangerRed else PrimaryEmerald,
                                trackColor = Color.White.copy(alpha = 0.2f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Budget: $currency${String.format(Locale.US, "%,d", budget.toInt())} ($usedPct% Used)",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.65f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ==========================================
            // 4. METRIC CARDS ROW
            // ==========================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    title = "Today's Spent",
                    value = "$currency${String.format(Locale.US, "%,.2f", todaySpent)}",
                    icon = Icons.Default.ShoppingCart,
                    modifier = Modifier.weight(1f),
                    accentColor = WarningYellow
                )
                MetricCard(
                    title = "This Week",
                    value = "$currency${String.format(Locale.US, "%,.2f", weekSpent)}",
                    icon = Icons.Default.CalendarMonth,
                    modifier = Modifier.weight(1f),
                    accentColor = Color(0xFF38BDF8)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Spending by Category Chart
            if (categoryMap.isNotEmpty()) {
                CategoryPieChart(
                    categoryData = categoryMap,
                    currencySymbol = currency
                )
            }

            // Daily Spending Trend
            if (dailyTrend.isNotEmpty()) {
                SpendingTrendChart(
                    dailyAmounts = dailyTrend
                )
            }

            // ==========================================
            // 5. TRANSACTIONS LEDGER & FILTER
            // ==========================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (searchQuery.isNotBlank()) "Search Results (${filteredTransactions.size})" else "Recent Transactions",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                Text(
                    text = "View All →",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = PrimaryEmerald,
                    modifier = Modifier.clickable(onClick = onNavigateToTransactions)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Filter Chips (All, Spends, Income)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == "All",
                    onClick = { selectedFilter = "All" },
                    label = { Text("All") }
                )
                FilterChip(
                    selected = selectedFilter == "Expense",
                    onClick = { selectedFilter = "Expense" },
                    label = { Text("Expenses") }
                )
                FilterChip(
                    selected = selectedFilter == "Income",
                    onClick = { selectedFilter = "Income" },
                    label = { Text("Income") }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (filteredTransactions.isEmpty()) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (searchQuery.isNotBlank()) "No transaction matching \"$searchQuery\"" else "No transactions yet. Scan SMS to get started!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    filteredTransactions.take(8).forEach { txn ->
                        val emoji = categories.find { it.name.equals(txn.category, ignoreCase = true) }?.emoji ?: "🏷️"
                        TransactionItem(
                            transaction = txn,
                            categoryEmoji = emoji,
                            currencySymbol = currency,
                            onClick = { selectedTxnForDetails = it },
                            onEdit = { txnToEdit = it },
                            onDelete = { viewModel.deleteTransaction(it) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(70.dp))
        }
    }

    if (selectedTxnForDetails != null) {
        val emoji = categories.find { it.name.equals(selectedTxnForDetails?.category, ignoreCase = true) }?.emoji ?: "🏷️"
        TransactionDetailDialog(
            transaction = selectedTxnForDetails!!,
            categoryEmoji = emoji,
            currencySymbol = currency,
            onDismiss = { selectedTxnForDetails = null },
            onEdit = {
                val t = it
                selectedTxnForDetails = null
                txnToEdit = t
            },
            onDelete = {
                viewModel.deleteTransaction(it)
                selectedTxnForDetails = null
            }
        )
    }

    if (txnToEdit != null) {
        AddEditTransactionDialog(
            transactionToEdit = txnToEdit,
            categories = categories,
            onDismiss = { txnToEdit = null },
            onSave = { amt, isInc, merch, cat, method, note ->
                val updated = txnToEdit!!.copy(
                    amount = amt,
                    isIncome = isInc,
                    merchant = merch,
                    category = cat,
                    paymentMethod = method,
                    note = note
                )
                viewModel.updateTransaction(updated)
                txnToEdit = null
            }
        )
    }
}

@Composable
fun QuickServiceTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            fontSize = 11.sp
        )
    }
}