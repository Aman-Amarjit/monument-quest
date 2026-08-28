package com.monumentquest.data.model

data class PartnerHotel(
    val id: String,
    val name: String,
    val category: String,
    val latitude: Double,
    val longitude: Double,
    val rating: Double,
    val pricePerNight: Int,
    val discountPercent: Int,
    val perkTitle: String,
    val perkDesc: String,
    val xpRequired: Int,
    val distanceMeters: Int = 0
)

data class ClaimVoucherResponse(
    val success: Boolean = false,
    val voucherCode: String = "",
    val message: String = ""
)

data class HotelsResponse(
    val hotels: List<PartnerHotel>
)

data class ClaimVoucherRequest(
    val hotelId: String
)
