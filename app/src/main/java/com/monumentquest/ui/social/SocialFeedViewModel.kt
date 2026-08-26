package com.monumentquest.ui.social

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
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

@HiltViewModel
class SocialFeedViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val monumentApi: MonumentApi
) : ViewModel() {

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
        fetchPosts()
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
                        timestamp = f.timestamp,
                        timestampFormatted = "Just now"
                    )
                }
                val allPosts = (userPostsList + serverPosts).distinctBy { it.id }
                _posts.value = filterList(allPosts, _currentFilter.value)
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
        _posts.value = _posts.value.map { post ->
            if (post.id == postId) {
                val newIsLiked = !post.isLiked
                val newCount = if (newIsLiked) post.likesCount + 1 else post.likesCount - 1
                post.copy(isLiked = newIsLiked, likesCount = newCount)
            } else post
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

            val newPost = SocialPost(
                id = "sp_" + System.currentTimeMillis(),
                userId = user?.uid ?: "user_me",
                userName = user?.displayName ?: "Explorer (You)",
                userRank = "Bhubaneswar Explorer",
                monumentName = monumentName.ifBlank { "Lingaraj Temple" },
                locationName = "Bhubaneswar, Odisha",
                imageUrl = photoUrlToUse,
                caption = caption,
                postType = "CHECKIN",
                likesCount = 1,
                isLiked = true,
                commentsCount = 0,
                timestamp = System.currentTimeMillis(),
                timestampFormatted = "Just now"
            )

            userPostsList.add(0, newPost)
            _posts.value = filterList(userPostsList, _currentFilter.value)

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
