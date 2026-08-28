package com.monumentquest.ui.hotel

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.monumentquest.data.model.PartnerHotel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun HotelPerksScreen() {
    var activeVoucherPayload by remember { mutableStateOf<String?>(null) }
    var activeHotelName by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var fetchedHotels by remember { mutableStateOf<List<PartnerHotel>>(emptyList()) }

    // Fallback hotel list if network connection is offline or empty
    val fallbackHotels = remember {
        listOf(
            PartnerHotel(id = "h1", name = "Mayfair Lagoon", category = "5-STAR LUXURY • OLD TOWN", latitude = 20.2981, longitude = 85.8268, rating = 4.8, pricePerNight = 6500, discountPercent = 20, perkTitle = "Heritage Explorer Pass (20% OFF)", perkDesc = "Free traditional breakfast & 2x XP Multiplier on check-in.", xpRequired = 300, distanceMeters = 1200),
            PartnerHotel(id = "h2", name = "Trident Bhubaneswar", category = "LUXURY RESORT • CITY CENTER", latitude = 20.2912, longitude = 85.8234, rating = 4.7, pricePerNight = 5800, discountPercent = 15, perkTitle = "Explorer Quest Pass (15% OFF)", perkDesc = "Complimentary airport shuttle & heritage guide map.", xpRequired = 250, distanceMeters = 1800),
            PartnerHotel(id = "h3", name = "Vivanta Bhubaneswar", category = "MODERN HOTEL • JANPATH", latitude = 20.2854, longitude = 85.8391, rating = 4.6, pricePerNight = 4900, discountPercent = 15, perkTitle = "Traveler's Choice (15% OFF)", perkDesc = "Free spa access voucher & late check-out.", xpRequired = 200, distanceMeters = 2400),
            PartnerHotel(id = "h4", name = "Swosti Premium", category = "BUSINESS & RESORT • NAYAPALLI", latitude = 20.3012, longitude = 85.8190, rating = 4.5, pricePerNight = 4200, discountPercent = 10, perkTitle = "Heritage Pass (10% OFF)", perkDesc = "Free high-speed WiFi & complimentary breakfast.", xpRequired = 150, distanceMeters = 3100),
            PartnerHotel(id = "h5", name = "Welcomhotel by ITC", category = "BOUTIQUE STAY • DUMDUMA", latitude = 20.2481, longitude = 85.7950, rating = 4.7, pricePerNight = 5200, discountPercent = 20, perkTitle = "Temple City Pass (20% OFF)", perkDesc = "Exclusive dinner coupon & temple walking tour.", xpRequired = 350, distanceMeters = 3800)
        )
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val okHttp = okhttp3.OkHttpClient.Builder()
                    .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                    .build()
                val retro = retrofit2.Retrofit.Builder()
                    .baseUrl("https://monument-quest-mocha.vercel.app/api/v1/")
                    .client(okHttp)
                    .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
                    .build()
                val api = retro.create(com.monumentquest.data.remote.MonumentApi::class.java)
                val res = api.getPartnerHotels(20.2381, 85.8338)
                val list = res.hotels ?: emptyList()
                withContext(Dispatchers.Main) {
                    fetchedHotels = if (list.isNotEmpty()) list else fallbackHotels
                    isLoading = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    fetchedHotels = fallbackHotels
                    isLoading = false
                }
            }
        }
    }

    val displayHotels = remember(searchQuery, fetchedHotels) {
        if (searchQuery.isBlank()) {
            fetchedHotels
        } else {
            fetchedHotels.filter { (it.name ?: "").contains(searchQuery, ignoreCase = true) || (it.category ?: "").contains(searchQuery, ignoreCase = true) }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(12.dp))

            // Screen Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "LIVE PARTNER HOTELS & STAYS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF0A500),
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        "Hotels Near You",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E293B))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        "SIH26202 Tourism Hub",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Search input for place/hotel
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF1E293B))
                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Search, null, tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (searchQuery.isEmpty()) {
                        Text("Search hotels near place...", color = Color(0xFF64748B), fontSize = 13.sp)
                    }
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 13.sp, color = Color.White),
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "Search hotels" }
                    )
                }
                if (searchQuery.isNotEmpty()) {
                    Icon(
                        Icons.Default.Close, null,
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(16.dp).clickable { searchQuery = "" }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Nearby Status Bar
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xCC064E3B))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(Color(0xFF10B981)))
                Text(
                    "Showing ${displayHotels.size} verified partner stays",
                    fontSize = 11.sp,
                    color = Color(0xFFA7F3D0),
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(14.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFFF0A500))
                }
            } else if (displayHotels.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    Text("No partner hotels found.", color = Color(0xFF94A3B8), fontSize = 14.sp)
                }
            } else {
                // Hotel List
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    items(displayHotels) { hotel ->
                        HotelCard(
                            hotel = hotel,
                            onClaim = {
                                activeHotelName = hotel.name
                                activeVoucherPayload = "MQ-QUEST-PASS|USER_1|HOTEL_${hotel.id}|DISCOUNT_${hotel.discountPercent}%|EXPIRE_24H"
                            }
                        )
                    }
                }
            }
        }

        // QR Code Modal Dialog
        AnimatedVisibility(
            visible = activeVoucherPayload != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f))
                    .clickable { activeVoucherPayload = null },
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .padding(16.dp)
                        .clickable(enabled = false) {},
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFF0F172A),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFF0A500))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "TOURIST QUEST PASS",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF0A500)
                            )
                            IconButton(onClick = { activeVoucherPayload = null }) {
                                Icon(Icons.Default.Close, null, tint = Color(0xFF94A3B8))
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        Text(
                            activeHotelName ?: "Partner Hotel",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )

                        Spacer(Modifier.height(16.dp))

                        Box(
                            modifier = Modifier
                                .size(200.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White)
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val qrBmp = remember(activeVoucherPayload) {
                                generateDummyQrBitmap(200, 200)
                            }
                            Image(
                                bitmap = qrBmp.asImageBitmap(),
                                contentDescription = "Voucher QR Code",
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(Modifier.height(16.dp))

                        Text(
                            "Present this QR Code at Check-in to receive discount & Activate 2x XP Multiplier!",
                            fontSize = 11.5.sp,
                            color = Color(0xFFCBD5E1),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            lineHeight = 16.sp
                        )

                        Spacer(Modifier.height(18.dp))

                        Button(
                            onClick = { activeVoucherPayload = null },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF0A500), contentColor = Color(0xFF0F172A)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Done / Back to Pass", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HotelCard(hotel: PartnerHotel, onClaim: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF1E293B),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        hotel.name ?: "Partner Hotel",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "${hotel.category ?: "HOTEL"} • ${formatDist(hotel.distanceMeters)} away",
                        fontSize = 11.5.sp,
                        color = Color(0xFF94A3B8)
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF064E3B))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        "${hotel.discountPercent}% OFF",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF86EFAC)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0F172A))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Default.CardGiftcard, null, tint = Color(0xFFF0A500), modifier = Modifier.size(14.dp))
                Text(
                    hotel.perkTitle ?: "Explorer Quest Pass",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFFFD166)
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    val priceDisplay = if (hotel.pricePerNight > 0) hotel.pricePerNight else (2800 + (Math.abs((hotel.name ?: "").hashCode()) % 3200))
                    Text(
                        "Starting from",
                        fontSize = 10.sp,
                        color = Color(0xFF64748B)
                    )
                    Text(
                        "₹$priceDisplay / night",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }

                Button(
                    onClick = onClaim,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF0A500), contentColor = Color(0xFF0F172A))
                ) {
                    Icon(Icons.Default.QrCode, null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Claim Pass", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun formatDist(meters: Int): String = when {
    meters <= 0 -> "Nearby"
    meters < 1000 -> "$meters m"
    else -> String.format("%.1f km", meters / 1000.0)
}

private fun generateDummyQrBitmap(width: Int, height: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val paint = android.graphics.Paint()
    canvas.drawColor(AndroidColor.WHITE)
    paint.color = AndroidColor.BLACK

    val moduleSize = 8
    val rows = height / moduleSize
    val cols = width / moduleSize

    for (r in 0 until rows) {
        for (c in 0 until cols) {
            if ((r * c + r + c) % 3 == 0 || (r < 6 && c < 6) || (r < 6 && c > cols - 7) || (r > rows - 7 && c < 6)) {
                canvas.drawRect(
                    (c * moduleSize).toFloat(),
                    (r * moduleSize).toFloat(),
                    ((c + 1) * moduleSize).toFloat(),
                    ((r + 1) * moduleSize).toFloat(),
                    paint
                )
            }
        }
    }
    return bitmap
}
