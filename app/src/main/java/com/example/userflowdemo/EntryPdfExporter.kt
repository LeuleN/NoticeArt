package com.example.userflowdemo

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class EntryPdfExporter(private val context: Context) {

    private val pageWidth = 595 // A4 width in points (72 dpi)
    private val pageHeight = 842 // A4 height in points (72 dpi)
    private val margin = 40f
    private val contentWidth = pageWidth - 2 * margin

    private var pdfDocument = PdfDocument()
    private var currentPage: PdfDocument.Page? = null
    private var canvas: Canvas? = null
    private var currentY = margin
    private var pageNumber = 0

    fun exportAndShare(entry: Entry) {
        val uri = generatePdf(entry)
        if (uri != null) {
            sharePdf(uri)
        }
    }

    private fun startNewPage() {
        currentPage?.let { pdfDocument.finishPage(it) }
        pageNumber++
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        currentPage = pdfDocument.startPage(pageInfo)
        canvas = currentPage?.canvas
        currentY = margin
    }

    private fun ensureSpace(heightNeeded: Float) {
        if (currentY + heightNeeded > pageHeight - margin) {
            startNewPage()
        }
    }

    private fun generatePdf(entry: Entry): Uri? {
        pdfDocument = PdfDocument()
        pageNumber = 0
        currentPage = null
        currentY = margin
        
        startNewPage()
        val currentCanvas = canvas ?: return null

        // 1. Title
        val titlePaint = TextPaint().apply {
            isAntiAlias = true
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val titleLayout = createStaticLayout(entry.title, titlePaint, contentWidth.toInt())
        ensureSpace(titleLayout.height.toFloat())
        titleLayout.drawOnCanvas(canvas!!, margin, currentY)
        currentY += titleLayout.height + 10f

        // 2. Timestamp
        val dateText = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault()).format(Date(entry.timestamp))
        val datePaint = TextPaint().apply {
            isAntiAlias = true
            textSize = 14f
            color = Color.GRAY
        }
        ensureSpace(20f)
        canvas!!.drawText(dateText, margin, currentY + 14f, datePaint)
        currentY += 40f

        // 3. Main Images
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        entry.media.forEach { mediaItem ->
            mediaItem.imageUri?.let { uriString ->
                val bitmap = loadBitmapFromUri(Uri.parse(uriString))
                if (bitmap != null) {
                    val scaledBitmap = scaleBitmapToWidth(bitmap, contentWidth)
                    ensureSpace(scaledBitmap.height.toFloat())
                    canvas!!.drawBitmap(scaledBitmap, margin, currentY, paint)
                    currentY += scaledBitmap.height + 20f
                    scaledBitmap.recycle()
                    bitmap.recycle()
                }
            }
        }

        // 4. Textures
        val allTextures = entry.media.flatMap { it.textures }
        if (allTextures.isNotEmpty()) {
            ensureSpace(40f)
            currentY = drawSectionHeader("Captured Textures", canvas!!, currentY)
            
            val textureSize = (contentWidth - 2 * 10f) / 3f
            var textureX = margin
            
            allTextures.forEachIndexed { index, texture ->
                if (index > 0 && index % 3 == 0) {
                    textureX = margin
                    currentY += textureSize + 30f
                }
                
                ensureSpace(textureSize + 20f)
                // If we jumped to a new page, reset textureX
                if (currentY == margin) {
                    textureX = margin
                }

                texture.imageUri?.let { uriStr ->
                    val bitmap = loadBitmapFromUri(Uri.parse(uriStr))
                    if (bitmap != null) {
                        val rect = RectF(textureX, currentY, textureX + textureSize, currentY + textureSize)
                        canvas!!.drawBitmap(bitmap, null, rect, paint)
                        
                        val labelPaint = TextPaint().apply { 
                            textSize = 8f
                            textAlign = Paint.Align.CENTER 
                            isAntiAlias = true
                        }
                        canvas!!.drawText(texture.name, textureX + textureSize / 2, currentY + textureSize + 12f, labelPaint)
                        bitmap.recycle()
                    }
                }
                textureX += textureSize + 10f
            }
            currentY += textureSize + 40f
        }

        // 5. Colors
        val allColors = entry.media.flatMap { it.colors }.distinct()
        if (allColors.isNotEmpty()) {
            ensureSpace(40f)
            currentY = drawSectionHeader("Extracted Colors", canvas!!, currentY)
            
            val swatchSize = 40f
            val spacing = 15f
            var swatchX = margin
            
            allColors.forEach { colorInt ->
                if (swatchX + swatchSize > pageWidth - margin) {
                    swatchX = margin
                    currentY += swatchSize + 30f
                }
                
                ensureSpace(swatchSize + 20f)
                if (currentY == margin) swatchX = margin

                paint.color = colorInt
                canvas!!.drawCircle(swatchX + swatchSize / 2, currentY + swatchSize / 2, swatchSize / 2, paint)
                
                val hexText = String.format("#%06X", 0xFFFFFF and colorInt)
                val hexPaint = TextPaint().apply { 
                    textSize = 8f
                    textAlign = Paint.Align.CENTER
                    color = Color.BLACK
                    isAntiAlias = true
                }
                canvas!!.drawText(hexText, swatchX + swatchSize / 2, currentY + swatchSize + 12f, hexPaint)
                
                swatchX += swatchSize + spacing
            }
            currentY += swatchSize + 40f
        }

        // 6. Observations
        entry.observation?.let { obs ->
            ensureSpace(40f)
            currentY = drawSectionHeader("Observations", canvas!!, currentY)
            
            val obsPaint = TextPaint().apply {
                isAntiAlias = true
                textSize = 14f
                color = Color.DKGRAY
            }
            
            val obsLayout = createStaticLayout(obs, obsPaint, contentWidth.toInt())
            
            // For observations, we'll just move to new page if it doesn't fit at all, 
            // or draw it and let it clip if it's extremely long (simple implementation).
            // A better one would split the text line by line.
            ensureSpace(obsLayout.height.toFloat().coerceAtMost(200f)) 
            obsLayout.drawOnCanvas(canvas!!, margin, currentY)
            currentY += obsLayout.height + 20f
        }

        currentPage?.let { pdfDocument.finishPage(it) }

        val pdfDir = File(context.cacheDir, "pdfs")
        if (!pdfDir.exists()) pdfDir.mkdirs()
        val pdfFile = File(pdfDir, "Entry_${entry.id}.pdf")
        
        return try {
            val outputStream = FileOutputStream(pdfFile)
            pdfDocument.writeTo(outputStream)
            outputStream.close()
            pdfDocument.close()
            FileProvider.getUriForFile(context, "${context.packageName}.provider", pdfFile)
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
            null
        }
    }

    private fun drawSectionHeader(text: String, canvas: Canvas, y: Float): Float {
        val paint = TextPaint().apply {
            isAntiAlias = true
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(text, margin, y + 18f, paint)
        return y + 30f
    }

    private fun loadBitmapFromUri(uri: Uri): Bitmap? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            null
        }
    }

    private fun scaleBitmapToWidth(bitmap: Bitmap, targetWidth: Float): Bitmap {
        val aspectRatio = bitmap.height.toFloat() / bitmap.width.toFloat()
        val targetHeight = targetWidth * aspectRatio
        return Bitmap.createScaledBitmap(bitmap, targetWidth.toInt(), targetHeight.toInt(), true)
    }

    private fun createStaticLayout(text: String, paint: TextPaint, width: Int): StaticLayout {
        return StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, 1.2f)
            .setIncludePad(false)
            .build()
    }

    private fun StaticLayout.drawOnCanvas(canvas: Canvas, x: Float, y: Float) {
        canvas.save()
        canvas.translate(x, y)
        this.draw(canvas)
        canvas.restore()
    }

    private fun sharePdf(uri: Uri) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Entry PDF").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }
}
