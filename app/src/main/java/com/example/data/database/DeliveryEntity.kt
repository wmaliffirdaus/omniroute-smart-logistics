package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "delivery_stops")
data class DeliveryStop(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val address: String,
    val lat: Double,
    val lng: Double,
    val status: String, // PENDING, EN_ROUTE, ARRIVED, COMPLETED, FAILED
    val sequence: Int,
    val eta: String,
    val recipientName: String,
    val phoneNumber: String,
    val notes: String = "",
    val signatureBase64: String? = null,
    val photoBase64: String? = null,
    val barcode: String? = null,
    val completedAt: Long? = null,
    val isOfflineLogged: Boolean = false
)

@Entity(tableName = "location_logs")
data class LocationLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "sync_queue")
data class SyncQueue(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val stopId: Int,
    val actionType: String, // COMPLETE, UPDATE_NOTES, ADD_STOP
    val payload: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_accounts")
data class UserAccount(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val email: String,
    val passwordHash: String,
    val fullName: String,
    val sessionToken: String? = null
)

@Entity(tableName = "collaborative_pins")
data class CollaborativePin(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val lat: Double,
    val lng: Double,
    val title: String,
    val description: String = "",
    val createdBy: String,
    val colorHex: String, // hex string for pin styling
    val timestamp: Long = System.currentTimeMillis()
)

