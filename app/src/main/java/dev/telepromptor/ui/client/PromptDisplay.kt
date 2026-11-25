package dev.telepromptor.ui.client

import android.os.SystemClock
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.telepromptor.protocol.SessionView
import kotlinx.coroutines.isActive
import kotlin.math.ceil
import kotlin.math.roundToInt

@Composable
fun rememberFrameTime(
    active: Boolean,
    key: Any,
): State<Long> =
    produceState(
        SystemClock.elapsedRealtime(),
        active,
        key,
    ) {
        value = SystemClock.elapsedRealtime()
        if (active) {
            while (isActive) {
                withFrameNanos { value = SystemClock.elapsedRealtime() }
            }
        }
    }

@Composable
fun PromptDisplay(
    view: SessionView,
    modifier: Modifier = Modifier,
    onTravelMeasured: (Int) -> Unit = {},
) {
    val playback = view.motion.state
    val frame = rememberFrameTime(playback.playing || view.countdownUntilMs != 0L, view.motion)
    val foreground = if (playback.theme == "dark") Color.White else Color.Black
    val background = if (playback.theme == "dark") Color(0xff101114) else Color(0xfffaf8f1)
    val text = view.script?.text
    val reportTravel by rememberUpdatedState(onTravelMeasured)
    var travel by remember { mutableIntStateOf(1) }
    LaunchedEffect(travel) { reportTravel(travel) }
    Box(modifier.background(background).clipToBounds()) {
        if (text == null) {
            Text("Waiting for the host’s script", Modifier.align(Alignment.Center), color = foreground)
        } else {
            Layout(
                content = {
                    Text(
                        text,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = foreground,
                        fontSize = playback.fontSizeSp.sp,
                        lineHeight = (playback.fontSizeSp * 1.25f).sp,
                        textAlign = if (playback.align == "left") TextAlign.Left else TextAlign.Center,
                    )
                },
                modifier = Modifier.fillMaxSize().graphicsLayer { scaleX = if (playback.mirror) -1f else 1f },
            ) { children, constraints ->
                val width = constraints.maxWidth
                val height = constraints.maxHeight
                val child = children.single().measure(Constraints.fixedWidth(width))
                val distance = child.height.coerceAtLeast(1)
                travel = distance
                layout(width, height) {
                    val offset = view.motion.position(frame.value)
                    child.place(0, (height * 0.30 - offset * distance).roundToInt())
                }
            }
            Canvas(Modifier.fillMaxSize()) {
                val y = size.height * 0.30f
                val end = minOf(13.dp.toPx(), size.width / 4)
                val color = if (playback.theme == "dark") Color(0xffffc36a) else Color(0xff8c4300)
                drawLine(color, Offset(0f, y), Offset(end, y), 3.dp.toPx())
                drawLine(color, Offset(size.width - end, y), Offset(size.width, y), 3.dp.toPx())
            }
        }
        if (view.countdownUntilMs != 0L) {
            val seconds = ceil((view.countdownUntilMs - frame.value) / 1000.0).toInt()
            if (seconds > 0) {
                Text(
                    seconds.toString(),
                    Modifier.align(Alignment.Center).background(background).padding(20.dp),
                    color = foreground,
                    fontSize = 64.sp,
                )
            }
        }
    }
}
