package com.monumentquest

import android.Manifest
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DynamicFeed
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.DynamicFeed
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Leaderboard
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.monumentquest.ui.auth.AuthScreen
import com.monumentquest.ui.auth.AuthViewModel
import com.monumentquest.ui.discovery.CameraScreen
import com.monumentquest.ui.discovery.DiscoveryFormScreen
import com.monumentquest.ui.journalist.JournalistScreen
import com.monumentquest.ui.map.MapScreen
import com.monumentquest.ui.map.MonumentDetailScreen
import com.monumentquest.ui.narrator.NarratorScreen
import com.monumentquest.ui.profile.ProfileScreen
import com.monumentquest.ui.social.GuildsScreen
import com.monumentquest.ui.social.LeaderboardScreen
import com.monumentquest.ui.social.SocialFeedScreen
import com.monumentquest.ui.splash.SplashScreen
import com.monumentquest.ui.theme.CardSurface
import com.monumentquest.ui.theme.ForestMid
import com.monumentquest.ui.theme.GoldBright
import com.monumentquest.ui.theme.MonumentQuestTheme
import com.monumentquest.ui.theme.MutedGray
import com.monumentquest.ui.theme.ObsidianBlack
import dagger.hilt.android.AndroidEntryPoint
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* handle results */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MonumentQuestTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                LaunchedEffect(Unit) {
                    requestPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.CAMERA
                        )
                    )
                }

                val hideNavRoutes = setOf("splash", "auth", "camera", "narrator/{monumentName}", "journalist",
                    "discovery_form/{imageUri}", "monument_wall/{monumentId}", "monument_detail/{monumentId}")
                val hideNav = hideNavRoutes.any { pattern ->
                    currentDestination?.route?.let { route ->
                        route == pattern || route.startsWith(pattern.substringBefore("{"))
                    } == true
                }

                Scaffold(
                    containerColor = ObsidianBlack,
                    bottomBar = {
                        AnimatedVisibility(
                            visible = !hideNav,
                            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                            exit  = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                        ) {
                            PremiumNavBar(
                                items = listOf(
                                    Screen.Map,
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
                                        restoreState = true
                                    }
                                }
                            )
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "splash",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("splash") {
                            SplashScreen(
                                onSplashFinished = {
                                    // Navigate to AuthScreen on first start
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
                                onNavigateToCamera = { navController.navigate("camera") },
                                onNavigateToNarrator = { name ->
                                    navController.navigate("narrator/$name")
                                },
                                onNavigateToJournalist = { navController.navigate("journalist") }
                            )
                        }
                        composable(Screen.Feed.route) {
                            SocialFeedScreen(
                                onNavigateToCamera = { navController.navigate("camera") },
                                onNavigateToNarrator = { name -> navController.navigate("narrator/$name") },
                                onNavigateToWall = { id -> navController.navigate("monument_detail/$id") }
                            )
                        }
                        composable(
                            route = "narrator/{monumentName}",
                            arguments = listOf(navArgument("monumentName") { type = NavType.StringType })
                        ) { entry ->
                            NarratorScreen(
                                monumentName = entry.arguments?.getString("monumentName") ?: "Unknown",
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable(
                            route = "monument_detail/{monumentId}",
                            arguments = listOf(navArgument("monumentId") { type = NavType.StringType })
                        ) { entry ->
                            MonumentDetailScreen(
                                monumentId = entry.arguments?.getString("monumentId") ?: "b1",
                                onNavigateBack = { navController.popBackStack() },
                                onNavigateToNarrator = { name -> navController.navigate("narrator/$name") },
                                onNavigateToCamera = { navController.navigate("camera") }
                            )
                        }
                        composable(Screen.Leaderboard.route) { LeaderboardScreen() }
                        composable(Screen.Guilds.route) { GuildsScreen() }
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
                                val encoded = URLEncoder.encode(uri.toString(), StandardCharsets.UTF_8.toString())
                                navController.navigate("discovery_form/$encoded")
                            })
                        }
                        composable(
                            route = "discovery_form/{imageUri}",
                            arguments = listOf(navArgument("imageUri") { type = NavType.StringType })
                        ) { entry ->
                            val encoded = entry.arguments?.getString("imageUri") ?: ""
                            val decoded = Uri.parse(Uri.decode(encoded))
                            DiscoveryFormScreen(
                                imageUri  = decoded,
                                onSuccess = { navController.popBackStack("map", inclusive = false) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PremiumNavBar(
    items: List<Screen>,
    currentDestination: androidx.navigation.NavDestination?,
    onNavigate: (Screen) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color.Transparent, ObsidianBlack.copy(alpha = 0.98f))
                )
            )
    ) {
        NavigationBar(
            containerColor = CardSurface.copy(alpha = 0.95f),
            tonalElevation = 0.dp,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
        ) {
            items.forEach { screen ->
                val isSelected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1.1f else 1f,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "nav_scale"
                )

                NavigationBarItem(
                    selected = isSelected,
                    onClick  = { onNavigate(screen) },
                    icon = {
                        Icon(
                            imageVector = if (isSelected) screen.icon else screen.outlinedIcon,
                            contentDescription = screen.label,
                            modifier = Modifier
                                .size(22.dp)
                                .scale(scale)
                        )
                    },
                    label = {
                        Text(
                            text = screen.label,
                            fontSize = 9.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            letterSpacing = 0.2.sp
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor   = GoldBright,
                        selectedTextColor   = GoldBright,
                        indicatorColor      = ForestMid.copy(alpha = 0.25f),
                        unselectedIconColor = MutedGray,
                        unselectedTextColor = MutedGray
                    )
                )
            }
        }
    }
}

sealed class Screen(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val outlinedIcon: androidx.compose.ui.graphics.vector.ImageVector
) {
    object Map         : Screen("map",         "Explore",     Icons.Filled.Map,         Icons.Outlined.Map)
    object Feed        : Screen("feed",        "Chronicles",  Icons.Filled.DynamicFeed, Icons.Outlined.DynamicFeed)
    object Leaderboard : Screen("leaderboard", "Rankings",    Icons.Filled.Leaderboard, Icons.Outlined.Leaderboard)
    object Guilds      : Screen("guilds",      "Guilds",      Icons.Filled.Group,       Icons.Outlined.Group)
    object Profile     : Screen("profile",     "Journal",     Icons.Filled.Person,      Icons.Outlined.Person)
}
