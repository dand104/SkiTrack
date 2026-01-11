package org.skitrace.skitrace.data.export

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.withContext
import org.skitrace.skitrace.data.db.SkiDatabase
import org.skitrace.skitrace.data.util.DispatcherProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class GpxExporter(
    private val context: Context,
    private val database: SkiDatabase,
    private val dispatchers: DispatcherProvider
) {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    suspend fun exportRunToGpx(runId: Long): Uri? = withContext(dispatchers.io) {
        val run = database.trackDao().getRunById(runId) ?: return@withContext null
        val points = database.trackDao().getPointsForRun(runId)

        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        sb.append("<gpx version=\"1.1\" creator=\"SkiTrace Android\" xmlns=\"http://www.topografix.com/GPX/1/1\">\n")
        sb.append("  <metadata>\n")
        sb.append("    <name>Ski Run ${dateFormat.format(Date(run.startTime))}</name>\n")
        sb.append("  </metadata>\n")
        sb.append("  <trk>\n")
        sb.append("    <name>Ski Session</name>\n")
        sb.append("    <trkseg>\n")

        points.forEach { p ->
            sb.append("      <trkpt lat=\"${p.latitude}\" lon=\"${p.longitude}\">\n")
            sb.append("        <ele>${p.altitude}</ele>\n")
            sb.append("        <time>${dateFormat.format(Date(p.timestamp))}</time>\n")
            sb.append("        <extensions>\n")
            sb.append("          <speed>${p.speedMs}</speed>\n")
            sb.append("        </extensions>\n")
            sb.append("      </trkpt>\n")
        }

        sb.append("    </trkseg>\n")
        sb.append("  </trk>\n")
        sb.append("</gpx>")

        try {
            val fileName = "skitrace_${run.startTime}.gpx"
            val file = File(context.cacheDir, fileName)
            file.writeText(sb.toString())

            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
