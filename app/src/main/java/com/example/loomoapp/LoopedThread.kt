package com.example.loomoapp

/**
 *
 */

import android.annotation.SuppressLint
import android.os.Handler
import android.os.HandlerThread

class LoopedThread(name: String, priority: Int) : HandlerThread(name, priority) {
    var handler = Handler()

    override fun onLooperPrepared() {
        handler = @SuppressLint("HandlerLeak")
        object : Handler() {}
    }

}