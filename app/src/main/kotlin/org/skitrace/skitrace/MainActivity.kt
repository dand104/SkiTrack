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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import org.skitrace.skitrace.ui.map.MapScreen
import org.skitrace.skitrace.ui.theme.SkiTraceTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        org.maplibre.android.MapLibre.getInstance(this)
        setContent {
            SkiTraceTheme {
                MainApp()
            }
        }
    }
}

@Composable
fun MainApp() {
    val navController = rememberNavController()
    val screens = listOf(
        Screen.Trace,
        Screen.Map,
        Screen.Stats
    )
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            NavigationBar(
                containerColor = if (currentRoute == Screen.Map.route) Color.Transparent else MaterialTheme.colorScheme.surface,
                tonalElevation = if (currentRoute == Screen.Map.route) 0.dp else 3.dp
            ) {
                val currentDestination = navBackStackEntry?.destination

                screens.forEach { screen ->
                    val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = null) },
                        label = { Text(screen.title) },
                        selected = selected,
                        onClick = {
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
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Map.route,
            modifier = Modifier.fillMaxSize(),
            enterTransition = { fadeIn(animationSpec = tween(300)) },
            exitTransition = { fadeOut(animationSpec = tween(300)) },
            popEnterTransition = { fadeIn(animationSpec = tween(300)) },
            popExitTransition = { fadeOut(animationSpec = tween(300)) }


        ) {
            composable(Screen.Trace.route) {
                Box(Modifier.padding(innerPadding)) { TraceScreen() }
            }
            composable(Screen.Map.route) {
                MapScreen()
            }
            composable(Screen.Stats.route) {
                Box(Modifier.padding(innerPadding)) { StatsScreen() }
            }
        }
    }
}

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Trace : Screen("trace", "Trace", Icons.Default.Timeline)
    data object Map : Screen("map", "Map", Icons.Default.Map)
    data object Stats : Screen("stats", "Stats", Icons.Default.BarChart)
}

@Composable
@Preview
fun TraceScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Trace Recording Screen")
    }
}

@Composable
@Preview
fun StatsScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Statistics Screen")
    }
}