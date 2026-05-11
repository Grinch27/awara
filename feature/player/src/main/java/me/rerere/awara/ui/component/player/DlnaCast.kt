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

private const val TAG = "DlnaCast"

private val disposeScope = Executors.newSingleThreadExecutor()

class DlnaCastState {
    lateinit var controlPoint: ControlPoint
    var initialized = mutableStateOf<Boolean>(false)

    val deviceList = mutableStateListOf<Device>()

    fun search() {
        controlPoint.search()
    }

    suspend fun playUrl(device: Device, url: String) {
        device.findAction("SetAVTransportURI")?.invokeAsync(
            mapOf(
                "InstanceID" to "0",
                "CurrentURI" to url,
                "CurrentURIMetaData" to ""
            ),
            true
        )
        device.findAction("Play")?.invokeAsync(
            mapOf(
                "InstanceID" to "0",
                "Speed" to "1"
            ),
            true
        )
        Log.d(TAG, "play: $url on ${device.friendlyName}")
    }
}

@Composable
fun rememberDlnaCastState(): DlnaCastState {
    val state = remember {
        DlnaCastState()
    }
    DisposableEffect(Unit) {
        disposeScope.execute {
            kotlin.runCatching {
                state.controlPoint = ControlPointFactory.create()
                state.controlPoint.addDiscoveryListener(object : ControlPoint.DiscoveryListener {
                    override fun onDiscover(device: Device) {
                        state.deviceList.add(device)
                    }

                    override fun onLost(device: Device) {
                        state.deviceList.remove(device)
                    }
                })
                state.controlPoint.initialize()
                state.controlPoint.start()
                state.controlPoint.search()
                state.initialized.value = true
                Log.i(TAG, "rememberDlnaCastState: initialized")
            }.onFailure {
                it.printStackTrace()
            }
        }
        onDispose {
            disposeScope.execute {
                kotlin.runCatching {
                    if (state.initialized.value) {
                        state.controlPoint.stop()
                        state.controlPoint.terminate()
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