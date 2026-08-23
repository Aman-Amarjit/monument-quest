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
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.monumentquest.data.model.MapMonumentItem
import com.monumentquest.ui.theme.*
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

// ── Tile source — OpenStreetMap (free, no key, always works) ─────────────────
private val OsmTileSource = object : OnlineTileSourceBase(
    "Mapnik", 0, 19, 256, ".png",
    arrayOf(
        "https://a.tile.openstreetmap.org/",
        "https://b.tile.openstreetmap.org/",
        "https://c.tile.openstreetmap.org/"
    )
) {
    override fun getTileURLString(pMapTileIndex: Long): String {
        val zoom = MapTileIndex.getZoom(pMapTileIndex)
        val x    = MapTileIndex.getX(pMapTileIndex)
        val y    = MapTileIndex.getY(pMapTileIndex)
        return "${baseUrl}$zoom/$x/$y.png"
    }
}

// ── Marker helpers ────────────────────────────────────────────────────────────

private fun createMonumentMarker(context: Context, isSelected: Boolean): Drawable {
    val size   = if (isSelected) 80 else 60
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint  = Paint(Paint.ANTI_ALIAS_FLAG)
    val cx     = size / 2f

    paint.color = AndroidColor.parseColor(if (isSelected) "#50F0A500" else "#30F0A500")
    canvas.drawCircle(cx, cx, cx - 2f, paint)
    paint.color = AndroidColor.WHITE
    canvas.drawCircle(cx, cx, cx - 7f, paint)
    paint.color = AndroidColor.parseColor("#F0A500")
    canvas.drawCircle(cx, cx, cx - 14f, paint)
    paint.color = AndroidColor.WHITE
    canvas.drawCircle(cx, cx, if (isSelected) 8f else 6f, paint)

    return BitmapDrawable(context.resources, bitmap)
}

private fun createUserDot(context: Context): Drawable {
    val size   = 48
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint  = Paint(Paint.ANTI_ALIAS_FLAG)
    val cx     = size / 2f

    // Outer pulse ring
    paint.color = AndroidColor.parseColor("#300080FF")
    canvas.drawCircle(cx, cx, cx - 2f, paint)
    // White border
    paint.color = AndroidColor.WHITE
    canvas.drawCircle(cx, cx, cx / 2f + 2f, paint)
    // Blue fill
    paint.color = AndroidColor.parseColor("#0080FF")
    canvas.drawCircle(cx, cx, cx / 2f - 1f, paint)

    return BitmapDrawable(context.resources, bitmap)
}

// ── Data ──────────────────────────────────────────────────────────────────────

data class RecentStopItem(
    val name: String,
    val time: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

// ── Screen ────────────────────────────────────────────────────────────────────

@SuppressLint("MissingPermission")
@Composable
fun MapScreen(
    onNavigateToCamera: () -> Unit,
    onNavigateToNarrator: (String) -> Unit,
    onNavigateToJournalist: () -> Unit = {},
    viewModel: MapViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    val monuments      by viewModel.monuments.collectAsState()
    val userLocation   by viewModel.userLocation.collectAsState()
    val bearing        by viewModel.currentBearing.collectAsState()
    val coverageStats  by viewModel.coverageStats.collectAsState()
    val walkPathPoints by viewModel.walkPathPoints.collectAsState()

    var selectedMonument  by remember { mutableStateOf<MapMonumentItem?>(null) }
    var mapViewInstance   by remember { mutableStateOf<MapView?>(null) }
    var userMarker        by remember { mutableStateOf<Marker?>(null) }
    val monumentMarkers   = remember { mutableMapOf<String, Marker>() }
    val selectedMarkerIds = remember { mutableStateOf<Set<String>>(emptySet()) }
    val walkPolyline      = remember { mutableStateOf<Polyline?>(null) }

    var isFollowingUser  by remember { mutableStateOf(true) }
    var isSatelliteMode  by remember { mutableStateOf(false) }
    var searchQuery      by remember { mutableStateOf("") }
    var isSheetExpanded  by remember { mutableStateOf(false) }
    // Track if we've done the first GPS zoom-in
    var hasZoomedToUser  by remember { mutableStateOf(false) }

    val sheetBottomPadding: Dp by animateDpAsState(
        targetValue   = if (isSheetExpanded) 300.dp else 152.dp,
        animationSpec = tween(280),
        label         = "sheetPad"
    )

    val recentStops = remember {
        listOf(
            RecentStopItem("Old Town Heritage Cafe",  "10:30 AM", Icons.Default.LocalCafe),
            RecentStopItem("Ekambrakanan Park Gate",  "1:15 PM",  Icons.Default.Park),
            RecentStopItem("Mukteshvara Temple Gate", "3:00 PM",  Icons.Default.Place)
        )
    }

    val currentSatelliteMode by rememberUpdatedState(isSatelliteMode)

    // ── Walk path polyline ────────────────────────────────────────────────────
    LaunchedEffect(walkPathPoints) {
        mapViewInstance?.let { map ->
            if (walkPathPoints.size < 2) return@let
            val existing = walkPolyline.value
            if (existing == null) {
                val poly = Polyline(map).apply {
                    outlinePaint.color       = AndroidColor.parseColor("#CC3B82F6")
                    outlinePaint.strokeWidth = 10f
                    outlinePaint.strokeCap   = Paint.Cap.ROUND
                    outlinePaint.strokeJoin  = Paint.Join.ROUND
                    setPoints(walkPathPoints)
                }
                map.overlays.add(0, poly)
                walkPolyline.value = poly
            } else {
                existing.setPoints(walkPathPoints)
            }
            map.invalidate()
        }
    }

    // ── User location ─────────────────────────────────────────────────────────
    LaunchedEffect(userLocation) {
        mapViewInstance?.let { map ->
            userLocation?.let { geo ->
                if (userMarker == null) {
                    val m = Marker(map).apply {
                        title = "You"
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        icon = createUserDot(context)
                    }
                    map.overlays.add(m)
                    userMarker = m
                }
                userMarker?.position = geo

                // First GPS fix: zoom in once
                if (!hasZoomedToUser) {
                    map.controller.setZoom(17.0)
                    map.controller.setCenter(geo)
                    hasZoomedToUser = true
                } else if (isFollowingUser) {
                    map.controller.animateTo(geo)
                }
                map.invalidate()
            }
        }
    }

    // ── Monument markers ──────────────────────────────────────────────────────
    LaunchedEffect(monuments, selectedMonument) {
        mapViewInstance?.let { map ->
            val newSelectedIds = mutableSetOf<String>()
            selectedMonument?.let { newSelectedIds.add(it.id) }

            monuments.forEach { item ->
                val isSel    = selectedMonument?.id == item.id
                val existing = monumentMarkers[item.id]
                if (existing == null) {
                    val marker = Marker(map).apply {
                        position = item.geoPoint
                        title    = item.name
                        icon     = createMonumentMarker(context, isSel)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        setOnMarkerClickListener { _, _ ->
                            selectedMonument = item
                            isFollowingUser  = false
                            map.controller.animateTo(item.geoPoint)
                            true
                        }
                    }
                    map.overlays.add(marker)
                    monumentMarkers[item.id] = marker
                } else {
                    existing.position = item.geoPoint
                    val wasSelected = selectedMarkerIds.value.contains(item.id)
                    if (isSel != wasSelected) {
                        existing.icon = createMonumentMarker(context, isSel)
                    }
                }
            }
            selectedMarkerIds.value = newSelectedIds
            map.invalidate()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // ── OSMDroid map ──────────────────────────────────────────────────────
        AndroidView(
            factory = { ctx ->
                val cfg = Configuration.getInstance()
                cfg.load(ctx, ctx.getSharedPreferences("osmdroid_pref", Context.MODE_PRIVATE))
                cfg.userAgentValue = "${ctx.packageName}/1.0 (MonumentQuest)"
                cfg.tileCacheMaxQueueSize = 12

                MapView(ctx).apply {
                    setTileSource(OsmTileSource)
                    setMultiTouchControls(true)
                    // Default view: India, zoom 5 — snaps to GPS on first fix
                    controller.setZoom(5.0)
                    controller.setCenter(GeoPoint(20.5937, 78.9629))
                    mapOrientation = 0f
                    mapViewInstance = this
                }
            },
            update = { mv ->
                if (mv.tileProvider.tileSource != OsmTileSource) {
                    mv.setTileSource(OsmTileSource)
                    mv.invalidate()
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // ── Search bar ────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, top = 12.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White.copy(alpha = 0.97f)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Search, null,
                tint     = Color(0xFF94A3B8),
                modifier = Modifier.padding(start = 14.dp).size(18.dp)
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp, vertical = 13.dp)
            ) {
                if (searchQuery.isEmpty()) {
                    Text("Search monuments or places…", color = Color(0xFFB0BEC5), fontSize = 13.sp)
                }
                BasicTextField(
                    value         = searchQuery,
                    onValueChange = { searchQuery = it },
                    singleLine    = true,
                    textStyle     = TextStyle(fontSize = 13.sp, color = Color(0xFF1E293B))
                )
            }
            Box(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFF0A500))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment    = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.Map, null, tint = Color.White, modifier = Modifier.size(13.dp))
                    Text("Map", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // ── Monument info card ────────────────────────────────────────────────
        AnimatedVisibility(
            visible  = selectedMonument != null,
            enter    = slideInVertically({ it }, tween(300)) + fadeIn(tween(200)),
            exit     = slideOutVertically({ it }, tween(240)) + fadeOut(tween(160)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = sheetBottomPadding + 12.dp)
        ) {
            selectedMonument?.let { mon ->
                MonumentCard(
                    monument   = mon,
                    onDismiss  = { selectedMonument = null },
                    onExplore  = { onNavigateToNarrator(mon.id) },
                    onLogVisit = { onNavigateToCamera() }
                )
            }
        }

        // ── Right FABs ────────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 14.dp, bottom = sheetBottomPadding + 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MapFab(onClick = { mapViewInstance?.controller?.zoomIn() }) {
                Icon(Icons.Default.Add, null, tint = Color(0xFF334155), modifier = Modifier.size(18.dp))
            }
            MapFab(onClick = {
                isFollowingUser = true
                userLocation?.let { mapViewInstance?.controller?.animateTo(it) }
            }) {
                Icon(Icons.Default.MyLocation, null, tint = Color(0xFF0080FF), modifier = Modifier.size(18.dp))
            }
        }

        // ── Camera FAB ────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = sheetBottomPadding + 14.dp)
                .size(52.dp)
                .clip(CircleShape)
                .background(Color(0xFFF0A500))
                .clickable { onNavigateToCamera() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.AddAPhoto, "Log Visit", tint = Color.White, modifier = Modifier.size(22.dp))
        }

        // ── Bottom sheet ──────────────────────────────────────────────────────
        Surface(
            modifier        = Modifier.fillMaxWidth().align(Alignment.BottomCenter),
            shape           = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            color           = Color.White,
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(36.dp).height(3.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE2E8F0))
                        .align(Alignment.CenterHorizontally)
                        .clickable { isSheetExpanded = !isSheetExpanded }
                )
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CoverageStat("Distance", coverageStats.totalTrackFormatted,         Modifier.weight(1f))
                    CoverageStat("Area",     coverageStats.coveredAreaFormatted,         Modifier.weight(1f))
                    CoverageStat("Sites",    "${coverageStats.structuresVisitedCount}",  Modifier.weight(1f))
                }

                AnimatedVisibility(
                    visible = isSheetExpanded,
                    enter   = fadeIn(tween(180)) + slideInVertically(tween(240)),
                    exit    = fadeOut(tween(140)) + slideOutVertically(tween(200))
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = Color(0xFFF1F5F9))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Recent Stops", fontWeight = FontWeight.SemiBold, color = Color(0xFF1E293B), fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            recentStops.forEach { stop ->
                                Row(
                                    modifier              = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment     = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment    = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier.size(30.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFF8FAFC)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(stop.icon, null, tint = Color(0xFF64748B), modifier = Modifier.size(15.dp))
                                        }
                                        Text(stop.name, fontWeight = FontWeight.Medium, color = Color(0xFF334155), fontSize = 13.sp)
                                    }
                                    Text(stop.time, color = Color(0xFF94A3B8), fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier              = Modifier.fillMaxWidth().padding(top = 10.dp).clickable { isSheetExpanded = !isSheetExpanded },
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        if (isSheetExpanded) "Show less ▲" else "Recent stops ▼",
                        color = Color(0xFF94A3B8), fontSize = 11.sp
                    )
                }
            }
        }
    }
}

// ── Monument card ─────────────────────────────────────────────────────────────

@Composable
private fun MonumentCard(
    monument: MapMonumentItem,
    onDismiss: () -> Unit,
    onExplore: () -> Unit,
    onLogVisit: () -> Unit = {}
) {
    Surface(
        shape = RoundedCornerShape(16.dp), color = Color.White,
        shadowElevation = 16.dp, modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(monument.name, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        monument.category.lowercase().replaceFirstChar { it.uppercase() },
                        color = Color(0xFF64748B), fontSize = 12.sp
                    )
                }
                Box(
                    modifier = Modifier.size(26.dp).clip(CircleShape).background(Color(0xFFF1F5F9)).clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Close, null, tint = Color(0xFF94A3B8), modifier = Modifier.size(13.dp))
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.clip(RoundedCornerShape(7.dp)).background(Color(0xFFFEF3C7)).padding(horizontal = 9.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.Star, null, tint = Color(0xFFF0A500), modifier = Modifier.size(12.dp))
                    Text("${monument.points} XP", color = Color(0xFFB45309), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
                Box(modifier = Modifier.clip(RoundedCornerShape(7.dp)).background(Color(0xFFEFF6FF)).padding(horizontal = 9.dp, vertical = 4.dp)) {
                    Text(formatDistance(monument.distanceMeters), color = Color(0xFF1D4ED8), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onExplore, modifier = Modifier.weight(1f).height(42.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF334155))
                ) { Text("Narrator", fontSize = 13.sp, fontWeight = FontWeight.Medium) }
                Button(
                    onClick = onLogVisit, modifier = Modifier.weight(1.4f).height(42.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF0A500))
                ) {
                    Icon(Icons.Default.AddAPhoto, null, modifier = Modifier.size(15.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Log Visit", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun CoverageStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier            = modifier.clip(RoundedCornerShape(10.dp)).background(Color(0xFFF8FAFC)).padding(vertical = 10.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(value, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), fontSize = 15.sp)
        Text(label, color = Color(0xFF94A3B8), fontSize = 10.sp)
    }
}

@Composable
private fun MapFab(onClick: () -> Unit, content: @Composable () -> Unit) {
    Surface(modifier = Modifier.size(40.dp), shape = RoundedCornerShape(10.dp), color = Color.White, shadowElevation = 8.dp) {
        Box(modifier = Modifier.fillMaxSize().clickable { onClick() }, contentAlignment = Alignment.Center) { content() }
    }
}

private fun formatDistance(meters: Int): String = when {
    meters <= 0   -> "Nearby"
    meters < 1000 -> "$meters m away"
    else          -> String.format("%.1f km away", meters / 1000.0)
}
