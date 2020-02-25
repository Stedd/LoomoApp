package com.example.loomoapp

import android.annotation.SuppressLint
import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import android.util.Log


class LoomoHandlerThread (name: String, priority: Int) :
    HandlerThread(name, priority) {
    companion object {
        private const val TAG = "CVHandlerThread"
    }

    lateinit var handler: Handler

    override fun onLooperPrepared() {

    }
}