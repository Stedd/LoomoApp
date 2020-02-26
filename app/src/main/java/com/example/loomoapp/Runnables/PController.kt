package com.example.loomoapp.Runnables

class PController(setpoint: Float, actual: Float, gain:Float, maxSpeed: Float) : Runnable {
    private val setpoint_   = setpoint
    private val actual_     = actual
    private val gain_       = gain
    private val maxSpeed_   = maxSpeed
    var out                 = 0.0f

    override fun run() {
        out = controller()
    }
    private fun controller():Float{
        return saturation((setpoint_-actual_)*gain_, maxSpeed_)
    }

    private fun saturation(speed: Float, maxSpeed_: Float): Float {
        return when {
            speed > maxSpeed_ -> {
                maxSpeed_
            }
            speed < -maxSpeed_ -> {
                -maxSpeed_
            }
            else -> {
                speed
            }
        }

    }
}