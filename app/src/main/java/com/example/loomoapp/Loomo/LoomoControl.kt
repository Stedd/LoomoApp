package com.example.loomoapp.Loomo

import android.util.Log
import com.example.loomoapp.utils.NonBlockingInfLoop
import com.example.loomoapp.utils.Vector


class LoomoControl(private val base: LoomoBase, private val sensor: LoomoSensor) {

    companion object {
        private val TAG = "LoomoControl"
    }

    var loomoPilotActive = false

    //static parameters
    private val startTime = System.currentTimeMillis()
    private val maxSpeed = 0.2F

    //Controller tuning
    private val linGain = -0.0009F
    private val angGain = -0.0025F

    //Variables
    private var error = 0.0F
    private var turnError = 0.0F

    private var dist = 0.0F
    private var setpoint = 750.0F

    private var a = Vector()
    private var b = Vector(2f, 2f)
    private var c = Vector(0f, 0f)
    private val vec = Vector()


    fun onResume() {
        a.set(1f, 2f)
        Log.d(TAG, "Vector set: x:${a.x_}, y:${a.y_}");
        c.add(a)
        Log.d(TAG, "Vector set: x:${c.x_}, y:${c.y_}");
        c.add(b)
        Log.d(TAG, "Vector addition: x:${c.x_}, y:${c.y_}");
        c.mult(-1f)
        Log.d(TAG, "Vector addition: x:${c.x_}, y:${c.y_}");


        NonBlockingInfLoop{
            if (loomoPilotActive){
                distanceController()
            }
        }
    }


    private fun distanceController(){
        //Logic
//        setpoint = 500.0F + (150.0F * sin((startTime - currentTimeMillis()).toFloat() * 2E-4F))

        dist = sensor.getSurroundings().UltraSonic.toFloat()
        error = linGain * (setpoint - dist)
        turnError = 0.0f
//        turnError = angGain * (mLoomoSensor.getSurroundings().IR_Left.toFloat() -
//                mLoomoSensor.getSurroundings().IR_Right.toFloat())

        //Set velocity
        base.mBase.setLinearVelocity(saturation(error, maxSpeed))
        base.mBase.setAngularVelocity(saturation(turnError, 0.5F))


//        viewModel.text.value =
//            "Distance controller\nsetp:$setpoint\ndist:$dist\nLin_Vel: ${saturation(
//                error,
//                maxSpeed
//            )}\nAng_Vel:$turnError"
    }

    private fun saturation(speed: Float, maxSpeed: Float): Float {
        return when {
            speed > maxSpeed -> {
                maxSpeed
            }
            speed < -maxSpeed -> {
                -maxSpeed
            }
            else -> {
                speed
            }
        }

    }
}
