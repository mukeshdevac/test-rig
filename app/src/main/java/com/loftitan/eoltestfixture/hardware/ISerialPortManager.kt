package com.loftitan.eoltestfixture.hardware

import com.loftitan.eoltestfixture.domain.model.ConnectionState
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Common interface for USB OTG Serial hardware drivers and software simulators.
 */
interface ISerialPortManager {
    val connectionState: StateFlow<ConnectionState>
    val incomingFrames: SharedFlow<String>

    suspend fun connect(): Boolean
    suspend fun disconnect()
    suspend fun sendCommand(jsonCommand: String): Boolean
    fun isConnected(): Boolean
}
