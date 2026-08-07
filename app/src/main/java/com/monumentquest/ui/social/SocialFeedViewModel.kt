package com.monumentquest.ui.social

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.monumentquest.data.model.DiscovererStory
import com.monumentquest.data.model.SocialPost
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

enum class FeedFilter(val label: String) {
    GLOBAL("🌐 Global"),
    GUILD("🛡️ Guild"),
    NEARBY("📍 Nearby")
}

data class PostComment(
    val id: String = "",
    val userName: String = "",
    val text: String = "",
    val timeAgo: String = "Just now"
)

@HiltViewModel
class SocialFeedViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _posts = MutableStateFlow<List<SocialPost>>(emptyList())
    val posts: StateFlow<List<SocialPost>> = _posts

    private val _stories = MutableStateFlow<List<DiscovererStory>>(
        listOf(
            DiscovererStory("s1", "Aarav Patnaik"),
            DiscovererStory("s2", "Priya Mohanty"),
            DiscovererStory("s3", "Subhashree Das"),
            DiscovererStory("s4", "Rohan Mishra")
        )
    )
    val stories: StateFlow<List<DiscovererStory>> = _stories

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
                PostComment("c3", "Aarav Patnaik", "Left a time capsule right near Marichi Kunda as well!")
            )
        )
    )
    val postComments: StateFlow<Map<String, List<PostComment>>> = _postComments

    init {
        fetchPosts()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
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

        // Update comments count on post
        _posts.value = _posts.value.map { post ->
            if (post.id == postId) post.copy(commentsCount = post.commentsCount + 1) else post
        }
    }

    fun fetchPosts() {
        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("social_posts")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .get()
                    .await()

                val list = snapshot.toObjects(SocialPost::class.java)
                if (list.isNotEmpty()) {
                    _posts.value = filterList(list, _currentFilter.value)
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

    fun createPost(caption: String, monumentName: String) {
        viewModelScope.launch {
            val user = auth.currentUser
            val newPost = SocialPost(
                id = "sp_" + System.currentTimeMillis(),
                userId = user?.uid ?: "user_me",
                userName = user?.displayName ?: "Explorer (You)",
                userRank = "Bhubaneswar Explorer",
                monumentName = monumentName,
                locationName = "Bhubaneswar, Odisha",
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
                userName = "Aarav Patnaik",
                userRank = "Temple City Historian",
                monumentName = "Lingaraj Temple",
                locationName = "Old Town, Bhubaneswar",
                imageUrl = "https://images.unsplash.com/photo-1627894483216-2138af692e32?q=80&w=800&auto=format&fit=crop",
                caption = "Early morning visit to Lingaraj Temple! The 55m Deula spire lit up in warm morning light is an absolute masterpiece of 11th-century Kalinga architecture. 🛕✨ #kalinga #lingaraj",
                postType = "CHECKIN",
                likesCount = 84,
                isLiked = true,
                commentsCount = 2,
                timestamp = now - 1000 * 60 * 20,
                timestampFormatted = "20m ago"
            ),
            SocialPost(
                id = "p2",
                userId = "u2",
                userName = "Priya Mohanty",
                userRank = "Master Explorer",
                monumentName = "Mukteshvara Temple",
                locationName = "Kedargouri, Bhubaneswar",
                imageUrl = "https://images.unsplash.com/photo-1600585154340-be6161a56a0c?q=80&w=800&auto=format&fit=crop",
                caption = "The iconic carved Torana archway of Mukteshvara is stunning! Left an AR Time Capsule near the sacred Marichi Kunda tank. 🔮📜 #mukteshvara",
                postType = "TIME_CAPSULE",
                likesCount = 62,
                isLiked = false,
                commentsCount = 1,
                timestamp = now - 1000 * 60 * 110,
                timestampFormatted = "1h ago"
            ),
            SocialPost(
                id = "p3",
                userId = "u3",
                userName = "Subhashree Das",
                userRank = "First Discoverer",
                monumentName = "Dhauli Shanti Stupa",
                locationName = "Dhauli Hills, Bhubaneswar",
                imageUrl = "https://images.unsplash.com/photo-1590050752117-238cb0fb12b1?q=80&w=800&auto=format&fit=crop",
                caption = "Stood on Dhauli Hills where Emperor Ashoka renounced war after the Kalinga War in 261 BC. The peace pagoda overlooks the Daya River. 🕊️🌿 #dhauli",
                postType = "DISCOVERY",
                likesCount = 115,
                isLiked = true,
                commentsCount = 0,
                timestamp = now - 1000 * 3600 * 5,
                timestampFormatted = "5h ago"
            )
        )
    }
}
