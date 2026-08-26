package com.monumentquest.ui.social

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.monumentquest.data.model.User
import com.monumentquest.ui.common.ExplorerProfileData
import com.monumentquest.ui.common.PublicProfileDialog
import com.monumentquest.ui.common.UserAvatar
import com.monumentquest.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(
    viewModel: LeaderboardViewModel = hiltViewModel()
) {
    val allUsers by viewModel.users.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }
    var activeProfile by remember { mutableStateOf<ExplorerProfileData?>(null) }
    var followedUserIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    val filteredUsers = remember(selectedTab, allUsers) {
        when (selectedTab) {
            0    -> allUsers.take(6)
            1    -> allUsers.filter { it.points > 300 }
            else -> allUsers
        }
    }

    val topThree       = filteredUsers.take(3)
    val remainingUsers = if (filteredUsers.size > 3) filteredUsers.drop(3) else emptyList()

    fun openUserProfile(user: User, rankDisplay: Int) {
        val userRankTitle = when {
            user.points >= 1000 -> "Grand Master Explorer"
            user.points >= 500 -> "Master Pathfinder"
            else -> "Heritage Explorer"
        }
        activeProfile = ExplorerProfileData(
            userId = user.id,
            name = user.name,
            username = "@${user.name.lowercase().replace(" ", "_")}",
            rank = userRankTitle,
            xp = user.points,
            visitedCount = (user.points / 150) + 3,
            distanceKm = 12.5 + (user.points / 100.0),
            guildName = "Kalinga Keepers",
            bio = "Master explorer competing in global leaderboard rankings!"
        )
    }

    Scaffold(
        containerColor = Bg,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Bg)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Leaderboard",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )

                // Tab row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    LeaderboardTab("Friends",          selected = selectedTab == 0) { selectedTab = 0 }
                    LeaderboardTab("Regional (Odisha)", selected = selectedTab == 1) { selectedTab = 1 }
                    LeaderboardTab("Global",           selected = selectedTab == 2) { selectedTab = 2 }
                }

                HorizontalDivider(color = BorderSubtle)
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(top = 8.dp, bottom = 140.dp)
        ) {
            if (topThree.size >= 3) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    PodiumSection(topThree, onMemberClick = { user, rank -> openUserProfile(user, rank) })
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = Border)
                        Text(
                            text = "  STANDINGS  ",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            letterSpacing = 1.sp,
                            fontSize = 10.sp
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f), color = Border)
                    }
                }
            }

            itemsIndexed(remainingUsers) { index, user ->
                LeaderboardItem(
                    rankIndex = index + 4,
                    user = user,
                    onClick = { openUserProfile(user, index + 4) }
                )
            }
        }

        activeProfile?.let { prof ->
            PublicProfileDialog(
                profile = prof,
                isFollowed = followedUserIds.contains(prof.userId),
                onFollowToggle = {
                    followedUserIds = if (followedUserIds.contains(prof.userId)) {
                        followedUserIds - prof.userId
                    } else {
                        followedUserIds + prof.userId
                    }
                },
                onDismiss = { activeProfile = null }
            )
        }
    }
}

@Composable
private fun LeaderboardTab(text: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = text,
            color = if (selected) TextPrimary else TextSecondary,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 13.5.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        if (selected) {
            Box(
                modifier = Modifier
                    .width(20.dp)
                    .height(2.5.dp)
                    .background(Gold, RoundedCornerShape(1.dp))
            )
        }
    }
}

@Composable
fun PodiumSection(topThree: List<User>, onMemberClick: (User, Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.Bottom
    ) {
        PodiumMember(topThree[1], 2, 68.dp, MedalSilver, onClick = { onMemberClick(topThree[1], 2) })
        PodiumMember(topThree[0], 1, 88.dp, MedalGold, onClick = { onMemberClick(topThree[0], 1) })
        PodiumMember(topThree[2], 3, 60.dp, MedalBronze, onClick = { onMemberClick(topThree[2], 3) })
    }
}

@Composable
fun PodiumMember(
    user: User,
    rank: Int,
    avatarSize: Dp,
    medalColor: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(medalColor.copy(alpha = 0.18f))
                .border(1.dp, medalColor.copy(alpha = 0.6f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "#$rank",
                fontWeight = FontWeight.Bold,
                color = medalColor,
                fontSize = 10.sp
            )
        }

        Box(
            modifier = if (rank == 1)
                Modifier
                    .clip(CircleShape)
                    .background(GoldTint)
                    .padding(4.dp)
            else
                Modifier
        ) {
            UserAvatar(name = user.name, size = avatarSize, borderColor = medalColor)
        }

        Text(
            user.name,
            fontWeight = FontWeight.Bold,
            fontSize = 12.5.sp,
            color = TextPrimary,
            maxLines = 1
        )
        Text(
            "${user.points} XP",
            style = MaterialTheme.typography.bodySmall,
            color = if (rank == 1) Gold else TextSecondary,
            fontSize = 11.5.sp,
            fontWeight = if (rank == 1) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun LeaderboardItem(rankIndex: Int, user: User, onClick: () -> Unit) {
    val userRankTitle = when {
        user.points >= 1000 -> "Grand Master"
        user.points >= 500 -> "Pathfinder"
        else -> "Explorer"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Surface1),
        border = androidx.compose.foundation.BorderStroke(1.dp, Border)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "#$rankIndex",
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                fontSize = 13.sp,
                modifier = Modifier.width(28.dp)
            )
            UserAvatar(name = user.name, size = 38.dp, borderColor = Gold)

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    user.name,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 14.sp
                )
                Text(
                    userRankTitle,
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }

            Text(
                "${user.points} XP",
                color = Gold,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}
