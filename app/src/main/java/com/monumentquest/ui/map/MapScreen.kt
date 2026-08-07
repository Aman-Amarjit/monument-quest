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
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Satellite
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.monumentquest.data.model.MapMonumentItem
import com.monumentquest.ui.theme.*
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.util.MapTileIndex
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

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

private val CustomDarkHeritageTileSource = object : OnlineTileSourceBase(
    "CustomDarkHeritage",
    0, 19, 256, ".png",
    arrayOf("https://basemaps.cartocdn.com/rastertiles/dark_all/")
) {
    override fun getTileURLString(pMapTileIndex: Long): String {
        val zoom = MapTileIndex.getZoom(pMapTileIndex)
        val x = MapTileIndex.getX(pMapTileIndex)
        val y = MapTileIndex.getY(pMapTileIndex)
        return "https://basemaps.cartocdn.com/rastertiles/dark_all/$zoom/$x/$y.png"
    }
}

private fun createCustomMonumentMarker(context: Context, isSelected: Boolean): Drawable {
    val size = if (isSelected) 88 else 72
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    paint.color = if (isSelected) AndroidColor.parseColor("#FFFFD700") else AndroidColor.parseColor("#80FFD700")
    canvas.drawCircle(size / 2f, size / 2f, size / 2f - 2f, paint)

    paint.color = AndroidColor.parseColor("#10121A")
    canvas.drawCircle(size / 2f, size / 2f, size / 2f - 6f, paint)

    paint.color = AndroidColor.parseColor("#F3B61D")
    canvas.drawCircle(size / 2f, size / 2f, size / 2f - 14f, paint)

    return BitmapDrawable(context.resources, bitmap)
}

private fun createUserLocationMarker(context: Context, isGhost: Boolean): Drawable {
    val size = 76
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    paint.color = if (isGhost) AndroidColor.parseColor("#80FFB703") else AndroidColor.parseColor("#804ECCA3")
    canvas.drawCircle(size / 2f, size / 2f, size / 2f - 2f, paint)

    paint.color = if (isGhost) AndroidColor.parseColor("#FFB703") else AndroidColor.parseColor("#4ECCA3")
    canvas.drawCircle(size / 2f, size / 2f, size / 4f, paint)

    return BitmapDrawable(context.resources, bitmap)
}

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
    val pace                by viewModel.currentPace.collectAsState()
    val bearing             by viewModel.currentBearing.collectAsState()
    val movementStatus      by viewModel.movementStatus.collectAsState()

    var selectedMonument by remember { mutableStateOf<MapMonumentItem?>(null) }
    var mapViewInstance  by remember { mutableStateOf<MapView?>(null) }
    var userMarker       by remember { mutableStateOf<Marker?>(null) }
    var radiusCircle     by remember { mutableStateOf<Polygon?>(null) }
    var isFollowingUser  by remember { mutableStateOf(true) }
    var isSatelliteMode  by remember { mutableStateOf(true) }
    var isGhostMode      by remember { mutableStateOf(false) }

    // Fun Gamification States
    var showRadarPing     by remember { mutableStateOf(false) }
    var userXP            by remember { mutableStateOf(1250) }
    var streakDays        by remember { mutableStateOf(7) }
    var showQuestModal    by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    LaunchedEffect(monuments) {
        if (selectedMonument == null && monuments.isNotEmpty()) {
            selectedMonument = monuments.first()
        }
    }

    LaunchedEffect(isSatelliteMode) {
        mapViewInstance?.let { map ->
            if (isSatelliteMode) {
                map.setTileSource(PhotorealisticSatelliteTileSource)
            } else {
                map.setTileSource(CustomDarkHeritageTileSource)
            }
            map.invalidate()
        }
    }

    LaunchedEffect(userLocation, bearing, isGhostMode, monuments, selectedMonument, showRadarPing) {
        mapViewInstance?.let { map ->
            userLocation?.let { currentGeo ->
                if (userMarker == null) {
                    val m = Marker(map).apply {
                        title = if (isGhostMode) "Ghost Mode" else "Live Position"
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        icon = createUserLocationMarker(context, isGhostMode)
                    }
                    map.overlays.add(m)
                    userMarker = m
                }
                userMarker?.position = currentGeo
                userMarker?.rotation = bearing
                userMarker?.icon = createUserLocationMarker(context, isGhostMode)
                userMarker?.alpha = if (isGhostMode) 0.5f else 1.0f

                val circleRadius = if (showRadarPing) 180.0 else 45.0
                if (radiusCircle == null) {
                    val circlePoints = Polygon.pointsAsCircle(currentGeo, circleRadius)
                    val p = Polygon().apply {
                        points = circlePoints
                        fillPaint.color = AndroidColor.parseColor("#25F3B61D")
                        outlinePaint.color = AndroidColor.parseColor("#A0F3B61D")
                        outlinePaint.strokeWidth = 3f
                    }
                    map.overlays.add(0, p)
                    radiusCircle = p
                } else {
                    radiusCircle?.points = Polygon.pointsAsCircle(currentGeo, circleRadius)
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
            .background(ObsidianBlack)
    ) {
        // ── 1. Map Canvas ───────────────────────────────────────────────────
        AndroidView(
            factory = { ctx ->
                val config = Configuration.getInstance()
                config.load(ctx, ctx.getSharedPreferences("osmdroid_pref", Context.MODE_PRIVATE))
                config.userAgentValue = ctx.packageName

                MapView(ctx).apply {
                    setTileSource(if (isSatelliteMode) PhotorealisticSatelliteTileSource else CustomDarkHeritageTileSource)
                    setMultiTouchControls(true)
                    controller.setZoom(18.0)

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
                        icon = createCustomMonumentMarker(context, isSel)
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

        // ── 2. Top Fun Gamification Bar (Streak, XP, Quests) ────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = GlassSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorderGradient),
                shadowElevation = 14.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // XP & Streak Badge
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(GoldBright.copy(alpha = 0.2f))
                                .border(1.dp, GoldBright, RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.AutoAwesome, null, tint = GoldBright, modifier = Modifier.size(13.dp))
                                Text("$userXP XP", fontWeight = FontWeight.Black, color = GoldBright, fontSize = 12.sp)
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(EmberMid.copy(alpha = 0.2f))
                                .border(1.dp, EmberMid, RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("🔥 $streakDays Days", fontWeight = FontWeight.Black, color = EmberGlow, fontSize = 12.sp)
                        }
                    }

                    // Daily Quest CTA Chip
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(ForestMid)
                            .clickable { showQuestModal = true }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.EmojiEvents, null, tint = CreamWhite, modifier = Modifier.size(13.dp))
                            Text("Daily Quests", fontWeight = FontWeight.Bold, color = CreamWhite, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // ── 3. Pill Utility Toolbar (with Fun AR Radar Scan) ────────────────
        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 110.dp, end = 16.dp),
            shape = RoundedCornerShape(24.dp),
            color = GlassSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, SubtleGray),
            shadowElevation = 14.dp
        ) {
            Column(
                modifier = Modifier.padding(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Interactive Radar Scan
                IconButton(
                    onClick = {
                        showRadarPing = !showRadarPing
                        if (showRadarPing) userXP += 50
                    },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Radar,
                        contentDescription = "Radar Ping",
                        tint = if (showRadarPing) GoldBright else MutedGray,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = { isSatelliteMode = !isSatelliteMode },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = if (isSatelliteMode) Icons.Default.Satellite else Icons.Default.Layers,
                        contentDescription = "Tile Layer",
                        tint = GoldBright,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = { isFollowingUser = !isFollowingUser },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = if (isFollowingUser) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = "Follow Lock",
                        tint = if (isFollowingUser) ForestMint else MutedGray,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = {
                        isFollowingUser = true
                        userLocation?.let { mapViewInstance?.controller?.animateTo(it) }
                    },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = "Re-center", tint = GoldBright, modifier = Modifier.size(18.dp))
                }
            }
        }

        // ── 4. Primary Capture FAB ───────────────────────────────────────────
        FloatingActionButton(
            onClick = onNavigateToCamera,
            containerColor = GoldBright,
            contentColor = ObsidianBlack,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(
                    end = 16.dp,
                    bottom = if (selectedMonument != null) 235.dp else 16.dp
                )
                .size(56.dp)
        ) {
            Icon(Icons.Default.AddAPhoto, contentDescription = "Capture Structure", modifier = Modifier.size(26.dp))
        }

        // ── 5. Selected Monument Sheet ──────────────────────────────────────
        AnimatedVisibility(
            visible = selectedMonument != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit  = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 4.dp)
        ) {
            selectedMonument?.let { monument ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp),
                    shape = RoundedCornerShape(26.dp),
                    color = GlassSurface,
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, GlassBorderGradient),
                    shadowElevation = 18.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(GoldBright.copy(alpha = 0.15f))
                                        .border(1.dp, GoldBright.copy(alpha = 0.5f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Explore, null, tint = GoldBright, modifier = Modifier.size(22.dp))
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = monument.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = CreamWhite,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = "${monument.locationName} · ${monument.category}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MutedGray,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            IconButton(onClick = { selectedMonument = null }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, null, tint = MutedGray, modifier = Modifier.size(16.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(ElevatedSurface)
                                    .padding(vertical = 10.dp, horizontal = 10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.Star, null, tint = GoldBright, modifier = Modifier.size(15.dp))
                                    Column {
                                        Text("${monument.points} XP", fontWeight = FontWeight.Bold, color = GoldBright, fontSize = 13.sp)
                                        Text("Reward Value", style = MaterialTheme.typography.labelSmall, color = MutedGray, fontSize = 9.sp)
                                    }
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(ElevatedSurface)
                                    .padding(vertical = 10.dp, horizontal = 10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.Navigation, null, tint = ForestMint, modifier = Modifier.size(15.dp))
                                    Column {
                                        Text(
                                            text = if (monument.distanceMeters > 0) "${monument.distanceMeters}m" else "Nearby",
                                            fontWeight = FontWeight.Bold,
                                            color = ForestMint,
                                            fontSize = 13.sp
                                        )
                                        Text("Live Distance", style = MaterialTheme.typography.labelSmall, color = MutedGray, fontSize = 9.sp)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onNavigateToNarrator(monument.name) },
                                shape = RoundedCornerShape(14.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, SubtleGray),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp)
                            ) {
                                Icon(Icons.Default.ChatBubble, null, tint = GoldBright, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Talk to Temple", color = CreamWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = onNavigateToCamera,
                                colors = ButtonDefaults.buttonColors(containerColor = GoldBright, contentColor = ObsidianBlack),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp)
                            ) {
                                Icon(Icons.Default.AddAPhoto, null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Log Visit", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Daily Quest Modal
        if (showQuestModal) {
            AlertDialog(
                onDismissRequest = { showQuestModal = false },
                containerColor = CardSurface,
                title = {
                    Text("🎯 Daily Expedition Quests", fontWeight = FontWeight.Black, color = GoldBright)
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        QuestRow("Walk 500 meters along a heritage trail", "+150 XP", 0.7f)
                        QuestRow("Scan 1 ancient monument using AR Camera", "+300 XP", 1.0f)
                        QuestRow("Talk with an AI Temple Narrator", "+100 XP", 1.0f)
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            userXP += 400
                            showQuestModal = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldBright, contentColor = ObsidianBlack)
                    ) {
                        Text("Claim Completed Rewards", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    }
}

@Composable
private fun QuestRow(title: String, reward: String, progress: Float) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, style = MaterialTheme.typography.bodySmall, color = CreamWhite, fontWeight = FontWeight.Bold)
            Text(reward, style = MaterialTheme.typography.labelSmall, color = GoldBright, fontWeight = FontWeight.Black)
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = if (progress >= 1.0f) SuccessGreen else GoldBright,
            trackColor = ElevatedSurface
        )
    }
}
