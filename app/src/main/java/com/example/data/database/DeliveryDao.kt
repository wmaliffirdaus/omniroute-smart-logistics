package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DeliveryDao {
    // --- Delivery Stops ---
    @Query("SELECT * FROM delivery_stops ORDER BY sequence ASC")
    fun getAllStopsFlow(): Flow<List<DeliveryStop>>

    @Query("SELECT * FROM delivery_stops ORDER BY sequence ASC")
    suspend fun getAllStops(): List<DeliveryStop>

    @Query("SELECT * FROM delivery_stops WHERE id = :id")
    suspend fun getStopById(id: Int): DeliveryStop?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStop(stop: DeliveryStop): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStops(stops: List<DeliveryStop>)

    @Update
    suspend fun updateStop(stop: DeliveryStop)

    @Query("DELETE FROM delivery_stops WHERE id = :id")
    suspend fun deleteStopById(id: Int)

    @Query("DELETE FROM delivery_stops")
    suspend fun deleteAllStops()

    // --- Location Logs ---
    @Query("SELECT * FROM location_logs ORDER BY timestamp DESC")
    fun getAllLocationLogsFlow(): Flow<List<LocationLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocationLog(log: LocationLog)

    @Query("DELETE FROM location_logs")
    suspend fun clearLocationLogs()

    // --- Sync Queue ---
    @Query("SELECT * FROM sync_queue ORDER BY timestamp ASC")
    suspend fun getPendingSyncQueue(): List<SyncQueue>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueueSyncAction(item: SyncQueue)

    @Query("DELETE FROM sync_queue WHERE id = :id")
    suspend fun dequeueSyncAction(id: Int)

    // --- User Accounts ---
    @Query("SELECT * FROM user_accounts WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserAccount?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserAccount): Long

    @Query("UPDATE user_accounts SET sessionToken = :token WHERE id = :userId")
    suspend fun updateUserSession(userId: Int, token: String?)

    // --- Collaborative Pins ---
    @Query("SELECT * FROM collaborative_pins ORDER BY timestamp DESC")
    fun getAllPinsFlow(): Flow<List<CollaborativePin>>

    @Query("SELECT * FROM collaborative_pins ORDER BY timestamp DESC")
    suspend fun getAllPins(): List<CollaborativePin>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPin(pin: CollaborativePin): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPins(pins: List<CollaborativePin>)

    @Query("DELETE FROM collaborative_pins WHERE id = :id")
    suspend fun deletePinById(id: Int)

    @Query("DELETE FROM collaborative_pins")
    suspend fun deleteAllPins()
}
