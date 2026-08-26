package com.monumentquest.ui.social

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.monumentquest.data.model.DiscovererStory
import com.monumentquest.data.model.SocialPost
import com.monumentquest.ui.common.EmptyStateView
import com.monumentquest.ui.common.ExplorerProfileData
import com.monumentquest.ui.common.PublicProfileDialog
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
    var activeExplorerProfile by remember { mutableStateOf<ExplorerProfileData?>(null) }

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
                    Column {
                        Text(
                            text = "CHRONICLES & EXPEDITIONS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Gold,
                            letterSpacing = 1.2.sp
                        )
                        Text(
                            text = "Explorer Feed",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Gold)
                            .clickable { showCreateModal = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Create Log",
                            tint = Bg,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Search bar
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
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor    = Gold,
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
                        text = "Global Expeditions",
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                StoriesBar(
                    stories = stories,
                    onAddStory = { showCreateModal = true },
                    onStoryClick = { story ->
                        activeExplorerProfile = ExplorerProfileData(
                            userId = story.id,
                            name = story.userName,
                            username = "@${story.userName.lowercase().replace(" ", "_")}",
                            rank = "Grand Master Explorer",
                            xp = 3450,
                            visitedCount = 14,
                            distanceKm = 28.5,
                            guildName = "Kalinga Keepers",
                            bio = "Passionate heritage explorer mapping ancient monuments and sharing historical chronicles across Odisha. 🛕✨"
                        )
                    }
                )
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
                        onNavigateToNarrator = onNavigateToNarrator,
                        onUserClick        = {
                            activeExplorerProfile = ExplorerProfileData(
                                userId = post.userId,
                                name = post.userName,
                                avatarUrl = post.userAvatarUrl,
                                username = "@${post.userName.lowercase().replace(" ", "_")}",
                                rank = post.userRank,
                                xp = 0,
                                visitedCount = 0,
                                distanceKm = 0.0,
                                guildName = "Kalinga Keepers",
                                bio = "Active explorer sharing discoveries and time capsules at historic landmarks."
                            )
                        }
                    )
                }
            }
        }

        // Modals & Dialogs
        if (showCreateModal) {
            CreatePostModal(
                onDismiss = { showCreateModal = false },
                onSubmit  = { caption, monument, photoUri ->
                    viewModel.createPost(caption, monument, photoUri)
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

        activeExplorerProfile?.let { prof ->
            PublicProfileDialog(
                profile = prof,
                isFollowed = followedUsers.contains(prof.userId),
                onFollowToggle = { viewModel.toggleFollow(prof.userId) },
                onDismiss = { activeExplorerProfile = null }
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
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 13.5.sp
        )
        Spacer(modifier = Modifier.height(5.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.5.dp)
                .background(
                    color = if (selected) Gold else Color.Transparent,
                    shape = RoundedCornerShape(2.dp)
                )
        )
    }
}

@Composable
private fun StoriesBar(
    stories: List<DiscovererStory>,
    onAddStory: () -> Unit,
    onStoryClick: (DiscovererStory) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
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
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Surface2)
                        .border(1.5.dp, Gold, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.AddAPhoto,
                        contentDescription = "Add",
                        tint = Gold,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Text("Your Story", style = MaterialTheme.typography.labelSmall, color = TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        items(stories) { story ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.clickable { onStoryClick(story) }
            ) {
                UserAvatar(name = story.userName, size = 56.dp, borderColor = Gold)
                Text(
                    story.userName.split(" ")[0],
                    style = MaterialTheme.typography.labelSmall,
                    color = TextPrimary,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Medium
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
    onNavigateToNarrator: (String) -> Unit,
    onUserClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Surface1),
        border = androidx.compose.foundation.BorderStroke(1.dp, Border)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: User avatar + info + tag chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onUserClick() }
                ) {
                    UserAvatar(name = post.userName, avatarUrl = post.userAvatarUrl, size = 42.dp, borderColor = Gold)
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                post.userName,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 14.5.sp
                            )
                            Icon(Icons.Default.Verified, null, tint = Gold, modifier = Modifier.size(13.dp))
                        }
                        Text(
                            "${post.userRank} · ${formatTimeAgo(post.timestamp)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                val (tagBg, tagFg, tagLabel) = when (post.postType) {
                    "DISCOVERY"    -> Triple(Color(0xFF2A1C00), Gold,              "🏛️ Discovery")
                    "TIME_CAPSULE" -> Triple(Color(0xFF160A24), Color(0xFFC084FC), "🔮 Time Capsule")
                    "REFLECTION"   -> Triple(Color(0xFF1A0A00), Color(0xFFFF8C42), "📜 Reflection")
                    else           -> Triple(Surface2,          TextSecondary,     "📍 Check-in")
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(tagBg)
                        .border(1.dp, tagFg.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(tagLabel, color = tagFg, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Caption text
            Text(
                text = post.caption,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                fontSize = 13.5.sp,
                lineHeight = 21.sp
            )

            // Monument Image
            post.imageUrl?.let { url ->
                Spacer(modifier = Modifier.height(12.dp))
                Box(contentAlignment = Alignment.BottomStart) {
                    AsyncImage(
                        model = url,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(210.dp)
                            .clip(RoundedCornerShape(14.dp)),
                        contentScale = ContentScale.Crop
                    )

                    // Monument location pill overlay on photo
                    Box(
                        modifier = Modifier
                            .padding(10.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.75f))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Place, null, tint = Gold, modifier = Modifier.size(13.dp))
                            Text(post.monumentName, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Footer Action Row: Likes, Comments, Save, AI Narrator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.clickable { onLikeToggle() },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = if (post.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Like",
                            tint = if (post.isLiked) RedAccent else TextSecondary,
                            modifier = Modifier.size(19.dp)
                        )
                        Text(
                            "${post.likesCount}",
                            fontWeight = FontWeight.Bold,
                            color = if (post.isLiked) RedAccent else TextSecondary,
                            fontSize = 12.sp
                        )
                    }

                    Row(
                        modifier = Modifier.clickable { onOpenComments() },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            Icons.Default.ChatBubbleOutline,
                            contentDescription = "Comments",
                            tint = TextSecondary,
                            modifier = Modifier.size(19.dp)
                        )
                        Text(
                            "${post.commentsCount}",
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }

                    IconButton(onClick = onSaveToggle, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Save",
                            tint = if (isSaved) Gold else TextSecondary,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }

                Button(
                    onClick = { onNavigateToNarrator(post.monumentName) },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    modifier = Modifier.height(32.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Surface2, contentColor = Gold),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Gold.copy(alpha = 0.3f))
                ) {
                    Icon(Icons.Default.Explore, null, tint = Gold, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("AI Narrator", color = Gold, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
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
                Text("Expedition Comments", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 16.sp)
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
                            Text("No comments yet. Be the first explorer to comment!", color = TextSecondary, fontSize = 12.sp)
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
                                        Text(comment.userName, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 12.sp)
                                        Text(comment.timeAgo, color = TextSecondary, fontSize = 10.sp)
                                    }
                                    Text(comment.text, color = TextPrimary, fontSize = 11.5.sp)
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
    onSubmit: (String, String, Uri?) -> Unit
) {
    var caption      by remember { mutableStateOf("") }
    var monumentName by remember { mutableStateOf("") }
    var selectedPhotoUri by remember { mutableStateOf<Uri?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedPhotoUri = uri
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Surface1,
        title = {
            Text("Create Expedition Log", fontWeight = FontWeight.Bold, color = TextPrimary)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = monumentName,
                    onValueChange = { monumentName = it },
                    label = { Text("Monument or Site Name") },
                    placeholder = { Text("e.g. Rajarani Temple, Mukteshwar", fontSize = 12.sp, color = TextSecondary) },
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
                        .height(90.dp),
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

                // Image upload selector card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Surface2)
                        .border(1.dp, Border, RoundedCornerShape(12.dp))
                        .clickable { photoPickerLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedPhotoUri != null) {
                        Image(
                            painter = rememberAsyncImagePainter(selectedPhotoUri),
                            contentDescription = "Selected Photo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.AddAPhoto, null, tint = Gold, modifier = Modifier.size(24.dp))
                            Text("Tap to Attach Monument Photo", fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(caption, monumentName, selectedPhotoUri) },
                colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Bg),
                enabled = caption.isNotBlank()
            ) {
                Text("Publish Log", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}
