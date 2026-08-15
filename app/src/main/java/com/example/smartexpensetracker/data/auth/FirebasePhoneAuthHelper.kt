package com.example.smartexpensetracker.data.auth

import android.app.Activity
import android.util.Log
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit

object FirebasePhoneAuthHelper {
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private var storedVerificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null

    fun sendVerificationCode(
        activity: Activity,
        phoneNumber: String,
        onCodeSent: (verificationId: String) -> Unit,
        onVerificationCompleted: (credential: PhoneAuthCredential) -> Unit,
        onVerificationFailed: (e: Exception) -> Unit
    ) {
        val cleanNumber = phoneNumber.filter { it.isDigit() }.let {
            if (it.length == 10) "+91$it" else if (!it.startsWith("+")) "+$it" else it
        }
        Log.i("FirebaseAuth", ">>> Sending Firebase SMS to: $cleanNumber with Activity: ${activity.localClassName}")

        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                Log.i("FirebaseAuth", ">>> Firebase Auto Verification completed!")
                onVerificationCompleted(credential)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                Log.e("FirebaseAuth", ">>> Firebase Verification Failed: ${e.message}", e)
                onVerificationFailed(e)
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                Log.i("FirebaseAuth", ">>> Firebase SMS Sent! Verification ID: $verificationId")
                storedVerificationId = verificationId
                resendToken = token
                onCodeSent(verificationId)
            }
        }

        try {
            val optionsBuilder = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(cleanNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(callbacks)

            resendToken?.let { optionsBuilder.setForceResendingToken(it) }

            PhoneAuthProvider.verifyPhoneNumber(optionsBuilder.build())
            Log.i("FirebaseAuth", ">>> verifyPhoneNumber request submitted to Google Firebase servers")
        } catch (e: Exception) {
            Log.e("FirebaseAuth", ">>> Exception starting PhoneAuthProvider: ${e.message}", e)
            onVerificationFailed(e)
        }
    }

    fun verifyCode(
        verificationId: String,
        code: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        try {
            Log.i("FirebaseAuth", ">>> Verifying SMS Code: $code for ID: $verificationId")
            val credential = PhoneAuthProvider.getCredential(verificationId, code)
            auth.signInWithCredential(credential)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.i("FirebaseAuth", ">>> Firebase Sign in SUCCESS!")
                        onSuccess()
                    } else {
                        Log.e("FirebaseAuth", ">>> Firebase Sign in FAILED: ${task.exception?.message}")
                        onFailure(task.exception ?: Exception("Invalid verification code"))
                    }
                }
        } catch (e: Exception) {
            Log.e("FirebaseAuth", ">>> Exception in verifyCode: ${e.message}", e)
            onFailure(e)
        }
    }
}