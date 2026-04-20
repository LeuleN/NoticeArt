package com.example.userflowdemo.utils

import android.graphics.Bitmap
import androidx.palette.graphics.Palette
import androidx.core.graphics.ColorUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.math.roundToInt
import kotlin.math.sqrt

class AiColorService {

    /**
     * Extracts a balanced set of colors from the image:
     * - Dominant colors (high frequency)
     * - Rare but visually distinct accent colors (low frequency)
     *
     * Follows a ~70-80% dominant and ~20-30% rare distribution logic.
     */
    suspend fun suggestColors(bitmap: Bitmap, colorCount: Int = 6): List<Int> = withContext(Dispatchers.Default) {
        suspendCancellableCoroutine { continuation ->
            try {
                // Generate the palette with a high enough cluster count to find rare accent colors.
                Palette.from(bitmap).maximumColorCount(32).generate { palette ->
                    val swatches = palette?.swatches ?: emptyList()
                    if (swatches.isEmpty()) {
                        continuation.resume(emptyList())
                        return@generate
                    }

                    // Sort swatches by population (frequency) descending
                    val sortedSwatches = swatches.sortedByDescending { it.population }
                    val totalPopulation = sortedSwatches.sumOf { it.population }.toDouble()
                    
                    // Determine distribution: ~30% rare colors, ensuring at least 1 if count is reasonable (>=4)
                    val numRareTarget = (colorCount * 0.3).roundToInt().coerceIn(0, colorCount - 1)
                    val numDominantTarget = colorCount - numRareTarget

                    val selectedColors = mutableListOf<Int>()
                    
                    // 1. Pick Dominant Colors
                    // We take the most frequent ones that are visually distinct from each other.
                    for (swatch in sortedSwatches) {
                        if (selectedColors.size >= numDominantTarget) break
                        
                        val color = swatch.rgb
                        // Ensure this dominant color isn't too similar to already selected ones
                        if (selectedColors.none { !isDistinct(it, color) }) {
                            selectedColors.add(color)
                        }
                    }

                    // 2. Pick Rare Colors (Accents)
                    // We look at swatches with lower population, but avoid noise/artifacts.
                    // Presence threshold: at least 0.1% of total pixels clustered by Palette.
                    val minPopulationThreshold = (totalPopulation * 0.001).coerceAtLeast(10.0)
                    val rareCandidates = sortedSwatches
                        .filter { it.population >= minPopulationThreshold }
                        .reversed() // Start from the least frequent meaningful clusters

                    val rareColors = mutableListOf<Int>()
                    for (swatch in rareCandidates) {
                        if (rareColors.size >= numRareTarget) break
                        
                        val color = swatch.rgb
                        // Rare colors must be distinct from both the dominant set and other rare picks.
                        // We use a slightly stricter threshold (30.0) to ensure they are true highlights.
                        val isDistinctFromAll = (selectedColors + rareColors).none { !isDistinct(it, color, threshold = 30.0) }
                        if (isDistinctFromAll) {
                            rareColors.add(color)
                        }
                    }

                    // 3. Combine and return
                    // We preserve dominant colors at the start of the list for visual hierarchy.
                    val result = (selectedColors + rareColors).toMutableList()
                    
                    // Fallback: If we couldn't find enough distinct rare colors, fill with remaining swatches
                    if (result.size < colorCount) {
                        for (swatch in sortedSwatches) {
                            if (result.size >= colorCount) break
                            val color = swatch.rgb
                            if (!result.contains(color)) {
                                result.add(color)
                            }
                        }
                    }

                    continuation.resume(result.take(colorCount))
                }
            } catch (e: Exception) {
                android.util.Log.e("AiColorService", "Error extracting colors", e)
                continuation.resume(emptyList())
            }
        }
    }

    /**
     * Checks if two colors are visually distinct using Euclidean distance in LAB color space.
     * LAB distance is better aligned with human perception than RGB or HSV distance.
     * A threshold of ~25 is typically considered "perceptually different".
     */
    private fun isDistinct(color1: Int, color2: Int, threshold: Double = 25.0): Boolean {
        val lab1 = DoubleArray(3)
        val lab2 = DoubleArray(3)
        ColorUtils.colorToLAB(color1, lab1)
        ColorUtils.colorToLAB(color2, lab2)
        
        val deltaL = lab1[0] - lab2[0]
        val deltaA = lab1[1] - lab2[1]
        val deltaB = lab1[2] - lab2[2]
        val distance = sqrt(deltaL * deltaL + deltaA * deltaA + deltaB * deltaB)
        return distance > threshold
    }
}
