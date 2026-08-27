package com.monumentquest.data.remote

import com.monumentquest.data.model.ClaimVoucherRequest
import com.monumentquest.data.model.ClaimVoucherResponse
import com.monumentquest.data.model.HotelsResponse
import com.monumentquest.data.model.Monument
import com.monumentquest.data.model.UserProfile
import retrofit2.http.*

// Auth request/response models
data class SendOtpRequest(val email: String)
data class SendOtpResponse(val success: Boolean, val message: String, val otpCode: String? = null)
data class LoginWithOtpRequest(val email: String, val code: String)
data class RegisterWithOtpRequest(val email: String, val code: String, val name: String)
data class AuthUser(
    val id: String, val name: String, val email: String,
    val userRank: String = "Bhubaneswar Explorer", val points: Int = 0,
    val role: String = "EXPLORER", val avatarUrl: String? = null,
    val guildName: String? = null, val isGuest: Boolean = false
)
data class AuthData(val token: String, val user: AuthUser)
data class AuthResponse(val success: Boolean, val data: AuthData, val needsSignup: Boolean = false, val alreadyExists: Boolean = false)

// Profile Update
data class UpdateProfileRequest(val name: String? = null, val avatarUrl: String? = null)

data class LeaderboardEntry(val rank: Int, val id: String? = null, val name: String, val xp: Int = 0, val monumentsCaptured: Int = 0, val badge: String = "EXPLORER")
data class LeaderboardResponse(val success: Boolean = true, val data: List<LeaderboardEntry>? = null, val leaderboard: List<LeaderboardEntry>? = null)

data class ApiFeedItem(
    val id: String,
    val userId: String? = null,
    val user_id: String? = null,
    val userName: String? = null,
    val user_name: String? = null,
    val userAvatarUrl: String? = null,
    val user_avatar_url: String? = null,
    val monumentName: String? = null,
    val monument_name: String? = null,
    val locationName: String? = null,
    val imageUrl: String? = null,
    val image_url: String? = null,
    val caption: String,
    val postType: String? = "CHECKIN",
    val likesCount: Int? = 0,
    val likes: Int? = 0,
    val isLiked: Boolean = false,
    val commentsCount: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

data class FeedResponse(
    val success: Boolean = true,
    val data: List<ApiFeedItem>? = null,
    val feed: List<ApiFeedItem>? = null
)

data class CreatePostRequest(val caption: String, val monumentId: String = "m1", val imageUrl: String? = null)
data class CreatePostResponse(val success: Boolean, val data: Map<String, Any>?)
data class GuildItem(val id: String, val name: String, val description: String, val members_count: Int, val total_xp: Int, val rank: Int)
data class GuildsResponse(val guilds: List<GuildItem>)

// Geofenced Monument Capture
data class CaptureRequest(
    val monumentId: String? = null,
    val name: String? = null,
    val latitude: Double,
    val longitude: Double,
    val imageUrl: String? = null
)

data class CaptureData(
    val success: Boolean = false,
    val monumentId: String? = null,
    val monumentName: String? = null,
    val locationName: String? = null,
    val distanceMeters: Int? = null,
    val requiredRadiusMeters: Int? = null,
    val pointsEarned: Int? = null,
    val rarity: String? = null,
    val alreadyCaptured: Boolean? = null,
    val message: String? = null
)

data class CaptureResponse(
    val success: Boolean = false,
    val data: CaptureData? = null,
    val error: String? = null
)

// Progress sync
data class SyncProgressRequest(
    val totalDistanceKm: Double,
    val areaUnlockedKm2: Double,
    val streakDays: Int,
    val walkPathJson: String,
    val exploredZonesJson: String,
    val xpDelta: Int = 0
)
data class SyncProgressResponse(val success: Boolean, val data: Map<String, Any>?)

interface MonumentApi {
    // Auth (OTP-only — no passwords)
    @POST("auth/send-otp")
    suspend fun sendOtp(@Body request: SendOtpRequest): SendOtpResponse

    @POST("auth/login-with-otp")
    suspend fun loginWithOtp(@Body request: LoginWithOtpRequest): AuthResponse

    @POST("auth/register-with-otp")
    suspend fun registerWithOtp(@Body request: RegisterWithOtpRequest): AuthResponse

    @POST("auth/guest")
    suspend fun loginAsGuest(): AuthResponse

    @GET("auth/me")
    suspend fun getMe(): Map<String, Any>

    // User profile & progress
    @GET("user/profile")
    suspend fun getUserProfile(): UserProfile

    @PATCH("user/profile")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): Map<String, Any>

    @PATCH("user/progress")
    suspend fun syncProgress(@Body request: SyncProgressRequest): SyncProgressResponse

    // Monuments & Geofenced Capture
    @GET("monuments")
    suspend fun getNearbyMonuments(@Query("lat") lat: Double, @Query("lon") lon: Double): Map<String, List<Monument>>

    @POST("monuments/capture")
    suspend fun captureMonument(@Body request: CaptureRequest): CaptureResponse

    @GET("hotels")
    suspend fun getPartnerHotels(@Query("lat") lat: Double, @Query("lon") lon: Double): HotelsResponse

    @POST("hotels/claim-voucher")
    suspend fun claimHotelVoucher(@Body request: ClaimVoucherRequest): ClaimVoucherResponse

    @GET("leaderboard")
    suspend fun getLeaderboard(): LeaderboardResponse

    @GET("feed")
    suspend fun getFeed(): FeedResponse

    @POST("feed/posts")
    suspend fun createPost(@Body request: CreatePostRequest): CreatePostResponse

    @DELETE("feed/posts/{id}")
    suspend fun deletePost(@Path("id") id: String): Map<String, Any>

    @POST("feed/posts/{id}/like")
    suspend fun toggleLike(@Path("id") id: String): Map<String, Any>

    @GET("guilds")
    suspend fun getGuilds(): GuildsResponse
}
