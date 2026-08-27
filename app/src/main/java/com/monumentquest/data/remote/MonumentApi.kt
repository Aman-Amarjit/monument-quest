package com.monumentquest.data.remote

import com.monumentquest.data.model.ClaimVoucherRequest
import com.monumentquest.data.model.ClaimVoucherResponse
import com.monumentquest.data.model.HotelsResponse
import com.monumentquest.data.model.Monument
import com.monumentquest.data.model.User
import com.monumentquest.data.model.UserProfile
import retrofit2.http.*

data class SendOtpRequest(val email: String)
data class SendOtpResponse(val success: Boolean, val message: String, val otpCode: String? = null)
data class LoginWithOtpRequest(val email: String, val code: String)
data class RegisterWithOtpRequest(val email: String, val code: String, val name: String)
data class AuthUser(val id: String, val name: String, val email: String, val userRank: String = "Bhubaneswar Explorer", val points: Int = 0, val role: String = "EXPLORER", val avatarUrl: String? = null, val guildName: String? = null, val isGuest: Boolean = false)
data class AuthData(val token: String, val user: AuthUser)
data class AuthResponse(val success: Boolean, val data: AuthData, val needsSignup: Boolean = false, val alreadyExists: Boolean = false)
data class UpdateProfileRequest(val name: String? = null, val avatarUrl: String? = null)
data class LeaderboardEntry(val rank: Int, val id: String? = null, val name: String, val xp: Int = 0, val monumentsCaptured: Int = 0, val badge: String = "EXPLORER")
data class LeaderboardResponse(val success: Boolean = true, val data: List<LeaderboardEntry>? = null, val leaderboard: List<LeaderboardEntry>? = null)
data class ApiFeedItem(val id: String, val userId: String? = null, val userName: String? = null, val userAvatarUrl: String? = null, val userRank: String? = null, val monumentName: String? = null, val monument_name: String? = null, val locationName: String? = null, val imageUrl: String? = null, val image_url: String? = null, val caption: String, val postType: String? = "CHECKIN", val likesCount: Int? = 0, val isLiked: Boolean = false, val isSaved: Boolean = false, val isFollowing: Boolean = false, val commentsCount: Int = 0, val timestamp: Long = 0L)
data class FeedResponse(val success: Boolean = true, val data: List<ApiFeedItem>? = null, val feed: List<ApiFeedItem>? = null)
data class CreatePostRequest(val caption: String, val monumentId: String = "m1", val imageUrl: String? = null, val postType: String = "CHECKIN")
data class CreatePostResponse(val success: Boolean, val data: ApiFeedItem?)
data class ToggleLikeData(val isLiked: Boolean = false, val likesCount: Int = 0)
data class ToggleLikeResponse(val success: Boolean = false, val data: ToggleLikeData? = null)
data class ApiComment(val id: String, val postId: String, val userId: String, val userName: String, val userAvatarUrl: String? = null, val body: String, val createdAt: Long = 0L)
data class CommentsResponse(val success: Boolean = false, val data: List<ApiComment> = emptyList())
data class ApiCommentResponse(val success: Boolean = false, val data: ApiComment? = null)
data class ToggleStateResponse(val success: Boolean = false, val data: ToggleState? = null)
data class ToggleState(val isFollowing: Boolean = false, val isSaved: Boolean = false)
data class StoryResponseItem(val id: String, val userId: String = "", val userName: String = "", val avatarUrl: String? = null, val mediaUrl: String? = null, val caption: String = "", val createdAt: Long = 0L, val expiresAt: Long = 0L, val viewsCount: Int = 0, val isViewed: Boolean = false)
data class StoriesResponse(val success: Boolean = false, val data: List<StoryResponseItem> = emptyList())
data class GuildItem(val id: String, val name: String, val region: String = "", val description: String = "", val membersCount: Int = 0, val totalXp: Int = 0, val rank: Int = 0)
data class GuildsResponse(val success: Boolean = false, val data: List<GuildItem>? = null, val guilds: List<GuildItem>? = null)
data class MyGuildResponse(val success: Boolean = false, val data: GuildItem? = null)
data class GuildMembersResponse(val success: Boolean = false, val data: List<User> = emptyList())
data class CaptureRequest(val monumentId: String? = null, val name: String? = null, val latitude: Double, val longitude: Double, val imageUrl: String? = null)
data class CaptureData(val success: Boolean = false, val monumentId: String? = null, val monumentName: String? = null, val locationName: String? = null, val distanceMeters: Int? = null, val requiredRadiusMeters: Int? = null, val pointsEarned: Int? = null, val rarity: String? = null, val alreadyCaptured: Boolean? = null, val message: String? = null)
data class CaptureResponse(val success: Boolean = false, val data: CaptureData? = null, val error: String? = null)
data class SyncProgressRequest(val totalDistanceKm: Double, val areaUnlockedKm2: Double, val streakDays: Int, val walkPathJson: String, val exploredZonesJson: String, val xpDelta: Int = 0)
data class SyncProgressResponse(val success: Boolean, val data: Map<String, Any>?)

interface MonumentApi {
    @POST("auth/send-otp") suspend fun sendOtp(@Body request: SendOtpRequest): SendOtpResponse
    @POST("auth/login-with-otp") suspend fun loginWithOtp(@Body request: LoginWithOtpRequest): AuthResponse
    @POST("auth/register-with-otp") suspend fun registerWithOtp(@Body request: RegisterWithOtpRequest): AuthResponse
    @POST("auth/guest") suspend fun loginAsGuest(): AuthResponse
    @GET("auth/me") suspend fun getMe(): Map<String, Any>
    @GET("user/profile") suspend fun getUserProfile(): UserProfile
    @PATCH("user/profile") suspend fun updateProfile(@Body request: UpdateProfileRequest): Map<String, Any>
    @PATCH("user/progress") suspend fun syncProgress(@Body request: SyncProgressRequest): SyncProgressResponse
    @GET("monuments") suspend fun getNearbyMonuments(@Query("lat") lat: Double, @Query("lon") lon: Double): Map<String, List<Monument>>
    @POST("monuments/capture") suspend fun captureMonument(@Body request: CaptureRequest): CaptureResponse
    @GET("hotels") suspend fun getPartnerHotels(@Query("lat") lat: Double, @Query("lon") lon: Double): HotelsResponse
    @POST("hotels/claim-voucher") suspend fun claimHotelVoucher(@Body request: ClaimVoucherRequest): ClaimVoucherResponse
    @GET("leaderboard") suspend fun getLeaderboard(): LeaderboardResponse
    @GET("feed") suspend fun getFeed(@Query("limit") limit: Int = 50, @Query("cursor") cursor: String? = null, @Query("scope") scope: String? = null): FeedResponse
    @POST("feed/posts") suspend fun createPost(@Body request: CreatePostRequest): CreatePostResponse
    @DELETE("feed/posts/{id}") suspend fun deletePost(@Path("id") id: String): Map<String, Any>
    @POST("feed/posts/{id}/like") suspend fun toggleLike(@Path("id") id: String): ToggleLikeResponse
    @GET("feed/posts/{id}/comments") suspend fun getComments(@Path("id") id: String): CommentsResponse
    @POST("feed/posts/{id}/comments") suspend fun addComment(@Path("id") id: String, @Body request: Map<String, String>): retrofit2.Response<ApiCommentResponse>
    @GET("guilds") suspend fun getGuilds(): GuildsResponse
    @GET("guilds/me") suspend fun getMyGuild(): MyGuildResponse
    @POST("guilds/{id}/join") suspend fun joinGuild(@Path("id") id: String): MyGuildResponse
    @DELETE("guilds/me") suspend fun leaveGuild(): Map<String, Any>
    @GET("guilds/{id}/leaderboard") suspend fun getGuildLeaderboard(@Path("id") id: String): GuildMembersResponse
    @GET("social/stories") suspend fun getStories(): StoriesResponse
    @POST("social/stories") suspend fun createStory(@Body request: Map<String, String>): StoryResponseItem
    @POST("social/stories/{id}/view") suspend fun viewStory(@Path("id") id: String): Map<String, Any>
    @POST("social/users/{id}/follow") suspend fun toggleFollow(@Path("id") id: String): ToggleStateResponse
    @POST("social/posts/{id}/save") suspend fun toggleSave(@Path("id") id: String): ToggleStateResponse
    @POST("social/posts/{id}/report") suspend fun reportPost(@Path("id") id: String, @Body request: Map<String, String>): Map<String, Any>
}
