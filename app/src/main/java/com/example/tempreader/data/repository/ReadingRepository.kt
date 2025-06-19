package com.example.tempreader.data.repository

import android.util.Log
import com.example.tempreader.data.local.Reading
import com.example.tempreader.data.local.ReadingDao
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * Repository class that serves as the single source of truth for sensor data.
 * It abstracts data operations from the ViewModels and handles fetching data
 * from the local Room database and the remote Firebase Realtime Database.
 *
 * @param readingDao The Data Access Object for the Reading entity.
 */
class ReadingRepository(private val readingDao: ReadingDao) {

    // Expose a Flow of all readings from the database. The UI will collect this Flow.
    val allReadings: Flow<List<Reading>> = readingDao.getAllReadings()

    /**
     * Fetches the latest readings from Firebase and updates the local Room database.
     * This function is the bridge between the remote and local data sources.
     */
    fun syncWithFirebase() {
        Log.d("TAG", "syncWithFirebase init")

        val database = Firebase.database("https://esp-temp-89f99-default-rtdb.europe-west1.firebasedatabase.app")
        val ref = database.getReference("/UsersData/IVcnpuP1hiX3p7SgsAa1n0M6gcI2/readings")

        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val newReadings = mutableListOf<Reading>()
                for (child in snapshot.children) {
                    try {
                        val reading = child.getValue(Reading::class.java)
                        if (reading != null) {
                            newReadings.add(reading)
                        }
                    } catch (e: Exception) {
                        Log.e("ReadingRepository", "Error parsing reading: ${e.message}")
                    }
                }

                // Once we have the new list from Firebase, we launch a coroutine
                // to insert them into the Room database.
                CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    if (newReadings.isNotEmpty()) {
                        readingDao.insertAll(newReadings)
                        Log.d("ReadingRepository", "${newReadings.size} readings synced from Firebase to Room.")
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ReadingRepository", "Firebase sync failed: ${error.message}")
                // In a production app, you might want to expose this error to the UI.
            }
        })
    }
}
