package com.monumentquest.ui.social

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.monumentquest.core.auth.TokenManager
import com.monumentquest.core.utils.ImageUtils
import com.monumentquest.data.model.DiscovererStory
import com.monumentquest.data.model.SocialPost
import com.monumentquest.data.remote.ApiComment
import com.monumentquest.data.remote.ApiFeedItem
import com.monumentquest.data.remote.CreatePostRequest
import com.monumentquest.data.remote.MonumentApi
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

enum class FeedFilter { GLOBAL, GUILD, NEARBY }
data class PostComment(val id: String, val userName: String, val text: String, val timeAgo: String = "Just now")
fun formatTimeAgo(timestampMs: Long): String {
    if (timestampMs <= 0) return "Just now"
    val diff = System.currentTimeMillis() - timestampMs
    val seconds = diff / 1000; val minutes = seconds / 60; val hours = minutes / 60; val days = hours / 24; val months = days / 30; val years = days / 365
    return when { diff < 0 || seconds < 45 -> "Just now"; minutes < 60 -> minutes.toString() + "m ago"; hours < 24 -> hours.toString() + "h ago"; days < 30 -> days.toString() + "d ago"; months < 12 -> months.toString() + "mo ago"; else -> years.toString() + "yr ago" }
}

@HiltViewModel
class SocialFeedViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val monumentApi: MonumentApi,
    private val tokenManager: TokenManager
) : ViewModel() {
    private val prefs = context.getSharedPreferences("monument_feed_cache", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val _isGuest = MutableStateFlow(tokenManager.isGuest())
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
    val currentUserId: String get() = tokenManager.getUserId() ?: ""
    val currentUserName: String get() = tokenManager.getUserName() ?: "Explorer"

    init { restoreCache(); fetchPosts() }

    private fun restoreCache() {
        _savedPostIds.value = prefs.getStringSet("saved_post_ids", emptySet()) ?: emptySet()
        _followedUsers.value = prefs.getStringSet("followed_user_ids", emptySet()) ?: emptySet()
        prefs.getString("post_comments_json", null)?.let { json -> try { val type = object : TypeToken<Map<String, List<PostComment>>>() {}.type; _postComments.value = gson.fromJson(json, type) } catch (_: Exception) {} }
    }

    private fun saveCache(posts: List<SocialPost>) {
        try {
            val cacheable = posts.take(50).map { post -> if (post.imageUrl?.startsWith("data:image/") == true) post.copy(imageUrl = null) else post }
            prefs.edit().putStringSet("saved_post_ids", _savedPostIds.value).putStringSet("followed_user_ids", _followedUsers.value).putString("post_comments_json", gson.toJson(_postComments.value)).putString("cached_posts_json", gson.toJson(cacheable)).apply()
        } catch (_: Exception) {}
    }

    fun setSearchQuery(query: String) { _searchQuery.value = query; _posts.value = filterList(_posts.value, _currentFilter.value) }
    fun setFilter(filter: FeedFilter) { _currentFilter.value = filter; fetchPosts() }

    fun fetchPosts() {
        viewModelScope.launch {
            try {
                val scope = if (_currentFilter.value == FeedFilter.GUILD) "guild" else null
                val response = withContext(Dispatchers.IO) { monumentApi.getFeed(scope = scope) }
                val mapped = (response.data ?: response.feed ?: emptyList()).map(::toSocialPost)
                _posts.value = filterList(mapped, _currentFilter.value)
                saveCache(mapped)
            } catch (_: Exception) { restoreCachedPosts() }
            try {
                val response = withContext(Dispatchers.IO) { monumentApi.getStories() }
                _stories.value = response.data.map { story -> DiscovererStory(story.id, story.userId, story.userName, story.avatarUrl, story.mediaUrl, story.caption, story.createdAt, story.expiresAt, story.viewsCount, story.isViewed) }
            } catch (_: Exception) {}
        }
    }

    private fun restoreCachedPosts() {
        prefs.getString("cached_posts_json", null)?.let { json -> try { val type = object : TypeToken<List<SocialPost>>() {}.type; _posts.value = filterList(gson.fromJson(json, type), _currentFilter.value) } catch (_: Exception) {} }
    }

    private fun toSocialPost(item: ApiFeedItem) = SocialPost(id = item.id, userId = item.userId ?: "", userName = item.userName ?: "Heritage Explorer", userAvatarUrl = item.userAvatarUrl, userRank = item.userRank ?: "Explorer", monumentName = item.monumentName ?: item.monument_name ?: "Monument", locationName = item.locationName ?: "", imageUrl = item.imageUrl ?: item.image_url, caption = item.caption, postType = item.postType ?: "CHECKIN", likesCount = item.likesCount ?: 0, isLiked = item.isLiked, isSaved = item.isSaved, isFollowing = item.isFollowing, commentsCount = item.commentsCount, timestamp = item.timestamp, timestampFormatted = formatTimeAgo(item.timestamp))

    private fun filterList(list: List<SocialPost>, filter: FeedFilter): List<SocialPost> {
        val byTab = when (filter) { FeedFilter.GLOBAL, FeedFilter.GUILD -> list; FeedFilter.NEARBY -> list.filter { it.locationName.contains("Bhubaneswar", true) || it.locationName.contains("Odisha", true) } }
        val query = _searchQuery.value.trim()
        return if (query.isEmpty()) byTab else byTab.filter { it.caption.contains(query, true) || it.userName.contains(query, true) || it.monumentName.contains(query, true) || it.locationName.contains(query, true) }
    }

    fun toggleFollow(userId: String) {
        if (_isGuest.value || userId.isBlank() || userId == currentUserId) return
        val wasFollowing = _followedUsers.value.contains(userId)
        val next = _followedUsers.value.toMutableSet().apply { if (wasFollowing) remove(userId) else add(userId) }
        _followedUsers.value = next
        _posts.value = _posts.value.map { if (it.userId == userId) it.copy(isFollowing = !wasFollowing) else it }
        saveCache(_posts.value)
        viewModelScope.launch(Dispatchers.IO) { try { monumentApi.toggleFollow(userId) } catch (_: Exception) { _followedUsers.value = if (wasFollowing) next + userId else next - userId; fetchPosts() } }
    }

    fun toggleSavePost(postId: String) {
        if (_isGuest.value) return
        val wasSaved = _savedPostIds.value.contains(postId)
        _savedPostIds.value = _savedPostIds.value.toMutableSet().apply { if (wasSaved) remove(postId) else add(postId) }
        _posts.value = _posts.value.map { if (it.id == postId) it.copy(isSaved = !wasSaved) else it }
        saveCache(_posts.value)
        viewModelScope.launch(Dispatchers.IO) { try { monumentApi.toggleSave(postId) } catch (_: Exception) { _savedPostIds.value = if (wasSaved) _savedPostIds.value + postId else _savedPostIds.value - postId; fetchPosts() } }
    }

    fun loadComments(postId: String) {
        viewModelScope.launch(Dispatchers.IO) { try { val comments = monumentApi.getComments(postId).data.map { it.toPostComment() }; _postComments.value = _postComments.value + (postId to comments); saveCache(_posts.value) } catch (_: Exception) {} }
    }

    fun addComment(postId: String, commentText: String) {
        if (_isGuest.value || commentText.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = monumentApi.addComment(postId, mapOf("body" to commentText.trim()))
                if (response.isSuccessful) {
                    val comment = response.body()?.data
                    if (comment != null) { _postComments.value = _postComments.value + (postId to ((_postComments.value[postId] ?: emptyList()) + comment.toPostComment())); _posts.value = _posts.value.map { if (it.id == postId) it.copy(commentsCount = it.commentsCount + 1) else it }; saveCache(_posts.value) }
                }
            } catch (_: Exception) {}
        }
    }

    fun deletePost(postId: String) {
        if (_isGuest.value) return
        val previous = _posts.value
        _posts.value = previous.filterNot { it.id == postId }
        viewModelScope.launch(Dispatchers.IO) { try { monumentApi.deletePost(postId) } catch (_: Exception) { _posts.value = previous } }
    }

    fun toggleLike(postId: String) {
        if (_isGuest.value) return
        val previous = _posts.value
        _posts.value = previous.map { if (it.id == postId) it.copy(isLiked = !it.isLiked, likesCount = (it.likesCount + if (it.isLiked) -1 else 1).coerceAtLeast(0)) else it }
        saveCache(_posts.value)
        viewModelScope.launch(Dispatchers.IO) { try { val result = monumentApi.toggleLike(postId).data; if (result != null) _posts.value = _posts.value.map { if (it.id == postId) it.copy(isLiked = result.isLiked, likesCount = result.likesCount) else it } } catch (_: Exception) { _posts.value = previous } }
    }

    fun createPost(caption: String, monumentName: String, photoUri: Uri? = null) {
        if (_isGuest.value || caption.trim().isEmpty()) return
        viewModelScope.launch {
            try {
                val encodedImage = photoUri?.let { withContext(Dispatchers.IO) { ImageUtils.uriToBase64DataUrl(context, it, maxDimension = 600, quality = 75) } }
                val response = withContext(Dispatchers.IO) { monumentApi.createPost(CreatePostRequest(caption = caption.trim(), monumentId = "m1", imageUrl = encodedImage)) }
                response.data?.let { item -> _posts.value = filterList(listOf(toSocialPost(item)) + _posts.value, _currentFilter.value); saveCache(_posts.value) }
            } catch (_: Exception) {}
        }
    }

    private fun ApiComment.toPostComment() = PostComment(id, userName, body, formatTimeAgo(createdAt))
}
