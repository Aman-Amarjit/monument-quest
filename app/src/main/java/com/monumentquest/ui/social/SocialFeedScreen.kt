package com.monumentquest.ui.social

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
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    val posts           by viewModel.posts.collectAsState()
    val stories         by viewModel.stories.collectAsState()
    val currentFilter   by viewModel.currentFilter.collectAsState()
    val searchQuery     by viewModel.searchQuery.collectAsState()
    val followedUsers   by viewModel.followedUsers.collectAsState()
    val savedPostIds    by viewModel.savedPostIds.collectAsState()
    val postCommentsMap by viewModel.postComments.collectAsState()

    var showCreateModal      by remember { mutableStateOf(false) }
    var activeCommentsPostId by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = Bg,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Bg)
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Top row: title + FAB
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Chronicles",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp
                    )

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Gold)
                            .clickable { showCreateModal = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Create Log",
                            tint = Bg,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Search bar — use OutlinedTextField to avoid height-clipping cursor issue
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text("Search monuments, explorers…", color = TextSecondary, fontSize = 13.sp)
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Search, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Close, null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor    = Border,
                        unfocusedBorderColor  = Border,
                        focusedContainerColor = Surface1,
                        unfocusedContainerColor = Surface1,
                        focusedTextColor      = TextPrimary,
                        unfocusedTextColor    = TextPrimary,
                        cursorColor           = Gold
                    ),
                    singleLine = true
                )

                // Filter tabs
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    FeedTabItem(
                        text = "Global",
                        selected = currentFilter == FeedFilter.GLOBAL,
                        onClick = { viewModel.setFilter(FeedFilter.GLOBAL) }
                    )
                    FeedTabItem(
                        text = "My Guild",
                        selected = currentFilter == FeedFilter.GUILD,
                        onClick = { viewModel.setFilter(FeedFilter.GUILD) }
                    )
                }

                // Bottom border
                HorizontalDivider(color = BorderSubtle)
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(top = 8.dp, bottom = 140.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                    val isSaved    = savedPostIds.contains(post.id)

                    PostCard(
                        post               = post,
                        isFollowed         = isFollowed,
                        isSaved            = isSaved,
                        onLikeToggle       = { viewModel.toggleLike(post.id) },
                        onFollowToggle     = { viewModel.toggleFollow(post.userId) },
                        onSaveToggle       = { viewModel.toggleSavePost(post.id) },
                        onOpenComments     = { activeCommentsPostId = post.id },
                        onNavigateToNarrator = onNavigateToNarrator
                    )
                }
            }
        }

        if (showCreateModal) {
            CreatePostModal(
                onDismiss = { showCreateModal = false },
                onSubmit  = { caption, monument ->
                    viewModel.createPost(caption, monument)
                    showCreateModal = false
                }
            )
        }

        activeCommentsPostId?.let { postId ->
            val commentsList = postCommentsMap[postId] ?: emptyList()
            CommentsSheetModal(
                comments     = commentsList,
                onDismiss    = { activeCommentsPostId = null },
                onAddComment = { text -> viewModel.addComment(postId, text) }
            )
        }
    }
}

@Composable
private fun FeedTabItem(text: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.Start,
        modifier = Modifier
            .width(IntrinsicSize.Min)
            .clickable { onClick() }
            .padding(bottom = 2.dp)
    ) {
        Text(
            text = text,
            color = if (selected) TextPrimary else TextSecondary,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(5.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(
                    color = if (selected) Gold else Color.Transparent,
                    shape = RoundedCornerShape(1.dp)
                )
        )
    }
}

@Composable
private fun StoriesBar(
    stories: List<DiscovererStory>,
    onAddStory: () -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.clickable { onAddStory() }
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Surface2)
                        .border(1.dp, Border, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.AddAPhoto,
                        contentDescription = "Add",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text("Add", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 10.sp)
            }
        }

        items(stories) { story ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Gold ring on story avatars so they're visible
                UserAvatar(name = story.userName, size = 52.dp, borderColor = Gold)
                Text(
                    story.userName.split(" ")[0],
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun PostCard(
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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Surface1),
        border = androidx.compose.foundation.BorderStroke(1.dp, Border)
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
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    UserAvatar(name = post.userName, size = 40.dp)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            post.userName,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary,
                            fontSize = 14.sp
                        )
                        Text(
                            "${post.userRank} · ${post.timestampFormatted}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                val (tagBg, tagFg, tagLabel) = when (post.postType) {
                    "DISCOVERY"    -> Triple(Color(0xFF1A1200), Gold,                  "Discovery")
                    "TIME_CAPSULE" -> Triple(Color(0xFF160A24), Color(0xFFB06EE8),     "Time Capsule")
                    "REFLECTION"   -> Triple(Color(0xFF1A0A00), Color(0xFFFF8C42),     "Reflection")
                    else           -> Triple(Surface3,          TextSecondary,         "Check-in")
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(tagBg)
                        .border(1.dp, tagFg.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(tagLabel, color = tagFg, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = post.caption,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                fontSize = 14.sp,
                lineHeight = 22.sp
            )

            post.imageUrl?.let { url ->
                Spacer(modifier = Modifier.height(10.dp))
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.clickable { onLikeToggle() },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (post.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Like",
                            tint = if (post.isLiked) RedAccent else TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            "${post.likesCount}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }

                    Row(
                        modifier = Modifier.clickable { onOpenComments() },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.ChatBubbleOutline,
                            contentDescription = "Comments",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            "${post.commentsCount}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }

                    IconButton(onClick = onSaveToggle, modifier = Modifier.size(18.dp)) {
                        Icon(
                            imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Save",
                            tint = if (isSaved) Gold else TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                OutlinedButton(
                    onClick = { onNavigateToNarrator(post.monumentName) },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    modifier = Modifier.height(28.dp),
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Border),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Surface2,
                        contentColor   = TextSecondary
                    )
                ) {
                    Icon(Icons.Default.Explore, null, tint = Gold, modifier = Modifier.size(11.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("AI Narrator", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
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
        containerColor = Surface1,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Comments", fontWeight = FontWeight.SemiBold, color = TextPrimary, fontSize = 16.sp)
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, null, tint = TextSecondary, modifier = Modifier.size(16.dp))
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
                            Text(
                                "No comments yet.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    } else {
                        items(comments) { comment ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                UserAvatar(name = comment.userName, size = 28.dp)
                                Column {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            comment.userName,
                                            fontWeight = FontWeight.Medium,
                                            color = TextPrimary,
                                            fontSize = 12.sp
                                        )
                                        Text(
                                            comment.timeAgo,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextSecondary,
                                            fontSize = 10.sp
                                        )
                                    }
                                    Text(
                                        comment.text,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextPrimary,
                                        fontSize = 11.sp
                                    )
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
                        placeholder = { Text("Add a comment…", fontSize = 11.sp, color = TextSecondary) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = Gold,
                            unfocusedBorderColor = Border,
                            focusedTextColor     = TextPrimary,
                            unfocusedTextColor   = TextPrimary
                        )
                    )
                    IconButton(
                        onClick = {
                            if (commentText.isNotBlank()) {
                                onAddComment(commentText)
                                commentText = ""
                            }
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Gold)
                    ) {
                        Icon(Icons.Default.Send, null, tint = Bg, modifier = Modifier.size(14.dp))
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
    var caption      by remember { mutableStateOf("") }
    var monumentName by remember { mutableStateOf("Lingaraj Temple") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Surface1,
        title = {
            Text("Create Expedition Log", fontWeight = FontWeight.SemiBold, color = TextPrimary)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = monumentName,
                    onValueChange = { monumentName = it },
                    label = { Text("Monument Site") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = Gold,
                        unfocusedBorderColor = Border,
                        focusedTextColor     = TextPrimary,
                        unfocusedTextColor   = TextPrimary,
                        focusedLabelColor    = TextSecondary,
                        unfocusedLabelColor  = TextSecondary
                    )
                )

                OutlinedTextField(
                    value = caption,
                    onValueChange = { caption = it },
                    label = { Text("Share your discovery…") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = Gold,
                        unfocusedBorderColor = Border,
                        focusedTextColor     = TextPrimary,
                        unfocusedTextColor   = TextPrimary,
                        focusedLabelColor    = TextSecondary,
                        unfocusedLabelColor  = TextSecondary
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(caption, monumentName) },
                colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Bg),
                enabled = caption.isNotBlank()
            ) {
                Text("Publish", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}
