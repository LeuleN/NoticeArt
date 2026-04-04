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
        
        startNewPage()

        // 1. Header
        val titlePaint = TextPaint().apply {
            isAntiAlias = true
            textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.BLACK
        }
        val titleLayout = createStaticLayout(entry.title, titlePaint, contentWidth.toInt())
        ensureSpace(titleLayout.height.toFloat())
        titleLayout.drawOnCanvas(canvas!!, margin, currentY)
        currentY += titleLayout.height + 8f

        val dateText = SimpleDateFormat("EEEE, MMMM dd, yyyy • hh:mm a", Locale.getDefault()).format(Date(entry.timestamp))
        val datePaint = TextPaint().apply {
            isAntiAlias = true
            textSize = 12f
            color = Color.GRAY
        }
        ensureSpace(20f)
        canvas!!.drawText(dateText, margin, currentY + 12f, datePaint)
        currentY += 40f

        // 2. Main Content Images
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        entry.media.forEach { mediaItem ->
            mediaItem.imageUri?.let { uriString ->
                val bitmap = loadBitmapFromUri(Uri.parse(uriString))
                if (bitmap != null) {
                    val scaledBitmap = scaleBitmapToWidth(bitmap, contentWidth)
                    ensureSpace(scaledBitmap.height.toFloat() + 20f)
                    canvas!!.drawBitmap(scaledBitmap, margin, currentY, paint)
                    currentY += scaledBitmap.height + 30f
                    scaledBitmap.recycle()
                    bitmap.recycle()
                }
            }
        }

        // 3. Captured Textures Section
        val validTextures = entry.media.flatMap { it.textures }.mapNotNull { texture ->
            val bitmap = texture.imageUri?.let { loadBitmapFromUri(Uri.parse(it)) }
            if (bitmap != null) texture to bitmap else null
        }

        if (validTextures.isNotEmpty()) {
            ensureSpace(80f)
            currentY = drawSectionHeader("Captured Textures", canvas!!, currentY)
            
            val textureSpacing = 15f
            val textureSize = (contentWidth - 2 * textureSpacing) / 3f
            
            validTextures.chunked(3).forEach { rowTextures ->
                ensureSpace(textureSize + 40f)
                
                // Centering Logic for rows with fewer than 3 items
                val rowWidth = (rowTextures.size * textureSize) + ((rowTextures.size - 1) * textureSpacing)
                var textureX = margin + (contentWidth - rowWidth) / 2f

                rowTextures.forEach { (texture, bitmap) ->
                    val rect = RectF(textureX, currentY, textureX + textureSize, currentY + textureSize)
                    canvas!!.drawBitmap(bitmap, null, rect, paint)
                    
                    val labelPaint = TextPaint().apply { 
                        textSize = 9f
                        textAlign = Paint.Align.CENTER 
                        isAntiAlias = true
                        color = Color.BLACK
                    }
                    val labelY = currentY + textureSize + 14f
                    val displayLabel = if (texture.name.length > 18) texture.name.take(15) + "..." else texture.name
                    canvas!!.drawText(displayLabel, textureX + textureSize / 2, labelY, labelPaint)
                    
                    bitmap.recycle()
                    textureX += textureSize + textureSpacing
                }
                currentY += textureSize + 45f
            }
            currentY += 20f
        }

        // 4. Color Palette Section
        val allColors = entry.media.flatMap { it.colors }.distinct()
        if (allColors.isNotEmpty()) {
            ensureSpace(80f)
            currentY = drawSectionHeader("Color Palette", canvas!!, currentY)
            
            val swatchSize = 42f
            val swatchSpacing = 20f
            val maxPerRow = (contentWidth / (swatchSize + swatchSpacing)).toInt()
            
            allColors.chunked(maxPerRow).forEach { rowColors ->
                ensureSpace(swatchSize + 40f)
                
                val rowWidth = (rowColors.size * swatchSize) + ((rowColors.size - 1) * swatchSpacing)
                var swatchX = margin + (contentWidth - rowWidth) / 2f
                
                rowColors.forEach { colorInt ->
                    paint.color = colorInt
                    paint.style = Paint.Style.FILL
                    canvas!!.drawCircle(swatchX + swatchSize / 2, currentY + swatchSize / 2, swatchSize / 2, paint)
                    
                    // Light border for clarity on white background
                    paint.color = Color.LTGRAY
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 0.5f
                    canvas!!.drawCircle(swatchX + swatchSize / 2, currentY + swatchSize / 2, swatchSize / 2, paint)
                    
                    val hexText = String.format("#%06X", 0xFFFFFF and colorInt)
                    val hexPaint = TextPaint().apply { 
                        textSize = 8f
                        textAlign = Paint.Align.CENTER
                        color = Color.DKGRAY
                        typeface = Typeface.MONOSPACE
                        isAntiAlias = true
                    }
                    canvas!!.drawText(hexText, swatchX + swatchSize / 2, currentY + swatchSize + 14f, hexPaint)
                    
                    swatchX += swatchSize + swatchSpacing
                }
                currentY += swatchSize + 40f
            }
            currentY += 20f
        }

        // 5. Observations Section
        entry.observation?.takeIf { it.isNotBlank() }?.let { obs ->
            ensureSpace(80f)
            currentY = drawSectionHeader("Observations", canvas!!, currentY)
            
            val obsPaint = TextPaint().apply {
                isAntiAlias = true
                textSize = 14f
                color = Color.rgb(40, 40, 40)
            }
            
            val obsLayout = createStaticLayout(obs, obsPaint, contentWidth.toInt())
            ensureSpace(obsLayout.height.toFloat() + 20f)
            obsLayout.drawOnCanvas(canvas!!, margin, currentY)
            currentY += obsLayout.height + 40f
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
            color = Color.BLACK
        }
        canvas.drawText(text, margin, y + 22f, paint)
        
        val linePaint = Paint().apply {
            color = Color.rgb(220, 220, 220)
            strokeWidth = 1.5f
        }
        canvas.drawLine(margin, y + 32f, margin + 60f, y + 32f, linePaint)
        
        return y + 50f
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
            .setLineSpacing(0f, 1.3f)
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
