package com.example.smartexpensetracker.ui.screens.budget

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartexpensetracker.ui.theme.DangerRed
import com.example.smartexpensetracker.ui.theme.WarningYellow
import com.example.smartexpensetracker.ui.viewmodel.MainViewModel

@Composable
fun BudgetScreen(viewModel: MainViewModel) {
    val budgetEntity by viewModel.monthlyBudget.collectAsState()
    val categoryBudgets by viewModel.categoryBudgets.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val categoryMap by viewModel.categoryExpenseMap.collectAsState()
    val totalExpense by viewModel.totalMonthlyExpenses.collectAsState()

    var showBudgetDialog by remember { mutableStateOf(false) }
    var budgetInput by remember { mutableStateOf((budgetEntity?.totalBudget ?: 10000.0).toString()) }

    var selectedCatForBudget by remember { mutableStateOf<String?>(null) }
    var catBudgetInput by remember { mutableStateOf("") }

    val totalBudget = budgetEntity?.totalBudget ?: 10000.0
    val currency = "\u20B9"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Monthly Budget",
            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold)
        )

        // Main Monthly Budget Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Overall Monthly Budget", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Button(onClick = { showBudgetDialog = true }) {
                        Text("Edit")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "$currency${totalBudget.toInt()}",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, fontSize = 28.sp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Spent: $currency${totalExpense.toInt()}", style = MaterialTheme.typography.bodyMedium)
                    Text("Remaining: $currency${(totalBudget - totalExpense).toInt().coerceAtLeast(0)}", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        // Category Budgets Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Category Budgets", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        }

        // List of Categories with Budget configuration
        categories.forEach { cat ->
            val catSpent = categoryMap[cat.name] ?: 0.0
            val catBudgetEntity = categoryBudgets.find { it.categoryName.equals(cat.name, ignoreCase = true) }
            val catBudget = catBudgetEntity?.allocatedAmount ?: 2000.0
            val pct = if (catBudget > 0) ((catSpent / catBudget) * 100).toInt() else 0

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(cat.emoji, fontSize = 22.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(cat.name, style = MaterialTheme.typography.titleLarge.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold))
                        }
                        TextButton(onClick = {
                            selectedCatForBudget = cat.name
                            catBudgetInput = catBudget.toInt().toString()
                        }) {
                            Text("Set limit")
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("$currency${catSpent.toInt()} / $currency${catBudget.toInt()}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                        Text(
                            text = "$pct%",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = when {
                                pct >= 100 -> DangerRed
                                pct >= 75 -> WarningYellow
                                else -> MaterialTheme.colorScheme.primary
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = { (pct / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                        color = when {
                            pct >= 100 -> DangerRed
                            pct >= 75 -> WarningYellow
                            else -> MaterialTheme.colorScheme.primary
                        },
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(60.dp))
    }

    // Set Total Budget Dialog
    if (showBudgetDialog) {
        AlertDialog(
            onDismissRequest = { showBudgetDialog = false },
            title = { Text("Set Monthly Budget") },
            text = {
                OutlinedTextField(
                    value = budgetInput,
                    onValueChange = { budgetInput = it },
                    label = { Text("Total Monthly Budget ($currency)") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    val amt = budgetInput.toDoubleOrNull() ?: 10000.0
                    viewModel.setMonthlyBudget(amt)
                    showBudgetDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBudgetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Set Category Budget Dialog
    if (selectedCatForBudget != null) {
        AlertDialog(
            onDismissRequest = { selectedCatForBudget = null },
            title = { Text("Category Budget: ${selectedCatForBudget}") },
            text = {
                OutlinedTextField(
                    value = catBudgetInput,
                    onValueChange = { catBudgetInput = it },
                    label = { Text("Allocated Budget ($currency)") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    val amt = catBudgetInput.toDoubleOrNull() ?: 2000.0
                    viewModel.setCategoryBudget(selectedCatForBudget!!, amt)
                    selectedCatForBudget = null
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedCatForBudget = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
