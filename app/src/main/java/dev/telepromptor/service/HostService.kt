package dev.telepromptor.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import dagger.hilt.android.AndroidEntryPoint
import dev.telepromptor.MainActivity
import dev.telepromptor.bluetooth.LiveSession
import dev.telepromptor.protocol.Role
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class HostService : Service() {
    @Inject lateinit var live: LiveSession

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        wakeLock =
            getSystemService(PowerManager::class.java)
                ?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Telepromptor:host")
                ?.apply {
                    setReferenceCounted(false)
                }
        scope.launch {
            live.session.view.collect { view ->
                val lock = wakeLock ?: return@collect
                if (view.peers.isNotEmpty() && !lock.isHeld) lock.acquire()
                if (view.peers.isEmpty() && lock.isHeld) lock.release()
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        if (intent?.action == STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        val stop =
            PendingIntent.getService(
                this,
                1,
                Intent(this, HostService::class.java).setAction(STOP),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        val open =
            PendingIntent.getActivity(
                this,
                2,
                Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, "Bluetooth host", NotificationManager.IMPORTANCE_LOW),
        )
        startForeground(
            11,
            Notification
                .Builder(this, CHANNEL)
                .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
                .setContentTitle("Telepromptor host")
                .setContentText("Bluetooth session is active")
                .setContentIntent(open)
                .setOngoing(true)
                .addAction(Notification.Action.Builder(null, "Stop hosting", stop).build())
                .build(),
        )
        live.session.begin(Role.HOST)
        if (live.radio.listening.value
                .isEmpty() ||
            intent?.action == RETRY
        ) {
            live.radio.listen()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
        live.radio.stopListening()
        live.session.endIf(Role.HOST)
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL = "host-session"
        private const val STOP = "dev.telepromptor.STOP_HOST"
        const val RETRY = "dev.telepromptor.RETRY_HOST"
    }
}
