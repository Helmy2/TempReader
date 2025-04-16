package com.example.tempreader.ui

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.tempreader.data.model.Reading
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase


class MainViewModel : ViewModel() {
    private val _readings = mutableStateOf<List<Reading>>(emptyList())
    val readings: State<List<Reading>> = _readings

    fun fetchReadings(context: Context) {
        val database =
            Firebase.database("https://esp-temp-89f99-default-rtdb.europe-west1.firebasedatabase.app")
        val ref = database.getReference("/UsersData").child("IVcnpuP1hiX3p7SgsAa1n0M6gcI2")
            .child("readings")

        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val newReadings = mutableListOf<Reading>()
                for (child in snapshot.children) {
                    val temp = child.child("temperature").getValue(Float::class.java) ?: 0f
                    val hum = child.child("humidity").getValue(Float::class.java) ?: 0f
                    val time = child.child("timestamp").getValue(Long::class.java) ?: 0L
                    Log.d("TAG", "onDataChange: $time")
                    newReadings.add(Reading(temp, hum, time))
                }
                _readings.value = newReadings.sortedBy { it.timestamp }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
