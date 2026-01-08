package org.skitrace.skitrace.ui.trace

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import org.skitrace.skitrace.SkiTraceApplication
import org.skitrace.skitrace.core.model.SkiStatistics
import org.skitrace.skitrace.ui.theme.AppTypography
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            StatusChip(statusLabel)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val speedVal = (stats.currentSpeedMs * 3.6)

                RollingNumber(
                    value = speedVal,
                    style = AppTypography.displayLarge,
                    format = "%.0f"
                )
                Text(
                    text = "km/h",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            SecondaryMetricsGrid(stats)

            Spacer(modifier = Modifier.height(24.dp))

            HoldToInteractButton(
                isTracking = isTracking,
                onToggle = { viewModel.toggleTracking() },
                modifier = Modifier.padding(bottom = 32.dp)
            )
        }
    }
}

@Composable
fun StatusChip(label: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(50),
        modifier = Modifier.padding(top = 16.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun SecondaryMetricsGrid(stats: SkiStatistics) {
    Column(
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            CompactMetricItem(
                value = stats.currentAltitude,
                label = "Altitude",
                unit = "m",
                format = "%.0f"
            )
            CompactMetricItem(
                value = stats.verticalDropMeters,
                label = "Vert. Drop",
                unit = "m",
                format = "%.0f"
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            CompactMetricItem(
                value = stats.totalDistanceMeters / 1000.0,
                label = "Distance",
                unit = "km",
                format = "%.1f"
            )
            val durationStr = formatDuration(stats.durationMs)
            MetricStaticItem(
                value = durationStr,
                label = "Duration"
            )
        }
    }
}

@Composable
fun CompactMetricItem(
    value: Double,
    label: String,
    unit: String,
    format: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        RollingNumber(
            value = value,
            style = AppTypography.displayMedium,
            format = format
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun MetricStaticItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = AppTypography.displayMedium,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}


@Composable
fun RollingNumber(
    value: Double,
    style: androidx.compose.ui.text.TextStyle,
    format: String,
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
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
fun HoldToInteractButton(
    isTracking: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val scope = rememberCoroutineScope()

    val holdDuration = 1000L

    var isPressed by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }

    val primaryColor = MaterialTheme.colorScheme.primary
    val errorColor = MaterialTheme.colorScheme.error
    val containerColor = if (isTracking) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.primaryContainer
    val iconColor = if (isTracking) errorColor else MaterialTheme.colorScheme.onPrimaryContainer

    val scale by animateFloatAsState(if (isPressed) 0.95f else 1f, label = "scale")

    LaunchedEffect(isPressed) {
        if (isPressed) {
            val startTime = System.currentTimeMillis()
            while (isPressed && progress < 1f) {
                val elapsed = System.currentTimeMillis() - startTime
                progress = (elapsed.toFloat() / holdDuration).coerceAtMost(1f)
                if (progress >= 1f) {
                    onToggle()
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
            .size(110.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(40.dp))
            .background(containerColor)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        if (isTracking) {
                            isPressed = true
                            tryAwaitRelease()
                            isPressed = false
                        } else {
                            onToggle()
                        }
                    }
                )
            }
            .drawBehind {
                if (isTracking && progress > 0f) {
                    drawArc(
                        color = errorColor,
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
            imageVector = if (isTracking) Icons.Default.Stop else Icons.Default.PlayArrow,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(48.dp)
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
