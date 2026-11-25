package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
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

@Composable
fun RolaysApp() {
    var selectedDestination by rememberSaveable { mutableStateOf(RolaysDestination.ENHANCER) }
    val glass = LocalLiquidGlassPalette.current

    // Keep feature ViewModels scoped at app level so tab switching preserves user work seamlessly
    val enhancerViewModel: EnhancerViewModel = viewModel()
    val downloaderViewModel: DownloaderViewModel = viewModel()
    val socialAiViewModel: SocialAiViewModel = viewModel()
    val geminiFlashViewModel: GeminiFlashViewModel = viewModel()

    BackHandler(enabled = selectedDestination != RolaysDestination.ENHANCER) {
        selectedDestination = RolaysDestination.ENHANCER
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
                    onDestinationSelected = { selectedDestination = it }
                )

                Column(modifier = Modifier.weight(1f).fillMaxSize()) {
                    RolaysTopBrandBar(selectedDestination = selectedDestination)
                    RolaysContentSwitcher(
                        selectedDestination = selectedDestination,
                        enhancerViewModel = enhancerViewModel,
                        downloaderViewModel = downloaderViewModel,
                        socialAiViewModel = socialAiViewModel,
                        geminiFlashViewModel = geminiFlashViewModel,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.statusBars)
                ) {
                    RolaysTopBrandBar(selectedDestination = selectedDestination)
                    RolaysContentSwitcher(
                        selectedDestination = selectedDestination,
                        enhancerViewModel = enhancerViewModel,
                        downloaderViewModel = downloaderViewModel,
                        socialAiViewModel = socialAiViewModel,
                        geminiFlashViewModel = geminiFlashViewModel,
                        modifier = Modifier.weight(1f)
                    )
                }

                InteractiveLiquidGlassNavBar(
                    selectedDestination = selectedDestination,
                    onDestinationSelected = { selectedDestination = it },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
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
            .padding(horizontal = 18.dp, vertical = 8.dp),
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
private fun RolaysContentSwitcher(
    selectedDestination: RolaysDestination,
    enhancerViewModel: EnhancerViewModel,
    downloaderViewModel: DownloaderViewModel,
    socialAiViewModel: SocialAiViewModel,
    geminiFlashViewModel: GeminiFlashViewModel,
    modifier: Modifier = Modifier
) {
    Crossfade(
        targetState = selectedDestination,
        animationSpec = tween(durationMillis = 220),
        modifier = modifier,
        label = "rolays_tab_crossfade"
    ) { destination ->
        when (destination) {
            RolaysDestination.ENHANCER -> EnhancerScreen(viewModel = enhancerViewModel)
            RolaysDestination.DOWNLOADER -> DownloaderScreen(viewModel = downloaderViewModel)
            RolaysDestination.SOCIAL_CHAT -> SocialAiScreen(viewModel = socialAiViewModel)
            RolaysDestination.GEMINI_FLASH -> GeminiFlashScreen(viewModel = geminiFlashViewModel)
        }
    }
}
