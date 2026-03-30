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
                val colorsArray = obj.optJSONArray("colors")
                val colors = mutableListOf<Int>()
                if (colorsArray != null) {
                    for (j in 0 until colorsArray.length()) {
                        colors.add(colorsArray.getInt(j))
                    }
                }
                list.add(
                    MediaItem(
                        imageUri = obj.getString("uri"),
                        colors = colors
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
            obj.put("uri", item.imageUri)
            val colorsArray = JSONArray()
            item.colors.forEach { colorsArray.put(it) }
            obj.put("colors", colorsArray)
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
