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
import com.monumentquest.ui.common.UserAvatar
import com.monumentquest.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(
    viewModel: LeaderboardViewModel = hiltViewModel()
) {
    val allUsers by viewModel.users.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }

    val filteredUsers = remember(selectedTab, allUsers) {
        when (selectedTab) {
            0    -> allUsers.take(6)
            1    -> allUsers.filter { it.points > 300 }
            else -> allUsers
        }
    }

    val topThree       = filteredUsers.take(3)
    val remainingUsers = if (filteredUsers.size > 3) filteredUsers.drop(3) else emptyList()

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
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp
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
                    PodiumSection(topThree)
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
                LeaderboardItem(index + 4, user)  // remainingUsers starts after top-3, so rank = index + 4
            }
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
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        if (selected) {
            Box(
                modifier = Modifier
                    .width(16.dp)
                    .height(2.dp)
                    .background(Gold, RoundedCornerShape(1.dp))
            )
        }
    }
}

@Composable
fun PodiumSection(topThree: List<User>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.Bottom
    ) {
        PodiumMember(topThree[1], 2, 68.dp, MedalSilver)
        PodiumMember(topThree[0], 1, 88.dp, MedalGold)
        PodiumMember(topThree[2], 3, 60.dp, MedalBronze)
    }
}

@Composable
fun PodiumMember(
    user: User,
    rank: Int,
    avatarSize: Dp,
    medalColor: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Rank badge above avatar — large enough to be readable
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
                fontSize = 9.sp
            )
        }

        // Avatar with optional gold-tint background for #1
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
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            color = TextPrimary,
            maxLines = 1
        )
        Text(
            "${user.points} XP",
            style = MaterialTheme.typography.bodySmall,
            color = if (rank == 1) Gold else TextSecondary,
            fontSize = 11.sp,
            fontWeight = if (rank == 1) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
fun LeaderboardItem(rankIndex: Int, user: User) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
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
                "$rankIndex",   // rankIndex is already the correct display rank (4, 5, 6…)
                fontWeight = FontWeight.Normal,
                color = TextSecondary,
                fontSize = 14.sp,
                modifier = Modifier.width(24.dp)
            )
            UserAvatar(name = user.name, size = 36.dp, borderColor = Border)

            Text(
                user.name,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f)
            )

            Text(
                "${user.points} pts",
                color = Gold,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
