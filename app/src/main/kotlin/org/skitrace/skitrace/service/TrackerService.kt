package org.skitrace.skitrace.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.skitrace.skitrace.MainActivity
import org.skitrace.skitrace.R
import org.skitrace.skitrace.SkiTraceApplication

class TrackerService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var notificationManager: NotificationManager

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val repository = (application as SkiTraceApplication).trackerRepository
        when (intent?.action) {
            ACTION_START -> startTracking()
            ACTION_STOP -> stopTracking()
            ACTION_PAUSE -> repository.pauseTracking()
            ACTION_RESUME -> repository.resumeTracking()
        }
        return START_STICKY
    }

    private fun startTracking() {
        startForeground(
            NOTIFICATION_ID,
            buildNotification("Waiting for GPS...", false),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        )
        val repository = (application as SkiTraceApplication).trackerRepository

        repository.startTracking(serviceScope)

        combine(repository.currentStats, repository.isPaused) { stats, isPaused ->
            val content = if (isPaused) {
                "Paused - %.1f km".format(stats.totalDistanceMeters() / 1000.0)
            } else {
                "Dist: %.1f km | Speed: %.0f km/h".format(
                    stats.totalDistanceMeters() / 1000.0,
                    stats.currentSpeedMs() * 3.6
                )
            }
            updateNotification(content, isPaused)
        }.launchIn(serviceScope)
    }

    private fun stopTracking() {
        val repository = (application as SkiTraceApplication).trackerRepository
        repository.stopTracking()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "SkiTrace Tracking",
            NotificationManager.IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(channel)
    }

    private fun buildNotification(content: String, isPaused: Boolean): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, TrackerService::class.java).apply { action = ACTION_STOP }
        val stopPendingIntent = PendingIntent.getService(this, 1, stopIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val pauseIntent = Intent(this, TrackerService::class.java).apply { action = if (isPaused) ACTION_RESUME else ACTION_PAUSE }
        val pausePendingIntent = PendingIntent.getService(this, 2, pauseIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SkiTrace Recording")
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_notification_ski)
            .setContentIntent(pendingIntent)
            .addAction(
                if (isPaused) android.R.drawable.ic_media_play else android.R.drawable.ic_media_pause,
                if (isPaused) "Resume" else "Pause",
                pausePendingIntent
            )
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)
            .setOngoing(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun updateNotification(content: String, isPaused: Boolean) {
        notificationManager.notify(NOTIFICATION_ID, buildNotification(content, isPaused))
    }

    companion object {
        const val CHANNEL_ID = "tracker_channel"
        const val NOTIFICATION_ID = 1
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_PAUSE = "ACTION_PAUSE"
        const val ACTION_RESUME = "ACTION_RESUME"
    }
}