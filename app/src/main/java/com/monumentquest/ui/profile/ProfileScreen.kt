package com.monumentquest.ui.profile

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
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.LocalLibrary
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
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
        CollectionItem("c1", "Lingaraj Temple",   "Odisha", 500,  true,  "https://images.unsplash.com/photo-1627894483216-2138af692e32?q=80&w=800&auto=format&fit=crop"),
        CollectionItem("c2", "Mukteshvara Temple","Odisha", 450,  true,  "https://images.unsplash.com/photo-1600585154340-be6161a56a0c?q=80&w=800&auto=format&fit=crop"),
        CollectionItem("c3", "Dhauli Shanti Stupa","Odisha",600, true,  "https://images.unsplash.com/photo-1590050752117-238cb0fb12b1?q=80&w=800&auto=format&fit=crop"),
        CollectionItem("c4", "Rajarani Temple",   "Odisha", 400,  false, ""),
        CollectionItem("c5", "Khandagiri Caves",  "Odisha", 550,  false, ""),
        CollectionItem("c6", "Konark Sun Temple", "Odisha", 1000, false, "")
    )

    val unlockedCount = collectionItems.count { it.isUnlocked }
    val collectionProgress = if (collectionItems.isEmpty()) 0f else unlockedCount.toFloat() / collectionItems.size.toFloat()

    val badges = listOf(
        BadgeData("First Discovery", Icons.Default.Explore,                   Gold),
        BadgeData("Nature Lover",    Icons.Default.Landscape,                 GreenAccent),
        BadgeData("History Buff",    Icons.Default.LocalLibrary,              RedAccent),
        BadgeData("Mountaineer",     Icons.Default.Terrain,                   BlueAccent),
        BadgeData("Scholar",         Icons.AutoMirrored.Filled.MenuBook,      TextSecondary)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
    ) {
        // ── Header ──────────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 28.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Surface2)
                    .border(1.5.dp, Border, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isGuest) Icons.Default.Shield else Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = Gold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = userName,
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 20.sp
                )
                if (isGuest) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Surface2)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            "Guest",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = rank,
                color = Gold,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = userEmail,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                fontSize = 11.sp
            )
        }

        // ── Stats Row ────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatChip(label = "Monuments", value = "3", modifier = Modifier.weight(1f))
            StatChip(label = "XP", value = "$points", modifier = Modifier.weight(1f))
            StatChip(label = "Rank", value = "#42", modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Custodian Status ──────────────────────────────────────────────────
        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Surface1),
                border = androidx.compose.foundation.BorderStroke(1.dp, Border)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(GoldTint),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Star,
                            null,
                            tint = Gold,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Grand Custodian Status",
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                            fontSize = 13.sp
                        )
                        Text(
                            "Lingaraj Temple · +50 Heritage Coins/day",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Surface1),
                border = androidx.compose.foundation.BorderStroke(1.dp, Border)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Surface2),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.School, null, tint = GreenAccent, modifier = Modifier.size(20.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Mentor Pair Program",
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                            fontSize = 13.sp
                        )
                        Text(
                            "Paired with Dr. Subhashree (Lvl 42) · +1.5x Guided XP",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Regional Progress ─────────────────────────────────────────────────
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Text(
                "REGIONAL PROGRESS",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 1.2.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Surface1),
                border = androidx.compose.foundation.BorderStroke(1.dp, Border)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Odisha Temples & Shrines",
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary,
                            fontSize = 13.sp
                        )
                        Text(
                            "${unlockedCount} / ${collectionItems.size}",
                            fontWeight = FontWeight.SemiBold,
                            color = Gold,
                            fontSize = 12.sp
                        )
                    }
                    LinearProgressIndicator(
                        progress = { collectionProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = Gold,
                        trackColor = Surface3
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Collection ────────────────────────────────────────────────────────
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Text(
                "COLLECTION",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 1.2.sp
            )
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

        // ── Badges ────────────────────────────────────────────────────────────
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Text(
                "BADGES",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 1.2.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(badges) { badge ->
                    BadgeCard(badge)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Actions ───────────────────────────────────────────────────────────
        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onNavigateToJournalist,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Gold,
                    contentColor   = Bg
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.MenuBook, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Journalist Mode", fontWeight = FontWeight.SemiBold)
            }

            OutlinedButton(
                onClick = {
                    authViewModel.logout()
                    onLogout()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Border),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Surface1,
                    contentColor   = RedAccent
                )
            ) {
                Icon(Icons.Default.Logout, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isGuest) "Switch Account" else "Sign Out", fontWeight = FontWeight.Medium)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun StatChip(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Surface1),
        border = androidx.compose.foundation.BorderStroke(1.dp, Border)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontWeight = FontWeight.SemiBold, color = TextPrimary, fontSize = 16.sp)
            Text(label, color = TextSecondary, fontSize = 11.sp)
        }
    }
}

@Composable
private fun CollectionCard(item: CollectionItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Surface1),
        border = androidx.compose.foundation.BorderStroke(1.dp, Border)
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
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Bg.copy(alpha = 0.85f))
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(10.dp)
                ) {
                    Text(item.name, fontWeight = FontWeight.SemiBold, color = TextPrimary, fontSize = 12.sp, maxLines = 1)
                    Text("+${item.points} XP", fontWeight = FontWeight.Medium, color = Gold, fontSize = 10.sp)
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            null,
                            tint = TextTertiary.copy(alpha = 0.4f),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            item.name,
                            fontWeight = FontWeight.Medium,
                            color = TextTertiary,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BadgeCard(badge: BadgeData) {
    Card(
        modifier = Modifier
            .width(72.dp)
            .height(88.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Surface1),
        border = androidx.compose.foundation.BorderStroke(1.dp, Border)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(badge.icon, null, tint = badge.color, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                badge.name,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                maxLines = 2,
                fontSize = 9.sp
            )
        }
    }
}

private data class BadgeData(
    val name: String,
    val icon: ImageVector,
    val color: Color
)
