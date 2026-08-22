package com.monumentquest.ui.social

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Castle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.monumentquest.data.model.Guild
import com.monumentquest.ui.common.UserAvatar
import com.monumentquest.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuildsScreen(
    viewModel: GuildsViewModel = hiltViewModel()
) {
    val guilds      by viewModel.guilds.collectAsState()
    val userGuild   by viewModel.userGuild.collectAsState()
    val leaderboard by viewModel.guildLeaderboard.collectAsState()

    Scaffold(
        containerColor = Bg,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Bg)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Guilds",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp
                )
                HorizontalDivider(color = BorderSubtle, modifier = Modifier.padding(top = 12.dp))
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (userGuild != null) {
                item {
                    ActiveGuildCard(
                        guildName   = userGuild!!.name,
                        region      = userGuild!!.region,
                        leaderboard = leaderboard.map { it.name to it.points }
                    )
                }
            }

            item {
                Text(
                    text = "AVAILABLE GUILDS",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            items(guilds) { guild ->
                GuildItem(
                    guild    = guild,
                    isJoined = userGuild?.id == guild.id,
                    onJoin   = { viewModel.joinGuild(guild.id) }
                )
            }

            // Challenge card
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Surface1),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Border)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.EmojiEvents,
                                null,
                                tint = Gold,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                "The Odisha Guild Challenge",
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary,
                                fontSize = 14.sp
                            )
                        }
                        Text(
                            text = "Visit 500 heritage shrines across Odisha this weekend to claim 2,500 Guild Bonus XP",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                        LinearProgressIndicator(
                            progress = { 0.68f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = Gold,
                            trackColor = Surface3
                        )
                        Text(
                            "342 / 500 Shrines (68%)",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Create guild button
            item {
                OutlinedButton(
                    onClick = { /* Create Guild */ },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Border),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Surface2,
                        contentColor   = TextPrimary
                    )
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Create Your Own Guild", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun ActiveGuildCard(
    guildName: String,
    region: String,
    leaderboard: List<Pair<String, Int>>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Surface1),
        border = androidx.compose.foundation.BorderStroke(1.dp, Border)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Surface2)
                        .border(1.dp, Border, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Castle,
                        contentDescription = null,
                        tint = Gold,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = guildName,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp
                    )
                    Text(
                        text = region,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Surface2)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Member",
                        style = MaterialTheme.typography.labelSmall,
                        color = GreenAccent,
                        fontSize = 11.sp
                    )
                }
            }

            if (leaderboard.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = Border)
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "TOP EXPLORERS",
                    color = TextSecondary,
                    fontSize = 10.sp,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                leaderboard.take(3).forEachIndexed { i, (name, pts) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            UserAvatar(name = name, size = 24.dp)
                            Text(
                                text = name,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextPrimary,
                                fontSize = 13.sp
                            )
                        }
                        Text(
                            text = "$pts pts",
                            style = MaterialTheme.typography.bodySmall,
                            color = Gold,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GuildItem(
    guild: Guild,
    isJoined: Boolean,
    onJoin: () -> Unit
) {
    val count      = guild.memberIds.size
    val memberText = "$count explorer${if (count == 1) "" else "s"}"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Surface1),
        border = androidx.compose.foundation.BorderStroke(1.dp, Border)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Surface2),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Group,
                    contentDescription = null,
                    tint = if (isJoined) GreenAccent else TextSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = guild.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${guild.region} · $memberText",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            if (!isJoined) {
                OutlinedButton(
                    onClick = onJoin,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Border),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Surface2,
                        contentColor   = TextPrimary
                    )
                ) {
                    Text("Join", fontWeight = FontWeight.Medium, fontSize = 12.sp)
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = GreenAccent,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "Joined",
                        style = MaterialTheme.typography.labelMedium,
                        color = GreenAccent
                    )
                }
            }
        }
    }
}
