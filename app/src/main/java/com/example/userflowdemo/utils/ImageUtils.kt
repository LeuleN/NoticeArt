package com.example.userflowdemo.utils

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.*

object ImageUtils {
    fun saveBitmapToInternalStorage(context: Context, bitmap: Bitmap): Uri? {
        val filename = "texture_${UUID.randomUUID()}.jpg"
        val file = File(context.filesDir, filename)
        return try {
            val out = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            out.flush()
            out.close()
            Uri.fromFile(file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
