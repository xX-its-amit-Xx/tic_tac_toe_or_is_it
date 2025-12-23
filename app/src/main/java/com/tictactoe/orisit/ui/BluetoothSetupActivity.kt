package com.tictactoe.orisit.ui

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.tictactoe.orisit.R
import com.tictactoe.orisit.bluetooth.BluetoothManager
import com.tictactoe.orisit.bluetooth.ConnectionState
import com.tictactoe.orisit.bluetooth.MatchSyncController
import com.tictactoe.orisit.databinding.ActivityBluetoothSetupBinding
import com.tictactoe.orisit.model.ModPool
import kotlinx.coroutines.launch

/**
 * Activity for setting up Bluetooth multiplayer connections.
 */
class BluetoothSetupActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBluetoothSetupBinding
    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var syncController: MatchSyncController
    private lateinit var deviceAdapter: DeviceAdapter

    private var boardSize = 4
    private var modPool = ModPool.NORMAL

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        if (allGranted) {
            updateUI()
        } else {
            Toast.makeText(this, "Bluetooth permissions required", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBluetoothSetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        boardSize = intent.getIntExtra(MainMenuActivity.EXTRA_BOARD_SIZE, 4)
        modPool = ModPool.valueOf(intent.getStringExtra(MainMenuActivity.EXTRA_MOD_POOL) ?: "NORMAL")

        bluetoothManager = BluetoothManager(this)
        syncController = MatchSyncController(bluetoothManager)

        setupUI()
        requestPermissions()
        observeState()
    }

    private fun setupUI() {
        deviceAdapter = DeviceAdapter { device ->
            joinGame(device)
        }
        binding.deviceList.layoutManager = LinearLayoutManager(this)
        binding.deviceList.adapter = deviceAdapter

        binding.hostButton.setOnClickListener {
            startHosting()
        }

        binding.refreshButton.setOnClickListener {
            refreshDevices()
        }

        binding.backButton.setOnClickListener {
            finish()
        }
    }

    private fun requestPermissions() {
        val permissions = mutableListOf<String>()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
        } else {
            permissions.add(Manifest.permission.BLUETOOTH)
            permissions.add(Manifest.permission.BLUETOOTH_ADMIN)
        }
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)

        val notGranted = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (notGranted.isNotEmpty()) {
            permissionLauncher.launch(notGranted.toTypedArray())
        } else {
            updateUI()
        }
    }

    private fun observeState() {
        lifecycleScope.launch {
            bluetoothManager.connectionState.collect { state ->
                updateConnectionUI(state)
            }
        }

        lifecycleScope.launch {
            syncController.matchConfigReceived.collect { config ->
                startGame()
            }
        }

        lifecycleScope.launch {
            bluetoothManager.errors.collect { error ->
                Toast.makeText(this@BluetoothSetupActivity, "Error: ${error.name}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateUI() {
        if (!bluetoothManager.isBluetoothAvailable()) {
            binding.statusText.text = "Bluetooth not available"
            binding.hostButton.isEnabled = false
            binding.refreshButton.isEnabled = false
            return
        }

        if (!bluetoothManager.isBluetoothEnabled()) {
            binding.statusText.text = "Please enable Bluetooth"
            binding.hostButton.isEnabled = false
            binding.refreshButton.isEnabled = false
            return
        }

        binding.hostButton.isEnabled = true
        binding.refreshButton.isEnabled = true
        refreshDevices()
    }

    private fun updateConnectionUI(state: ConnectionState) {
        when (state) {
            ConnectionState.DISCONNECTED -> {
                binding.statusText.text = "Select a device or host a game"
                binding.progressBar.isVisible = false
                binding.hostButton.isEnabled = true
                binding.deviceList.isVisible = true
            }
            ConnectionState.LISTENING -> {
                binding.statusText.text = "Waiting for opponent..."
                binding.progressBar.isVisible = true
                binding.hostButton.isEnabled = false
                binding.deviceList.isVisible = false
            }
            ConnectionState.CONNECTING -> {
                binding.statusText.text = "Connecting..."
                binding.progressBar.isVisible = true
                binding.hostButton.isEnabled = false
            }
            ConnectionState.CONNECTED -> {
                binding.statusText.text = "Connected! Starting game..."
                binding.progressBar.isVisible = true
                
                if (syncController.isHost()) {
                    startGame()
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun refreshDevices() {
        val devices = bluetoothManager.getPairedDevices()
        deviceAdapter.submitList(devices)
        
        if (devices.isEmpty()) {
            binding.statusText.text = "No paired devices. Pair a device in Settings."
        } else {
            binding.statusText.text = "Select a device to join or host a game"
        }
    }

    private fun startHosting() {
        syncController.startHosting(boardSize, boardSize, modPool)
    }

    private fun joinGame(device: BluetoothDevice) {
        syncController.joinGame(device)
    }

    private fun startGame() {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra(MainMenuActivity.EXTRA_GAME_MODE, "BLUETOOTH_MULTIPLAYER")
            putExtra(MainMenuActivity.EXTRA_BOARD_SIZE, boardSize)
            putExtra(MainMenuActivity.EXTRA_MOD_POOL, modPool.name)
            putExtra(EXTRA_IS_HOST, syncController.isHost())
            putExtra(EXTRA_SEED, syncController.getMatchConfig()?.seed ?: System.currentTimeMillis())
        }
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!isFinishing) {
            syncController.disconnect()
        }
    }

    companion object {
        const val EXTRA_IS_HOST = "is_host"
        const val EXTRA_SEED = "seed"
    }
}

/**
 * Adapter for displaying paired Bluetooth devices.
 */
class DeviceAdapter(
    private val onDeviceClick: (BluetoothDevice) -> Unit
) : RecyclerView.Adapter<DeviceAdapter.ViewHolder>() {

    private var devices = listOf<BluetoothDevice>()

    @SuppressLint("NotifyDataSetChanged")
    fun submitList(newDevices: List<BluetoothDevice>) {
        devices = newDevices
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_2, parent, false)
        return ViewHolder(view)
    }

    @SuppressLint("MissingPermission")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val device = devices[position]
        holder.nameText.text = device.name ?: "Unknown Device"
        holder.addressText.text = device.address
        holder.itemView.setOnClickListener { onDeviceClick(device) }
    }

    override fun getItemCount() = devices.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameText: TextView = view.findViewById(android.R.id.text1)
        val addressText: TextView = view.findViewById(android.R.id.text2)
    }
}
