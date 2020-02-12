package com.example.loomoapp

import android.util.Log
import java.lang.System.currentTimeMillis
import kotlin.math.sin

class DistanceController : ThreadLoop() {
    override var interval: Long = 10
    override var enable = false

    private var error: Float = 0.0F
    private var turnError = 0.0F
    private var startTime = currentTimeMillis()
    private var dist: Float = 0.0F
    private val linGain: Float = -0.0009F
    private val angGain: Float = -0.0025F
    private var setpoint: Float = 500.0F

    override fun main() {
        //Logic
//        setpoint = 500.0F + (150.0F * sin((startTime - currentTimeMillis()).toFloat() * 2E-4F))

        dist = mLoomoSensor.getSurroundings().UltraSonic.toFloat()
        error = linGain * (setpoint - dist)
//        turnError = angGain * (mLoomoSensor.getSurroundings().IR_Left.toFloat() -
//                mLoomoSensor.getSurroundings().IR_Right.toFloat())
        //Set velocity
        mBase.setLinearVelocity(error)
        mBase.setAngularVelocity(0.0F)


        //Loggers
//        Log.i("asd", "Ultrasonic: $dist. ${Thread.currentThread()}")


        //Post variables to UI
        threadHandler.post {
            viewModel.text.value =
                "Distance controller\n" +
                        "setp:$setpoint\n" +
                        "dist:$dist\n" +
                        "Lin_Vel: $error\n" +
                        "Ang_Vel:$turnError"
        }
    }

    override fun close() {
        mBase.setLinearVelocity(0.0F)
        mBase.setAngularVelocity(0.0F)
        threadHandler.post {
            viewModel.text.value = "Thread stopped"
        }
    }

}