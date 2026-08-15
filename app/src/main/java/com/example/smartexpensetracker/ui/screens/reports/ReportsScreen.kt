package com.example.smartexpensetracker.ui.screens.reports

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartexpensetracker.data.export.BankStatementGenerator
import com.example.smartexpensetracker.ui.theme.PrimaryEmerald
import com.example.smartexpensetracker.ui.theme.SuccessGreen
import com.example.smartexpensetracker.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val monthlySummary by viewModel.monthlySummary.collectAsState()
    val totalExpense by viewModel.totalMonthlyExpenses.collectAsState()
    val totalIncome by viewModel.totalMonthlyIncome.collectAsState()
    val categoryMap by viewModel.categoryExpenseMap.collectAsState()
    val budgetEntity by viewModel.monthlyBudget.collectAsState()
    val latestBalance by viewModel.latestBankBalance.collectAsState()
    val latestAccountNum by viewModel.latestAccountNumber.collectAsState()

    val currency = "₹"
    val netSavings = totalIncome - totalExpense

    val highestCategoryEntry = categoryMap.maxByOrNull { it.value }
    val highestCategory = highestCategoryEntry?.key ?: "N/A"
    val highestCategoryAmt = highestCategoryEntry?.value ?: 0.0

    val userProfile by viewModel.userProfile.collectAsState()

    // Bank Statement Settings
    var customerName by remember(userProfile.userName) { mutableStateOf(userProfile.userName) }
    var mobileNumber by remember(userProfile.mobileNumber) { mutableStateOf(userProfile.mobileNumber) }
    var accountNumber by remember(userProfile.accountNumber) { mutableStateOf(userProfile.accountNumber) }
    var bankName by remember(userProfile.bankName) { mutableStateOf(userProfile.bankName) }
    var passwordPattern by remember(userProfile.passwordPattern) { mutableStateOf(userProfile.passwordPattern) }
    var customPassInput by remember(userProfile.customPassword) { mutableStateOf(userProfile.customPassword) }
    var selectedPeriod by remember { mutableStateOf("August 2026") }
    var isGeneratingPdf by remember { mutableStateOf(false) }
    var isGeneratingExcel by remember { mutableStateOf(false) }
    var showStatementDateRangePicker by remember { mutableStateOf(false) }
    var showBankSelector by remember { mutableStateOf(false) }

    val statementDateRangePickerState = rememberDateRangePickerState()
    val fullDateFormat = remember { SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()) }

    val passwordHint = remember(customerName, mobileNumber, accountNumber, passwordPattern, customPassInput) {
        BankStatementGenerator.getPasswordHint(
            customerName = customerName,
            mobileNumber = mobileNumber,
            accountNumber = accountNumber,
            pattern = passwordPattern,
            customPassword = customPassInput
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Reports & Statements",
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold)
            )
            IconButton(onClick = { viewModel.generateMonthlySummary(viewModel.currentMonthKey) }) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh Report")
            }
        }

        // Summary Card
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("REPORT SUMMARY (${viewModel.currentMonthKey})", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Total Income", style = MaterialTheme.typography.labelMedium)
                        Text("$currency${totalIncome.toInt()}", style = MaterialTheme.typography.headlineMedium.copy(color = SuccessGreen, fontWeight = FontWeight.Bold))
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Total Expenses", style = MaterialTheme.typography.labelMedium)
                        Text("$currency${totalExpense.toInt()}", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Net Savings", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    Text("$currency${netSavings.toInt()}", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, color = if (netSavings >= 0) SuccessGreen else MaterialTheme.colorScheme.error))
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Highest Category", style = MaterialTheme.typography.bodyLarge)
                    Text("$highestCategory ($currency${highestCategoryAmt.toInt()})", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Average Daily Spend", style = MaterialTheme.typography.bodyLarge)
                    Text("$currency${(totalExpense / 30).toInt()}", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
                }
            }
        }

        // --- OFFICIAL BANK & UPI ACCOUNT STATEMENT GENERATOR ---
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccountBalance, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Official Bank & UPI Statement", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("PDF â€¢ Excel â€¢ Password Protected", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    }
                }

                Text(
                    text = "Generates an authentic bank statement featuring your app logo, account details, debits, credits, and running balance exactly like official bank statements.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )

                // Editable Customer Info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Account Holder Details", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    TextButton(
                        onClick = {
                            val detected = viewModel.autoDetectUserProfile()
                            customerName = detected.userName
                            mobileNumber = detected.mobileNumber
                            accountNumber = detected.accountNumber
                            bankName = detected.bankName
                            Toast.makeText(context, "âš¡ Auto-detected: ${detected.bankName} (${detected.accountNumber})", Toast.LENGTH_SHORT).show()
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp), tint = PrimaryEmerald)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Auto-Detect from SMS", style = MaterialTheme.typography.labelMedium, color = PrimaryEmerald)
                    }
                }

                OutlinedTextField(
                    value = customerName,
                    onValueChange = { customerName = it },
                    label = { Text("Account Holder Name") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = mobileNumber,
                        onValueChange = { mobileNumber = it },
                        label = { Text("Mobile Number") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = accountNumber,
                        onValueChange = { accountNumber = it },
                        label = { Text("Account No.") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = bankName,
                    onValueChange = { bankName = it },
                    label = { Text("Bank / Institution Name") },
                    leadingIcon = { Icon(Icons.Default.AccountBalance, contentDescription = null, tint = PrimaryEmerald) },
                    trailingIcon = {
                        IconButton(onClick = { showBankSelector = true }) {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Bank")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showBankSelector = true },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                // Period Selector with Calendar Range Picker
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Statement Period:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    OutlinedButton(
                        onClick = { showStatementDateRangePicker = true },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(selectedPeriod, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    }
                }

                // Password Protection Rules Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = PrimaryEmerald, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("PDF Passcode: $passwordHint", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = PrimaryEmerald)
                        }

                        Text(
                            text = "Choose your preferred password protection formula for statement PDF:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )

                        // Password Pattern Chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilterChip(
                                selected = passwordPattern == "NAME_ACCOUNT",
                                onClick = { passwordPattern = "NAME_ACCOUNT" },
                                label = { Text("Name + A/C (e.g. ${customerName.filter{it.isLetter()}.take(4).uppercase()}${accountNumber.filter{it.isDigit()}.takeLast(4)})") },
                                modifier = Modifier.padding(end = 6.dp)
                            )
                            FilterChip(
                                selected = passwordPattern == "NAME_PHONE",
                                onClick = { passwordPattern = "NAME_PHONE" },
                                label = { Text("Name + Mobile (e.g. ${customerName.filter{it.isLetter()}.take(4).uppercase()}${mobileNumber.filter{it.isDigit()}.takeLast(4)})") },
                                modifier = Modifier.padding(end = 6.dp)
                            )
                            FilterChip(
                                selected = passwordPattern == "NAME_LAST_PHONE_FIRST",
                                onClick = { passwordPattern = "NAME_LAST_PHONE_FIRST" },
                                label = { Text("Name (End) + Mobile (Start)") },
                                modifier = Modifier.padding(end = 6.dp)
                            )
                            FilterChip(
                                selected = passwordPattern == "CUSTOM",
                                onClick = { passwordPattern = "CUSTOM" },
                                label = { Text("Custom Passcode") },
                                modifier = Modifier.padding(end = 6.dp)
                            )
                        }

                        if (passwordPattern == "CUSTOM") {
                            OutlinedTextField(
                                value = customPassInput,
                                onValueChange = { customPassInput = it },
                                label = { Text("Custom Statement Password") },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // Action Buttons
                Button(
                    onClick = {
                        isGeneratingPdf = true
                        viewModel.updateUserProfile(
                            com.example.smartexpensetracker.data.local.UserProfile(
                                userName = customerName,
                                mobileNumber = mobileNumber,
                                accountNumber = accountNumber,
                                bankName = bankName,
                                isLoggedIn = true,
                                isPhoneVerified = true,
                                passwordPattern = passwordPattern,
                                customPassword = customPassInput
                            )
                        )
                        viewModel.generatePdfStatement(
                            customerName = customerName,
                            mobileNumber = mobileNumber,
                            accountNumber = accountNumber,
                            bankName = bankName,
                            periodText = selectedPeriod,
                            password = passwordHint
                        ) { file ->
                            isGeneratingPdf = false
                            Toast.makeText(context, "PDF Statement generated with passcode: $passwordHint", Toast.LENGTH_SHORT).show()
                            BankStatementGenerator.shareStatementFile(context, file, "application/pdf", "Bank Account Statement ($customerName)")
                        }
                    },
                    enabled = !isGeneratingPdf,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isGeneratingPdf) "Generating Official PDF..." else "Generate & Share PDF Statement")
                }

                OutlinedButton(
                    onClick = {
                        isGeneratingExcel = true
                        viewModel.updateUserProfile(
                            com.example.smartexpensetracker.data.local.UserProfile(
                                userName = customerName,
                                mobileNumber = mobileNumber,
                                accountNumber = accountNumber,
                                bankName = bankName,
                                isLoggedIn = true,
                                isPhoneVerified = true,
                                passwordPattern = passwordPattern,
                                customPassword = customPassInput
                            )
                        )
                        viewModel.generateExcelStatement(
                            customerName = customerName,
                            mobileNumber = mobileNumber,
                            accountNumber = accountNumber,
                            bankName = bankName,
                            periodText = selectedPeriod,
                            password = passwordHint
                        ) { file ->
                            isGeneratingExcel = false
                            Toast.makeText(context, "Excel (.csv) Statement generated!", Toast.LENGTH_SHORT).show()
                            BankStatementGenerator.shareStatementFile(context, file, "text/csv", "Bank Account Statement ($customerName)")
                        }
                    },
                    enabled = !isGeneratingExcel,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.TableChart, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isGeneratingExcel) "Generating Excel..." else "Export Excel (.csv) Statement")
                }

                OutlinedButton(
                    onClick = {
                        viewModel.syncAllToGoogleSheets { count ->
                            Toast.makeText(context, "âœ… Synced $count transaction(s) to Google Sheet!", Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sync Live Statement to Google Sheets")
                }
            }
        }

        // Export Raw Data Actions
        Text("Raw Data Exports", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = {
                        val csv = viewModel.exportCsv()
                        Toast.makeText(context, "Exported ${csv.lines().size - 1} transactions to CSV", Toast.LENGTH_LONG).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export Raw CSV Data")
                }

                OutlinedButton(
                    onClick = {
                        val json = viewModel.exportJson()
                        Toast.makeText(context, "Exported JSON data format", Toast.LENGTH_LONG).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export Raw JSON Data")
                }
            }
        }

        Spacer(modifier = Modifier.height(60.dp))
    }

    if (showStatementDateRangePicker) {
        DatePickerDialog(
            onDismissRequest = { showStatementDateRangePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val start = statementDateRangePickerState.selectedStartDateMillis
                        val end = statementDateRangePickerState.selectedEndDateMillis
                        if (start != null) {
                            selectedPeriod = if (end != null && end != start) {
                                "${fullDateFormat.format(Date(start))} - ${fullDateFormat.format(Date(end))}"
                            } else {
                                fullDateFormat.format(Date(start))
                            }
                        }
                        showStatementDateRangePicker = false
                    }
                ) {
                    Text("Apply Range")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStatementDateRangePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DateRangePicker(
                state = statementDateRangePickerState,
                title = {
                    Text(
                        "Statement Date Range",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 24.dp, top = 16.dp)
                    )
                },
                headline = {
                    Text(
                        "Tap start date and end date for statement",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.padding(start = 24.dp, bottom = 12.dp)
                    )
                }
            )
        }
    }

    if (showBankSelector) {
        com.example.smartexpensetracker.ui.components.BankSelectionDialog(
            selectedBankName = bankName,
            onDismiss = { showBankSelector = false },
            onBankSelected = { selected ->
                bankName = selected
                showBankSelector = false
            }
        )
    }
}
