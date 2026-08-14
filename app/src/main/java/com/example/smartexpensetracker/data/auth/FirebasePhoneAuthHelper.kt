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
            if (it.length == 10) "+91" else if (!it.startsWith("+")) "+" else it
        }
        Log.d("FirebaseAuth", "Initiating Firebase Phone Auth for $cleanNumber")

        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                Log.d("FirebaseAuth", "Auto verification completed / instant SMS detected")
                onVerificationCompleted(credential)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                Log.e("FirebaseAuth", "Firebase verification failed: ${e.message}", e)
                onVerificationFailed(e)
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                Log.d("FirebaseAuth", "SMS sent successfully! Verification ID: $verificationId")
                storedVerificationId = verificationId
                resendToken = token
                onCodeSent(verificationId)
            }
        }

        val optionsBuilder = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(cleanNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)

        resendToken?.let { optionsBuilder.setForceResendingToken(it) }

        PhoneAuthProvider.verifyPhoneNumber(optionsBuilder.build())
    }

    fun verifyCode(
        verificationId: String,
        code: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        try {
            val credential = PhoneAuthProvider.getCredential(verificationId, code)
            auth.signInWithCredential(credential)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d("FirebaseAuth", "Firebase Sign in success!")
                        onSuccess()
                    } else {
                        Log.e("FirebaseAuth", "Firebase Sign in failed: ${task.exception?.message}")
                        onFailure(task.exception ?: Exception("Invalid verification code"))
                    }
                }
        } catch (e: Exception) {
            onFailure(e)
        }
    }
}