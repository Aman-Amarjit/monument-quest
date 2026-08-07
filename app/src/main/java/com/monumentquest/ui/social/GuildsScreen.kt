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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
    val guilds by viewModel.guilds.collectAsState()
    val userGuild by viewModel.userGuild.collectAsState()
    val leaderboard by viewModel.guildLeaderboard.collectAsState()

    Scaffold(
        containerColor = ObsidianBlack,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "REGIONAL GUILDS",
                            style = MaterialTheme.typography.titleLarge,
                            color = GoldBright,
                            letterSpacing = 2.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "Unite. Explore. Complete Regional Challenges.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MutedGray,
                            letterSpacing = 1.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                modifier = Modifier.background(
                    Brush.verticalGradient(
                        colors = listOf(ForestDeep.copy(alpha = 0.8f), Color.Transparent)
                    )
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Active guild card
            if (userGuild != null) {
                item {
                    ActiveGuildCard(
                        guildName   = userGuild!!.name,
                        region      = userGuild!!.region,
                        leaderboard = leaderboard.map { it.name to it.points }
                    )
                }
            }

            // Section header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = SubtleGray)
                    Text(
                        text = "  REGIONAL EXPLORATION GUILDS  ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MutedGray,
                        letterSpacing = 2.sp
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f), color = SubtleGray)
                }
            }

            items(guilds) { guild ->
                GuildItem(
                    guild    = guild,
                    isJoined = userGuild?.id == guild.id,
                    onJoin   = { viewModel.joinGuild(guild.id) }
                )
            }

            // Active Guild Regional Challenge Event
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = GlassSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GoldBright.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.EmojiEvents, null, tint = GoldBright, modifier = Modifier.size(20.dp))
                            Text("The Odisha Guild Challenge", fontWeight = FontWeight.Black, color = GoldBright, fontSize = 14.sp)
                        }
                        Text(
                            text = "Visit 500 heritage shrines across Odisha this weekend to claim 2,500 Guild Bonus XP!",
                            style = MaterialTheme.typography.bodySmall,
                            color = CreamWhite
                        )
                        LinearProgressIndicator(
                            progress = { 0.68f },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                            color = GoldBright,
                            trackColor = SubtleGray
                        )
                        Text("Progress: 342 / 500 Shrines Discovered (68%)", style = MaterialTheme.typography.labelSmall, color = MutedGray, fontSize = 10.sp)
                    }
                }
            }

            // Create Custom Guild CTA Card
            item {
                OutlinedButton(
                    onClick = { /* Create Guild */ },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ForestMint),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ForestMint)
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Create Your Own Regional Guild (+200 XP)", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(24.dp))
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
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Brush.linearGradient(listOf(GoldBright.copy(alpha = 0.6f), ForestMid.copy(alpha = 0.4f)))
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            ForestDeep.copy(alpha = 0.7f),
                            GoldDark.copy(alpha = 0.15f)
                        )
                    )
                )
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(GoldBright.copy(alpha = 0.15f))
                            .border(1.5.dp, GoldBright.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Castle,
                            contentDescription = null,
                            tint = GoldBright,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = guildName,
                            style = MaterialTheme.typography.titleMedium,
                            color = GoldBright,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "⚑  $region",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedGray
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(ForestMid.copy(alpha = 0.25f))
                            .border(1.dp, ForestMint.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "MEMBER",
                            style = MaterialTheme.typography.labelSmall,
                            color = ForestMint,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }

                if (leaderboard.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Guild Top Explorers",
                        style = MaterialTheme.typography.labelMedium,
                        color = GoldBright.copy(alpha = 0.7f),
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    leaderboard.take(3).forEachIndexed { i, (name, pts) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                UserAvatar(name = name, size = 26.dp)
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CreamWhite
                                )
                            }
                            Text(
                                text = "$pts pts",
                                style = MaterialTheme.typography.bodySmall,
                                color = GoldBright.copy(alpha = 0.8f),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
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
    val count = guild.memberIds.size
    val memberText = "$count explorer${if (count == 1) "" else "s"}"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        border = if (isJoined) androidx.compose.foundation.BorderStroke(
            1.dp, ForestMid.copy(alpha = 0.4f)
        ) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isJoined) ForestMid.copy(alpha = 0.2f)
                        else ElevatedSurface
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Group,
                    contentDescription = null,
                    tint = if (isJoined) ForestMint else MutedGray,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = guild.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = CreamWhite,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${guild.region} · $memberText",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedGray
                )
            }

            if (!isJoined) {
                Button(
                    onClick = onJoin,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ForestMid,
                        contentColor   = CreamWhite
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Join", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = SuccessGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Joined",
                        style = MaterialTheme.typography.labelMedium,
                        color = SuccessGreen,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
