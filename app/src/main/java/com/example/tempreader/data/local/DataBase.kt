package com.example.tempreader.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

// --- Entity ---
// Added default values to the constructor. This is the fix.
// It allows Firebase to create an instance of the class when deserializing data.
// It also makes the class a valid Room entity.

@Entity(tableName = "readings_table")
data class Reading(
    @PrimaryKey val timestamp: Long = 0L,
    val temperature: Float = 0f,
    val humidity: Float = 0f
)


// --- Data Access Object (DAO) ---
// This interface declares all the necessary database operations.
// Using Flow ensures that the UI will automatically update when the data changes.

@Dao
interface ReadingDao {
    /**
     * Inserts a list of readings into the database. If a reading with the same
     * timestamp already exists, it will be replaced.
     * @param readings The list of readings to insert.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(readings: List<Reading>)

    /**
     * Fetches all readings from the database, ordered by timestamp.
     * @return A Flow emitting the list of all readings whenever the data changes.
     */
    @Query("SELECT * FROM readings_table ORDER BY timestamp ASC")
    fun getAllReadings(): Flow<List<Reading>>

    /**
     * Deletes all readings from the database.
     */
    @Query("DELETE FROM readings_table")
    suspend fun deleteAll()
}


// --- Database ---
// This is the main database class for the application.
// It defines the entities that the database contains and provides access to the DAOs.

@Database(entities = [Reading::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun readingDao(): ReadingDao

    companion object {
        // @Volatile ensures that the INSTANCE is always up-to-date and the same for all execution threads.
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: android.content.Context): AppDatabase {
            // Return the existing instance if it exists, otherwise create a new database instance.
            return INSTANCE ?: synchronized(this) {
                val instance = androidx.room.Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
