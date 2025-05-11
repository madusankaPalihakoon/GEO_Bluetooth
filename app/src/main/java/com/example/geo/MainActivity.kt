package com.example.geo

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ListView
import android.widget.SimpleAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    private var btPermission = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    fun scanBluetooth(view: View) {
        val bluetoothManager = getSystemService(BluetoothManager::class.java)
        val bluetoothAdapter = bluetoothManager?.adapter

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Device doesn't support Bluetooth", Toast.LENGTH_LONG).show()
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                BluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
            } else {
                BluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_ADMIN)
            }
        }
    }

    private val BluetoothPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        val bluetoothManager = getSystemService(BluetoothManager::class.java)
        val bluetoothAdapter = bluetoothManager?.adapter

        if (isGranted) {
            btPermission = true
            if (bluetoothAdapter?.isEnabled == false) {
                val enableBTIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                BluetoothActivityResultLauncher.launch(enableBTIntent)
            } else {
                btScan()
            }
        } else {
            btPermission = false
        }
    }

    private val BluetoothActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == RESULT_OK) {
            btScan()
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun btScan() {
        //Toast.makeText(this, "Bluetooth Connected Successfully", Toast.LENGTH_LONG).show()
        val inflater = layoutInflater
        val dialogView:View = inflater.inflate(R.layout.scan_bt,null)
        builder.setCancelable(false)
        builder.seView(dialogView)
        val btlist = dialogView.findViewById<ListView>(R.id.bt_list)
        val dialog = builder.create()
        val paireDevices:Set<BluetoothDevice> = bluetoothAdapter?.bondedDevices as Set<BluetoothDevice>
        val ADAhere:SimpleAdapter
        var data:MutableList<Map<String?,Any?>?>? = null
        data = ArrayList()
        if(paireDevices.isNotEmpty()){
            val datanum1: MutableMap<String?,Any?> = HashMap()
            datanum1["A"] = ""
            datanum1["B"] = ""
            data.add(datanum1)
            for (device in paireDevices){
                val datanum:MutableMap<String?,Any?> = HashMap()
                datanum["A"] = device.name
                datanum["B"] = device.address
                data.add(datanum)
            }
            val fromWare = arrayOf("A")
            val viewswhere  = intArrayOf(R.id.item_name)
            ADAhere = SimpleAdapter(this@MainActivity,data,R.layout.item_list,fromWare,viewswhere)
            btlst.adapter = ADAhere
            ADAhere.notifyDataSetChanged()
            btlst.onItemClickListner = AdapterView.OnItemClickListener{adapterView,view,position,l ->
             val string = ADAhere.getItem(position) as HashMap<String,String>
                val deviceName = string["A"]
                binding.deviceName.text = deviceName
                dialog.dismiss()
            }
        }else{
            val value = "No Devices found"
            Toast.makeText(this,value,Toast.LENGTH_LONG).show()
            return
        }
        dialog.show()
    }
}