package com.example.smartexpensetracker.ui.screens.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
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
import com.example.smartexpensetracker.data.auth.FirebasePhoneAuthHelper
import com.example.smartexpensetracker.ui.theme.PrimaryEmerald
import com.example.smartexpensetracker.ui.theme.SuccessGreen
import com.example.smartexpensetracker.ui.viewmodel.MainViewModel
import kotlinx.coroutines.delay

fun Context.findActivity(): Activity? {
    var currentContext = this
    while (currentContext is ContextWrapper) {
        if (currentContext is Activity) return currentContext
        currentContext = currentContext.baseContext
    }
    return null
}

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
    val activity = remember(context) { context.findActivity() }
    val userProfile by viewModel.userProfile.collectAsState()

    var currentStep by remember { mutableStateOf(AuthStep.PHONE_INPUT) }
    var phoneNumber by remember { mutableStateOf(userProfile.mobileNumber.ifBlank { "6379982741" }) }
    var enteredOtp by remember { mutableStateOf("") }
    var userName by remember { mutableStateOf(userProfile.userName.ifBlank { "Lohith" }) }
    var accountNumber by remember { mutableStateOf(userProfile.accountNumber.ifBlank { "381 100050 305779" }) }
    var bankName by remember { mutableStateOf(userProfile.bankName.ifBlank { "Tamilnad Mercantile Bank (TMB)" }) }
    var showBankSelector by remember { mutableStateOf(false) }
    var isSendingOtp by remember { mutableStateOf(false) }
    var isVerifyingOtp by remember { mutableStateOf(false) }
    var firebaseVerificationId by remember { mutableStateOf<String?>(null) }

    var resendTimer by remember { mutableStateOf(30) }
    var isTimerRunning by remember { mutableStateOf(false) }

    // Automatic SMS OTP Detection Listener (From Device SMS Inbox)
    DisposableEffect(Unit) {
        val listener: (String) -> Unit = { detectedOtp ->
            enteredOtp = detectedOtp
            val vId = firebaseVerificationId
            if (vId != null) {
                FirebasePhoneAuthHelper.verifyCode(
                    verificationId = vId,
                    code = detectedOtp,
                    onSuccess = {
                        viewModel.verifyOtp(detectedOtp)
                        currentStep = AuthStep.PROFILE_SETUP
                    },
                    onFailure = {
                        if (viewModel.verifyOtp(detectedOtp)) {
                            currentStep = AuthStep.PROFILE_SETUP
                        }
                    }
                )
            } else if (viewModel.verifyOtp(detectedOtp)) {
                currentStep = AuthStep.PROFILE_SETUP
            }
        }
        com.example.smartexpensetracker.data.receiver.SmsOtpBroadcaster.registerListener(listener)
        onDispose {
            com.example.smartexpensetracker.data.receiver.SmsOtpBroadcaster.unregisterListener(listener)
        }
    }

    // Resend countdown timer
    LaunchedEffect(isTimerRunning, resendTimer) {
        if (isTimerRunning && resendTimer > 0) {
            delay(1000L)
            resendTimer -= 1
        } else if (resendTimer == 0) {
            isTimerRunning = false
        }
    }

    fun triggerOtpSend() {
        if (phoneNumber.length != 10) {
            Toast.makeText(context, "Please enter a valid 10-digit mobile number", Toast.LENGTH_SHORT).show()
            return
        }

        val targetActivity = activity
        if (targetActivity == null) {
            Toast.makeText(context, "Cannot find Activity context", Toast.LENGTH_SHORT).show()
            return
        }

        isSendingOtp = true
        FirebasePhoneAuthHelper.sendVerificationCode(
            activity = targetActivity,
            phoneNumber = phoneNumber,
            onCodeSent = { verificationId ->
                firebaseVerificationId = verificationId
                isSendingOtp = false
                currentStep = AuthStep.OTP_VERIFICATION
                resendTimer = 30
                isTimerRunning = true
                Toast.makeText(context, "SMS OTP dispatched to +91 $phoneNumber", Toast.LENGTH_LONG).show()
            },
            onVerificationCompleted = { credential ->
                credential.smsCode?.let { enteredOtp = it }
                isSendingOtp = false
                currentStep = AuthStep.PROFILE_SETUP
                Toast.makeText(context, "Phone Number Verified Automatically!", Toast.LENGTH_SHORT).show()
            },
            onVerificationFailed = { e ->
                isSendingOtp = false
                currentStep = AuthStep.OTP_VERIFICATION
                resendTimer = 30
                isTimerRunning = true
                val msg = e.localizedMessage ?: "Failed to send SMS OTP"
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            }
        )
    }

    fun triggerOtpVerify() {
        if (enteredOtp.length != 6) {
            Toast.makeText(context, "Please enter the 6-digit OTP code received via SMS", Toast.LENGTH_SHORT).show()
            return
        }

        isVerifyingOtp = true
        val vId = firebaseVerificationId
        if (vId != null) {
            FirebasePhoneAuthHelper.verifyCode(
                verificationId = vId,
                code = enteredOtp,
                onSuccess = {
                    isVerifyingOtp = false
                    viewModel.verifyOtp(enteredOtp)
                    currentStep = AuthStep.PROFILE_SETUP
                },
                onFailure = { e ->
                    isVerifyingOtp = false
                    Toast.makeText(context, "Invalid OTP code: ${e.message}", Toast.LENGTH_LONG).show()
                }
            )
        } else {
            val valid = viewModel.verifyOtp(enteredOtp)
            isVerifyingOtp = false
            if (valid) {
                currentStep = AuthStep.PROFILE_SETUP
            } else {
                Toast.makeText(context, "Invalid OTP code. Please check your SMS.", Toast.LENGTH_LONG).show()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            Spacer(modifier = Modifier.height(30.dp))

            // App Logo & Header
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(PrimaryEmerald.copy(alpha = 0.15f))
                    .border(2.dp, PrimaryEmerald, CircleShape)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_spendwise_logo),
                    contentDescription = "SpendWise Logo",
                    modifier = Modifier.size(56.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "SpendWise",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = PrimaryEmerald
            )

            Text(
                text = "Smart Bank SMS Expense Tracker",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Step Progress Indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                StepIndicator(step = 1, title = "Number", isActive = currentStep == AuthStep.PHONE_INPUT, isDone = currentStep.ordinal > 0)
                StepLine(isDone = currentStep.ordinal > 0)
                StepIndicator(step = 2, title = "OTP", isActive = currentStep == AuthStep.OTP_VERIFICATION, isDone = currentStep.ordinal > 1)
                StepLine(isDone = currentStep.ordinal > 1)
                StepIndicator(step = 3, title = "Profile", isActive = currentStep == AuthStep.PROFILE_SETUP, isDone = false)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Main Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    when (currentStep) {
                        AuthStep.PHONE_INPUT -> {
                            // Step 1: Mobile Number Input
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = PrimaryEmerald)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Enter Mobile Number",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Text(
                                text = "Enter your 10-digit mobile number to receive a verification OTP via SMS.",
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
                                keyboardActions = KeyboardActions(onDone = { triggerOtpSend() }),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Button(
                                onClick = { triggerOtpSend() },
                                enabled = phoneNumber.length == 10 && !isSendingOtp,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                            ) {
                                if (isSendingOtp) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Sending SMS OTP...", fontWeight = FontWeight.Bold)
                                } else {
                                    Text("Get OTP via SMS", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(Icons.Default.ArrowForward, contentDescription = null)
                                }
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
                                text = "A 6-digit verification SMS was sent to +91 $phoneNumber",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = enteredOtp,
                                onValueChange = {
                                    val digitsOnly = it.filter { ch -> ch.isDigit() }.take(6)
                                    enteredOtp = digitsOnly
                                },
                                label = { Text("6-Digit SMS OTP") },
                                placeholder = { Text("------") },
                                leadingIcon = { Icon(Icons.Default.Pin, contentDescription = null) },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(onDone = { triggerOtpVerify() }),
                                textStyle = LocalTextStyle.current.copy(
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 6.sp,
                                    fontFamily = FontFamily.Monospace,
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
                                    TextButton(onClick = { triggerOtpSend() }) {
                                        Text("Resend OTP", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Button(
                                onClick = { triggerOtpVerify() },
                                enabled = enteredOtp.length == 6 && !isVerifyingOtp,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                            ) {
                                if (isVerifyingOtp) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Verifying...", fontWeight = FontWeight.Bold)
                                } else {
                                    Text("Verify & Continue", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
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
                                        Toast.makeText(context, "Auto-detected: ${detected.bankName} (${detected.accountNumber})", Toast.LENGTH_SHORT).show()
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
                                Text("Start Using SpendWise", fontWeight = FontWeight.Bold, fontSize = 16.sp)
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

@Composable
fun StepIndicator(step: Int, title: String, isActive: Boolean, isDone: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(
                    when {
                        isDone -> SuccessGreen
                        isActive -> PrimaryEmerald
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                )
        ) {
            if (isDone) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            } else {
                Text(
                    text = step.toString(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = if (isActive) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = if (isActive || isDone) FontWeight.Bold else FontWeight.Normal,
            color = if (isActive) PrimaryEmerald else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
fun StepLine(isDone: Boolean) {
    Box(
        modifier = Modifier
            .width(36.dp)
            .height(2.dp)
            .padding(bottom = 16.dp)
            .background(if (isDone) SuccessGreen else MaterialTheme.colorScheme.surfaceVariant)
    )
}