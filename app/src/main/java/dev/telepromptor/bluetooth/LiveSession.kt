package dev.telepromptor.bluetooth

import android.annotation.SuppressLint
import android.os.Build
import android.os.SystemClock
import dev.telepromptor.protocol.Session
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

@Singleton
class LiveSession @Inject constructor(val radio: RfcommTransport) {
    @SuppressLint("MissingPermission")
    val session = Session(
        radio,
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
        SystemClock::elapsedRealtime,
    ) { runCatching { radio.adapter?.name }.getOrNull() ?: Build.MODEL }
}
