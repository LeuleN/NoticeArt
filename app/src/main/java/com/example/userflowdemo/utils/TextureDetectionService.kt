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

    fun getProminentTextureAreas(bitmap: Bitmap, gridCount: Int = 10): List<Rect> {
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
                textureScores.add(Pair(rect, score))
                
                cell.release()
            }
        }

        // Clean up
        mat.release()
        grayMat.release()
        laplacianMat.release()
        bmp32.recycle()

        // 4. Apply Non-Maximum Suppression (Spatial Diversity Filter)
        // Sort by score descending
        val sortedScores = textureScores.sortedByDescending { it.second }
        val selectedRects = mutableListOf<Rect>()
        
        // Minimum Euclidean distance between centers to be considered "diverse"
        val minDistanceThreshold = cellWidth * 2.0 

        for (candidate in sortedScores) {
            val rect = candidate.first
            val centerX = rect.x + rect.width / 2.0
            val centerY = rect.y + rect.height / 2.0

            val isTooClose = selectedRects.any { selected ->
                val sCenterX = selected.x + selected.width / 2.0
                val sCenterY = selected.y + selected.height / 2.0
                // Calculate Euclidean distance between centers
                val distance = hypot(centerX - sCenterX, centerY - sCenterY)
                distance < minDistanceThreshold
            }

            if (!isTooClose) {
                selectedRects.add(rect)
            }

            // Stop when we have enough diverse textures
            if (selectedRects.size >= 6) break
        }

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
