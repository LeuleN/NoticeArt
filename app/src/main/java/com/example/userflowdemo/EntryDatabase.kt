package com.example.userflowdemo

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Entry::class],
    version = 100
)
abstract class EntryDatabase : RoomDatabase() {

    abstract fun entryDao(): EntryDao

    companion object {
        @Volatile
        private var INSTANCE: EntryDatabase? = null

        fun getDatabase(context: Context): EntryDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    EntryDatabase::class.java,
                    "entry_database_new"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}