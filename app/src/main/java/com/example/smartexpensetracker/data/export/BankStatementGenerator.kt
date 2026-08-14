package com.example.smartexpensetracker.data.export

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.smartexpensetracker.data.local.entity.TransactionEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object BankStatementGenerator {

    data class StatementCustomerInfo(
        val customerName: String = "Lohith",
        val accountNumber: String = "381 100050 305779",
        val bankName: String = "Tamilnad Mercantile Bank (TMB)",
        val mobileNumber: String = "+91 6379982741",
        val email: String = "lohith@spendwise.app",
        val periodText: String = "Current Period",
        val openingBalance: Double = 0.0,
        val closingBalance: Double = 353.35,
        val password: String = "LOHI5779"
    )

    fun getPasswordHint(
        customerName: String,
        mobileNumber: String,
        accountNumber: String = "",
        pattern: String = "NAME_ACCOUNT",
        customPassword: String = ""
    ): String {
        return com.example.smartexpensetracker.data.local.UserProfileManager.computePassword(
            userName = customerName,
            mobileNumber = mobileNumber,
            accountNumber = accountNumber,
            pattern = pattern,
            customPass = customPassword
        )
    }

    /**
     * Generates an official Bank Statement PDF in A4 format (595 x 842 pt).
     */
    fun generatePdfStatement(
        context: Context,
        transactions: List<TransactionEntity>,
        info: StatementCustomerInfo
    ): File {
        val pdfDoc = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842

        val validTxns = transactions.filter { !it.isDuplicate }.sortedBy { it.timestamp }
        val totalIncome = validTxns.filter { it.isIncome }.sumOf { it.amount }
        val totalExpense = validTxns.filter { !it.isIncome }.sumOf { it.amount }

        val sdfDate = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        val sdfTime = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val sdfFull = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

        val password = info.password.ifBlank { getPasswordHint(info.customerName, info.mobileNumber, info.accountNumber) }

        // Page paint definitions
        val paint = Paint().apply { isAntiAlias = true }
        val textPaint = Paint().apply { isAntiAlias = true }

        val rowsPerPageFirst = 18
        val rowsPerPageNext = 26
        val totalPages = if (validTxns.size <= rowsPerPageFirst) 1 else 1 + ((validTxns.size - rowsPerPageFirst + rowsPerPageNext - 1) / rowsPerPageNext)

        var currentTxnIdx = 0

        for (pageIndex in 1..totalPages) {
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex).create()
            val page = pdfDoc.startPage(pageInfo)
            val canvas = page.canvas

            // Page Background
            canvas.drawColor(Color.WHITE)

            if (pageIndex == 1) {
                // --- FIRST PAGE HEADER ---
                // Top Brand Bar
                paint.color = Color.parseColor("#0F2027")
                canvas.drawRect(0f, 0f, pageWidth.toFloat(), 95f, paint)

                // Accent Line
                paint.color = Color.parseColor("#00D2D3")
                canvas.drawRect(0f, 95f, pageWidth.toFloat(), 98f, paint)

                // Render Real SpendWise App Logo
                val logoDrawable = try {
                    BitmapFactory.decodeResource(context.resources, com.example.smartexpensetracker.R.drawable.ic_spendwise_logo)
                        ?: BitmapFactory.decodeResource(context.resources, com.example.smartexpensetracker.R.mipmap.ic_launcher)
                } catch (e: Exception) {
                    null
                }

                if (logoDrawable != null) {
                    val logoSize = 60
                    val scaledLogo = Bitmap.createScaledBitmap(logoDrawable, logoSize, logoSize, true)
                    val output = Bitmap.createBitmap(logoSize, logoSize, Bitmap.Config.ARGB_8888)
                    val roundCanvas = Canvas(output)
                    val roundPaint = Paint().apply { isAntiAlias = true }
                    val rect = Rect(0, 0, logoSize, logoSize)
                    val rectF = RectF(rect)
                    roundCanvas.drawRoundRect(rectF, 14f, 14f, roundPaint)
                    roundPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
                    roundCanvas.drawBitmap(scaledLogo, rect, rect, roundPaint)

                    canvas.drawBitmap(output, 24f, 18f, paint)
                } else {
                    paint.color = Color.parseColor("#00D2D3")
                    canvas.drawRoundRect(24f, 18f, 84f, 78f, 14f, 14f, paint)
                    textPaint.color = Color.parseColor("#0F2027")
                    textPaint.textSize = 20f
                    textPaint.isFakeBoldText = true
                    canvas.drawText("SW", 38f, 53f, textPaint)
                }

                // Title
                textPaint.color = Color.WHITE
                textPaint.textSize = 17f
                textPaint.isFakeBoldText = true
                canvas.drawText("SPENDWISE ACCOUNT STATEMENT", 94f, 40f, textPaint)

                textPaint.color = Color.parseColor("#A0AEC0")
                textPaint.textSize = 9.5f
                textPaint.isFakeBoldText = false
                canvas.drawText("${info.bankName} • Comprehensive Official Ledger", 94f, 56f, textPaint)
                canvas.drawText("Generated: ${sdfFull.format(Date())}", 94f, 70f, textPaint)

                // Verified Stamp on Right
                paint.color = Color.parseColor("#1A365D")
                canvas.drawRoundRect(pageWidth - 145f, 24f, pageWidth - 24f, 70f, 8f, 8f, paint)
                textPaint.color = Color.parseColor("#48BB78")
                textPaint.textSize = 9f
                textPaint.isFakeBoldText = true
                canvas.drawText("● VERIFIED STATEMENT", pageWidth - 138f, 42f, textPaint)
                textPaint.color = Color.WHITE
                textPaint.textSize = 8f
                textPaint.isFakeBoldText = false
                canvas.drawText("Passcode: $password", pageWidth - 138f, 58f, textPaint)

                // --- CUSTOMER & ACCOUNT INFO CARD ---
                var y = 110f
                paint.color = Color.parseColor("#F7FAFC")
                canvas.drawRoundRect(24f, y, pageWidth - 24f, y + 80f, 8f, 8f, paint)
                paint.color = Color.parseColor("#E2E8F0")
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 1f
                canvas.drawRoundRect(24f, y, pageWidth - 24f, y + 80f, 8f, 8f, paint)
                paint.style = Paint.Style.FILL

                // Column 1: Account Holder & Details
                textPaint.color = Color.parseColor("#718096")
                textPaint.textSize = 8f
                textPaint.isFakeBoldText = true
                canvas.drawText("ACCOUNT HOLDER", 36f, y + 18f, textPaint)
                textPaint.color = Color.parseColor("#2D3748")
                textPaint.textSize = 12f
                canvas.drawText(info.customerName.uppercase(), 36f, y + 34f, textPaint)

                textPaint.color = Color.parseColor("#718096")
                textPaint.textSize = 8f
                canvas.drawText("REGISTERED MOBILE", 36f, y + 52f, textPaint)
                textPaint.color = Color.parseColor("#2D3748")
                textPaint.textSize = 10f
                canvas.drawText(info.mobileNumber, 36f, y + 66f, textPaint)

                // Column 2: Account Number & Period
                textPaint.color = Color.parseColor("#718096")
                textPaint.textSize = 8f
                canvas.drawText("ACCOUNT NUMBER", 210f, y + 18f, textPaint)
                textPaint.color = Color.parseColor("#2D3748")
                textPaint.textSize = 12f
                canvas.drawText(info.accountNumber, 210f, y + 34f, textPaint)

                textPaint.color = Color.parseColor("#718096")
                textPaint.textSize = 8f
                canvas.drawText("STATEMENT PERIOD", 210f, y + 52f, textPaint)
                textPaint.color = Color.parseColor("#2D3748")
                textPaint.textSize = 10f
                canvas.drawText(info.periodText, 210f, y + 66f, textPaint)

                // Column 3: Closing Balance Box
                paint.color = Color.parseColor("#EBF8FF")
                canvas.drawRoundRect(385f, y + 10f, pageWidth - 36f, y + 70f, 6f, 6f, paint)
                textPaint.color = Color.parseColor("#2B6CB0")
                textPaint.textSize = 8f
                canvas.drawText("AVAILABLE BALANCE", 395f, y + 26f, textPaint)
                textPaint.color = Color.parseColor("#2C5282")
                textPaint.textSize = 15f
                textPaint.isFakeBoldText = true
                canvas.drawText("₹${String.format(Locale.getDefault(), "%,.2f", info.closingBalance)}", 395f, y + 48f, textPaint)

                // --- FINANCIAL KPI SUMMARY ---
                y += 90f
                val kpiBoxWidth = (pageWidth - 48f - 16f) / 3f

                // Total Income (Cr)
                paint.color = Color.parseColor("#F0FFF4")
                canvas.drawRoundRect(24f, y, 24f + kpiBoxWidth, y + 42f, 6f, 6f, paint)
                textPaint.color = Color.parseColor("#276749")
                textPaint.textSize = 8f
                textPaint.isFakeBoldText = true
                canvas.drawText("TOTAL CREDITED (INCOME)", 32f, y + 15f, textPaint)
                textPaint.textSize = 12f
                canvas.drawText("+₹${String.format(Locale.getDefault(), "%,.2f", totalIncome)}", 32f, y + 32f, textPaint)

                // Total Expense (Dr)
                val kpi2X = 24f + kpiBoxWidth + 8f
                paint.color = Color.parseColor("#FFF5F5")
                canvas.drawRoundRect(kpi2X, y, kpi2X + kpiBoxWidth, y + 42f, 6f, 6f, paint)
                textPaint.color = Color.parseColor("#9B2C2C")
                textPaint.textSize = 8f
                textPaint.isFakeBoldText = true
                canvas.drawText("TOTAL DEBITED (EXPENSES)", kpi2X + 8f, y + 15f, textPaint)
                textPaint.textSize = 12f
                canvas.drawText("-₹${String.format(Locale.getDefault(), "%,.2f", totalExpense)}", kpi2X + 8f, y + 32f, textPaint)

                // Net Flow
                val kpi3X = kpi2X + kpiBoxWidth + 8f
                val netFlow = totalIncome - totalExpense
                paint.color = Color.parseColor("#EDF2F7")
                canvas.drawRoundRect(kpi3X, y, kpi3X + kpiBoxWidth, y + 42f, 6f, 6f, paint)
                textPaint.color = Color.parseColor("#4A5568")
                textPaint.textSize = 8f
                textPaint.isFakeBoldText = true
                canvas.drawText("NET SAVINGS / FLOW", kpi3X + 8f, y + 15f, textPaint)
                textPaint.textSize = 12f
                textPaint.color = if (netFlow >= 0) Color.parseColor("#276749") else Color.parseColor("#9B2C2C")
                canvas.drawText("${if (netFlow >= 0) "+" else ""}₹${String.format(Locale.getDefault(), "%,.2f", netFlow)}", kpi3X + 8f, y + 32f, textPaint)

                // --- TRANSACTION TABLE HEADER ---
                y += 54f
                paint.color = Color.parseColor("#1A2E3B")
                canvas.drawRect(24f, y, pageWidth - 24f, y + 20f, paint)

                textPaint.color = Color.WHITE
                textPaint.textSize = 8f
                textPaint.isFakeBoldText = true
                canvas.drawText("DATE & TIME", 30f, y + 13f, textPaint)
                canvas.drawText("TRANSACTION PARTICULARS", 115f, y + 13f, textPaint)
                canvas.drawText("MODE / REF", 260f, y + 13f, textPaint)
                canvas.drawText("DEBITS (DR)", 350f, y + 13f, textPaint)
                canvas.drawText("CREDITS (CR)", 430f, y + 13f, textPaint)
                canvas.drawText("BALANCE", 510f, y + 13f, textPaint)

                y += 20f
                val maxRows = rowsPerPageFirst
                var rowsDrawn = 0

                while (currentTxnIdx < validTxns.size && rowsDrawn < maxRows) {
                    val txn = validTxns[currentTxnIdx]
                    val isAlt = rowsDrawn % 2 == 1

                    if (isAlt) {
                        paint.color = Color.parseColor("#F8FAFC")
                        canvas.drawRect(24f, y, pageWidth - 24f, y + 22f, paint)
                    }

                    // Divider Line
                    paint.color = Color.parseColor("#EDF2F7")
                    canvas.drawLine(24f, y + 22f, pageWidth - 24f, y + 22f, paint)

                    // Date & Time
                    textPaint.color = Color.parseColor("#2D3748")
                    textPaint.textSize = 7.5f
                    textPaint.isFakeBoldText = true
                    canvas.drawText(sdfDate.format(Date(txn.timestamp)), 30f, y + 10f, textPaint)
                    textPaint.color = Color.parseColor("#718096")
                    textPaint.textSize = 6.5f
                    textPaint.isFakeBoldText = false
                    canvas.drawText(sdfTime.format(Date(txn.timestamp)), 30f, y + 19f, textPaint)

                    // Description / Merchant (Trimming for PDF width)
                    textPaint.color = Color.parseColor("#1A202C")
                    textPaint.textSize = 8f
                    textPaint.isFakeBoldText = true
                    val merchantStr = if (txn.merchant.length > 28) txn.merchant.take(26) + ".." else txn.merchant
                    canvas.drawText(merchantStr, 115f, y + 10f, textPaint)

                    textPaint.color = Color.parseColor("#718096")
                    textPaint.textSize = 6.5f
                    textPaint.isFakeBoldText = false
                    canvas.drawText(txn.category, 115f, y + 19f, textPaint)

                    // Mode / Ref
                    textPaint.color = Color.parseColor("#4A5568")
                    textPaint.textSize = 7f
                    val refStr = txn.refId?.let { if (it.length > 12) it.take(10) + ".." else it } ?: "N/A"
                    canvas.drawText("${txn.paymentMethod}", 260f, y + 10f, textPaint)
                    textPaint.textSize = 6f
                    canvas.drawText("Ref: $refStr", 260f, y + 19f, textPaint)

                    // Debit / Credit
                    if (!txn.isIncome) {
                        textPaint.color = Color.parseColor("#E53E3E")
                        textPaint.textSize = 8f
                        textPaint.isFakeBoldText = true
                        canvas.drawText("₹${String.format(Locale.getDefault(), "%,.2f", txn.amount)}", 350f, y + 14f, textPaint)
                        textPaint.color = Color.parseColor("#A0AEC0")
                        textPaint.textSize = 8f
                        textPaint.isFakeBoldText = false
                        canvas.drawText("-", 450f, y + 14f, textPaint)
                    } else {
                        textPaint.color = Color.parseColor("#A0AEC0")
                        textPaint.textSize = 8f
                        textPaint.isFakeBoldText = false
                        canvas.drawText("-", 370f, y + 14f, textPaint)
                        textPaint.color = Color.parseColor("#38A169")
                        textPaint.textSize = 8f
                        textPaint.isFakeBoldText = true
                        canvas.drawText("₹${String.format(Locale.getDefault(), "%,.2f", txn.amount)}", 430f, y + 14f, textPaint)
                    }

                    // Balance
                    textPaint.color = Color.parseColor("#2D3748")
                    textPaint.textSize = 7.5f
                    textPaint.isFakeBoldText = true
                    val balStr = txn.accountBalance?.let { "₹${String.format(Locale.getDefault(), "%,.2f", it)}" } ?: "-"
                    canvas.drawText(balStr, 510f, y + 14f, textPaint)

                    y += 22f
                    rowsDrawn++
                    currentTxnIdx++
                }

            } else {
                // --- SUBSEQUENT PAGES ---
                paint.color = Color.parseColor("#0F2027")
                canvas.drawRect(0f, 0f, pageWidth.toFloat(), 35f, paint)

                textPaint.color = Color.WHITE
                textPaint.textSize = 10f
                textPaint.isFakeBoldText = true
                canvas.drawText("SPENDWISE STATEMENT (Contd.) • ${info.customerName.uppercase()} • ${info.accountNumber}", 24f, 22f, textPaint)

                var y = 48f
                paint.color = Color.parseColor("#1A2E3B")
                canvas.drawRect(24f, y, pageWidth - 24f, y + 20f, paint)

                textPaint.color = Color.WHITE
                textPaint.textSize = 8f
                textPaint.isFakeBoldText = true
                canvas.drawText("DATE & TIME", 30f, y + 13f, textPaint)
                canvas.drawText("TRANSACTION PARTICULARS", 115f, y + 13f, textPaint)
                canvas.drawText("MODE / REF", 260f, y + 13f, textPaint)
                canvas.drawText("DEBITS (DR)", 350f, y + 13f, textPaint)
                canvas.drawText("CREDITS (CR)", 430f, y + 13f, textPaint)
                canvas.drawText("BALANCE", 510f, y + 13f, textPaint)

                y += 20f
                var rowsDrawn = 0
                val maxRows = rowsPerPageNext

                while (currentTxnIdx < validTxns.size && rowsDrawn < maxRows) {
                    val txn = validTxns[currentTxnIdx]
                    val isAlt = rowsDrawn % 2 == 1

                    if (isAlt) {
                        paint.color = Color.parseColor("#F8FAFC")
                        canvas.drawRect(24f, y, pageWidth - 24f, y + 22f, paint)
                    }

                    paint.color = Color.parseColor("#EDF2F7")
                    canvas.drawLine(24f, y + 22f, pageWidth - 24f, y + 22f, paint)

                    textPaint.color = Color.parseColor("#2D3748")
                    textPaint.textSize = 7.5f
                    textPaint.isFakeBoldText = true
                    canvas.drawText(sdfDate.format(Date(txn.timestamp)), 30f, y + 10f, textPaint)
                    textPaint.color = Color.parseColor("#718096")
                    textPaint.textSize = 6.5f
                    textPaint.isFakeBoldText = false
                    canvas.drawText(sdfTime.format(Date(txn.timestamp)), 30f, y + 19f, textPaint)

                    textPaint.color = Color.parseColor("#1A202C")
                    textPaint.textSize = 8f
                    textPaint.isFakeBoldText = true
                    val merchantStr = if (txn.merchant.length > 28) txn.merchant.take(26) + ".." else txn.merchant
                    canvas.drawText(merchantStr, 115f, y + 10f, textPaint)

                    textPaint.color = Color.parseColor("#718096")
                    textPaint.textSize = 6.5f
                    textPaint.isFakeBoldText = false
                    canvas.drawText(txn.category, 115f, y + 19f, textPaint)

                    textPaint.color = Color.parseColor("#4A5568")
                    textPaint.textSize = 7f
                    val refStr = txn.refId?.let { if (it.length > 12) it.take(10) + ".." else it } ?: "N/A"
                    canvas.drawText("${txn.paymentMethod}", 260f, y + 10f, textPaint)
                    textPaint.textSize = 6f
                    canvas.drawText("Ref: $refStr", 260f, y + 19f, textPaint)

                    if (!txn.isIncome) {
                        textPaint.color = Color.parseColor("#E53E3E")
                        textPaint.textSize = 8f
                        textPaint.isFakeBoldText = true
                        canvas.drawText("₹${String.format(Locale.getDefault(), "%,.2f", txn.amount)}", 350f, y + 14f, textPaint)
                        textPaint.color = Color.parseColor("#A0AEC0")
                        textPaint.textSize = 8f
                        textPaint.isFakeBoldText = false
                        canvas.drawText("-", 450f, y + 14f, textPaint)
                    } else {
                        textPaint.color = Color.parseColor("#A0AEC0")
                        textPaint.textSize = 8f
                        textPaint.isFakeBoldText = false
                        canvas.drawText("-", 370f, y + 14f, textPaint)
                        textPaint.color = Color.parseColor("#38A169")
                        textPaint.textSize = 8f
                        textPaint.isFakeBoldText = true
                        canvas.drawText("₹${String.format(Locale.getDefault(), "%,.2f", txn.amount)}", 430f, y + 14f, textPaint)
                    }

                    textPaint.color = Color.parseColor("#2D3748")
                    textPaint.textSize = 7.5f
                    textPaint.isFakeBoldText = true
                    val balStr = txn.accountBalance?.let { "₹${String.format(Locale.getDefault(), "%,.2f", it)}" } ?: "-"
                    canvas.drawText(balStr, 510f, y + 14f, textPaint)

                    y += 22f
                    rowsDrawn++
                    currentTxnIdx++
                }
            }

            // --- PAGE FOOTER ---
            paint.color = Color.parseColor("#EDF2F7")
            canvas.drawLine(24f, pageHeight - 35f, pageWidth - 24f, pageHeight - 35f, paint)

            textPaint.color = Color.parseColor("#718096")
            textPaint.textSize = 7.5f
            textPaint.isFakeBoldText = false
            canvas.drawText("This is a computer-generated bank statement. Verified by SpendWise.", 24f, pageHeight - 20f, textPaint)

            val pageText = "Page $pageIndex of $totalPages"
            canvas.drawText(pageText, pageWidth - 80f, pageHeight - 20f, textPaint)

            pdfDoc.finishPage(page)
        }

        // Save PDF to cache dir
        val outputDir = File(context.cacheDir, "statements").apply { mkdirs() }
        val fileName = "SpendWise_Statement_${info.customerName}_${System.currentTimeMillis()}.pdf"
        val outputFile = File(outputDir, fileName)

        FileOutputStream(outputFile).use { out ->
            pdfDoc.writeTo(out)
        }
        pdfDoc.close()

        return outputFile
    }

    /**
     * Generates a fully formatted Excel / CSV Bank Statement.
     */
    fun generateExcelStatement(
        context: Context,
        transactions: List<TransactionEntity>,
        info: StatementCustomerInfo
    ): File {
        val validTxns = transactions.filter { !it.isDuplicate }.sortedBy { it.timestamp }
        val totalIncome = validTxns.filter { it.isIncome }.sumOf { it.amount }
        val totalExpense = validTxns.filter { !it.isIncome }.sumOf { it.amount }

        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        val sb = StringBuilder()
        sb.append("OFFICIAL BANK ACCOUNT STATEMENT\n")
        sb.append("Bank Name:,\"${info.bankName}\"\n")
        sb.append("Account Holder:,\"${info.customerName}\"\n")
        sb.append("Account Number:,\"${info.accountNumber}\"\n")
        sb.append("Registered Mobile:,\"${info.mobileNumber}\"\n")
        sb.append("Statement Period:,\"${info.periodText}\"\n")
        sb.append("Closing Balance:,\"₹${String.format(Locale.getDefault(), "%,.2f", info.closingBalance)}\"\n")
        sb.append("Total Credited (Income):,\"₹${String.format(Locale.getDefault(), "%,.2f", totalIncome)}\"\n")
        sb.append("Total Debited (Expenses):,\"₹${String.format(Locale.getDefault(), "%,.2f", totalExpense)}\"\n")
        sb.append("Password Protected Note:,\"First 4 letters of name + last 4 digits of phone (${getPasswordHint(info.customerName, info.mobileNumber)})\"\n\n")

        sb.append("Date & Time,Narration / Payee,Payment Mode,Reference / UPI Number,Category,Withdrawal (Dr ₹),Deposit (Cr ₹),Running Balance (₹),Status\n")

        for (txn in validTxns) {
            val dateStr = sdf.format(Date(txn.timestamp))
            val merchant = txn.merchant.replace("\"", "\"\"")
            val mode = txn.paymentMethod.replace("\"", "\"\"")
            val ref = (txn.refId ?: "").replace("\"", "\"\"")
            val category = txn.category.replace("\"", "\"\"")
            val dr = if (!txn.isIncome) txn.amount.toString() else ""
            val cr = if (txn.isIncome) txn.amount.toString() else ""
            val bal = txn.accountBalance?.toString() ?: ""

            sb.append("\"$dateStr\",\"$merchant\",\"$mode\",\"$ref\",\"$category\",\"$dr\",\"$cr\",\"$bal\",\"Success\"\n")
        }

        val outputDir = File(context.cacheDir, "statements").apply { mkdirs() }
        val fileName = "SpendWise_Statement_${info.customerName}_${System.currentTimeMillis()}.csv"
        val outputFile = File(outputDir, fileName)
        outputFile.writeText(sb.toString())

        return outputFile
    }

    /**
     * Triggers Android share / open intent for the generated statement.
     */
    fun shareStatementFile(context: Context, file: File, mimeType: String, title: String) {
        val uri = FileProvider.getUriForFile(context, "com.example.smartexpensetracker.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, "Here is your official account statement generated from SpendWise.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "Share $title via...")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    fun openStatementFile(context: Context, file: File, mimeType: String) {
        val uri = FileProvider.getUriForFile(context, "com.example.smartexpensetracker.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(intent, "Open Statement")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
