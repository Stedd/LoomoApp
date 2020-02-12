package com.example.loomoapp

import android.util.Log

open class ThreadLoop: Runnable {

    open val interval: Long = 10 //Thread interval in milliseconds

    //Enable code execution on thread
    open var enable = false

    override fun run() {
        while (enable) {
            main()
            Thread.sleep(interval)
            if (!enable) {
                close()
                break
            }
        }
        Log.i(TAG, "${Thread.currentThread()} stopped.")
    }

    open fun main() {
        //Write Thread loop code here

        //Post results to viewModel or UI Thread
        //threadHandler.post {
            //Example:
            //viewModel.text.value = "Value: $someValue"
        //}
    }

    open fun close() {
        //Instructions when stopping
        //Example:

        //mBase.setLinearVelocity(0.0F)

        //threadHandler.post {
            //viewModel.text.value = "Thread X stopped"
        //}
    }
}