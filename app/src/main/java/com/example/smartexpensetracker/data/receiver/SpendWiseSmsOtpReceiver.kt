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
                        Log.d("SpendWiseOtpReceiver", "Auto-detected OTP from SMS: $detectedOtp")
                        SmsOtpBroadcaster.notifyOtpReceived(detectedOtp)
                    }
                }
            }
        }
    }

    companion object {
        // Fast2SMS API key — direct Indian SMS gateway (free credits on signup)
        private const val FAST2SMS_API_KEY =
            "FRSy4oBJjnMm28NZVhkr1Kf5DqY6UgXWHzsx70aCvlLTIPciwdcDXU50fue21yJvbFHalG7TzQCS3ngt"

        private const val FAST2SMS_URL = "https://www.fast2sms.com/dev/bulkV2"

        /**
         * Sends a real OTP SMS to the user's phone via Fast2SMS.
         * Runs on a background IO coroutine — never blocks the UI thread.
         */
        fun sendVerificationSms(context: Context, phoneNumber: String, otp: String) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val cleanPhone = phoneNumber.filter { it.isDigit() }.takeLast(10)
                    val message = "Your SpendWise OTP is $otp. Do not share this with anyone. Valid for 10 minutes."

                    // Fast2SMS Quick SMS (route=q) — works without DLT registration
                    val json = """
                        {
                            "route": "q",
                            "message": "$message",
                            "language": "english",
                            "flash": "0",
                            "numbers": "$cleanPhone"
                        }
                    """.trimIndent()

                    val url = URL(FAST2SMS_URL)
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.setRequestProperty("Content-Type", "application/json")
                    conn.setRequestProperty("authorization", FAST2SMS_API_KEY)
                    conn.setRequestProperty("cache-control", "no-cache")
                    conn.doOutput = true
                    conn.connectTimeout = 15000
                    conn.readTimeout = 20000

                    conn.outputStream.use { it.write(json.toByteArray(Charsets.UTF_8)) }

                    val responseCode = conn.responseCode
                    val response = try {
                        conn.inputStream.bufferedReader().readText()
                    } catch (e: Exception) {
                        conn.errorStream?.bufferedReader()?.readText() ?: "no response body"
                    }

                    Log.d("SpendWiseOtpSender", "Fast2SMS response [$responseCode]: $response")

                    if (responseCode == 200 && response.contains("\"return\":true", ignoreCase = true)) {
                        Log.d("SpendWiseOtpSender", "✅ OTP SMS sent successfully to $cleanPhone")
                    } else {
                        Log.w("SpendWiseOtpSender", "⚠️ Fast2SMS responded [$responseCode]: $response")
                    }

                    conn.disconnect()
                } catch (e: Exception) {
                    Log.e("SpendWiseOtpSender", "❌ Failed to send OTP SMS: ${e.message}", e)
                }
            }
        }
    }
}
