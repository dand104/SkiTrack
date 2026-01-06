package org.skitrace.skitrace.ui.trace

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import org.skitrace.skitrace.SkiTraceApplication
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(
                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = formatDuration(stats.durationMs()),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
                AnimatedContent(
                    targetState = statusLabel,
                    transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) }, label = "Status"
                ) { label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            MetricCard(
                modifier = Modifier.weight(1f),
                title = "Speed",
                value = "%.1f".format(stats.currentSpeedMs() * 3.6),
                unit = "km/h"
            )
            MetricCard(
                modifier = Modifier.weight(1f),
                title = "Altitude",
                value = "%.0f".format(stats.currentAltitude()),
                unit = "m"
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            MetricCard(
                modifier = Modifier.weight(1f),
                title = "Distance",
                value = "%.2f".format(stats.totalDistanceMeters() / 1000.0),
                unit = "km"
            )
            MetricCard(
                modifier = Modifier.weight(1f),
                title = "Vert. Drop",
                value = "%.0f".format(stats.verticalDropMeters()),
                unit = "m"
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        val buttonColor by animateColorAsState(
            if (isTracking) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            label = "BtnColor"
        )

        Button(
            onClick = { viewModel.toggleTracking() },
            modifier = Modifier
                .size(96.dp)
                .padding(bottom = 16.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
            elevation = ButtonDefaults.buttonElevation(8.dp)
        ) {
            Icon(
                imageVector = if (isTracking) Icons.Default.Stop else Icons.Default.PlayArrow,
                contentDescription = if (isTracking) "Stop" else "Start",
                modifier = Modifier.size(48.dp)
            )
        }
    }
}

@Composable
fun MetricCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    unit: String
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = unit,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatDuration(millis: Long): String {
    val hours = TimeUnit.MILLISECONDS.toHours(millis)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}