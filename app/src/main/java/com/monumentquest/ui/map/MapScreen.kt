package com.monumentquest.ui.map

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Satellite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.monumentquest.data.model.MapMonumentItem
import com.monumentquest.ui.common.UserAvatar
import com.monumentquest.ui.theme.*
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

// ── Gamified Illustrated Pastel Topographic Tile Source ───────────────────────
private val GamifiedTopographicTileSource = object : OnlineTileSourceBase(
    "GamifiedTopographic",
    0, 19, 256, ".png",
    arrayOf("https://basemaps.cartocdn.com/rastertiles/voyager/")
) {
    override fun getTileURLString(pMapTileIndex: Long): String {
        val zoom = MapTileIndex.getZoom(pMapTileIndex)
        val x = MapTileIndex.getX(pMapTileIndex)
        val y = MapTileIndex.getY(pMapTileIndex)
        return "https://basemaps.cartocdn.com/rastertiles/voyager/$zoom/$x/$y.png"
    }
}

private val PhotorealisticSatelliteTileSource = object : OnlineTileSourceBase(
    "PhotorealisticSatellite",
    0, 20, 256, ".jpeg",
    arrayOf("https://mt0.google.com/vt/lyrs=y&x=", "https://mt1.google.com/vt/lyrs=y&x=")
) {
    override fun getTileURLString(pMapTileIndex: Long): String {
        val zoom = MapTileIndex.getZoom(pMapTileIndex)
        val x = MapTileIndex.getX(pMapTileIndex)
        val y = MapTileIndex.getY(pMapTileIndex)
        return "https://mt1.google.com/vt/lyrs=y&x=$x&y=$y&z=$zoom"
    }
}

// Custom 3D Isometric Pin Marker Generator
private fun create3DIsometricMarker(context: Context, name: String, isSelected: Boolean): Drawable {
    val size = if (isSelected) 96 else 80
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Outer Teal Glow Ring
    paint.color = if (isSelected) AndroidColor.parseColor("#FFFFD700") else AndroidColor.parseColor("#6000E5FF")
    canvas.drawCircle(size / 2f, size / 2f, size / 2f - 2f, paint)

    // Inner Solid Container
    paint.color = AndroidColor.parseColor("#F8F9FA")
    canvas.drawCircle(size / 2f, size / 2f, size / 2f - 6f, paint)

    // Gold / Mint Core Badge
    paint.color = if (isSelected) AndroidColor.parseColor("#F3B61D") else AndroidColor.parseColor("#00C9A7")
    canvas.drawCircle(size / 2f, size / 2f, size / 2f - 14f, paint)

    return BitmapDrawable(context.resources, bitmap)
}

private fun createCyanVehicleMarker(context: Context): Drawable {
    val size = 72
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Cyan Pulsing Halo
    paint.color = AndroidColor.parseColor("#4000E5FF")
    canvas.drawCircle(size / 2f, size / 2f, size / 2f - 2f, paint)

    // Solid Blue Inner Beacon
    paint.color = AndroidColor.parseColor("#007AFF")
    canvas.drawCircle(size / 2f, size / 2f, size / 4f, paint)

    return BitmapDrawable(context.resources, bitmap)
}

data class RecentStopItem(
    val name: String,
    val time: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@SuppressLint("MissingPermission")
@Composable
fun MapScreen(
    onNavigateToCamera: () -> Unit,
    onNavigateToNarrator: (String) -> Unit,
    onNavigateToJournalist: () -> Unit = {},
    viewModel: MapViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val monuments           by viewModel.monuments.collectAsState()
    val userLocation        by viewModel.userLocation.collectAsState()
    val detectedCityName    by viewModel.detectedCityName.collectAsState()
    val speedKmh            by viewModel.currentSpeedKmh.collectAsState()
    val bearing             by viewModel.currentBearing.collectAsState()

    var selectedMonument by remember { mutableStateOf<MapMonumentItem?>(null) }
    var mapViewInstance  by remember { mutableStateOf<MapView?>(null) }
    var userMarker       by remember { mutableStateOf<Marker?>(null) }
    var coverageGrid     by remember { mutableStateOf<Polygon?>(null) }
    var isFollowingUser  by remember { mutableStateOf(true) }
    var isSatelliteMode  by remember { mutableStateOf(false) }

    var searchQuery      by remember { mutableStateOf("") }
    var isSheetExpanded  by remember { mutableStateOf(true) }

    val recentStops = remember {
        listOf(
            RecentStopItem("Old Town Heritage Cafe", "10:30 AM", Icons.Default.LocalCafe),
            RecentStopItem("Ekambrakanan Park Gate", "1:15 PM", Icons.Default.Park),
            RecentStopItem("Mukteshvara Temple Gate", "3:00 PM", Icons.Default.Place)
        )
    }

    LaunchedEffect(isSatelliteMode) {
        mapViewInstance?.let { map ->
            map.setTileSource(if (isSatelliteMode) PhotorealisticSatelliteTileSource else GamifiedTopographicTileSource)
            map.invalidate()
        }
    }

    LaunchedEffect(userLocation, bearing) {
        mapViewInstance?.let { map ->
            userLocation?.let { currentGeo ->
                if (userMarker == null) {
                    val m = Marker(map).apply {
                        title = "Live Position"
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        icon = createCyanVehicleMarker(context)
                    }
                    map.overlays.add(m)
                    userMarker = m
                }
                userMarker?.position = currentGeo
                userMarker?.rotation = bearing

                // Explored Fog-of-War Cyan Grid Circle Overlay
                if (coverageGrid == null) {
                    val circlePoints = Polygon.pointsAsCircle(currentGeo, 320.0)
                    val p = Polygon().apply {
                        points = circlePoints
                        fillPaint.color = AndroidColor.parseColor("#3000E5FF")
                        outlinePaint.color = AndroidColor.parseColor("#9000E5FF")
                        outlinePaint.strokeWidth = 4f
                    }
                    map.overlays.add(0, p)
                    coverageGrid = p
                } else {
                    coverageGrid?.points = Polygon.pointsAsCircle(currentGeo, 320.0)
                }

                if (isFollowingUser) {
                    map.controller.animateTo(currentGeo)
                }

                map.invalidate()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE5ECE9))
    ) {
        // ── 1. Gamified Topographic Map Canvas ─────────────────────────────
        AndroidView(
            factory = { ctx ->
                val config = Configuration.getInstance()
                config.load(ctx, ctx.getSharedPreferences("osmdroid_pref", Context.MODE_PRIVATE))
                config.userAgentValue = ctx.packageName

                MapView(ctx).apply {
                    setTileSource(if (isSatelliteMode) PhotorealisticSatelliteTileSource else GamifiedTopographicTileSource)
                    setMultiTouchControls(true)
                    controller.setZoom(17.5)

                    userLocation?.let { controller.setCenter(it) }
                    mapViewInstance = this

                    val locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(ctx), this).apply {
                        enableMyLocation()
                        enableFollowLocation()
                    }
                    overlays.add(locationOverlay)
                }
            },
            update = { map ->
                map.overlays.removeAll { it is Marker && it != userMarker }
                monuments.forEach { item ->
                    val isSel = selectedMonument?.id == item.id
                    val marker = Marker(map).apply {
                        position = item.geoPoint
                        title = item.name
                        subDescription = "${item.category} · ${item.points} XP"
                        icon = create3DIsometricMarker(context, item.name, isSel)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        setOnMarkerClickListener { _, _ ->
                            selectedMonument = item
                            isFollowingUser = false
                            map.controller.animateTo(item.geoPoint)
                            true
                        }
                    }
                    map.overlays.add(marker)
                }
                map.invalidate()
            },
            modifier = Modifier.fillMaxSize()
        )

        // ── 2. Top Floating Pill Search Bar ─────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = Color.White.copy(alpha = 0.95f),
                shadowElevation = 10.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    UserAvatar(name = "Aarav Patnaik", size = 36.dp)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = Color(0xFF64748B), modifier = Modifier.size(18.dp))
                        Box(modifier = Modifier.weight(1f)) {
                            if (searchQuery.isEmpty()) {
                                Text("Search area or landmarks...", color = Color(0xFF94A3B8), fontSize = 14.sp)
                            }
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                singleLine = true
                            )
                        }
                    }

                    IconButton(onClick = { isSatelliteMode = !isSatelliteMode }, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = if (isSatelliteMode) Icons.Default.Satellite else Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color(0xFF475569),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // ── 3. Bottom Left "📍 My Location" Pill Button ──────────────────────
        Surface(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(
                    start = 16.dp,
                    bottom = if (isSheetExpanded) 290.dp else 120.dp
                ),
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .clickable {
                        isFollowingUser = true
                        userLocation?.let { mapViewInstance?.controller?.animateTo(it) }
                    }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = null, tint = Color(0xFF007AFF), modifier = Modifier.size(16.dp))
                Text("My Location", fontWeight = FontWeight.Bold, color = Color(0xFF1E293B), fontSize = 13.sp)
            }
        }

        // ── 4. Bottom Right FAB Zoom Control ────────────────────────────────
        Surface(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(
                    end = 16.dp,
                    bottom = if (isSheetExpanded) 290.dp else 120.dp
                ),
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            shadowElevation = 8.dp
        ) {
            IconButton(
                onClick = { mapViewInstance?.controller?.zoomIn() },
                modifier = Modifier.size(44.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Zoom In", tint = Color(0xFF1E293B), modifier = Modifier.size(20.dp))
            }
        }

        // ── 5. Bottom "AREA COVERAGE & EXPLORATION" Slide Sheet ──────────────
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = Color.White,
            shadowElevation = 20.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                // Drag handle bar
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFCBD5E1))
                        .align(Alignment.CenterHorizontally)
                        .clickable { isSheetExpanded = !isSheetExpanded }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Title
                Text(
                    text = "AREA COVERAGE & EXPLORATION",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF1E293B),
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.1.sp,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Primary Metrics Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Total Covered Area", style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B), fontSize = 11.sp)
                        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("4.2 km²", fontWeight = FontWeight.Black, color = Color(0xFF0F172A), fontSize = 18.sp)
                            Text("(35% of district)", style = MaterialTheme.typography.labelSmall, color = Color(0xFF00C9A7), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("Total Track", style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B), fontSize = 11.sp)
                        Text("12.5 km", fontWeight = FontWeight.Black, color = Color(0xFF0F172A), fontSize = 18.sp)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Breakdown Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Structures\nVisited: 15", style = MaterialTheme.typography.labelSmall, color = Color(0xFF475569), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("Roads Fully\nTraveled: 21", style = MaterialTheme.typography.labelSmall, color = Color(0xFF475569), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("Public Spaces\nExplored: 3", style = MaterialTheme.typography.labelSmall, color = Color(0xFF475569), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                if (isSheetExpanded) {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color(0xFFF1F5F9))
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Recent Stops",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color(0xFF1E293B),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        recentStops.forEach { stop ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFF1F5F9)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(stop.icon, null, tint = Color(0xFF475569), modifier = Modifier.size(16.dp))
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(stop.name, fontWeight = FontWeight.Bold, color = Color(0xFF334155), fontSize = 13.sp)
                                        Icon(Icons.Default.ChevronRight, null, tint = Color(0xFF94A3B8), modifier = Modifier.size(14.dp))
                                    }
                                }
                                Text(stop.time, style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B), fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
