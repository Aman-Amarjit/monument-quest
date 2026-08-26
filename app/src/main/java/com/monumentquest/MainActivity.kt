package com.monumentquest

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.monumentquest.ui.auth.AuthScreen
import com.monumentquest.ui.discovery.CameraScreen
import com.monumentquest.ui.discovery.DiscoveryFormScreen
import com.monumentquest.ui.social.GuildsScreen
import com.monumentquest.ui.hotel.HotelPerksScreen
import com.monumentquest.ui.journalist.JournalistScreen
import com.monumentquest.ui.social.LeaderboardScreen
import com.monumentquest.ui.map.MapScreen
import com.monumentquest.ui.map.MonumentDetailScreen
import com.monumentquest.ui.map.MonumentWallScreen
import com.monumentquest.ui.narrator.NarratorScreen
import com.monumentquest.ui.profile.ProfileScreen
import com.monumentquest.ui.social.SocialFeedScreen
import com.monumentquest.ui.splash.SplashScreen
import com.monumentquest.ui.theme.*
import dagger.hilt.android.AndroidEntryPoint
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MonumentQuestTheme {
                val navController      = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                val mainRoutes = setOf(
                    Screen.Map.route,
                    Screen.Hotels.route,
                    Screen.Feed.route,
                    Screen.Leaderboard.route,
                    Screen.Guilds.route,
                    Screen.Profile.route
                )

                val showBottomNav = currentDestination?.route in mainRoutes

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Bg,
                    bottomBar = {
                        AnimatedVisibility(
                            visible = showBottomNav,
                            enter   = slideInVertically(
                                animationSpec = tween(320),
                                initialOffsetY = { it }
                            ) + fadeIn(tween(280)),
                            exit    = slideOutVertically(
                                animationSpec = tween(280),
                                targetOffsetY = { it }
                            ) + fadeOut(tween(180))
                        ) {
                            FloatingNavBar(
                                items = listOf(
                                    Screen.Map,
                                    Screen.Hotels,
                                    Screen.Feed,
                                    Screen.Leaderboard,
                                    Screen.Guilds,
                                    Screen.Profile
                                ),
                                currentDestination = currentDestination,
                                onNavigate = { screen ->
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState    = true
                                    }
                                }
                            )
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController    = navController,
                        startDestination = "splash",
                        modifier         = Modifier
                            .fillMaxSize()
                            .padding(top = innerPadding.calculateTopPadding())
                    ) {
                        composable("splash") {
                            SplashScreen(
                                onSplashFinished = {
                                    navController.navigate("auth") {
                                        popUpTo("splash") { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable("auth") {
                            AuthScreen(
                                onAuthSuccess = {
                                    navController.navigate(Screen.Map.route) {
                                        popUpTo("auth") { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable(Screen.Map.route) {
                            MapScreen(
                                onNavigateToCamera    = { navController.navigate("camera") },
                                onNavigateToNarrator  = { name -> navController.navigate("narrator/$name") },
                                onNavigateToJournalist = { navController.navigate("journalist") }
                            )
                        }
                        composable(Screen.Hotels.route) {
                            HotelPerksScreen()
                        }
                        composable(Screen.Feed.route) {
                            SocialFeedScreen(
                                onNavigateToCamera   = { navController.navigate("camera") },
                                onNavigateToNarrator = { name -> navController.navigate("narrator/$name") },
                                onNavigateToWall     = { id -> navController.navigate("monument_detail/$id") }
                            )
                        }
                        composable(
                            route     = "narrator/{monumentName}",
                            arguments = listOf(navArgument("monumentName") { type = NavType.StringType })
                        ) { entry ->
                            NarratorScreen(
                                monumentName    = entry.arguments?.getString("monumentName") ?: "Unknown",
                                onNavigateBack  = { navController.popBackStack() }
                            )
                        }
                        composable(
                            route     = "monument_detail/{monumentId}",
                            arguments = listOf(navArgument("monumentId") { type = NavType.StringType })
                        ) { entry ->
                            MonumentDetailScreen(
                                monumentId         = entry.arguments?.getString("monumentId") ?: "b1",
                                onNavigateBack     = { navController.popBackStack() },
                                onNavigateToNarrator = { name -> navController.navigate("narrator/$name") },
                                onNavigateToCamera = { navController.navigate("camera") }
                            )
                        }
                        composable(Screen.Leaderboard.route) { LeaderboardScreen() }
                        composable(Screen.Guilds.route)      { GuildsScreen() }
                        composable(Screen.Profile.route) {
                            ProfileScreen(
                                onNavigateToJournalist = { navController.navigate("journalist") },
                                onLogout = {
                                    navController.navigate("auth") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable("journalist") {
                            JournalistScreen(onNavigateBack = { navController.popBackStack() })
                        }
                        composable("camera") {
                            CameraScreen(onImageCaptured = { uri ->
                                val encoded = URLEncoder.encode(
                                    uri.toString(),
                                    StandardCharsets.UTF_8.toString()
                                )
                                navController.navigate("discovery_form/$encoded")
                            })
                        }
                        composable(
                            route     = "discovery_form/{imageUri}",
                            arguments = listOf(navArgument("imageUri") { type = NavType.StringType })
                        ) { entry ->
                            val encoded = entry.arguments?.getString("imageUri") ?: ""
                            val decoded = Uri.parse(Uri.decode(encoded))
                            DiscoveryFormScreen(
                                imageUri  = decoded,
                                onSuccess = {
                                    navController.navigate(Screen.Map.route) {
                                        popUpTo(Screen.Map.route) { inclusive = false }
                                        launchSingleTop = true
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FloatingNavBar(
    items: List<Screen>,
    currentDestination: androidx.navigation.NavDestination?,
    onNavigate: (Screen) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        color = Surface1.copy(alpha = 0.98f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Border),
        shadowElevation = 18.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(68.dp).padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            items.forEach { screen ->
                val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                NavPillItem(screen, selected, { onNavigate(screen) }, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun NavPillItem(
    screen: Screen,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tint = if (isSelected) Gold else TextTertiary
    Box(
        modifier = modifier.fillMaxHeight().clip(RoundedCornerShape(18.dp)).background(if (isSelected) GoldTint else Color.Transparent).clickable(
            indication = null,
            interactionSource = remember { MutableInteractionSource() },
            onClick = onClick
        ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(imageVector = if (isSelected) screen.icon else screen.outlinedIcon, contentDescription = screen.label, tint = tint, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(3.dp))
            Text(screen.label, color = tint, fontSize = 9.5.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, maxLines = 1)
        }
    }
}

sealed class Screen(
    val route:       String,
    val label:       String,
    val icon:        androidx.compose.ui.graphics.vector.ImageVector,
    val outlinedIcon: androidx.compose.ui.graphics.vector.ImageVector
) {
    object Map         : Screen("map",         "Explore",    Icons.Filled.Map,          Icons.Outlined.Map)
    object Hotels      : Screen("hotels",      "Hotels",     Icons.Filled.Hotel,        Icons.Outlined.Hotel)
    object Feed        : Screen("feed",        "Feed",       Icons.Filled.DynamicFeed, Icons.Outlined.DynamicFeed)
    object Leaderboard : Screen("leaderboard", "Rankings",   Icons.Filled.Leaderboard, Icons.Outlined.Leaderboard)
    object Guilds      : Screen("guilds",      "Guilds",     Icons.Filled.Group,        Icons.Outlined.Group)
    object Profile     : Screen("profile",     "Journal",    Icons.Filled.Person,       Icons.Outlined.Person)
}
