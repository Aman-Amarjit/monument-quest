package com.monumentquest.ui.discovery

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Star
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
            // Bottom fade
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(listOf(Color.Transparent, Bg))
                    )
            )
            // Discovery tag
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(Gold)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    "Discovery Log",
                    style      = MaterialTheme.typography.labelSmall,
                    color      = Bg,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // ── Form ──────────────────────────────────────────────────────────────
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Name your discovery",
                fontWeight = FontWeight.SemiBold,
                color      = TextPrimary,
                fontSize   = 20.sp
            )

            Text(
                "Points are calculated based on how many explorers have uploaded this monument before you — the fewer, the more XP you earn.",
                style      = MaterialTheme.typography.bodySmall,
                color      = TextSecondary,
                lineHeight = 18.sp
            )

            OutlinedTextField(
                value       = name,
                onValueChange = { name = it },
                label       = { Text("Monument Name") },
                placeholder = { Text("e.g. Lingaraj Temple…", color = TextSecondary) },
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
                                "Calculating rarity XP multiplier…",
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
                                verticalAlignment    = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Default.MilitaryTech, null, tint = Gold, modifier = Modifier.size(26.dp))
                                Column {
                                    Text(result.rarityBadge, fontWeight = FontWeight.SemiBold, color = Gold, fontSize = 14.sp)
                                    Text(
                                        "${result.previousUploadersCount} explorers uploaded this before you",
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
                                    Text("MULTIPLIER", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 10.sp)
                                    Text("${result.multiplier}x Bonus", fontWeight = FontWeight.Bold, color = GreenAccent, fontSize = 15.sp)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("XP EARNED", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 10.sp)
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
                    SubmitButton(enabled = true) { viewModel.uploadDiscovery(name, imageUri) }
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
        Text("Submit & Calculate XP", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}
