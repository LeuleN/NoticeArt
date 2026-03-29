package com.example.userflowdemo

import androidx.room.TypeConverter
import org.json.JSONArray
import org.json.JSONObject

class Converters {
    @TypeConverter
    fun fromMediaList(value: String): List<MediaItem> {
        if (value.isBlank()) return emptyList()
        return try {
            val jsonArray = JSONArray(value)
            val list = mutableListOf<MediaItem>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    MediaItem(
                        imageUri = obj.getString("uri"),
                        color = if (obj.has("color")) obj.getInt("color") else null
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun toMediaList(list: List<MediaItem>): String {
        val jsonArray = JSONArray()
        list.forEach { item ->
            val obj = JSONObject()
            obj.put("uri", item.imageUri)
            item.color?.let { obj.put("color", it) }
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
