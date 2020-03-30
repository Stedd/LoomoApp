package com.example.loomoapp.utils

/**
 * Usage:
 * Thread priority must be from android.os and not from java.lang.Thread
 * Below is an example. The 'start()' function must be called before posting to the thread
 *
 * mLoopedThread = LoopedThread("Foo", Process.THREAD_PRIORITY_DEFAULT)
 * mLoopedThread.start()
 * mLoopedThread.handler.post {
 *     bar()
 * }
 * mLoopedThread.handler.post(Runnable)
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