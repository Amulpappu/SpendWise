package com.example.smartexpensetracker.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartexpensetracker.data.local.entity.TransactionEntity
import com.example.smartexpensetracker.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TransactionItem(
    transaction: TransactionEntity,
    categoryEmoji: String = "🏷️",
    currencySymbol: String = "₹",
    onClick: (TransactionEntity) -> Unit = {},
    onEdit: (TransactionEntity) -> Unit = {},
    onDelete: (TransactionEntity) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val cleanCurrency = if (currencySymbol.contains("Ã") || currencySymbol.contains("") || currencySymbol.isBlank()) "₹" else currencySymbol
    val dateFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }
    val dateStr = remember(transaction.timestamp) { dateFormat.format(Date(transaction.timestamp)) }
    val isIncome = transaction.isIncome
    val amountColor = if (isIncome) SuccessGreen else MaterialTheme.colorScheme.onSurface

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f), RoundedCornerShape(18.dp))
            .clickable { onClick(transaction) }
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MerchantLogo(
                merchant = transaction.merchant,
                categoryEmoji = categoryEmoji,
                isIncome = isIncome,
                size = 46.dp
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Merchant / Payee Info Column (takes flexible space with padding)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = transaction.merchant,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (transaction.isDuplicate) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = WarningYellow.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "Duplicate",
                                style = MaterialTheme.typography.labelSmall,
                                color = WarningYellow,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = transaction.category,
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                    )
                    Text(
                        text = " • ",
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                    )
                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                    )
                }
                if (transaction.note.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "📝 ${transaction.note}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Amount and Balance Column
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.wrapContentWidth()
            ) {
                Text(
                    text = "${if (isIncome) "+" else "-"}$cleanCurrency${String.format(Locale.US, "%,.2f", transaction.amount)}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp
                    ),
                    color = amountColor
                )
                if (transaction.accountBalance != null) {
                    Text(
                        text = "Bal: $cleanCurrency${String.format(Locale.US, "%,.2f", transaction.accountBalance)}",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = transaction.paymentMethod,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    IconButton(onClick = { onEdit(transaction) }, modifier = Modifier.size(24.dp)) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(13.dp))
                    }
                    IconButton(onClick = { onDelete(transaction) }, modifier = Modifier.size(24.dp)) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = DangerRed, modifier = Modifier.size(13.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryChip(
    name: String,
    emoji: String,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    FilterChip(
        selected = isSelected,
        onClick = onSelect,
        label = { Text("$emoji $name", style = MaterialTheme.typography.labelMedium) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.padding(end = 6.dp)
    )
}