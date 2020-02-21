package com.example.loomoapp

import android.util.Log
import java.lang.System.currentTimeMillis
import kotlin.math.sin

class DistanceController : ThreadLoop() {
    //Thread variables
    override var interval: Long = 100
    override var enable         = false

    //static parameters
    private val startTime       = currentTimeMillis()
    private val maxSpeed        = 0.2F

    //Controller tuning
    private val linGain         = -0.0009F
    private val angGain         = -0.0025F

    //Variables
    private var error           = 0.0F
    private var turnError       = 0.0F

    private var dist            = 0.0F
    private var setpoint        = 350.0F

    override fun main() {
        //Logic
//        setpoint = 500.0F + (150.0F * sin((startTime - currentTimeMillis()).toFloat() * 2E-4F))

        dist = mLoomoSensor.getSurroundings().UltraSonic.toFloat()
        error = linGain * (setpoint - dist)
//        turnError = angGain * (mLoomoSensor.getSurroundings().IR_Left.toFloat() -
//                mLoomoSensor.getSurroundings().IR_Right.toFloat())
        //Set velocity
        mBase.setLinearVelocity(saturation(error,maxSpeed))
        mBase.setAngularVelocity(saturation(turnError,0.5F))

        //Loggers
//        Log.i("asd", "Ultrasonic: $dist. ${Thread.currentThread()}")
//        Log.i(TAG, "All sensors: ${mLoomoSensor.getSensPose2D()}")

        //Post variables to UI
        threadHandler.post {
            viewModel.text.value = "Distance controller\nsetp:$setpoint\ndist:$dist\nLin_Vel: ${saturation(error,maxSpeed)}\nAng_Vel:$turnError"
        }
    }

    override fun close() {
        mBase.setLinearVelocity(0.0F)
        mBase.setAngularVelocity(0.0F)
        threadHandler.post {
            viewModel.text.value = "Thread stopped"
        }
    }

    private fun saturation(speed: Float, maxSpeed:Float):Float{
        return when {
            speed > maxSpeed    -> {maxSpeed}
            speed < -maxSpeed   -> {-maxSpeed}
            else                -> {speed}
        }

    }
}