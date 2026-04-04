package com.example.userflowdemo

import androidx.room.TypeConverter
import org.json.JSONArray
import org.json.JSONObject

@Suppress("UNUSED")
class Converters {
    @TypeConverter
    fun fromMediaList(value: String): List<MediaItem> {
        if (value.isBlank()) return emptyList()
        return try {
            val jsonArray = JSONArray(value)
            val list = mutableListOf<MediaItem>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                
                val id = obj.optString("id", java.util.UUID.randomUUID().toString())
                val uri = obj.optString("uri").takeIf { it.isNotEmpty() }

                val colorsArray = obj.optJSONArray("colors")
                val colors = mutableListOf<Int>()
                if (colorsArray != null) {
                    for (j in 0 until colorsArray.length()) {
                        colors.add(colorsArray.getInt(j))
                    }
                }

                val texturesArray = obj.optJSONArray("textures")
                val textures = mutableListOf<Texture>()
                if (texturesArray != null) {
                    for (j in 0 until texturesArray.length()) {
                        val texObj = texturesArray.getJSONObject(j)
                        val cropObj = texObj.optJSONObject("cropRect")
                        val cropRect = if (cropObj != null) {
                            CropRect(
                                left = cropObj.getDouble("left").toFloat(),
                                top = cropObj.getDouble("top").toFloat(),
                                right = cropObj.getDouble("right").toFloat(),
                                bottom = cropObj.getDouble("bottom").toFloat()
                            )
                        } else null
                        
                        textures.add(
                            Texture(
                                id = texObj.getString("id"),
                                imageUri = texObj.optString("uri").takeIf { it.isNotEmpty() },
                                name = texObj.getString("name"),
                                isCustomName = texObj.optBoolean("isCustomName", false),
                                cropRect = cropRect
                            )
                        )
                    }
                }

                list.add(
                    MediaItem(
                        id = id,
                        imageUri = uri,
                        colors = colors,
                        textures = textures
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun toMediaList(list: List<MediaItem>): String {
        val jsonArray = JSONArray()
        list.forEach { item ->
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("uri", item.imageUri ?: "")
            
            val colorsArray = JSONArray()
            item.colors.forEach { colorsArray.put(it) }
            obj.put("colors", colorsArray)

            val texturesArray = JSONArray()
            item.textures.forEach { tex ->
                val texObj = JSONObject()
                texObj.put("id", tex.id)
                texObj.put("uri", tex.imageUri ?: "")
                texObj.put("name", tex.name)
                texObj.put("isCustomName", tex.isCustomName)
                
                tex.cropRect?.let { rect ->
                    val cropObj = JSONObject()
                    cropObj.put("left", rect.left.toDouble())
                    cropObj.put("top", rect.top.toDouble())
                    cropObj.put("right", rect.right.toDouble())
                    cropObj.put("bottom", rect.bottom.toDouble())
                    texObj.put("cropRect", cropObj)
                }
                texturesArray.put(texObj)
            }
            obj.put("textures", texturesArray)

            jsonArray.put(obj)
        }
        return jsonArray.toString()
    }

    @TypeConverter
    fun fromString(value: String): List<String> {
        return if (value.isBlank()) emptyList() else value.split(",")
    }

    @TypeConverter
    fun fromList(list: List<String>): String {
        return list.joinToString(",")
    }
}
