package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.ActivityHistoryEntity
import com.example.data.InventoryItemEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExportHelper {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    fun exportToCsv(
        context: Context,
        historyList: List<ActivityHistoryEntity>,
        inventoryList: List<InventoryItemEntity>
    ): Uri? {
        return try {
            val fileName = "smart_counter_export_${System.currentTimeMillis()}.csv"
            val file = File(context.cacheDir, fileName)

            file.bufferedWriter().use { writer ->
                // CSV Header
                writer.write("Section,Date,Time,Type,Title,Quantity,Category,Barcode_or_Result,Notes\n")

                // History records
                for (item in historyList) {
                    val dateStr = dateFormat.format(Date(item.timestamp))
                    val timeStr = timeFormat.format(Date(item.timestamp))
                    val safeTitle = escapeCsv(item.title)
                    val safeCategory = escapeCsv(item.category)
                    val safeResult = escapeCsv(item.result)
                    val safeNotes = escapeCsv(item.notes)
                    writer.write("Activity,$dateStr,$timeStr,${item.type},$safeTitle,${item.quantity},$safeCategory,$safeResult,$safeNotes\n")
                }

                // Inventory records
                for (inv in inventoryList) {
                    val dateStr = dateFormat.format(Date(inv.updatedAt))
                    val timeStr = timeFormat.format(Date(inv.updatedAt))
                    val safeName = escapeCsv(inv.name)
                    val safeCategory = escapeCsv(inv.category)
                    val safeBarcode = escapeCsv(inv.barcode)
                    val safeNotes = escapeCsv(inv.notes)
                    writer.write("Inventory,$dateStr,$timeStr,STOCK,$safeName,${inv.quantity},$safeCategory,$safeBarcode,$safeNotes\n")
                }
            }

            FileProvider.getUriForFile(
                context,
                "com.smartcounter.ai.fileprovider",
                file
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun exportToPdf(
        context: Context,
        historyList: List<ActivityHistoryEntity>,
        inventoryList: List<InventoryItemEntity>,
        totalItemsCounted: Int,
        totalScans: Int
    ): Uri? {
        val document = PdfDocument()
        return try {
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            val titlePaint = Paint().apply {
                color = Color.rgb(20, 30, 50)
                textSize = 20f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }

            val subtitlePaint = Paint().apply {
                color = Color.rgb(100, 116, 139)
                textSize = 11f
                isAntiAlias = true
            }

            val headerPaint = Paint().apply {
                color = Color.rgb(15, 23, 42)
                textSize = 13f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }

            val bodyPaint = Paint().apply {
                color = Color.rgb(30, 41, 59)
                textSize = 10f
                isAntiAlias = true
            }

            val linePaint = Paint().apply {
                color = Color.rgb(226, 232, 240)
                strokeWidth = 1f
            }

            var y = 45f
            canvas.drawText("Smart Counter & Scanner - Report", 40f, y, titlePaint)
            y += 18f
            canvas.drawText("Generated: ${dateTimeFormat.format(Date())} | App: Smart Counter v2.0.0", 40f, y, subtitlePaint)
            y += 20f
            canvas.drawLine(40f, y, 555f, y, linePaint)
            y += 24f

            // Executive Summary Box
            canvas.drawText("Executive Summary", 40f, y, headerPaint)
            y += 16f
            canvas.drawText("• Total Items Counted: $totalItemsCounted", 45f, y, bodyPaint)
            y += 14f
            canvas.drawText("• Total Barcode / QR Scans: $totalScans", 45f, y, bodyPaint)
            y += 14f
            canvas.drawText("• Active Inventory SKUs: ${inventoryList.size}", 45f, y, bodyPaint)
            y += 24f
            canvas.drawLine(40f, y, 555f, y, linePaint)
            y += 22f

            // Inventory Summary (Top 8 items)
            canvas.drawText("Inventory Snapshot (Top Items)", 40f, y, headerPaint)
            y += 18f

            if (inventoryList.isEmpty()) {
                canvas.drawText("No inventory items recorded yet.", 45f, y, bodyPaint)
                y += 16f
            } else {
                for (item in inventoryList.take(8)) {
                    val statusStr = if (item.isOutOfStock) "[Out of Stock]" else if (item.isLowStock) "[Low Stock]" else "[In Stock]"
                    val line = "• ${item.name} (Barcode: ${if (item.barcode.isBlank()) "N/A" else item.barcode}) - Qty: ${item.quantity} $statusStr"
                    canvas.drawText(line, 45f, y, bodyPaint)
                    y += 14f
                }
            }

            y += 15f
            canvas.drawLine(40f, y, 555f, y, linePaint)
            y += 22f

            // Recent Activity
            canvas.drawText("Recent Activity History", 40f, y, headerPaint)
            y += 18f

            if (historyList.isEmpty()) {
                canvas.drawText("No activity records available.", 45f, y, bodyPaint)
                y += 16f
            } else {
                for (act in historyList.take(12)) {
                    val dateStr = dateTimeFormat.format(Date(act.timestamp))
                    val line = "• [$dateStr] [${act.type}] ${act.title}: ${act.result} (Qty: ${act.quantity})"
                    val truncated = if (line.length > 80) line.substring(0, 77) + "..." else line
                    canvas.drawText(truncated, 45f, y, bodyPaint)
                    y += 14f
                }
            }

            // Footer
            canvas.drawLine(40f, 800f, 555f, 800f, linePaint)
            canvas.drawText("Confidential - Exported locally from Smart Counter (Offline Utility)", 40f, 815f, subtitlePaint)

            document.finishPage(page)

            val fileName = "smart_counter_report_${System.currentTimeMillis()}.pdf"
            val file = File(context.cacheDir, fileName)
            FileOutputStream(file).use { out ->
                document.writeTo(out)
            }

            FileProvider.getUriForFile(
                context,
                "com.smartcounter.ai.fileprovider",
                file
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            document.close()
        }
    }

    fun shareExportedFile(context: Context, fileUri: Uri, mimeType: String, chooserTitle: String) {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, fileUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, chooserTitle))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Sharing failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }
}
