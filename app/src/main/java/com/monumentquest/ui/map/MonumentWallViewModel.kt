package com.monumentquest.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.monumentquest.data.model.Monument
import com.monumentquest.data.model.MonumentPost
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class MonumentWallViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _posts = MutableStateFlow<List<MonumentPost>>(emptyList())
    val posts: StateFlow<List<MonumentPost>> = _posts

    private val _isCustodian = MutableStateFlow(false)
    val isCustodian: StateFlow<Boolean> = _isCustodian

    val currentUserId: String? get() = auth.currentUser?.uid

    fun fetchPosts(monumentId: String) {
        viewModelScope.launch {
            try {
                val monumentDoc = firestore.collection("monuments").document(monumentId).get().await()
                val monument = monumentDoc.toObject(Monument::class.java)
                _isCustodian.value = monument?.custodianId == currentUserId

                val snapshot = firestore.collection("posts")
                    .whereEqualTo("monumentId", monumentId)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .get()
                    .await()
                
                val list = snapshot.toObjects(MonumentPost::class.java)
                if (list.isNotEmpty()) {
                    _posts.value = list
                } else {
                    _posts.value = getSamplePosts(monumentId)
                }
            } catch (e: Exception) {
                _posts.value = getSamplePosts(monumentId)
            }
        }
    }

    fun deletePost(postId: String, monumentId: String) {
        viewModelScope.launch {
            try {
                firestore.collection("posts").document(postId).delete().await()
                fetchPosts(monumentId)
            } catch (e: Exception) {
                _posts.value = _posts.value.filter { it.id != postId }
            }
        }
    }

    fun addPost(monumentId: String, content: String) {
        viewModelScope.launch {
            val newPost = MonumentPost(
                id = "p_" + System.currentTimeMillis(),
                monumentId = monumentId,
                userId = currentUserId ?: "user_me",
                userName = auth.currentUser?.displayName ?: "Adventurer (You)",
                content = content,
                timestamp = System.currentTimeMillis()
            )
            try {
                firestore.collection("posts").document(newPost.id).set(newPost).await()
                fetchPosts(monumentId)
            } catch (e: Exception) {
                _posts.value = listOf(newPost) + _posts.value
            }
        }
    }

    private fun getSamplePosts(monumentId: String): List<MonumentPost> {
        return listOf(
            MonumentPost(
                id = "101",
                monumentId = monumentId,
                userId = "u1",
                userName = "Elena Rostova",
                content = "Visited during sunset! The Victorian Gothic architecture is breathtaking up close.",
                timestamp = System.currentTimeMillis() - 3600000 * 2
            ),
            MonumentPost(
                id = "102",
                monumentId = monumentId,
                userId = "u2",
                userName = "Arthur Pendelton",
                content = "Left an AR Time Capsule near the north clock face. Check it out when you reach 20m!",
                timestamp = System.currentTimeMillis() - 3600000 * 24
            )
        )
    }
}
