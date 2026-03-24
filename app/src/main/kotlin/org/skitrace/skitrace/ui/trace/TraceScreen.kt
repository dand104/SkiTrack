package org.skitrace.skitrace.ui.trace

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DownhillSkiing
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import org.skitrace.skitrace.SkiTraceApplication
import org.skitrace.skitrace.core.model.SkiStatistics
import java.util.concurrent.TimeUnit

@Composable
fun TraceScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as SkiTraceApplication
    val viewModel: TraceViewModel = viewModel(
        factory = TraceViewModel.Factory(app, app.trackerRepository)
    )

    val stats by viewModel.stats.collectAsState()
    val isTracking by viewModel.isTracking.collectAsState()
    val isPaused by viewModel.isPaused.collectAsState()
    val statusLabel by viewModel.currentStateLabel.collectAsState()

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        hasPermission = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) {
            launcher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACTIVITY_RECOGNITION
                )
            )
        }
    }

    if (!hasPermission) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Permissions required to track")
        }
        return
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            StatusChip(statusLabel, isPaused)
            Spacer(modifier = Modifier.height(24.dp))

            ExpressiveMetricsGrid(stats = stats, modifier = Modifier.weight(1f))

            Spacer(modifier = Modifier.height(24.dp))

            ControlButtons(
                isTracking = isTracking,
                isPaused = isPaused,
                onToggleTracking = { viewModel.toggleTracking() },
                onTogglePause = { viewModel.togglePause() },
                modifier = Modifier.padding(bottom = 32.dp)
            )
        }
    }
}

@Composable
fun StatusChip(label: String, isPaused: Boolean) {
    Surface(
        color = if (isPaused) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(50),
        modifier = Modifier.padding(top = 16.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
            color = if (isPaused) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ExpressiveMetricsGrid(stats: SkiStatistics, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ExpressiveMetricCard(
                modifier = Modifier.weight(1f),
                label = "Speed",
                value = stats.currentSpeedMs * 3.6,
                unit = "km/h",
                icon = Icons.Default.Speed,
                format = "%.0f",
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
            ExpressiveMetricCard(
                modifier = Modifier.weight(1f),
                label = "Runs",
                value = stats.descentsCount.toDouble(),
                unit = "",
                icon = Icons.Default.DownhillSkiing,
                format = "%.0f",
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ExpressiveMetricCard(
                modifier = Modifier.weight(1f),
                label = "Distance",
                value = stats.totalDistanceMeters / 1000.0,
                unit = "km",
                icon = Icons.Default.Route,
                format = "%.1f"
            )
            ExpressiveMetricCard(
                modifier = Modifier.weight(1f),
                label = "Vert Drop",
                value = stats.verticalDropMeters,
                unit = "m",
                icon = Icons.Default.TrendingDown,
                format = "%.0f"
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ExpressiveMetricCard(
                modifier = Modifier.weight(1f),
                label = "Duration",
                value = 0.0,
                unit = "",
                icon = Icons.Default.Timer,
                format = "",
                customValueStr = formatDuration(stats.totalDurationMs)
            )
            ExpressiveMetricCard(
                modifier = Modifier.weight(1f),
                label = "Altitude",
                value = stats.currentAltitude,
                unit = "m",
                icon = Icons.Default.Terrain,
                format = "%.0f"
            )
        }
    }
}

@Composable
fun ExpressiveMetricCard(
    modifier: Modifier = Modifier,
    label: String,
    value: Double,
    unit: String,
    icon: ImageVector,
    format: String,
    customValueStr: String? = null,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Card(
        modifier = modifier.aspectRatio(1.5f),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor.copy(alpha = 0.8f),
                modifier = Modifier.size(28.dp)
            )

            Spacer(Modifier.weight(1f))

            Column {
                if (customValueStr != null) {
                    Text(
                        text = customValueStr,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = contentColor
                    )
                } else {
                    RollingNumber(
                        value = value,
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        format = format,
                        color = contentColor
                    )
                }
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = contentColor.copy(alpha = 0.8f)
                    )
                    if (unit.isNotEmpty()) {
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = unit,
                            style = MaterialTheme.typography.labelSmall,
                            color = contentColor.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RollingNumber(
    value: Double,
    style: androidx.compose.ui.text.TextStyle,
    format: String,
    color: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier
) {
    val animatedValue by animateFloatAsState(
        targetValue = value.toFloat(),
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "rolling_number"
    )

    val text = remember(animatedValue, format) {
        format.format(animatedValue)
    }

    Text(
        text = text,
        style = style,
        modifier = modifier,
        color = color
    )
}

@Composable
fun ControlButtons(
    isTracking: Boolean,
    isPaused: Boolean,
    onToggleTracking: () -> Unit,
    onTogglePause: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (isTracking) {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledIconButton(
                onClick = onTogglePause,
                modifier = Modifier.size(72.dp),
                shape = CircleShape,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Icon(
                    imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                    contentDescription = if (isPaused) "Resume" else "Pause",
                    modifier = Modifier.size(32.dp)
                )
            }

            HoldToInteractButton(
                icon = Icons.Default.Stop,
                containerColor = MaterialTheme.colorScheme.errorContainer,
                iconColor = MaterialTheme.colorScheme.onErrorContainer,
                progressColor = MaterialTheme.colorScheme.error,
                onAction = onToggleTracking
            )
        }
    } else {
        Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            HoldToInteractButton(
                icon = Icons.Default.PlayArrow,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                iconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                progressColor = Color.Transparent,
                onAction = onToggleTracking,
                requiresHold = false
            )
        }
    }
}

@Composable
fun HoldToInteractButton(
    icon: ImageVector,
    containerColor: Color,
    iconColor: Color,
    progressColor: Color,
    onAction: () -> Unit,
    requiresHold: Boolean = true,
    modifier: Modifier = Modifier
) {
    val holdDuration = 1000L

    var isPressed by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    val scale by animateFloatAsState(if (isPressed) 0.95f else 1f, label = "scale")

    LaunchedEffect(isPressed) {
        if (isPressed && requiresHold) {
            val startTime = System.currentTimeMillis()
            while (isPressed && progress < 1f) {
                val elapsed = System.currentTimeMillis() - startTime
                progress = (elapsed.toFloat() / holdDuration).coerceAtMost(1f)
                if (progress >= 1f) {
                    onAction()
                    isPressed = false
                }
                delay(16)
            }
        } else {
            while (progress > 0f) {
                progress = (progress - 0.1f).coerceAtLeast(0f)
                delay(16)
            }
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(90.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(containerColor)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        if (requiresHold) {
                            isPressed = true
                            tryAwaitRelease()
                            isPressed = false
                        } else {
                            onAction()
                        }
                    }
                )
            }
            .drawBehind {
                if (requiresHold && progress > 0f) {
                    drawArc(
                        color = progressColor,
                        startAngle = -90f,
                        sweepAngle = 360f * progress,
                        useCenter = false,
                        style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round),
                        size = Size(size.width - 12.dp.toPx(), size.height - 12.dp.toPx()),
                        topLeft = Offset(6.dp.toPx(), 6.dp.toPx())
                    )
                }
            }
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(42.dp)
        )
    }
}

private fun formatDuration(millis: Long): String {
    val hours = TimeUnit.MILLISECONDS.toHours(millis)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
    return if (hours > 0) {
        "%02d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}
