package com.monumentquest.ui.social

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.monumentquest.data.model.DiscovererStory
import com.monumentquest.data.model.SocialPost
import com.monumentquest.ui.common.EmptyStateView
import com.monumentquest.ui.common.UserAvatar
import com.monumentquest.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialFeedScreen(
    onNavigateToCamera: () -> Unit,
    onNavigateToNarrator: (String) -> Unit,
    onNavigateToWall: (String) -> Unit,
    viewModel: SocialFeedViewModel = hiltViewModel()
) {
    val posts by viewModel.posts.collectAsState()
    val stories by viewModel.stories.collectAsState()
    val currentFilter by viewModel.currentFilter.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val followedUsers by viewModel.followedUsers.collectAsState()
    val savedPostIds by viewModel.savedPostIds.collectAsState()
    val postCommentsMap by viewModel.postComments.collectAsState()

    var showCreateModal by remember { mutableStateOf(false) }
    var activeCommentsPostId by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = ObsidianBlack,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(ForestDeep.copy(alpha = 0.95f), ObsidianBlack)
                        )
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Editorial Human Top Title Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "CHRONICLES",
                            style = MaterialTheme.typography.titleLarge,
                            color = GoldBright,
                            letterSpacing = 2.5.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "Live Explorer Expeditions & Discoveries",
                            style = MaterialTheme.typography.labelSmall,
                            color = MutedGray,
                            fontSize = 11.sp
                        )
                    }

                    FloatingActionButton(
                        onClick = { showCreateModal = true },
                        containerColor = GoldBright,
                        contentColor = ObsidianBlack,
                        shape = CircleShape,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Create Log", modifier = Modifier.size(22.dp))
                    }
                }

                // Professional Search Input Bar
                TextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    placeholder = {
                        Text("Search monuments, explorers, or #hashtags…", color = MutedGray, fontSize = 12.sp)
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = GoldBright, modifier = Modifier.size(18.dp))
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Close, null, tint = MutedGray, modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    shape = RoundedCornerShape(24.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor   = CardSurface,
                        unfocusedContainerColor = ElevatedSurface,
                        focusedTextColor        = CreamWhite,
                        unfocusedTextColor      = CreamWhite,
                        cursorColor             = GoldBright,
                        focusedIndicatorColor   = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    singleLine = true
                )

                // Tab Filter Chips
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = currentFilter == FeedFilter.GLOBAL,
                        onClick = { viewModel.setFilter(FeedFilter.GLOBAL) },
                        label = { Text("Global Feed", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GoldBright,
                            selectedLabelColor = ObsidianBlack
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                    FilterChip(
                        selected = currentFilter == FeedFilter.GUILD,
                        onClick = { viewModel.setFilter(FeedFilter.GUILD) },
                        label = { Text("My Guild", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ForestMid,
                            selectedLabelColor = CreamWhite
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                StoriesBar(stories = stories, onAddStory = { showCreateModal = true })
            }

            if (posts.isEmpty()) {
                item {
                    EmptyStateView(
                        icon = Icons.Default.Search,
                        title = "No Expeditions Found",
                        subtitle = "Try searching for a different monument, location, or explorer."
                    )
                }
            } else {
                items(posts, key = { it.id }) { post ->
                    val isFollowed = followedUsers.contains(post.userId)
                    val isSaved = savedPostIds.contains(post.id)

                    HumanPostCard(
                        post = post,
                        isFollowed = isFollowed,
                        isSaved = isSaved,
                        onLikeToggle = { viewModel.toggleLike(post.id) },
                        onFollowToggle = { viewModel.toggleFollow(post.userId) },
                        onSaveToggle = { viewModel.toggleSavePost(post.id) },
                        onOpenComments = { activeCommentsPostId = post.id },
                        onNavigateToNarrator = onNavigateToNarrator
                    )
                }
            }
        }

        if (showCreateModal) {
            CreatePostModal(
                onDismiss = { showCreateModal = false },
                onSubmit = { caption, monument ->
                    viewModel.createPost(caption, monument)
                    showCreateModal = false
                }
            )
        }

        activeCommentsPostId?.let { postId ->
            val commentsList = postCommentsMap[postId] ?: emptyList()
            CommentsSheetModal(
                comments = commentsList,
                onDismiss = { activeCommentsPostId = null },
                onAddComment = { text -> viewModel.addComment(postId, text) }
            )
        }
    }
}

@Composable
private fun StoriesBar(
    stories: List<DiscovererStory>,
    onAddStory: () -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.clickable { onAddStory() }
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(ElevatedSurface)
                        .border(1.5.dp, GoldBright, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.AddAPhoto, contentDescription = "Add Check-in", tint = GoldBright, modifier = Modifier.size(24.dp))
                }
                Text("Log Visit", style = MaterialTheme.typography.labelSmall, color = MutedGray, fontSize = 10.sp)
            }
        }

        items(stories) { story ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                UserAvatar(name = story.userName, size = 64.dp, borderColor = GoldBright)
                Text(story.userName.split(" ")[0], style = MaterialTheme.typography.labelSmall, color = CreamWhite, fontSize = 10.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun HumanPostCard(
    post: SocialPost,
    isFollowed: Boolean,
    isSaved: Boolean,
    onLikeToggle: () -> Unit,
    onFollowToggle: () -> Unit,
    onSaveToggle: () -> Unit,
    onOpenComments: () -> Unit,
    onNavigateToNarrator: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    UserAvatar(name = post.userName, size = 44.dp)
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(post.userName, fontWeight = FontWeight.Bold, color = CreamWhite, fontSize = 14.sp)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isFollowed) ForestMid.copy(alpha = 0.3f) else GoldBright.copy(alpha = 0.2f))
                                    .clickable { onFollowToggle() }
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (isFollowed) "Following" else "+ Follow",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isFollowed) ForestMint else GoldBright
                                )
                            }
                        }
                        Text("${post.userRank} · ${post.timestampFormatted}", style = MaterialTheme.typography.labelSmall, color = MutedGray, fontSize = 11.sp)
                    }
                }

                val (tagColor, tagText) = when (post.postType) {
                    "DISCOVERY" -> Pair(GoldBright, "✦ DISCOVERY")
                    "TIME_CAPSULE" -> Pair(Color(0xFF8E44AD), "📜 TIME CAPSULE")
                    "REFLECTION" -> Pair(EmberMid, "✍️ REFLECTION")
                    else -> Pair(ForestMint, "📍 CHECK-IN")
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(tagColor.copy(alpha = 0.15f))
                        .border(1.dp, tagColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(tagText, style = MaterialTheme.typography.labelSmall, color = tagColor, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = post.caption,
                style = MaterialTheme.typography.bodyMedium,
                color = CreamWhite,
                lineHeight = 22.sp
            )

            post.imageUrl?.let { url ->
                Spacer(modifier = Modifier.height(12.dp))
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        modifier = Modifier.clickable { onLikeToggle() },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (post.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Like",
                            tint = if (post.isLiked) ErrorRed else MutedGray,
                            modifier = Modifier.size(20.dp)
                        )
                        Text("${post.likesCount}", style = MaterialTheme.typography.labelSmall, color = MutedGray, fontWeight = FontWeight.Bold)
                    }

                    Row(
                        modifier = Modifier.clickable { onOpenComments() },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.ChatBubbleOutline, contentDescription = "Comments", tint = MutedGray, modifier = Modifier.size(20.dp))
                        Text("${post.commentsCount}", style = MaterialTheme.typography.labelSmall, color = MutedGray, fontWeight = FontWeight.Bold)
                    }

                    IconButton(onClick = onSaveToggle, modifier = Modifier.size(20.dp)) {
                        Icon(
                            imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Save",
                            tint = if (isSaved) GoldBright else MutedGray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                OutlinedButton(
                    onClick = { onNavigateToNarrator(post.monumentName) },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SubtleGray)
                ) {
                    Icon(Icons.Default.Explore, null, tint = GoldBright, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("AI Narrator", color = GoldBright, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun CommentsSheetModal(
    comments: List<PostComment>,
    onDismiss: () -> Unit,
    onAddComment: (String) -> Unit
) {
    var commentText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardSurface,
        title = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Expedition Comments", fontWeight = FontWeight.Black, color = CreamWhite, fontSize = 16.sp)
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, null, tint = MutedGray, modifier = Modifier.size(16.dp))
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 280.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (comments.isEmpty()) {
                        item {
                            Text("No comments yet. Be the first explorer to comment!", style = MaterialTheme.typography.bodySmall, color = MutedGray)
                        }
                    } else {
                        items(comments) { comment ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                                UserAvatar(name = comment.userName, size = 28.dp)
                                Column {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text(comment.userName, fontWeight = FontWeight.Bold, color = CreamWhite, fontSize = 12.sp)
                                        Text(comment.timeAgo, style = MaterialTheme.typography.labelSmall, color = MutedGray, fontSize = 9.sp)
                                    }
                                    Text(comment.text, style = MaterialTheme.typography.bodySmall, color = ParchmentLight, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedTextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        placeholder = { Text("Add a comment…", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(20.dp),
                        singleLine = true
                    )
                    IconButton(
                        onClick = {
                            if (commentText.isNotBlank()) {
                                onAddComment(commentText)
                                commentText = ""
                            }
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(GoldBright)
                    ) {
                        Icon(Icons.Default.Send, null, tint = ObsidianBlack, modifier = Modifier.size(16.dp))
                    }
                }
            }
        },
        confirmButton = {}
    )
}

@Composable
private fun CreatePostModal(
    onDismiss: () -> Unit,
    onSubmit: (String, String) -> Unit
) {
    var caption by remember { mutableStateOf("") }
    var monumentName by remember { mutableStateOf("Lingaraj Temple") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardSurface,
        title = {
            Text("Create Expedition Log", fontWeight = FontWeight.Black, color = CreamWhite)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = monumentName,
                    onValueChange = { monumentName = it },
                    label = { Text("Monument Site") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = caption,
                    onValueChange = { caption = it },
                    label = { Text("Share your historical discovery…") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(caption, monumentName) },
                colors = ButtonDefaults.buttonColors(containerColor = GoldBright, contentColor = ObsidianBlack),
                enabled = caption.isNotBlank()
            ) {
                Text("Publish Log", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MutedGray)
            }
        }
    )
}
