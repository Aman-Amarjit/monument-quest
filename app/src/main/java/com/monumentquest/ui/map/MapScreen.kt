package com.monumentquest.ui.map

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
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
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.Polygon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

private suspend fun fetchRoute(
    fromLat: Double, fromLon: Double,
    toLat: Double,   toLon: Double
): List<GeoPoint> = withContext(Dispatchers.IO) {
    try {
        val url      = "https://router.project-osrm.org/route/v1/walking/" +
            "$fromLon,$fromLat;$toLon,$toLat?overview=full&geometries=geojson"
        val response = URL(url).readText()
        val json     = JSONObject(response)
        val coords   = json.getJSONArray("routes")
            .getJSONObject(0)
            .getJSONObject("geometry")
            .getJSONArray("coordinates")
        (0 until coords.length()).map { i ->
            val pt = coords.getJSONArray(i)
            GeoPoint(pt.getDouble(1), pt.getDouble(0))
        }
    } catch (e: Exception) { emptyList() }
}

private fun createMonumentMarker(context: Context, isSelected: Boolean, distanceText: String): Drawable {
    val pinSize = if (isSelected) 54 else 44
    val textH   = if (distanceText.isNotBlank()) 30 else 0
    val pad     = if (distanceText.isNotBlank()) 6 else 0
    val totalW  = maxOf(pinSize + 20, 140)
    val totalH  = pinSize + textH + pad + 10

    val bitmap = Bitmap.createBitmap(totalW, totalH, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint  = Paint(Paint.ANTI_ALIAS_FLAG)
    val cx     = totalW / 2f
    val cy     = pinSize / 2f + 4f

    paint.color = AndroidColor.parseColor("#5010B981")
    canvas.drawCircle(cx, cy, cy - 2f, paint)

    paint.color = AndroidColor.WHITE
    canvas.drawCircle(cx, cy, cy - 5f, paint)

    paint.color = AndroidColor.parseColor(if (isSelected) "#FFB703" else "#F0A500")
    canvas.drawCircle(cx, cy, cy - 9f, paint)

    paint.color = AndroidColor.WHITE
    val emblemRadius = if (isSelected) 8f else 6f
    canvas.drawCircle(cx, cy, emblemRadius, paint)
    paint.color = AndroidColor.parseColor("#D97706")
    canvas.drawCircle(cx, cy, emblemRadius - 3f, paint)

    if (distanceText.isNotBlank()) {
        val labelTop = (pinSize + pad).toFloat()
        paint.color = AndroidColor.parseColor("#F00F172A")
        val rect = RectF(cx - 56f, labelTop, cx + 56f, labelTop + 26f)
        canvas.drawRoundRect(rect, 13f, 13f, paint)

        paint.color = AndroidColor.parseColor("#8010B981")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        canvas.drawRoundRect(rect, 13f, 13f, paint)

        paint.style          = Paint.Style.FILL
        paint.color          = AndroidColor.WHITE
        paint.textSize       = 18f
        paint.textAlign      = Paint.Align.CENTER
        paint.isFakeBoldText = true
        canvas.drawText(distanceText, cx, labelTop + 19f, paint)
    }
    return BitmapDrawable(context.resources, bitmap)
}


private fun createHotelMarker(context: Context, hotelName: String, discountText: String): Drawable {
    val totalW = 160
    val totalH = 70
    val bitmap = Bitmap.createBitmap(totalW, totalH, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint  = Paint(Paint.ANTI_ALIAS_FLAG)

    // Gold Pill Container
    paint.color = AndroidColor.parseColor("#F00F172A")
    val rect = RectF(4f, 4f, totalW - 4f, totalH - 4f)
    canvas.drawRoundRect(rect, 18f, 18f, paint)

    paint.color = AndroidColor.parseColor("#FFF0A500")
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 3f
    canvas.drawRoundRect(rect, 18f, 18f, paint)

    // Inner Text
    paint.style = Paint.Style.FILL
    paint.color = AndroidColor.WHITE
    paint.textSize = 18f
    paint.textAlign = Paint.Align.CENTER
    paint.isFakeBoldText = true
    canvas.drawText("🏨 " + discountText, totalW / 2f, 32f, paint)

    paint.textSize = 15f
    paint.color = AndroidColor.parseColor("#FF94A3B8")
    val shortName = if (hotelName.length > 14) hotelName.take(12) + ".." else hotelName
    canvas.drawText(shortName, totalW / 2f, 54f, paint)

    return BitmapDrawable(context.resources, bitmap)
}

private fun createUserDot(context: Context): Drawable {
    // A character marker makes the player’s position instantly readable on the map.
    // Keep the canvas generously padded so the marker remains crisp while moving.
    val size = 76
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { isDither = true }
    val cx = size / 2f

    // Warm location halo, intentionally not a blue GPS dot.
    paint.color = AndroidColor.parseColor("#2610B981")
    canvas.drawCircle(cx, 37f, 31f, paint)
    paint.color = AndroidColor.parseColor("#6010B981")
    canvas.drawCircle(cx, 37f, 25f, paint)

    // Ground shadow.
    paint.color = AndroidColor.parseColor("#550B1220")
    canvas.drawOval(RectF(20f, 61f, 56f, 70f), paint)

    // Legs and boots.
    paint.color = AndroidColor.parseColor("#243044")
    canvas.drawRoundRect(RectF(30f, 49f, 37f, 64f), 3f, 3f, paint)
    canvas.drawRoundRect(RectF(39f, 49f, 46f, 64f), 3f, 3f, paint)
    paint.color = AndroidColor.parseColor("#111827")
    canvas.drawRoundRect(RectF(28f, 60f, 38f, 66f), 3f, 3f, paint)
    canvas.drawRoundRect(RectF(38f, 60f, 49f, 66f), 3f, 3f, paint)

    // Explorer jacket and arms.
    paint.color = AndroidColor.parseColor("#E79A24")
    canvas.drawRoundRect(RectF(25f, 30f, 51f, 54f), 9f, 9f, paint)
    paint.color = AndroidColor.parseColor("#FFC857")
    canvas.drawRoundRect(RectF(28f, 31f, 48f, 52f), 7f, 7f, paint)
    paint.color = AndroidColor.parseColor("#D17A16")
    canvas.drawRoundRect(RectF(22f, 34f, 29f, 49f), 3f, 3f, paint)
    canvas.drawRoundRect(RectF(47f, 34f, 54f, 49f), 3f, 3f, paint)

    // Backpack peeking over the shoulders.
    paint.color = AndroidColor.parseColor("#176B5A")
    canvas.drawRoundRect(RectF(23f, 29f, 29f, 45f), 3f, 3f, paint)

    // Head, hair, and face.
    paint.color = AndroidColor.parseColor("#F4B183")
    canvas.drawCircle(cx, 22f, 10f, paint)
    paint.color = AndroidColor.parseColor("#3A2418")
    canvas.drawArc(RectF(28f, 11f, 48f, 30f), 180f, 180f, true, paint)
    paint.color = AndroidColor.parseColor("#F4B183")
    canvas.drawCircle(31f, 23f, 2.5f, paint)
    canvas.drawCircle(45f, 23f, 2.5f, paint)

    // Small compass badge on the jacket.
    paint.color = AndroidColor.WHITE
    canvas.drawCircle(cx, 41f, 4f, paint)
    paint.color = AndroidColor.parseColor("#0F766E")
    canvas.drawCircle(cx, 41f, 2f, paint)

    return BitmapDrawable(context.resources, bitmap).apply {
        setBounds(0, 0, size, size)
    }
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

    val monuments        by viewModel.monuments.collectAsState()
    val userLocation     by viewModel.userLocation.collectAsState()
    val bearing          by viewModel.currentBearing.collectAsState()
    val coverageStats    by viewModel.coverageStats.collectAsState()
    val walkPathPoints   by viewModel.walkPathPoints.collectAsState()

    val searchResults    by viewModel.searchResults.collectAsState()
    val isSearching      by viewModel.isSearching.collectAsState()
    val userProfile        by viewModel.userProfile.collectAsState()

    var selectedMonument  by remember { mutableStateOf<MapMonumentItem?>(null) }
    var mapViewInstance   by remember { mutableStateOf<PerspectiveMapView?>(null) }
    var userMarker        by remember { mutableStateOf<Marker?>(null) }
    val monumentMarkers   = remember { mutableMapOf<String, Marker>() }
    val geofencePolygons  = remember { mutableMapOf<String, Polygon>() }
    val hotelMarkers       = remember { mutableMapOf<String, Marker>() }
    val selectedMarkerIds = remember { mutableStateOf<Set<String>>(emptySet()) }
    val walkPolyline      = remember { mutableStateOf<Polyline?>(null) }
    val routePolyline     = remember { mutableStateOf<Polyline?>(null) }
    val mapViewRef        = remember { mutableStateOf<MapView?>(null) }


    var isFollowingUser  by remember { mutableStateOf(true) }
    var searchQuery      by remember { mutableStateOf("") }
    var isSheetExpanded  by remember { mutableStateOf(false) }
    var hasZoomedToUser  by remember { mutableStateOf(false) }


    val collapsedSheetH = 195.dp
    val cardBottomPad: Dp by animateDpAsState(
        targetValue   = if (isSheetExpanded) 360.dp else collapsedSheetH + 10.dp,
        animationSpec = tween(280), label = "cardPad"
    )

    val geofenceRadiusMeters = 150.0

    val isCapturableInRange = remember(monuments, selectedMonument, userLocation) {
        selectedMonument?.let { it.distanceMeters in 1..geofenceRadiusMeters.toInt() }
            ?: monuments.any { it.distanceMeters in 1..geofenceRadiusMeters.toInt() }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue  = if (isCapturableInRange) 1.15f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation  = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val pulseGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue  = if (isCapturableInRange) 0.85f else 0.25f,
        animationSpec = infiniteRepeatable(
            animation  = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseGlow"
    )

    val attemptCapture: () -> Unit = {
        if (isCapturableInRange) {
            Toast.makeText(context, "🟢 Instant Verification Mode: +500 XP!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "✨ Special Discovery Mode: Live photo will be submitted for Community Review (+250 XP Pending)", Toast.LENGTH_LONG).show()
        }
        onNavigateToCamera()
    }

    LaunchedEffect(selectedMonument) {
        if (selectedMonument != null) isSheetExpanded = false
    }

    val recentStops = remember {
        listOf(
            RecentStopItem("Old Town Heritage Cafe", "10:30 AM", Icons.Default.LocalCafe),
            RecentStopItem("Ekambrakanan Park Gate", "1:15 PM",  Icons.Default.Park),
            RecentStopItem("Mukteshvara Temple Gate", "3:00 PM", Icons.Default.Place)
        )
    }

    LaunchedEffect(walkPathPoints, mapViewRef.value) {
        val map = mapViewRef.value ?: return@LaunchedEffect
        if (walkPathPoints.size < 2) return@LaunchedEffect
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

    LaunchedEffect(userLocation, mapViewRef.value) {
        val map = mapViewRef.value ?: return@LaunchedEffect
        userLocation?.let { geo ->
            if (userMarker == null) {
                val m = Marker(map).apply {
                    title = "You"
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    icon  = createUserDot(context)
                    infoWindow = null
                }
                map.overlays.add(m)
                userMarker = m
            }
            userMarker?.position = geo
            if (!hasZoomedToUser) {
                map.controller.setZoom(15.5)
                map.controller.setCenter(geo)
                hasZoomedToUser = true
            } else if (isFollowingUser) {
                map.controller.animateTo(geo)
            }
            map.invalidate()
        }
    }

    LaunchedEffect(monuments, selectedMonument, mapViewRef.value) {
        val map = mapViewRef.value ?: return@LaunchedEffect
        val newSelectedIds = mutableSetOf<String>()
        selectedMonument?.let { newSelectedIds.add(it.id) }

        monuments.forEach { item ->
            val isSel    = selectedMonument?.id == item.id
            val existing = monumentMarkers[item.id]

            var existingGeofence = geofencePolygons[item.id]
            if (existingGeofence == null) {
                val circlePoly = Polygon(map).apply {
                    points = Polygon.pointsAsCircle(item.geoPoint, geofenceRadiusMeters)
                    fillPaint.color   = AndroidColor.parseColor("#4010B981")
                    outlinePaint.color = AndroidColor.parseColor("#FF10B981")
                    outlinePaint.strokeWidth = 6.0f
                }
                map.overlays.add(circlePoly)
                geofencePolygons[item.id] = circlePoly
            } else {
                existingGeofence.points = Polygon.pointsAsCircle(item.geoPoint, geofenceRadiusMeters)
            }

            if (existing == null) {
                val marker = Marker(map).apply {
                    position   = item.geoPoint
                    title      = item.name
                    icon       = createMonumentMarker(context, isSel, formatDistance(item.distanceMeters))
                    infoWindow = null
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
                val wasSelected   = selectedMarkerIds.value.contains(item.id)
                if (isSel != wasSelected) {
                    existing.icon = createMonumentMarker(context, isSel, formatDistance(item.distanceMeters))
                }
            }
        }
        
        // Partner Hotels rendered dynamically from live API / Overpass repository

        selectedMarkerIds.value = newSelectedIds
        map.invalidate()
    }

    LaunchedEffect(selectedMonument, userLocation, mapViewRef.value) {
        val map = mapViewRef.value ?: return@LaunchedEffect
        routePolyline.value?.let { map.overlays.remove(it) }
        routePolyline.value = null

        val mon     = selectedMonument ?: return@LaunchedEffect
        val userLoc = userLocation     ?: return@LaunchedEffect

        val pts = fetchRoute(
            userLoc.latitude, userLoc.longitude,
            mon.geoPoint.latitude, mon.geoPoint.longitude
        )
        if (pts.size >= 2) {
            val poly = Polyline(map).apply {
                outlinePaint.color       = AndroidColor.parseColor("#CC1A73E8")
                outlinePaint.strokeWidth = 14f
                outlinePaint.strokeCap   = Paint.Cap.ROUND
                outlinePaint.strokeJoin  = Paint.Join.ROUND
                outlinePaint.pathEffect  = android.graphics.DashPathEffect(floatArrayOf(30f, 15f), 0f)
                setPoints(pts)
            }
            map.overlays.add(poly)
            routePolyline.value = poly
            map.invalidate()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF090D16))) {

        AndroidView(
            factory = { ctx ->
                val prefs = ctx.getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
                val cfg = Configuration.getInstance()
                cfg.load(ctx, prefs)
                cfg.userAgentValue = "${ctx.packageName}/1.0 (MonumentQuest)"

                PerspectiveMapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setBackgroundColor(AndroidColor.parseColor("#DDE7E8"))
                    setMultiTouchControls(true)
                    setBuiltInZoomControls(false)
                    setUseDataConnection(true)
                    controller.setZoom(15.0)
                    controller.setCenter(GeoPoint(20.5937, 78.9629))
                    mapViewInstance = this
                    mapViewRef.value = this
                }
            },
            update = { mv ->
                if (mv.tileProvider.tileSource == null) {
                    mv.setTileSource(TileSourceFactory.MAPNIK)
                    mv.invalidate()
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(12.dp, RoundedCornerShape(20.dp))
                    .clip(
                        if (searchResults.isNotEmpty())
                            RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                        else RoundedCornerShape(20.dp)
                    )
                    .background(Surface1.copy(alpha = 0.95f))
                    .border(1.dp, BorderSubtle, if (searchResults.isNotEmpty()) RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp) else RoundedCornerShape(20.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .padding(start = 6.dp)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSearching) Gold.copy(alpha = 0.15f)
                            else Color.Transparent
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSearching) {
                        CircularProgressIndicator(
                            modifier    = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color       = Gold
                        )
                    } else {
                        Icon(
                            Icons.Default.Search, null,
                            tint     = Gold,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    if (searchQuery.isEmpty()) {
                        Text(
                            "Search monuments...",
                            color    = TextSecondary,
                            fontSize = 13.5.sp
                        )
                    }
                    BasicTextField(
                        value         = searchQuery,
                        onValueChange = { q ->
                            searchQuery = q
                            when {
                                q.length >= 3 -> viewModel.searchPlaces(q)
                                q.isEmpty()   -> viewModel.clearSearch()
                            }
                        },
                        singleLine = true,
                        textStyle  = TextStyle(fontSize = 13.5.sp, color = TextPrimary, fontWeight = FontWeight.Medium),
                        modifier   = Modifier.fillMaxWidth()
                    )
                }

                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick  = { searchQuery = ""; viewModel.clearSearch() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Close, null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                    }
                }

                Row(
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .shadow(4.dp, RoundedCornerShape(14.dp))
                        .clip(RoundedCornerShape(14.dp))
                        .background(GoldLinearGradient)
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(Icons.Default.Star, null, tint = Bg, modifier = Modifier.size(15.dp))
                    Text(
                        "${userProfile.xp} XP",
                        color      = Bg,
                        fontSize   = 12.5.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isCapturableInRange) Color(0xDC064E3B) else Surface2.copy(alpha = 0.92f))
                    .border(1.dp, if (isCapturableInRange) GreenAccent else Gold.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
                    .align(Alignment.Start),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(if (isCapturableInRange) GreenAccent else Gold)
                )
                Text(
                    text = if (isCapturableInRange) "GREEN ZONE: Instant Verification (+500 XP)" else "SPECIAL DISCOVERY MODE: Photo Review (+250 XP)",
                    color = if (isCapturableInRange) GreenAccent else Gold,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }

            if (searchResults.isNotEmpty()) {
                Surface(
                    modifier        = Modifier.fillMaxWidth(),
                    shape           = RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp),
                    color           = Surface1,
                    border          = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                    shadowElevation = 12.dp
                ) {
                    Column {
                        searchResults.take(6).forEachIndexed { idx, result ->
                            if (idx > 0) HorizontalDivider(color = BorderSubtle, thickness = 0.5.dp)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        mapViewRef.value?.controller?.animateTo(result.geoPoint)
                                        mapViewRef.value?.controller?.setZoom(17.0)
                                        searchQuery = result.name
                                        viewModel.clearSearch()
                                    }
                                    .padding(horizontal = 16.dp, vertical = 11.dp),
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(GoldTint),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Place, null,
                                        tint     = Gold,
                                        modifier = Modifier.size(16.dp))
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(result.name, fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary, maxLines = 1)
                                    if (result.distanceMeters > 0) {
                                        Text(formatDistance(result.distanceMeters),
                                            fontSize = 11.sp, color = TextSecondary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 14.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MapControlFab(
                onClick = { mapViewInstance?.controller?.zoomIn() },
                bgColor = Surface1
            ) {
                Icon(Icons.Default.Add, null, tint = TextPrimary, modifier = Modifier.size(22.dp))
            }
            MapControlFab(
                onClick = { mapViewInstance?.controller?.zoomOut() },
                bgColor = Surface1
            ) {
                Icon(Icons.Default.Remove, null, tint = TextPrimary, modifier = Modifier.size(22.dp))
            }
            MapControlFab(
                onClick = {
                    isFollowingUser = true
                    userLocation?.let { mapViewInstance?.controller?.animateTo(it) }
                },
                bgColor = Color(0xFF1A73E8)
            ) {
                Icon(Icons.Default.MyLocation, null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
        }

        AnimatedVisibility(
            visible  = selectedMonument != null,
            enter    = slideInVertically(tween(300)) { it } + fadeIn(tween(200)),
            exit     = slideOutVertically(tween(240)) { it } + fadeOut(tween(160)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = 14.dp, end = 14.dp, bottom = cardBottomPad)
        ) {
            selectedMonument?.let { mon ->
                MonumentCard(
                    monument   = mon,
                    isCapturableInRange = isCapturableInRange,
                    onDismiss  = { selectedMonument = null },
                    onExplore  = { onNavigateToNarrator(mon.id) },
                    onLogVisit = attemptCapture
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(start = 14.dp, end = 14.dp, bottom = 4.dp)
        ) {
            Surface(
                modifier        = Modifier.fillMaxWidth(),
                shape           = RoundedCornerShape(22.dp),
                color           = Surface1.copy(alpha = 0.96f),
                border          = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                shadowElevation = 16.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(Border)
                            .align(Alignment.CenterHorizontally)
                            .clickable(
                                indication        = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) { isSheetExpanded = !isSheetExpanded }
                    )

                    Spacer(Modifier.height(10.dp))

                    Text(
                        "GAME QUEST PROGRESS",
                        fontSize      = 10.sp,
                        fontWeight    = FontWeight.Bold,
                        color         = Gold,
                        letterSpacing = 1.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DarkStat("Points", "${userProfile.xp} XP", Icons.Default.WorkspacePremium, Modifier.weight(1f))
                        DarkStat("Sites Captured", "${coverageStats.structuresVisitedCount}", Icons.Default.Place, Modifier.weight(1f))
                        DarkStat("Streak", "${userProfile.streakDays} Days", Icons.Default.LocalFireDepartment, Modifier.weight(1f))
                    }

                    Spacer(Modifier.height(12.dp))

                    Text(
                        "EXPLORATION LOGISTICS",
                        fontSize      = 10.sp,
                        fontWeight    = FontWeight.Bold,
                        color         = BlueAccent,
                        letterSpacing = 1.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DarkStat("Distance", coverageStats.totalTrackFormatted, Icons.Default.DirectionsWalk, Modifier.weight(1f))
                        DarkStat("Area Unlocked", coverageStats.coveredAreaFormatted, Icons.Default.Public, Modifier.weight(1f))
                    }

                    AnimatedVisibility(
                        visible = isSheetExpanded,
                        enter   = fadeIn(tween(180)) + slideInVertically(tween(240)) { it },
                        exit    = fadeOut(tween(140)) + slideOutVertically(tween(200)) { it }
                    ) {
                        Column {
                            Spacer(Modifier.height(14.dp))
                            HorizontalDivider(color = BorderSubtle)
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "Recent Stops",
                                fontWeight = FontWeight.SemiBold,
                                color      = TextPrimary,
                                fontSize   = 13.sp
                            )
                            Spacer(Modifier.height(10.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                recentStops.forEach { stop ->
                                    Row(
                                        modifier              = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment     = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment     = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(RoundedCornerShape(9.dp))
                                                    .background(Surface2),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    stop.icon, null,
                                                    tint     = Gold,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                            Text(
                                                stop.name,
                                                fontWeight = FontWeight.Medium,
                                                color      = TextPrimary,
                                                fontSize   = 13.sp
                                            )
                                        }
                                        Text(
                                            stop.time,
                                            color    = TextSecondary,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .clickable(
                                indication        = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) { isSheetExpanded = !isSheetExpanded },
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            if (isSheetExpanded) "Show less" else "Recent stops",
                            color    = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-14).dp, y = (-28).dp)
            ) {
                if (isCapturableInRange) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(GreenAccent.copy(alpha = pulseGlowAlpha))
                            .align(Alignment.Center)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .scale(if (isCapturableInRange) pulseScale else 1.0f)
                        .shadow(12.dp, CircleShape)
                        .clip(CircleShape)
                        .background(
                            if (isCapturableInRange)
                                EmeraldLinearGradient
                            else
                                GoldLinearGradient
                        )
                        .clickable { attemptCapture() }
                        .align(Alignment.Center),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.AddAPhoto,
                        contentDescription = "Log Visit",
                        tint               = Bg,
                        modifier           = Modifier.size(26.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MonumentCard(
    monument:  MapMonumentItem,
    isCapturableInRange: Boolean,
    onDismiss: () -> Unit,
    onExplore: () -> Unit,
    onLogVisit: () -> Unit = {}
) {
    val context = LocalContext.current
    Surface(
        shape           = RoundedCornerShape(20.dp),
        color           = Color(0xFF0F172A),
        border          = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
        shadowElevation = 20.dp,
        modifier        = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        monument.name,
                        fontWeight = FontWeight.Bold,
                        color      = Color(0xFFF8FAFC),
                        fontSize   = 16.sp
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        monument.category.lowercase().replaceFirstChar { it.uppercase() },
                        color    = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E293B))
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Close, null, tint = Color(0xFF94A3B8), modifier = Modifier.size(14.dp))
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF2E1C00))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.Star, null, tint = Color(0xFFF0A500), modifier = Modifier.size(12.dp))
                    Text(
                        "${monument.points} XP",
                        color      = Color(0xFFF0A500),
                        fontSize   = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isCapturableInRange) Color(0xFF064E3B) else Color(0xFF0D2240))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        formatDistance(monument.distanceMeters),
                        color      = if (isCapturableInRange) Color(0xFF86EFAC) else Color(0xFF3B82F6),
                        fontSize   = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            // Nearby Hotel Pass Action Button (SIH26202 Tourism Hub)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF1E293B))
                    .clickable {
                        Toast.makeText(context, "🏨 Nearby Hotels near " + monument.name + ": 3 Hotels with 20% OFF Pass!", Toast.LENGTH_LONG).show()
                    }
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Hotel, null, tint = Color(0xFFF0A500), modifier = Modifier.size(15.dp))
                    Text("Hotels Near " + monument.name, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Text("View Passes >", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick  = onExplore,
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape    = RoundedCornerShape(12.dp),
                    border   = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFCBD5E1))
                ) {
                    Text("Narrator", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
                Button(
                    onClick  = onLogVisit,
                    modifier = Modifier.weight(1.4f).height(44.dp),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = if (isCapturableInRange) Color(0xFF10B981) else Color(0xFFF0A500)
                    )
                ) {
                    Icon(Icons.Default.AddAPhoto, null, modifier = Modifier.size(15.dp), tint = Color.White)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (isCapturableInRange) "Capture Live" else "Submit Discovery",
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color      = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun DarkStat(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1E293B))
            .padding(vertical = 8.dp, horizontal = 10.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, null, tint = Color(0xFF94A3B8), modifier = Modifier.size(12.dp))
            Text(label, color = Color(0xFF94A3B8), fontSize = 10.5.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.height(3.dp))
        Text(value, fontWeight = FontWeight.ExtraBold, color = Color(0xFFF8FAFC), fontSize = 14.sp)
    }
}

@Composable
private fun MapControlFab(
    onClick: () -> Unit,
    bgColor: Color,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .shadow(6.dp, RoundedCornerShape(13.dp))
            .clip(RoundedCornerShape(13.dp))
            .background(bgColor)
            .clickable(
                indication        = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) { content() }
}

private fun formatDistance(meters: Int): String = when {
    meters <= 0   -> "Nearby"
    meters < 1000 -> "$meters m away"
    else          -> String.format("%.1f km away", meters / 1000.0)
}
