package com.example.loomoapp

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log

class ValueReader : Service() {
    override fun onBind(intent: Intent): IBinder {

        Log.i("asd", "Service bound")

        TODO("Return the communication channel to the service.")
    }

    override fun onCreate() {
        super.onCreate()
        Log.i("asd", "Service created")
    }
}
