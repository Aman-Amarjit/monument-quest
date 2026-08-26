package com.monumentquest.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.monumentquest.ui.theme.*

data class ExplorerProfileData(
    val userId: String,
    val name: String,
    val avatarUrl: String? = null,
    val username: String = "@explorer",
    val rank: String = "Heritage Explorer",
    val xp: Int = 500,
    val visitedCount: Int = 3,
    val distanceKm: Double = 5.2,
    val guildName: String = "Kalinga Keepers",
    val badges: List<String> = listOf("Deula Pioneer", "Kalinga Keeper", "Temple Scholar"),
    val bio: String = "Passionate heritage explorer mapping temple architecture across Odisha. 🛕📜"
)

@Composable
fun PublicProfileDialog(
    profile: ExplorerProfileData,
    isFollowed: Boolean,
    onFollowToggle: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .border(1.5.dp, Gold, RoundedCornerShape(24.dp)),
            color = Surface1
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top header row: Close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }
                }

                // Avatar with Gold Ring & Badge
                Box(contentAlignment = Alignment.BottomEnd) {
                    UserAvatar(
                        name = profile.name,
                        avatarUrl = profile.avatarUrl,
                        size = 76.dp,
                        borderColor = Gold
                    )
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Gold)
                            .border(2.dp, Surface1, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Verified, null, tint = Bg, modifier = Modifier.size(14.dp))
                    }
                }

                Spacer(Modifier.height(10.dp))

                Text(
                    text = profile.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary
                )

                Text(
                    text = profile.username,
                    fontSize = 12.sp,
                    color = TextSecondary
                )

                Spacer(Modifier.height(6.dp))

                // Rank Chip
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xFF2A1C00), Color(0xFF1E1400))
                            )
                        )
                        .border(1.dp, Gold.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.MilitaryTech, null, tint = Gold, modifier = Modifier.size(14.dp))
                        Text(
                            profile.rank,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Gold
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Bio
                Text(
                    text = profile.bio,
                    fontSize = 11.5.sp,
                    color = TextPrimary,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(Modifier.height(14.dp))

                // Stats Grid: XP, Visited, Distance
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Surface2)
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem(label = "XP Points", value = "${profile.xp} XP", icon = Icons.Default.ElectricBolt, iconColor = Gold)
                    Box(modifier = Modifier.width(1.dp).height(32.dp).background(BorderSubtle))
                    StatItem(label = "Monuments", value = "${profile.visitedCount}", icon = Icons.Default.Place, iconColor = Color(0xFF38BDF8))
                    Box(modifier = Modifier.width(1.dp).height(32.dp).background(BorderSubtle))
                    StatItem(label = "Walked", value = "${profile.distanceKm} km", icon = Icons.Default.DirectionsWalk, iconColor = Color(0xFF4ADE80))
                }

                Spacer(Modifier.height(14.dp))

                // Badges section
                Text(
                    "Explorer Badges",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start
                )

                Spacer(Modifier.height(6.dp))

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(profile.badges) { badge ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Surface2)
                                .border(1.dp, Border, RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.WorkspacePremium, null, tint = Gold, modifier = Modifier.size(13.dp))
                                Text(badge, fontSize = 11.sp, color = TextPrimary, fontWeight = FontWeight.Medium, maxLines = 1)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))

                // Action buttons: Follow + Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onFollowToggle,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isFollowed) Surface2 else Gold,
                            contentColor = if (isFollowed) TextPrimary else Bg
                        )
                    ) {
                        Icon(
                            if (isFollowed) Icons.Default.Check else Icons.Default.PersonAdd,
                            null,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            if (isFollowed) "Following" else "Follow Explorer",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Border),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Surface2,
                            contentColor = TextSecondary
                        )
                    ) {
                        Text("Close", fontWeight = FontWeight.Medium, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(icon, null, tint = iconColor, modifier = Modifier.size(13.dp))
            Text(value, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
        }
        Text(label, fontSize = 10.sp, color = TextSecondary)
    }
}
