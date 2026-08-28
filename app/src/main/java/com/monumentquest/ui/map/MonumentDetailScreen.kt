package com.monumentquest.ui.map

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.monumentquest.data.model.MapMonumentItem
import com.monumentquest.ui.theme.*

@Composable
fun MonumentDetailScreen(
    monumentId: String,
    onNavigateBack: () -> Unit,
    onNavigateToNarrator: (String) -> Unit,
    onNavigateToCamera: () -> Unit
) {
    // Use the actual monument name decoded from the nav argument
    val decodedName = remember(monumentId) {
        try { java.net.URLDecoder.decode(monumentId, "UTF-8") } catch (_: Exception) { monumentId }
    }

    val monument = MapMonumentItem(
        id            = monumentId,
        name          = decodedName,
        locationName  = "India",
        points        = 500,
        category      = "Heritage Monument",
        distanceMeters = 0
    )

    val samplePhotos = listOf(
        "https://images.unsplash.com/photo-1627894483216-2138af692e32?q=80&w=800&auto=format&fit=crop",
        "https://images.unsplash.com/photo-1600585154340-be6161a56a0c?q=80&w=800&auto=format&fit=crop",
        "https://images.unsplash.com/photo-1590050752117-238cb0fb12b1?q=80&w=800&auto=format&fit=crop"
    )

    val recentVisitors = listOf("Aarav P.", "Priya M.", "Subhashree D.", "Vikram R.", "Rohan S.")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
        ) {
            // ── 1. Hero Image & Top Navigation Bar ───────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
            ) {
                AsyncImage(
                    model = samplePhotos[0],
                    contentDescription = monument.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Dark gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    ObsidianBlack.copy(alpha = 0.6f),
                                    Color.Transparent,
                                    ObsidianBlack
                                )
                            )
                        )
                )

                // Back Button & Share Icon Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(ObsidianBlack.copy(alpha = 0.7f))
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = CreamWhite)
                    }

                    IconButton(
                        onClick = { /* Share */ },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(ObsidianBlack.copy(alpha = 0.7f))
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = GoldBright)
                    }
                }

                // Rarity Tag
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(GoldBright)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "✦ LEGENDARY HERITAGE · +${monument.points} XP",
                        style = MaterialTheme.typography.labelSmall,
                        color = ObsidianBlack,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            // ── 2. Title & Key Architectural Stats Cards ─────────────────────
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column {
                    Text(
                        text = monument.name,
                        style = MaterialTheme.typography.headlineMedium,
                        color = CreamWhite,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = monument.locationName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedGray
                    )
                }

                // Quick Facts Cards Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FactCard(title = "Year Built", value = "1060 AD", modifier = Modifier.weight(1f))
                    FactCard(title = "Style", value = "Kalinga Deula", modifier = Modifier.weight(1f))
                    FactCard(title = "Rating", value = "4.9 ★ (128)", modifier = Modifier.weight(1f))
                }

                // Historical Facts Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = CardSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Info, null, tint = GoldBright, modifier = Modifier.size(18.dp))
                            Text("Historical Significance", fontWeight = FontWeight.Bold, color = CreamWhite, fontSize = 14.sp)
                        }
                        Text(
                            text = "Constructed by King Jajati Keshari of the Somavamsi dynasty in the 11th century, Lingaraj Temple stands at 55 metres tall. It represents the pinnacle of Kalinga architectural synthesis, combining Harihara (Lord Shiva and Lord Vishnu) traditions.",
                            style = MaterialTheme.typography.bodySmall,
                            color = ParchmentLight,
                            lineHeight = 20.sp
                        )
                    }
                }

                // "Did You Know?" Trivia Digestible Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = GlassSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ForestMid.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(Icons.Default.Lightbulb, null, tint = GoldBright, modifier = Modifier.size(24.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Did You Know?", fontWeight = FontWeight.Black, color = GoldBright, fontSize = 13.sp)
                            Text(
                                text = "The main Deula tower contains over 108 smaller shrines within its sacred precinct, and no shadow is cast at solar noon!",
                                style = MaterialTheme.typography.bodySmall,
                                color = CreamWhite,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                // ── 3. Inline Social Proof: Recent Visitors & Community Photos ──
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Recent Explorers", fontWeight = FontWeight.Bold, color = CreamWhite, fontSize = 14.sp)

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        recentVisitors.forEach { visitor ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(ElevatedSurface)
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(visitor, fontSize = 11.sp, color = MutedGray, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Community Discoveries", fontWeight = FontWeight.Bold, color = CreamWhite, fontSize = 14.sp)

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(samplePhotos) { photo ->
                            AsyncImage(
                                model = photo,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(120.dp, 80.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(100.dp)) // Clearance for bottom CTA
            }
        }

        // ── 4. Primary CTA Fixed Dock (Collect & Talk Buttons) ────────────────
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            color = ObsidianBlack.copy(alpha = 0.96f),
            shadowElevation = 20.dp
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .navigationBarsPadding(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { onNavigateToNarrator(monument.name) },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GoldBright)
                ) {
                    Icon(Icons.Default.ChatBubble, null, tint = GoldBright, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("AI Narrator", color = GoldBright, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Button(
                    onClick = onNavigateToCamera,
                    modifier = Modifier
                        .weight(1.5f)
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GoldBright, contentColor = ObsidianBlack),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.AddAPhoto, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Log Visit (+${monument.points} XP)", fontWeight = FontWeight.Black, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun FactCard(title: String, value: String, modifier: Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontWeight = FontWeight.Black, color = GoldBright, fontSize = 13.sp)
            Text(title, style = MaterialTheme.typography.labelSmall, color = MutedGray, fontSize = 10.sp)
        }
    }
}
