package org.skitrace.skitrace.data.recognition

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityRecognitionResult
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class GmsActivityClient(private val context: Context) : ActivityClient {
    private val client = ActivityRecognition.getClient(context)

    @SuppressLint("MissingPermission")
    override fun getActivityUpdates(intervalMs: Long): Flow<ActivityData> = callbackFlow {
        val action = "${context.packageName}.ACTION_ACTIVITY_UPDATE"

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (ActivityRecognitionResult.hasResult(intent)) {
                    val result = ActivityRecognitionResult.extractResult(intent)
                    result?.mostProbableActivity?.let { activity ->
                        trySend(ActivityData(activity.type, activity.confidence))
                    }
                }
            }
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(action),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        val intent = Intent(action).setPackage(context.packageName)

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, flags)

        client.requestActivityUpdates(intervalMs, pendingIntent)
            .addOnFailureListener { close(it) }

        awaitClose {
            client.removeActivityUpdates(pendingIntent)
            context.unregisterReceiver(receiver)
        }
    }
}