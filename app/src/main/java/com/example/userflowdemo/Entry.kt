package com.example.userflowdemo

import androidx.room.Entity
import androidx.room.PrimaryKey

data class MediaItem(
    val imageUri: String,
    val colors: List<Int> = emptyList()
)

@Entity
data class Entry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isDraft: Boolean = false,
    val media: List<MediaItem> = emptyList(),
    val observation: String? = null
)