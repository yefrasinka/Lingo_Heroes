package com.example.lingoheroesapp.utils

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

/**
 * Klasa pomocnicza do debugowania deserializacji danych z Firebase
 */
object FirebaseDebugHelper {
    
    private const val TAG = "FirebaseDebugHelper"
    
    /**
     * Loguje strukturę danych z DataSnapshot
     */
    fun logDataSnapshot(snapshot: DataSnapshot, prefix: String = "") {
        if (!snapshot.exists()) {
            Log.d(TAG, "$prefix - Snapshot does not exist")
            return
        }
        
        Log.d(TAG, "$prefix - Key: ${snapshot.key}, Value type: ${if (snapshot.value == null) "null" else snapshot.value!!::class.java.simpleName}")
        
        if (snapshot.hasChildren()) {
            for (child in snapshot.children) {
                logDataSnapshot(child, "$prefix > ${snapshot.key}")
            }
        } else {
            Log.d(TAG, "$prefix > ${snapshot.key} = ${snapshot.value}")
        }
    }
    
    /**
     * Tworzy ValueEventListener, który loguje dane przed deserializacją
     */
    fun <T> createDebuggingListener(
        tag: String,
        valueType: Class<T>,
        onSuccess: (T) -> Unit,
        onError: (DatabaseError) -> Unit
    ): ValueEventListener {
        return object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d(TAG, "===== DEBUG FOR $tag =====")
                logDataSnapshot(snapshot)
                
                try {
                    val value = snapshot.getValue(valueType)
                    if (value != null) {
                        Log.d(TAG, "Successfully deserialized to ${valueType.simpleName}")
                        onSuccess(value)
                    } else {
                        Log.e(TAG, "Deserialization returned null for ${valueType.simpleName}")
                        onError(DatabaseError.fromException(
                            Exception("Deserialization returned null")))
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error deserializing to ${valueType.simpleName}: ${e.message}")
                    e.printStackTrace()
                    onError(DatabaseError.fromException(e))
                }
            }
            
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Firebase operation cancelled: ${error.message}")
                onError(error)
            }
        }
    }
} 