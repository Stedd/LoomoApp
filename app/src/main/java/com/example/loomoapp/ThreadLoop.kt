package com.example.loomoapp

/**
 * Class that creates an infinite loop in a thread.
 * The main() function is meant to be analogous to the 'void loop()' in Arduino
 */


import android.util.Log

open class ThreadLoop: Runnable {

    //Thread interval in milliseconds
    open var interval: Long = 10

    //Enable code execution on thread
    open var enable = false

    //Don't override this
    override fun run() {
        Log.i(TAG, "${Thread.currentThread()} Started.")
        while (enable) {
            main()
            Thread.sleep(interval)
        }
        close()
        Log.i(TAG, "${Thread.currentThread()} stopped.")
    }

    //Override this
    open fun main() {
        //Write Thread loop code here

        //Post results to viewModel or UI Thread
        //threadHandler.post {
            //Example:
            //viewModel.text.value = "Value: $someValue"
        //}
    }

    //Override this if needed
    open fun close() {
        //Instructions when stopping
        //Example:

        //mBase.setLinearVelocity(0.0F)

        //threadHandler.post {
            //viewModel.text.value = "Thread X stopped"
        //}
    }
}