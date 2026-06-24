package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.GeminiClient
import com.example.data.database.*
import com.example.data.repository.DeliveryRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.*

data class SyncConflict(
    val stopId: Int,
    val localStop: DeliveryStop,
    val remoteStop: DeliveryStop
)

class DeliveryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: DeliveryRepository
    private val prefs = application.getSharedPreferences("omniroute_prefs", android.content.Context.MODE_PRIVATE)
    
    // Core State Flows
    val stops: StateFlow<List<DeliveryStop>>
    val locationLogs: StateFlow<List<LocationLog>>
    val pins: StateFlow<List<CollaborativePin>>
    
    private val _currentUser = MutableStateFlow<UserAccount?>(null)
    val currentUser: StateFlow<UserAccount?> = _currentUser.asStateFlow()

    private val _activeConflict = MutableStateFlow<SyncConflict?>(null)
    val activeConflict: StateFlow<SyncConflict?> = _activeConflict.asStateFlow()

    private val _webSocketEvents = MutableSharedFlow<String>(extraBufferCapacity = 15)
    val webSocketEvents: SharedFlow<String> = _webSocketEvents.asSharedFlow()
    
    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _syncStatus = MutableStateFlow("Synced")
    val syncStatus: StateFlow<String> = _syncStatus.asStateFlow()

    // GPS Simulation Coordinates (Centered in San Francisco)
    private val _gpsLat = MutableStateFlow(37.7749)
    val gpsLat: StateFlow<Double> = _gpsLat.asStateFlow()

    private val _gpsLng = MutableStateFlow(-122.4194)
    val gpsLng: StateFlow<Double> = _gpsLng.asStateFlow()

    private val _isGpsSimulating = MutableStateFlow(false)
    val isGpsSimulating: StateFlow<Boolean> = _isGpsSimulating.asStateFlow()

    private val _geofenceAlert = MutableStateFlow<String?>(null)
    val geofenceAlert: StateFlow<String?> = _geofenceAlert.asStateFlow()

    private val _activeRole = MutableStateFlow("DRIVER") // DRIVER or MANAGER
    val activeRole: StateFlow<String> = _activeRole.asStateFlow()

    // Gemini AI Assistant State
    private val _geminiResponse = MutableStateFlow("")
    val geminiResponse: StateFlow<String> = _geminiResponse.asStateFlow()

    private val _isGeminiLoading = MutableStateFlow(false)
    val isGeminiLoading: StateFlow<Boolean> = _isGeminiLoading.asStateFlow()

    // Active Delivery State
    private val _activeStopId = MutableStateFlow<Int?>(null)
    val activeStopId: StateFlow<Int?> = _activeStopId.asStateFlow()

    private var gpsJob: Job? = null

    private var webSocketJob: Job? = null

    init {
        val database = DeliveryDatabase.getDatabase(application)
        repository = DeliveryRepository(database.deliveryDao())

        stops = repository.allStopsFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        locationLogs = repository.allLocationLogsFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        pins = repository.allPinsFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Prepopulate database with default stops and user accounts, and restore session
        viewModelScope.launch {
            val currentStops = repository.getAllStops()
            if (currentStops.isEmpty()) {
                prepopulateDatabase()
            }
            prepopulateDefaultUsers()
            restoreUserSession()
        }

        // Start WebSocket collaboration and background sync simulation
        startWebSocketSimulation()
    }

    private suspend fun prepopulateDatabase() {
        val defaultStops = listOf(
            DeliveryStop(
                address = "742 Evergreen Terrace",
                lat = 37.7833,
                lng = -122.4167,
                status = "PENDING",
                sequence = 1,
                eta = "09:30 AM",
                recipientName = "Homer Simpson",
                phoneNumber = "555-0199",
                notes = "Leave package by the garage door if not home."
            ),
            DeliveryStop(
                address = "123 Maple Street",
                lat = 37.7699,
                lng = -122.4468,
                status = "PENDING",
                sequence = 2,
                eta = "10:15 AM",
                recipientName = "John Doe",
                phoneNumber = "555-4321",
                notes = "Requires signature. Knock on front door."
            ),
            DeliveryStop(
                address = "456 Oak Avenue",
                lat = 37.7512,
                lng = -122.4112,
                status = "PENDING",
                sequence = 3,
                eta = "11:45 AM",
                recipientName = "Alice Smith",
                phoneNumber = "555-8765",
                notes = "Call upon arrival. Access code: #4560"
            ),
            DeliveryStop(
                address = "890 Pine Boulevard",
                lat = 37.7954,
                lng = -122.4023,
                status = "PENDING",
                sequence = 4,
                eta = "01:15 PM",
                recipientName = "Sarah Connor",
                phoneNumber = "555-9000",
                notes = "Deliver to lobby reception."
            )
        )
        repository.insertStops(defaultStops)
    }

    // Role switcher
    fun setRole(role: String) {
        _activeRole.value = role
    }

    // Online/Offline simulation switcher
    fun toggleOnlineStatus() {
        _isOnline.value = !_isOnline.value
        if (_isOnline.value) {
            _syncStatus.value = "Online"
            syncOfflineQueue()
        } else {
            _syncStatus.value = "Offline Mode"
        }
    }

    // Run offline-first queue-sync
    fun syncOfflineQueue() {
        viewModelScope.launch {
            if (!_isOnline.value) return@launch
            repository.syncPendingActions { progress ->
                _syncStatus.value = progress
            }
            _syncStatus.value = "Synced"
        }
    }

    // --- Dynamic Route Optimization (Greedy TSP Engine) ---
    fun optimizeStopsSequence() {
        viewModelScope.launch {
            val allStops = repository.getAllStops()
            if (allStops.isEmpty()) return@launch

            val pending = allStops.filter { it.status == "PENDING" || it.status == "EN_ROUTE" || it.status == "ARRIVED" }
            val completed = allStops.filter { it.status == "COMPLETED" || it.status == "FAILED" }.sortedBy { it.sequence }

            var currentLat = _gpsLat.value
            var currentLng = _gpsLng.value
            val unvisited = pending.toMutableList()
            val optimizedPending = mutableListOf<DeliveryStop>()

            var seqCounter = completed.size + 1

            while (unvisited.isNotEmpty()) {
                // Find nearest unvisited stop
                var nearestIndex = 0
                var minDistance = Double.MAX_VALUE

                for (i in unvisited.indices) {
                    val dist = calculateDistance(currentLat, currentLng, unvisited[i].lat, unvisited[i].lng)
                    if (dist < minDistance) {
                        minDistance = dist
                        nearestIndex = i
                    }
                }

                val nearestStop = unvisited.removeAt(nearestIndex)
                // Update coordinates to simulate sequence jumps
                currentLat = nearestStop.lat
                currentLng = nearestStop.lng

                // Generate a realistic updated ETA
                val calculatedEta = generateUpdatedEta(seqCounter)
                optimizedPending.add(nearestStop.copy(sequence = seqCounter, eta = calculatedEta))
                seqCounter++
            }

            // Write back updated sequences
            val updatedAllStops = completed + optimizedPending
            repository.deleteAllStops()
            repository.insertStops(updatedAllStops)

            // Select first pending stop to be EN_ROUTE automatically
            val nextPending = optimizedPending.firstOrNull()
            if (nextPending != null) {
                repository.updateStop(nextPending.copy(status = "EN_ROUTE"))
                _activeStopId.value = nextPending.id
            }
        }
    }

    private fun generateUpdatedEta(index: Int): String {
        val baseHour = 9 + (index - 1)
        val mins = if (index % 2 == 0) "15" else "45"
        val amPm = if (baseHour >= 12) "PM" else "AM"
        val displayHour = if (baseHour > 12) baseHour - 12 else baseHour
        return String.format("%02d:%s %s", displayHour, mins, amPm)
    }

    // --- Live Location GPS Simulation ---
    fun toggleGpsSimulation() {
        if (_isGpsSimulating.value) {
            _isGpsSimulating.value = false
            gpsJob?.cancel()
        } else {
            _isGpsSimulating.value = true
            startGpsLoop()
        }
    }

    private fun startGpsLoop() {
        gpsJob?.cancel()
        gpsJob = viewModelScope.launch {
            while (true) {
                val currentStops = repository.getAllStops()
                val enRouteStop = currentStops.firstOrNull { it.status == "EN_ROUTE" || it.status == "ARRIVED" }

                if (enRouteStop != null) {
                    _activeStopId.value = enRouteStop.id
                    val targetLat = enRouteStop.lat
                    val targetLng = enRouteStop.lng

                    val currentLat = _gpsLat.value
                    val currentLng = _gpsLng.value

                    val dLat = targetLat - currentLat
                    val dLng = targetLng - currentLng
                    val dist = sqrt(dLat * dLat + dLng * dLng)

                    if (dist > 0.0005) {
                        // Move step towards target
                        val step = 0.0004 // Simulated speed step
                        val moveLat = currentLat + (dLat / dist) * step
                        val moveLng = currentLng + (dLng / dist) * step
                        
                        _gpsLat.value = moveLat
                        _gpsLng.value = moveLng
                        repository.logLocation(moveLat, moveLng)
                        
                        // Check if geofence boundary is crossed (approx. 100 meters)
                        val distMeters = calculateDistance(moveLat, moveLng, targetLat, targetLng)
                        if (distMeters <= 100.0 && enRouteStop.status == "EN_ROUTE") {
                            // Enters Geofence Zone
                            repository.updateStop(enRouteStop.copy(status = "ARRIVED"))
                            _geofenceAlert.value = "Entered Geofence around: ${enRouteStop.address}. Tap to complete signature and secure proof of delivery!"
                        }
                    } else {
                        // Arrived perfectly
                        _gpsLat.value = targetLat
                        _gpsLng.value = targetLng
                        if (enRouteStop.status == "EN_ROUTE") {
                            repository.updateStop(enRouteStop.copy(status = "ARRIVED"))
                        }
                    }
                } else {
                    // No stop en-route. Auto find first pending stop and set to en route
                    val firstPending = currentStops.firstOrNull { it.status == "PENDING" }
                    if (firstPending != null) {
                        repository.updateStop(firstPending.copy(status = "EN_ROUTE"))
                        _activeStopId.value = firstPending.id
                    }
                }
                delay(1200) // update frequency
            }
        }
    }

    fun dismissGeofenceAlert() {
        _geofenceAlert.value = null
    }

    // --- Proof of Delivery Complete Action ---
    fun completeActiveStop(
        stopId: Int,
        signature: String?,
        photo: String?,
        barcode: String?,
        notes: String
    ) {
        viewModelScope.launch {
            if (_isOnline.value) {
                // Online immediate update
                val stop = repository.getAllStops().firstOrNull { it.id == stopId }
                if (stop != null) {
                    val completedStop = stop.copy(
                        status = "COMPLETED",
                        signatureBase64 = signature,
                        photoBase64 = photo,
                        barcode = barcode,
                        notes = notes,
                        completedAt = System.currentTimeMillis(),
                        isOfflineLogged = false
                    )
                    repository.updateStop(completedStop)
                    _syncStatus.value = "Synced"
                }
            } else {
                // Offline logged queue
                repository.enqueueCompletedStopOffline(stopId, signature, photo, barcode, notes)
                _syncStatus.value = "Offline Mode: 1 Action Queued"
            }

            // After completion, clear active stop alert, and automatically trigger next route en-route
            dismissGeofenceAlert()
            val allStops = repository.getAllStops()
            val nextPending = allStops.firstOrNull { it.status == "PENDING" }
            if (nextPending != null) {
                repository.updateStop(nextPending.copy(status = "EN_ROUTE"))
                _activeStopId.value = nextPending.id
            } else {
                _activeStopId.value = null
            }
        }
    }

    // --- Fleet Manager Assignment ---
    fun assignNewStopFromManager(address: String, recipient: String, phone: String, notes: String) {
        viewModelScope.launch {
            val allStops = repository.getAllStops()
            val nextSeq = (allStops.maxByOrNull { it.sequence }?.sequence ?: 0) + 1
            
            // Randomly scatter coordinates around current GPS with minor variations
            val randomOffsetLat = (Math.random() - 0.5) * 0.04
            val randomOffsetLng = (Math.random() - 0.5) * 0.04
            val newLat = _gpsLat.value + randomOffsetLat
            val newLng = _gpsLng.value + randomOffsetLng

            val newStop = DeliveryStop(
                address = address,
                lat = newLat,
                lng = newLng,
                status = "PENDING",
                sequence = nextSeq,
                eta = generateUpdatedEta(nextSeq),
                recipientName = recipient,
                phoneNumber = phone,
                notes = notes
            )
            repository.insertStop(newStop)
        }
    }

    // Reset database stops
    fun resetAllStops() {
        viewModelScope.launch {
            repository.deleteAllStops()
            repository.clearLocationLogs()
            prepopulateDatabase()
            _activeStopId.value = null
            _gpsLat.value = 37.7749
            _gpsLng.value = -122.4194
        }
    }

    // --- Smart AI Delivery Assistant (Gemini) ---
    fun askGeminiAssistant(queryType: String) {
        viewModelScope.launch {
            _isGeminiLoading.value = true
            _geminiResponse.value = "Analyzing routing parameters..."

            val allStops = repository.getAllStops()
            val stopsSummary = allStops.joinToString("\n") {
                "- Seq ${it.sequence}: ${it.address} (Recipient: ${it.recipientName}, Status: ${it.status}, Coordinates: ${it.lat}, ${it.lng})"
            }

            val prompt = when (queryType) {
                "SCHEDULE" -> {
                    """
                    You are OmniRoute's Enterprise Logistics Optimizer. Here is our list of delivery stops:
                    $stopsSummary
                    
                    Please suggest an optimal delivery schedule. Identify which stop should be serviced first, suggest timing increments, and explain how the sequence minimizes fuel consumption and driver fatigue. Return a clean, formatted markdown response.
                    """.trimIndent()
                }
                "TRAFFIC" -> {
                    """
                    You are OmniRoute's real-time Traffic Analyzer. Here are our active stops:
                    $stopsSummary
                    
                    Simulate real-time traffic conditions in San Francisco and predict delay factors for each stop. Highlight any bottleneck roads and recommend concrete speed adjustment intervals.
                    """.trimIndent()
                }
                "RISK" -> {
                    """
                    You are OmniRoute's Fleet Risk & Safety Officer. Here is our delivery plan:
                    $stopsSummary
                    
                    Evaluate high-risk areas in this route. Considerations: narrow lanes, double-parking difficulty, high pedestrian density, or school zone speed limits. Suggest preventive safety protocols for the driver.
                    """.trimIndent()
                }
                else -> "Analyze our current delivery stops and provide operational efficiency improvements."
            }

            val systemInstruction = "You are a professional enterprise SaaS logistics routing expert. Keep recommendations clean, factual, professional, and actionable."
            val responseText = GeminiClient.getAssistantResponse(prompt, systemInstruction)
            _geminiResponse.value = responseText
            _isGeminiLoading.value = false
        }
    }

    // --- Enterprise Authentication & Sessions ---
    private suspend fun prepopulateDefaultUsers() {
        val existingDriver = repository.getUserByEmail("driver@omniroute.com")
        if (existingDriver == null) {
            repository.insertUser(UserAccount(
                email = "driver@omniroute.com",
                passwordHash = "password", // mock password for evaluation
                fullName = "John Driver"
            ))
        }
        val existingManager = repository.getUserByEmail("manager@omniroute.com")
        if (existingManager == null) {
            repository.insertUser(UserAccount(
                email = "manager@omniroute.com",
                passwordHash = "password",
                fullName = "Jane Manager"
            ))
        }
    }

    private suspend fun restoreUserSession() {
        val savedEmail = prefs.getString("logged_in_user_email", null)
        if (savedEmail != null) {
            val user = repository.getUserByEmail(savedEmail)
            if (user != null) {
                _currentUser.value = user
                if (savedEmail.contains("manager", ignoreCase = true)) {
                    _activeRole.value = "MANAGER"
                } else {
                    _activeRole.value = "DRIVER"
                }
            }
        }
    }

    fun login(email: String, password: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val user = repository.getUserByEmail(email)
            if (user != null && user.passwordHash == password) {
                _currentUser.value = user
                prefs.edit().putString("logged_in_user_email", email).apply()
                if (email.contains("manager", ignoreCase = true)) {
                    _activeRole.value = "MANAGER"
                } else {
                    _activeRole.value = "DRIVER"
                }
                onSuccess()
            } else {
                onError("Invalid email or password.")
            }
        }
    }

    fun register(email: String, password: String, name: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            if (email.isBlank() || password.isBlank() || name.isBlank()) {
                onError("Please fill out all fields.")
                return@launch
            }
            val existing = repository.getUserByEmail(email)
            if (existing != null) {
                onError("An account with this email already exists.")
                return@launch
            }
            val newUser = UserAccount(email = email, passwordHash = password, fullName = name)
            repository.insertUser(newUser)
            _currentUser.value = newUser
            prefs.edit().putString("logged_in_user_email", email).apply()
            if (email.contains("manager", ignoreCase = true)) {
                _activeRole.value = "MANAGER"
            } else {
                _activeRole.value = "DRIVER"
            }
            onSuccess()
        }
    }

    fun logout() {
        _currentUser.value = null
        prefs.edit().remove("logged_in_user_email").apply()
    }

    fun resetPassword(email: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            if (email.isBlank()) {
                onError("Please enter your email.")
                return@launch
            }
            val user = repository.getUserByEmail(email)
            if (user != null) {
                onSuccess()
            } else {
                onError("No user account found with this email.")
            }
        }
    }

    // --- WebSockets Collaborative Collaboration & Sync Conflicts ---
    fun startWebSocketSimulation() {
        webSocketJob?.cancel()
        webSocketJob = viewModelScope.launch {
            var counter = 0
            while (true) {
                delay(15000)
                if (_isOnline.value && _currentUser.value != null) {
                    counter++
                    when (counter % 4) {
                        1 -> {
                            val offsetLat = (Math.random() - 0.5) * 0.02
                            val offsetLng = (Math.random() - 0.5) * 0.02
                            val pinLat = _gpsLat.value + offsetLat
                            val pinLng = _gpsLng.value + offsetLng
                            val pin = CollaborativePin(
                                lat = pinLat,
                                lng = pinLng,
                                title = "Sarah: Delay alert",
                                description = "Road construction bottleneck on 4th Ave.",
                                createdBy = "Dispatcher Sarah",
                                colorHex = "#EF4444"
                            )
                            repository.insertPin(pin)
                            _webSocketEvents.emit("Sarah pinned a real-time detour on the map")
                        }
                        2 -> {
                            val allStops = repository.getAllStops()
                            val pendingStops = allStops.filter { it.status == "PENDING" || it.status == "EN_ROUTE" }
                            if (pendingStops.isNotEmpty()) {
                                val target = pendingStops.first()
                                val remoteStop = target.copy(
                                    notes = "REMOTE OVERWRITE: Direct dropoff inside Suite 502 with gate passcode #0812.",
                                    eta = "11:15 AM"
                                )
                                _activeConflict.value = SyncConflict(
                                    stopId = target.id,
                                    localStop = target,
                                    remoteStop = remoteStop
                                )
                                _webSocketEvents.emit("🚨 Conflict: Remote dispatcher modified details of Stop #${target.id}!")
                            }
                        }
                        3 -> {
                            val offsetLat = (Math.random() - 0.5) * 0.01
                            val offsetLng = (Math.random() - 0.5) * 0.01
                            val pinLat = _gpsLat.value + offsetLat
                            val pinLng = _gpsLng.value + offsetLng
                            val pin = CollaborativePin(
                                lat = pinLat,
                                lng = pinLng,
                                title = "Bob: Open Loading Spot",
                                description = "Large commercial vehicle parking spot free.",
                                createdBy = "Courier Bob",
                                colorHex = "#10B981"
                            )
                            repository.insertPin(pin)
                            _webSocketEvents.emit("Bob added an open loading zone pin")
                        }
                        0 -> {
                            _webSocketEvents.emit("Fleet Broadcast: SF central zone traffic cleared.")
                        }
                    }
                }
            }
        }
    }

    fun addCollaborativePinFromUser(title: String, description: String, lat: Double, lng: Double, colorHex: String) {
        viewModelScope.launch {
            val user = _currentUser.value
            val author = user?.fullName ?: "Driver"
            val pin = CollaborativePin(
                lat = lat,
                lng = lng,
                title = "$author: $title",
                description = description,
                createdBy = author,
                colorHex = colorHex
            )
            repository.insertPin(pin)
            if (_isOnline.value) {
                _webSocketEvents.emit("You pinned: '$title' on the collaborative map")
            }
        }
    }

    fun deleteCollaborativePin(id: Int) {
        viewModelScope.launch {
            repository.deletePinById(id)
            if (_isOnline.value) {
                _webSocketEvents.emit("A pin was removed from the shared map")
            }
        }
    }

    // --- Conflict Resolution Strategy Implementations ---
    fun resolveConflictWithLocal() {
        viewModelScope.launch {
            val conflict = _activeConflict.value ?: return@launch
            _webSocketEvents.emit("Resolved conflict for Stop #${conflict.stopId} using LOCAL driver data.")
            _activeConflict.value = null
        }
    }

    fun resolveConflictWithRemote() {
        viewModelScope.launch {
            val conflict = _activeConflict.value ?: return@launch
            repository.updateStop(conflict.remoteStop)
            _webSocketEvents.emit("Resolved conflict for Stop #${conflict.stopId} using REMOTE dispatch data.")
            _activeConflict.value = null
        }
    }

    fun resolveConflictWithMerge() {
        viewModelScope.launch {
            val conflict = _activeConflict.value ?: return@launch
            val merged = conflict.localStop.copy(
                notes = conflict.remoteStop.notes,
                eta = conflict.remoteStop.eta
            )
            repository.updateStop(merged)
            _webSocketEvents.emit("Resolved conflict for Stop #${conflict.stopId} by MERGING changes.")
            _activeConflict.value = null
        }
    }

    // --- Haversine Distance Calculation (Meters) ---
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0 // Earth's radius in meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2.0) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }
}
