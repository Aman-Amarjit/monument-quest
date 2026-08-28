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
            prefs.edit().remove("cached_posts_json").apply()
        } catch (_: Exception) {}
    }

    init {
        _isGuest.value = tokenManager.isGuest()
        purgeLocalPhotoStubs()
        restoreCache()
        fetchPosts()
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
                            monumentName = mon, locationName = "Bhubaneswar, Odisha",
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

            // Merge: user's new posts first, then deduplicate, then sort newest-first
            val combined = (userPostsWithAvatar + serverPosts + firestorePosts)
                .distinctBy { it.id }
                .sortedByDescending { it.timestamp }
            _posts.value = filterList(combined, _currentFilter.value)
            saveCache(_posts.value)
        }
    }

    private fun filterList(list: List<SocialPost>, filter: FeedFilter): List<SocialPost> {
        val filteredByTab = when (filter) {
            FeedFilter.GLOBAL -> list
            FeedFilter.GUILD -> list.filter { it.isFollowing || it.userId == currentUserId }
            FeedFilter.NEARBY -> list.filter { it.locationName.contains("Bhubaneswar") || it.locationName.contains("Odisha") }
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

        // Sync to backend — do NOT call fetchPosts() after this to avoid resetting like state
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val res = monumentApi.toggleLike(postId)
                // Update with server-confirmed like count
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
                // Revert optimistic update on failure
                _posts.value = _posts.value.map { post ->
                    if (post.id == postId) {
                        val revertLiked = !post.isLiked
                        val revertCount = if (revertLiked) post.likesCount + 1 else maxOf(0, post.likesCount - 1)
                        post.copy(isLiked = revertLiked, likesCount = revertCount)
                    } else post
                }
                saveCache(_posts.value)
            }
        }
    }

    fun createPost(caption: String, monumentName: String, photoUri: Uri? = null) {
        if (_isGuest.value) return
        viewModelScope.launch {
            val user = auth.currentUser
            val myName = currentUserName
            val myAvatar = getMyAvatarUrl()

            // Upload photo to Firebase Storage to get a public HTTPS URL visible to all users
            var publicPhotoUrl: String? = null
            if (photoUri != null) {
                try {
                    val storageRef = com.google.firebase.storage.FirebaseStorage.getInstance()
                        .reference.child("post_photos/${System.currentTimeMillis()}.jpg")
                    storageRef.putFile(photoUri).await()
                    publicPhotoUrl = storageRef.downloadUrl.await().toString()
                } catch (e: Exception) {
                    // Firebase Storage upload failed — use compressed base64 as fallback
                    try {
                        publicPhotoUrl = withContext(Dispatchers.IO) {
                            com.monumentquest.core.utils.ImageUtils.uriToBase64DataUrl(
                                context, photoUri, maxDimension = 400, quality = 60
                            )
                        }
                    } catch (_: Exception) {}
                }
            }

            val photoUrlToUse = publicPhotoUrl ?: "https://images.unsplash.com/photo-1627894483216-2138af692e32?q=80&w=800"
            val nowMs = System.currentTimeMillis()

            val newPost = SocialPost(
                id = "sp_" + nowMs,
                userId = tokenManager.getUserId() ?: user?.uid ?: "user_me",
                userName = myName,
                userAvatarUrl = myAvatar,
                userRank = "Bhubaneswar Explorer",
                monumentName = monumentName.ifBlank { "Bhubaneswar Monument" },
                locationName = "Bhubaneswar, Odisha",
                imageUrl = photoUrlToUse,
                caption = caption,
                postType = "CHECKIN",
                likesCount = 0,
                isLiked = false,
                commentsCount = 0,
                timestamp = nowMs,
                timestampFormatted = "Just now"
            )

            // Save to Firebase Firestore so it's stored permanently in cloud database for everyone
            try {
                val firestoreData = hashMapOf(
                    "userId" to (user?.uid ?: "user_me"),
                    "userName" to myName,
                    "userAvatarUrl" to myAvatar,
                    "caption" to caption,
                    "content" to caption,
                    "monumentName" to monumentName.ifBlank { "Bhubaneswar Monument" },
                    "imageUrl" to photoUrlToUse,
                    "timestamp" to nowMs
                )
                firestore.collection("posts").document(newPost.id).set(firestoreData)
            } catch (e: Exception) {}

            userPostsList.add(0, newPost)
            val updatedList = (listOf(newPost) + _posts.value).distinctBy { it.id }
            _posts.value = filterList(updatedList, _currentFilter.value)
            saveCache(_posts.value)

            withContext(Dispatchers.IO) {
                try {
                    monumentApi.createPost(CreatePostRequest(
                        caption = caption,
                        monumentId = monumentName.ifBlank { "Bhubaneswar Monument" },
                        imageUrl = photoUrlToUse
                    ))
                } catch (e: Exception) {}
            }
            fetchPosts()
        }
    }
}
