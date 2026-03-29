package com.example.userflowdemo

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Entry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isDraft: Boolean = false,
    val imageUris: List<String> = emptyList(), // Changed from imageUri: String?
    val color: Int? = null,
    val observation: String? = null
)