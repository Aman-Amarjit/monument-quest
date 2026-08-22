package com.monumentquest.ui.map

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Satellite
import androidx.compose.material.icons.filled.Search
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
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

// ── 3D Stylized Vector City Model Tile Source ─────────────────────────────
private val Stylized3DCityTileSource = object : OnlineTileSourceBase(
    "Stylized3DCity",
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

// ── 3D Isometric City Model Canvas Overlay ──────────────────────────────────
class Isometric3DCityOverlay : Overlay() {
    private val highwayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.parseColor("#F59E0B") // Warm Golden Highway
        style = Paint.Style.STROKE
        strokeWidth = 22f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        setShadowLayer(8f, 0f, 6f, AndroidColor.parseColor("#40000000"))
    }

    private val highwayBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.parseColor("#D97706") // Highway Edge Border
        style = Paint.Style.STROKE
        strokeWidth = 26f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val buildingBlockPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.parseColor("#94A3B8") // 3D Slate Grey Building
        style = Paint.Style.FILL
        setShadowLayer(6f, 3f, 6f, AndroidColor.parseColor("#30000000"))
    }

    private val buildingRoofPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.parseColor("#CBD5E1") // Lighter Roof Top
        style = Paint.Style.FILL
    }

    private val parkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.parseColor("#86EFAC") // Pastel Green Park
        style = Paint.Style.FILL
    }

    private val waterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.parseColor("#38BDF8") // Vibrant Blue Water
        style = Paint.Style.FILL
    }

    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        if (shadow) return
        val w = mapView.width.toFloat()
        val h = mapView.height.toFloat()
        if (w <= 0 || h <= 0) return

        // 1. Draw Water Canals & Ponds
        val waterPath = Path().apply {
            moveTo(0f, h * 0.7f)
            cubicTo(w * 0.3f, h * 0.65f, w * 0.5f, h * 0.85f, w, h * 0.75f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        canvas.drawPath(waterPath, waterPaint)

        // 2. Draw Pastel Green Parks
        val parkPath = Path().apply {
            moveTo(w * 0.6f, h * 0.1f)
            lineTo(w * 0.95f, h * 0.15f)
            lineTo(w * 0.9f, h * 0.45f)
            lineTo(w * 0.55f, h * 0.35f)
            close()
        }
        canvas.drawPath(parkPath, parkPaint)

        // 3. Draw Elevated 3D Golden Highways & Overpass Loop
        val highwayPath = Path().apply {
            moveTo(-50f, h * 0.25f)
            cubicTo(w * 0.35f, h * 0.2f, w * 0.45f, h * 0.45f, w + 50f, h * 0.55f)
        }
        canvas.drawPath(highwayPath, highwayBorderPaint)
        canvas.drawPath(highwayPath, highwayPaint)

        // Golden Overpass Roundabout Loop
        val loopCenterX = w * 0.45f
        val loopCenterY = h * 0.45f
        canvas.drawCircle(loopCenterX, loopCenterY, 38f, highwayBorderPaint)
        canvas.drawCircle(loopCenterX, loopCenterY, 38f, highwayPaint)
        canvas.drawCircle(loopCenterX, loopCenterY, 18f, parkPaint)

        // 4. Draw Extruded 3D Slate Grey Building Blocks
        val stepX = 120f
        val stepY = 90f
        var startY = 80f
        while (startY < h * 0.65f) {
            var startX = 40f
            while (startX < w - 60f) {
                // Skip highway path collision
                if (Math.abs(startX - loopCenterX) > 60f || Math.abs(startY - loopCenterY) > 60f) {
                    val bw = 65f
                    val bh = 45f
                    val depth = 12f

                    // 3D Building Base Side
                    canvas.drawRect(startX, startY + depth, startX + bw, startY + bh + depth, buildingBlockPaint)
                    // 3D Building Roof Top
                    canvas.drawRect(startX, startY, startX + bw, startY + bh, buildingRoofPaint)
                }
                startX += stepX
            }
            startY += stepY
        }
    }
}

// ── 3D Golden Loop Pin Marker (Matching Reference Image) ────────────────────
private fun createGolden3DLoopMarker(context: Context, isSelected: Boolean): Drawable {
    val size = if (isSelected) 96 else 80
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Outer Glow Ring
    paint.color = if (isSelected) AndroidColor.parseColor("#FFFFD700") else AndroidColor.parseColor("#80F59E0B")
    canvas.drawCircle(size / 2f, size / 2f, size / 2f - 2f, paint)

    // Inner White Container
    paint.color = AndroidColor.parseColor("#FFFFFF")
    canvas.drawCircle(size / 2f, size / 2f, size / 2f - 6f, paint)

    // Core Golden Pin Loop
    paint.color = AndroidColor.parseColor("#F59E0B")
    canvas.drawCircle(size / 2f, size / 2f, size / 2f - 14f, paint)

    // Center Core Dot
    paint.color = AndroidColor.parseColor("#FFFFFF")
    canvas.drawCircle(size / 2f, size / 2f, 8f, paint)

    return BitmapDrawable(context.resources, bitmap)
}

private fun createCyanVehicleMarker(context: Context): Drawable {
    val size = 72
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    paint.color = AndroidColor.parseColor("#4000E5FF")
    canvas.drawCircle(size / 2f, size / 2f, size / 2f - 2f, paint)

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
            map.setTileSource(if (isSatelliteMode) PhotorealisticSatelliteTileSource else Stylized3DCityTileSource)
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
        // ── 1. 3D Isometric Stylized City Map Canvas ───────────────────────
        AndroidView(
            factory = { ctx ->
                val config = Configuration.getInstance()
                config.load(ctx, ctx.getSharedPreferences("osmdroid_pref", Context.MODE_PRIVATE))
                config.userAgentValue = ctx.packageName

                MapView(ctx).apply {
                    setTileSource(if (isSatelliteMode) PhotorealisticSatelliteTileSource else Stylized3DCityTileSource)
                    setMultiTouchControls(true)
                    controller.setZoom(17.5)
                    mapOrientation = 45f // 3D Isometric Perspective Tilt

                    userLocation?.let { controller.setCenter(it) }
                    mapViewInstance = this

                    val locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(ctx), this).apply {
                        enableMyLocation()
                        enableFollowLocation()
                    }
                    overlays.add(locationOverlay)

                    // Add 3D Stylized City Model Overlay
                    if (!isSatelliteMode) {
                        overlays.add(0, Isometric3DCityOverlay())
                    }
                }
            },
            update = { map ->
                map.overlays.removeAll { it is Marker && it != userMarker }
                if (!isSatelliteMode && map.overlays.none { it is Isometric3DCityOverlay }) {
                    map.overlays.add(0, Isometric3DCityOverlay())
                } else if (isSatelliteMode) {
                    map.overlays.removeAll { it is Isometric3DCityOverlay }
                }

                monuments.forEach { item ->
                    val isSel = selectedMonument?.id == item.id
                    val marker = Marker(map).apply {
                        position = item.geoPoint
                        title = item.name
                        subDescription = "${item.category} · ${item.points} XP"
                        icon = createGolden3DLoopMarker(context, isSel)
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
                .padding(top = 14.dp, start = 16.dp, end = 16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = Color.White.copy(alpha = 0.96f),
                shadowElevation = 12.dp,
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
                                Text("Search area or landmarks...", color = Color(0xFF94A3B8), fontSize = 13.sp)
                            }
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                singleLine = true
                            )
                        }
                    }

                    // 1-Tap Map Mode Toggle Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (isSatelliteMode) Color(0xFF0F172A) else Color(0xFFF59E0B))
                            .clickable { isSatelliteMode = !isSatelliteMode }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(
                                imageVector = if (isSatelliteMode) Icons.Default.Satellite else Icons.Default.Map,
                                contentDescription = "Toggle Map Mode",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (isSatelliteMode) "Satellite" else "3D City",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }
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
                    bottom = if (isSheetExpanded) 310.dp else 170.dp
                ),
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            shadowElevation = 10.dp
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
                    bottom = if (isSheetExpanded) 310.dp else 170.dp
                ),
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            shadowElevation = 10.dp
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
            shadowElevation = 24.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFCBD5E1))
                        .align(Alignment.CenterHorizontally)
                        .clickable { isSheetExpanded = !isSheetExpanded }
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isSheetExpanded = !isSheetExpanded },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "AREA COVERAGE & EXPLORATION",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF1E293B),
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.1.sp,
                        fontSize = 12.sp
                    )

                    Text(
                        text = if (isSheetExpanded) "Collapse ▲" else "Expand ▼",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF007AFF),
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Total Covered Area", style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B), fontSize = 11.sp)
                        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("4.2 km²", fontWeight = FontWeight.Black, color = Color(0xFF0F172A), fontSize = 17.sp)
                            Text("(35% of district)", style = MaterialTheme.typography.labelSmall, color = Color(0xFF00C9A7), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("Total Track", style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B), fontSize = 11.sp)
                        Text("12.5 km", fontWeight = FontWeight.Black, color = Color(0xFF0F172A), fontSize = 17.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Structures\nVisited: 15", style = MaterialTheme.typography.labelSmall, color = Color(0xFF475569), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("Roads Fully\nTraveled: 21", style = MaterialTheme.typography.labelSmall, color = Color(0xFF475569), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("Public Spaces\nExplored: 3", style = MaterialTheme.typography.labelSmall, color = Color(0xFF475569), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                if (isSheetExpanded) {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color(0xFFF1F5F9))
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Recent Stops",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color(0xFF1E293B),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFF1F5F9)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(stop.icon, null, tint = Color(0xFF475569), modifier = Modifier.size(15.dp))
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(stop.name, fontWeight = FontWeight.Bold, color = Color(0xFF334155), fontSize = 12.sp)
                                        Icon(Icons.Default.ChevronRight, null, tint = Color(0xFF94A3B8), modifier = Modifier.size(14.dp))
                                    }
                                }
                                Text(stop.time, style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B), fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
