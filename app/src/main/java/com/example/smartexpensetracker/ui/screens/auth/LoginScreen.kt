package com.example.smartexpensetracker.ui.screens.auth

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartexpensetracker.R
import com.example.smartexpensetracker.ui.theme.PrimaryEmerald
import com.example.smartexpensetracker.ui.theme.SuccessGreen
import com.example.smartexpensetracker.ui.viewmodel.MainViewModel
import kotlinx.coroutines.delay

enum class AuthStep {
    PHONE_INPUT,
    OTP_VERIFICATION,
    PROFILE_SETUP
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: MainViewModel,
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    val userProfile by viewModel.userProfile.collectAsState()
    val generatedOtp by viewModel.currentGeneratedOtp.collectAsState()

    var currentStep by remember { mutableStateOf(AuthStep.PHONE_INPUT) }
    var phoneNumber by remember { mutableStateOf(userProfile.mobileNumber.ifBlank { "6379982741" }) }
    var enteredOtp by remember { mutableStateOf("") }
    var userName by remember { mutableStateOf(userProfile.userName.ifBlank { "Lohith" }) }
    var accountNumber by remember { mutableStateOf(userProfile.accountNumber.ifBlank { "381 100050 305779" }) }
    var bankName by remember { mutableStateOf(userProfile.bankName.ifBlank { "Tamilnad Mercantile Bank (TMB)" }) }
    var showBankSelector by remember { mutableStateOf(false) }

    var resendTimer by remember { mutableStateOf(30) }
    var isTimerRunning by remember { mutableStateOf(false) }

    // Automatic SMS OTP Detection Listener
    DisposableEffect(Unit) {
        val listener: (String) -> Unit = { detectedOtp ->
            enteredOtp = detectedOtp
            val valid = viewModel.verifyOtp(detectedOtp)
            if (valid) {
                currentStep = AuthStep.PROFILE_SETUP
            }
        }
        com.example.smartexpensetracker.data.receiver.SmsOtpBroadcaster.registerListener(listener)
        onDispose {
            com.example.smartexpensetracker.data.receiver.SmsOtpBroadcaster.unregisterListener(listener)
        }
    }

    LaunchedEffect(isTimerRunning) {
        if (isTimerRunning) {
            while (resendTimer > 0) {
                delay(1000)
                resendTimer--
            }
            isTimerRunning = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // App Logo
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(80.dp),
                shadowElevation = 6.dp
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_spendwise_logo),
                    contentDescription = "SpendWise Logo",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                        .clip(RoundedCornerShape(16.dp))
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "SpendWise",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "Smart Bank & UPI Expense Tracker",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Animated Step Container
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    when (currentStep) {
                        AuthStep.PHONE_INPUT -> {
                            // Step 1: Phone Number
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.PhoneIphone, contentDescription = null, tint = PrimaryEmerald)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Mobile Login & Verification",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Text(
                                text = "Enter your 10-digit mobile number to verify your bank SMS identity via OTP.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = phoneNumber,
                                onValueChange = {
                                    val digitsOnly = it.filter { ch -> ch.isDigit() }.take(10)
                                    phoneNumber = digitsOnly
                                },
                                label = { Text("Mobile Number") },
                                prefix = { Text("+91 ", fontWeight = FontWeight.Bold) },
                                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Phone,
                                    imeAction = ImeAction.Done
                                ),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Button(
                                onClick = {
                                    if (phoneNumber.length == 10) {
                                        val otp = viewModel.sendOtp(phoneNumber)
                                        currentStep = AuthStep.OTP_VERIFICATION
                                        resendTimer = 30
                                        isTimerRunning = true
                                        Toast.makeText(context, "OTP Sent to +91 $phoneNumber", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Please enter a valid 10-digit mobile number", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                enabled = phoneNumber.length == 10,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                            ) {
                                Text("Get OTP Code", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(Icons.Default.ArrowForward, contentDescription = null)
                            }
                        }

                        AuthStep.OTP_VERIFICATION -> {
                            // Step 2: 6-Digit OTP Verification
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = PrimaryEmerald)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Enter 6-Digit OTP",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Text(
                                text = "A 6-digit verification code was sent to +91 $phoneNumber",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Quick OTP Demo Badge
                            if (generatedOtp != null) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = PrimaryEmerald.copy(alpha = 0.15f)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            enteredOtp = generatedOtp!!
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.MarkEmailRead, contentDescription = null, tint = PrimaryEmerald, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Your OTP: ${generatedOtp}", fontWeight = FontWeight.Bold, color = PrimaryEmerald)
                                        }
                                        Text("Tap to Fill", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = PrimaryEmerald)
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = enteredOtp,
                                onValueChange = {
                                    val digitsOnly = it.filter { ch -> ch.isDigit() }.take(6)
                                    enteredOtp = digitsOnly
                                },
                                label = { Text("6-Digit OTP Code") },
                                placeholder = { Text("• • • • • •") },
                                leadingIcon = { Icon(Icons.Default.Pin, contentDescription = null) },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Done
                                ),
                                textStyle = MaterialTheme.typography.titleLarge.copy(
                                    letterSpacing = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                ),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Resend Timer Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(onClick = { currentStep = AuthStep.PHONE_INPUT }) {
                                    Text("Change Number", style = MaterialTheme.typography.bodySmall)
                                }

                                if (isTimerRunning) {
                                    Text("Resend in ${resendTimer}s", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                } else {
                                    TextButton(onClick = {
                                        viewModel.sendOtp(phoneNumber)
                                        resendTimer = 30
                                        isTimerRunning = true
                                        Toast.makeText(context, "OTP Resent!", Toast.LENGTH_SHORT).show()
                                    }) {
                                        Text("Resend OTP", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    val valid = viewModel.verifyOtp(enteredOtp)
                                    if (valid) {
                                        currentStep = AuthStep.PROFILE_SETUP
                                    } else {
                                        Toast.makeText(context, "Invalid OTP code. Please enter 6-digit code or tap to auto-fill.", Toast.LENGTH_LONG).show()
                                    }
                                },
                                enabled = enteredOtp.length == 6,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                            ) {
                                Text("Verify & Continue", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }

                        AuthStep.PROFILE_SETUP -> {
                            // Step 3: Quick Profile & Bank Details Setup
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Account Setup",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                TextButton(
                                    onClick = {
                                        val detected = viewModel.autoDetectUserProfile()
                                        userName = detected.userName
                                        accountNumber = detected.accountNumber
                                        bankName = detected.bankName
                                        Toast.makeText(context, "⚡ Auto-detected: ${detected.bankName} (${detected.accountNumber})", Toast.LENGTH_SHORT).show()
                                    },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(14.dp), tint = PrimaryEmerald)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Auto-Detect", style = MaterialTheme.typography.labelSmall, color = PrimaryEmerald)
                                }
                            }

                            Text(
                                text = "Personalize your account for statement reports and Google Sheets ledger sync:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = userName,
                                onValueChange = { userName = it },
                                label = { Text("Your Name / Account Holder") },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = bankName,
                                onValueChange = { bankName = it },
                                label = { Text("Primary Bank Name") },
                                leadingIcon = { Icon(Icons.Default.AccountBalance, contentDescription = null, tint = PrimaryEmerald) },
                                trailingIcon = {
                                    IconButton(onClick = { showBankSelector = true }) {
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Bank")
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showBankSelector = true }
                            )

                            OutlinedTextField(
                                value = accountNumber,
                                onValueChange = { accountNumber = it },
                                label = { Text("Primary Account No.") },
                                leadingIcon = { Icon(Icons.Default.CreditCard, contentDescription = null) },
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Button(
                                onClick = {
                                    viewModel.completeOnboarding(
                                        userName = userName.ifBlank { "User" },
                                        mobileNumber = phoneNumber,
                                        accountNumber = accountNumber.ifBlank { "SB 305779" },
                                        bankName = bankName.ifBlank { "Tamilnad Mercantile Bank (TMB)" }
                                    )
                                    Toast.makeText(context, "Welcome to SpendWise, $userName!", Toast.LENGTH_LONG).show()
                                    onLoginSuccess()
                                },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                            ) {
                                Text("Start Using SpendWise 🚀", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Privacy Security Footer
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Security, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "100% On-Device Sandbox Privacy Guaranteed",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
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
