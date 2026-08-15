package com.example.smartexpensetracker.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartexpensetracker.data.local.entity.CategoryEntity
import com.example.smartexpensetracker.data.local.entity.TransactionEntity
import com.example.smartexpensetracker.ui.theme.DangerRed
import com.example.smartexpensetracker.ui.theme.PrimaryEmerald

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionDialog(
    transactionToEdit: TransactionEntity? = null,
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onSave: (amount: Double, isIncome: Boolean, merchant: String, category: String, paymentMethod: String, note: String) -> Unit
) {
    var amountText by remember { mutableStateOf(transactionToEdit?.let { String.format("%.2f", it.amount) } ?: "") }
    var isIncome by remember { mutableStateOf(transactionToEdit?.isIncome ?: false) }
    var merchant by remember { mutableStateOf(transactionToEdit?.merchant ?: "") }
    var selectedCategory by remember { mutableStateOf(transactionToEdit?.category ?: (if (isIncome) "Income" else "Food")) }
    var paymentMethod by remember { mutableStateOf(transactionToEdit?.paymentMethod ?: "UPI") }
    var note by remember { mutableStateOf(transactionToEdit?.note ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (transactionToEdit == null) "Add Transaction" else "Edit Transaction",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Income vs Expense Segmented Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilterChip(
                        selected = !isIncome,
                        onClick = {
                            isIncome = false
                            if (selectedCategory == "Income" || selectedCategory == "Salary") {
                                selectedCategory = "Food"
                            }
                        },
                        leadingIcon = {
                            Icon(Icons.Default.ArrowUpward, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        label = { Text("Expense (Spent)", fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = DangerRed.copy(alpha = 0.2f),
                            selectedLabelColor = DangerRed,
                            selectedLeadingIconColor = DangerRed
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    FilterChip(
                        selected = isIncome,
                        onClick = {
                            isIncome = true
                            selectedCategory = "Income"
                        },
                        leadingIcon = {
                            Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        label = { Text("Income (Received)", fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryEmerald.copy(alpha = 0.2f),
                            selectedLabelColor = PrimaryEmerald,
                            selectedLeadingIconColor = PrimaryEmerald
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Amount Field
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount (\u20B9)") },
                    placeholder = { Text("e.g. 250.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )

                // Merchant / Payee Field
                OutlinedTextField(
                    value = merchant,
                    onValueChange = { merchant = it },
                    label = { Text(if (isIncome) "Received From / Sender" else "Paid To / Merchant") },
                    placeholder = { Text(if (isIncome) "e.g. Company Salary, Friend" else "e.g. Swiggy, Sri Murugan Stores") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )

                // Category Selection (Smooth horizontal scroll, never squeezed)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Select Category", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categories.forEach { cat ->
                            CategoryChip(
                                name = cat.name,
                                emoji = cat.emoji,
                                isSelected = selectedCategory.equals(cat.name, ignoreCase = true),
                                onSelect = { selectedCategory = cat.name }
                            )
                        }
                    }
                }

                // Payment Method (Smooth horizontal scroll)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Payment Method", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("UPI", "Card", "NetBanking", "Cash", "IMPS", "NEFT").forEach { method ->
                            FilterChip(
                                selected = paymentMethod.equals(method, ignoreCase = true),
                                onClick = { paymentMethod = method },
                                label = { Text(method) }
                            )
                        }
                    }
                }

                // Note Field
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note / Description (Optional)") },
                    placeholder = { Text("e.g. Grocery items for dinner") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    if (amt > 0 && merchant.isNotBlank()) {
                        onSave(amt, isIncome, merchant.trim(), selectedCategory, paymentMethod, note.trim())
                    }
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save Transaction", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
