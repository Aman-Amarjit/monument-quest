package com.monumentquest.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.LocalLibrary
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.monumentquest.ui.auth.AuthViewModel
import com.monumentquest.ui.theme.*

data class CollectionItem(
    val id: String,
    val name: String,
    val region: String,
    val points: Int,
    val isUnlocked: Boolean,
    val imageUrl: String
)

@Composable
fun ProfileScreen(
    onNavigateToJournalist: () -> Unit = {},
    onLogout: () -> Unit = {},
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val currentSession by authViewModel.currentSession.collectAsState()

    val userName        = currentSession?.name ?: "Explorer"
    val userEmail       = currentSession?.email ?: "guest@monumentquest.app"
    val isGuest         = currentSession?.isGuest ?: true
    val points          = currentSession?.points ?: 100
    val nextLevelPoints = 1000
    val progress        = points.toFloat() / nextLevelPoints
    val rank            = currentSession?.userRank ?: "Novice Wanderer"

    val collectionItems = listOf(
        CollectionItem("c1", "Lingaraj Temple", "Odisha", 500, true, "https://images.unsplash.com/photo-1627894483216-2138af692e32?q=80&w=800&auto=format&fit=crop"),
        CollectionItem("c2", "Mukteshvara Temple", "Odisha", 450, true, "https://images.unsplash.com/photo-1600585154340-be6161a56a0c?q=80&w=800&auto=format&fit=crop"),
        CollectionItem("c3", "Dhauli Shanti Stupa", "Odisha", 600, true, "https://images.unsplash.com/photo-1590050752117-238cb0fb12b1?q=80&w=800&auto=format&fit=crop"),
        CollectionItem("c4", "Rajarani Temple", "Odisha", 400, false, ""),
        CollectionItem("c5", "Khandagiri Caves", "Odisha", 550, false, ""),
        CollectionItem("c6", "Konark Sun Temple", "Odisha", 1000, false, "")
    )

    val badges = listOf(
        BadgeData("First Discovery",   Icons.Default.Explore,      GoldBright),
        BadgeData("Nature Lover",      Icons.Default.Landscape,    SuccessGreen),
        BadgeData("History Buff",      Icons.Default.LocalLibrary, EmberMid),
        BadgeData("Mountaineer",       Icons.Default.Terrain,      ForestMint),
        BadgeData("Scholar",           Icons.AutoMirrored.Filled.MenuBook,     ForestLight)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack)
            .verticalScroll(rememberScrollState())
    ) {
        // ── 1. Digital Passport Hero Header ────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                ForestDeep.copy(alpha = 0.9f),
                                ForestMid.copy(alpha = 0.6f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(GoldBright.copy(alpha = 0.3f), ForestMid.copy(alpha = 0.4f))
                            )
                        )
                        .border(
                            width = 2.dp,
                            brush = Brush.linearGradient(listOf(GoldBright, ForestMint)),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isGuest) Icons.Default.Shield else Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(44.dp),
                        tint = GoldBright
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = userName,
                        style = MaterialTheme.typography.headlineSmall,
                        color = CreamWhite,
                        fontWeight = FontWeight.Black
                    )
                    if (isGuest) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(GoldBright.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("GUEST MODE", style = MaterialTheme.typography.labelSmall, color = GoldBright, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Text(
                    text = "DIGITAL PASSPORT · $userEmail",
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedGray,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, null, tint = GoldBright, modifier = Modifier.size(14.dp))
                    Text(
                        text = rank.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = GoldBright,
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(Icons.Default.AutoAwesome, null, tint = GoldBright, modifier = Modifier.size(14.dp))
                }
            }
        }

        // ── 2. Grand Custodian & Mentor Pair Cards ───────────────────────────
        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Adopt-a-Monument: Grand Custodian Status
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = GlassSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, GoldBright.copy(alpha = 0.6f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(GoldBright.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("👑", fontSize = 20.sp)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Grand Custodian Status", fontWeight = FontWeight.Black, color = GoldBright, fontSize = 13.sp)
                        Text("Lingaraj Temple · Earning +50 🪙 Heritage Coins/day", style = MaterialTheme.typography.labelSmall, color = CreamWhite, fontSize = 11.sp)
                    }
                }
            }

            // Mentor Pair Status
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, ForestMint.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(ForestMint.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.School, null, tint = ForestMint, modifier = Modifier.size(22.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Mentor Pair Program", fontWeight = FontWeight.Bold, color = ForestMint, fontSize = 13.sp)
                        Text("Paired with Dr. Subhashree (Lvl 42) · +1.5x Guided XP", style = MaterialTheme.typography.labelSmall, color = MutedGray, fontSize = 11.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── 3. Regional Progress Bars ─────────────────────────────────────────
        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Regional Progress", fontWeight = FontWeight.Bold, color = CreamWhite, fontSize = 15.sp)

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Odisha Temples & Shrines", fontWeight = FontWeight.Bold, color = CreamWhite, fontSize = 13.sp)
                        Text("3 / 6 Collected (50%)", fontWeight = FontWeight.Bold, color = GoldBright, fontSize = 12.sp)
                    }
                    LinearProgressIndicator(
                        progress = { 0.5f },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = GoldBright,
                        trackColor = SubtleGray
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── 4. Pokédex Collection Vault ───────────────────────────────────────
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Text("Collection Book (Relic Vault)", fontWeight = FontWeight.Bold, color = CreamWhite, fontSize = 15.sp)
            Spacer(modifier = Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                collectionItems.chunked(2).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowItems.forEach { item ->
                            Box(modifier = Modifier.weight(1f)) {
                                CollectionCard(item)
                            }
                        }
                        if (rowItems.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── 5. Badges Strip ──────────────────────────────────────────────────
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.MilitaryTech, null, tint = GoldBright, modifier = Modifier.size(18.dp))
                Text("Honors & Badges", fontWeight = FontWeight.Bold, color = CreamWhite, fontSize = 15.sp)
            }

            Spacer(modifier = Modifier.height(10.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(badges) { badge ->
                    BadgeCard(badge)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── 6. Actions & Logout ───────────────────────────────────────────────
        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onNavigateToJournalist,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ForestMid),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.MenuBook, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Journalist Mode (Off-Season Reflection)", fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = {
                    authViewModel.logout()
                    onLogout()
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ErrorRed.copy(alpha = 0.7f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed)
            ) {
                Icon(Icons.Default.Logout, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isGuest) "Switch Account / Log In" else "Sign Out", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun CollectionCard(item: CollectionItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (item.isUnlocked) GoldBright.copy(alpha = 0.5f) else SubtleGray
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (item.isUnlocked) {
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = item.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, ObsidianBlack.copy(alpha = 0.9f))
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(10.dp)
                ) {
                    Text(item.name, fontWeight = FontWeight.Black, color = CreamWhite, fontSize = 12.sp, maxLines = 1)
                    Text("+${item.points} XP", fontWeight = FontWeight.Bold, color = GoldBright, fontSize = 10.sp)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.Lock, null, tint = MutedGray, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(item.name, fontWeight = FontWeight.Bold, color = MutedGray, fontSize = 11.sp, textAlign = TextAlign.Center, maxLines = 1)
                    Text("🔒 Unexplored", style = MaterialTheme.typography.labelSmall, color = MutedGray, fontSize = 9.sp)
                }
            }
        }
    }
}

@Composable
private fun BadgeCard(badge: BadgeData) {
    Card(
        modifier = Modifier.width(100.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, badge.color.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(badge.color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(badge.icon, null, tint = badge.color, modifier = Modifier.size(22.dp))
            }
            Text(
                badge.name,
                style = MaterialTheme.typography.labelSmall,
                color = CreamWhite,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }
    }
}

private data class BadgeData(
    val name: String,
    val icon: ImageVector,
    val color: Color
)
