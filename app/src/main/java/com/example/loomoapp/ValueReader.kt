package com.example.loomoapp

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Binder
import android.os.IBinder
import android.util.Log
import kotlin.concurrent.thread


class ValueReader : Service() {

    private val myBinder = LocalBinder()

    inner class LocalBinder:Binder(){
        fun getService():ValueReader{
            return this@ValueReader
        }
    }

    override fun onBind(intent: Intent): IBinder {
        Log.i("asd", "Service bound")
        return myBinder
        TODO("Return the communication channel to the service.")
    }

    override fun onCreate() {
        var index = 0
        super.onCreate()
        Log.i("asd", "Service created")
//        viewModel.text.value = "moo"

//        val intent = Intent (this, MainActivity::class.java)
//        intent.putExtra("serviceText", "moo")
//        bindService(intent,conn, Context.BIND_IMPORTANT)

    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i("asd", "Service destroyed")
    }

    override fun onUnbind(intent: Intent?): Boolean {
        return super.onUnbind(intent)
        Log.i("asd", "Service Unbound")
    }
}
