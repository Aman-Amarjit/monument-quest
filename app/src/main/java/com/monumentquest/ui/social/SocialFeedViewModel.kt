package com.monumentquest.ui.social

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.monumentquest.core.auth.TokenManager
import com.monumentquest.data.model.DiscovererStory
import com.monumentquest.data.model.SocialPost
import com.monumentquest.data.remote.CreatePostRequest
import com.monumentquest.data.remote.MonumentApi
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

enum class FeedFilter {
    GLOBAL, GUILD, NEARBY
}

data class PostComment(
    val id: String,
    val userName: String,
    val text: String,
    val timeAgo: String = "Just now"
)

fun formatTimeAgo(timestampMs: Long): String {
    if (timestampMs <= 0) return "Just now"
    val diff = System.currentTimeMillis() - timestampMs
    if (diff < 0) return "Just now"
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24
    val months = days / 30
    val years = days / 365

    return when {
        seconds < 45 -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days < 30 -> "${days}d ago"
        months < 12 -> "${months}mo ago"
        else -> "${years}yr ago"
    }
}

@HiltViewModel
class SocialFeedViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val monumentApi: MonumentApi,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val prefs = context.getSharedPreferences("monument_feed_cache", Context.MODE_PRIVATE)
    private val profilePrefs = context.getSharedPreferences("profile_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _isGuest = MutableStateFlow(false)
    val isGuest: StateFlow<Boolean> = _isGuest

    private val _stories = MutableStateFlow<List<DiscovererStory>>(emptyList())
    val stories: StateFlow<List<DiscovererStory>> = _stories

    private val _posts = MutableStateFlow<List<SocialPost>>(emptyList())
    val posts: StateFlow<List<SocialPost>> = _posts

    private val _currentFilter = MutableStateFlow(FeedFilter.GLOBAL)
    val currentFilter: StateFlow<FeedFilter> = _currentFilter

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _followedUsers = MutableStateFlow<Set<String>>(emptySet())
    val followedUsers: StateFlow<Set<String>> = _followedUsers

    private val _savedPostIds = MutableStateFlow<Set<String>>(emptySet())
    val savedPostIds: StateFlow<Set<String>> = _savedPostIds

    private val _postComments = MutableStateFlow<Map<String, List<PostComment>>>(emptyMap())
    val postComments: StateFlow<Map<String, List<PostComment>>> = _postComments

    private val userPostsList = mutableListOf<SocialPost>()

    val currentUserId: String get() = tokenManager.getUserId() ?: auth.currentUser?.uid ?: "user_me"
    // Firebase UID — used when posts are saved to Firestore (auth.currentUser?.uid)
    val currentFirebaseUid: String get() = auth.currentUser?.uid ?: ""
    val currentUserName: String get() = tokenManager.getUserName() ?: auth.currentUser?.displayName ?: "Explorer (You)"

    private fun getMyAvatarUrl(): String? {
        val rawEmail = tokenManager.getUserEmail()?.lowercase()?.trim()
            ?: auth.currentUser?.email?.lowercase()?.trim()
            ?: ""
        val userKey = if (rawEmail.isNotEmpty()) rawEmail.replace("@", "_").replace(".", "_") else "guest"
        return profilePrefs.getString("profile_avatar_uri_$userKey", null)
    }

    private fun purgeLocalPhotoStubs() {
        try {
            val postsDir = File(context.filesDir, "post_photos")
            if (postsDir.exists()) {
                postsDir.deleteRecursively()
            }
            prefs.edit().clear().apply()
        } catch (_: Exception) {}
    }

    fun clearAllPreviousPosts() {
        _posts.value = emptyList()
        userPostsList.clear()
        prefs.edit().clear().apply()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val snapshot = firestore.collection("posts").get().await()
                for (doc in snapshot.documents) {
                    firestore.collection("posts").document(doc.id).delete()
                }
            } catch (_: Exception) {}
        }
    }

    init {
        _isGuest.value = tokenManager.isGuest()
        purgeLocalPhotoStubs()
        fetchPosts()
        fetchStories()
    }

    private fun fetchStories() {
        viewModelScope.launch {
            try {
                // Fetch stories from Firestore: any user who posted in the last 24 hours
                val cutoff = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
                val snapshot = firestore.collection("posts")
                    .whereGreaterThan("timestamp", cutoff)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .get()
                    .await()

                val seen = mutableSetOf<String>()
                val storyList = mutableListOf<DiscovererStory>()
                for (doc in snapshot.documents) {
                    val userId = doc.getString("userId") ?: continue
                    if (seen.contains(userId)) continue
                    seen.add(userId)
                    val name = doc.getString("userName") ?: doc.getString("authorName") ?: "Explorer"
                    val avatar = doc.getString("userAvatarUrl") ?: doc.getString("avatarUrl")
                        ?: "https://ui-avatars.com/api/?name=${android.net.Uri.encode(name)}&background=1E293B&color=D4AF37&bold=true&size=200"
                    storyList.add(DiscovererStory(id = userId, userName = name, avatarUrl = avatar))
                    if (storyList.size >= 20) break
                }
                if (storyList.isNotEmpty()) {
                    _stories.value = storyList
                }
            } catch (e: Exception) {
                // Stories are non-critical; silently ignore fetch failure
            }
        }
    }

    private fun restoreCache() {
        try {
            val savedIds = prefs.getStringSet("saved_post_ids", emptySet()) ?: emptySet()
            _savedPostIds.value = savedIds

            val followed = prefs.getStringSet("followed_user_ids", emptySet()) ?: emptySet()
            _followedUsers.value = followed

            val commentsJson = prefs.getString("post_comments_json", null)
            if (!commentsJson.isNullOrBlank()) {
                val type = object : TypeToken<Map<String, List<PostComment>>>() {}.type
                val map: Map<String, List<PostComment>> = gson.fromJson(commentsJson, type)
                _postComments.value = map
            }
        } catch (e: Exception) {}
    }

    private fun saveCache(posts: List<SocialPost>) {
        try {
            prefs.edit().apply {
                putStringSet("saved_post_ids", _savedPostIds.value)
                putStringSet("followed_user_ids", _followedUsers.value)
                putString("post_comments_json", gson.toJson(_postComments.value))
                putString("cached_posts_json", gson.toJson(posts.take(50)))
                apply()
            }
        } catch (e: Exception) {}
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        fetchPosts()
    }

    fun setFilter(filter: FeedFilter) {
        _currentFilter.value = filter
        fetchPosts()
    }

    fun toggleFollow(userId: String) {
        val current = _followedUsers.value.toMutableSet()
        if (current.contains(userId)) current.remove(userId) else current.add(userId)
        _followedUsers.value = current
        saveCache(_posts.value)

        viewModelScope.launch(Dispatchers.IO) {
            try { monumentApi.toggleFollow(userId) } catch (e: Exception) {}
        }
    }

    fun toggleSavePost(postId: String) {
        val current = _savedPostIds.value.toMutableSet()
        if (current.contains(postId)) current.remove(postId) else current.add(postId)
        _savedPostIds.value = current
        saveCache(_posts.value)
    }

    fun addComment(postId: String, commentText: String) {
        if (_isGuest.value) return
        if (commentText.isBlank()) return
        val currentMap = _postComments.value.toMutableMap()
        val existingComments = currentMap[postId]?.toMutableList() ?: mutableListOf()
        val myName = currentUserName
        val newComment = PostComment(
            id = "comment_" + System.currentTimeMillis(),
            userName = myName,
            text = commentText,
            timeAgo = "Just now"
        )
        existingComments.add(newComment)
        currentMap[postId] = existingComments
        _postComments.value = currentMap

        _posts.value = _posts.value.map { post ->
            if (post.id == postId) post.copy(commentsCount = post.commentsCount + 1) else post
        }
        saveCache(_posts.value)

        viewModelScope.launch(Dispatchers.IO) {
            try { monumentApi.addComment(postId, com.monumentquest.data.remote.AddCommentRequest(commentText)) } catch (e: Exception) {}
        }
    }

    fun deletePost(postId: String) {
        _posts.value = _posts.value.filter { it.id != postId }
        userPostsList.removeAll { it.id == postId }
        saveCache(_posts.value)

        // Delete local photo file if it was local
        if (postId.startsWith("local_photo_")) {
            try {
                val fileName = postId.removePrefix("local_photo_")
                val file = File(File(context.filesDir, "post_photos"), fileName)
                if (file.exists()) file.delete()
                prefs.edit().remove("caption_$fileName").apply()
            } catch (e: Exception) {}
        }

        // Delete from Firebase Firestore & Backend
        viewModelScope.launch(Dispatchers.IO) {
            try {
                firestore.collection("posts").document(postId).delete().await()
            } catch (e: Exception) {}

            try {
                monumentApi.deletePost(postId)
            } catch (e: Exception) {}
        }
    }

    fun fetchPosts() {
        viewModelScope.launch {
            val myAvatar = getMyAvatarUrl()
            val myName = currentUserName
            val currentLocalMap = _posts.value.associate { it.id to Pair(it.isLiked, it.likesCount) }

            val firestorePosts = mutableListOf<SocialPost>()
            try {
                val snapshot = firestore.collection("posts")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .get()
                    .await()

                for (doc in snapshot.documents) {
                    val id = doc.id
                    val userId = doc.getString("userId") ?: "user_fs_$id"
                    val name = doc.getString("userName") ?: doc.getString("authorName") ?: "Heritage Explorer"
                    val caption = doc.getString("content") ?: doc.getString("caption") ?: ""
                    val img = doc.getString("imageUrl") ?: doc.getString("photoUrl") ?: "https://images.unsplash.com/photo-1627894483216-2138af692e32?q=80&w=800"
                    val mon = doc.getString("monumentName") ?: "Lingaraj Temple"
                    val ts = doc.getLong("timestamp") ?: System.currentTimeMillis()

                    val isMe = name.equals(myName, ignoreCase = true)
                    val defaultAvatar = "https://ui-avatars.com/api/?name=${android.net.Uri.encode(name)}&background=1E293B&color=D4AF37&bold=true&size=200"
                    val remoteAvatar = doc.getString("userAvatarUrl") ?: doc.getString("avatarUrl")
                    val avatarToUse = if (isMe && !myAvatar.isNullOrBlank()) myAvatar
                                      else if (!remoteAvatar.isNullOrBlank()) remoteAvatar
                                      else defaultAvatar

                    val localState = currentLocalMap[id]
                    val isLiked = localState?.first ?: false
                    val likesCount = localState?.second ?: 0

                    firestorePosts.add(
                        SocialPost(
                            id = id, userId = userId, userName = name,
                            userAvatarUrl = avatarToUse, userRank = "Heritage Explorer",
                            monumentName = mon, locationName = doc.getString("locationName") ?: "India",
                            imageUrl = img, caption = caption, postType = "CHECKIN",
                            likesCount = likesCount, isLiked = isLiked, commentsCount = 0,
                            timestamp = ts, timestampFormatted = formatTimeAgo(ts)
                        )
                    )
                }
            } catch (e: Exception) {}

            var serverPosts = emptyList<SocialPost>()
            try {
                val feedRes = withContext(Dispatchers.IO) { monumentApi.getFeed() }
                val rawItems = feedRes.data ?: feedRes.feed ?: emptyList()

                serverPosts = rawItems.map { f ->
                    val uId = f.userId ?: f.user_id ?: "user_feed_${f.id}"
                    val name = f.userName ?: f.user_name ?: "Heritage Explorer"
                    val mon = f.monumentName ?: f.monument_name ?: "Lingaraj Temple"
                    val img = f.imageUrl ?: f.image_url ?: "https://images.unsplash.com/photo-1627894483216-2138af692e32?q=80&w=800"
                    val ts = if (f.timestamp > 0) f.timestamp else System.currentTimeMillis()

                    val isMe = name.equals(myName, ignoreCase = true)
                    val serverAvatar = f.userAvatarUrl ?: f.user_avatar_url
                    val defaultAvatar = "https://ui-avatars.com/api/?name=${android.net.Uri.encode(name)}&background=1E293B&color=D4AF37&bold=true&size=200"
                    val avatarToUse = if (isMe && !myAvatar.isNullOrBlank()) myAvatar
                                      else if (!serverAvatar.isNullOrBlank()) serverAvatar
                                      else defaultAvatar

                    val localState = currentLocalMap[f.id]
                    val isLiked = localState?.first ?: f.isLiked
                    val likesCount = localState?.second ?: (f.likesCount ?: f.likes ?: 0)

                    SocialPost(
                        id = f.id, userId = uId, userName = name,
                        userAvatarUrl = avatarToUse, userRank = "Heritage Explorer",
                        monumentName = mon, locationName = f.locationName ?: "Bhubaneswar, Odisha",
                        imageUrl = img, caption = f.caption, postType = f.postType ?: "CHECKIN",
                        likesCount = likesCount, isLiked = isLiked,
                        isSaved = f.isSaved, isFollowing = f.isFollowing,
                        commentsCount = f.commentsCount, timestamp = ts,
                        timestampFormatted = formatTimeAgo(ts)
                    )
                }
            } catch (e: Exception) {}

            val userPostsWithAvatar = userPostsList.map { p ->
                val localState = currentLocalMap[p.id]
                val isLiked = localState?.first ?: p.isLiked
                val likesCount = localState?.second ?: p.likesCount
                val avatar = if (!myAvatar.isNullOrBlank()) myAvatar else p.userAvatarUrl
                p.copy(userAvatarUrl = avatar, isLiked = isLiked, likesCount = likesCount)
            }

            val sampleTravelPosts = listOf(
                SocialPost(
                    id = "sample_1",
                    userId = "u_aarav_sharma",
                    userName = "Aarav Sharma",
                    userAvatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?q=80&w=300",
                    userRank = "Master Explorer",
                    monumentName = "Taj Mahal, Agra",
                    locationName = "Agra, Uttar Pradesh",
                    imageUrl = "https://images.unsplash.com/photo-1564507592333-c60657eea523?q=80&w=1000",
                    caption = "Witnessed sunrise at Taj Mahal today! The marble reflection in the morning light is beyond magnificent. 🌅🕌",
                    postType = "DISCOVERY",
                    likesCount = 142,
                    isLiked = false,
                    commentsCount = 18,
                    timestamp = System.currentTimeMillis() - 2 * 3600 * 1000L,
                    timestampFormatted = "2h ago"
                ),
                SocialPost(
                    id = "sample_2",
                    userId = "u_priya_nair",
                    userName = "Priya Nair",
                    userAvatarUrl = "https://images.unsplash.com/photo-1517841905240-472988babdf9?q=80&w=300",
                    userRank = "Odisha Guild Leader",
                    monumentName = "Konark Sun Temple",
                    locationName = "Konark, Odisha",
                    imageUrl = "https://images.unsplash.com/photo-1627894483216-2138af692e32?q=80&w=1000",
                    caption = "Extensive carved stone wheels of the Sun Chariot at Konark. Ancient Indian architecture never fails to amaze me!",
                    postType = "CHECKIN",
                    likesCount = 98,
                    isLiked = true,
                    commentsCount = 12,
                    timestamp = System.currentTimeMillis() - 5 * 3600 * 1000L,
                    timestampFormatted = "5h ago"
                ),
                SocialPost(
                    id = "sample_3",
                    userId = "u_marcus_vance",
                    userName = "Marcus Vance",
                    userAvatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?q=80&w=300",
                    userRank = "Global Nomad",
                    monumentName = "Hawa Mahal",
                    locationName = "Jaipur, Rajasthan",
                    imageUrl = "https://images.unsplash.com/photo-1599661046289-e31897846e41?q=80&w=1000",
                    caption = "Exploring the Palace of Winds in Jaipur! 953 honeycombed windows designed for natural breeze circulation.",
                    postType = "DISCOVERY",
                    likesCount = 215,
                    isLiked = false,
                    commentsCount = 24,
                    timestamp = System.currentTimeMillis() - 12 * 3600 * 1000L,
                    timestampFormatted = "12h ago"
                ),
                SocialPost(
                    id = "sample_4",
                    userId = "u_elena_rostova",
                    userName = "Elena Rostova",
                    userAvatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?q=80&w=300",
                    userRank = "Heritage Photographer",
                    monumentName = "Qutub Minar",
                    locationName = "New Delhi",
                    imageUrl = "https://images.unsplash.com/photo-1587474260584-136574528ed5?q=80&w=1000",
                    caption = "Standing beneath the world's tallest brick minaret! Built in 1192 AD, breathtaking red sandstone carving.",
                    postType = "REFLECTION",
                    likesCount = 176,
                    isLiked = false,
                    commentsCount = 15,
                    timestamp = System.currentTimeMillis() - 18 * 3600 * 1000L,
                    timestampFormatted = "18h ago"
                ),
                SocialPost(
                    id = "sample_5",
                    userId = "u_devendra_patel",
                    userName = "Devendra Patel",
                    userAvatarUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?q=80&w=300",
                    userRank = "Senior Explorer",
                    monumentName = "Golden Temple",
                    locationName = "Amritsar, Punjab",
                    imageUrl = "https://images.unsplash.com/photo-1609137144813-7d9921338f24?q=80&w=1000",
                    caption = "Serene night view of Sri Harmandir Sahib reflected in the Amrit Sarovar lake. Pure peace.",
                    postType = "CHECKIN",
                    likesCount = 310,
                    isLiked = true,
                    commentsCount = 45,
                    timestamp = System.currentTimeMillis() - 24 * 3600 * 1000L,
                    timestampFormatted = "1d ago"
                ),
                SocialPost(
                    id = "sample_6",
                    userId = "u_ananya_sen",
                    userName = "Ananya Sen",
                    userAvatarUrl = "https://images.unsplash.com/photo-1438761681033-6461ffad8d80?q=80&w=300",
                    userRank = "Kolkata Explorer",
                    monumentName = "Victoria Memorial",
                    locationName = "Kolkata, West Bengal",
                    imageUrl = "https://images.unsplash.com/photo-1558431382-27e303142255?q=80&w=1000",
                    caption = "Evening stroll around the lush gardens surrounding Victoria Memorial marble monument. Stunning sunset view!",
                    postType = "TIME_CAPSULE",
                    likesCount = 124,
                    isLiked = false,
                    commentsCount = 9,
                    timestamp = System.currentTimeMillis() - 36 * 3600 * 1000L,
                    timestampFormatted = "1d ago"
                )
            )

            // Merge: user's new posts first, then realistic travel photos, then sort newest-first
            val combined = (userPostsWithAvatar + sampleTravelPosts)
                .distinctBy { it.id }
                .sortedByDescending { it.timestamp }
            _posts.value = filterList(combined, _currentFilter.value)
            saveCache(_posts.value)
        }
    }

    private fun filterList(list: List<SocialPost>, filter: FeedFilter): List<SocialPost> {
        val filteredByTab = when (filter) {
            FeedFilter.GLOBAL -> list
            // Guild: show all posts if the user hasn't followed anyone yet (new users see content
            // immediately); once they follow someone, narrow to followed + own posts.
            FeedFilter.GUILD -> if (_followedUsers.value.isEmpty()) list
                                else list.filter { it.isFollowing || it.userId == currentUserId }
            // Nearby: don't hard-code a single city — show all posts until real GPS distance
            // filtering is implemented. The Nearby tab previously hid everything for users
            // outside Bhubaneswar and for posts with a missing/different locationName.
            FeedFilter.NEARBY -> list
        }

        val query = _searchQuery.value.trim().lowercase()
        return if (query.isEmpty()) {
            filteredByTab
        } else {
            filteredByTab.filter { post ->
                post.caption.lowercase().contains(query) ||
                        post.userName.lowercase().contains(query) ||
                        post.monumentName.lowercase().contains(query) ||
                        post.locationName.lowercase().contains(query)
            }
        }
    }

    fun toggleLike(postId: String) {
        if (_isGuest.value) return
        // Optimistic update — toggle locally immediately
        _posts.value = _posts.value.map { post ->
            if (post.id == postId) {
                val newIsLiked = !post.isLiked
                val newCount = if (newIsLiked) post.likesCount + 1 else maxOf(0, post.likesCount - 1)
                post.copy(isLiked = newIsLiked, likesCount = newCount)
            } else post
        }
        saveCache(_posts.value)

        // Sync to backend — best-effort, do NOT revert on failure.
        // Posts created from the app use client-generated IDs (e.g. "sp_123") that may not
        // exist in the backend DB. A 404 is expected and should not undo the local like.
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val res = monumentApi.toggleLike(postId)
                // Update with server-confirmed like count only if server responds successfully
                val serverLiked = res["isLiked"] as? Boolean
                val serverCount = (res["likesCount"] as? Double)?.toInt()
                if (serverLiked != null && serverCount != null) {
                    _posts.value = _posts.value.map { post ->
                        if (post.id == postId) post.copy(isLiked = serverLiked, likesCount = serverCount)
                        else post
                    }
                    saveCache(_posts.value)
                }
            } catch (e: Exception) {
                // Backend sync failed (e.g. post not in backend DB) — keep local state as-is.
                // Do NOT revert: the user tapped like and it should stay liked locally.
                android.util.Log.d("SocialFeed", "Like sync skipped for $postId: ${e.message}")
            }
        }
    }

    private fun copyUriToLocalFile(uri: Uri, fileName: String): String? {
        return try {
            val dir = File(context.filesDir, "post_photos")
            if (!dir.exists()) dir.mkdirs()
            val destFile = File(dir, fileName)
            context.contentResolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            Uri.fromFile(destFile).toString()
        } catch (e: Exception) {
            android.util.Log.e("SocialFeed", "Failed to save photo locally: ${e.message}")
            null
        }
    }

    fun createPost(caption: String, monumentName: String, photoUri: Uri? = null) {
        viewModelScope.launch {
            val user = auth.currentUser
            val myName = currentUserName
            val myAvatar = getMyAvatarUrl()
            val nowMs = System.currentTimeMillis()
            val postId = "sp_" + nowMs

            // Save photo to internal app storage so URI permissions never expire
            var localPhotoUrl: String? = null
            if (photoUri != null) {
                localPhotoUrl = copyUriToLocalFile(photoUri, "$postId.jpg")
            }

            val photoUrlToUse = localPhotoUrl 
                ?: photoUri?.toString() 
                ?: "https://images.unsplash.com/photo-1627894483216-2138af692e32?q=80&w=800"

            val newPost = SocialPost(
                id = postId,
                userId = user?.uid ?: tokenManager.getUserId() ?: "user_me",
                userName = myName,
                userAvatarUrl = myAvatar,
                userRank = "Heritage Explorer",
                monumentName = monumentName.ifBlank { "Monument" },
                locationName = "India",
                imageUrl = photoUrlToUse,
                caption = caption,
                postType = "CHECKIN",
                likesCount = 0,
                isLiked = false,
                commentsCount = 0,
                timestamp = nowMs,
                timestampFormatted = "Just now"
            )

            // ALWAYS add to local state & save to cache FIRST (Instant response!)
            userPostsList.add(0, newPost)
            val updatedList = (listOf(newPost) + _posts.value).distinctBy { it.id }
            _posts.value = filterList(updatedList, _currentFilter.value)
            saveCache(_posts.value)

            // Asynchronous background sync (Cloudinary / Firestore / Backend)
            withContext(Dispatchers.IO) {
                var cloudPhotoUrl: String? = null
                if (photoUri != null) {
                    try {
                        cloudPhotoUrl = com.monumentquest.core.utils.ImageUtils.uploadToCloudinary(context, photoUri)
                    } catch (e: Exception) {
                        android.util.Log.d("SocialFeed", "Cloudinary upload skipped: ${e.message}")
                    }
                }

                val remotePhotoUrl = cloudPhotoUrl ?: photoUrlToUse

                try {
                    val firestoreData = hashMapOf(
                        "userId" to (user?.uid ?: "user_me"),
                        "userName" to myName,
                        "userAvatarUrl" to myAvatar,
                        "caption" to caption,
                        "content" to caption,
                        "monumentName" to monumentName.ifBlank { "Monument" },
                        "locationName" to "India",
                        "imageUrl" to remotePhotoUrl,
                        "timestamp" to nowMs
                    )
                    firestore.collection("posts").document(postId).set(firestoreData)
                } catch (e: Exception) {
                    android.util.Log.d("SocialFeed", "Firestore sync skipped: ${e.message}")
                }

                try {
                    monumentApi.createPost(CreatePostRequest(
                        caption = caption,
                        monumentId = monumentName.ifBlank { "Bhubaneswar Monument" },
                        imageUrl = remotePhotoUrl
                    ))
                } catch (e: Exception) {}
            }
        }
    }
}
