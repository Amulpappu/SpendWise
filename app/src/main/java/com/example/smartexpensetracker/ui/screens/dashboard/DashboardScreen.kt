package com.example.smartexpensetracker.ui.screens.dashboard

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
import com.example.smartexpensetracker.ui.components.AddEditTransactionDialog
import com.example.smartexpensetracker.ui.components.CategoryPieChart
import com.example.smartexpensetracker.ui.components.MetricCard
import com.example.smartexpensetracker.ui.components.SpendingTrendChart
import com.example.smartexpensetracker.ui.components.TransactionDetailDialog
import com.example.smartexpensetracker.ui.components.TransactionItem
import com.example.smartexpensetracker.ui.theme.*
import com.example.smartexpensetracker.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onAddTransactionClick: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToHelp: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    onNavigateToReports: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val totalExpense by viewModel.totalMonthlyExpenses.collectAsState()
    val totalIncome by viewModel.totalMonthlyIncome.collectAsState()
    val allTransactions by viewModel.allTransactions.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val budgetEntity by viewModel.monthlyBudget.collectAsState()
    val categoryMap by viewModel.categoryExpenseMap.collectAsState()
    val dailyTrend by viewModel.dailyTrendData.collectAsState()
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

    val budget = budgetEntity?.totalBudget ?: 0.0
    val currency = budgetEntity?.currencySymbol ?: "ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¹"
    val netBalance = totalIncome - totalExpense
    val usedPct = if (budget > 0) ((totalExpense / budget) * 100).toInt().coerceIn(0, 100) else 0

    val matchedBank = remember(userProfile.bankName) {
        BankDirectory.getBankByCodeOrName(userProfile.bankName)
    }

    // Filter transactions by Search Query & Type
    val filteredTransactions = remember(allTransactions, searchQuery, selectedFilter) {
        allTransactions.filter { txn ->
            val matchesQuery = searchQuery.isBlank() ||
                    txn.merchant.contains(searchQuery, ignoreCase = true) ||
                    txn.category.contains(searchQuery, ignoreCase = true) ||
                    txn.amount.toString().contains(searchQuery) ||
                    txn.note.contains(searchQuery, ignoreCase = true) ||
                    txn.paymentMethod.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (selectedFilter) {
                "Expense" -> !txn.isIncome
                "Income" -> txn.isIncome
                else -> true
            }

            matchesQuery && matchesFilter
        }
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddTransactionClick,
                icon = { Icon(Icons.Default.Add, contentDescription = "Add") },
                text = { Text("Add Expense", fontWeight = FontWeight.Bold) },
                containerColor = PrimaryEmerald,
                contentColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // ==========================================
            // 1. PAYTM-STYLE TOP APP HEADER WITH SEARCH & HELP
            // ==========================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Profile Avatar with Verified Shield (Tap -> Profile)
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clickable(onClick = onNavigateToProfile)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(PrimaryEmerald),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = userProfile.userName.take(2).uppercase().ifBlank { "ME" },
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.surface
                        )
                    }
                    // Verified Badge overlay
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .align(Alignment.BottomEnd)
                            .clip(CircleShape)
                            .background(SuccessGreen),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Verified",
                            tint = Color.White,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Paytm-style Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search Payees, Banks...", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = PrimaryEmerald, modifier = Modifier.size(20.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    shape = RoundedCornerShape(24.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        focusedBorderColor = PrimaryEmerald,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                )

                Spacer(modifier = Modifier.width(10.dp))

                // Paytm 24x7 Help & Support Shortcut Icon
                IconButton(
                    onClick = onNavigateToHelp,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(
                        imageVector = Icons.Default.HelpOutline,
                        contentDescription = "24x7 Help & Support",
                        tint = PrimaryEmerald
                    )
                }
            }

            // ==========================================
            // 2. PAYTM-STYLE QUICK ACTION SERVICES GRID
            // ==========================================
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Service 1: Passbook / History
                    QuickServiceTile(
                        icon = Icons.Default.ReceiptLong,
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
                                Toast.makeText(context, "ÃƒÂ¢Ã…â€œÃ¢â‚¬Â¦ Synced $synced historical transactions to Google Sheet!", Toast.LENGTH_SHORT).show()
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

            // ==========================================
            // 3. PAYTM BANK ACCOUNT & LIVE BALANCE HERO CARD
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
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFF0F2027), // Deep Dark Teal
                                    Color(0xFF203A43), // Mid Slate Teal
                                    Color(0xFF2C5364)  // Slate Blue
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        // Bank Header Row with Official Vector Logo
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (matchedBank?.logoResId != null) {
                                    Image(
                                        painter = painterResource(matchedBank.logoResId),
                                        contentDescription = matchedBank.name,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(PrimaryEmerald),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = userProfile.bankName.take(3).uppercase().ifBlank { "BNK" },
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            color = Color.White
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column {
                                    Text(
                                        text = userProfile.bankName.ifBlank { "Primary Bank Account" },
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White
                                    )
                                    Text(
                                        text = "A/c No: ${latestAccNo ?: userProfile.accountNumber.ifBlank { "XXXX 305779" }}",
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
                                if (latestBankBal != null) "$currency${String.format("%,.2f", latestBankBal)}" else "$currency${String.format("%,.2f", (353.35).coerceAtLeast(netBalance))}"
                            } else "ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¢",
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
                            // Income Block
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(PrimaryEmerald.copy(alpha = 0.25f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.ArrowDownward, contentDescription = "Income", tint = PrimaryEmeraldLight, modifier = Modifier.size(18.dp))
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Income", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                                    Text(
                                        text = "+$currency${String.format("%,.2f", totalIncome)}",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = PrimaryEmeraldLight
                                    )
                                }
                            }

                            // Expense Block
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(DangerRed.copy(alpha = 0.25f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.ArrowUpward, contentDescription = "Expense", tint = DangerRed, modifier = Modifier.size(18.dp))
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Spent", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                                    Text(
                                        text = "-$currency${String.format("%,.2f", totalExpense)}",
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
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = if (usedPct > 90) DangerRed else PrimaryEmeraldLight,
                                trackColor = Color.White.copy(alpha = 0.2f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Budget: $currency${String.format("%,d", budget.toInt())} ($usedPct% Used)",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.65f)
                            )
                        }
                    }
                }
            }

            // ==========================================
            // 4. QUICK METRICS OVERVIEW
            // ==========================================
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard(
                    title = "Today's Spent",
                    value = "$currency${String.format("%,.2f", todaySpent)}",
                    icon = Icons.Default.ShoppingCart,
                    modifier = Modifier.weight(1f),
                    accentColor = WarningYellow
                )
                MetricCard(
                    title = "This Week",
                    value = "$currency${String.format("%,.2f", weekSpent)}",
                    icon = Icons.Default.CalendarMonth,
                    modifier = Modifier.weight(1f),
                    accentColor = Color(0xFF38BDF8)
                )
            }

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
            // 5. PAYTM-STYLE TRANSACTIONS LEDGER & FILTER
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
                    text = "View All ÃƒÂ¢Ã¢â‚¬Â Ã¢â‚¬â„¢",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = PrimaryEmerald,
                    modifier = Modifier.clickable(onClick = onNavigateToTransactions)
                )
            }

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
                        val emoji = categories.find { it.name.equals(txn.category, ignoreCase = true) }?.emoji ?: "Ã°Å¸ÂÂ·Ã¯Â¸Â"
                        TransactionItem(
                            transaction = txn,
                            categoryEmoji = emoji,
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
        val emoji = categories.find { it.name.equals(selectedTxnForDetails?.category, ignoreCase = true) }?.emoji ?: "ðŸ·ï¸"
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