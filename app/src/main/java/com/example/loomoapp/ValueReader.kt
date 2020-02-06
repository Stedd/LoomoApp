package com.example.loomoapp

import android.app.Service
import android.content.Intent
import android.os.*
import android.util.Log
import androidx.lifecycle.ViewModelProvider
import com.example.loomoapp.viewModel.MainActivityViewModel

class ValueReader : Service() {

    private lateinit var viewModel: MainActivityViewModel

    var isBound = false
    var message = "asd"
//    val mThread = thread{}

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
//        intent.putExtra("message", "Service is bound")
//        task = AsyncTask<>
//        Asynch{
//        }
        return myBinder
    }

    override fun onCreate() {
        Log.i("asd", "Service created")
//        viewModel = ViewModelProvider(this)
//            .get(MainActivityViewModel::class.java)
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
