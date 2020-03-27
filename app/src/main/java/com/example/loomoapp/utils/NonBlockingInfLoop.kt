package com.example.loomoapp.utils

import android.os.Process
import android.util.Log
import kotlin.concurrent.thread

/*
 * Usage example:
 * var i = 0
 * val myLoop = NonBlockingInfLoop {
 *      // Code to run repeatedly
 *      foo()
 *      bar()
 *      i++
 * }
 *
 * someButton.setOnClickListener {
 *      myLoop.togglePause()
 *      Log.d(TAG, "Loop iterations: $i")
 * }
 */
class NonBlockingInfLoop(val loop: () -> Unit) {

    private val TAG = "NonBlockingInfLoop"
    private var running = true
    private var paused = false

    private val foo =
        thread(
            start = true,
            isDaemon = false,
            contextClassLoader = null,
            name = "Inf Loop",
            priority = Process.THREAD_PRIORITY_DEFAULT
        ) {
            while (running) {
                if (paused) continue
                loop()
            }
        }

    /**
     * The current loop cycle will finish, and then the loop is paused
     */
    fun pause() {
        paused = true
    }

    /**
     * Resumes the loop
     */
    fun resume() {
        paused = false
    }

    /**
     * Toggles the paused state instead of explicitly setting it
     */
    fun togglePause() {
        paused = !paused
    }


    /**
     * The current loop cycle will finish, and then the thread is stopped
     */
    fun kill() {
        running = false
    }
}
//
//class NonBlockingInfLoop(val loop: () -> Unit) {
//    private val TAG = "NonBlockingInfLoop"
//    private var running = true
//    private var paused = false
//
//    private val thread = LoopedThread("$TAG thread", Process.THREAD_PRIORITY_DEFAULT)
//    private inner class Loop: Runnable {
//        override fun run() {
//            Log.d(TAG, "${Thread.currentThread()} started")
//            while (running) {
//                if (paused) continue
//                loop()
//            }
//            Log.d(TAG, "${Thread.currentThread()} stopped")
//        }
//    }
//    private val loopObject = Loop()
//    init {
//        thread.start()
//        thread.handler.post {
//            loopObject.run()
//        }
//    }
//
//
//    /**
//     * The current loop cycle will finish, and then the loop is paused
//     */
//    fun pause() {
//        paused = true
//    }
//
//    /**
//     * Resumes the loop
//     */
//    fun resume() {
//        paused = false
//    }
//
//    /**
//     * Toggles the paused state instead of explicitly setting it
//     */
//    fun togglePause() {
//        paused = !paused
//    }
//
//
////    /**
////     * The current loop cycle will finish, and then the thread is stopped
////     */
////    fun kill() {
////        running = false
////    }
//}
