package com.example.userflowdemo.utils

import com.example.userflowdemo.Entry

/**
 * Helper to determine if a draft is effectively empty and should be discarded.
 */
fun isDraftEmpty(entry: Entry?): Boolean {
    return entry == null || (entry.title.isBlank() && entry.imageUris.isEmpty() && entry.color == null && entry.observation.isNullOrBlank())
}

/**
 * Helper to determine if an entry has changed.
 */
fun hasEntryChanged(original: Entry?, current: Entry?): Boolean {
    if (original == null || current == null) return false
    return original.title != current.title ||
            original.imageUris != current.imageUris ||
            original.color != current.color ||
            original.observation != current.observation
}