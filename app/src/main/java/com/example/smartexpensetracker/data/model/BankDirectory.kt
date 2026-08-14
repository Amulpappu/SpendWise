package com.example.smartexpensetracker.data.model

import com.example.smartexpensetracker.R

data class BankInfo(
    val code: String,
    val name: String,
    val shortName: String,
    val category: String, // "Popular", "Public", "Private", "Payments"
    val isPopular: Boolean = false,
    val brandColor: Long = 0xFF008ECF,
    val accentColor: Long = 0xFFFFFFFF,
    val emblemSymbol: String = "🏛️",
    val logoResId: Int? = null
)

object BankDirectory {

    val ALL_BANKS = listOf(
        // ⭐ Popular Banks with Official Vector Drawables
        BankInfo("TMB", "Tamilnad Mercantile Bank (TMB)", "TMB", "Popular", isPopular = true, brandColor = 0xFF006837, accentColor = 0xFFFFD700, logoResId = R.drawable.ic_bank_tmb),
        BankInfo("SBI", "State Bank of India (SBI)", "SBI", "Popular", isPopular = true, brandColor = 0xFF008ECF, accentColor = 0xFFFFFFFF, logoResId = R.drawable.ic_bank_sbi),
        BankInfo("HDFC", "HDFC Bank", "HDFC", "Popular", isPopular = true, brandColor = 0xFF004C8F, accentColor = 0xFFED232A, logoResId = R.drawable.ic_bank_hdfc),
        BankInfo("ICICI", "ICICI Bank", "ICICI", "Popular", isPopular = true, brandColor = 0xFFF37021, accentColor = 0xFFB30B00, logoResId = R.drawable.ic_bank_icici),
        BankInfo("AXIS", "Axis Bank", "Axis", "Popular", isPopular = true, brandColor = 0xFF861F41, accentColor = 0xFF97144D, logoResId = R.drawable.ic_bank_axis),
        BankInfo("KOTAK", "Kotak Mahindra Bank", "Kotak", "Popular", isPopular = true, brandColor = 0xFFED1C24, accentColor = 0xFF002D62, logoResId = R.drawable.ic_bank_kotak),

        // 🏛️ Public Sector Banks
        BankInfo("BOB", "Bank of Baroda", "BOB", "Public", isPopular = true, brandColor = 0xFFF26522, accentColor = 0xFFFFFFFF, logoResId = R.drawable.ic_bank_bob),
        BankInfo("CANARA", "Canara Bank", "Canara", "Public", isPopular = true, brandColor = 0xFF0091DA, accentColor = 0xFFFDC82F, logoResId = R.drawable.ic_bank_canara),
        BankInfo("PNB", "Punjab National Bank (PNB)", "PNB", "Public", brandColor = 0xFFA20000, accentColor = 0xFFFFD100, logoResId = R.drawable.ic_bank_pnb),
        BankInfo("UNION", "Union Bank of India", "Union Bank", "Public", brandColor = 0xFF003399, accentColor = 0xFFED1C24),
        BankInfo("INDIAN", "Indian Bank", "Indian Bank", "Public", brandColor = 0xFF00205B, accentColor = 0xFFEE3124),
        BankInfo("IOB", "Indian Overseas Bank (IOB)", "IOB", "Public", brandColor = 0xFF005696, accentColor = 0xFFFFC20E),
        BankInfo("BOI", "Bank of India (BOI)", "BOI", "Public", brandColor = 0xFFE31837, accentColor = 0xFF003366),
        BankInfo("CENTRAL", "Central Bank of India", "Central Bank", "Public", brandColor = 0xFF003399, accentColor = 0xFFE31B23),
        BankInfo("UCO", "UCO Bank", "UCO", "Public", brandColor = 0xFF0054A6, accentColor = 0xFFF58220),
        BankInfo("MAHABANK", "Bank of Maharashtra", "Maha Bank", "Public", brandColor = 0xFF004B87, accentColor = 0xFFFDB913),

        // 🏢 Private Sector Banks
        BankInfo("FEDERAL", "Federal Bank", "Federal", "Private", brandColor = 0xFF003366, accentColor = 0xFFFFCC00),
        BankInfo("INDUSIND", "IndusInd Bank", "IndusInd", "Private", brandColor = 0xFF800000, accentColor = 0xFFFDB813),
        BankInfo("YESB", "Yes Bank", "Yes Bank", "Private", brandColor = 0xFF004B87, accentColor = 0xFFED1C24),
        BankInfo("IDFC", "IDFC FIRST Bank", "IDFC FIRST", "Private", brandColor = 0xFF9B1B30, accentColor = 0xFFC2A25D),
        BankInfo("RBL", "RBL Bank", "RBL", "Private", brandColor = 0xFF003366, accentColor = 0xFFE31B23),
        BankInfo("KVB", "Karur Vysya Bank (KVB)", "KVB", "Private", brandColor = 0xFF003366, accentColor = 0xFF00A651),
        BankInfo("SIB", "South Indian Bank", "SIB", "Private", brandColor = 0xFF990000, accentColor = 0xFFFFCC00),
        BankInfo("CUB", "City Union Bank (CUB)", "CUB", "Private", brandColor = 0xFF002B49, accentColor = 0xFFED1C24),
        BankInfo("BANDHAN", "Bandhan Bank", "Bandhan", "Private", brandColor = 0xFF003366, accentColor = 0xFFED1C24),
        BankInfo("J_AND_K", "Jammu & Kashmir Bank", "J&K Bank", "Private", brandColor = 0xFF005BA6, accentColor = 0xFFFFD100),

        // 📱 Payment & Digital Banks
        BankInfo("PAYTM", "Paytm Payments Bank", "Paytm", "Payments", isPopular = true, brandColor = 0xFF002E6E, accentColor = 0xFF00BAF2, logoResId = R.drawable.ic_bank_paytm),
        BankInfo("AIRTEL", "Airtel Payments Bank", "Airtel Bank", "Payments", brandColor = 0xFFE40000, accentColor = 0xFFFFFFFF),
        BankInfo("IPPB", "India Post Payments Bank (IPPB)", "IPPB", "Payments", brandColor = 0xFF990000, accentColor = 0xFFFFCC00),
        BankInfo("JIO", "Jio Payments Bank", "Jio Bank", "Payments", brandColor = 0xFF0A3C91, accentColor = 0xFFE31837)
    )

    fun searchBanks(query: String, categoryFilter: String? = null): List<BankInfo> {
        val q = query.trim().lowercase()
        return ALL_BANKS.filter { bank ->
            val matchesCategory = categoryFilter == null || categoryFilter == "All" ||
                    (categoryFilter == "Popular" && bank.isPopular) ||
                    bank.category.equals(categoryFilter, ignoreCase = true)
            val matchesQuery = q.isEmpty() ||
                    bank.name.lowercase().contains(q) ||
                    bank.shortName.lowercase().contains(q) ||
                    bank.code.lowercase().contains(q)
            matchesCategory && matchesQuery
        }
    }

    fun getBankByCodeOrName(nameOrCode: String): BankInfo? {
        val q = nameOrCode.trim().lowercase()
        return ALL_BANKS.find { 
            it.code.equals(q, ignoreCase = true) || 
            it.name.contains(q, ignoreCase = true) || 
            it.shortName.contains(q, ignoreCase = true) 
        }
    }
}
