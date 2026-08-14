package com.example.smartexpensetracker.ui.screens.profile

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartexpensetracker.data.model.BankDirectory
import com.example.smartexpensetracker.ui.components.BankSelectionDialog
import com.example.smartexpensetracker.ui.theme.PrimaryEmerald
import com.example.smartexpensetracker.ui.theme.SuccessGreen
import com.example.smartexpensetracker.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: MainViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val userProfile by viewModel.userProfile.collectAsState()

    var userName by remember(userProfile.userName) { mutableStateOf(userProfile.userName) }
    var mobileNumber by remember(userProfile.mobileNumber) { mutableStateOf(userProfile.mobileNumber) }
    var accountNumber by remember(userProfile.accountNumber) { mutableStateOf(userProfile.accountNumber) }
    var bankName by remember(userProfile.bankName) { mutableStateOf(userProfile.bankName) }
    var passwordPattern by remember(userProfile.passwordPattern) { mutableStateOf(userProfile.passwordPattern) }
    var customPassInput by remember(userProfile.customPassword) { mutableStateOf(userProfile.customPassword) }
    var showBankSelector by remember { mutableStateOf(false) }

    val currentPasswordPreview = remember(userName, mobileNumber, accountNumber, passwordPattern, customPassInput) {
        com.example.smartexpensetracker.data.local.UserProfileManager.computePassword(
            userName = userName,
            mobileNumber = mobileNumber,
            accountNumber = accountNumber,
            pattern = passwordPattern,
            customPass = customPassInput
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile & Bank Details", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val detected = viewModel.autoDetectUserProfile()
                        userName = detected.userName
                        mobileNumber = detected.mobileNumber
                        accountNumber = detected.accountNumber
                        bankName = detected.bankName
                        Toast.makeText(context, "⚡ Auto-detected: ${detected.bankName} (${detected.accountNumber})", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.AutoFixHigh, contentDescription = "Auto Detect", tint = PrimaryEmerald)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Profile Header Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(PrimaryEmerald),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = userName.take(2).uppercase().ifBlank { "ME" },
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.surface
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = userName.ifBlank { "User Account" },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Verified: +91 $mobileNumber",
                                style = MaterialTheme.typography.bodySmall,
                                color = SuccessGreen,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Section 1: Account Holder Details
            Text("Personal & Contact Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            OutlinedTextField(
                value = userName,
                onValueChange = { userName = it },
                label = { Text("Account Holder Full Name") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryEmerald) },
                shape = RoundedCornerShape(14.dp),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = mobileNumber,
                onValueChange = { mobileNumber = it },
                label = { Text("Registered Mobile Number") },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = PrimaryEmerald) },
                shape = RoundedCornerShape(14.dp),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Section 2: Bank & Account Details
            Text("Banking Information", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            val matchedBank = BankDirectory.getBankByCodeOrName(bankName)
            OutlinedTextField(
                value = bankName,
                onValueChange = { bankName = it },
                label = { Text("Primary Bank Name") },
                leadingIcon = {
                    if (matchedBank?.logoResId != null) {
                        Image(
                            painter = painterResource(matchedBank.logoResId),
                            contentDescription = bankName,
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                        )
                    } else {
                        Icon(Icons.Default.AccountBalance, contentDescription = null, tint = PrimaryEmerald)
                    }
                },
                trailingIcon = {
                    IconButton(onClick = { showBankSelector = true }) {
                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Bank")
                    }
                },
                shape = RoundedCornerShape(14.dp),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showBankSelector = true }
            )

            OutlinedTextField(
                value = accountNumber,
                onValueChange = { accountNumber = it },
                label = { Text("Primary Bank Account Number") },
                leadingIcon = { Icon(Icons.Default.CreditCard, contentDescription = null, tint = PrimaryEmerald) },
                shape = RoundedCornerShape(14.dp),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Auto-detect helper button
            OutlinedButton(
                onClick = {
                    val detected = viewModel.autoDetectUserProfile()
                    userName = detected.userName
                    mobileNumber = detected.mobileNumber
                    accountNumber = detected.accountNumber
                    bankName = detected.bankName
                    Toast.makeText(context, "⚡ Auto-detected: ${detected.bankName} (${detected.accountNumber})", Toast.LENGTH_SHORT).show()
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = PrimaryEmerald)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Auto-Detect Bank & A/C from SMS History", fontWeight = FontWeight.SemiBold)
            }

            // Section 3: Statement PDF Password Formula
            Text("Statement PDF Password Protection", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = PrimaryEmerald, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Current PDF Passcode: $currentPasswordPreview",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryEmerald
                        )
                    }

                    Text(
                        text = "Choose your statement password formula:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = passwordPattern == "NAME_ACCOUNT",
                            onClick = { passwordPattern = "NAME_ACCOUNT" },
                            label = { Text("Name + A/C ($currentPasswordPreview)") },
                            modifier = Modifier.padding(end = 6.dp)
                        )
                        FilterChip(
                            selected = passwordPattern == "NAME_PHONE",
                            onClick = { passwordPattern = "NAME_PHONE" },
                            label = { Text("Name + Mobile") },
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
                            label = { Text("Enter Custom Statement Password") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Save Button
            Button(
                onClick = {
                    viewModel.updateUserProfile(
                        com.example.smartexpensetracker.data.local.UserProfile(
                            userName = userName,
                            mobileNumber = mobileNumber,
                            accountNumber = accountNumber,
                            bankName = bankName,
                            isLoggedIn = true,
                            isPhoneVerified = true,
                            passwordPattern = passwordPattern,
                            customPassword = customPassInput
                        )
                    )
                    Toast.makeText(context, "✅ Profile & Bank details saved successfully!", Toast.LENGTH_SHORT).show()
                    onBackClick()
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Profile & Bank Details", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            // Switch Account / Logout Button
            OutlinedButton(
                onClick = {
                    viewModel.logout()
                    Toast.makeText(context, "Logged out. Please verify mobile number.", Toast.LENGTH_SHORT).show()
                    onBackClick()
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Switch Account / Logout", fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showBankSelector) {
        BankSelectionDialog(
            selectedBankName = bankName,
            onDismiss = { showBankSelector = false },
            onBankSelected = { selected ->
                bankName = selected
                showBankSelector = false
            }
        )
    }
}
