package com.example.smartexpensetracker.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.smartexpensetracker.data.local.entity.TransactionEntity
import com.example.smartexpensetracker.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TransactionDetailDialog(
    transaction: TransactionEntity,
    categoryEmoji: String = "🏷️",
    currencySymbol: String = "₹",
    onDismiss: () -> Unit,
    onEdit: (TransactionEntity) -> Unit,
    onDelete: (TransactionEntity) -> Unit
) {
    val context = LocalContext.current
    val fullDateFormat = remember { SimpleDateFormat("EEEE, dd MMMM yyyy • hh:mm:ss a", Locale.getDefault()) }
    val fullDateStr = remember(transaction.timestamp) { fullDateFormat.format(Date(transaction.timestamp)) }
    val isIncome = transaction.isIncome
    val primaryColor = if (isIncome) SuccessGreen else PrimaryEmerald
    val amountColor = if (isIncome) SuccessGreen else MaterialTheme.colorScheme.onSurface

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(28.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Header Row with Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = (if (isIncome) SuccessGreen else MaterialTheme.colorScheme.primary).copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = if (isIncome) "✓ INCOME RECEIVED" else "EXPENSE DEBIT",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isIncome) SuccessGreen else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Big Logo / Avatar
                MerchantLogo(
                    merchant = transaction.merchant,
                    categoryEmoji = categoryEmoji,
                    isIncome = isIncome,
                    size = 64.dp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Payee / Merchant Name
                Text(
                    text = transaction.merchant,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold, fontSize = 20.sp),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Big Amount
                Text(
                    text = "${if (isIncome) "+" else "-"}$currencySymbol${String.format("%,.2f", transaction.amount)}",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 32.sp
                    ),
                    color = amountColor
                )

                if (transaction.accountBalance != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                    ) {
                        Text(
                            text = "Running Balance: $currencySymbol${String.format("%,.2f", transaction.accountBalance)}",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                Spacer(modifier = Modifier.height(16.dp))

                // Detailed Key-Value Grid
                DetailRow(icon = Icons.Default.Category, label = "Category", value = "$categoryEmoji ${transaction.category}")
                DetailRow(icon = Icons.Default.Schedule, label = "Date & Time", value = fullDateStr)
                DetailRow(
                    icon = Icons.Default.AccountBalance,
                    label = "Bank & Account",
                    value = "${transaction.bankName ?: "Tamilnad Mercantile Bank (TMB)"} ${if (!transaction.accountNumber.isNullOrEmpty()) "(${transaction.accountNumber})" else ""}"
                )
                DetailRow(icon = Icons.Default.Payment, label = "Payment Mode", value = transaction.paymentMethod)

                if (!transaction.refId.isNullOrEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Tag, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Ref / Txn ID", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = transaction.refId,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("RefId", transaction.refId))
                                    Toast.makeText(context, "Copied Ref ID: ${transaction.refId}", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }

                if (transaction.note.isNotBlank()) {
                    DetailRow(icon = Icons.Default.Notes, label = "Personal Note", value = transaction.note)
                }

                // Raw SMS Card
                if (transaction.rawText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Sms, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ORIGINAL BANK SMS",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = transaction.rawText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Share Receipt Button
                    OutlinedButton(
                        onClick = {
                            val shareBody = """
                                💳 SpendWise Transaction Receipt
                                --------------------------------
                                Amount: ${if (isIncome) "+" else "-"}$currencySymbol${transaction.amount}
                                Payee/Sender: ${transaction.merchant}
                                Category: ${transaction.category}
                                Date: $fullDateStr
                                Method: ${transaction.paymentMethod}
                                ${if (!transaction.refId.isNullOrEmpty()) "Ref No: ${transaction.refId}" else ""}
                                --------------------------------
                                Tracked privately with SpendWise
                            """.trimIndent()
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, shareBody)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Transaction Receipt"))
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share")
                    }

                    // Edit Button
                    Button(
                        onClick = {
                            onDismiss()
                            onEdit(transaction)
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Edit")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Delete Button
                TextButton(
                    onClick = {
                        onDismiss()
                        onDelete(transaction)
                        Toast.makeText(context, "Transaction deleted", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = DangerRed, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Delete Transaction", color = DangerRed)
                }
            }
        }
    }
}

@Composable
private fun DetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2
        )
    }
}