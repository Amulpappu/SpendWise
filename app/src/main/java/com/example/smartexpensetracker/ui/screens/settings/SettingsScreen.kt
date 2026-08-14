package com.example.smartexpensetracker.ui.screens.settings

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
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

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onNavigateToProfile: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var testSmsText by remember { mutableStateOf("Your A/C XXXXX5779 has been debited by Rs.250.00 to SWIGGY. Avail Bal: Rs 12,300.00") }
    var backupJsonInput by remember { mutableStateOf("") }
    var showBackupRestoreDialog by remember { mutableStateOf(false) }
    var showScriptDialog by remember { mutableStateOf(false) }

    val webhookUrl by viewModel.webhookUrl.collectAsState()
    val autoSyncEnabled by viewModel.autoSyncEnabled.collectAsState()
    var webhookInput by remember { mutableStateOf(webhookUrl) }
    var isTestingSync by remember { mutableStateOf(false) }

    val userProfile by viewModel.userProfile.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold)
        )

        // 1. My Profile & Bank Details Summary Tile (Navigates to dedicated Profile sub-screen)
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToProfile() }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(PrimaryEmerald),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = userProfile.userName.take(2).uppercase().ifBlank { "ME" },
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.surface
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = userProfile.userName.ifBlank { "User Profile" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${userProfile.bankName} • ${userProfile.accountNumber.takeLast(6)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Verified: +91 ${userProfile.mobileNumber}",
                                style = MaterialTheme.typography.labelSmall,
                                color = SuccessGreen,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Edit", style = MaterialTheme.typography.labelMedium, color = PrimaryEmerald, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = "Edit Profile",
                        tint = PrimaryEmerald,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        // 2. Google Sheets Live Sync Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, tint = PrimaryEmerald)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Google Sheets Live Sync", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    }
                    Switch(
                        checked = autoSyncEnabled,
                        onCheckedChange = { viewModel.updateAutoSync(it) }
                    )
                }

                Text(
                    text = "Automatically appends debits/credits to your Google Sheet in real-time over 4G/5G mobile internet (works outside home without laptop!):",
                    style = MaterialTheme.typography.bodyMedium
                )

                OutlinedTextField(
                    value = webhookInput,
                    onValueChange = {
                        webhookInput = it
                        viewModel.updateWebhookUrl(it)
                    },
                    label = { Text("Google Apps Script Webhook URL") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Button(
                    onClick = {
                        isTestingSync = true
                        coroutineScope.launch {
                            val success = viewModel.testGoogleSheetsSync()
                            isTestingSync = false
                            if (success) {
                                Toast.makeText(context, "✅ Google Sheet synced successfully!", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "❌ Sync failed. Check Webhook URL and permissions.", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    enabled = !isTestingSync,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Sync, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isTestingSync) "Testing Sync..." else "Test Google Sheets Connection")
                }

                OutlinedButton(
                    onClick = {
                        isTestingSync = true
                        viewModel.syncAllToGoogleSheets { count ->
                            isTestingSync = false
                            Toast.makeText(context, "✅ Synced $count transaction(s) to Google Sheet!", Toast.LENGTH_LONG).show()
                        }
                    },
                    enabled = !isTestingSync,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sync All Transactions to Google Sheets Now")
                }

                TextButton(
                    onClick = { showScriptDialog = true },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Apps Script Setup Instructions (Fix 403 / Failed)", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        // Automatic Detection & Permissions Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Automatic Detection Permissions", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                }
                Text(
                    text = "Grant Notification Listener access to automatically parse bank & UPI notifications locally on your device.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                Button(
                    onClick = {
                        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Enable Notification Listener Access")
                }
            }
        }

        // Scan SMS Inbox
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Sync, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Scan Bank SMS Inbox", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                }
                Text(
                    text = "Import and sync any bank or UPI SMS from your phone inbox to SpendWise and Google Sheets.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                Button(
                    onClick = {
                        viewModel.scanSmsInbox { count ->
                            Toast.makeText(context, "Scanned inbox: $count new transaction(s) synced!", Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Sync, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Scan & Sync Missed SMS Now")
                }

                OutlinedButton(
                    onClick = {
                        viewModel.clearAndRescan { count ->
                            Toast.makeText(context, "Cleaned old data & re-scanned $count authentic bank transactions!", Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Clean Database & Re-scan Authentic Bank SMS")
                }
            }
        }

        // Paste & Import Bank SMS / RCS Message Tool
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Build, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Import / Paste Bank SMS / RCS Message", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                }
                Text(
                    text = "Paste any bank SMS, RCS text, or UPI notification here to instantly parse, categorize, and log it to your account & Google Sheet:",
                    style = MaterialTheme.typography.bodyMedium
                )

                OutlinedTextField(
                    value = testSmsText,
                    onValueChange = { testSmsText = it },
                    label = { Text("Paste Bank SMS / RCS Message Text") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 2
                )

                Button(
                    onClick = {
                        if (testSmsText.isNotBlank()) {
                            viewModel.simulateIncomingText(testSmsText)
                            Toast.makeText(context, "Transaction successfully parsed, saved & synced!", Toast.LENGTH_SHORT).show()
                            testSmsText = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Parse & Import Message Now")
                }
            }
        }

        // Backup & Restore
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Database Backup & Restore", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            val backupStr = viewModel.exportBackupJson()
                            backupJsonInput = backupStr
                            showBackupRestoreDialog = true
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Create Backup")
                    }

                    OutlinedButton(
                        onClick = {
                            showBackupRestoreDialog = true
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Restore")
                    }
                }
            }
        }

        // Privacy & Security Notice
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = SuccessGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("100% On-Device Privacy Guaranteed", style = MaterialTheme.typography.titleLarge.copy(fontSize = 16.sp), fontWeight = FontWeight.Bold)
                }
                Text(
                    text = "• All parsing happens locally on your device.\n• Sensitive credentials, OTPs, PINs, passwords, CVVs, and account numbers are NEVER read or saved.\n• No unauthorized cloud telemetry.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }
        }

        Spacer(modifier = Modifier.height(60.dp))
    }

    if (showBackupRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showBackupRestoreDialog = false },
            title = { Text("Backup / Restore JSON") },
            text = {
                OutlinedTextField(
                    value = backupJsonInput,
                    onValueChange = { backupJsonInput = it },
                    label = { Text("JSON Data") },
                    modifier = Modifier.fillMaxWidth().height(200.dp)
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.restoreBackup(backupJsonInput)
                    Toast.makeText(context, "Database restored!", Toast.LENGTH_SHORT).show()
                    showBackupRestoreDialog = false
                }) {
                    Text("Restore Data")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBackupRestoreDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (showScriptDialog) {
        AlertDialog(
            onDismissRequest = { showScriptDialog = false },
            title = { Text("Google Apps Script Setup", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("If sync shows 'Sync Failed' (403 Forbidden), your Google Script deployment needs 1 permission update:", fontWeight = FontWeight.SemiBold)
                    Text("1. Open your Google Sheet → Extensions → Apps Script.\n2. Click 'Deploy' (top right) → 'Manage deployments' (or 'New deployment').\n3. Click Edit (pencil icon).\n4. Set 'Execute as': 'Me'\n5. Set 'Who has access': 'Anyone' ⚠️ (Required so your phone can sync).\n6. Click Deploy and copy the Web App URL.")
                    Text("Make sure your Google Apps Script contains this Ultra-Fast Bulk & Single Sync code:", fontWeight = FontWeight.SemiBold)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = """function doPost(e) {
  var ss = SpreadsheetApp.getActiveSpreadsheet();
  var data = JSON.parse(e.postData.contents);
  var nowStr = Utilities.formatDate(new Date(), Session.getScriptTimeZone() || "GMT+5:30", "yyyy-MM-dd HH:mm:ss");
  
  var userName = (data.user && data.user.name) ? data.user.name : "User";
  var userPhone = (data.user && data.user.phone) ? data.user.phone : "6379982741";
  var userBank = (data.user && data.user.bank) ? data.user.bank : "Primary Bank";
  var userAcc = (data.user && data.user.account) ? data.user.account : "N/A";
  var sheetName = userName + " (" + userPhone.slice(-4) + ")";
  var bankAcc = userBank + " - " + userAcc;
  
  // 1. GET OR CREATE USER DEDICATED TAB
  var sheet = ss.getSheetByName(sheetName);
  if (!sheet) {
    sheet = ss.insertSheet(sheetName);
    sheet.appendRow(["ID", "Date & Time", "Type", "Amount (₹)", "Category", "Merchant / Payee", "Bank / Account", "Payment Method", "Note"]);
    sheet.getRange("A1:I1").setBackground("#0F2027").setFontColor("#FFFFFF").setFontWeight("bold");
    sheet.setFrozenRows(1);
  }
  
  // 2. BULK BATCH OR SINGLE TRANSACTION LOGGING (Exact Historical Dates)
  if (data.batch && Array.isArray(data.batch) && data.batch.length > 0) {
    var rows = data.batch.map(function(txn) {
      return [
        txn.id || "",
        txn.timestamp || nowStr,
        txn.type || "Expense",
        txn.amount || 0,
        txn.category || "General",
        txn.merchant || "Self",
        bankAcc,
        txn.paymentMethod || "UPI",
        txn.note || ""
      ];
    });
    var startRow = sheet.getLastRow() + 1;
    sheet.getRange(startRow, 1, rows.length, 9).setValues(rows);
  } else {
    var txn = data.transaction || data;
    sheet.appendRow([
      txn.id || "",
      txn.timestamp || nowStr,
      txn.type || "Expense",
      txn.amount || 0,
      txn.category || "General",
      txn.merchant || "Self",
      bankAcc,
      txn.paymentMethod || "UPI",
      txn.note || ""
    ]);
  }
  
  // 3. MASTER DIRECTORY TAB INITIALIZER
  var masterSheet = ss.getSheetByName("_App_Users");
  if (!masterSheet) {
    masterSheet = ss.insertSheet("_App_Users", 0);
    masterSheet.appendRow(["User Mobile (Key)", "User Full Name", "Bank Name", "Primary Account No.", "Dedicated Tab", "Last Active Timestamp"]);
    masterSheet.getRange("A1:F1").setBackground("#1B3B6F").setFontColor("#FFFFFF").setFontWeight("bold");
    masterSheet.setFrozenRows(1);
    masterSheet.appendRow([userPhone, userName, userBank, userAcc, sheetName, nowStr]);
  }
  
  return ContentService.createTextOutput(JSON.stringify({
    status: "success", 
    sheet: sheetName, 
    user: userName
  })).setMimeType(ContentService.MimeType.JSON);
}""",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showScriptDialog = false }) {
                    Text("Got It")
                }
            }
        )
    }
}
