package com.monumentquest.ui.profile

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import com.monumentquest.core.auth.TokenManager
import com.monumentquest.core.di.NetworkModule
import com.monumentquest.data.model.UserProfile
import com.monumentquest.ui.auth.AuthViewModel
import com.monumentquest.ui.common.UserAvatar
import com.monumentquest.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateToJournalist: () -> Unit = {},
    onLogout: () -> Unit = {},
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val currentSession by authViewModel.currentSession.collectAsState()
    val context = LocalContext.current

    val prefs = remember { context.getSharedPreferences("profile_prefs", Context.MODE_PRIVATE) }
    val tokenManager = remember { TokenManager(context) }

    var liveProfile by remember { mutableStateOf(UserProfile()) }

    var customUsername by remember {
        mutableStateOf(
            prefs.getString("profile_name", null)
                ?: currentSession?.name
                ?: tokenManager.getUserName()
                ?: "Explorer"
        )
    }

    var customBio by remember {
        mutableStateOf(
            prefs.getString("profile_bio", "Odisha Heritage Explorer & Monument Discoverer")!!
        )
    }

    var customAvatarUriString by remember {
        mutableStateOf(prefs.getString("profile_avatar_uri", null))
    }

    var showEditDialog by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val destFile = File(context.filesDir, "user_profile_avatar.jpg")
                val outputStream = FileOutputStream(destFile)
                inputStream?.use { input -> outputStream.use { output -> input.copyTo(output) } }
                val savedUri = Uri.fromFile(destFile).toString()

                customAvatarUriString = savedUri
                prefs.edit().putString("profile_avatar_uri", savedUri).apply()
            } catch (e: Exception) {
                customAvatarUriString = uri.toString()
                prefs.edit().putString("profile_avatar_uri", uri.toString()).apply()
            }
        }
    }

    LaunchedEffect(currentSession) {
        if (currentSession != null && !currentSession!!.isGuest) {
            if (prefs.getString("profile_name", null) == null) {
                customUsername = currentSession!!.name
                prefs.edit().putString("profile_name", currentSession!!.name).apply()
            }
        }

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
    val levelProgress = (xp.toFloat() / nextLevelXp.toFloat()).coerceIn(0.05f, 1f)

    Scaffold(
        containerColor = Bg,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Bg)
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "EXPLORER PASSPORT",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Gold,
                    letterSpacing = 1.2.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "My Journal & Profile",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp
                    )

                    IconButton(
                        onClick = { showEditDialog = true },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Surface2)
                            .border(1.dp, Border, CircleShape)
                    ) {
                        Icon(Icons.Default.Settings, null, tint = Gold, modifier = Modifier.size(19.dp))
                    }
                }
                HorizontalDivider(color = BorderSubtle, modifier = Modifier.padding(top = 8.dp))
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(top = 8.dp, bottom = 140.dp, start = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Profile Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = Surface1),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Gold.copy(alpha = 0.45f))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Avatar
                        Box(contentAlignment = Alignment.BottomEnd) {
                            if (customAvatarUriString != null) {
                                Image(
                                    painter = rememberAsyncImagePainter(customAvatarUriString),
                                    contentDescription = "Profile Photo",
                                    modifier = Modifier
                                        .size(92.dp)
                                        .clip(CircleShape)
                                        .border(2.5.dp, Gold, CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                UserAvatar(
                                    name = customUsername,
                                    size = 92.dp,
                                    borderColor = Gold
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(Gold)
                                    .border(2.dp, Surface1, CircleShape)
                                    .clickable { imagePickerLauncher.launch("image/*") },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "Change Photo",
                                    tint = Bg,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = customUsername,
                                style = MaterialTheme.typography.titleLarge,
                                color = TextPrimary,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 21.sp
                            )
                            Icon(Icons.Default.Verified, null, tint = Gold, modifier = Modifier.size(18.dp))
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(Color(0xFF2A1C00), Color(0xFF1E1400))
                                        )
                                    )
                                    .border(1.dp, Gold.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                                    .padding(horizontal = 12.dp, vertical = 5.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.MilitaryTech, null, tint = Gold, modifier = Modifier.size(14.dp))
                                    Text(
                                        text = "Bhubaneswar Explorer",
                                        color = Gold,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.5.sp
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color(0xFF2A0A00))
                                    .border(1.dp, RedAccent.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                                    .padding(horizontal = 12.dp, vertical = 5.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.LocalFireDepartment, null, tint = RedAccent, modifier = Modifier.size(14.dp))
                                    Text(
                                        text = "$streakDays Day Streak",
                                        color = RedAccent,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.5.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = customBio,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontSize = 12.5.sp,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedButton(
                            onClick = { showEditDialog = true },
                            modifier = Modifier.height(38.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Border),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Surface2,
                                contentColor = TextPrimary
                            )
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Edit Profile Details", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // XP Progress Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Surface1),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Border)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.ElectricBolt, null, tint = Gold, modifier = Modifier.size(17.dp))
                                Text(
                                    text = "LEVEL $level EXPLORER",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Gold,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp
                                )
                            }
                            Text(
                                text = "$xp / $nextLevelXp XP",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        LinearProgressIndicator(
                            progress = { levelProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp)),
                            color = Gold,
                            trackColor = Surface2,
                        )
                    }
                }
            }

            // Stats Grid
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        label = "Monuments Discovered",
                        value = "$visitedCount",
                        icon = Icons.Default.Place,
                        iconColor = Color(0xFF38BDF8),
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        label = "Distance Walked",
                        value = String.format("%.1f km", totalDistanceKm),
                        icon = Icons.Default.DirectionsWalk,
                        iconColor = Color(0xFF4ADE80),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Badges & Trophies Section
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "EXPLORER BADGES & UNLOCKS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Gold,
                        letterSpacing = 1.sp
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            BadgeCard(title = "Deula Pioneer", desc = "Visited 1st Kalinga Temple", icon = Icons.Default.TempleHindu, accentColor = Gold, isUnlocked = visitedCount >= 1)
                        }
                        item {
                            BadgeCard(title = "Kalinga Keeper", desc = "Walked over 5.0 km", icon = Icons.Default.Hiking, accentColor = Color(0xFF38BDF8), isUnlocked = totalDistanceKm >= 5.0)
                        }
                        item {
                            BadgeCard(title = "Time Capsule Master", desc = "Left 3 secret capsules", icon = Icons.Default.VpnKey, accentColor = Color(0xFFC084FC), isUnlocked = xp >= 300)
                        }
                    }
                }
            }

            // Action Buttons
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onNavigateToJournalist,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, RedAccent.copy(alpha = 0.4f)),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = Surface1, contentColor = RedAccent)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Sign Out", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Edit Username/Bio Dialog Modal
    if (showEditDialog) {
        var tempUsername by remember { mutableStateOf(customUsername) }
        var tempBio by remember { mutableStateOf(customBio) }

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            containerColor = Surface1,
            title = { Text("Edit Explorer Profile", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = tempUsername,
                        onValueChange = { tempUsername = it },
                        label = { Text("Explorer Username") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Gold,
                            unfocusedBorderColor = Border,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                    OutlinedTextField(
                        value = tempBio,
                        onValueChange = { tempBio = it },
                        label = { Text("Explorer Bio") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Gold,
                            unfocusedBorderColor = Border,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleanName = tempUsername.trim().ifBlank { "Explorer" }
                        val cleanBio = tempBio.trim().ifBlank { "Odisha Heritage Explorer" }

                        customUsername = cleanName
                        customBio = cleanBio

                        prefs.edit()
                            .putString("profile_name", cleanName)
                            .putString("profile_bio", cleanBio)
                            .apply()
                        tokenManager.saveUserName(cleanName)

                        showEditDialog = false
                    },
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
private fun MetricCard(
    label: String,
    value: String,
    icon: ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
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
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = iconColor, modifier = Modifier.size(20.dp))
            }

            Column {
                Text(value, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                Text(label, fontSize = 11.sp, color = TextSecondary)
            }
        }
    }
}

@Composable
private fun BadgeCard(
    title: String,
    desc: String,
    icon: ImageVector,
    accentColor: Color,
    isUnlocked: Boolean
) {
    Card(
        modifier = Modifier.width(175.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Surface1),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isUnlocked) accentColor.copy(alpha = 0.5f) else Border)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(if (isUnlocked) accentColor.copy(alpha = 0.2f) else Surface2),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon, null,
                    tint = if (isUnlocked) accentColor else TextSecondary,
                    modifier = Modifier.size(17.dp)
                )
            }

            Text(
                title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isUnlocked) TextPrimary else TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                if (isUnlocked) "UNLOCKED 🎉" else desc,
                fontSize = 10.5.sp,
                color = if (isUnlocked) accentColor else TextSecondary,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
