package com.monumentquest.ui.discovery

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.monumentquest.ui.theme.*

@Composable
fun DiscoveryFormScreen(
    imageUri: Uri,
    onSuccess: () -> Unit,
    viewModel: DiscoveryViewModel = hiltViewModel()
) {
    var name  by remember { mutableStateOf("") }
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .statusBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
    ) {
        // ── Photo preview ─────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
        ) {
            AsyncImage(
                model            = imageUri,
                contentDescription = "Captured Monument",
                modifier         = Modifier.fillMaxSize(),
                contentScale     = ContentScale.Crop
            )
            // Bottom fade gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Bg)
                        )
                    )
            )

            // Live Camera Badge
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.TopStart)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xCC0F172A))
                    .border(1.dp, Color(0xFF22C55E), RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF22C55E), modifier = Modifier.size(14.dp))
                Text("Live Onsite Photo Verified", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        // ── Form Content ──────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Name your discovery",
                fontWeight = FontWeight.SemiBold,
                color      = TextPrimary,
                fontSize   = 20.sp
            )

            // Geofenced Verification Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(14.dp),
                colors   = CardDefaults.cardColors(containerColor = Surface1),
                border   = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF0A500).copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.GpsFixed, null, tint = Color(0xFFF0A500), modifier = Modifier.size(24.dp))
                    Column {
                        Text("Server Geofenced Verification", fontWeight = FontWeight.Bold, color = Color(0xFFF0A500), fontSize = 12.5.sp)
                        Text(
                            "Captures are checked in real-time by the backend server. XP and leaderboard points are awarded instantly upon server distance confirmation!",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            OutlinedTextField(
                value       = name,
                onValueChange = { name = it },
                label       = { Text("Monument / Landmark Name") },
                placeholder = { Text("e.g. Lingaraj Temple, Rajarani Temple…", color = TextSecondary) },
                leadingIcon = {
                    Icon(Icons.Default.Landscape, null, tint = Gold, modifier = Modifier.size(20.dp))
                },
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor      = Gold,
                    unfocusedBorderColor    = Border,
                    focusedLabelColor       = Gold,
                    unfocusedLabelColor     = TextSecondary,
                    focusedTextColor        = TextPrimary,
                    unfocusedTextColor      = TextPrimary,
                    cursorColor             = Gold,
                    focusedContainerColor   = Surface1,
                    unfocusedContainerColor = Surface1
                )
            )

            when (state) {
                is DiscoveryState.Loading -> {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(color = Gold, strokeWidth = 2.5.dp)
                            Text(
                                "Verifying location & claiming server reward…",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }

                is DiscoveryState.Success -> {
                    val result = (state as DiscoveryState.Success).result
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape    = RoundedCornerShape(14.dp),
                        colors   = CardDefaults.cardColors(containerColor = Surface1),
                        border   = androidx.compose.foundation.BorderStroke(1.dp, Gold.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Default.MilitaryTech, null, tint = Gold, modifier = Modifier.size(26.dp))
                                Column {
                                    Text(result.monumentName.ifBlank { "Monument Captured!" }, fontWeight = FontWeight.Bold, color = Gold, fontSize = 14.sp)
                                    Text(
                                        result.rarityBadge,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                }
                            }

                            HorizontalDivider(color = Border)

                            Row(
                                modifier              = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment     = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("SERVER STATUS", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 10.sp)
                                    Text("Verified ✓", fontWeight = FontWeight.Bold, color = Color(0xFF22C55E), fontSize = 14.sp)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("SERVER REWARD", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 10.sp)
                                    Text("+${result.pointsEarned} XP", fontWeight = FontWeight.Bold, color = Gold, fontSize = 18.sp)
                                }
                            }

                            Button(
                                onClick  = onSuccess,
                                modifier = Modifier.fillMaxWidth().height(46.dp),
                                colors   = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Bg),
                                shape    = RoundedCornerShape(10.dp)
                            ) {
                                Text("Back to Map", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                is DiscoveryState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(RedAccent.copy(alpha = 0.08f))
                            .border(1.dp, RedAccent.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(14.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Error, null, tint = RedAccent, modifier = Modifier.size(20.dp))
                            Text(
                                (state as DiscoveryState.Error).message,
                                color = RedAccent,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    SubmitButton(enabled = name.isNotBlank()) { viewModel.uploadDiscovery(name, imageUri) }
                }

                else -> {
                    SubmitButton(enabled = name.isNotBlank()) {
                        viewModel.uploadDiscovery(name, imageUri)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SubmitButton(enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick  = onClick,
        enabled  = enabled,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        colors   = ButtonDefaults.buttonColors(
            containerColor         = Gold,
            contentColor           = Bg,
            disabledContainerColor = Surface2,
            disabledContentColor   = TextSecondary
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Icon(Icons.Default.CloudUpload, null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Verify & Claim Server Capture Reward", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}
