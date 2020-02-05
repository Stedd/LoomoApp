package com.example.loomoapp

import android.app.Service
import android.content.Intent
import android.os.AsyncTask
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.example.loomoapp.viewModel.ASDFViewModel
import kotlin.concurrent.thread

class ValueReader : Service() {

    private lateinit var viewModel: ASDFViewModel

    var isBound = false
    var message = "asd"



    private val myBinder = LocalBinder()

    inner class LocalBinder:Binder(){
        fun getService():ValueReader{
            return this@ValueReader
        }
    }

    override fun onBind(intent: Intent): IBinder {
        Log.i("asd", "Service bound")
        isBound = true
//        message = "Service is bound"
        intent.putExtra("message", "Service is bound")
        task = AsyncTask<>
        Asynch{

        }

        return myBinder

        TODO("Return the communication channel to the service.")
    }

    override fun onCreate() {
        Log.i("asd", "Service created")
        super.onCreate()
    }

    override fun onDestroy() {
        Log.i("asd", "Service destroyed")
        super.onDestroy()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.i("asd", "Service Unbound")
        isBound = false
        viewModel.text.value="asdf"
//        intent?.putExtra("message", "Service is unbound")
        return super.onUnbind(intent)
    }

}
