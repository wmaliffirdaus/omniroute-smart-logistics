package com.example.data.repository

import com.example.data.database.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.delay

class DeliveryRepository(private val dao: DeliveryDao) {

    val allStopsFlow: Flow<List<DeliveryStop>> = dao.getAllStopsFlow()
    val allLocationLogsFlow: Flow<List<LocationLog>> = dao.getAllLocationLogsFlow()
    val allPinsFlow: Flow<List<CollaborativePin>> = dao.getAllPinsFlow()

    suspend fun getAllStops(): List<DeliveryStop> = dao.getAllStops()
    
    suspend fun getAllPins(): List<CollaborativePin> = dao.getAllPins()
    
    suspend fun insertPin(pin: CollaborativePin): Long = dao.insertPin(pin)
    
    suspend fun insertPins(pins: List<CollaborativePin>) = dao.insertPins(pins)
    
    suspend fun deletePinById(id: Int) = dao.deletePinById(id)
    
    suspend fun deleteAllPins() = dao.deleteAllPins()

    suspend fun getUserByEmail(email: String): UserAccount? = dao.getUserByEmail(email)

    suspend fun insertUser(user: UserAccount): Long = dao.insertUser(user)

    suspend fun updateUserSession(userId: Int, token: String?) = dao.updateUserSession(userId, token)

    suspend fun insertStop(stop: DeliveryStop): Long {
        return dao.insertStop(stop)
    }

    suspend fun insertStops(stops: List<DeliveryStop>) {
        dao.insertStops(stops)
    }

    suspend fun updateStop(stop: DeliveryStop) {
        dao.updateStop(stop)
    }

    suspend fun deleteStopById(id: Int) {
        dao.deleteStopById(id)
    }

    suspend fun deleteAllStops() {
        dao.deleteAllStops()
    }

    suspend fun logLocation(latitude: Double, longitude: Double) {
        dao.insertLocationLog(LocationLog(latitude = latitude, longitude = longitude))
    }

    suspend fun clearLocationLogs() {
        dao.clearLocationLogs()
    }

    // --- Offline-First Sync Engine ---
    suspend fun enqueueCompletedStopOffline(stopId: Int, signature: String?, photo: String?, barcode: String?, notes: String) {
        // Update local stop immediately as COMPLETED and mark as logged offline
        val stop = dao.getStopById(stopId)
        if (stop != null) {
            val updated = stop.copy(
                status = "COMPLETED",
                signatureBase64 = signature,
                photoBase64 = photo,
                barcode = barcode,
                notes = notes,
                completedAt = System.currentTimeMillis(),
                isOfflineLogged = true
            )
            dao.updateStop(updated)
        }

        // Add to sync queue
        val payload = """
            {
              "stopId": $stopId,
              "notes": "$notes",
              "barcode": "$barcode",
              "signatureLength": ${signature?.length ?: 0},
              "photoLength": ${photo?.length ?: 0}
            }
        """.trimIndent()
        dao.enqueueSyncAction(SyncQueue(stopId = stopId, actionType = "COMPLETE", payload = payload))
    }

    suspend fun syncPendingActions(onSyncProgress: (String) -> Unit): Int {
        val pending = dao.getPendingSyncQueue()
        if (pending.isEmpty()) return 0

        var syncedCount = 0
        onSyncProgress("Starting sync of ${pending.size} pending items...")
        
        for (item in pending) {
            delay(1000) // Simulate network latency
            onSyncProgress("Syncing stop ID #${item.stopId} to server...")
            
            // Mark the stop locally as synced (isOfflineLogged = false since it's now pushed to backend)
            val stop = dao.getStopById(item.stopId)
            if (stop != null) {
                dao.updateStop(stop.copy(isOfflineLogged = false))
            }
            
            // Remove from queue
            dao.dequeueSyncAction(item.id)
            syncedCount++
        }
        
        onSyncProgress("Sync completed successfully! $syncedCount actions synchronized.")
        return syncedCount
    }
}
