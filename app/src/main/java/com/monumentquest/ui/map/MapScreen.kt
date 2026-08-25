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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

// ââ Tile source â CartoDB Voyager (colorful, matches 3D reference) ââââââââââââ
// We define a fresh object each time to bypass OSMDroid's name-based tile cache.
private fun makeCartoVoyagerSource() = object : OnlineTileSourceBase(
    "CartoVoyagerMQ", 0, 20, 256, ".png",
    arrayOf(
        "https://a.basemaps.cartocdn.com/rastertiles/voyager/",
        "https://b.basemaps.cartocdn.com/rastertiles/voyager/",
        "https://c.basemaps.cartocdn.com/rastertiles/voyager/",
        "https://d.basemaps.cartocdn.com/rastertiles/voyager/"
    )
) {
    override fun getTileURLString(pMapTileIndex: Long): String {
        val zoom = MapTileIndex.getZoom(pMapTileIndex)
        val x    = MapTileIndex.getX(pMapTileIndex)
        val y    = MapTileIndex.getY(pMapTileIndex)
        return "${baseUrl}$zoom/$x/$y.png"
    }
}

// ââ OSRM Routing ââââââââââââââââââââââââââââââââââââââââââââââââââââââââââââââ
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

// ââ Marker helpers ââââââââââââââââââââââââââââââââââââââââââââââââââââââââââââ

private fun createMonumentMarker(context: Context, isSelected: Boolean, distanceText: String): Drawable {
    val pinSize = if (isSelected) 56 else 44
    val textH   = if (distanceText.isNotBlank()) 32 else 0
    val pad     = if (distanceText.isNotBlank()) 6 else 0
    val totalW  = maxOf(pinSize, 140)
    val totalH  = pinSize + textH + pad

    val bitmap = Bitmap.createBitmap(totalW, totalH, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint  = Paint(Paint.ANTI_ALIAS_FLAG)
    val cx     = totalW / 2f
    val cy     = pinSize / 2f

    // Outer glow
    paint.color = AndroidColor.parseColor("#40F0A500")
    canvas.drawCircle(cx, cy, cy - 1f, paint)
    // White ring
    paint.color = AndroidColor.WHITE
    canvas.drawCircle(cx, cy, cy - 4f, paint)
    // Gold fill
    paint.color = AndroidColor.parseColor("#F0A500")
    canvas.drawCircle(cx, cy, cy - 8f, paint)
    // Inner white dot
    paint.color = AndroidColor.WHITE
    canvas.drawCircle(cx, cy, if (isSelected) 8f else 6f, paint)

    if (distanceText.isNotBlank()) {
        val labelTop = (pinSize + pad).toFloat()
        paint.color = AndroidColor.parseColor("#DD0F172A")
        canvas.drawRoundRect(
            android.graphics.RectF(cx - 56f, labelTop, cx + 56f, labelTop + 28f),
            10f, 10f, paint
        )
        paint.color          = AndroidColor.WHITE
        paint.textSize       = 19f
        paint.textAlign      = Paint.Align.CENTER
        paint.isFakeBoldText = true
        canvas.drawText(distanceText, cx, labelTop + 21f, paint)
    }
    return BitmapDrawable(context.resources, bitmap)
}

private fun createUserDot(context: Context): Drawable {
    val size   = 40
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint  = Paint(Paint.ANTI_ALIAS_FLAG)
    val cx     = size / 2f

    paint.color = AndroidColor.parseColor("#301A73E8")
    canvas.drawCircle(cx, cx, cx, paint)
    paint.color = AndroidColor.parseColor("#801A73E8")
    canvas.drawCircle(cx, cx, cx - 4f, paint)
    paint.color = AndroidColor.WHITE
    canvas.drawCircle(cx, cx, cx - 8f, paint)
    paint.color = AndroidColor.parseColor("#1A73E8")
    canvas.drawCircle(cx, cx, cx - 11f, paint)
    paint.color = AndroidColor.WHITE
    canvas.drawCircle(cx, cx, 5f, paint)
    return BitmapDrawable(context.resources, bitmap)
}

data class RecentStopItem(
    val name: String,
    val time: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

// ââ Screen ââââââââââââââââââââââââââââââââââââââââââââââââââââââââââââââââââââ

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
    val tacticalGeometry by viewModel.tacticalGeometry.collectAsState()
    val searchResults  by viewModel.searchResults.collectAsState()
    val isSearching    by viewModel.isSearching.collectAsState()

    var selectedMonument  by remember { mutableStateOf<MapMonumentItem?>(null) }
    var mapViewInstance   by remember { mutableStateOf<MapView?>(null) }
    var userMarker        by remember { mutableStateOf<Marker?>(null) }
    val monumentMarkers   = remember { mutableMapOf<String, Marker>() }
    val selectedMarkerIds = remember { mutableStateOf<Set<String>>(emptySet()) }
    val walkPolyline      = remember { mutableStateOf<Polyline?>(null) }
    val routePolyline     = remember { mutableStateOf<Polyline?>(null) }
    val mapViewRef        = remember { mutableStateOf<MapView?>(null) }
    val isometricOverlay  = remember { Isometric3DOverlay() }

    var isFollowingUser  by remember { mutableStateOf(true) }
    var searchQuery      by remember { mutableStateOf("") }
    var isSheetExpanded  by remember { mutableStateOf(false) }
    var hasZoomedToUser  by remember { mutableStateOf(false) }
    var isAerialView     by remember { mutableStateOf(true) }

    val sheetBottomPadding: Dp by animateDpAsState(
        targetValue   = if (isSheetExpanded) 290.dp else 148.dp,
        animationSpec = tween(280), label = "sheetPad"
    )

    val recentStops = remember {
        listOf(
            RecentStopItem("Old Town Heritage Cafe", "10:30 AM", Icons.Default.LocalCafe),
            RecentStopItem("Ekambrakanan Park Gate", "1:15 PM",  Icons.Default.Park),
            RecentStopItem("Mukteshvara Temple Gate","3:00 PM",  Icons.Default.Place)
        )
    }

    // ââ Effects âââââââââââââââââââââââââââââââââââââââââââââââââââââââââââââââ

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
                // A wider starting frame feels like the reference's city-scale
                // aerial view, while still leaving enough detail for monuments.
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
        selectedMarkerIds.value = newSelectedIds
        map.invalidate()
    }

    // Paint the fetched OSM blueprint above the raster tiles. This is the
    // layer that turns the ordinary map into the grey-roof / gold-road city
    // model shown in the reference image.
    LaunchedEffect(tacticalGeometry, mapViewRef.value, isAerialView) {
        val map = mapViewRef.value ?: return@LaunchedEffect
        isometricOverlay.geometry = tacticalGeometry
        isometricOverlay.enabled = isAerialView
        if (!map.overlays.contains(isometricOverlay)) {
            map.overlays.add(minOf(1, map.overlays.size), isometricOverlay)
        }
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
            map.overlays.add(minOf(2, map.overlays.size), poly)
            routePolyline.value = poly
            map.invalidate()
        }
    }

    // ââ Root container ââââââââââââââââââââââââââââââââââââââââââââââââââââââââ
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0A0A12))) {

        // ââ Map âââââââââââââââââââââââââââââââââââââââââââââââââââââââââââââââ
        AndroidView(
            factory = { ctx ->
                // Keep OSMDroid's tile cache between launches. Clearing these
                // prefs here made the map re-download tiles every time.
                val prefs = ctx.getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
                val cfg = Configuration.getInstance()
                cfg.load(ctx, prefs)
                cfg.userAgentValue = "${ctx.packageName}/1.0 (MonumentQuest)"

                MapView(ctx).apply {
                    // MAPNIK is a stable, detailed fallback base. The custom
                    // overlay above it supplies the raised roofs and gold
                    // arterials, so the map is still styled like the reference
                    // without depending on one raster tile CDN.
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    // The reference is a diagonal, low aerial view rather than
                    // a north-up street map. OSMDroid has no pitch camera, so a
                    // modest bearing gives the same visual direction without
                    // breaking marker placement or gestures.
                    controller.setZoom(15.0)
                    controller.setCenter(GeoPoint(20.5937, 78.9629))
                    // OSMDroid rotation clips the rectangular tile surface and
                    // exposes a black triangle at the corners. Keep the camera
                    // north-up; the overlay provides the 3D depth safely.
                    mapOrientation = 0f
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

        // ââ TOP: Search bar âââââââââââââââââââââââââââââââââââââââââââââââââââ
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            // Glass search pill
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(
                        if (searchResults.isNotEmpty())
                            RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)
                        else RoundedCornerShape(18.dp)
                    )
                    .background(Color(0xF2FFFFFF))
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .padding(start = 10.dp)
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSearching) Color(0xFFF0A500).copy(alpha = 0.15f)
                            else Color.Transparent
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSearching) {
                        CircularProgressIndicator(
                            modifier    = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color       = Color(0xFFF0A500)
                        )
                    } else {
                        Icon(
                            Icons.Default.Search, null,
                            tint     = Color(0xFF8899BB),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 10.dp, vertical = 12.dp)
                ) {
                    if (searchQuery.isEmpty()) {
                        Text(
                            "Search monuments or placesâ¦",
                            color    = Color(0xFFAAB8CC),
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
                        textStyle  = TextStyle(fontSize = 13.5.sp, color = Color(0xFF1E293B)),
                        modifier   = Modifier.fillMaxWidth()
                    )
                }

                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = ""; viewModel.clearSearch() },
                        modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.Close, null,
                            tint = Color(0xFF8899BB), modifier = Modifier.size(15.dp))
                    }
                }

                // Explore badge
                Row(
                    modifier = Modifier
                        .padding(end = 6.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.horizontalGradient(listOf(Color(0xFFF0A500), Color(0xFFFFCA28)))
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(Icons.Default.Explore, null,
                        tint = Color.White, modifier = Modifier.size(14.dp))
                    Text("Explore", color = Color.White, fontSize = 12.sp,
                        fontWeight = FontWeight.Bold)
                }
            }

            // Search results dropdown
            if (searchResults.isNotEmpty()) {
                Surface(
                    modifier        = Modifier.fillMaxWidth(),
                    shape           = RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp),
                    color           = Color.White,
                    shadowElevation = 12.dp
                ) {
                    Column {
                        searchResults.take(6).forEachIndexed { idx, result ->
                            if (idx > 0) HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 0.5.dp)
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
                                        .background(Color(0xFFFEF3C7)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Place, null,
                                        tint     = Color(0xFFF0A500),
                                        modifier = Modifier.size(16.dp))
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(result.name, fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF1E293B), maxLines = 1)
                                    if (result.distanceMeters > 0) {
                                        Text(formatDistance(result.distanceMeters),
                                            fontSize = 11.sp, color = Color(0xFF94A3B8))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ââ RIGHT: Zoom + locate FABs âââââââââââââââââââââââââââââââââââââââââ
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp, bottom = 60.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MapControlFab(
                onClick = {
                    isAerialView = !isAerialView
                    isometricOverlay.enabled = isAerialView
                    mapViewInstance?.mapOrientation = if (isAerialView) 22f else 0f
                    mapViewInstance?.invalidate()
                },
                bgColor = if (isAerialView) Color(0xFF172033) else Color(0xF5FFFFFF)
            ) {
                Text(
                    if (isAerialView) "3D" else "2D",
                    color = if (isAerialView) Color(0xFFFFC857) else Color(0xFF334155),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            MapControlFab(
                onClick = { mapViewInstance?.controller?.zoomIn() },
                bgColor = Color(0xF5FFFFFF)
            ) {
                Icon(Icons.Default.Add, null,
                    tint = Color(0xFF334155), modifier = Modifier.size(20.dp))
            }
            MapControlFab(
                onClick = { mapViewInstance?.controller?.zoomOut() },
                bgColor = Color(0xF5FFFFFF)
            ) {
                Icon(Icons.Default.Remove, null,
                    tint = Color(0xFF334155), modifier = Modifier.size(20.dp))
            }
            MapControlFab(
                onClick = {
                    isFollowingUser = true
                    userLocation?.let { mapViewInstance?.controller?.animateTo(it) }
                },
                bgColor = Color(0xFF1A73E8)
            ) {
                Icon(Icons.Default.MyLocation, null,
                    tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }

        // ââ Monument info card ââââââââââââââââââââââââââââââââââââââââââââââââ
        AnimatedVisibility(
            visible  = selectedMonument != null,
            enter    = slideInVertically(tween(300)) { it } + fadeIn(tween(200)),
            exit     = slideOutVertically(tween(240)) { it } + fadeOut(tween(160)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = 14.dp, end = 14.dp, bottom = sheetBottomPadding + 8.dp)
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

        // ââ Bottom sheet ââââââââââââââââââââââââââââââââââââââââââââââââââââââ
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
            // Sheet body
            Surface(
                modifier        = Modifier.fillMaxWidth(),
                shape           = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color           = Color(0xFF0F0F1A),
                shadowElevation = 20.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 18.dp, vertical = 14.dp)
                ) {
                    // Drag handle
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2A2A3A))
                            .align(Alignment.CenterHorizontally)
                            .clickable(
                                indication        = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) { isSheetExpanded = !isSheetExpanded }
                    )

                    Spacer(Modifier.height(14.dp))

                    // Stats row
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        DarkStat("Distance", coverageStats.totalTrackFormatted,         Modifier.weight(1f))
                        DarkStat("Area",     coverageStats.coveredAreaFormatted,         Modifier.weight(1f))
                        DarkStat("Sites",    "${coverageStats.structuresVisitedCount}",  Modifier.weight(1f))
                    }

                    // Expanded: recent stops
                    AnimatedVisibility(
                        visible = isSheetExpanded,
                        enter   = fadeIn(tween(180)) + slideInVertically(tween(240)) { it },
                        exit    = fadeOut(tween(140)) + slideOutVertically(tween(200)) { it }
                    ) {
                        Column {
                            Spacer(Modifier.height(14.dp))
                            HorizontalDivider(color = Color(0xFF1E1E2E))
                            Spacer(Modifier.height(12.dp))
                            Text("Recent Stops",
                                fontWeight = FontWeight.SemiBold,
                                color      = Color(0xFFF1F5F9),
                                fontSize   = 13.sp)
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
                                                    .background(Color(0xFF1A1A2A)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(stop.icon, null,
                                                    tint     = Color(0xFFF0A500),
                                                    modifier = Modifier.size(16.dp))
                                            }
                                            Text(stop.name,
                                                fontWeight = FontWeight.Medium,
                                                color      = Color(0xFFCBD5E1),
                                                fontSize   = 13.sp)
                                        }
                                        Text(stop.time,
                                            color    = Color(0xFF64748B),
                                            fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                            .clickable(
                                indication        = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) { isSheetExpanded = !isSheetExpanded },
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            if (isSheetExpanded) "Show less â²" else "Recent stops â¼",
                            color    = Color(0xFF475569),
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Camera FAB â anchored top-right of the sheet
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-16).dp, y = (-26).dp)
                    .size(58.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFFF0A500), Color(0xFFFFCA28))
                        )
                    )
                    .clickable { onNavigateToCamera() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.AddAPhoto, "Log Visit",
                    tint     = Color.White,
                    modifier = Modifier.size(24.dp))
            }
        }
    }
}

// ââ Sub-components ââââââââââââââââââââââââââââââââââââââââââââââââââââââââââââ

@Composable
private fun MonumentCard(
    monument:  MapMonumentItem,
    onDismiss: () -> Unit,
    onExplore: () -> Unit,
    onLogVisit: () -> Unit = {}
) {
    Surface(
        shape           = RoundedCornerShape(20.dp),
        color           = Color(0xFF0F0F1A),
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
                    Text(monument.name,
                        fontWeight = FontWeight.Bold,
                        color      = Color(0xFFF1F5F9),
                        fontSize   = 15.sp)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        monument.category.lowercase().replaceFirstChar { it.uppercase() },
                        color    = Color(0xFF64748B),
                        fontSize = 12.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1A1A2A))
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Close, null,
                        tint = Color(0xFF64748B), modifier = Modifier.size(14.dp))
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1A1200))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.Star, null,
                        tint = Color(0xFFF0A500), modifier = Modifier.size(12.dp))
                    Text("${monument.points} XP",
                        color = Color(0xFFF0A500), fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0D1A2E))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(formatDistance(monument.distanceMeters),
                        color = Color(0xFF3B82F6), fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick  = onExplore,
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape    = RoundedCornerShape(12.dp),
                    border   = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFCBD5E1))
                ) {
                    Text("Narrator", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
                Button(
                    onClick  = onLogVisit,
                    modifier = Modifier.weight(1.4f).height(44.dp),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFFF0A500))
                ) {
                    Icon(Icons.Default.AddAPhoto, null,
                        modifier = Modifier.size(15.dp), tint = Color.White)
                    Spacer(Modifier.width(6.dp))
                    Text("Log Visit", fontSize = 13.sp,
                        fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun DarkStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF161625))
            .padding(vertical = 10.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(value,
            fontWeight = FontWeight.Bold,
            color      = Color(0xFFF1F5F9),
            fontSize   = 15.sp)
        Text(label,
            color    = Color(0xFF475569),
            fontSize = 10.sp)
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
            .size(42.dp)
            .clip(RoundedCornerShape(12.dp))
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
