package com.example.geo

import android.Manifest
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    private var btPermission = false
    private var bluetoothAdapter: BluetoothAdapter? = null
    private lateinit var deviceNameText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val bluetoothManager = getSystemService(BluetoothManager::class.java)
        bluetoothAdapter = bluetoothManager?.adapter

        deviceNameText = findViewById(R.id.device_name)
    }

    fun scanBluetooth(view: View) {
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Device doesn't support Bluetooth", Toast.LENGTH_LONG).show()
            return
        }

        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.BLUETOOTH
            )
        }

        bluetoothMultiplePermissionsLauncher.launch(permissions)
    }


    private val bluetoothPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            btPermission = true
            if (bluetoothAdapter?.isEnabled == false) {
                val enableBTIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                bluetoothActivityResultLauncher.launch(enableBTIntent)
            } else {
                btScan()
            }
        } else {
            btPermission = false
            Toast.makeText(this, "Bluetooth permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    private val bluetoothMultiplePermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            btPermission = true
            if (bluetoothAdapter?.isEnabled == false) {
                val enableBTIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                bluetoothActivityResultLauncher.launch(enableBTIntent)
            } else {
                btScan()
            }
        } else {
            btPermission = false
            Toast.makeText(this, "Bluetooth permissions denied", Toast.LENGTH_SHORT).show()
        }
    }


    private val bluetoothActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == RESULT_OK) {
            btScan()
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN])
    private fun btScan() {
        val builder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        val dialogView: View = inflater.inflate(R.layout.scan_bt, null)
        builder.setCancelable(true)
        builder.setView(dialogView)

        val dialog = builder.create() // ✅ Create the dialog before using it

        val btList = dialogView.findViewById<ListView>(R.id.bt_list)
        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices

        val data: MutableList<Map<String, String>> = ArrayList()
        if (!pairedDevices.isNullOrEmpty()) {
            for (device in pairedDevices) {
                val deviceMap = HashMap<String, String>()
                deviceMap["A"] = device.name ?: "Unknown Device"
                deviceMap["B"] = device.address
                data.add(deviceMap)
            }

            val from = arrayOf("A")
            val to = intArrayOf(R.id.item_name)
            val adapter = SimpleAdapter(this, data, R.layout.item_list, from, to)
            btList.adapter = adapter

            btList.setOnItemClickListener { _, _, position, _ ->
                val selectedDevice = data[position]
                val name = selectedDevice["A"]
                val address = selectedDevice["B"]

                deviceNameText.text = "$name\n$address"
                dialog.dismiss()

                val device = bluetoothAdapter?.getRemoteDevice(address)
                device?.let {
                    connectToDevice(it)
                }
            }
        } else {
            Toast.makeText(this, "No Devices Found", Toast.LENGTH_LONG).show()
            return
        }

        dialog.show()
    }

    private val readBuffer = StringBuilder() // Shared buffer

    @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN])
    private fun connectToDevice(device: BluetoothDevice) {
        Thread {
            try {
                // UUID for SPP (Serial Port Profile)
                val uuid = device.uuids?.firstOrNull()?.uuid
                    ?: java.util.UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

                // Create the RFCOMM socket
                val socket = device.createRfcommSocketToServiceRecord(uuid)

                // Cancel Bluetooth discovery to improve connection performance
                bluetoothAdapter?.cancelDiscovery()

                // Connect to the Bluetooth device
                socket.connect()

                runOnUiThread {
                    // Notify the user that the connection was successful
                    Toast.makeText(this, "Connected to ${device.name}", Toast.LENGTH_SHORT).show()
                }

                // Set up input stream for receiving data
                val inputStream = socket.inputStream
                val buffer = ByteArray(1024)

                while (true) {
                    val bytesRead = inputStream.read(buffer)
                    if (bytesRead > 0) {
                        val data = buffer.copyOfRange(0, bytesRead)
                        onBluetoothDataReceived(data)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    // You can show a Toast or log the error message here if needed
                    println("Connection failed: ${e.message}")
                }
            }
        }.start()

        // If permissions are granted for Android 12 and above, cancel discovery
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
            bluetoothAdapter?.cancelDiscovery()
        }
    }

    fun onBluetoothDataReceived(bytes: ByteArray) {
        val data = String(bytes, Charsets.UTF_8)
        readBuffer.append(data)

        var endIdx: Int
        while (true) {
            endIdx = readBuffer.indexOf("\r\n")
            if (endIdx == -1) break

            val line = readBuffer.substring(0, endIdx).trim()
            readBuffer.delete(0, endIdx + 2)

            println("Received NMEA: $line")
        }
    }
}
