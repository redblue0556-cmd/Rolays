package com.example.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.feature.downloader.DownloaderScreen
import com.example.feature.downloader.DownloaderViewModel
import com.example.feature.enhancer.EnhancerScreen
import com.example.feature.enhancer.EnhancerViewModel
import com.example.feature.geminiflash.GeminiFlashScreen
import com.example.feature.geminiflash.GeminiFlashViewModel
import com.example.feature.socialchat.SocialAiScreen
import com.example.feature.socialchat.SocialAiViewModel
import com.example.ui.components.InteractiveLiquidGlassNavBar
import com.example.ui.components.LiquidGlassAmbientBackground
import com.example.ui.components.LiquidGlassNavRail
import com.example.ui.components.RolaysDestination
import com.example.ui.theme.LocalLiquidGlassPalette

/**
 * Main Jetpack Compose Navigation Shell for ROLAYS.
 * Uses Navigation Compose (NavHost + NavController) with a floating frosted Liquid Glass
 * bottom navigation bar on Compact/Medium viewports and a Liquid Glass NavigationRail
 * on Expanded tablet/foldable screens.
 */
@Composable
fun RolaysApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val selectedDestination = RolaysDestination.entries.firstOrNull { it.route == currentRoute }
        ?: RolaysDestination.ENHANCER
    val glass = LocalLiquidGlassPalette.current

    // Keep feature ViewModels scoped to the shell so switching tabs preserves state
    val enhancerViewModel: EnhancerViewModel = viewModel()
    val downloaderViewModel: DownloaderViewModel = viewModel()
    val socialAiViewModel: SocialAiViewModel = viewModel()
    val geminiFlashViewModel: GeminiFlashViewModel = viewModel()

    val navigateToDestination: (RolaysDestination) -> Unit = { target ->
        if (target.route != currentRoute) {
            navController.navigate(target.route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    val ambientAccent = when (selectedDestination) {
        RolaysDestination.ENHANCER -> glass.cyanAccent
        RolaysDestination.DOWNLOADER -> glass.blueAccent
        RolaysDestination.SOCIAL_CHAT -> glass.violetAccent
        RolaysDestination.GEMINI_FLASH -> glass.mintAccent
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpandedScreen = maxWidth >= 840.dp

        LiquidGlassAmbientBackground(accentColor = ambientAccent)

        if (isExpandedScreen) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
            ) {
                LiquidGlassNavRail(
                    selectedDestination = selectedDestination,
                    onDestinationSelected = navigateToDestination
                )

                Column(modifier = Modifier.weight(1f).fillMaxSize()) {
                    if (selectedDestination != RolaysDestination.SOCIAL_CHAT) {
                        RolaysTopBrandBar(selectedDestination = selectedDestination)
                    }
                    RolaysNavHost(
                        navController = navController,
                        enhancerViewModel = enhancerViewModel,
                        downloaderViewModel = downloaderViewModel,
                        socialAiViewModel = socialAiViewModel,
                        geminiFlashViewModel = geminiFlashViewModel,
                        onNavigateHome = { navigateToDestination(RolaysDestination.ENHANCER) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        } else {
            Scaffold(
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                bottomBar = {
                    InteractiveLiquidGlassNavBar(
                        selectedDestination = selectedDestination,
                        onDestinationSelected = navigateToDestination
                    )
                }
            ) { _ ->
                // Content renders full-bleed behind the floating frosted Liquid Glass bar
                Box(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .windowInsetsPadding(WindowInsets.statusBars)
                    ) {
                        if (selectedDestination != RolaysDestination.SOCIAL_CHAT) {
                            RolaysTopBrandBar(selectedDestination = selectedDestination)
                        }
                        RolaysNavHost(
                            navController = navController,
                            enhancerViewModel = enhancerViewModel,
                            downloaderViewModel = downloaderViewModel,
                            socialAiViewModel = socialAiViewModel,
                            geminiFlashViewModel = geminiFlashViewModel,
                            onNavigateHome = { navigateToDestination(RolaysDestination.ENHANCER) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RolaysTopBrandBar(
    selectedDestination: RolaysDestination
) {
    val glass = LocalLiquidGlassPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "ROLAYS",
            style = MaterialTheme.typography.titleLarge,
            color = glass.textPrimary
        )
        Text(
            text = if (selectedDestination == RolaysDestination.GEMINI_FLASH) {
                "GEMINI 3.8 FLASH"
            } else {
                "LIQUID GLASS • TAHAN & GESER BAR"
            },
            style = MaterialTheme.typography.labelSmall,
            color = glass.cyanAccent
        )
    }
}

@Composable
private fun RolaysNavHost(
    navController: NavHostController,
    enhancerViewModel: EnhancerViewModel,
    downloaderViewModel: DownloaderViewModel,
    socialAiViewModel: SocialAiViewModel,
    geminiFlashViewModel: GeminiFlashViewModel,
    onNavigateHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = RolaysDestination.ENHANCER.route,
        modifier = modifier,
        enterTransition = { fadeIn(animationSpec = tween(220)) },
        exitTransition = { fadeOut(animationSpec = tween(180)) }
    ) {
        composable(RolaysDestination.ENHANCER.route) {
            EnhancerScreen(viewModel = enhancerViewModel)
        }
        composable(RolaysDestination.DOWNLOADER.route) {
            DownloaderScreen(viewModel = downloaderViewModel)
        }
        composable(RolaysDestination.SOCIAL_CHAT.route) {
            SocialAiScreen(
                onExitToHome = onNavigateHome,
                viewModel = socialAiViewModel
            )
        }
        composable(RolaysDestination.GEMINI_FLASH.route) {
            GeminiFlashScreen(viewModel = geminiFlashViewModel)
        }
    }
}
