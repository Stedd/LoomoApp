package com.example.loomoapp

import android.util.Log
import java.lang.System.currentTimeMillis
import kotlin.math.sin

class DistanceController : Runnable {
    var enable = false
    var error: Float = 0.0F
    var turnError = 0.0F
    var startTime = currentTimeMillis()
    var dist: Float = 0.0F
    val gain: Float = 0.002F
    var setpoint: Float = 500.0F

    override fun run() {
        while (enable) {
            //Logic

            setpoint = 500.0F + (150.0F* sin((startTime-currentTimeMillis()).toFloat()*2E-4F))
//                Log.i(TAG, "setp: ${sin((startTime-currentTimeMillis()).toFloat()*2E-4)}. ${Thread.currentThread()}")

            dist = mLoomoSensor.getSurroundings().UltraSonic.toFloat()
//                Log.i("asd", "Ultrasonic: $dist. ${Thread.currentThread()}")
            error = (setpoint - dist) * gain * -1.0f
//                turnError = (mLoomoSensor.getSurroundings().IR_Left.toFloat()-
//                            mLoomoSensor.getSurroundings().IR_Right.toFloat())*
//                            -0.01F
            //Set velocity
            mBase.setLinearVelocity(error)
            mBase.setAngularVelocity(turnError)

            //Post variables to UI
            threadHandler.post {
                viewModel.text.value =
                        "Distance controller\n" +
                        "setp:$setpoint\n" +
                        "dist:$dist\n" +
                        "Lin_Vel: $error\n" +
                        "Ang_Vel:$turnError"
            }

            //Thread interval
            Thread.sleep(10)

            //Check for stop signal
            if (!enable) {
                mBase.setLinearVelocity(0.0F)
                threadHandler.post {
                    viewModel.text.value = "Thread stopped"
                }
                break
            }
        }
        Log.i(
            TAG,
            "${Thread.currentThread()} stopped."
        )
    }
}