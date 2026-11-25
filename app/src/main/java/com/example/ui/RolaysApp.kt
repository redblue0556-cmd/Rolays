package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
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
import com.example.ui.components.LiquidGlassSurface
import com.example.ui.components.RolaysDestination
import com.example.ui.components.WorkflowHubModalSheet
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
    var showWorkflowHub by rememberSaveable { mutableStateOf(false) }

    // Keep feature ViewModels scoped to the shell so switching tabs preserves state
    val enhancerViewModel: EnhancerViewModel = viewModel()
    val downloaderViewModel: DownloaderViewModel = viewModel()
    val socialAiViewModel: SocialAiViewModel = viewModel()
    val geminiFlashViewModel: GeminiFlashViewModel = viewModel()

    val navigateToDestination: (RolaysDestination) -> Unit = { target ->
        val activeRoute = navController.currentBackStackEntry?.destination?.route
        if (target.route != activeRoute) {
            navController.navigate(target.route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    val targetAmbientAccent = when (selectedDestination) {
        RolaysDestination.ENHANCER -> glass.cyanAccent
        RolaysDestination.DOWNLOADER -> glass.blueAccent
        RolaysDestination.SOCIAL_CHAT -> glass.violetAccent
        RolaysDestination.GEMINI_FLASH -> glass.mintAccent
    }
    val ambientAccent by animateColorAsState(
        targetValue = targetAmbientAccent,
        animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
        label = "ambient_accent"
    )

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
                    AnimatedVisibility(
                        visible = selectedDestination != RolaysDestination.SOCIAL_CHAT,
                        enter = expandVertically(tween(240, easing = FastOutSlowInEasing)) + fadeIn(tween(220)),
                        exit = shrinkVertically(tween(200, easing = FastOutSlowInEasing)) + fadeOut(tween(180))
                    ) {
                        RolaysTopBrandBar(
                            selectedDestination = selectedDestination,
                            onOpenWorkflowHub = { showWorkflowHub = true }
                        )
                    }
                    RolaysNavHost(
                        navController = navController,
                        enhancerViewModel = enhancerViewModel,
                        downloaderViewModel = downloaderViewModel,
                        socialAiViewModel = socialAiViewModel,
                        geminiFlashViewModel = geminiFlashViewModel,
                        onNavigateToTab = navigateToDestination,
                        onOpenWorkflowHub = { showWorkflowHub = true },
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
                        AnimatedVisibility(
                            visible = selectedDestination != RolaysDestination.SOCIAL_CHAT,
                            enter = expandVertically(tween(240, easing = FastOutSlowInEasing)) + fadeIn(tween(220)),
                            exit = shrinkVertically(tween(200, easing = FastOutSlowInEasing)) + fadeOut(tween(180))
                        ) {
                            RolaysTopBrandBar(
                                selectedDestination = selectedDestination,
                                onOpenWorkflowHub = { showWorkflowHub = true }
                            )
                        }
                        RolaysNavHost(
                            navController = navController,
                            enhancerViewModel = enhancerViewModel,
                            downloaderViewModel = downloaderViewModel,
                            socialAiViewModel = socialAiViewModel,
                            geminiFlashViewModel = geminiFlashViewModel,
                            onNavigateToTab = navigateToDestination,
                            onOpenWorkflowHub = { showWorkflowHub = true },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        if (showWorkflowHub) {
            WorkflowHubModalSheet(
                onDismiss = { showWorkflowHub = false },
                onSelectWorkflow = { target -> navigateToDestination(target) }
            )
        }
    }
}

@Composable
private fun RolaysTopBrandBar(
    selectedDestination: RolaysDestination,
    onOpenWorkflowHub: () -> Unit
) {
    val glass = LocalLiquidGlassPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "ROLAYS",
                style = MaterialTheme.typography.titleLarge,
                color = glass.textPrimary
            )
            Text(
                text = if (selectedDestination == RolaysDestination.GEMINI_FLASH) {
                    "MODUL GEMINI 3.8 FLASH"
                } else {
                    "TAHAN & GESER BAR NAVIGASI"
                },
                style = MaterialTheme.typography.labelSmall,
                color = glass.cyanAccent
            )
        }

        LiquidGlassSurface(
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("open_workflow_hub_button"),
            shape = RoundedCornerShape(22.dp),
            cornerRadiusDp = 22.dp,
            tintColor = glass.cyanAccent.copy(alpha = 0.24f),
            elevated = true,
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            onClick = onOpenWorkflowHub
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Route,
                    contentDescription = "Buka Alur Kerja ROLAYS",
                    tint = glass.cyanAccent,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Alur Kerja",
                    style = MaterialTheme.typography.labelMedium,
                    color = glass.textPrimary
                )
            }
        }
    }
}

@Composable
private fun RolaysNavHost(
    navController: NavHostController,
    enhancerViewModel: EnhancerViewModel,
    downloaderViewModel: DownloaderViewModel,
    socialAiViewModel: SocialAiViewModel,
    geminiFlashViewModel: GeminiFlashViewModel,
    onNavigateToTab: (RolaysDestination) -> Unit,
    onOpenWorkflowHub: () -> Unit,
    modifier: Modifier = Modifier
) {
    val destinationOrder = RolaysDestination.entries
    fun routeIndex(route: String?): Int =
        destinationOrder.indexOfFirst { it.route == route }.takeIf { it >= 0 } ?: 0

    NavHost(
        navController = navController,
        startDestination = RolaysDestination.ENHANCER.route,
        modifier = modifier,
        enterTransition = {
            val fromIdx = routeIndex(initialState.destination.route)
            val toIdx = routeIndex(targetState.destination.route)
            val forward = toIdx >= fromIdx
            slideInHorizontally(
                animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
                initialOffsetX = { width -> if (forward) (width * 0.16f).toInt() else -(width * 0.16f).toInt() }
            ) + fadeIn(
                animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing)
            ) + scaleIn(
                initialScale = 0.985f,
                animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
            )
        },
        exitTransition = {
            val fromIdx = routeIndex(initialState.destination.route)
            val toIdx = routeIndex(targetState.destination.route)
            val forward = toIdx >= fromIdx
            slideOutHorizontally(
                animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
                targetOffsetX = { width -> if (forward) -(width * 0.10f).toInt() else (width * 0.10f).toInt() }
            ) + fadeOut(
                animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
            )
        }
    ) {
        composable(RolaysDestination.ENHANCER.route) {
            EnhancerScreen(
                onSendToSocialAi = { bitmap ->
                    socialAiViewModel.attachBitmapFromWorkflow(
                        bitmap = bitmap,
                        suggestedPrompt = "Buatkan caption & hashtag menarik untuk foto HD ini."
                    )
                    onNavigateToTab(RolaysDestination.SOCIAL_CHAT)
                },
                viewModel = enhancerViewModel
            )
        }
        composable(RolaysDestination.DOWNLOADER.route) {
            DownloaderScreen(
                onSendToSocialAi = { draftPrompt ->
                    socialAiViewModel.prefillPromptFromWorkflow(draftPrompt)
                    onNavigateToTab(RolaysDestination.SOCIAL_CHAT)
                },
                onSendToEnhancer = { bitmap, title ->
                    enhancerViewModel.loadBitmapForEnhance(bitmap, title)
                    onNavigateToTab(RolaysDestination.ENHANCER)
                },
                viewModel = downloaderViewModel
            )
        }
        composable(RolaysDestination.SOCIAL_CHAT.route) {
            SocialAiScreen(
                onExitToHome = onOpenWorkflowHub,
                viewModel = socialAiViewModel
            )
        }
        composable(RolaysDestination.GEMINI_FLASH.route) {
            GeminiFlashScreen(viewModel = geminiFlashViewModel)
        }
    }
}
