package com.monumentquest.ui.social

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
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
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
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
    val diff = System.currentTimeMillis() - timestampMs
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        seconds < 45 -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days < 30 -> "${days}d ago"
        else -> "${days / 30}mo ago"
    }
}

@HiltViewModel
class SocialFeedViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val monumentApi: MonumentApi
) : ViewModel() {

    private val prefs = context.getSharedPreferences("monument_feed_cache", Context.MODE_PRIVATE)
    private val gson = Gson()

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

    init {
        restoreCache()
        fetchPosts()
    }

    private fun restoreCache() {
        try {
            val savedIds = prefs.getStringSet("saved_post_ids", emptySet()) ?: emptySet()
            _savedPostIds.value = savedIds

            val commentsJson = prefs.getString("post_comments_json", null)
            if (!commentsJson.isNullOrBlank()) {
                val type = object : TypeToken<Map<String, List<PostComment>>>() {}.type
                val map: Map<String, List<PostComment>> = gson.fromJson(commentsJson, type)
                _postComments.value = map
            }

            val cachedPostsJson = prefs.getString("cached_posts_json", null)
            if (!cachedPostsJson.isNullOrBlank()) {
                val type = object : TypeToken<List<SocialPost>>() {}.type
                val list: List<SocialPost> = gson.fromJson(cachedPostsJson, type)
                _posts.value = filterList(list, _currentFilter.value)
            }
        } catch (e: Exception) {}
    }

    private fun saveCache(posts: List<SocialPost>) {
        try {
            prefs.edit().apply {
                putStringSet("saved_post_ids", _savedPostIds.value)
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
    }

    fun toggleSavePost(postId: String) {
        val current = _savedPostIds.value.toMutableSet()
        if (current.contains(postId)) current.remove(postId) else current.add(postId)
        _savedPostIds.value = current
        saveCache(_posts.value)
    }

    fun addComment(postId: String, commentText: String) {
        if (commentText.isBlank()) return
        val currentMap = _postComments.value.toMutableMap()
        val existingComments = currentMap[postId]?.toMutableList() ?: mutableListOf()
        val newComment = PostComment(
            id = "comment_" + System.currentTimeMillis(),
            userName = auth.currentUser?.displayName ?: "Explorer (You)",
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
    }

    fun fetchPosts() {
        viewModelScope.launch {
            try {
                val feedRes = withContext(Dispatchers.IO) { monumentApi.getFeed() }
                val rawItems = feedRes.data ?: feedRes.feed ?: emptyList()
                val serverPosts = rawItems.map { f ->
                    val name = f.userName ?: f.user_name ?: "Heritage Explorer"
                    val mon = f.monumentName ?: f.monument_name ?: "Lingaraj Temple"
                    val img = f.imageUrl ?: f.image_url ?: "https://images.unsplash.com/photo-1627894483216-2138af692e32?q=80&w=800"
                    val likes = f.likesCount ?: f.likes ?: 0
                    val ts = if (f.timestamp > 0) f.timestamp else System.currentTimeMillis()

                    SocialPost(
                        id = f.id,
                        userId = "user_feed_" + f.id,
                        userName = name,
                        userRank = "Heritage Explorer",
                        monumentName = mon,
                        locationName = f.locationName ?: "Bhubaneswar, Odisha",
                        imageUrl = img,
                        caption = f.caption,
                        postType = f.postType ?: "CHECKIN",
                        likesCount = likes,
                        isLiked = f.isLiked,
                        commentsCount = f.commentsCount,
                        timestamp = ts,
                        timestampFormatted = formatTimeAgo(ts)
                    )
                }
                val allPosts = (userPostsList + serverPosts).distinctBy { it.id }
                _posts.value = filterList(allPosts, _currentFilter.value)
                saveCache(_posts.value)
            } catch (e: Exception) {
                _posts.value = filterList(userPostsList, _currentFilter.value)
            }
        }
    }

    private fun filterList(list: List<SocialPost>, filter: FeedFilter): List<SocialPost> {
        val filteredByTab = when (filter) {
            FeedFilter.GLOBAL -> list
            FeedFilter.GUILD -> list.filter { it.userRank.contains("Master") || it.postType == "DISCOVERY" }
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
        // Instant optimistic UI update
        _posts.value = _posts.value.map { post ->
            if (post.id == postId) {
                val newIsLiked = !post.isLiked
                val newCount = if (newIsLiked) post.likesCount + 1 else Math.max(0, post.likesCount - 1)
                post.copy(isLiked = newIsLiked, likesCount = newCount)
            } else post
        }
        saveCache(_posts.value)

        // Persist to backend database
        viewModelScope.launch(Dispatchers.IO) {
            try {
                monumentApi.toggleLike(postId)
            } catch (e: Exception) {}
        }
    }

    fun createPost(caption: String, monumentName: String, photoUri: Uri? = null) {
        viewModelScope.launch {
            val user = auth.currentUser

            var savedPhotoUrl: String? = null
            if (photoUri != null) {
                try {
                    val postsDir = File(context.filesDir, "post_photos")
                    if (!postsDir.exists()) postsDir.mkdirs()
                    val destFile = File(postsDir, "post_${System.currentTimeMillis()}.jpg")

                    val inputStream = context.contentResolver.openInputStream(photoUri)
                    val outputStream = FileOutputStream(destFile)
                    inputStream?.use { input -> outputStream.use { output -> input.copyTo(output) } }
                    savedPhotoUrl = Uri.fromFile(destFile).toString()
                } catch (e: Exception) {
                    savedPhotoUrl = photoUri.toString()
                }
            }

            val photoUrlToUse = savedPhotoUrl ?: "https://images.unsplash.com/photo-1627894483216-2138af692e32?q=80&w=800"
            val nowMs = System.currentTimeMillis()

            val newPost = SocialPost(
                id = "sp_" + nowMs,
                userId = user?.uid ?: "user_me",
                userName = user?.displayName ?: "Explorer (You)",
                userRank = "Bhubaneswar Explorer",
                monumentName = monumentName.ifBlank { "Bhubaneswar Monument" },
                locationName = "Bhubaneswar, Odisha",
                imageUrl = photoUrlToUse,
                caption = caption,
                postType = "CHECKIN",
                likesCount = 1,
                isLiked = true,
                commentsCount = 0,
                timestamp = nowMs,
                timestampFormatted = "Just now"
            )

            userPostsList.add(0, newPost)
            val updatedList = (listOf(newPost) + _posts.value).distinctBy { it.id }
            _posts.value = filterList(updatedList, _currentFilter.value)
            saveCache(_posts.value)

            // Publish post to Supabase PostgreSQL database via backend API
            withContext(Dispatchers.IO) {
                try {
                    monumentApi.createPost(CreatePostRequest(
                        caption = caption,
                        monumentId = "m1",
                        imageUrl = photoUrlToUse
                    ))
                } catch (e: Exception) {}
            }
            fetchPosts()
        }
    }
}
