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
    val role: String = "EXPLORER", val guildName: String? = null, val isGuest: Boolean = false
)
data class AuthData(val token: String, val user: AuthUser)
data class AuthResponse(val success: Boolean, val data: AuthData, val needsSignup: Boolean = false, val alreadyExists: Boolean = false)

// Legacy compat
data class SignUpRequest(val name: String, val username: String, val email: String, val password: String, val role: String = "Heritage Explorer")
data class SignUpResponse(val success: Boolean, val userId: String, val user: UserProfile, val message: String)

data class LeaderboardEntry(val rank: Int, val name: String, val xp: Int, val monuments_captured: Int, val badge: String)
data class LeaderboardResponse(val leaderboard: List<LeaderboardEntry>)
data class FeedPost(val id: String, val user_name: String, val monument_name: String, val image_url: String, val caption: String, val likes: Int)
data class FeedResponse(val feed: List<FeedPost>)
data class GuildItem(val id: String, val name: String, val description: String, val members_count: Int, val total_xp: Int, val rank: Int)
data class GuildsResponse(val guilds: List<GuildItem>)

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

    @PATCH("user/progress")
    suspend fun syncProgress(@Body request: SyncProgressRequest): SyncProgressResponse

    // Monuments
    @GET("monuments")
    suspend fun getNearbyMonuments(@Query("lat") lat: Double, @Query("lon") lon: Double): Map<String, List<Monument>>

    @GET("hotels")
    suspend fun getPartnerHotels(@Query("lat") lat: Double, @Query("lon") lon: Double): HotelsResponse

    @POST("hotels/claim-voucher")
    suspend fun claimHotelVoucher(@Body request: ClaimVoucherRequest): ClaimVoucherResponse

    @GET("leaderboard")
    suspend fun getLeaderboard(): LeaderboardResponse

    @GET("feed")
    suspend fun getFeed(): FeedResponse

    @GET("guilds")
    suspend fun getGuilds(): GuildsResponse
}
