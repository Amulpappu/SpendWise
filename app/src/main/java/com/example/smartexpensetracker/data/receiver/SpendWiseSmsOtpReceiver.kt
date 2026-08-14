package com.example.smartexpensetracker.data.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsManager
import android.util.Log

object SmsOtpBroadcaster {
    private val listeners = mutableSetOf<(String) -> Unit>()

    fun registerListener(listener: (String) -> Unit) {
        listeners.add(listener)
    }

    fun unregisterListener(listener: (String) -> Unit) {
        listeners.remove(listener)
    }

    fun notifyOtpReceived(otp: String) {
        listeners.forEach { it(otp) }
    }
}

class SpendWiseSmsOtpReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (Telephony.Sms.Intents.SMS_RECEIVED_ACTION == intent.action) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            for (sms in messages) {
                val body = sms.messageBody ?: continue
                Log.d("SpendWiseOtpReceiver", "Incoming SMS: $body")

                // Extract 6-digit OTP from message
                val otpRegex = Regex("""(?i)(?:otp|code|spendwise|verification)[:\s-]*([0-9]{6})\b|(?<!\d)(\d{6})(?!\d)""")
                val match = otpRegex.find(body)
                if (match != null) {
                    val detectedOtp = match.groupValues.lastOrNull { it.isNotBlank() && it.length == 6 } ?: match.value.filter { it.isDigit() }
                    if (detectedOtp.length == 6) {
                        Log.d("SpendWiseOtpReceiver", "Detected OTP from SMS: $detectedOtp")
                        SmsOtpBroadcaster.notifyOtpReceived(detectedOtp)
                    }
                }
            }
        }
    }

    companion object {
        /**
         * Dispatches a real verification SMS to the user's mobile number offline.
         */
        fun sendVerificationSms(context: Context, phoneNumber: String, otp: String): Boolean {
            return try {
                val cleanPhone = phoneNumber.filter { it.isDigit() }.let {
                    if (it.length == 10) "+91$it" else if (!it.startsWith("+")) "+$it" else it
                }
                val smsManager = context.getSystemService(SmsManager::class.java) ?: SmsManager.getDefault()
                val message = "Your SpendWise verification code is: $otp. Valid for 10 minutes. Do not share this OTP with anyone."
                smsManager.sendTextMessage(cleanPhone, null, message, null, null)
                Log.d("SpendWiseOtpReceiver", "Real SMS sent to $cleanPhone: $message")
                true
            } catch (e: Exception) {
                Log.e("SpendWiseOtpReceiver", "Failed to send real SMS: ${e.message}", e)
                false
            }
        }
    }
}
