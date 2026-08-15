package com.example.smartexpensetracker.data.parser

import com.example.smartexpensetracker.data.local.dao.MerchantRuleDao
import com.example.smartexpensetracker.data.local.dao.TransactionDao
import com.example.smartexpensetracker.data.local.entity.TransactionEntity
import java.util.regex.Pattern

class DuplicateDetector(private val transactionDao: TransactionDao) {

    suspend fun checkDuplicate(parsed: ParsedTransaction): Pair<Boolean, Long?> {
        // 0. Zero amount rejection
        if (parsed.amount <= 0.0) {
            return Pair(true, null)
        }

        // 1. Ref ID match (Primary unique key)
        if (!parsed.refId.isNullOrEmpty()) {
            val existing = transactionDao.findByRefId(parsed.refId)
            if (existing != null) {
                return Pair(true, existing.id)
            }
        }

        // 2. Exact carrier duplicate match: only within 2 minutes window with same amount and exact raw text / merchant
        val windowMs = 2 * 60 * 1000L // 2 minutes carrier duplication window
        val minTime = parsed.timestamp - windowMs
        val maxTime = parsed.timestamp + windowMs

        val recents = transactionDao.findRecentTransactions(minTime, maxTime)
        val exactDuplicate = recents.firstOrNull {
            it.amount == parsed.amount &&
            it.isIncome == parsed.isIncome &&
            (it.rawText == parsed.rawSanitizedText || it.merchant.equals(parsed.merchant, ignoreCase = true))
        }

        if (exactDuplicate != null) {
            return Pair(true, exactDuplicate.id)
        }

        return Pair(false, null)
    }
}

class Categorizer(private val merchantRuleDao: MerchantRuleDao) {

    suspend fun categorize(merchant: String, rawText: String = "", isIncome: Boolean = false): String {
        val upperMerchant = merchant.uppercase().trim()
        val upperRaw = rawText.uppercase()

        // If transaction is Income, match income categories
        if (isIncome) {
            return when {
                containsWord(upperRaw, listOf("SALARY", "PAYROLL", "WAGE", "STIPEND", "BONUS")) -> "Salary"
                containsWord(upperRaw, listOf("CASHBACK", "REWARD", "SCRATCH CARD")) -> "Cashback"
                containsWord(upperRaw, listOf("REFUND", "REVERSAL", "RETURN")) -> "Refund"
                containsWord(upperRaw, listOf("DIVIDEND", "INTEREST", "MUTUAL FUND", "ZERODHA", "GROWW", "UPSTOX")) -> "Investment"
                else -> "Income"
            }
        }

        // 1. Check custom user rules & learned rules from database
        val rule = merchantRuleDao.findRuleForMerchant(upperMerchant)
        if (rule != null) {
            return rule.categoryName
        }

        // Check if any rule pattern is contained in merchant
        val allRules = merchantRuleDao.getAllRulesSync()
        for (r in allRules) {
            if (upperMerchant.contains(r.merchantPattern.uppercase())) {
                return r.categoryName
            }
        }

        // 2. Keyword matching with strict word boundaries (ignoring bank noise like "Current")
        val sanitizedRaw = upperRaw.replace("CURRENT", "").replace("AVBL", "").replace("BAL", "")
        val combinedText = "$upperMerchant $sanitizedRaw"

        return when {
            // Friends, Family & Direct P2P Personal Transfers
            isPersonalFriendTransfer(upperMerchant, combinedText) -> "Friends"

            // Groceries & Daily Needs
            containsWord(combinedText, listOf("GROCERY", "GROCERIES", "BLINKIT", "ZEPTO", "INSTAMART", "BIGBASKET", "DMART", "NATURES BASKET", "SUPERMARKET", "PROVISION", "KIRANA", "VEGETABLE", "FRUITS", "DAIRY", "MILK", "COUNTRY DELIGHT", "SPENCERS", "MORE RETAIL", "STORES", "STORE", "MART")) -> "Groceries"
            containsWord(combinedText, listOf("GROCERY", "GROCERIES", "BLINKIT", "ZEPTO", "INSTAMART", "BIGBASKET", "DMART", "NATURES BASKET", "SUPERMARKET", "PROVISION", "KIRANA", "VEGETABLE", "FRUITS", "DAIRY", "MILK", "COUNTRY DELIGHT", "SPENCERS", "MORE RETAIL", "STORES", "STORE", "MART")) -> "Groceries"

            // Food, Dining & Snacks
            containsWord(combinedText, listOf("SWIGGY", "ZOMATO", "MCDONALD", "DOMINO", "STARBUCKS", "KFC", "PIZZA", "BURGER", "RESTAURANT", "CAFE", "DINING", "BAKERY", "DHABA", "FOOD", "EATS", "CHAI", "TEA", "SWEETS", "BIRYANI", "CANTEEN", "SNACKS", "HOTEL")) -> "Food"

            // Recharges, Utilities & Bills
            containsWord(combinedText, listOf("RECHARGE", "PREPAID", "POSTPAID", "AIRTEL", "JIO", "VI", "BSNL", "ELECTRICITY", "BESCOM", "TSSPDCL", "WATER", "GAS", "CYLINDER", "INDANE", "HP GAS", "BHARAT GAS", "BROADBAND", "ACT FIBERNET", "WIFI", "DTH", "TATA PLAY", "DISH TV", "SUN DIRECT", "FASTAG", "BILL", "UTILITY", "PAYBIL")) -> "Recharge & Bills"

            // Transport, Fuel & Cabs
            containsWord(combinedText, listOf("UBER", "OLA", "RAPIDO", "METRO", "IRCTC", "PETROL", "FUEL", "SHELL", "HPCL", "BPCL", "INDIAN OIL", "AUTO", "CAB", "PARKING", "TOLL")) -> "Transport"

            // Shopping & E-Commerce
            containsWord(combinedText, listOf("AMAZON", "FLIPKART", "MYNTRA", "AJIO", "MEESHO", "ZUDIO", "RELIANCE DIGITAL", "CROMA", "CLOTHES", "FASHION", "MALL", "LIFESTYLE", "MAX FASHION", "SHOPPERS STOP", "DECATHLON", "NYKAA", "PURPLLE", "SHOPPING", "EKART")) -> "Shopping"

            // Entertainment & OTT
            containsWord(combinedText, listOf("NETFLIX", "SPOTIFY", "HOTSTAR", "PRIME VIDEO", "BOOKMYSHOW", "PVR", "INOX", "MOVIE", "CINEMA", "ENTERTAINMENT")) -> "Entertainment"

            // Gaming
            containsWord(combinedText, listOf("STEAM", "PLAYSTATION", "XBOX", "NINTENDO", "RIOT", "EPIC GAMES", "BGMI", "GAMING")) -> "Gaming"

            // Education & Courses
            containsWord(combinedText, listOf("UDEMY", "COURSERA", "EDX", "COLLEGE", "SCHOOL", "UNIVERSITY", "FEE", "BOOKS", "TUITION", "EDUCATION", "RAISESMARTLEARNSOLUT", "LEARN")) -> "Education"

            // Recurring Subscriptions
            containsWord(combinedText, listOf("SUBSCRIPTION", "APPLE", "ICLOUD", "GOOGLE ONE", "PATREON", "MEMBERSHIP")) -> "Subscriptions"

            // Travel, Flights & Booking
            containsWord(combinedText, listOf("MAKE MY TRIP", "GOIBIBO", "INDIGO", "AIR INDIA", "FLIGHT", "BOOKING", "TRAVEL", "RESORT")) -> "Travel"

            // Medical, Pharmacy & Health
            containsWord(combinedWord(combinedText), listOf("APOLLO", "PHARMEASY", "1MG", "HOSPITAL", "CLINIC", "PHARMACY", "DOCTOR", "LAB", "MEDICAL", "MEDICINE")) -> "Medical"

            // Technology & Software
            containsWord(combinedText, listOf("MICROSOFT", "AWS", "CLOUD", "GITHUB", "JETBRAINS", "HOSTING", "SOFTWARE", "TECHNOLOGY")) -> "Technology"

            // Home & Rent (Strict whole words only)
            containsStrictWord(combinedText, listOf("HOUSE RENT", "ROOM RENT", "FLAT RENT", "MAINTENANCE FEE", "FURNITURE", "IKEA", "URBAN COMPANY")) -> "Home"

            else -> "Other"
        }
    }

    private fun isPersonalFriendTransfer(merchant: String, combinedText: String): Boolean {
        // Direct keywords
        if (containsWord(combinedText, listOf("FRIEND", "FRIENDS", "FAMILY", "BROTHER", "SISTER", "MOM", "DAD", "COLLEAGUE", "ROOMMATE", "TRANSFER TO", "SENT TO", "PERSONAL"))) {
            return true
        }

        // Check if commercial / merchant keywords exist
        val commercialKeywords = listOf("MART", "STORE", "SHOP", "RESTAURANT", "HOTEL", "CAFE", "BAKERY", "TEA", "CHAI", "SWEETS", "SWIGGY", "ZOMATO", "BLINKIT", "ZEPTO", "AMAZON", "FLIPKART", "EKART", "PAYTMQR", "BHARATPE", "MERCHANT", "PVT", "LTD", "SOLUT", "SERVICES", "PETROL", "BILLS", "RECHARGE", "AIRTEL", "JIO")
        for (ck in commercialKeywords) {
            if (combinedText.contains(ck, ignoreCase = true)) {
                return false
            }
        }

        // Phone number based VPA or contact (e.g. 9342934661, 9342934661@upi, q130896036@ybl)
        if (merchant.matches(Regex("^[0-9]{10}.*")) || merchant.matches(Regex("^[A-Za-z0-9._-]+@(UPI|YBL|OKAXIS|OKSBI|OKICICI|OKHDFCBANK|PAYTM|IBL)$", RegexOption.IGNORE_CASE))) {
            return true
        }

        // Personal human name with 2 or more words (e.g. "KARUPPASAMY PANDIYAN", "REJANDEREETA", "KAVIYAKAVIYA")
        val cleanLettersOnly = merchant.replace(Regex("[^A-Za-z\\s]"), "").trim()
        if (cleanLettersOnly.length >= 4 && !cleanLettersOnly.contains("BANK") && !cleanLettersOnly.contains("ACC")) {
            val words = cleanLettersOnly.split(Regex("\\s+"))
            if (words.size >= 2 || cleanLettersOnly.length >= 8) {
                return true
            }
        }

        return false
    }

    private fun containsWord(text: String, keywords: List<String>): Boolean {
        for (kw in keywords) {
            val pattern = Pattern.compile("\\b" + Pattern.quote(kw) + "\\b", Pattern.CASE_INSENSITIVE)
            if (pattern.matcher(text).find()) {
                return true
            }
        }
        return false
    }

    private fun containsStrictWord(text: String, keywords: List<String>): Boolean {
        for (kw in keywords) {
            if (text.contains(kw, ignoreCase = true)) return true
        }
        return false
    }

    private fun combinedWord(text: String): String = text
}
