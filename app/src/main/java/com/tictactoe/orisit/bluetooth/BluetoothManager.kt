package com.tictactoe.orisit.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

/**
 * Manages Bluetooth connections for local multiplayer.
 * Handles device discovery, pairing, connection, and data transfer.
 */
class BluetoothManager(private val context: Context) {

    companion object {
        private const val APP_NAME = "TicTacToeOrIsIt"
        private val APP_UUID: UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66")
        private const val BUFFER_SIZE = 1024
    }

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var serverSocket: BluetoothServerSocket? = null
    private var clientSocket: BluetoothSocket? = null
    private var connectedSocket: BluetoothSocket? = null
    
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null
    
    private var acceptJob: Job? = null
    private var connectJob: Job? = null
    private var readJob: Job? = null
    
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<BluetoothDevice>> = _discoveredDevices.asStateFlow()

    private val _receivedMessages = MutableSharedFlow<BluetoothMessage>()
    val receivedMessages: SharedFlow<BluetoothMessage> = _receivedMessages.asSharedFlow()

    private val _errors = MutableSharedFlow<BluetoothError>()
    val errors: SharedFlow<BluetoothError> = _errors.asSharedFlow()

    /**
     * Check if Bluetooth is available and enabled.
     */
    fun isBluetoothAvailable(): Boolean = bluetoothAdapter != null

    fun isBluetoothEnabled(): Boolean = bluetoothAdapter?.isEnabled == true

    /**
     * Check if required permissions are granted.
     */
    fun hasRequiredPermissions(): Boolean {
        val permissions = listOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        return permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Get paired devices.
     */
    @SuppressLint("MissingPermission")
    fun getPairedDevices(): List<BluetoothDevice> {
        if (!hasRequiredPermissions()) return emptyList()
        return bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()
    }

    /**
     * Start accepting connections as host.
     */
    @SuppressLint("MissingPermission")
    fun startHosting() {
        if (!hasRequiredPermissions()) {
            scope.launch { _errors.emit(BluetoothError.PERMISSION_DENIED) }
            return
        }

        _connectionState.value = ConnectionState.LISTENING

        acceptJob = scope.launch {
            try {
                serverSocket = bluetoothAdapter?.listenUsingRfcommWithServiceRecord(APP_NAME, APP_UUID)
                
                // Block until a connection is accepted
                val socket = serverSocket?.accept()
                
                socket?.let {
                    connectedSocket = it
                    setupStreams(it)
                    _connectionState.value = ConnectionState.CONNECTED
                    startReading()
                }
            } catch (e: IOException) {
                _connectionState.value = ConnectionState.DISCONNECTED
                _errors.emit(BluetoothError.CONNECTION_FAILED)
            }
        }
    }

    /**
     * Connect to a host device.
     */
    @SuppressLint("MissingPermission")
    fun connectToDevice(device: BluetoothDevice) {
        if (!hasRequiredPermissions()) {
            scope.launch { _errors.emit(BluetoothError.PERMISSION_DENIED) }
            return
        }

        _connectionState.value = ConnectionState.CONNECTING

        connectJob = scope.launch {
            try {
                clientSocket = device.createRfcommSocketToServiceRecord(APP_UUID)
                
                // Cancel discovery to speed up connection
                bluetoothAdapter?.cancelDiscovery()
                
                clientSocket?.connect()
                
                clientSocket?.let {
                    connectedSocket = it
                    setupStreams(it)
                    _connectionState.value = ConnectionState.CONNECTED
                    startReading()
                }
            } catch (e: IOException) {
                _connectionState.value = ConnectionState.DISCONNECTED
                _errors.emit(BluetoothError.CONNECTION_FAILED)
                clientSocket?.close()
                clientSocket = null
            }
        }
    }

    /**
     * Set up input/output streams.
     */
    private fun setupStreams(socket: BluetoothSocket) {
        try {
            inputStream = socket.inputStream
            outputStream = socket.outputStream
        } catch (e: IOException) {
            scope.launch { _errors.emit(BluetoothError.STREAM_ERROR) }
        }
    }

    /**
     * Start reading messages from the connected device.
     */
    private fun startReading() {
        readJob = scope.launch {
            val buffer = ByteArray(BUFFER_SIZE)
            
            while (_connectionState.value == ConnectionState.CONNECTED) {
                try {
                    val bytesRead = inputStream?.read(buffer) ?: -1
                    
                    if (bytesRead > 0) {
                        val message = String(buffer, 0, bytesRead)
                        val parsed = BluetoothMessage.parse(message)
                        parsed?.let { _receivedMessages.emit(it) }
                    }
                } catch (e: IOException) {
                    if (_connectionState.value == ConnectionState.CONNECTED) {
                        _connectionState.value = ConnectionState.DISCONNECTED
                        _errors.emit(BluetoothError.DISCONNECTED)
                    }
                    break
                }
            }
        }
    }

    /**
     * Send a message to the connected device.
     */
    suspend fun sendMessage(message: BluetoothMessage): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val data = message.serialize().toByteArray()
                outputStream?.write(data)
                outputStream?.flush()
                true
            } catch (e: IOException) {
                _errors.emit(BluetoothError.SEND_FAILED)
                false
            }
        }
    }

    /**
     * Disconnect from the current connection.
     */
    fun disconnect() {
        acceptJob?.cancel()
        connectJob?.cancel()
        readJob?.cancel()
        
        try {
            inputStream?.close()
            outputStream?.close()
            connectedSocket?.close()
            serverSocket?.close()
            clientSocket?.close()
        } catch (e: IOException) {
            // Ignore close errors
        }
        
        inputStream = null
        outputStream = null
        connectedSocket = null
        serverSocket = null
        clientSocket = null
        
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    /**
     * Attempt to reconnect to the last connected device.
     */
    @SuppressLint("MissingPermission")
    fun attemptReconnect() {
        connectedSocket?.remoteDevice?.let { device ->
            disconnect()
            connectToDevice(device)
        }
    }

    /**
     * Check if currently connected.
     */
    fun isConnected(): Boolean = _connectionState.value == ConnectionState.CONNECTED
}

enum class ConnectionState {
    DISCONNECTED,
    LISTENING,
    CONNECTING,
    CONNECTED
}

enum class BluetoothError {
    PERMISSION_DENIED,
    BLUETOOTH_DISABLED,
    CONNECTION_FAILED,
    DISCONNECTED,
    SEND_FAILED,
    STREAM_ERROR
}

/**
 * Messages exchanged between Bluetooth devices.
 */
sealed class BluetoothMessage {
    data class MatchConfig(
        val seed: Long,
        val boardSize: Int,
        val winCondition: Int,
        val modActivationTurn: Int
    ) : BluetoothMessage()

    data class Move(
        val row: Int,
        val col: Int,
        val turnNumber: Int
    ) : BluetoothMessage()

    data class GameEnd(
        val winner: String // "X", "O", or "DRAW"
    ) : BluetoothMessage()

    data object Ready : BluetoothMessage()
    
    data object Ping : BluetoothMessage()

    fun serialize(): String {
        return when (this) {
            is MatchConfig -> "CONFIG:$seed:$boardSize:$winCondition:$modActivationTurn"
            is Move -> "MOVE:$row:$col:$turnNumber"
            is GameEnd -> "END:$winner"
            is Ready -> "READY"
            is Ping -> "PING"
        }
    }

    companion object {
        fun parse(data: String): BluetoothMessage? {
            val parts = data.trim().split(":")
            return when (parts.getOrNull(0)) {
                "CONFIG" -> {
                    if (parts.size >= 5) {
                        MatchConfig(
                            seed = parts[1].toLongOrNull() ?: return null,
                            boardSize = parts[2].toIntOrNull() ?: return null,
                            winCondition = parts[3].toIntOrNull() ?: return null,
                            modActivationTurn = parts[4].toIntOrNull() ?: return null
                        )
                    } else null
                }
                "MOVE" -> {
                    if (parts.size >= 4) {
                        Move(
                            row = parts[1].toIntOrNull() ?: return null,
                            col = parts[2].toIntOrNull() ?: return null,
                            turnNumber = parts[3].toIntOrNull() ?: return null
                        )
                    } else null
                }
                "END" -> {
                    if (parts.size >= 2) {
                        GameEnd(winner = parts[1])
                    } else null
                }
                "READY" -> Ready
                "PING" -> Ping
                else -> null
            }
        }
    }
}
