package com.loftitan.eoltestfixture.hardware

import android.util.Log
import com.loftitan.eoltestfixture.domain.model.ConnectionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicInteger

private const val TAG = "HeartbeatStateMachine"
private const val PING_INTERVAL_MS = 1000L
private const val RECONNECT_POLL_INTERVAL_MS = 500L
private const val MAX_MISSED_HEARTBEATS = 3

/**
 * Background Heartbeat & Self-Healing Reconnection Manager.
 * Guarantees zero UI blocking, automatic detection of dropped frames, and seamless recovery.
 */
class HeartbeatStateMachine(
    private val scope: CoroutineScope,
    private val getActiveSerialManager: () -> ISerialPortManager
) {

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val missedHeartbeats = AtomicInteger(0)
    private var heartbeatJob: Job? = null
    private var reconnectPollerJob: Job? = null

    fun start() {
        startHeartbeatLoop()
    }

    fun stop() {
        heartbeatJob?.cancel()
        reconnectPollerJob?.cancel()
    }

    fun onPongReceived() {
        missedHeartbeats.set(0)
        if (_connectionState.value != ConnectionState.CONNECTED) {
            _connectionState.value = ConnectionState.CONNECTED
            Log.i(TAG, "Heartbeat verified: Connection state restored to CONNECTED.")
        }
    }

    private fun startHeartbeatLoop() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                val serialManager = getActiveSerialManager()

                if (serialManager.isConnected()) {
                    val pingPayload = JSONObject().apply {
                        put("cmd", "PING")
                        put("ts", System.currentTimeMillis())
                    }.toString()

                    val sent = serialManager.sendCommand(pingPayload)
                    if (sent) {
                        val missed = missedHeartbeats.incrementAndGet()
                        if (missed >= MAX_MISSED_HEARTBEATS) {
                            Log.w(TAG, "Missed $missed consecutive heartbeats! Triggering RECONNECTING.")
                            handleConnectionFailure(serialManager)
                        }
                    } else {
                        Log.w(TAG, "Failed to send heartbeat packet.")
                        handleConnectionFailure(serialManager)
                    }
                } else {
                    if (_connectionState.value != ConnectionState.RECONNECTING &&
                        _connectionState.value != ConnectionState.DISCONNECTED) {
                        handleConnectionFailure(serialManager)
                    }
                }

                delay(PING_INTERVAL_MS)
            }
        }
    }

    private fun handleConnectionFailure(serialManager: ISerialPortManager) {
        _connectionState.value = ConnectionState.RECONNECTING
        missedHeartbeats.set(0)
        startReconnectPoller(serialManager)
    }

    private fun startReconnectPoller(serialManager: ISerialPortManager) {
        if (reconnectPollerJob?.isActive == true) return

        reconnectPollerJob = scope.launch(Dispatchers.IO) {
            Log.i(TAG, "Starting active USB reconnect poller (every ${RECONNECT_POLL_INTERVAL_MS}ms)...")
            while (isActive && _connectionState.value == ConnectionState.RECONNECTING) {
                try {
                    val reconnected = serialManager.connect()
                    if (reconnected) {
                        Log.i(TAG, "USB Hardware reconnected! Sending initial verification ping...")
                        val probePayload = JSONObject().apply {
                            put("cmd", "PING")
                            put("ts", System.currentTimeMillis())
                        }.toString()
                        serialManager.sendCommand(probePayload)
                        _connectionState.value = ConnectionState.CONNECTED
                        missedHeartbeats.set(0)
                        break
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "Reconnect attempt failed: ${e.message}")
                }
                delay(RECONNECT_POLL_INTERVAL_MS)
            }
        }
    }

    fun forceState(state: ConnectionState) {
        _connectionState.value = state
        if (state == ConnectionState.CONNECTED) {
            missedHeartbeats.set(0)
            reconnectPollerJob?.cancel()
        }
    }
}
