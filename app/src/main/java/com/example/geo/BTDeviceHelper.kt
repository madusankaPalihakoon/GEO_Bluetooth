package com.example.geo

import android.Manifest
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.ListView
import android.widget.SimpleAdapter
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import java.io.IOException

class BTDeviceHelper(private val context: Context, private val bluetoothAdapter: BluetoothAdapter?) {
    var btPermission = false
    private var deviceNameText: TextView? = null
    private val readBuffer = StringBuilder()

    fun setDeviceNameText(deviceNameText: TextView) {
        this.deviceNameText = deviceNameText
    }

    fun scanBluetooth(bluetoothMultiplePermissionsLauncher: ActivityResultContracts.RequestMultiplePermissions) {
        if (bluetoothAdapter == null) {
            Toast.makeText(context, "Device doesn't support Bluetooth", Toast.LENGTH_LONG).show()
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

    private val bluetoothActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == RESULT_OK) {
            btScan()
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN])
    private fun btScan() {
        val builder = AlertDialog.Builder(context)
        val inflater = LayoutInflater.from(context)
        val dialogView: View = inflater.inflate(R.layout.scan_bt, null)
        builder.setCancelable(true)
        builder.setView(dialogView)

        val dialog = builder.create()

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
            val adapter = SimpleAdapter(context, data, R.layout.item_list, from, to)
            btList.adapter = adapter

            btList.setOnItemClickListener { _, _, position, _ ->
                val selectedDevice = data[position]
                val name = selectedDevice["A"]
                val address = selectedDevice["B"]

                deviceNameText?.text = "$name\n$address"
                dialog.dismiss()

                val device = bluetoothAdapter?.getRemoteDevice(address)
                device?.let {
                    connectToDevice(it)
                }
            }
        } else {
            Toast.makeText(context, "No Devices Found", Toast.LENGTH_LONG).show()
            return
        }

        dialog.show()
    }

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

                (context as? MainActivity)?.runOnUiThread {
                    // Notify the user that the connection was successful
                    Toast.makeText(context, "Connected to ${device.name}", Toast.LENGTH_SHORT).show()
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
                (context as? MainActivity)?.runOnUiThread {
                    // You can show a Toast or log the error message here if needed
                    println("Connection failed: ${e.message}")
                }
            }
        }.start()

        // If permissions are granted for Android 12 and above, cancel discovery
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            (context.checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED)) {
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
