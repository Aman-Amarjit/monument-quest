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
import androidx.compose.ui.text.style.TextAlign
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
            .background(ObsidianBlack)
            .verticalScroll(rememberScrollState())
    ) {
        // ── Captured Image ───────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
        ) {
            AsyncImage(
                model = imageUri,
                contentDescription = "Captured Monument",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, ObsidianBlack)
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(GoldBright.copy(alpha = 0.9f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "DISCOVERY LOG",
                    style = MaterialTheme.typography.labelSmall,
                    color = ObsidianBlack,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
            }
        }

        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Name Your Discovery",
                style = MaterialTheme.typography.headlineSmall,
                color = CreamWhite,
                fontWeight = FontWeight.Black
            )

            Text(
                text = "Points and multipliers are dynamically calculated based on how many explorers have uploaded this monument before you.",
                style = MaterialTheme.typography.bodySmall,
                color = MutedGray,
                lineHeight = 18.sp
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Monument Name") },
                placeholder = { Text("e.g. Lingaraj Temple, Mukteshvara…", color = MutedGray) },
                leadingIcon = {
                    Icon(Icons.Default.Landscape, null, tint = GoldBright, modifier = Modifier.size(20.dp))
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor      = GoldBright,
                    unfocusedBorderColor    = SubtleGray,
                    focusedLabelColor       = GoldBright,
                    unfocusedLabelColor     = MutedGray,
                    focusedTextColor        = CreamWhite,
                    unfocusedTextColor      = CreamWhite,
                    cursorColor             = GoldBright,
                    focusedContainerColor   = ElevatedSurface,
                    unfocusedContainerColor = CardSurface
                )
            )

            // Dynamic Rarity Points Area
            when (state) {
                is DiscoveryState.Loading -> {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(color = GoldBright, strokeWidth = 3.dp)
                            Text("Checking previous uploaders & calculating rarity XP multiplier…", style = MaterialTheme.typography.bodySmall, color = MutedGray)
                        }
                    }
                }

                is DiscoveryState.Success -> {
                    val result = (state as DiscoveryState.Success).result
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = CardSurface),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, GoldBright)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Default.MilitaryTech, null, tint = GoldBright, modifier = Modifier.size(28.dp))
                                Column {
                                    Text(result.rarityBadge, fontWeight = FontWeight.Black, color = GoldBright, fontSize = 15.sp)
                                    Text(
                                        text = "${result.previousUploadersCount} explorers uploaded this monument before you",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MutedGray
                                    )
                                }
                            }

                            HorizontalDivider(color = SubtleGray)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("XP MULTIPLIER", style = MaterialTheme.typography.labelSmall, color = MutedGray)
                                    Text("${result.multiplier}x Bonus", fontWeight = FontWeight.Bold, color = ForestMint, fontSize = 16.sp)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("TOTAL AWARDED", style = MaterialTheme.typography.labelSmall, color = MutedGray)
                                    Text("+${result.pointsEarned} XP", fontWeight = FontWeight.Black, color = GoldBright, fontSize = 18.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Button(
                                onClick = onSuccess,
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ForestMid),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Continue to Map", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                is DiscoveryState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(ErrorRed.copy(alpha = 0.1f))
                            .border(1.dp, ErrorRed.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Error, null, tint = ErrorRed, modifier = Modifier.size(24.dp))
                            Text(
                                "Error: ${(state as DiscoveryState.Error).message}",
                                color = ErrorRed,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    SubmitButton(label = "Retry", enabled = true) {
                        viewModel.uploadDiscovery(name, imageUri)
                    }
                }

                else -> {
                    SubmitButton(
                        label = "Submit & Calculate Rarity XP",
                        enabled = name.isNotBlank()
                    ) {
                        viewModel.uploadDiscovery(name, imageUri)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SubmitButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (enabled)
                    Brush.horizontalGradient(listOf(ForestMid, GoldDark))
                else
                    Brush.horizontalGradient(listOf(SubtleGray, SubtleGray))
            )
    ) {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor   = CreamWhite,
                disabledContainerColor = Color.Transparent,
                disabledContentColor = MutedGray
            ),
            shape = RoundedCornerShape(16.dp),
            elevation = ButtonDefaults.buttonElevation(0.dp)
        ) {
            Icon(Icons.Default.CloudUpload, null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}
