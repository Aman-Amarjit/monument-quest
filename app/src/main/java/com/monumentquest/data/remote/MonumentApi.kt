package com.monumentquest.data.remote

import com.monumentquest.data.model.Monument
import retrofit2.http.GET
import retrofit2.http.Query

interface MonumentApi {
    @GET("monuments/nearby")
    suspend fun getNearbyMonuments(
        @Query("latitude") lat: Double,
        @Query("longitude") lon: Double
    ): List<Monument>
}
