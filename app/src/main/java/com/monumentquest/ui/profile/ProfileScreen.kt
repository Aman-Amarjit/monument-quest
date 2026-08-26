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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@Composable
fun ProfileScreen(
    onNavigateToJournalist: () -> Unit = {},
    onLogout: () -> Unit = {},
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val currentSession by authViewModel.currentSession.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val prefs = remember { context.getSharedPreferences("profile_prefs", Context.MODE_PRIVATE) }
    val tokenManager = remember { TokenManager(context) }

    var liveProfile by remember { mutableStateOf(UserProfile()) }

    // Read initial values from SharedPreferences / TokenManager / Session
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

    // Launcher for picking profile picture & saving permanently
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                // Copy photo to internal app storage so it survives app updates & restarts
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
                // Keep loaded profile
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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg),
        contentPadding = PaddingValues(top = 16.dp, bottom = 140.dp, start = 16.dp, end = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Surface1),
                border = androidx.compose.foundation.BorderStroke(1.dp, Border)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Profile Avatar with edit camera button
                    Box(contentAlignment = Alignment.BottomEnd) {
                        if (customAvatarUriString != null) {
                            Image(
                                painter = rememberAsyncImagePainter(customAvatarUriString),
                                contentDescription = "Profile Photo",
                                modifier = Modifier
                                    .size(84.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, Gold, CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            UserAvatar(
                                name = customUsername,
                                size = 84.dp,
                                borderColor = Gold
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(28.dp)
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
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = customUsername,
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Surface2)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Bhubaneswar Explorer",
                                style = MaterialTheme.typography.labelSmall,
                                color = Gold,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Surface2)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "🔥 $streakDays Day Streak",
                                style = MaterialTheme.typography.labelSmall,
                                color = RedAccent,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = customBio,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = { showEditDialog = true },
                        modifier = Modifier.height(36.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Border),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Surface2,
                            contentColor = TextPrimary
                        )
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Edit Profile", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        item {
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
                            text = "LEVEL $level EXPLORER",
                            style = MaterialTheme.typography.labelSmall,
                            color = Gold,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$xp / $nextLevelXp XP",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LinearProgressIndicator(
                        progress = { levelProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = Gold,
                        trackColor = Surface2,
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    label = "Monuments Discovered",
                    value = "$visitedCount",
                    icon = Icons.Default.Place,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    label = "Distance Walked",
                    value = String.format("%.1f km", totalDistanceKm),
                    icon = Icons.Default.DirectionsWalk,
                    modifier = Modifier.weight(1f)
                )
            }
        }

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

    // Edit Username/Bio Dialog Modal with permanent saving
    if (showEditDialog) {
        var tempUsername by remember { mutableStateOf(customUsername) }
        var tempBio by remember { mutableStateOf(customBio) }

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Profile Info", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = tempUsername,
                        onValueChange = { tempUsername = it },
                        label = { Text("Username") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = tempBio,
                        onValueChange = { tempBio = it },
                        label = { Text("Bio") },
                        modifier = Modifier.fillMaxWidth()
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

                        // Permanently save to SharedPreferences + TokenManager
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
