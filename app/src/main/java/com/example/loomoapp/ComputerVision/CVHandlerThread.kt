package com.example.loomoapp.ComputerVision

import android.annotation.SuppressLint
import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import android.util.Log


class CVHandlerThread(name: String, priority: Int) :
    HandlerThread(name, priority) {
    companion object {
        private const val TAG = "CVHandlerThread"

        // These can be used if you want the thread to handle messages
        /*const val TASK1 = 1
        const val TASK2 = 2
        const val TASK3 = 3
        const val TASK4 = 4*/
    }

    lateinit var handler: Handler//? = null
//        private set

    override fun onLooperPrepared() {
        handler = @SuppressLint("HandlerLeak") object : Handler() {}
    }

//    override fun onLooperPrepared() {
//        handler = @SuppressLint("HandlerLeak")
//        object : Handler() {
//            override fun handleMessage(msg: Message) {
//                when (msg.what) {
//                    TASK1 -> {
//                        Log.d(TAG, "TASK1, arg1: " + msg.arg1 + ", obj: " + msg.obj
//                        )
//                    }
//                    else -> Log.d(TAG, "Non-recognized task: ${msg.what}")
//                }
//            }
//        }
//    }

}