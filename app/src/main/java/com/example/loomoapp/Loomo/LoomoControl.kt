package com.example.loomoapp.Loomo

import android.content.Context
import android.os.Handler

import android.os.Process
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.Observer
import com.example.loomoapp.DistanceController
import com.example.loomoapp.LoopedThread
import com.example.loomoapp.Runnables.PController
//import com.example.loomoapp.viewModel.MainActivityViewModel

class LoomoControl(base: LoomoBase, sensor: LoomoSensor) {
    private val TAG = "LoomoControl"
    private val sensor_ = sensor
    private val base_ = base

    //Initialize threads
    private val mDistanceController = DistanceController(sensor)
    val mControllerThread: LoopedThread =
        LoopedThread("LoomoControl", Process.THREAD_PRIORITY_FOREGROUND)

    fun startController(context: Context, msg: String) {
        if (!mControllerThread.isAlive) {
            Log.i(TAG, msg)
//            mDistanceController.enable = true
            mControllerThread.handler.post {
                    base_.mBase.setLinearVelocity(PController(350.0F, sensor_.getSurroundings().UltraSonic.toFloat(), -0.0009F, 0.2F).out)
            }

        } else {
            Toast.makeText(
                context,
                "Dude, the controller is already activated..",
                Toast.LENGTH_SHORT
            )
                .show()
        }
    }

    fun stopController(context: Context, msg: String) {
        if (mControllerThread.isAlive) {
            Log.i(TAG, msg)
//            mDistanceController.enable = false
        }
    }


}