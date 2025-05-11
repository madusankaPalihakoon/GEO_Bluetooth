package com.example.geo

import android.Manifest
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Intent
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
    private lateinit var deviceNameText: TextView  // UI element reference

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

        deviceNameText = findViewById(R.id.device_name) // Match with your layout TextView ID
    }

    fun scanBluetooth(view: View) {
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Device doesn't support Bluetooth", Toast.LENGTH_LONG).show()
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                bluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
            } else {
                bluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_ADMIN)
            }
        }
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

    private val bluetoothActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == RESULT_OK) {
            btScan()
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
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
                deviceNameText.text = name
                dialog.dismiss()
            }
        } else {
            Toast.makeText(this, "No Devices Found", Toast.LENGTH_LONG).show()
            return
        }

        dialog.show()
    }

}
