package com.monumentquest.ui.social

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
            0 -> allUsers.take(6)
            1 -> allUsers.filter { it.points > 300 }
            else -> allUsers
        }
    }

    // Top 3 Podium vs Standings List (#4+)
    val topThree = filteredUsers.take(3)
    val remainingUsers = if (filteredUsers.size > 3) filteredUsers.drop(3) else emptyList()

    Scaffold(
        containerColor = ObsidianBlack,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(ForestDeep.copy(alpha = 0.9f), ObsidianBlack)
                        )
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "HALL OF FAME LEADERBOARDS",
                    style = MaterialTheme.typography.titleMedium,
                    color = GoldBright,
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Black
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(ElevatedSurface)
                        .padding(4.dp)
                ) {
                    TabButton("Friends", selectedTab == 0, Modifier.weight(1f)) { selectedTab = 0 }
                    TabButton("Regional (Odisha)", selectedTab == 1, Modifier.weight(1.3f)) { selectedTab = 1 }
                    TabButton("Global", selectedTab == 2, Modifier.weight(1f)) { selectedTab = 2 }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp)
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
                        HorizontalDivider(modifier = Modifier.weight(1f), color = SubtleGray)
                        Text(
                            text = "  STANDINGS (#4+)  ",
                            style = MaterialTheme.typography.labelSmall,
                            color = MutedGray,
                            letterSpacing = 2.sp
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f), color = SubtleGray)
                    }
                }
            }

            itemsIndexed(remainingUsers) { index, user ->
                LeaderboardItem(index + 3, user) // 0-indexed in remaining starts at #4 (index + 3)
            }
        }
    }
}

@Composable
private fun TabButton(text: String, isSelected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) GoldBright else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) ObsidianBlack else MutedGray,
            fontSize = 11.sp
        )
    }
}

@Composable
fun PodiumSection(topThree: List<User>) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.Bottom
        ) {
            PodiumMember(topThree[1], 2, 68.dp, MedalSilver)
            PodiumMember(topThree[0], 1, 90.dp, MedalGold, isWinner = true)
            PodiumMember(topThree[2], 3, 62.dp, MedalBronze)
        }
    }
}

@Composable
fun PodiumMember(
    user: User,
    rank: Int,
    avatarSize: Dp,
    medalColor: Color,
    isWinner: Boolean = false
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (isWinner) {
            Icon(
                imageVector = Icons.Default.EmojiEvents,
                contentDescription = null,
                tint = GoldShimmer,
                modifier = Modifier.size(26.dp)
            )
        } else {
            Spacer(modifier = Modifier.height(26.dp))
        }

        UserAvatar(name = user.name, size = avatarSize, borderColor = medalColor)

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(medalColor.copy(alpha = 0.2f))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text("#$rank", fontWeight = FontWeight.Black, color = medalColor, fontSize = 11.sp)
        }

        Text(user.name, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = CreamWhite, maxLines = 1)
        Text("${user.points} XP", style = MaterialTheme.typography.bodySmall, color = MutedGray, fontSize = 10.sp)
    }
}

@Composable
fun LeaderboardItem(rankIndex: Int, user: User) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("${rankIndex + 1}", fontWeight = FontWeight.Bold, color = MutedGray, fontSize = 14.sp)
            UserAvatar(name = user.name, size = 36.dp, borderColor = SubtleGray)

            Column(modifier = Modifier.weight(1f)) {
                Text(user.name, fontWeight = FontWeight.Bold, color = CreamWhite, fontSize = 14.sp)
                Text("${user.points} Expedition Points", style = MaterialTheme.typography.bodySmall, color = MutedGray, fontSize = 11.sp)
            }
        }
    }
}
