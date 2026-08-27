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
    val isGuest         by viewModel.isGuest.collectAsState()

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
                        text = "Guild Squad",
                        selected = currentFilter == FeedFilter.GUILD,
                        onClick = { viewModel.setFilter(FeedFilter.GUILD) }
                    )
                    FeedTabItem(
                        text = "Nearby Discoveries",
                        selected = currentFilter == FeedFilter.NEARBY,
                        onClick = { viewModel.setFilter(FeedFilter.NEARBY) }
                    )
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Horizontal story avatars row
            item {
                StoriesRow(
                    stories = stories,
                    onStoryClick = { story ->
                        activeExplorerProfile = ExplorerProfileData(
                            userId = story.id,
                            name = story.userName,
                            avatarUrl = story.avatarUrl,
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
                    val isMyPost   = post.userName.equals(viewModel.currentUserName, ignoreCase = true) ||
                                     post.userId == viewModel.currentUserId ||
                                     post.id.startsWith("local_photo_") ||
                                     post.id.startsWith("sp_")

                    PostCard(
                        post               = post,
                        isFollowed         = isFollowed,
                        isSaved            = isSaved,
                        onLikeToggle       = { if (!isGuest) viewModel.toggleLike(post.id) },
                        canLike            = !isGuest,
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
                        },
                        onDeletePost       = if (isMyPost) { { viewModel.deletePost(post.id) } } else null
                    )
                }
            }
        }
    }

    // Modal Sheet for creating a new post
    if (showCreateModal) {
        CreatePostBottomSheet(
            onDismiss = { showCreateModal = false },
            onSubmit  = { caption, monumentName, photoUri ->
                viewModel.createPost(caption, monumentName, photoUri)
                showCreateModal = false
            }
        )
    }

    // Modal Sheet for post comments
    activeCommentsPostId?.let { postId ->
        val comments = postCommentsMap[postId] ?: emptyList()
        PostCommentsBottomSheet(
            postId     = postId,
            comments   = comments,
            isGuest    = isGuest,
            onDismiss  = { activeCommentsPostId = null },
            onAddComment = { text -> viewModel.addComment(postId, text) }
        )
    }

    // Public Explorer Profile Modal
    activeExplorerProfile?.let { profile ->
        PublicProfileDialog(
            profile = profile,
            isFollowed = followedUsers.contains(profile.userId),
            onFollowToggle = { viewModel.toggleFollow(profile.userId) },
            onDismiss = { activeExplorerProfile = null }
        )
    }
}

@Composable
private fun FeedTabItem(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = text,
            fontSize = 13.5.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) Gold else TextSecondary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .height(2.dp)
                .width(if (selected) 24.dp else 0.dp)
                .background(Gold, shape = RoundedCornerShape(1.dp))
        )
    }
}

@Composable
private fun StoriesRow(
    stories: List<DiscovererStory>,
    onStoryClick: (DiscovererStory) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(stories, key = { it.id }) { story ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(68.dp)
                    .clickable { onStoryClick(story) }
            ) {
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .clip(CircleShape)
                        .border(
                            width = 2.dp,
                            color = Gold,
                            shape = CircleShape
                        )
                        .padding(3.dp)
                ) {
                    UserAvatar(
                        name = story.userName,
                        avatarUrl = story.avatarUrl,
                        size = 54.dp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = story.userName.take(9),
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
    canLike: Boolean = true,
    onNavigateToNarrator: (String) -> Unit,
    onUserClick: () -> Unit,
    onDeletePost: (() -> Unit)? = null
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
            // Header: User avatar + info + tag chip + Delete option
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

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
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

                    // Delete post option button for user's own posts
                    if (onDeletePost != null) {
                        IconButton(
                            onClick = onDeletePost,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.DeleteOutline,
                                contentDescription = "Delete Post",
                                tint = RedAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
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
                        modifier = Modifier.clickable(enabled = canLike) { onLikeToggle() },
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
                            contentDescription = "Bookmark",
                            tint = if (isSaved) Gold else TextSecondary,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }

                // AI Narrator shortcut button
                Button(
                    onClick = { onNavigateToNarrator(post.monumentName) },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Gold.copy(alpha = 0.15f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, null, tint = Gold, modifier = Modifier.size(14.dp))
                        Text("AI Spirit", color = Gold, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreatePostBottomSheet(
    onDismiss: () -> Unit,
    onSubmit: (caption: String, monumentName: String, photoUri: Uri?) -> Unit
) {
    var caption by remember { mutableStateOf("") }
    var monumentName by remember { mutableStateOf("Lingaraj Temple") }
    var selectedPhotoUri by remember { mutableStateOf<Uri?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) selectedPhotoUri = uri
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Surface1,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Border) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Log New Expedition",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                fontSize = 18.sp
            )

            OutlinedTextField(
                value = monumentName,
                onValueChange = { monumentName = it },
                label = { Text("Monument / Site Name", color = TextSecondary) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Gold,
                    unfocusedBorderColor = Border,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )

            OutlinedTextField(
                value = caption,
                onValueChange = { caption = it },
                label = { Text("Share your discovery chronicle…", color = TextSecondary) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Gold,
                    unfocusedBorderColor = Border,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )

            // Photo picker section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Surface2)
                        .clickable { photoPickerLauncher.launch("image/*") }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, null, tint = Gold, modifier = Modifier.size(20.dp))
                    Text(
                        if (selectedPhotoUri != null) "Photo Selected ✓" else "Attach Expedition Photo",
                        color = TextPrimary,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (selectedPhotoUri != null) {
                    IconButton(onClick = { selectedPhotoUri = null }) {
                        Icon(Icons.Default.Close, null, tint = RedAccent, modifier = Modifier.size(18.dp))
                    }
                }
            }

            selectedPhotoUri?.let { uri ->
                AsyncImage(
                    model = uri,
                    contentDescription = "Selected Photo",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            Button(
                onClick = {
                    if (caption.isNotBlank()) {
                        onSubmit(caption.trim(), monumentName.trim(), selectedPhotoUri)
                    }
                },
                enabled = caption.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Gold,
                    disabledContainerColor = Surface2
                )
            ) {
                Text("Publish Expedition Log", color = Bg, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PostCommentsBottomSheet(
    postId: String,
    comments: List<PostComment>,
    isGuest: Boolean,
    onDismiss: () -> Unit,
    onAddComment: (String) -> Unit
) {
    var newCommentText by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Surface1,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Border) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Explorer Discussions (${comments.size})",
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                fontSize = 17.sp
            )

            if (comments.isEmpty()) {
                Text(
                    "No discussions yet. Be the first explorer to comment!",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 280.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(comments, key = { it.id }) { comment ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            UserAvatar(name = comment.userName, size = 32.dp)
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(comment.userName, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 12.5.sp)
                                    Text(comment.timeAgo, color = TextSecondary, fontSize = 10.5.sp)
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(comment.text, color = TextPrimary, fontSize = 12.sp, lineHeight = 17.sp)
                            }
                        }
                    }
                }
            }

            if (!isGuest) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newCommentText,
                        onValueChange = { newCommentText = it },
                        placeholder = { Text("Add to discussion…", color = TextSecondary, fontSize = 12.5.sp) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Gold,
                            unfocusedBorderColor = Border,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = true
                    )

                    IconButton(
                        onClick = {
                            if (newCommentText.isNotBlank()) {
                                onAddComment(newCommentText.trim())
                                newCommentText = ""
                            }
                        },
                        enabled = newCommentText.isNotBlank(),
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (newCommentText.isNotBlank()) Gold else Surface2)
                    ) {
                        Icon(Icons.Default.Send, null, tint = Bg, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
