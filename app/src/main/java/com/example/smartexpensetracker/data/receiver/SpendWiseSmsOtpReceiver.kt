package com.example.smartexpensetracker.data.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

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

                // Extract 6-digit OTP from message body
                val otpRegex = Regex("""(?i)(?:otp|code|spendwise|verification)[:\s-]*([0-9]{6})\b|(?<!\d)(\d{6})(?!\d)""")
                val match = otpRegex.find(body)
                if (match != null) {
                    val detectedOtp = match.groupValues.lastOrNull { it.isNotBlank() && it.length == 6 }
                        ?: match.value.filter { it.isDigit() }
                    if (detectedOtp.length == 6) {
                        Log.d("SpendWiseOtpReceiver", "Auto-detected OTP: $detectedOtp")
                        SmsOtpBroadcaster.notifyOtpReceived(detectedOtp)
                    }
                }
            }
        }
    }

    companion object {
        // Your active Google Apps Script webhook URL — used as a free SMS relay
        private const val WEBHOOK_URL =
            "https://script.google.com/macros/s/AKfycbxVXjX6oeYpWJoFh-wT6ENPbnITMvy0n00ckSxEV2stv68EfskZatjJNXHnWjMrqqogow/exec"

        /**
         * Sends OTP via your Google Apps Script → Fast2SMS relay.
         * Runs in a background coroutine, never blocks the UI.
         */
        fun sendVerificationSms(context: Context, phoneNumber: String, otp: String) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val cleanPhone = phoneNumber.filter { it.isDigit() }.takeLast(10)
                    val message = "Your SpendWise OTP is: $otp . Do not share this with anyone. Valid for 10 minutes."

                    // Build JSON payload for Apps Script OTP relay
                    val json = """
                        {
                            "action": "send_otp",
                            "phone": "$cleanPhone",
                            "otp": "$otp",
                            "message": "${message.replace("\"", "'")}"
                        }
                    """.trimIndent()

                    val url = URL(WEBHOOK_URL)
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.setRequestProperty("Content-Type", "application/json")
                    conn.doOutput = true
                    conn.connectTimeout = 10000
                    conn.readTimeout = 15000

                    conn.outputStream.use { it.write(json.toByteArray()) }
                    val responseCode = conn.responseCode
                    val response = conn.inputStream.bufferedReader().readText()

                    Log.d("SpendWiseOtpSender", "OTP relay response [$responseCode]: $response")
                    conn.disconnect()
                } catch (e: Exception) {
                    Log.e("SpendWiseOtpSender", "OTP relay failed: ${e.message}", e)
                }
            }
        }
    }
}
