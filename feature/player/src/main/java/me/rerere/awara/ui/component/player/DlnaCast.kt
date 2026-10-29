package me.rerere.awara.ui.component.player

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import net.mm2d.upnp.ControlPoint
import net.mm2d.upnp.ControlPointFactory
import net.mm2d.upnp.Device
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.*

private const val TAG = "DlnaCast"

private val disposeScope = Executors.newSingleThreadExecutor()

class DlnaCastState {
    lateinit var controlPoint: ControlPoint
    var initialized = mutableStateOf<Boolean>(false)

    val deviceList = mutableStateListOf<Device>()

    fun release() {
        if (this::controlPoint.isInitialized) {
            controlPoint.stop()
            controlPoint.terminate()
        }
    }

    fun search() {
        if (initialized.value) controlPoint.search()
    }

    suspend fun playUrl(device: Device, url: String) {
        requireNotNull(device.findAction("SetAVTransportURI")) { "Device cannot receive media" }.invokeAsync(
            mapOf(
                "InstanceID" to "0",
                "CurrentURI" to url,
                "CurrentURIMetaData" to ""
            ),
            true
        )
        requireNotNull(device.findAction("Play")) { "Device cannot play media" }.invokeAsync(
            mapOf(
                "InstanceID" to "0",
                "Speed" to "1"
            ),
            true
        )
        Log.d(TAG, "Cast request completed")
    }
}

@Composable
fun rememberDlnaCastState(): DlnaCastState {
    val state = remember {
        DlnaCastState()
    }
    DisposableEffect(state) {
        val active = AtomicBoolean(true)
        val callbacks = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        disposeScope.execute {
            kotlin.runCatching {
                state.controlPoint = ControlPointFactory.create()
                state.controlPoint.addDiscoveryListener(object : ControlPoint.DiscoveryListener {
                    override fun onDiscover(device: Device) {
                        callbacks.launch { if (active.get() && device !in state.deviceList) state.deviceList.add(device) }
                    }

                    override fun onLost(device: Device) {
                        callbacks.launch { if (active.get()) state.deviceList.remove(device) }
                    }
                })
                state.controlPoint.initialize()
                state.controlPoint.start()
                state.controlPoint.search()
                callbacks.launch { if (active.get()) state.initialized.value = true }
                Log.i(TAG, "rememberDlnaCastState: initialized")
            }.onFailure {
                it.printStackTrace()
            }
        }
        onDispose {
            active.set(false)
            callbacks.cancel()
            state.deviceList.clear()
            state.initialized.value = false
            disposeScope.execute {
                kotlin.runCatching {
                    state.release()
                    run {
                        Log.i(TAG, "rememberDlnaCastState: disposed")
                    }
                }.onFailure {
                    it.printStackTrace()
                }
            }
        }
    }

    return state
}
