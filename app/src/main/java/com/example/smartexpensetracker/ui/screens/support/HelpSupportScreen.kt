package com.example.smartexpensetracker.ui.screens.support

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartexpensetracker.ui.theme.PrimaryEmerald
import com.example.smartexpensetracker.ui.theme.SuccessGreen
import com.example.smartexpensetracker.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpSupportScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("24x7 Help & Support", fontWeight = FontWeight.Bold)
                        Text("Paytm-Style Intelligent Assistance", style = MaterialTheme.typography.bodySmall, color = PrimaryEmerald)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 18.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(2.dp))

            // 1. Support Hero Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(PrimaryEmerald.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.SupportAgent, contentDescription = null, tint = PrimaryEmerald, modifier = Modifier.size(30.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("How can we help you?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Quick diagnostic tools & answers for SpendWise", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    }
                }
            }

            // 2. System Diagnostics Section
            Text("System Diagnostics", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            // Diagnostic 1: SMS Permission
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Sms, contentDescription = null, tint = PrimaryEmerald)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("SMS Auto-Read Access", fontWeight = FontWeight.SemiBold)
                            Text("Required for offline debit/credit detection", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                    }
                    Button(
                        onClick = {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Check", fontSize = 12.sp, color = MaterialTheme.colorScheme.surface)
                    }
                }
            }

            // 2. Frequently Asked Questions (FAQs)
            Text("Frequently Asked Questions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            FaqAccordion(
                question = "How does SpendWise parse bank SMS offline?",
                answer = "SpendWise contains a built-in on-device regular expression engine that scans debit/credit SMS alerts from 25+ Indian banks locally. No personal SMS or banking data is ever sent to external cloud servers."
            )

            FaqAccordion(
                question = "What is the password for my PDF bank statements?",
                answer = "By default, your password is the First 4 letters of your Name in CAPITAL + Last 4 digits of your Account Number (e.g., LOHI5779 for Lohith, account ending in 5779). You can customize this anytime in Profile & Bank Details."
            )

            FaqAccordion(
                question = "Is my financial data private and offline?",
                answer = "Yes! SpendWise is designed strictly for your personal use. All banking data, transactions, and budgets are stored 100% locally on your phone in SQLite. Zero cloud tracking, no third-party servers."
            )

            FaqAccordion(
                question = "How do I backup and restore my transactions?",
                answer = "Go to Settings -> Backup & Restore to export your entire database as a secure JSON backup file anytime."
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun FaqAccordion(question: String, answer: String) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = question,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = PrimaryEmerald
                )
            }
            if (expanded) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = answer,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    lineHeight = 18.sp
                )
            }
        }
    }
}
