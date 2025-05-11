package com.example.geo

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class NtripActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ntrip)

        val etCaster = findViewById<EditText>(R.id.etCaster)
        val etPort = findViewById<EditText>(R.id.etPort)
        val etMountpoint = findViewById<EditText>(R.id.etMountpoint)
        val etUsername = findViewById<EditText>(R.id.etUsername)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnConnect = findViewById<Button>(R.id.btnConnectNtrip)

        btnConnect.setOnClickListener {
            val host = etCaster.text.toString()
            val port = etPort.text.toString().toIntOrNull() ?: 0
            val mount = etMountpoint.text.toString()
            val user = etUsername.text.toString()
            val pass = etPassword.text.toString()

            // Call your NTRIP connection logic here (e.g., open socket and read RTCM data)
            Toast.makeText(this, "Connecting to $host:$port", Toast.LENGTH_SHORT).show()
        }
    }
}