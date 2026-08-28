package com.monumentquest.ui.profile

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.monumentquest.core.auth.TokenManager
import com.monumentquest.core.utils.ImageUtils
import com.monumentquest.ui.auth.AuthViewModel
import com.monumentquest.ui.common.UserAvatar
import com.monumentquest.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateToJournalist: () -> Unit = {},
    onLogout: () -> Unit = {},
    authViewModel: AuthViewModel = hiltViewModel(),
    profileViewModel: ProfileViewModel = hiltViewModel()
) {
    val currentSession by authViewModel.currentSession.collectAsState()
    val liveProfile by profileViewModel.userProfile.collectAsState()
    val context = LocalContext.current

    val prefs = remember { context.getSharedPreferences("profile_prefs", Context.MODE_PRIVATE) }
    val tokenManager = remember { TokenManager(context) }

    val userKey = remember(currentSession) {
        currentSession?.email?.lowercase()?.replace("@", "_")?.replace(".", "_") ?: "guest"
    }

    var customUsername by remember {
        mutableStateOf(
            prefs.getString("profile_name_$userKey", null)
                ?: currentSession?.name
                ?: tokenManager.getUserName()
                ?: "Explorer"
        )
    }

    var customBio by remember {
        mutableStateOf(
            prefs.getString("profile_bio_$userKey", "Odisha Heritage Explorer & Monument Discoverer")!!
        )
    }

    var customAvatarUriString by remember {
        mutableStateOf(prefs.getString("profile_avatar_uri_$userKey", null)
             ?: liveProfile.avatarUrl
             ?: tokenManager.getUserAvatarUrl())
    }

    var showEditDialog by remember { mutableStateOf(false) }
    var isUploadingAvatar by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                isUploadingAvatar = true
                try {
                    // Upload to Cloudinary — returns a public HTTPS URL visible on all devices
                    val cloudUrl = withContext(Dispatchers.IO) {
                        ImageUtils.uploadToCloudinary(context, uri)
                    }
                    val urlToSave = cloudUrl
                        ?: ImageUtils.uriToBase64DataUrl(context, uri)  // local-only fallback
                        ?: return@launch

                    customAvatarUriString = urlToSave
                    prefs.edit().putString("profile_avatar_uri_$userKey", urlToSave).apply()
                    profileViewModel.updateProfile(avatarUrl = urlToSave)
                } catch (e: Exception) {
                    // Silent fail — avatar stays unchanged
                } finally {
                    isUploadingAvatar = false
                }
            }
        }
    }

    LaunchedEffect(liveProfile.avatarUrl, userKey) {
        val serverAvatar = liveProfile.avatarUrl
        // Only apply server avatar if user has not picked a local one
        val localAvatar = prefs.getString("profile_avatar_uri_$userKey", null)
        if (!serverAvatar.isNullOrBlank() && localAvatar.isNullOrBlank()) {
            customAvatarUriString = serverAvatar
            prefs.edit().putString("profile_avatar_uri_$userKey", serverAvatar).apply()
        }
    }

    LaunchedEffect(currentSession, userKey) {
        customUsername = prefs.getString("profile_name_$userKey", null)
            ?: currentSession?.name
            ?: tokenManager.getUserName()
            ?: "Explorer"
        customBio = prefs.getString("profile_bio_$userKey", "Odisha Heritage Explorer & Monument Discoverer")!!
        // Only use server/token avatar as fallback if no local avatar saved
        val localAvatar = prefs.getString("profile_avatar_uri_$userKey", null)
        if (customAvatarUriString.isNullOrBlank() && !localAvatar.isNullOrBlank()) {
            customAvatarUriString = localAvatar
        } else if (customAvatarUriString.isNullOrBlank()) {
            customAvatarUriString = liveProfile.avatarUrl ?: tokenManager.getUserAvatarUrl()
        }

        if (currentSession != null && !currentSession!!.isGuest) {
            if (prefs.getString("profile_name_$userKey", null) == null) {
                customUsername = currentSession!!.name
                prefs.edit().putString("profile_name_$userKey", currentSession!!.name).apply()
            }
        }
    }

    Scaffold(
        containerColor = Bg
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "EXPLORER PASSPORT",
                    style = MaterialTheme.typography.labelSmall,
                    color = Gold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "My Journal & Profile",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    IconButton(
                        onClick = { showEditDialog = true },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Surface2)
                            .border(1.dp, Border, CircleShape)
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Gold)
                    }
                }
            }

            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .border(1.5.dp, Gold.copy(alpha = 0.5f), RoundedCornerShape(24.dp)),
                    color = Surface1
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(contentAlignment = Alignment.BottomEnd) {
                            UserAvatar(
                                name = customUsername,
                                avatarUrl = customAvatarUriString,
                                size = 96.dp,
                                borderColor = Gold
                            )
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(if (isUploadingAvatar) Surface2 else Gold)
                                    .border(2.dp, Surface1, CircleShape)
                                    .clickable(enabled = !isUploadingAvatar) { imagePickerLauncher.launch("image/*") },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isUploadingAvatar) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = Gold,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(Icons.Default.CameraAlt, null, tint = Bg, modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = customUsername,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Icon(Icons.Default.Verified, null, tint = Gold, modifier = Modifier.size(18.dp))
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Surface2)
                                    .border(1.dp, Gold.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.MilitaryTech, null, tint = Gold, modifier = Modifier.size(14.dp))
                                    Text("Bhubaneswar Explorer", fontSize = 11.sp, color = Gold, fontWeight = FontWeight.Bold)
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color(0xFF2A1C00))
                                    .border(1.dp, Color(0xFFFF8C42).copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.LocalFireDepartment, null, tint = Color(0xFFFF8C42), modifier = Modifier.size(14.dp))
                                    Text("0 Day Streak", fontSize = 11.sp, color = Color(0xFFFF8C42), fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Text(
                            text = customBio,
                            fontSize = 12.sp,
                            color = TextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )

                        OutlinedButton(
                            onClick = { showEditDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Border),
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = Surface2, contentColor = TextPrimary)
                        ) {
                            Icon(Icons.Default.Edit, null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Edit Profile Details", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            item {
                val totalXp = liveProfile.xp
                val currentLevel = Math.max(1, (totalXp / 500) + 1)
                val progressInLevel = (totalXp % 500) / 500f

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    color = Surface1
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.ElectricBolt, null, tint = Gold, modifier = Modifier.size(16.dp))
                                Text("LEVEL $currentLevel EXPLORER", fontWeight = FontWeight.Bold, color = Gold, fontSize = 12.sp)
                            }
                            Text("${totalXp % 500} / 500 XP", fontWeight = FontWeight.Bold, color = TextSecondary, fontSize = 12.sp)
                        }

                        LinearProgressIndicator(
                            progress = { progressInLevel.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape),
                            color = Gold,
                            trackColor = Surface2
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        icon = Icons.Default.Place,
                        value = "${liveProfile.visitedCount}",
                        label = "Monuments Discovered",
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        icon = Icons.Default.DirectionsWalk,
                        value = "${liveProfile.totalDistanceKm} km",
                        label = "Distance Walked",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Text(
                    text = "EXPLORER BADGES & UNLOCKS",
                    style = MaterialTheme.typography.labelSmall,
                    color = Gold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item { BadgeCard("Deula Pioneer", "Visited 1st Kalinga Temple", Icons.Default.Castle) }
                    item { BadgeCard("Kalinga Keeper", "Walked over 5.0 km", Icons.Default.DirectionsWalk) }
                    item { BadgeCard("Time Traveler", "Left 3 Time Capsules", Icons.Default.VpnKey) }
                }
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onNavigateToJournalist,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Bg)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.MenuBook, null, tint = Bg)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Journalist & Audio Narrator Mode", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            authViewModel.logout()
                            onLogout()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f)),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFF2A1010), contentColor = Color(0xFFEF4444))
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, null, tint = Color(0xFFEF4444))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sign Out", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
    }

    if (showEditDialog) {
        var tempName by remember { mutableStateOf(customUsername) }
        var tempBio  by remember { mutableStateOf(customBio) }

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            containerColor = Surface1,
            title = { Text("Edit Profile Details", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = tempName,
                        onValueChange = { tempName = it },
                        label = { Text("Explorer Name") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = Gold,
                            unfocusedBorderColor = Border,
                            focusedTextColor     = TextPrimary,
                            unfocusedTextColor   = TextPrimary
                        )
                    )
                    OutlinedTextField(
                        value = tempBio,
                        onValueChange = { tempBio = it },
                        label = { Text("Bio") },
                        modifier = Modifier.height(90.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = Gold,
                            unfocusedBorderColor = Border,
                            focusedTextColor     = TextPrimary,
                            unfocusedTextColor   = TextPrimary
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (tempName.isNotBlank()) {
                            customUsername = tempName.trim()
                            prefs.edit().putString("profile_name_$userKey", tempName.trim()).apply()
                        }
                        if (tempBio.isNotBlank()) {
                            customBio = tempBio.trim()
                            prefs.edit().putString("profile_bio_$userKey", tempBio.trim()).apply()
                        }
                        profileViewModel.updateProfile(name = tempName.trim())
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
private fun StatCard(
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, Border, RoundedCornerShape(16.dp)),
        color = Surface1
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Surface2),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = Gold, modifier = Modifier.size(18.dp))
            }
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
            Text(label, fontSize = 11.sp, color = TextSecondary)
        }
    }
}

@Composable
private fun BadgeCard(
    title: String,
    subtitle: String,
    icon: ImageVector
) {
    Surface(
        modifier = Modifier
            .width(180.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, Border, RoundedCornerShape(16.dp)),
        color = Surface1
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Surface2),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = Gold, modifier = Modifier.size(16.dp))
            }
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(subtitle, fontSize = 10.5.sp, color = TextSecondary)
        }
    }
}
