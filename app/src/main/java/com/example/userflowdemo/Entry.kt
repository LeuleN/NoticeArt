package com.example.userflowdemo

import androidx.room.Entity
import androidx.room.PrimaryKey

data class Texture(
    val id: String = java.util.UUID.randomUUID().toString(),
    val imageUri: String?, 
    val name: String,
    val isCustomName: Boolean = false,
    val cropRect: CropRect? = null
)

data class CropRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
)

data class MediaItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val imageUri: String?,
    val colors: List<Int> = emptyList(),
    val textures: List<Texture> = emptyList()
)

@Entity
data class Entry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isDraft: Boolean = false,
    val media: List<MediaItem> = emptyList(),
    val audioUris: List<String> = emptyList(),
    val observation: String? = null
)
