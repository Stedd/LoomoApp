package com.example.loomoapp.Runnables

class PController(setpoint: Float, actual: Float, gain:Float) : Runnable {
    private val setpoint_   = setpoint
    private val actual_     = actual
    private val gain_       = gain
    var out                 = 0.0f

    override fun run() {
        out = controller()
    }
    private fun controller():Float{
        return (setpoint_-actual_)*gain_
    }
}