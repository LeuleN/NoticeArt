package com.example.userflowdemo.utils

import android.graphics.Bitmap
import android.graphics.Color
import com.example.userflowdemo.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content

class AiColorService {
    private val generativeModel = GenerativeModel(
        modelName = "gemini-1.5-flash-latest",
        apiKey = BuildConfig.GEMINI_API_KEY
    )

    suspend fun suggestColors(bitmap: Bitmap): List<Int> {
        val prompt = "Extract 5-6 visually prominent and high-contrast colors from this image. " +
                     "Return ONLY a list of hex strings like [#RRGGBB, #RRGGBB]."
        
        return try {
            val response = generativeModel.generateContent(
                content {
                    image(bitmap)
                    text(prompt)
                }
            )
            parseHexColors(response.text ?: "")
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parseHexColors(text: String): List<Int> {
        val hexRegex = Regex("#[0-9A-Fa-f]{6}")
        return hexRegex.findAll(text).map { 
            try {
                Color.parseColor(it.value)
            } catch (e: Exception) {
                null
            }
        }.filterNotNull().toList()
    }
}
