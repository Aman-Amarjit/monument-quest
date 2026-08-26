package com.monumentquest.data.remote

import com.monumentquest.data.model.ClaimVoucherRequest
import com.monumentquest.data.model.ClaimVoucherResponse
import com.monumentquest.data.model.HotelsResponse
import com.monumentquest.data.model.Monument
import com.monumentquest.data.model.UserProfile
import retrofit2.http.*

data class SendOtpRequest(val email: String)
data class SendOtpResponse(val success: Boolean, val email: String, val otpCode: String, val message: String)

data class VerifyOtpRequest(val email: String, val otpCode: String)
data class VerifyOtpResponse(val success: Boolean, val email: String, val message: String)

data class SignUpRequest(
    val name: String,
    val username: String,
    val email: String,
    val password: String,
    val role: String = "Heritage Explorer"
)

data class SignUpResponse(
    val success: Boolean,
    val userId: String,
    val user: UserProfile,
    val message: String
)

data class LeaderboardEntry(
    val rank: Int,
    val name: String,
    val xp: Int,
    val monuments_captured: Int,
    val badge: String
)

data class LeaderboardResponse(
    val leaderboard: List<LeaderboardEntry>
)

data class FeedPost(
    val id: String,
    val user_name: String,
    val monument_name: String,
    val image_url: String,
    val caption: String,
    val likes: Int
)

data class FeedResponse(
    val feed: List<FeedPost>
)

data class GuildItem(
    val id: String,
    val name: String,
    val description: String,
    val members_count: Int,
    val total_xp: Int,
    val rank: Int
)

data class GuildsResponse(
    val guilds: List<GuildItem>
)

interface MonumentApi {
    @GET("user/profile")
    suspend fun sendOtp(@Body request: SendOtpRequest): SendOtpResponse

    @POST("auth/verify-otp")
    suspend fun verifyOtp(@Body request: VerifyOtpRequest): VerifyOtpResponse

    @POST("auth/signup")
    suspend fun signUp(@Body request: SignUpRequest): SignUpResponse

    @GET("user/profile")
    suspend fun getUserProfile(): UserProfile

    @GET("monuments")
    suspend fun getNearbyMonuments(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double
    ): Map<String, List<Monument>>

    @GET("hotels")
    suspend fun getPartnerHotels(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double
    ): HotelsResponse

    @POST("hotels/claim-voucher")
    suspend fun claimHotelVoucher(
        @Body request: ClaimVoucherRequest
    ): ClaimVoucherResponse

    @GET("leaderboard")
    suspend fun getLeaderboard(): LeaderboardResponse

    @GET("feed")
    suspend fun getFeed(): FeedResponse

    @GET("guilds")
    suspend fun getGuilds(): GuildsResponse
}
