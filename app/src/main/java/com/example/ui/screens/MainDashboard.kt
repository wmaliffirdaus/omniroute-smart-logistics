package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.util.Base64
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.database.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.DeliveryViewModel
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MainDashboard(viewModel: DeliveryViewModel) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    if (currentUser == null) {
        AuthenticationScreen(viewModel = viewModel)
        return
    }

    val stops by viewModel.stops.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()
    val activeRole by viewModel.activeRole.collectAsStateWithLifecycle()
    val geofenceAlert by viewModel.geofenceAlert.collectAsStateWithLifecycle()
    val isGpsSimulating by viewModel.isGpsSimulating.collectAsStateWithLifecycle()
    val activeConflict by viewModel.activeConflict.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf("MAP") } // MAP, STOPS, ANALYTICS, ASSISTANT, MANAGER
    var selectedStopForDetails by remember { mutableStateOf<DeliveryStop?>(null) }
    var activeProofOfDeliveryStopId by remember { mutableStateOf<Int?>(null) }

    val context = LocalContext.current

    // Real-time WebSocket notifications state
    var latestWebSocketEvent by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        viewModel.webSocketEvents.collect { event ->
            latestWebSocketEvent = event
            kotlinx.coroutines.delay(6000)
            if (latestWebSocketEvent == event) {
                latestWebSocketEvent = null
            }
        }
    }

    // Observe geofence alert and trigger a local app toast for notification simulation
    LaunchedEffect(geofenceAlert) {
        geofenceAlert?.let {
            Toast.makeText(context, "🚨 GEOFENCE ALERT: $it", Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        topBar = {
            HeaderBar(
                isOnline = isOnline,
                syncStatus = syncStatus,
                activeRole = activeRole,
                currentUser = currentUser,
                onToggleOnline = { viewModel.toggleOnlineStatus() },
                onRoleChange = { viewModel.setRole(it) },
                onReset = { 
                    viewModel.resetAllStops()
                    Toast.makeText(context, "Database and Simulation reset!", Toast.LENGTH_SHORT).show()
                },
                onLogout = {
                    viewModel.logout()
                    Toast.makeText(context, "Logged out successfully.", Toast.LENGTH_SHORT).show()
                }
            )
        },
        bottomBar = {
            BottomNavBar(
                activeTab = activeTab,
                onTabSelect = { activeTab = it },
                activeRole = activeRole
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Connection/Sync Warning Status Banner if offline or syncing
                ConnectionStatusBar(isOnline = isOnline, syncStatus = syncStatus)

                // Main screen routing based on active tab
                when (activeTab) {
                    "MAP" -> MapScreen(
                        viewModel = viewModel,
                        stops = stops,
                        isGpsSimulating = isGpsSimulating,
                        onStopSelect = { selectedStopForDetails = it },
                        onCompletePod = { activeProofOfDeliveryStopId = it }
                    )
                    "STOPS" -> StopsScreen(
                        stops = stops,
                        onStopSelect = { selectedStopForDetails = it },
                        onOptimizeRoute = { viewModel.optimizeStopsSequence() },
                        onCompletePod = { activeProofOfDeliveryStopId = it }
                    )
                    "ANALYTICS" -> AnalyticsScreen(stops = stops)
                    "ASSISTANT" -> AssistantScreen(viewModel = viewModel)
                    "MANAGER" -> ManagerScreen(viewModel = viewModel, stops = stops)
                }
            }

            // Real-time WebSocket floating notification ticker
            latestWebSocketEvent?.let { webSocketMsg ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .align(Alignment.BottomCenter)
                        .border(1.dp, TechBlue, RoundedCornerShape(16.dp))
                        .testTag("websocket_live_toast"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary),
                    elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(TechBlue.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "WebSocket Live Event",
                                tint = TechBlue,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "REAL-TIME COLLABORATION",
                                color = TechBlue,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = webSocketMsg,
                                color = HighDensityText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = { latestWebSocketEvent = null },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Dismiss",
                                tint = LogiGray,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            // GEOFENCE floating sticky notification
            geofenceAlert?.let { alertMsg ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .align(Alignment.TopCenter)
                        .testTag("geofence_notification"),
                    colors = CardDefaults.cardColors(containerColor = LogiAmber),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Geofence Triggered",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Geofence Detection",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = alertMsg,
                                color = Color.White,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(onClick = { viewModel.dismissGeofenceAlert() }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Dismiss", tint = Color.White)
                        }
                    }
                }
            }

            // Stop Detail Dialog
            selectedStopForDetails?.let { stop ->
                StopDetailsDialog(
                    stop = stop,
                    onDismiss = { selectedStopForDetails = null },
                    onCompletePod = {
                        activeProofOfDeliveryStopId = it
                        selectedStopForDetails = null
                    }
                )
            }

            // Proof of Delivery Completion Dialog Flow
            activeProofOfDeliveryStopId?.let { stopId ->
                val associatedStop = stops.firstOrNull { it.id == stopId }
                if (associatedStop != null) {
                    ProofOfDeliveryFlowDialog(
                        stop = associatedStop,
                        onDismiss = { activeProofOfDeliveryStopId = null },
                        onPodComplete = { sig, pic, bar, nts ->
                            viewModel.completeActiveStop(stopId, sig, pic, bar, nts)
                            activeProofOfDeliveryStopId = null
                            Toast.makeText(context, "Proof of Delivery saved successfully!", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }

            // Database Sync Conflict Resolution Dialog Side-By-Side Comparison
            activeConflict?.let { conflict ->
                AlertDialog(
                    onDismissRequest = { /* Force resolution to avoid bad sync state */ },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Conflict",
                                tint = LogiRed,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Dispatch Conflict Detected",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = HighDensityText
                            )
                        }
                    },
                    text = {
                        Column {
                            Text(
                                text = "Dispatch updated details for Stop #${conflict.stopId} (${conflict.localStop.address}) while you were servicing it offline. Choose which version to persist:",
                                fontSize = 13.sp,
                                color = LogiGray,
                                lineHeight = 18.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, HighDensityBorder, RoundedCornerShape(16.dp)),
                                colors = CardDefaults.cardColors(containerColor = HighDensitySecondary),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "YOUR LOCAL COURIER CHANGES",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = HighDensityPrimary,
                                        letterSpacing = 0.5.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = "Notes: ${conflict.localStop.notes.ifBlank { "None" }}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = HighDensityText)
                                    Text(text = "ETA: ${conflict.localStop.eta}", fontSize = 12.sp, color = LogiGray)
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    HorizontalDivider(color = HighDensityBorder)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    Text(
                                        text = "REMOTE DISPATCH UPDATE",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = AccentOrange,
                                        letterSpacing = 0.5.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = "Notes: ${conflict.remoteStop.notes}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = HighDensityText)
                                    Text(text = "ETA: ${conflict.remoteStop.eta}", fontSize = 12.sp, color = LogiGray)
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.resolveConflictWithLocal() },
                                modifier = Modifier.fillMaxWidth().testTag("resolve_local_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = HighDensityPrimary),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Keep My Local Changes", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Button(
                                onClick = { viewModel.resolveConflictWithRemote() },
                                modifier = Modifier.fillMaxWidth().testTag("resolve_remote_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = AccentOrange),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Accept Remote Dispatch Update", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            OutlinedButton(
                                onClick = { viewModel.resolveConflictWithMerge() },
                                modifier = Modifier.fillMaxWidth().testTag("resolve_merge_btn"),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, HighDensityBorder)
                            ) {
                                Text("Merge (Accept Remote Notes + Local Signature)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = HighDensityText)
                            }
                        }
                    },
                    shape = RoundedCornerShape(24.dp)
                )
            }
        }
    }
}

// --- TOP APPLICATION HEADER ---
@Composable
fun HeaderBar(
    isOnline: Boolean,
    syncStatus: String,
    activeRole: String,
    currentUser: UserAccount?,
    onToggleOnline: () -> Unit,
    onRoleChange: (String) -> Unit,
    onReset: () -> Unit,
    onLogout: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .border(1.dp, HighDensityBorder, RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "OmniRoute",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSecondary
                )
                Text(
                    text = currentUser?.fullName ?: "Enterprise SaaS Logistics",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.7f)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Database Reset Action (for presentation convenience)
                IconButton(onClick = onReset, modifier = Modifier.testTag("reset_button")) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset Simulation",
                        tint = MaterialTheme.colorScheme.onSecondary
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Online / Offline state switcher
                IconButton(
                    onClick = onToggleOnline,
                    modifier = Modifier.testTag("offline_toggle_btn")
                ) {
                    Icon(
                        imageVector = if (isOnline) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = "Toggle Network Status",
                        tint = if (isOnline) LogiGreen else LogiRed
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Role Toggler (Driver / Fleet Manager)
                FilledTonalButton(
                    onClick = {
                        val newRole = if (activeRole == "DRIVER") "MANAGER" else "DRIVER"
                        onRoleChange(newRole)
                    },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (activeRole == "MANAGER") AccentOrange else HighDensityPrimary
                    ),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("role_toggle_btn")
                ) {
                    Icon(
                        imageVector = if (activeRole == "MANAGER") Icons.Default.Person else Icons.Default.Build,
                        contentDescription = "Role Mode",
                        modifier = Modifier.size(14.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (activeRole == "MANAGER") "Manager" else "Driver",
                        fontSize = 11.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Avatar initials matching the HTML
                val initials = currentUser?.fullName?.split(" ")?.map { it.firstOrNull() ?: "" }?.joinToString("")?.take(2)?.uppercase() ?: "JD"
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(HighDensityPrimary, CircleShape)
                        .clickable { onLogout() }
                        .testTag("logout_avatar_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// --- SYNC ENGINE STATUS HEADER BANNER ---
@Composable
fun ConnectionStatusBar(isOnline: Boolean, syncStatus: String) {
    val bgColor = if (!isOnline) LogiRed.copy(alpha = 0.9f) else if (syncStatus.startsWith("Starting") || syncStatus.startsWith("Syncing")) TechBlue else Color.Transparent
    if (!isOnline || syncStatus.startsWith("Starting") || syncStatus.startsWith("Syncing")) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(bgColor)
                .padding(vertical = 4.dp, horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (!isOnline) Icons.Default.Warning else Icons.Default.Refresh,
                    contentDescription = "Status",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (!isOnline) "OFFLINE MODE — Completing deliveries saves safely to offline sync queue" else syncStatus,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// --- SYSTEM NAVIGATION BAR ---
@Composable
fun BottomNavBar(activeTab: String, onTabSelect: (String) -> Unit, activeRole: String) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.secondary,
        tonalElevation = 8.dp,
        windowInsets = WindowInsets.navigationBars
    ) {
        val navItemColors = NavigationBarItemDefaults.colors(
            selectedIconColor = HighDensitySyncText,
            indicatorColor = HighDensitySyncBg,
            unselectedIconColor = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.6f),
            unselectedTextColor = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.6f),
            selectedTextColor = HighDensityText
        )

        NavigationBarItem(
            selected = activeTab == "MAP",
            onClick = { onTabSelect("MAP") },
            label = { Text("Route Map") },
            icon = { Icon(imageVector = Icons.Default.LocationOn, contentDescription = "Route Map") },
            colors = navItemColors
        )
        NavigationBarItem(
            selected = activeTab == "STOPS",
            onClick = { onTabSelect("STOPS") },
            label = { Text("Stops") },
            icon = { Icon(imageVector = Icons.Default.List, contentDescription = "Stops") },
            colors = navItemColors
        )
        NavigationBarItem(
            selected = activeTab == "ANALYTICS",
            onClick = { onTabSelect("ANALYTICS") },
            label = { Text("Analytics") },
            icon = { Icon(imageVector = Icons.Default.Star, contentDescription = "Analytics") },
            colors = navItemColors
        )
        NavigationBarItem(
            selected = activeTab == "ASSISTANT",
            onClick = { onTabSelect("ASSISTANT") },
            label = { Text("AI Copilot") },
            icon = { Icon(imageVector = Icons.Default.Face, contentDescription = "AI Assistant") },
            colors = navItemColors
        )
        if (activeRole == "MANAGER") {
            NavigationBarItem(
                selected = activeTab == "MANAGER",
                onClick = { onTabSelect("MANAGER") },
                label = { Text("Manager") },
                icon = { Icon(imageVector = Icons.Default.Person, contentDescription = "Manager") },
                colors = navItemColors
            )
        }
    }
}

// --- TAB 1: NAVIGATION & INTERACTIVE MAP SCREEN ---
@Composable
fun MapScreen(
    viewModel: DeliveryViewModel,
    stops: List<DeliveryStop>,
    isGpsSimulating: Boolean,
    onStopSelect: (DeliveryStop) -> Unit,
    onCompletePod: (Int) -> Unit
) {
    val gpsLat by viewModel.gpsLat.collectAsStateWithLifecycle()
    val gpsLng by viewModel.gpsLng.collectAsStateWithLifecycle()
    val activeStopId by viewModel.activeStopId.collectAsStateWithLifecycle()
    val pins by viewModel.pins.collectAsStateWithLifecycle()

    val activeStop = stops.firstOrNull { it.id == activeStopId }
    var showAddPinDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Upper Map Canvas Card matching High Density HTML
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, Color(0xFFC4C7C5), RoundedCornerShape(24.dp))
                .background(Color(0xFFDDE3EA))
        ) {
            // Interactive custom vector map drawing
            InteractiveVectorMapCanvas(
                stops = stops,
                pins = pins,
                gpsLat = gpsLat,
                gpsLng = gpsLng,
                activeStopId = activeStopId,
                onStopSelect = onStopSelect
            )

            // Top overlay GPS telemetry HUD
            Surface(
                modifier = Modifier
                    .padding(12.dp)
                    .align(Alignment.TopStart),
                color = SlateSurface.copy(alpha = 0.85f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, OverlayWhite)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text("TELEMETRY HUD", color = AccentOrange, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Speed: ${if (isGpsSimulating) "24 mph" else "0 mph"}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("Odometer: ${"142.6 mi"}", color = Color.White, fontSize = 11.sp)
                    Text("GPS: ${String.format("%.4f", gpsLat)}, ${String.format("%.4f", gpsLng)}", color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
                }
            }

            // Map HUD Overlays Right Column
            Column(
                modifier = Modifier
                    .padding(12.dp)
                    .align(Alignment.TopEnd),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.End
            ) {
                // Top overlay simulation play button
                IconButton(
                    onClick = { viewModel.toggleGpsSimulation() },
                    modifier = Modifier
                        .size(48.dp)
                        .background(if (isGpsSimulating) LogiGreen else TechBlue, CircleShape)
                        .testTag("gps_simulation_play_btn")
                ) {
                    Icon(
                        imageVector = if (isGpsSimulating) Icons.Default.Clear else Icons.Default.PlayArrow,
                        contentDescription = "Simulate",
                        tint = Color.White
                    )
                }

                // Add Collaborative Map Pin Button
                IconButton(
                    onClick = { showAddPinDialog = true },
                    modifier = Modifier
                        .size(48.dp)
                        .background(AccentOrange, CircleShape)
                        .testTag("add_collaborative_pin_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Shared Pin",
                        tint = Color.White
                    )
                }
            }

            // Real-time overlay: "Live Tracking Active" badge matching HTML
            Surface(
                modifier = Modifier
                    .padding(12.dp)
                    .align(Alignment.BottomStart),
                color = Color.White.copy(alpha = 0.9f),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color.White),
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color(0xFF10B981), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "LIVE TRACKING ACTIVE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF444746),
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }

        // Bottom HUD Panel with current target delivery
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (activeStop != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(TechBlue, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "${activeStop.sequence}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (activeStop.status == "ARRIVED") "ARRIVED AT ZONE" else "ACTIVE EN ROUTE",
                                color = if (activeStop.status == "ARRIVED") LogiGreen else TechBlue,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp
                            )
                        }
                        
                        Text(
                            text = "ETA: ${activeStop.eta}",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = activeStop.address,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Person, contentDescription = null, modifier = Modifier.size(14.dp), tint = LogiGray)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "${activeStop.recipientName} (${activeStop.phoneNumber})", fontSize = 12.sp, color = LogiGray)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onStopSelect(activeStop) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Instructions")
                        }
                        Button(
                            onClick = { onCompletePod(activeStop.id) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("secure_pod_button"),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = LogiGreen)
                        ) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Complete POD", color = Color.White)
                        }
                    }
                } else {
                    // Empty navigation state
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = LogiGreen, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("All deliveries completed!", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("No active en-route stop. Reoptimize or assign new stops.", fontSize = 12.sp, color = LogiGray, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }

    // Collaborative Pin Submission Modal
    if (showAddPinDialog) {
        AddPinDialog(
            onDismiss = { showAddPinDialog = false },
            onPinSubmit = { title, desc, colorHex ->
                viewModel.addCollaborativePinFromUser(title, desc, gpsLat, gpsLng, colorHex)
                showAddPinDialog = false
            }
        )
    }
}

@Composable
fun AddPinDialog(
    onDismiss: () -> Unit,
    onPinSubmit: (title: String, description: String, colorHex: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf("#EF4444") }

    val pinColors = listOf(
        Pair("#EF4444", "Red / Delay"),
        Pair("#10B981", "Green / Zone"),
        Pair("#3B82F6", "Blue / Info"),
        Pair("#F59E0B", "Yellow / Caution")
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Collaborative Map Pin", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Pin coordinates will be annotated instantly at your current GPS location and synchronized in real-time with all logged-in fleet drivers.",
                    fontSize = 12.sp,
                    color = LogiGray,
                    lineHeight = 16.sp
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Pin Label (e.g. Broken Gate)") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("pin_title_input"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = HighDensityPrimary,
                        unfocusedBorderColor = HighDensityBorder
                    )
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Additional Information") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("pin_desc_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = HighDensityPrimary,
                        unfocusedBorderColor = HighDensityBorder
                    )
                )

                Text("Visual Pin Category Color", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = HighDensityText)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    pinColors.forEach { (hex, name) ->
                        val parsedColor = Color(android.graphics.Color.parseColor(hex))
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(parsedColor, CircleShape)
                                .border(
                                    border = BorderStroke(
                                        width = if (selectedColor == hex) 3.dp else 1.dp,
                                        color = if (selectedColor == hex) HighDensityText else Color.Transparent
                                    ),
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = hex }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onPinSubmit(title, description, selectedColor)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = HighDensityPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("submit_pin_btn")
            ) {
                Text("Broadcast Pin")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

// --- INTERACTIVE VECTOR MAP DESIGN CANVAS ---
@Composable
fun InteractiveVectorMapCanvas(
    stops: List<DeliveryStop>,
    pins: List<CollaborativePin>,
    gpsLat: Double,
    gpsLng: Double,
    activeStopId: Int?,
    onStopSelect: (DeliveryStop) -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition()
    val pulseAnim by infiniteTransition.animateFloat(
        initialValue = 4f,
        targetValue = 24f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(stops) {
                detectDragGestures(
                    onDragStart = { },
                    onDrag = { _, _ -> }
                )
            }
    ) {
        val width = size.width
        val height = size.height

        val minLat = 37.740
        val maxLat = 37.810
        val minLng = -122.460
        val maxLng = -122.390

        fun toX(lng: Double): Float {
            return ((lng - minLng) / (maxLng - minLng) * width).toFloat()
        }

        fun toY(lat: Double): Float {
            return (height - (lat - minLat) / (maxLat - minLat) * height).toFloat()
        }

        // 1. Draw Grid Roads
        val roadPaint = Paint().apply {
            color = android.graphics.Color.WHITE
            strokeWidth = 14f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }

        // Main Avenues
        drawContext.canvas.nativeCanvas.drawLine(toX(-122.450), 0f, toX(-122.450), height, roadPaint)
        drawContext.canvas.nativeCanvas.drawLine(toX(-122.420), 0f, toX(-122.420), height, roadPaint)
        drawContext.canvas.nativeCanvas.drawLine(toX(-122.400), 0f, toX(-122.400), height, roadPaint)
        // Intersecting Streets
        drawContext.canvas.nativeCanvas.drawLine(0f, toY(37.795), width, toY(37.795), roadPaint)
        drawContext.canvas.nativeCanvas.drawLine(0f, toY(37.775), width, toY(37.775), roadPaint)
        drawContext.canvas.nativeCanvas.drawLine(0f, toY(37.755), width, toY(37.755), roadPaint)

        // 2. Draw Dash route connection sequence paths
        val pendingStops = stops.filter { it.status != "COMPLETED" && it.status != "FAILED" }.sortedBy { it.sequence }
        if (pendingStops.size >= 2) {
            val path = Path()
            path.moveTo(toX(gpsLng), toY(gpsLat))
            for (stop in pendingStops) {
                path.lineTo(toX(stop.lng), toY(stop.lat))
            }
            drawPath(
                path = path,
                color = TechBlue.copy(alpha = 0.8f),
                style = Stroke(
                    width = 6f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                )
            )
        }

        // 3. Draw Geofences, Stop Pins & Checkmarks
        for (stop in stops) {
            val stopX = toX(stop.lng)
            val stopY = toY(stop.lat)

            val color = when (stop.status) {
                "COMPLETED" -> LogiGreen
                "ARRIVED" -> LogiGreen
                "EN_ROUTE" -> TechBlue
                else -> LogiAmber
            }

            // Draw Geofence boundaries (dashed light gray rings)
            drawCircle(
                color = color.copy(alpha = 0.2f),
                radius = 70f,
                center = Offset(stopX, stopY),
                style = Stroke(width = 3f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f))
            )

            // Draw Marker Base Shadow
            drawCircle(
                color = Color.Black.copy(alpha = 0.4f),
                radius = 16f,
                center = Offset(stopX, stopY + 4f)
            )

            // Draw Pin
            drawCircle(
                color = color,
                radius = 14f,
                center = Offset(stopX, stopY)
            )

            // Inner checkmark or sequence text
            if (stop.status == "COMPLETED") {
                val tickPath = Path().apply {
                    moveTo(stopX - 6f, stopY)
                    lineTo(stopX - 2f, stopY + 4f)
                    lineTo(stopX + 6f, stopY - 4f)
                }
                drawPath(path = tickPath, color = Color.White, style = Stroke(width = 4f))
            } else {
                val textPaint = Paint().apply {
                    setColor(android.graphics.Color.WHITE)
                    textSize = 24f
                    textAlign = Paint.Align.CENTER
                    setTypeface(android.graphics.Typeface.DEFAULT_BOLD)
                }
                drawContext.canvas.nativeCanvas.drawText(
                    "${stop.sequence}",
                    stopX,
                    stopY + 8f,
                    textPaint
                )
            }
        }

        // 4. Draw Collaborative Team Pins (WebSocket Pinned annotations)
        for (pin in pins) {
            val pinX = toX(pin.lng)
            val pinY = toY(pin.lat)
            val parsedColor = try {
                Color(android.graphics.Color.parseColor(pin.colorHex))
            } catch (e: Exception) {
                Color(0xFFEF4444)
            }

            // Shadow
            drawCircle(
                color = Color.Black.copy(alpha = 0.3f),
                radius = 13f,
                center = Offset(pinX, pinY + 3f)
            )

            // Pin Core Distinctive Design
            drawCircle(
                color = parsedColor,
                radius = 11f,
                center = Offset(pinX, pinY)
            )
            drawCircle(
                color = Color.White,
                radius = 5f,
                center = Offset(pinX, pinY)
            )
        }

        // 5. Draw Driver GPS Location Cursor with radar pulse ripple
        val driverX = toX(gpsLng)
        val driverY = toY(gpsLat)

        drawCircle(
            color = TechBlue.copy(alpha = (1f - (pulseAnim / 24f)).coerceIn(0f, 1f)),
            radius = pulseAnim * 2f,
            center = Offset(driverX, driverY)
        )

        drawCircle(
            color = TechBlue,
            radius = 12f,
            center = Offset(driverX, driverY)
        )
        drawCircle(
            color = Color.White,
            radius = 6f,
            center = Offset(driverX, driverY)
        )
    }
}

// --- TAB 2: STOPS LIST & QUEUE SCREEN ---
@Composable
fun StopsScreen(
    stops: List<DeliveryStop>,
    onStopSelect: (DeliveryStop) -> Unit,
    onOptimizeRoute: () -> Unit,
    onCompletePod: (Int) -> Unit
) {
    val pendingStops = stops.filter { it.status != "COMPLETED" && it.status != "FAILED" }
    val completedStops = stops.filter { it.status == "COMPLETED" || it.status == "FAILED" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Delivery Schedule",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "${pendingStops.size} stops remaining today",
                    fontSize = 12.sp,
                    color = LogiGray
                )
            }

            // TSP Route Optimizer trigger button
            Button(
                onClick = onOptimizeRoute,
                colors = ButtonDefaults.buttonColors(containerColor = TechBlue),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("optimize_route_btn")
            ) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Optimize Sequence", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (pendingStops.isNotEmpty()) {
                item {
                    SectionHeader(title = "UPCOMING DELIVERIES")
                }
                items(pendingStops) { stop ->
                    StopCard(
                        stop = stop,
                        onStopSelect = onStopSelect,
                        onCompletePod = onCompletePod
                    )
                }
            }

            if (completedStops.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    SectionHeader(title = "COMPLETED HISTORY")
                }
                items(completedStops) { stop ->
                    CompletedStopCard(stop = stop)
                }
            }

            if (stops.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No delivery stops scheduled. Switch to Manager Role to add stops.",
                            textAlign = TextAlign.Center,
                            color = LogiGray,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        color = AccentOrange,
        letterSpacing = 1.2.sp,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}

@Composable
fun StopCard(
    stop: DeliveryStop,
    onStopSelect: (DeliveryStop) -> Unit,
    onCompletePod: (Int) -> Unit
) {
    val statusColor = when (stop.status) {
        "ARRIVED" -> LogiGreen
        "EN_ROUTE" -> TechBlue
        else -> LogiAmber
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onStopSelect(stop) }
            .border(1.dp, HighDensityBorder, RoundedCornerShape(20.dp))
            .testTag("stop_card_${stop.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Sequence Ring
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(statusColor.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${stop.sequence}",
                    color = statusColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stop.status,
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "ETA: ${stop.eta}",
                        color = LogiGray,
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = stop.address,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Recipient: ${stop.recipientName}",
                    fontSize = 12.sp,
                    color = LogiGray
                )

                if (stop.status == "ARRIVED" || stop.status == "EN_ROUTE") {
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { onCompletePod(stop.id) },
                        colors = ButtonDefaults.buttonColors(containerColor = LogiGreen),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Complete Proof of Delivery", fontSize = 11.sp, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun CompletedStopCard(stop: DeliveryStop) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, HighDensityBorder.copy(alpha = 0.5f), RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Completed",
                tint = LogiGreen,
                modifier = Modifier.size(28.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stop.address,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Text(
                    text = "Delivered to ${stop.recipientName}",
                    fontSize = 11.sp,
                    color = LogiGray
                )
            }

            if (stop.isOfflineLogged) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = LogiAmber.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "QUEUED",
                        color = LogiAmber,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

// --- TAB 3: PERFORMANCE ANALYTICS DASHBOARD SCREEN ---
@Composable
fun AnalyticsScreen(stops: List<DeliveryStop>) {
    val totalCount = stops.size
    val completedCount = stops.count { it.status == "COMPLETED" }
    val pendingCount = stops.count { it.status == "PENDING" || it.status == "EN_ROUTE" || it.status == "ARRIVED" }
    
    val completionRate = if (totalCount > 0) (completedCount.toFloat() / totalCount.toFloat()) else 0.0f

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Performance Metrics",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Real-time fleet operations & driver performance",
                    fontSize = 12.sp,
                    color = LogiGray
                )
            }
        }

        // 1. KPI Cards Row Grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                KpiCard(
                    title = "Deliveries",
                    value = "$completedCount/$totalCount",
                    sub = "Completion rate: ${(completionRate * 100).toInt()}%",
                    color = TechBlue,
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    title = "Productivity Score",
                    value = "94%",
                    sub = "Grade: Senior A+",
                    color = LogiGreen,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                KpiCard(
                    title = "Avg Delivery",
                    value = "12.4 min",
                    sub = "Saves 3 mins/stop",
                    color = AccentOrange,
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    title = "Fuel Saved",
                    value = "1.8 gal",
                    sub = "4.2 lbs CO2 offsets",
                    color = LogiGreen,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 2. Bar Chart weekly volume
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Weekly Delivery Trends",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    WeeklyTrendsBarChart()
                }
            }
        }

        // 3. Line Chart odometer / distance savings
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Daily Distance Saved (Haversine Miles)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OdometerLineChart()
                }
            }
        }
    }
}

@Composable
fun KpiCard(title: String, value: String, sub: String, color: Color, modifier: Modifier) {
    val (cardBg, cardBorder, cardTextColor) = when {
        title.contains("Sync", ignoreCase = true) || title.contains("Fuel", ignoreCase = true) -> {
            Triple(HighDensitySyncBg, HighDensitySyncBorder, HighDensitySyncText)
        }
        title.contains("Rate", ignoreCase = true) || title.contains("Completed", ignoreCase = true) || title.contains("Time", ignoreCase = true) || title.contains("Productivity", ignoreCase = true) -> {
            Triple(HighDensityApiBg, HighDensityApiBorder, HighDensityApiText)
        }
        else -> {
            Triple(Color.White, HighDensityBorder, HighDensityText)
        }
    }

    Card(
        modifier = modifier
            .border(1.dp, cardBorder, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title.uppercase(),
                fontSize = 10.sp,
                color = cardTextColor.copy(alpha = 0.8f),
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = cardTextColor
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = sub,
                fontSize = 10.sp,
                color = cardTextColor.copy(alpha = 0.6f),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// --- CUSTOM CANVAS BAR CHART ---
@Composable
fun WeeklyTrendsBarChart() {
    val weeklyVolume = listOf(14, 18, 22, 19, 25, 30, 15) // Mon-Sun
    val days = listOf("M", "T", "W", "T", "F", "S", "S")

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
    ) {
        val width = size.width
        val height = size.height
        val maxVolume = 35f

        val barCount = weeklyVolume.size
        val barWidth = 32.dp.toPx()
        val spacing = (width - (barWidth * barCount)) / (barCount + 1)

        for (i in weeklyVolume.indices) {
            val volume = weeklyVolume[i]
            val barHeight = (volume / maxVolume) * (height - 30.dp.toPx())
            val x = spacing + i * (barWidth + spacing)
            val y = height - 20.dp.toPx() - barHeight

            // Draw Bar Background slot
            drawRoundRect(
                color = Color.LightGray.copy(alpha = 0.1f),
                topLeft = Offset(x, 10.dp.toPx()),
                size = androidx.compose.ui.geometry.Size(barWidth, height - 30.dp.toPx()),
                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
            )

            // Draw Filled Volume Bar
            drawRoundRect(
                brush = Brush.verticalGradient(listOf(TechBlue, TechBlue.copy(alpha = 0.6f))),
                topLeft = Offset(x, y),
                size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
            )

            // Draw Day Text using native paint
            val paint = Paint().apply {
                color = android.graphics.Color.GRAY
                textSize = 24f
                textAlign = Paint.Align.CENTER
            }
            drawContext.canvas.nativeCanvas.drawText(
                days[i],
                x + barWidth / 2,
                height - 4.dp.toPx(),
                paint
            )
        }
    }
}

// --- CUSTOM CANVAS LINE CHART ---
@Composable
fun OdometerLineChart() {
    val distanceValues = listOf(12f, 15f, 9f, 22f, 28f, 18f, 21f)

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
    ) {
        val width = size.width
        val height = size.height
        val maxVal = 35f

        val pointCount = distanceValues.size
        val stepX = width / (pointCount - 1)

        val path = Path()
        val fillPath = Path()

        for (i in distanceValues.indices) {
            val value = distanceValues[i]
            val x = i * stepX
            val y = height - (value / maxVal) * height

            if (i == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, height)
                fillPath.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }

            if (i == pointCount - 1) {
                fillPath.lineTo(x, height)
                fillPath.close()
            }
        }

        // Draw Area Fill
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(TechBlue.copy(alpha = 0.25f), Color.Transparent)
            )
        )

        // Draw Line
        drawPath(
            path = path,
            color = TechBlue,
            style = Stroke(width = 4f, cap = StrokeCap.Round)
        )

        // Draw Dots
        for (i in distanceValues.indices) {
            val value = distanceValues[i]
            val x = i * stepX
            val y = height - (value / maxVal) * height

            drawCircle(
                color = Color.White,
                radius = 6f,
                center = Offset(x, y)
            )
            drawCircle(
                color = TechBlue,
                radius = 4f,
                center = Offset(x, y)
            )
        }
    }
}

// --- TAB 4: SMART DELIVERY CO-PILOT (AI ASSISTANT SCREEN) ---
@Composable
fun AssistantScreen(viewModel: DeliveryViewModel) {
    val geminiResponse by viewModel.geminiResponse.collectAsStateWithLifecycle()
    val isGeminiLoading by viewModel.isGeminiLoading.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text(
                text = "AI Smart Copilot",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Real-time AI logistics optimization suggestions, traffic delays, and safety officer evaluation.",
                fontSize = 12.sp,
                color = LogiGray
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Request AI Recommendations",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = { viewModel.askGeminiAssistant("SCHEDULE") },
                        modifier = Modifier.weight(1f).testTag("ai_schedule_btn"),
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = TechBlue.copy(alpha = 0.15f))
                    ) {
                        Text("Schedules", fontSize = 11.sp, color = TechBlue, fontWeight = FontWeight.Bold)
                    }
                    FilledTonalButton(
                        onClick = { viewModel.askGeminiAssistant("TRAFFIC") },
                        modifier = Modifier.weight(1f).testTag("ai_traffic_btn"),
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = TechBlue.copy(alpha = 0.15f))
                    ) {
                        Text("Traffic Delays", fontSize = 11.sp, color = TechBlue, fontWeight = FontWeight.Bold)
                    }
                    FilledTonalButton(
                        onClick = { viewModel.askGeminiAssistant("RISK") },
                        modifier = Modifier.weight(1f).testTag("ai_risk_btn"),
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = TechBlue.copy(alpha = 0.15f))
                    ) {
                        Text("Risk Assessment", fontSize = 11.sp, color = TechBlue, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Output Window
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.0f, fill = false),
            colors = CardDefaults.cardColors(containerColor = SlateSurface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Face, contentDescription = null, tint = AccentOrange)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "OmniRoute AI Engine",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Divider(color = OverlayWhite)

                Spacer(modifier = Modifier.height(12.dp))

                if (isGeminiLoading) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = AccentOrange)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Analyzing delivery vectors...", color = Color.White, fontSize = 12.sp)
                    }
                } else {
                    if (geminiResponse.isEmpty()) {
                        Text(
                            text = "Please tap one of the recommendation tools above to generate smart scheduling insights based on your actual routes.",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                    } else {
                        Text(
                            text = geminiResponse,
                            color = Color.White,
                            fontSize = 13.sp,
                            lineHeight = 20.sp,
                            modifier = Modifier.testTag("ai_response_box")
                        )
                    }
                }
            }
        }
    }
}

// --- TAB 5: FLEET MANAGER CONSOLE SCREEN ---
@Composable
fun ManagerScreen(viewModel: DeliveryViewModel, stops: List<DeliveryStop>) {
    var addressInput by remember { mutableStateOf("") }
    var recipientInput by remember { mutableStateOf("") }
    var phoneInput by remember { mutableStateOf("") }
    var instructionsInput by remember { mutableStateOf("") }

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text(
                text = "Fleet Manager Console",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Dispatch active vehicles, assign stops, and track real-time fleet utilization.",
                fontSize = 12.sp,
                color = LogiGray
            )
        }

        // Fleet Utilization Summary
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Fleet Utilization", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("ACTIVE COURIERS", color = LogiGray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text("3 Drivers Online", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TechBlue)
                    }
                    Column {
                        Text("FLEET ASSIGNMENTS", color = LogiGray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text("${stops.size} stops allocated", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }

        // Assign New Stop Input Form
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Assign New Delivery Task",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                OutlinedTextField(
                    value = addressInput,
                    onValueChange = { addressInput = it },
                    label = { Text("Delivery Address") },
                    modifier = Modifier.fillMaxWidth().testTag("manager_address_input")
                )

                OutlinedTextField(
                    value = recipientInput,
                    onValueChange = { recipientInput = it },
                    label = { Text("Recipient Full Name") },
                    modifier = Modifier.fillMaxWidth().testTag("manager_recipient_input")
                )

                OutlinedTextField(
                    value = phoneInput,
                    onValueChange = { phoneInput = it },
                    label = { Text("Recipient Phone") },
                    modifier = Modifier.fillMaxWidth().testTag("manager_phone_input")
                )

                OutlinedTextField(
                    value = instructionsInput,
                    onValueChange = { instructionsInput = it },
                    label = { Text("Special Dispatch Instructions") },
                    modifier = Modifier.fillMaxWidth().testTag("manager_instructions_input")
                )

                Button(
                    onClick = {
                        if (addressInput.isNotEmpty() && recipientInput.isNotEmpty()) {
                            viewModel.assignNewStopFromManager(
                                address = addressInput,
                                recipient = recipientInput,
                                phone = phoneInput,
                                notes = instructionsInput
                            )
                            Toast.makeText(context, "Task dispatched to active courier database!", Toast.LENGTH_SHORT).show()
                            addressInput = ""
                            recipientInput = ""
                            phoneInput = ""
                            instructionsInput = ""
                        } else {
                            Toast.makeText(context, "Please fill address and recipient fields.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("manager_assign_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = TechBlue),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Dispatch to Courier DB", color = Color.White)
                }
            }
        }
    }
}

// --- STOP DETAILS MODAL DIALOG ---
@Composable
fun StopDetailsDialog(
    stop: DeliveryStop,
    onDismiss: () -> Unit,
    onCompletePod: (Int) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Stop Information #${stop.sequence}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = TechBlue
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Close")
                    }
                }

                Divider()

                Spacer(modifier = Modifier.height(12.dp))

                Text(text = "DELIVERY ADDRESS", fontSize = 10.sp, color = LogiGray, fontWeight = FontWeight.Bold)
                Text(text = stop.address, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(bottom = 12.dp))

                Text(text = "RECIPIENT", fontSize = 10.sp, color = LogiGray, fontWeight = FontWeight.Bold)
                Text(text = stop.recipientName, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(bottom = 12.dp))

                Text(text = "CONTACT", fontSize = 10.sp, color = LogiGray, fontWeight = FontWeight.Bold)
                Text(text = stop.phoneNumber, fontSize = 14.sp, modifier = Modifier.padding(bottom = 12.dp))

                Text(text = "SPECIAL ROUTING NOTES", fontSize = 10.sp, color = LogiGray, fontWeight = FontWeight.Bold)
                Text(text = stop.notes.ifEmpty { "None" }, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))

                Spacer(modifier = Modifier.height(20.dp))

                if (stop.status != "COMPLETED" && stop.status != "FAILED") {
                    Button(
                        onClick = { onCompletePod(stop.id) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = LogiGreen),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Complete Proof of Delivery", color = Color.White)
                    }
                }
            }
        }
    }
}

// --- PROOF OF DELIVERY FLOW MODAL STEPPER ---
@Composable
fun ProofOfDeliveryFlowDialog(
    stop: DeliveryStop,
    onDismiss: () -> Unit,
    onPodComplete: (String?, String?, String?, String) -> Unit
) {
    var activeStep by remember { mutableStateOf(1) } // 1: Barcode Scan, 2: Signature Drawing, 3: Photo Capture, 4: Complete/Confirm

    var barcodeResult by remember { mutableStateOf("") }
    val drawnSignaturePoints = remember { mutableStateListOf<Offset>() }
    var photoCapturedBase64 by remember { mutableStateOf<String?>(null) }
    var notesInput by remember { mutableStateOf("") }

    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                // Stepper Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Secure POD: Step $activeStep of 4",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = TechBlue
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Cancel")
                    }
                }

                // Progress Step bar indicator
                LinearProgressIndicator(
                    progress = activeStep.toFloat() / 4.0f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    color = TechBlue,
                    trackColor = TechBlue.copy(alpha = 0.15f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Steps Views
                when (activeStep) {
                    1 -> { // Step 1: Simulated Barcode Scanner Camera view
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "Align Package Barcode to Scan",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // Visual Camera active overlay simulation
                            Box(
                                modifier = Modifier
                                    .size(width = 240.dp, height = 140.dp)
                                    .background(Color.Black, RoundedCornerShape(8.dp))
                                    .border(2.dp, TechBlue, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                // Animated Scan Laser line
                                val laserTransition = rememberInfiniteTransition()
                                val yOffset by laserTransition.animateFloat(
                                    initialValue = 0f,
                                    targetValue = 110f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(1500, easing = LinearEasing),
                                        repeatMode = RepeatMode.Reverse
                                    )
                                )

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(2.dp)
                                        .offset(y = yOffset.dp)
                                        .background(Color.Red)
                                )

                                if (barcodeResult.isNotEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .background(LogiGreen.copy(alpha = 0.85f))
                                            .padding(6.dp)
                                    ) {
                                        Text("SUCCESS: $barcodeResult", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Text("VIEWFINDER CAM SIMULATOR", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Button(
                                onClick = { 
                                    barcodeResult = "OMNI-${(100000..999999).random()}-A"
                                    Toast.makeText(context, "Barcode read successfully!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.fillMaxWidth().testTag("pod_scan_barcode_btn"),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Simulate Laser Scan")
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedButton(
                                onClick = {
                                    if (barcodeResult.isEmpty()) {
                                        barcodeResult = "BYPASSED-BARCODE"
                                    }
                                    activeStep = 2
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Next Step")
                            }
                        }
                    }
                    2 -> { // Step 2: Finger signature drawing Canvas
                        Column {
                            Text(
                                "Capture Customer Signature",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            // Canvas Board
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp)
                                    .background(Color.LightGray.copy(alpha = 0.2f))
                                    .border(1.dp, Color.LightGray)
                                    .pointerInput(Unit) {
                                        detectDragGestures { change, dragAmount ->
                                            change.consume()
                                            val pt = change.position
                                            drawnSignaturePoints.add(pt)
                                        }
                                    }
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    if (drawnSignaturePoints.size >= 2) {
                                        val sigPath = Path()
                                        sigPath.moveTo(drawnSignaturePoints[0].x, drawnSignaturePoints[0].y)
                                        for (i in 1 until drawnSignaturePoints.size) {
                                            sigPath.lineTo(drawnSignaturePoints[i].x, drawnSignaturePoints[i].y)
                                        }
                                        drawPath(
                                            path = sigPath,
                                            color = SlateBg,
                                            style = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                                        )
                                    }
                                }

                                if (drawnSignaturePoints.isEmpty()) {
                                    Text(
                                        text = "Draw signature with finger here",
                                        modifier = Modifier.align(Alignment.Center),
                                        color = LogiGray.copy(alpha = 0.5f),
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { drawnSignaturePoints.clear() },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Clear board")
                                }

                                Button(
                                    onClick = { activeStep = 3 },
                                    modifier = Modifier.weight(1f).testTag("pod_signature_next_btn"),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Next Step")
                                }
                            }
                        }
                    }
                    3 -> { // Step 3: Photo Capture Upload
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "Capture Package Handover Photo",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            // Camera photo preview frame
                            Box(
                                modifier = Modifier
                                    .size(160.dp)
                                    .background(Color.LightGray.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                    .border(1.dp, Color.LightGray, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (photoCapturedBase64 != null) {
                                    // Simulated Package Photo Icon
                                    Icon(
                                        imageVector = Icons.Default.ShoppingCart,
                                        contentDescription = "Package Captured",
                                        tint = LogiGreen,
                                        modifier = Modifier.size(64.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.ShoppingCart,
                                        contentDescription = "Take Photo",
                                        tint = LogiGray,
                                        modifier = Modifier.size(48.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Button(
                                onClick = { 
                                    // Inject simple mock payload
                                    photoCapturedBase64 = "MOCK_JPEG_BASE64_PAYLOAD"
                                    Toast.makeText(context, "Package photo captured successfully!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.fillMaxWidth().testTag("pod_capture_photo_btn"),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Trigger Camera Shutter")
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedButton(
                                onClick = { activeStep = 4 },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Next Step")
                            }
                        }
                    }
                    4 -> { // Step 4: Final Summary & Dispatch Notes
                        Column {
                            Text(
                                "Finalize Delivery Proof",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = notesInput,
                                onValueChange = { notesInput = it },
                                label = { Text("Add Delivery Notes") },
                                placeholder = { Text("e.g., Handed directly to spouse, porch delivery") },
                                modifier = Modifier.fillMaxWidth().testTag("pod_notes_input")
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Summary Items
                            Text("Summary Checklist:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = LogiGray)
                            Text("- Barcode ID: ${barcodeResult.ifEmpty { "N/A" }}", fontSize = 12.sp)
                            Text("- Signature: ${if (drawnSignaturePoints.isNotEmpty()) "Signed" else "Missing"}", fontSize = 12.sp)
                            Text("- Photo: ${if (photoCapturedBase64 != null) "Captured" else "Missing"}", fontSize = 12.sp)

                            Spacer(modifier = Modifier.height(20.dp))

                            Button(
                                onClick = {
                                    // Generate a simulated drawn signature image as base64 or construct placeholder
                                    val finalSig = if (drawnSignaturePoints.isNotEmpty()) "CAPTURED_SIGNATURE" else null
                                    onPodComplete(
                                        finalSig,
                                        photoCapturedBase64,
                                        barcodeResult.ifEmpty { null },
                                        notesInput
                                    )
                                },
                                modifier = Modifier.fillMaxWidth().testTag("pod_finalize_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = LogiGreen),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Secure and Submit", color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}
