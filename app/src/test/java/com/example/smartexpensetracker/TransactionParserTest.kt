package com.example.smartexpensetracker

import com.example.smartexpensetracker.data.parser.TransactionParser
import org.junit.Assert.*
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale

class TransactionParserTest {

    private val sdf = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.US)

    @Test
    fun testTmbBank50000Debit() {
        val text = "Your A/c No.XXXX5779 is debited with Rs.50,000.00 on 14-08-2026 11:24 AM and HDFC A/c linked to raisesmartlearnsolut.69514104@hdfcbank is credited (UPI Ref No.000000386901).Current AVBL bal is Rs.198.35 - TMB"
        val parsed = TransactionParser.parse(text, "AD-TMBANK-S")

        assertNotNull("Should parse TMB 50,000 debit SMS", parsed)
        assertEquals(50000.0, parsed!!.amount, 0.01)
        assertFalse("Should be marked as Expense (isIncome = false)", parsed.isIncome)
        assertEquals("XXXX5779", parsed.accountNumber)
        assertEquals("RAISESMARTLEARNSOLUT", parsed.merchant)
        assertEquals("000000386901", parsed.refId)
        assertEquals(198.35, parsed.availableBalance!!, 0.01)
        assertEquals("TMB", parsed.bankName)
        assertEquals("14-08-2026 11:24", sdf.format(parsed.timestamp))
    }

    @Test
    fun testTmbBank60DebitBharatPe() {
        val text = "Your A/c No.XXXX5779 is debited with Rs.60.00 on 14-08-2026 12:08 PM and FDRL A/c linked to bharatpe.9g0kOu7l3i381424@fbpe is credited (UPI Ref No.312428660435).Current AVBL bal is Rs.53.35 - TMB"
        val parsed = TransactionParser.parse(text, "AD-TMBANK-S")

        assertNotNull("Should parse TMB 60 Rs debit SMS", parsed)
        assertEquals(60.0, parsed!!.amount, 0.01)
        assertFalse("Should be marked as Expense", parsed.isIncome)
        assertEquals("XXXX5779", parsed.accountNumber)
        assertEquals("BharatPe Merchant", parsed.merchant)
        assertEquals("312428660435", parsed.refId)
        assertEquals(53.35, parsed.availableBalance!!, 0.01)
        assertEquals("14-08-2026 12:08", sdf.format(parsed.timestamp))
    }

    @Test
    fun testTmbBank160DebitPaytm() {
        val text = "Your A/c No.XXXX5779 is debited with Rs.160.00 on 04-08-2026 07:50 PM and YESB A/c linked to paytmqr5dbhs9@ptys is credited (UPI Ref No.000000347548).Current AVBL bal is Rs.991.75 - TMB"
        val parsed = TransactionParser.parse(text, "AD-TMBANK-S")

        assertNotNull("Should parse TMB 160 Rs debit SMS", parsed)
        assertEquals(160.0, parsed!!.amount, 0.01)
        assertFalse(parsed.isIncome)
        assertEquals("XXXX5779", parsed.accountNumber)
        assertEquals("Paytm QR Merchant", parsed.merchant)
        assertEquals("000000347548", parsed.refId)
        assertEquals(991.75, parsed.availableBalance!!, 0.01)
        assertEquals("04-08-2026 19:50", sdf.format(parsed.timestamp))
    }

    @Test
    fun testTmbBank18419IncomeNeft() {
        val text = "SB 305779 credited Rs.18,419.60 on 10-08-26 16:32..Info: NEFT-ICIC0000035-KARUPPASAMY PANDIYAN-IN72622238446542.---.Clr Bal Rs.19,150.35 - TMB"
        val parsed = TransactionParser.parse(text, "TMBANK")

        assertNotNull("Should parse TMB NEFT income SMS", parsed)
        assertEquals(18419.60, parsed!!.amount, 0.01)
        assertTrue("Should be marked as Income (isIncome = true)", parsed.isIncome)
        assertEquals("305779", parsed.accountNumber)
        assertEquals("KARUPPASAMY PANDIYAN", parsed.merchant)
        assertEquals(19150.35, parsed.availableBalance!!, 0.01)
        assertEquals("NEFT", parsed.paymentMethod)
        assertEquals("10-08-2026 16:32", sdf.format(parsed.timestamp))
    }

    @Test
    fun testTmbBank22000IncomeImps() {
        val text = "Dear Customer, Ur SB305779 is credited with Rs.22000.00 on 12-08-2026 15:44:09 by KARUPPASAMY PANDIYAN from FDRL bank via IMPS RefNo: 622415299744.. Avbl Bal Rs.40358.35 -TMB"
        val parsed = TransactionParser.parse(text, "TMBANK")

        assertNotNull("Should parse TMB IMPS income SMS", parsed)
        assertEquals(22000.0, parsed!!.amount, 0.01)
        assertTrue(parsed.isIncome)
        assertEquals("305779", parsed.accountNumber)
        assertEquals("KARUPPASAMY PANDIYAN", parsed.merchant)
        assertEquals("622415299744", parsed.refId)
        assertEquals(40358.35, parsed.availableBalance!!, 0.01)
        assertEquals("IMPS", parsed.paymentMethod)
        assertEquals("12-08-2026 15:44", sdf.format(parsed.timestamp))
    }

    @Test
    fun testTmbBank10000IncomeImps() {
        val text = "Dear Customer, Ur SB305779 is credited with Rs.10000.00 on 13-08-2026 12:05:27 by KARUPPASAMY PANDIYAN from FDRL bank via IMPS RefNo: 622512561264.. Avbl Bal Rs.50278.35 -TMB"
        val parsed = TransactionParser.parse(text, "TMBANK")

        assertNotNull("Should parse TMB IMPS income SMS", parsed)
        assertEquals(10000.0, parsed!!.amount, 0.01)
        assertTrue(parsed.isIncome)
        assertEquals("305779", parsed.accountNumber)
        assertEquals("KARUPPASAMY PANDIYAN", parsed.merchant)
        assertEquals("622512561264", parsed.refId)
        assertEquals(50278.35, parsed.availableBalance!!, 0.01)
        assertEquals("13-08-2026 12:05", sdf.format(parsed.timestamp))
    }

    @Test
    fun testTmbBank300IncomeImps() {
        val text = "Dear Customer, Ur SB305779 is credited with Rs.300.00 on 14-08-2026 19:38:14 by KARUPPASAMY PANDIYAN from FDRL bank via IMPS RefNo: 622619163616.. Avbl Bal Rs.353.35 -TMB"
        val parsed = TransactionParser.parse(text, "BG-TMBANK-S")

        assertNotNull("Should parse TMB IMPS 300 Rs income SMS", parsed)
        assertEquals(300.0, parsed!!.amount, 0.01)
        assertTrue(parsed.isIncome)
        assertEquals("305779", parsed.accountNumber)
        assertEquals("KARUPPASAMY PANDIYAN", parsed.merchant)
        assertEquals("622619163616", parsed.refId)
        assertEquals(353.35, parsed.availableBalance!!, 0.01)
        assertEquals("14-08-2026 19:38", sdf.format(parsed.timestamp))
    }

    @Test
    fun testPaytmGroceriesExpense() {
        val text = "Paid ₹85 to Sri Murugan Stores via Paytm"
        val parsed = TransactionParser.parse(text, "Paytm")

        assertNotNull(parsed)
        assertEquals(85.0, parsed!!.amount, 0.01)
        assertFalse(parsed.isIncome)
        assertEquals("SRI MURUGAN STORES", parsed.merchant)
    }

    @Test
    fun testSpamLoanSmsRejected() {
        val text = "Congratulations! Pre-approved loan of Rs 5,00,000 is ready for you. Click link to claim."
        val parsed = TransactionParser.parse(text, "VM-MYLOAN")

        assertNull("Spam loan SMS from unauthorized sender must be rejected", parsed)
    }

    @Test
    fun testUserSpecificAccountDebitMessage() {
        val text = "Your A/C XXXXX5779 has been debited by Rs.250.00 to SWIGGY. Avail Bal: Rs 12,300.00"
        val parsed = TransactionParser.parse(text)

        assertNotNull("Should successfully parse debit transaction", parsed)
        assertEquals(250.0, parsed!!.amount, 0.01)
        assertFalse("Should be marked as Expense (isIncome = false)", parsed.isIncome)
        assertEquals("SWIGGY", parsed.merchant)
        assertEquals("XXXXX5779", parsed.accountNumber)
        assertEquals(12300.0, parsed.availableBalance!!, 0.01)
    }

    @Test
    fun testUserSpecificAccountCreditMessage() {
        val text = "A/C XXXXX5779 Credited by Rs 5000.00 by SALARY. Avail Bal: Rs 45,000.00"
        val parsed = TransactionParser.parse(text)

        assertNotNull("Should successfully parse credit transaction", parsed)
        assertEquals(5000.0, parsed!!.amount, 0.01)
        assertTrue("Should be marked as Income (isIncome = true)", parsed.isIncome)
        assertEquals("XXXXX5779", parsed.accountNumber)
        assertEquals(45000.0, parsed.availableBalance!!, 0.01)
    }

    @Test
    fun testStandardUpiSwiggyDebit() {
        val text = "₹250 debited from your account via UPI to SWIGGY"
        val parsed = TransactionParser.parse(text)

        assertNotNull(parsed)
        assertEquals(250.0, parsed!!.amount, 0.01)
        assertFalse(parsed.isIncome)
        assertEquals("SWIGGY", parsed.merchant)
        assertEquals("UPI", parsed.paymentMethod)
    }

    @Test
    fun testUberDebit() {
        val text = "Rs.250 paid to UBER INDIA"
        val parsed = TransactionParser.parse(text)

        assertNotNull(parsed)
        assertEquals(250.0, parsed!!.amount, 0.01)
        assertFalse(parsed.isIncome)
        assertTrue(parsed.merchant.contains("UBER"))
    }

    @Test
    fun testAmazonInrDebit() {
        val text = "UPI transaction of INR 1,499.00 to AMAZON"
        val parsed = TransactionParser.parse(text)

        assertNotNull(parsed)
        assertEquals(1499.0, parsed!!.amount, 0.01)
        assertFalse(parsed.isIncome)
        assertEquals("AMAZON", parsed.merchant)
    }

    @Test
    fun testZomatoUpiRefId() {
        val text = "UPI payment successful: ₹250 to ZOMATO Ref: 321456987012"
        val parsed = TransactionParser.parse(text)

        assertNotNull(parsed)
        assertEquals(250.0, parsed!!.amount, 0.01)
        assertEquals("321456987012", parsed.refId)
    }

    @Test
    fun testUpiMoneyReceivedIncome() {
        val text = "Rs 1,500.00 received from JOHN via UPI Ref 998877"
        val parsed = TransactionParser.parse(text)

        assertNotNull(parsed)
        assertEquals(1500.0, parsed!!.amount, 0.01)
        assertTrue("Should be marked as Income", parsed.isIncome)
        assertEquals("UPI", parsed.paymentMethod)
    }

    @Test
    fun testPrivacySecurityOtpIgnored() {
        val text = "OTP for payment of Rs 250 is 492019. Do not share with anyone."
        val parsed = TransactionParser.parse(text)

        assertNull("OTP messages MUST be ignored for security", parsed)
    }
}
