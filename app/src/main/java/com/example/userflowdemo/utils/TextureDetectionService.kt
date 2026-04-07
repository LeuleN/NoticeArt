package com.example.userflowdemo.utils

import android.graphics.Bitmap
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import kotlin.math.hypot

class TextureDetectionService {

    init {
        OpenCVLoader.initDebug()
    }

    fun getProminentTextureAreas(
        bitmap: Bitmap, 
        gridCount: Int = 10,
        textureCount: Int = 6
    ): List<Rect> {
        val mat = Mat()
        // Ensure bitmap is in a format OpenCV likes
        val bmp32 = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        Utils.bitmapToMat(bmp32, mat)
        
        // 1. Convert to Grayscale for texture analysis
        val grayMat = Mat()
        Imgproc.cvtColor(mat, grayMat, Imgproc.COLOR_RGB2GRAY)

        // 2. Apply Laplacian filter to find "edges" / "detail"
        val laplacianMat = Mat()
        Imgproc.Laplacian(grayMat, laplacianMat, CvType.CV_64F)

        val cellWidth = mat.width() / gridCount
        val cellHeight = mat.height() / gridCount
        val textureScores = mutableListOf<Pair<Rect, Double>>()

        // 3. Scan the image in a grid
        for (y in 0 until gridCount) {
            for (x in 0 until gridCount) {
                // Ensure we don't go out of bounds
                val w = if (x * cellWidth + cellWidth <= mat.width()) cellWidth else mat.width() - x * cellWidth
                val h = if (y * cellHeight + cellHeight <= mat.height()) cellHeight else mat.height() - y * cellHeight
                
                if (w <= 0 || h <= 0) continue

                val rect = Rect(x * cellWidth, y * cellHeight, w, h)
                val cell = laplacianMat.submat(rect)
                
                // Calculate Variance (Standard Deviation)
                val stdDev = MatOfDouble()
                val mean = MatOfDouble()
                Core.meanStdDev(cell, mean, stdDev)
                
                val score = stdDev.toArray()[0]
                // Filter out near-zero variance (completely blank/solid areas)
                if (score > 0.1) {
                    textureScores.add(Pair(rect, score))
                }
                
                cell.release()
            }
        }

        // Clean up
        mat.release()
        grayMat.release()
        laplacianMat.release()
        bmp32.recycle()

        // 4. Balanced Selection with Spatial Diversity
        val sortedScores = textureScores.sortedByDescending { it.second }
        if (sortedScores.isEmpty()) return emptyList()

        val highCountTotal = (sortedScores.size * 0.3).toInt().coerceAtLeast(1)
        val lowCountTotal = (sortedScores.size * 0.3).toInt().coerceAtLeast(1)
        val midCountTotal = (sortedScores.size - highCountTotal - lowCountTotal).coerceAtLeast(0)

        val highBucket = sortedScores.take(highCountTotal)
        val midBucket = sortedScores.drop(highCountTotal).take(midCountTotal)
        val lowBucket = sortedScores.takeLast(lowCountTotal)

        val selectedRects = mutableListOf<Rect>()
        val minDistanceThreshold = cellWidth * 2.0 

        fun selectFromBucket(bucket: List<Pair<Rect, Double>>, count: Int) {
            var selectedInBucket = 0
            for (candidate in bucket) {
                if (selectedInBucket >= count) break
                
                val rect = candidate.first
                val centerX = rect.x + rect.width / 2.0
                val centerY = rect.y + rect.height / 2.0

                val isTooClose = selectedRects.any { selected ->
                    val sCenterX = selected.x + selected.width / 2.0
                    val sCenterY = selected.y + selected.height / 2.0
                    hypot(centerX - sCenterX, centerY - sCenterY) < minDistanceThreshold
                }

                if (!isTooClose) {
                    selectedRects.add(rect)
                    selectedInBucket++
                }
            }
        }

        // Calculate how many to take from each bucket based on textureCount
        // Distribution logic: prioritize high -> medium -> low
        val targetPerBucket = textureCount / 3
        var remainder = textureCount % 3
        
        val highToTake = targetPerBucket + if (remainder > 0) 1 else 0
        if (remainder > 0) remainder--
        val midToTake = targetPerBucket + if (remainder > 0) 1 else 0
        if (remainder > 0) remainder--
        val lowToTake = targetPerBucket + if (remainder > 0) 1 else 0

        selectFromBucket(highBucket, highToTake)
        selectFromBucket(midBucket, midToTake)
        selectFromBucket(lowBucket, lowToTake)

        return selectedRects
    }

    fun cropTextureBitmaps(original: Bitmap, rects: List<Rect>): List<Bitmap> {
        return rects.mapNotNull { rect ->
            try {
                Bitmap.createBitmap(original, rect.x, rect.y, rect.width, rect.height)
            } catch (e: Exception) {
                null
            }
        }
    }
}
