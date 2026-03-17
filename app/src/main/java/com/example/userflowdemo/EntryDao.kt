package com.example.userflowdemo
// Leule was here

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface EntryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: Entry)

    @Update
    suspend fun update(entry: Entry)

    @Query("SELECT * FROM Entry ORDER BY timestamp DESC")
    fun getAllEntries(): Flow<List<Entry>>

    @Query("SELECT * FROM Entry WHERE isDraft = 1 LIMIT 1")
    suspend fun getDraft(): Entry?

    @Query("DELETE FROM Entry WHERE id = :id")
    suspend fun deleteById(id: Long)
}