package com.monumentquest.ui.map

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.monumentquest.data.model.MonumentPost
import com.monumentquest.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonumentWallScreen(
    monumentId: String,
    viewModel: MonumentWallViewModel = hiltViewModel()
) {
    val posts      by viewModel.posts.collectAsState()
    val isCustodian by viewModel.isCustodian.collectAsState()
    var commentText by remember { mutableStateOf("") }

    LaunchedEffect(monumentId) { viewModel.fetchPosts(monumentId) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack)
    ) {
        // ── Top Bar ──────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(ForestDeep.copy(alpha = 0.9f), Color.Transparent)
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text(
                        text = "Monument Wall",
                        style = MaterialTheme.typography.titleMedium,
                        color = CreamWhite,
                        fontWeight = FontWeight.Bold
                    )
                    if (isCustodian) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = GoldBright,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "Custodian View",
                                style = MaterialTheme.typography.labelSmall,
                                color = GoldBright,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }
        }

        // ── Posts ────────────────────────────────────────────────────────────
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (posts.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("✦", fontSize = 32.sp, color = GoldBright.copy(alpha = 0.4f))
                            Text(
                                text = "No posts yet",
                                style = MaterialTheme.typography.titleSmall,
                                color = MutedGray
                            )
                            Text(
                                text = "Be the first to share your experience",
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedGray.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }

            items(posts) { post ->
                PostItem(post = post, isCustodian = isCustodian, onDelete = { viewModel.deletePost(post.id, monumentId) })
            }
        }

        // ── Comment Input ─────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, NightSurface.copy(alpha = 0.98f))
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text("Share your experience…", color = MutedGray, fontSize = 14.sp)
                    },
                    shape = RoundedCornerShape(24.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor   = ElevatedSurface,
                        unfocusedContainerColor = CardSurface,
                        focusedTextColor        = CreamWhite,
                        unfocusedTextColor      = CreamWhite,
                        cursorColor             = GoldBright,
                        focusedIndicatorColor   = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    singleLine = true
                )

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            if (commentText.isNotBlank())
                                Brush.linearGradient(listOf(ForestMid, GoldBright))
                            else
                                Brush.linearGradient(listOf(SubtleGray, SubtleGray))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = {
                            if (commentText.isNotBlank()) {
                                viewModel.addPost(monumentId, commentText)
                                commentText = ""
                            }
                        },
                        enabled = commentText.isNotBlank()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send",
                            tint = if (commentText.isNotBlank()) CreamWhite else MutedGray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PostItem(
    post: MonumentPost,
    isCustodian: Boolean,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(ForestMid.copy(alpha = 0.2f))
                        .border(1.dp, ForestMint.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = ForestMint,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = post.userName,
                        fontWeight = FontWeight.Bold,
                        color = CreamWhite,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                if (isCustodian) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(GoldBright.copy(alpha = 0.12f))
                                .border(1.dp, GoldBright.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = GoldBright,
                                    modifier = Modifier.size(10.dp)
                                )
                                Text(
                                    text = "MOD",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = GoldBright,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }

                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = ErrorRed.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = post.content,
                style = MaterialTheme.typography.bodyMedium,
                color = ParchmentLight,
                lineHeight = 20.sp
            )
        }
    }
}
