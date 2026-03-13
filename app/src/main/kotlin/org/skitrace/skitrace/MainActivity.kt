package org.skitrace.skitrace

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.arkivanov.decompose.defaultComponentContext
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import org.skitrace.skitrace.ui.details.TrackDetailsScreen
import org.skitrace.skitrace.ui.map.MapScreen
import org.skitrace.skitrace.ui.navigation.RootComponent
import org.skitrace.skitrace.ui.navigation.TabLifecycleAware
import org.skitrace.skitrace.ui.settings.SettingsScreen
import org.skitrace.skitrace.ui.stats.StatsScreen
import org.skitrace.skitrace.ui.theme.SkiTraceTheme
import org.skitrace.skitrace.ui.trace.TraceScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val rootComponent = RootComponent(defaultComponentContext())

        setContent {
            SkiTraceTheme {
                RootContent(rootComponent)
            }
        }
    }
}

@Composable
fun RootContent(component: RootComponent) {
    Children(
        stack = component.stack,
        animation = stackAnimation(fade())
    ) {
        when (val child = it.instance) {
            is RootComponent.Child.MainTabs -> MainTabsScreen(
                onNavigateToSettings = component::navigateToSettings,
                onNavigateToDetails = component::navigateToDetails
            )
            is RootComponent.Child.Settings -> SettingsScreen(onBack = component::navigateBack)
            is RootComponent.Child.Details -> TrackDetailsScreen(runId = child.runId, onBack = component::navigateBack)
        }
    }
}

enum class TabConfig(val title: String, val icon: ImageVector) {
    Trace("Tracker", Icons.Default.Timeline),
    Map("Map", Icons.Default.Map),
    Stats("History", Icons.Default.BarChart)
}

@Composable
fun MainTabsScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToDetails: (Long) -> Unit
) {
    var activeTab by remember { mutableStateOf(TabConfig.Trace) }

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            val isMapScreen = activeTab == TabConfig.Map
            val navBarContainerColor = if (isMapScreen) {
                MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.7f)
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            }

            NavigationBar(
                containerColor = navBarContainerColor,
                tonalElevation = if (isMapScreen) 0.dp else 3.dp
            ) {
                TabConfig.entries.forEach { tab ->
                    NavigationBarItem(
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(tab.title) },
                        selected = activeTab == tab,
                        onClick = { activeTab = tab }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(Modifier.fillMaxSize()) {

            if (activeTab == TabConfig.Trace) {
                Box(Modifier.padding(innerPadding).fillMaxSize().zIndex(1f)) {
                    TraceScreen()
                }
            }

            val isMapActive = activeTab == TabConfig.Map
            Box(
                Modifier
                    .fillMaxSize()
                    .alpha(if (isMapActive) 1f else 0f)
                    .zIndex(if (isMapActive) 1f else -1f)
            ) {
                TabLifecycleAware(isActive = isMapActive) {
                    MapScreen(
                        contentPadding = innerPadding,
                        isActive = isMapActive
                    )
                }
            }

            if (activeTab == TabConfig.Stats) {
                Box(Modifier.fillMaxSize().zIndex(1f)) {
                    StatsScreen(
                        contentPadding = innerPadding,
                        onNavigateToSettings = onNavigateToSettings,
                        onNavigateToDetails = onNavigateToDetails
                    )
                }
            }
        }
    }
}