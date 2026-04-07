package com.example.userflowdemo.utils

import android.graphics.Bitmap
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

class AiColorService {

    /**
     * Extracts prominent colors from the image using the Android Palette API.
     * This replaces the Gemini-based suggestion with a more reliable local extraction.
     */
    suspend fun suggestColors(bitmap: Bitmap, colorCount: Int = 6): List<Int> = withContext(Dispatchers.Default) {
        suspendCancellableCoroutine { continuation ->
            try {
                // Generate the palette.
                // .maximumColorCount(32) tells the engine to look at 32 color clusters
                Palette.from(bitmap).maximumColorCount(32).generate { palette ->
                    val colors = mutableSetOf<Int>()

                    // Get all swatches (color clusters) and sort them by population (frequency)
                    val swatches = palette?.swatches?.sortedByDescending { it.population }

                    // Take the top colorCount unique colors
                    swatches?.forEach { swatch ->
                        if (colors.size < colorCount) {
                            colors.add(swatch.rgb)
                        }
                    }

                    continuation.resume(colors.toList())
                }
            } catch (e: Exception) {
                android.util.Log.e("AiColorService", "Error extracting colors", e)
                continuation.resume(emptyList())
            }
        }
    }
}
