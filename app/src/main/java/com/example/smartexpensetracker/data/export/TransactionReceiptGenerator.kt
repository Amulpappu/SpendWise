package com.example.smartexpensetracker.data.export

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.smartexpensetracker.data.local.entity.TransactionEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object TransactionReceiptGenerator {

    fun generateReceiptBitmap(context: Context, transaction: TransactionEntity): Bitmap {
        val width = 1080
        val height = 1440
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val isIncome = transaction.isIncome
        val fullDateFormat = SimpleDateFormat("EEEE, dd MMM yyyy \u2022 hh:mm a", Locale.getDefault())
        val dateStr = fullDateFormat.format(Date(transaction.timestamp))
        val sign = if (isIncome) "+" else "-"
        val formattedAmount = String.format(Locale.US, "%,.2f", transaction.amount)

        // 1. Outer Background
        val outerPaint = Paint().apply {
            color = Color.parseColor("#0B111A")
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), outerPaint)

        // 2. Main Card Background
        val cardLeft = 56f
        val cardTop = 56f
        val cardRight = width.toFloat() - 56f
        val cardBottom = height.toFloat() - 56f
        val cardRadius = 44f

        val cardPaint = Paint().apply {
            color = Color.parseColor("#131D2A")
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val cardRect = RectF(cardLeft, cardTop, cardRight, cardBottom)
        canvas.drawRoundRect(cardRect, cardRadius, cardRadius, cardPaint)

        // Card Border
        val borderPaint = Paint().apply {
            color = Color.parseColor("#1E293B")
            style = Paint.Style.STROKE
            strokeWidth = 3f
            isAntiAlias = true
        }
        canvas.drawRoundRect(cardRect, cardRadius, cardRadius, borderPaint)

        // 3. Status Pill Header (Top)
        val pillWidth = 440f
        val pillHeight = 64f
        val pillX = (width - pillWidth) / 2f
        val pillY = 120f
        val pillRect = RectF(pillX, pillY, pillX + pillWidth, pillY + pillHeight)

        val pillBgPaint = Paint().apply {
            color = if (isIncome) Color.parseColor("#064E3B") else Color.parseColor("#0F3E33")
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawRoundRect(pillRect, 32f, 32f, pillBgPaint)

        val pillBorderPaint = Paint().apply {
            color = Color.parseColor("#10B981")
            style = Paint.Style.STROKE
            strokeWidth = 2f
            isAntiAlias = true
        }
        canvas.drawRoundRect(pillRect, 32f, 32f, pillBorderPaint)

        val pillTextPaint = Paint().apply {
            color = Color.parseColor("#34D399")
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        val statusText = if (isIncome) "\u2713 MONEY RECEIVED" else "\u2713 PAYMENT SUCCESSFUL"
        canvas.drawText(statusText, width / 2f, pillY + 43f, pillTextPaint)

        // 4. Merchant / Payee Name
        val merchantPaint = Paint().apply {
            color = Color.parseColor("#FFFFFF")
            textSize = if (transaction.merchant.length > 20) 42f else 50f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText(transaction.merchant, width / 2f, 265f, merchantPaint)

        // 5. Large Amount
        val amountPaint = Paint().apply {
            color = if (isIncome) Color.parseColor("#10B981") else Color.parseColor("#F8FAFC")
            textSize = 72f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("$sign\u20B9$formattedAmount", width / 2f, 365f, amountPaint)

        // 6. Date & Time
        val datePaint = Paint().apply {
            color = Color.parseColor("#94A3B8")
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText(dateStr, width / 2f, 430f, datePaint)

        // 7. Running Balance (if available)
        var nextY = 500f
        if (transaction.accountBalance != null) {
            val balPillWidth = 480f
            val balPillHeight = 52f
            val balPillX = (width - balPillWidth) / 2f
            val balPillRect = RectF(balPillX, 470f, balPillX + balPillWidth, 470f + balPillHeight)

            val balBgPaint = Paint().apply {
                color = Color.parseColor("#0C2338")
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            canvas.drawRoundRect(balPillRect, 26f, 26f, balBgPaint)

            val balBorderPaint = Paint().apply {
                color = Color.parseColor("#0284C7")
                style = Paint.Style.STROKE
                strokeWidth = 1.5f
                isAntiAlias = true
            }
            canvas.drawRoundRect(balPillRect, 26f, 26f, balBorderPaint)

            val balTextPaint = Paint().apply {
                color = Color.parseColor("#38BDF8")
                textSize = 26f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            val balFormatted = String.format(Locale.US, "%,.2f", transaction.accountBalance)
            canvas.drawText("Running Balance: \u20B9$balFormatted", width / 2f, 506f, balTextPaint)
            nextY = 560f
        }

        // 8. Divider 1 (Dashed Line)
        val dashedPaint = Paint().apply {
            color = Color.parseColor("#334155")
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
            pathEffect = DashPathEffect(floatArrayOf(16f, 12f), 0f)
            isAntiAlias = true
        }
        val dividerY1 = nextY + 30f
        canvas.drawLine(cardLeft + 36f, dividerY1, cardRight - 36f, dividerY1, dashedPaint)

        // 9. Key-Value Transaction Details Rows
        val rowStartY = dividerY1 + 65f
        val rowSpacing = 82f
        val labelX = cardLeft + 50f
        val valueX = cardRight - 50f

        val labelPaint = Paint().apply {
            color = Color.parseColor("#94A3B8")
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.LEFT
            isAntiAlias = true
        }

        val valuePaint = Paint().apply {
            color = Color.parseColor("#F1F5F9")
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
        }

        val monoValuePaint = Paint().apply {
            color = Color.parseColor("#38BDF8")
            textSize = 28f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
        }

        val details = mutableListOf<Triple<String, String, Boolean>>()
        if (!transaction.refId.isNullOrBlank()) {
            details.add(Triple("UPI Ref / Txn ID", transaction.refId, true))
        }
        val bankInfo = if (!transaction.accountNumber.isNullOrBlank()) {
            "${transaction.bankName ?: "Bank"} (${transaction.accountNumber})"
        } else {
            transaction.bankName ?: "Primary Account"
        }
        details.add(Triple("Bank & Account", bankInfo, false))
        details.add(Triple("Payment Method", transaction.paymentMethod, false))
        details.add(Triple("Category", transaction.category, false))
        if (transaction.note.isNotBlank()) {
            details.add(Triple("Note", transaction.note, false))
        }

        var currentY = rowStartY
        for ((lbl, valStr, isMono) in details) {
            canvas.drawText(lbl, labelX, currentY, labelPaint)
            val p = if (isMono) monoValuePaint else valuePaint
            canvas.drawText(valStr, valueX, currentY, p)
            currentY += rowSpacing
        }

        // 10. Ticket Cutout & Divider 2
        val cutoutY = 1110f
        val cutoutRadius = 32f

        // Cutouts on left & right edge
        canvas.drawCircle(cardLeft, cutoutY, cutoutRadius, outerPaint)
        canvas.drawCircle(cardRight, cutoutY, cutoutRadius, outerPaint)

        // Arc border around cutouts
        val cutoutArcPaint = Paint().apply {
            color = Color.parseColor("#1E293B")
            style = Paint.Style.STROKE
            strokeWidth = 3f
            isAntiAlias = true
        }
        canvas.drawArc(RectF(cardLeft - cutoutRadius, cutoutY - cutoutRadius, cardLeft + cutoutRadius, cutoutY + cutoutRadius), 270f, 180f, false, cutoutArcPaint)
        canvas.drawArc(RectF(cardRight - cutoutRadius, cutoutY - cutoutRadius, cardRight + cutoutRadius, cutoutY + cutoutRadius), 90f, 180f, false, cutoutArcPaint)

        // Dashed line between cutouts
        canvas.drawLine(cardLeft + cutoutRadius + 12f, cutoutY, cardRight - cutoutRadius - 12f, cutoutY, dashedPaint)

        // 11. Footer Branding
        val footerStatusPaint = Paint().apply {
            color = Color.parseColor("#10B981")
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("\u2713 100% Verified Digital Receipt", width / 2f, 1205f, footerStatusPaint)

        val footerBrandPaint = Paint().apply {
            color = Color.parseColor("#64748B")
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("SpendWise \u2022 Personal Expense Tracker", width / 2f, 1255f, footerBrandPaint)

        val footerSecPaint = Paint().apply {
            color = Color.parseColor("#475569")
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("Generated locally on device \u2022 100% Offline & Secure", width / 2f, 1295f, footerSecPaint)

        return bitmap
    }

    fun shareTransactionReceipt(context: Context, transaction: TransactionEntity) {
        try {
            val bitmap = generateReceiptBitmap(context, transaction)
            val receiptsDir = File(context.cacheDir, "receipts").apply { mkdirs() }
            val receiptFile = File(receiptsDir, "receipt_${transaction.id}_${System.currentTimeMillis()}.png")

            FileOutputStream(receiptFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                receiptFile
            )

            val sign = if (transaction.isIncome) "+" else "-"
            val formattedAmount = String.format(Locale.US, "%,.2f", transaction.amount)
            val actionText = if (transaction.isIncome) "Received" else "Paid"
            val prepText = if (transaction.isIncome) "from" else "to"
            val dateFormat = SimpleDateFormat("EEEE, dd MMM yyyy \u2022 hh:mm a", Locale.getDefault())
            val dateStr = dateFormat.format(Date(transaction.timestamp))

            val caption = StringBuilder().apply {
                append("$actionText $sign\u20B9$formattedAmount $prepText ${transaction.merchant}\n")
                append("\uD83D\uDCC5 Date: $dateStr\n")
                if (!transaction.refId.isNullOrBlank()) {
                    append("\uD83D\uDD22 UPI Ref ID: ${transaction.refId}\n")
                }
                if (!transaction.accountNumber.isNullOrBlank()) {
                    append("\uD83C\uDFE6 Bank: ${transaction.bankName ?: "Bank"} (${transaction.accountNumber})\n")
                }
                append("\uD83C\uDFF7\uFE0F Category: ${transaction.category}\n\n")
                append("Tracked with SpendWise \uD83D\uDCB3\n")
                append("https://github.com/Amulpappu/SpendWise")
            }.toString()

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, caption)
                putExtra(Intent.EXTRA_SUBJECT, "Transaction Receipt - ${transaction.merchant}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(shareIntent, "Share Receipt via"))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Could not generate receipt image: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
