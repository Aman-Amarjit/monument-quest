package com.monumentquest.ui.profile

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.monumentquest.core.di.NetworkModule
import com.monumentquest.data.model.UserProfile
import com.monumentquest.ui.auth.AuthViewModel
import com.monumentquest.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ProfileScreen(
    onNavigateToJournalist: () -> Unit = {},
    onLogout: () -> Unit = {},
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val currentSession by authViewModel.currentSession.collectAsState()
    var liveProfile by remember { mutableStateOf(UserProfile()) }

    var customUsername by remember { mutableStateOf("Explorer Prime") }
    var customBio by remember { mutableStateOf("Odisha Heritage Explorer & Monument Discoverer") }
    var customAvatarUri by remember { mutableStateOf<Uri?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current

    // Launcher for picking profile picture
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            customAvatarUri = uri
        }
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val okHttp = okhttp3.OkHttpClient.Builder()
                    .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                    .build()
                val retro = NetworkModule.provideRetrofit(okHttp)
                val api = NetworkModule.provideMonumentApi(retro)
                val p = api.getUserProfile()
                withContext(Dispatchers.Main) {
                    liveProfile = p
                    if (p.name.isNotBlank()) customUsername = p.name
                }
            } catch (e: Exception) {
                // Keep defaults
            }
        }
    }

    val xp = liveProfile.xp
    val level = liveProfile.level
    val streakDays = liveProfile.streakDays
    val visitedCount = liveProfile.visitedCount
    val totalDistanceKm = liveProfile.totalDistanceKm

    val nextLevelXp = 500
    val levelProgress = (xp.toFloat() / nextLevelXp.toFloat()).coerceIn(0f, 1f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 120.dp)
    ) {
        // ── HERO PROFILE HEADER ──────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF1E293B), Bg)
                    )
                )
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Avatar with Camera Edit Icon
                Box(contentAlignment = Alignment.BottomEnd) {
                    Box(
                        modifier = Modifier
                            .size(92.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0F172A))
                            .border(2.dp, Gold, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (customAvatarUri != null) {
                            AsyncImage(
                                model = customAvatarUri,
                                contentDescription = "Profile Photo",
                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                Icons.Default.Person,
                                null,
                                modifier = Modifier.size(48.dp),
                                tint = Gold
                            )
                        }
                    }

                    // Change Photo Button
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(Gold)
                            .clickable { imagePickerLauncher.launch("image/*") }
                            .padding(6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.PhotoCamera,
                            contentDescription = "Change profile photo",
                            tint = Color(0xFF0F172A),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Username & Handle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        customUsername,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                    Icon(
                        Icons.Default.Edit,
                        null,
                        tint = TextSecondary,
                        modifier = Modifier
                            .size(16.dp)
                            .clickable { showEditDialog = true }
                    )
                }

                Text(
                    "@${customUsername.lowercase().replace(" ", "_")}",
                    fontSize = 12.sp,
                    color = TextSecondary
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    customBio,
                    fontSize = 12.sp,
                    color = TextTertiary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )

                Spacer(Modifier.height(12.dp))

                // Level Tag
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF2E1C00))
                        .border(1.dp, Gold.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Text(
                        "LEVEL $level NOVICE EXPLORER",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Gold,
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        // ── LEVEL & PROGRESSION CARD ───────────────────────────────────────
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Surface1),
                border = androidx.compose.foundation.BorderStroke(1.dp, Border)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "XP Progress to Level ${level + 1}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            "$xp / $nextLevelXp XP",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Gold
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    LinearProgressIndicator(
                        progress = { levelProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = Gold,
                        trackColor = Surface2
                    )

                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF0F172A))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.LocalFireDepartment, null, tint = Color(0xFFF97316), modifier = Modifier.size(18.dp))
                        Text(
                            if (streakDays > 0) "🔥 $streakDays Day Explorer Streak Active!" else "🔥 0 Day Streak (Visit a monument today to start!)",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFFED7AA)
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── STATS METRICS GRID (4 CARDS) ──────────────────────────────────
            Text(
                "EXPLORER STATS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 1.2.sp
            )

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard("Monuments", "$visitedCount", Icons.Default.AccountBalance, Modifier.weight(1f))
                MetricCard("Streak", "$streakDays Days", Icons.Default.LocalFireDepartment, Modifier.weight(1f))
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard("Distance", "${String.format("%.1f", totalDistanceKm)} km", Icons.Default.DirectionsWalk, Modifier.weight(1f))
                MetricCard("XP Points", "$xp", Icons.Default.Star, Modifier.weight(1f))
            }

            Spacer(Modifier.height(24.dp))

            // ── ACHIEVEMENTS & BADGES ────────────────────────────────────────
            Text(
                "ACHIEVEMENT BADGES",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 1.2.sp
            )

            Spacer(Modifier.height(10.dp))

            val badgesList = remember(visitedCount) {
                listOf(
                    BadgeItem("First Discovery", "Capture 1st monument", Icons.Default.Explore, Gold, visitedCount >= 1),
                    BadgeItem("Temple Scout", "Discover 3 temples", Icons.Default.AccountBalance, GreenAccent, visitedCount >= 3),
                    BadgeItem("Historian", "Read 5 audio stories", Icons.Default.MenuBook, BlueAccent, false),
                    BadgeItem("3D Pathfinder", "Walk 5.0 km", Icons.Default.DirectionsWalk, Color(0xFFA855F7), totalDistanceKm >= 5.0),
                    BadgeItem("Hotel Quest Pass", "Claim hotel perk", Icons.Default.Hotel, Color(0xFFEC4899), false),
                    BadgeItem("Master Custodian", "Earn 1,000 XP", Icons.Default.EmojiEvents, Gold, xp >= 1000)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                badgesList.chunked(2).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        row.forEach { badge ->
                            Box(modifier = Modifier.weight(1f)) {
                                BadgeCardDetailed(badge)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── ACTIONS & ACCOUNT ───────────────────────────────────────────
            Text(
                "ACCOUNT & PREFERENCES",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 1.2.sp
            )

            Spacer(Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onNavigateToJournalist,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Bg),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.MenuBook, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Journalist & Audio Narrator Mode", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = {
                        authViewModel.logout()
                        onLogout()
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Border),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Surface1, contentColor = RedAccent)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Sign Out", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Edit Username/Bio Dialog Modal
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Profile Info", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = customUsername,
                        onValueChange = { customUsername = it },
                        label = { Text("Username") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = customBio,
                        onValueChange = { customBio = it },
                        label = { Text("Bio") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showEditDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Bg)
                ) {
                    Text("Save Changes", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun MetricCard(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Surface1),
        border = androidx.compose.foundation.BorderStroke(1.dp, Border)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Surface2),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = Gold, modifier = Modifier.size(18.dp))
            }

            Column {
                Text(value, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                Text(label, fontSize = 11.sp, color = TextSecondary)
            }
        }
    }
}

private data class BadgeItem(
    val title: String,
    val desc: String,
    val icon: ImageVector,
    val accentColor: Color,
    val isUnlocked: Boolean
)

@Composable
private fun BadgeCardDetailed(badge: BadgeItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = if (badge.isUnlocked) Surface1 else Color(0xFF0F172A)),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (badge.isUnlocked) badge.accentColor.copy(alpha = 0.5f) else BorderSubtle)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(10.dp).heightIn(min = 56.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (badge.isUnlocked) badge.accentColor.copy(alpha = 0.2f) else Surface2),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    badge.icon, null,
                    tint = if (badge.isUnlocked) badge.accentColor else TextTertiary,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    badge.title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (badge.isUnlocked) TextPrimary else TextTertiary,
                    maxLines = 1
                )
                Text(
                    if (badge.isUnlocked) "UNLOCKED 🎉" else badge.desc,
                    fontSize = 10.sp,
                    color = if (badge.isUnlocked) badge.accentColor else TextTertiary,
                    maxLines = 1
                )
            }
        }
    }
}
