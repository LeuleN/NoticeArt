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
    private val margin = 45f
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
        canvas?.drawColor(Color.WHITE)
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

        // --- PAGE 1: ENTRY HEADER + OBSERVATIONS ---
        startNewPage()

        // MANDATORY: Fixed top-down positioning. NO vertical centering.
        currentY = 100f 

        val titlePaint = TextPaint().apply {
            isAntiAlias = true
            textSize = 32f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.BLACK
            textAlign = Paint.Align.CENTER
        }
        val datePaint = TextPaint().apply {
            isAntiAlias = true
            textSize = 14f
            color = Color.GRAY
            textAlign = Paint.Align.CENTER
        }
        val obsPaint = TextPaint().apply {
            isAntiAlias = true
            textSize = 14f
            color = Color.rgb(40, 40, 40)
        }

        val titleLayout = createStaticLayout(entry.title, titlePaint, contentWidth.toInt(), Layout.Alignment.ALIGN_CENTER)
        val dateText = SimpleDateFormat("EEEE, MMMM dd, yyyy • hh:mm a", Locale.getDefault()).format(Date(entry.timestamp))
        val observation = entry.observation?.takeIf { it.isNotBlank() } ?: ""

        // 1. Draw Title (horizontally centered)
        titleLayout.drawOnCanvas(canvas!!, margin, currentY)
        currentY += titleLayout.height + 12f

        // 2. Draw Date (horizontally centered)
        canvas!!.drawText(dateText, pageWidth / 2f, currentY + 14f, datePaint)
        currentY += 80f // Large spacing before Observations section

        // 3. Draw Observations (left-aligned)
        if (observation.isNotEmpty()) {
            currentY = drawSectionHeader("Observations", canvas!!, currentY)
            
            val obsLayout = createStaticLayout(observation, obsPaint, contentWidth.toInt())
            
            // Handle multi-page overflow for observations
            val lines = observation.split("\n")
            lines.forEach { line ->
                if (line.isBlank()) {
                    currentY += 10f
                    return@forEach
                }
                val lineLayout = createStaticLayout(line, obsPaint, contentWidth.toInt())
                ensureSpace(lineLayout.height.toFloat())
                lineLayout.drawOnCanvas(canvas!!, margin, currentY)
                currentY += lineLayout.height.toFloat()
            }
        }

        // --- PER-IMAGE FLOW ---
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        entry.media.forEach { mediaItem ->
            // 1. PAGE - IMAGE
            mediaItem.imageUri?.let { uriString ->
                val bitmap = loadBitmapFromUri(Uri.parse(uriString))
                if (bitmap != null) {
                    startNewPage()
                    val scaledBitmap = scaleBitmapToFitPage(bitmap, contentWidth, pageHeight - 2 * margin)
                    val x = (pageWidth - scaledBitmap.width) / 2f
                    val y = (pageHeight - scaledBitmap.height) / 2f
                    canvas!!.drawBitmap(scaledBitmap, x, y, paint)
                    scaledBitmap.recycle()
                    bitmap.recycle()
                }
            }

            // 2. PAGE - TEXTURES (FOR THAT IMAGE ONLY)
            if (mediaItem.textures.isNotEmpty()) {
                startNewPage()
                currentY = drawSectionHeader("Captured Textures", canvas!!, currentY)
                
                val textureSpacing = 20f
                val textureSize = (contentWidth - 2 * textureSpacing) / 3f
                
                mediaItem.textures.chunked(3).forEach { rowTextures ->
                    ensureSpace(textureSize + 50f)
                    var textureX = margin
                    
                    rowTextures.forEach { texture ->
                        texture.imageUri?.let { tUri ->
                            loadBitmapFromUri(Uri.parse(tUri))?.let { tBitmap ->
                                val rect = RectF(textureX, currentY, textureX + textureSize, currentY + textureSize)
                                canvas!!.drawBitmap(tBitmap, null, rect, paint)
                                
                                val labelPaint = TextPaint().apply { 
                                    textSize = 10f
                                    textAlign = Paint.Align.CENTER 
                                    isAntiAlias = true
                                    color = Color.BLACK
                                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                                }
                                val displayLabel = if (texture.name.length > 20) texture.name.take(17) + "..." else texture.name
                                canvas!!.drawText(displayLabel, textureX + textureSize / 2, currentY + textureSize + 16f, labelPaint)
                                
                                tBitmap.recycle()
                            }
                        }
                        textureX += textureSize + textureSpacing
                    }
                    currentY += textureSize + 50f
                }
            }

            // 3. PAGE - COLOR PALETTE (FOR THAT IMAGE ONLY)
            if (mediaItem.colors.isNotEmpty()) {
                startNewPage()
                currentY = drawSectionHeader("Color Palette", canvas!!, currentY)
                
                val swatchSize = 50f
                val swatchSpacing = 25f
                val itemsPerRow = 5
                
                mediaItem.colors.chunked(itemsPerRow).forEach { rowColors ->
                    ensureSpace(swatchSize + 50f)
                    var swatchX = margin
                    
                    rowColors.forEach { colorInt ->
                        // Circular swatch
                        paint.color = colorInt
                        paint.style = Paint.Style.FILL
                        canvas!!.drawCircle(swatchX + swatchSize / 2, currentY + swatchSize / 2, swatchSize / 2, paint)
                        
                        // Border
                        paint.color = Color.LTGRAY
                        paint.style = Paint.Style.STROKE
                        paint.strokeWidth = 1f
                        canvas!!.drawCircle(swatchX + swatchSize / 2, currentY + swatchSize / 2, swatchSize / 2, paint)
                        
                        val hexText = String.format("#%06X", 0xFFFFFF and colorInt)
                        val hexPaint = TextPaint().apply { 
                            textSize = 10f
                            textAlign = Paint.Align.CENTER
                            color = Color.DKGRAY
                            typeface = Typeface.MONOSPACE
                            isAntiAlias = true
                        }
                        canvas!!.drawText(hexText, swatchX + swatchSize / 2, currentY + swatchSize + 18f, hexPaint)
                        
                        swatchX += swatchSize + swatchSpacing
                    }
                    currentY += swatchSize + 50f
                }
            }
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
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.BLACK
        }
        ensureSpace(40f)
        canvas.drawText(text, margin, y + 24f, paint)
        
        val linePaint = Paint().apply {
            color = Color.BLACK
            strokeWidth = 2f
        }
        canvas.drawLine(margin, y + 36f, margin + 40f, y + 36f, linePaint)
        
        return y + 60f
    }

    private fun loadBitmapFromUri(uri: Uri): Bitmap? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            null
        }
    }

    private fun scaleBitmapToFitPage(bitmap: Bitmap, maxWidth: Float, maxHeight: Float): Bitmap {
        val width = bitmap.width.toFloat()
        val height = bitmap.height.toFloat()
        val ratio = Math.min(maxWidth / width, maxHeight / height)
        
        val finalWidth = (width * ratio).toInt()
        val finalHeight = (height * ratio).toInt()
        
        return Bitmap.createScaledBitmap(bitmap, finalWidth, finalHeight, true)
    }

    private fun createStaticLayout(text: String, paint: TextPaint, width: Int, alignment: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL): StaticLayout {
        return StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setAlignment(alignment)
            .setLineSpacing(0f, 1.4f)
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
