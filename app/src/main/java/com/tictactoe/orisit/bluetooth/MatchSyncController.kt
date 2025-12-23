package com.tictactoe.orisit.bluetooth

import com.tictactoe.orisit.model.Cell
import com.tictactoe.orisit.model.GameMode
import com.tictactoe.orisit.model.GameState
import com.tictactoe.orisit.model.MatchConfig
import com.tictactoe.orisit.model.ModPool
import com.tictactoe.orisit.model.Player
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Controller for synchronizing game state between Bluetooth devices.
 * Implements turn-based lockstep synchronization.
 */
class MatchSyncController(
    private val bluetoothManager: BluetoothManager
) {
    private val scope = CoroutineScope(Dispatchers.Main)

    private val _syncState = MutableStateFlow(SyncState.WAITING_FOR_CONNECTION)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val _remoteMove = MutableSharedFlow<Cell>()
    val remoteMove: SharedFlow<Cell> = _remoteMove.asSharedFlow()

    private val _matchConfigReceived = MutableSharedFlow<MatchConfig>()
    val matchConfigReceived: SharedFlow<MatchConfig> = _matchConfigReceived.asSharedFlow()

    private var isHost = false
    private var localPlayer: Player = Player.X
    private var currentMatchConfig: MatchConfig? = null
    private var lastProcessedTurn = -1

    init {
        observeBluetoothMessages()
        observeConnectionState()
    }

    private fun observeBluetoothMessages() {
        scope.launch {
            bluetoothManager.receivedMessages.collect { message ->
                handleReceivedMessage(message)
            }
        }
    }

    private fun observeConnectionState() {
        scope.launch {
            bluetoothManager.connectionState.collect { state ->
                when (state) {
                    ConnectionState.CONNECTED -> {
                        _syncState.value = SyncState.CONNECTED
                        if (isHost) {
                            sendMatchConfig()
                        }
                    }
                    ConnectionState.DISCONNECTED -> {
                        _syncState.value = SyncState.DISCONNECTED
                    }
                    ConnectionState.LISTENING -> {
                        _syncState.value = SyncState.WAITING_FOR_CONNECTION
                    }
                    ConnectionState.CONNECTING -> {
                        _syncState.value = SyncState.CONNECTING
                    }
                }
            }
        }
    }

    private suspend fun handleReceivedMessage(message: BluetoothMessage) {
        when (message) {
            is BluetoothMessage.MatchConfig -> {
                val config = MatchConfig.generateForBluetooth(
                    seed = message.seed,
                    boardSize = message.boardSize,
                    winCondition = message.winCondition,
                    isHost = false
                )
                currentMatchConfig = config
                _matchConfigReceived.emit(config)
                _syncState.value = SyncState.READY
            }
            is BluetoothMessage.Move -> {
                if (message.turnNumber > lastProcessedTurn) {
                    lastProcessedTurn = message.turnNumber
                    val cell = Cell(message.row, message.col)
                    _remoteMove.emit(cell)
                }
            }
            is BluetoothMessage.GameEnd -> {
                _syncState.value = SyncState.GAME_OVER
            }
            is BluetoothMessage.Ready -> {
                if (isHost) {
                    _syncState.value = SyncState.READY
                }
            }
            is BluetoothMessage.Ping -> {
                // Keep-alive, no action needed
            }
        }
    }

    /**
     * Start hosting a Bluetooth game.
     */
    fun startHosting(
        boardSize: Int = 4,
        winCondition: Int = 4,
        modPool: ModPool = ModPool.NORMAL
    ) {
        isHost = true
        localPlayer = Player.X
        
        val seed = System.currentTimeMillis()
        currentMatchConfig = MatchConfig.generateForBluetooth(
            seed = seed,
            boardSize = boardSize,
            winCondition = winCondition,
            modPool = modPool,
            isHost = true
        )
        
        bluetoothManager.startHosting()
    }

    /**
     * Join a Bluetooth game.
     */
    fun joinGame(device: android.bluetooth.BluetoothDevice) {
        isHost = false
        localPlayer = Player.O
        bluetoothManager.connectToDevice(device)
    }

    /**
     * Send match configuration to the connected device.
     */
    private fun sendMatchConfig() {
        val config = currentMatchConfig ?: return
        
        scope.launch {
            val message = BluetoothMessage.MatchConfig(
                seed = config.seed,
                boardSize = config.boardSize,
                winCondition = config.winCondition,
                modActivationTurn = config.modActivationTurn
            )
            bluetoothManager.sendMessage(message)
        }
    }

    /**
     * Send a move to the connected device.
     */
    fun sendMove(cell: Cell, turnNumber: Int) {
        scope.launch {
            val message = BluetoothMessage.Move(
                row = cell.row,
                col = cell.col,
                turnNumber = turnNumber
            )
            bluetoothManager.sendMessage(message)
        }
    }

    /**
     * Send game end notification.
     */
    fun sendGameEnd(winner: Player) {
        scope.launch {
            val winnerStr = when (winner) {
                Player.X -> "X"
                Player.O -> "O"
                Player.NONE -> "DRAW"
            }
            bluetoothManager.sendMessage(BluetoothMessage.GameEnd(winnerStr))
            _syncState.value = SyncState.GAME_OVER
        }
    }

    /**
     * Send ready signal.
     */
    fun sendReady() {
        scope.launch {
            bluetoothManager.sendMessage(BluetoothMessage.Ready)
        }
    }

    /**
     * Check if it's the local player's turn.
     */
    fun isLocalPlayerTurn(state: GameState): Boolean {
        return state.currentPlayer == localPlayer
    }

    /**
     * Get the current match configuration.
     */
    fun getMatchConfig(): MatchConfig? = currentMatchConfig

    /**
     * Get the local player.
     */
    fun getLocalPlayer(): Player = localPlayer

    /**
     * Check if this device is the host.
     */
    fun isHost(): Boolean = isHost

    /**
     * Disconnect and reset state.
     */
    fun disconnect() {
        bluetoothManager.disconnect()
        _syncState.value = SyncState.DISCONNECTED
        currentMatchConfig = null
        lastProcessedTurn = -1
    }

    /**
     * Attempt to reconnect.
     */
    fun attemptReconnect() {
        bluetoothManager.attemptReconnect()
    }
}

enum class SyncState {
    WAITING_FOR_CONNECTION,
    CONNECTING,
    CONNECTED,
    READY,
    IN_GAME,
    DISCONNECTED,
    GAME_OVER
}
