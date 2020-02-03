package com.example.loomoapp

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.example.loomoapp.viewModel.ViewModel


class ValueReader : Service() {

    private lateinit var viewModel: ViewModel

    override fun onBind(intent: Intent): IBinder {

        Log.i("asd", "Service bound")

        TODO("Return the communication channel to the service.")
    }

    override fun onCreate() {
        super.onCreate()
        Log.i("asd", "Service created")

//        viewModel.text.value = "moo"

        val intent = Intent (this, MainActivity::class.java)
        intent.putExtra("serviceText", "moo")
    }
}
