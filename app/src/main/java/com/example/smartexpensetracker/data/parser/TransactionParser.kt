package com.example.smartexpensetracker.data.parser

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.regex.Pattern
import kotlin.math.abs

data class ParsedTransaction(
    val amount: Double,
    val isIncome: Boolean,
    val merchant: String,
    val refId: String? = null,
    val paymentMethod: String = "UPI",
    val bankName: String? = null,
    val accountNumber: String? = null,
    val availableBalance: Double? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val rawSanitizedText: String
)

object TransactionParser {

    private val SENSITIVE_KEYWORDS = listOf(
        "otp", "secret", "pin", "password", "cvv", "verification code", "one time password"
    )

    // Authorized bank senders list to reject spam / loan / scam SMS
    fun isAuthorizedSender(sender: String?, rawText: String? = null): Boolean {
        if (sender.isNullOrEmpty()) return true
        val s = sender.uppercase().replace("-", "").replace("_", "")
        val isSenderAuth = s.contains("TMBANK") ||
                s.contains("TMB") ||
                s.contains("PAYTM") ||
                s.contains("PHONEPE") ||
                s.contains("GPAY") ||
                s.contains("CRED") ||
                s.contains("BHARATPE") ||
                s.contains("HDFC") ||
                s.contains("ICICI") ||
                s.contains("SBI") ||
                s.contains("AXIS") ||
                s.contains("KOTAK") ||
                s.contains("BOB") ||
                s.contains("CANARA") ||
                s.contains("PNB") ||
                s.contains("UNION") ||
                s.contains("FDRL") ||
                s.contains("FEDERAL") ||
                s.contains("IDFC") ||
                s.contains("INDUS") ||
                s.contains("RBL") ||
                s.contains("YES") ||
                s.contains("BANK") ||
                s.contains("ALERTS") ||
                s.contains("ALERT") ||
                s.contains("TXN") ||
                s.contains("NOTIFICATION")
        if (isSenderAuth) return true

        if (!rawText.isNullOrEmpty()) {
            val upper = rawText.uppercase()
            if (upper.contains("RS.") || upper.contains("INR") || upper.contains("RS ") || upper.contains("\u20B9")) {
                if (upper.contains("DEBITED") || upper.contains("CREDITED") || upper.contains("PAID") || upper.contains("SENT") || upper.contains("RECEIVED") || upper.contains("TRANSFERRED") || upper.contains("A/C") || upper.contains("ACCOUNT") || upper.contains("VPA") || upper.contains("UPI") || upper.contains("IMPS") || upper.contains("NEFT")) {
                    return true
                }
            }
        }
        return false
    }

    // Date & Time extraction patterns from bank SMS
    private val DATE_PATTERNS = listOf(
        Pair(Pattern.compile("on\\s+([0-9]{2}-[0-9]{2}-[0-9]{4}\\s+[0-9]{1,2}:[0-9]{2}:[0-9]{2})", Pattern.CASE_INSENSITIVE), "dd-MM-yyyy HH:mm:ss"),
        Pair(Pattern.compile("on\\s+([0-9]{2}-[0-9]{2}-[0-9]{4}\\s+[0-9]{1,2}:[0-9]{2}\\s+(?:AM|PM))", Pattern.CASE_INSENSITIVE), "dd-MM-yyyy hh:mm a"),
        Pair(Pattern.compile("on\\s+([0-9]{2}-[0-9]{2}-[0-9]{4}\\s+[0-9]{1,2}:[0-9]{2})", Pattern.CASE_INSENSITIVE), "dd-MM-yyyy HH:mm"),
        Pair(Pattern.compile("on\\s+([0-9]{2}-[0-9]{2}-[0-9]{2}\\s+[0-9]{1,2}:[0-9]{2})", Pattern.CASE_INSENSITIVE), "dd-MM-yy HH:mm"),
        Pair(Pattern.compile("as\\s+on\\s+([0-9]{2}-[0-9]{2}-[0-9]{4}\\s+[0-9]{1,2}:[0-9]{2})", Pattern.CASE_INSENSITIVE), "dd-MM-yyyy HH:mm"),
        Pair(Pattern.compile("([0-9]{2}-[0-9]{2}-[0-9]{4})", Pattern.CASE_INSENSITIVE), "dd-MM-yyyy")
    )

    fun extractTimestamp(rawText: String, fallbackTimestamp: Long): Long {
        for ((pattern, format) in DATE_PATTERNS) {
            val matcher = pattern.matcher(rawText)
            if (matcher.find()) {
                val dateStr = matcher.group(1)?.trim()
                if (!dateStr.isNullOrEmpty()) {
                    try {
                        val sdf = SimpleDateFormat(format, Locale.US)
                        val parsed = sdf.parse(dateStr)
                        if (parsed != null) {
                            return parsed.time
                        }
                    } catch (e: Exception) {
                        // try next
                    }
                }
            }
        }
        return fallbackTimestamp
    }

    // Account Patterns (e.g. XXXX5779, SB 305779, SB305779, 305779)
    private val ACCOUNT_PATTERNS = listOf(
        Pattern.compile("(?:your\\s+)?(?:a/c|acc(?:ount)?|sb)\\s*(?:no\\.?)?\\s*[:\\-]?\\s*([a-zA-Z0-9*X]+)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("sb\\s*([0-9]{4,10})", Pattern.CASE_INSENSITIVE),
        Pattern.compile("a/c\\s*([xX0-9]+)", Pattern.CASE_INSENSITIVE)
    )

    // Balance Patterns (e.g. "Current AVBL bal is Rs.991.75", "Avbl Bal Rs.40358.35", "Clr Bal Rs.19,150.35")
    private val BALANCE_PATTERNS = listOf(
        Pattern.compile("(?:current\\s+)?(?:avbl|avail(?:able)?|clr)\\s*bal(?:ance)?\\s*(?:is|:|-)?\\s*(?:rs\\.?|inr|\u20B9)?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?:bal(?:ance)?)\\s*(?:is|:|-)?\\s*(?:rs\\.?|inr|\u20B9)?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)", Pattern.CASE_INSENSITIVE)
    )

    // Amount Patterns
    private val TXN_AMOUNT_PATTERNS = listOf(
        Pattern.compile("(?:debited\\s+with|credited\\s+with|debited\\s+by|credited\\s+by|credited\\s+for|credited|debited|paid|spent|sent|received|transferred|transfer|transaction|payment|deposit|deposited|added|cashback|refund)\\s+(?:by|with|of|for)?\\s*(?:rs\\.?|inr|\u20B9)?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?:rs\\.?|inr|\u20B9)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("([0-9,]+(?:\\.[0-9]{1,2})?)\\s*(?:rs\\.?|inr|\u20B9)", Pattern.CASE_INSENSITIVE)
    )

    // Linked VPA pattern
    private val LINKED_VPA_PATTERN = Pattern.compile("linked\\s+to\\s+([a-zA-Z0-9.\\-_@]+)", Pattern.CASE_INSENSITIVE)

    // NEFT / IMPS sender pattern (e.g. "by KARUPPASAMY PANDIYAN from FDRL bank")
    private val NEFT_INFO_PATTERN = Pattern.compile("info:\\s*neft-[a-z0-9]+-([a-z\\s]+)-", Pattern.CASE_INSENSITIVE)
    private val BY_SENDER_PATTERN = Pattern.compile("by\\s+([a-zA-Z\\s]{3,35}?)\\s+from", Pattern.CASE_INSENSITIVE)

    // Merchant / Payee extraction patterns
    private val TO_MERCHANT_PATTERN = Pattern.compile(
        "(?:paid\\s+to|sent\\s+to|paid\\s+(?:rs\\.?|inr|\u20B9)?[0-9,.]+\\s+to|to|at|vpa)\\s+([a-zA-Z0-9&'\\-][a-zA-Z0-9&'\\-\\s]{1,45}?)(?=[\\,\\;\\.]|\\s+(?:on|ref|txn|via|a/c|bal|avail|info|for)|$)",
        Pattern.CASE_INSENSITIVE
    )

    // Ref / Txn ID patterns
    private val REF_PATTERNS = listOf(
        Pattern.compile("(?:upi\\s+ref|ref\\s*no|refno|ref(?:erence)?)\\s*(?:no\\.?)?\\s*[:\\-]?\\s*([a-zA-Z0-9]+)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?:txn\\s*id|rrn)\\s*[:\\-]?\\s*([a-zA-Z0-9]+)", Pattern.CASE_INSENSITIVE)
    )

    private val BANKS = listOf("TMB", "TMBANK", "HDFC", "ICICI", "SBI", "AXIS", "KOTAK", "PNB", "BOB", "YESB", "FDRL", "FEDERAL", "PAYTM", "PHONEPE", "GPAY", "CRED", "BHARATPE")

    fun parse(rawText: String, sender: String? = null, fallbackTimestamp: Long = System.currentTimeMillis()): ParsedTransaction? {
        if (!isAuthorizedSender(sender, rawText)) {
            return null
        }

        val lowerText = rawText.lowercase()

        // 1. Privacy filter
        for (kw in SENSITIVE_KEYWORDS) {
            if (lowerText.contains(kw)) {
                return null
            }
        }

        // 2. Reject Spam / Loan
        if (lowerText.contains("pre-approved") || lowerText.contains("bonus points") || lowerText.contains("claim reward")) {
            if (!lowerText.contains("credited") && !lowerText.contains("debited")) {
                return null
            }
        }

        // 3. Extract Available Balance first
        var availBalance: Double? = null
        for (bp in BALANCE_PATTERNS) {
            val balMatcher = bp.matcher(rawText)
            if (balMatcher.find()) {
                val balStr = balMatcher.group(1)?.replace(",", "")
                availBalance = balStr?.toDoubleOrNull()
                if (availBalance != null) break
            }
        }

        // 4. Extract Account Number
        var accNo: String? = null
        for (p in ACCOUNT_PATTERNS) {
            val m = p.matcher(rawText)
            if (m.find()) {
                val candidate = m.group(1)?.trim()
                if (!candidate.isNullOrEmpty() && candidate.length >= 4) {
                    accNo = candidate
                    break
                }
            }
        }

        // 5. Determine Debit (Expense) vs Credit (Income) with strict user-account context
        // Check if USER's account was credited or debited
        val isUserAccountDebited = lowerText.contains("your a/c") && lowerText.contains("is debited") ||
                lowerText.contains("ur sb") && lowerText.contains("is debited") ||
                lowerText.contains("a/c no.") && lowerText.contains("is debited") ||
                lowerText.contains("is debited with") ||
                lowerText.contains("debited from") ||
                lowerText.contains("debited by")

        val isUserAccountCredited = lowerText.contains("your a/c") && lowerText.contains("is credited") ||
                lowerText.contains("ur sb") && lowerText.contains("is credited") ||
                lowerText.contains("a/c no.") && lowerText.contains("is credited") ||
                lowerText.contains("is credited with") ||
                lowerText.contains("credited rs") ||
                lowerText.contains("credited by") ||
                lowerText.contains("credited for") ||
                lowerText.contains("received rs") ||
                lowerText.contains("money received") ||
                lowerText.contains("you received") ||
                lowerText.contains("deposited") ||
                lowerText.contains("cashback") ||
                lowerText.contains("refund")

        val isIncome = when {
            isUserAccountDebited -> false
            isUserAccountCredited -> true
            lowerText.contains("debited") -> false
            lowerText.contains("credited") -> true
            else -> false
        }

        // 6. Extract Transaction Amount (distinguishing from balance)
        var amount: Double? = null
        for (pattern in TXN_AMOUNT_PATTERNS) {
            val matcher = pattern.matcher(rawText)
            while (matcher.find()) {
                val amtStr = matcher.group(1)?.replace(",", "")
                val parsed = amtStr?.toDoubleOrNull()
                if (parsed != null && parsed > 0) {
                    if (availBalance == null || abs(parsed - availBalance) > 0.001 || !rawText.lowercase().contains("bal")) {
                        amount = parsed
                        break
                    } else if (amount == null) {
                        amount = parsed
                    }
                }
            }
            if (amount != null) break
        }

        if (amount == null || amount <= 0.0 || amount.isNaN() || amount.isInfinite()) {
            return null
        }

        // 7. Extract Merchant / Beneficiary / Sender
        var merchant: String? = null

        // Check NEFT Info pattern (e.g. Info: NEFT-ICIC0000035-KARUPPASAMY PANDIYAN-IN72622238446542)
        val neftMatcher = NEFT_INFO_PATTERN.matcher(rawText)
        if (neftMatcher.find()) {
            merchant = neftMatcher.group(1)?.trim()?.uppercase()
        }

        // Check 'by <Sender> from FDRL bank'
        if (merchant == null) {
            val byMatcher = BY_SENDER_PATTERN.matcher(rawText)
            if (byMatcher.find()) {
                merchant = byMatcher.group(1)?.trim()?.uppercase()
            }
        }

        // Check 'linked to <VPA>'
        if (merchant == null) {
            val linkedMatcher = LINKED_VPA_PATTERN.matcher(rawText)
            if (linkedMatcher.find()) {
                val vpaRaw = linkedMatcher.group(1)?.trim() ?: ""
                merchant = parseVpaToMerchantName(vpaRaw)
            }
        }

        // Check 'Paid to <Merchant>' / 'to <Merchant>'
        if (merchant == null) {
            val toMatcher = TO_MERCHANT_PATTERN.matcher(rawText)
            if (toMatcher.find()) {
                val candidate = toMatcher.group(1)?.trim()
                if (!candidate.isNullOrEmpty() && !isNoise(candidate)) {
                    merchant = sanitizeMerchant(candidate)
                }
            }
        }

        if (merchant == null) {
            merchant = if (isIncome) {
                if (accNo != null) "Income (A/C $accNo)" else "Money Received"
            } else {
                if (accNo != null) "Acc $accNo" else "Bank Transaction"
            }
        }

        // 8. Extract Reference ID
        var refId: String? = null
        for (pattern in REF_PATTERNS) {
            val matcher = pattern.matcher(rawText)
            if (matcher.find()) {
                refId = matcher.group(1)?.trim()
                break
            }
        }

        // 9. Payment Method
        val paymentMethod = when {
            rawText.contains("IMPS", ignoreCase = true) -> "IMPS"
            rawText.contains("NEFT", ignoreCase = true) -> "NEFT"
            rawText.contains("UPI", ignoreCase = true) || rawText.contains("paytm", ignoreCase = true) || rawText.contains("phonepe", ignoreCase = true) -> "UPI"
            rawText.contains("card", ignoreCase = true) -> "Card"
            else -> "Bank Transfer"
        }

        // 10. Bank Name
        var bankName: String? = null
        for (bank in BANKS) {
            if (rawText.contains(bank, ignoreCase = true) || (sender != null && sender.contains(bank, ignoreCase = true))) {
                bankName = bank
                break
            }
        }

        // 11. Extract Real Transaction Timestamp from SMS Text
        val transactionTimestamp = extractTimestamp(rawText, fallbackTimestamp)

        return ParsedTransaction(
            amount = amount,
            isIncome = isIncome,
            merchant = merchant,
            refId = refId,
            paymentMethod = paymentMethod,
            bankName = bankName,
            accountNumber = accNo,
            availableBalance = availBalance,
            timestamp = transactionTimestamp,
            rawSanitizedText = sanitizeRawText(rawText)
        )
    }

    private fun parseVpaToMerchantName(vpa: String): String {
        val prefix = vpa.split("@").firstOrNull() ?: vpa
        val lower = prefix.lowercase()
        return when {
            lower.contains("paytmqr") -> "Paytm QR Merchant"
            lower.contains("paytm") -> "Paytm Merchant"
            lower.contains("bharatpe") -> "BharatPe Merchant"
            lower.contains("phonepe") || lower.contains("ybl") -> "PhonePe Merchant"
            lower.contains("gpay") || lower.contains("okaxis") || lower.contains("okhdfcbank") -> "Google Pay Merchant"
            lower.contains("swiggy") -> "Swiggy"
            lower.contains("zomato") -> "Zomato"
            lower.contains("zepto") -> "Zepto"
            lower.contains("blinkit") -> "Blinkit"
            else -> {
                val cleaned = prefix.replace(Regex("[0-9]{4,}"), "")
                    .replace(".", " ")
                    .replace("_", " ")
                    .replace("-", " ")
                    .trim()
                if (cleaned.length >= 3) cleaned.uppercase() else prefix.uppercase()
            }
        }
    }

    private fun isNoise(str: String): Boolean {
        val lower = str.lowercase()
        return lower.length < 2 ||
                lower.startsWith("rs") ||
                lower.startsWith("inr") ||
                lower.contains("your account") ||
                lower.contains("a/c") ||
                lower.contains("available") ||
                lower.contains("bal")
    }

    private fun sanitizeMerchant(str: String): String {
        return str.replace(Regex("(?i)\\b(on|ref|txn|via|a/c|bal|avail|xx\\d+)\\b.*"), "")
            .trim()
            .trimEnd('.', ',', ';', ':', '-', ' ')
            .split(Regex("\\s+"))
            .take(6)
            .joinToString(" ")
    }

    private fun sanitizeRawText(rawText: String): String {
        return rawText.replace(Regex("(?i)\\b(\\d{4,8})\\b"), "****")
    }
}