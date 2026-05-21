package com.example.smartpot.iot

import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

data class SensorData(
    val temperature: Float = 0f,
    val humidity: Float = 0f,
    val soilMoisture: Float = 0f,
    val light: Float = 0f,
    val timestamp: Long = System.currentTimeMillis()
)

class FirebaseManager {
    private val database: FirebaseDatabase? by lazy {
        try {
            FirebaseDatabase.getInstance()
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Firebase not initialized: ${e.message}")
            null
        }
    }

    fun observeSensorData(onDataChanged: (SensorData?) -> Unit) {
        val db = database ?: return
        val sensorRef = db.getReference("sensors")
        
        sensorRef.limitToLast(1).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val data = snapshot.children.firstOrNull()?.getValue(SensorData::class.java)
                onDataChanged(data)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseManager", "Database error: ${error.message}")
            }
        })
    }

    fun sendControlCommand(command: String, value: Any) {
        val db = database ?: return
        db.getReference("controls").child(command).setValue(value)
    }
}
