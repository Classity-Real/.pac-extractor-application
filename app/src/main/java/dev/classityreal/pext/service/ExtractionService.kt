package dev.classityreal.pext.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import dev.classityreal.pext.MainActivity

/**
 * Declared in the manifest as a `dataSync` foreground service.
 *
 * The MVP wiring in [dev.classityreal.pext.ui.PacViewModel] runs extraction
 * directly in a viewModelScope coroutine, which is fine while the app is in
 * the foreground but will be killed if the user backgrounds the app during a
 * large (multi-GB) extraction. Route the actual copy loop through this
 * service (or a WorkManager CoroutineWorker using
 * setForeground()/setForegroundAsync()) once you need extraction to survive
 * the user leaving the app — the PacParser/PacRandomAccess classes don't
 * care who calls them.
 */
class ExtractionService : Service() {

    private val channelId = "pac_extraction"

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification(progressText = "Starting extraction…"))
        // TODO: pull the source Uri / output tree Uri / selected entry indices out of
        // the incoming Intent's extras and drive PacParser.extractEntry() here,
        // calling updateNotification(...) as progress comes in, then stopSelf()
        // when done.
        return START_NOT_STICKY
    }

    private fun buildNotification(progressText: String): Notification {
        ensureChannel()
        val openApp = Intent(this, MainActivity::class.java)
        val pendingIntent = android.app.PendingIntent.getActivity(
            this, 0, openApp,
            android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Extracting firmware")
            .setContentText(progressText)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun updateNotification(progressText: String) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIFICATION_ID, buildNotification(progressText))
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(channelId) == null) {
            nm.createNotificationChannel(
                NotificationChannel(channelId, "Firmware extraction", NotificationManager.IMPORTANCE_LOW)
            )
        }
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
    }
}
