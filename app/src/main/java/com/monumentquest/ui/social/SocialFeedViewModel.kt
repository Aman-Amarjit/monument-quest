package com.monumentquest.ui.social

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.monumentquest.data.model.DiscovererStory
import com.monumentquest.data.model.SocialPost
import com.monumentquest.data.remote.MonumentApi
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
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

    private val _stories = MutableStateFlow<List<DiscovererStory>>(
        listOf(
            DiscovererStory("s1", "Aarav Sharma", "https://images.unsplash.com/photo-1627894483216-2138af692e32?q=80&w=400"),
            DiscovererStory("s2", "Priya Patel", "https://images.unsplash.com/photo-1600585154340-be6161a56a0c?q=80&w=400"),
            DiscovererStory("s3", "Subhashree Das", "https://images.unsplash.com/photo-1548013146-72479768bada?q=80&w=400"),
            DiscovererStory("s4", "Rohan Mohanty", "https://images.unsplash.com/photo-1564507592333-c60657eea523?q=80&w=400")
        )
    )
    val stories: StateFlow<List<DiscovererStory>> = _stories

    private val _posts = MutableStateFlow<List<SocialPost>>(emptyList())
    val posts: StateFlow<List<SocialPost>> = _posts

    private val _currentFilter = MutableStateFlow(FeedFilter.GLOBAL)
    val currentFilter: StateFlow<FeedFilter> = _currentFilter

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _followedUsers = MutableStateFlow<Set<String>>(setOf("u2"))
    val followedUsers: StateFlow<Set<String>> = _followedUsers

    private val _savedPostIds = MutableStateFlow<Set<String>>(emptySet())
    val savedPostIds: StateFlow<Set<String>> = _savedPostIds

    private val _postComments = MutableStateFlow<Map<String, List<PostComment>>>(
        mapOf(
            "p1" to listOf(
                PostComment("c1", "Priya Mohanty", "The morning light on the Deula spire is breathtaking! ✨"),
                PostComment("c2", "Subhashree Das", "Visited during Shivaratri, such divine energy!")
            ),
            "p2" to listOf(
                PostComment("c3", "Aarav Sharma", "The detailed carvings here are ancient mastercraft!")
            )
        )
    )
    val postComments: StateFlow<Map<String, List<PostComment>>> = _postComments

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
                val feedRes = monumentApi.getFeed()
                if (feedRes.feed.isNotEmpty()) {
                    val postsList = feedRes.feed.map { f ->
                        SocialPost(
                            id = f.id,
                            userId = "user_feed_" + f.id,
                            userName = f.user_name,
                            userRank = "Heritage Explorer",
                            monumentName = f.monument_name,
                            locationName = "Bhubaneswar, Odisha",
                            imageUrl = f.image_url.ifBlank { "https://images.unsplash.com/photo-1627894483216-2138af692e32?q=80&w=800" },
                            caption = f.caption,
                            postType = "CHECKIN",
                            likesCount = f.likes,
                            isLiked = false,
                            commentsCount = 1,
                            timestamp = System.currentTimeMillis(),
                            timestampFormatted = "Just now"
                        )
                    }
                    _posts.value = filterList(postsList + getSampleSocialPosts(), _currentFilter.value)
                } else {
                    _posts.value = filterList(getSampleSocialPosts(), _currentFilter.value)
                }
            } catch (e: Exception) {
                _posts.value = filterList(getSampleSocialPosts(), _currentFilter.value)
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

            // Save photo permanently to app internal files if selected
            var savedPhotoUrl: String? = "https://images.unsplash.com/photo-1627894483216-2138af692e32?q=80&w=800"
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

            val newPost = SocialPost(
                id = "sp_" + System.currentTimeMillis(),
                userId = user?.uid ?: "user_me",
                userName = user?.displayName ?: "Explorer (You)",
                userRank = "Bhubaneswar Explorer",
                monumentName = monumentName,
                locationName = "Bhubaneswar, Odisha",
                imageUrl = savedPhotoUrl,
                caption = caption,
                postType = "CHECKIN",
                likesCount = 1,
                isLiked = true,
                commentsCount = 0,
                timestamp = System.currentTimeMillis(),
                timestampFormatted = "Just now"
            )

            _posts.value = listOf(newPost) + _posts.value
        }
    }

    private fun getSampleSocialPosts(): List<SocialPost> {
        val now = System.currentTimeMillis()
        return listOf(
            SocialPost(
                id = "p1",
                userId = "u1",
                userName = "Aarav Sharma",
                userRank = "Grand Master Explorer",
                monumentName = "Lingaraj Temple",
                locationName = "Old Town, Bhubaneswar",
                imageUrl = "https://images.unsplash.com/photo-1627894483216-2138af692e32?q=80&w=800&auto=format&fit=crop",
                caption = "Witnessed the magnificent 11th-century Kalinga architecture at Lingaraj Temple today! The 180ft deula tower radiates ancient majesty. 🛕✨ #lingaraj #kalinga",
                postType = "DISCOVERY",
                likesCount = 42,
                isLiked = true,
                commentsCount = 2,
                timestamp = now - 1000 * 60 * 20,
                timestampFormatted = "20m ago"
            ),
            SocialPost(
                id = "p2",
                userId = "u2",
                userName = "Priya Patel",
                userRank = "Master Explorer",
                monumentName = "Mukteshvara Temple",
                locationName = "Kedargouri, Bhubaneswar",
                imageUrl = "https://images.unsplash.com/photo-1600585154340-be6161a56a0c?q=80&w=800&auto=format&fit=crop",
                caption = "The ornate stone archway (torana) at Mukteshvara Temple is hailed as the 'Gem of Kalinga Architecture'. Left a time capsule message for future explorers! 🔮📜 #mukteshvara",
                postType = "TIME_CAPSULE",
                likesCount = 29,
                isLiked = false,
                commentsCount = 1,
                timestamp = now - 1000 * 60 * 110,
                timestampFormatted = "1h ago"
            ),
            SocialPost(
                id = "p3",
                userId = "u3",
                userName = "Subhashree Das",
                userRank = "Temple Scholar",
                monumentName = "Dhauli Shanti Stupa",
                locationName = "Dhauli Hills, Bhubaneswar",
                imageUrl = "https://images.unsplash.com/photo-1548013146-72479768bada?q=80&w=800&auto=format&fit=crop",
                caption = "Standing on Dhauli Hill where Emperor Ashoka renounced war after the Kalinga War in 261 BC. Quiet peace and Buddhist stupa white reflections against sunset. 🕊️🌅 #dhauli",
                postType = "REFLECTION",
                likesCount = 58,
                isLiked = true,
                commentsCount = 4,
                timestamp = now - 1000 * 60 * 240,
                timestampFormatted = "4h ago"
            )
        )
    }
}
