package com.example.tempreader.data.repository

import android.util.Log
import com.example.tempreader.data.local.Reading
import com.example.tempreader.data.local.ReadingDao
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class ReadingRepository(
    private val readingDao: ReadingDao,
    private val auth: FirebaseAuth
) {

    val allReadings: Flow<List<Reading>> = readingDao.getAllReadings()

    fun syncWithFirebase() {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            Log.e("ReadingRepository", "User is not logged in. Cannot sync with Firebase.")
            return
        }

        val database = Firebase.database("https://esp-temp-89f99-default-rtdb.europe-west1.firebasedatabase.app")
        val path = "/UsersData/$uid/readings"
        val ref = database.getReference(path)

        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val newReadings = snapshot.children.mapNotNull { it.getValue(Reading::class.java) }

                if (newReadings.isNotEmpty()) {
                    CoroutineScope(Dispatchers.IO).launch {
                        readingDao.insertAll(newReadings)
                        Log.d("ReadingRepository", "${newReadings.size} readings synced from path: $path")
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ReadingRepository", "Firebase sync failed for path $path: ${error.message}")
            }
        })
    }
}
