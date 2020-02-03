package com.example.loomoapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Log.i("asd", "Activity created")

        btnStartService.setOnClickListener {
            startService()
        }
        btnStopService.setOnClickListener {
            stopService()
        }

    }

    private fun startService(){
        Log.i("asd", "Service start command")
        val intent = Intent(this, ValueReader::class.java)
        startService(intent)
        //bindService(intent, )
    }

    private fun stopService (){
        Log.i("asd", "Service stop command")
        val intent = Intent(this, ValueReader::class.java)
        stopService(intent)
    }
}




